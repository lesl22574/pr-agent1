/* Игра «Вытри букву»: полотенце стирает мыльную пену с буквы.
   Совместимость уровня «web 1»: только mouse/touch-события, синтаксис ES5,
   никаких сборок и серверов — достаточно открыть index.html. */
(function () {
  'use strict';

  var CFG = {
    STAGE_W: 1280,
    STAGE_H: 720,
    LETTER_H: 520,        // высота буквы на сцене, px (ширина — по пропорции спрайта)
    BRUSH: 40,            // радиус кисти стирания, px
    WIN_AT: 0.90,         // доля стёртой пены для победы
    WIN_CLEAN: 0.02,      // «на глаз чисто»: остаток пены меньше 2 % (и стёрто больше половины) — тоже победа
    CHECK_MS: 100,        // как часто пересчитывать прогресс, мс
    PROBE: 96,            // разрешение пробы прогресса (96x96, от размера буквы не зависит)
    GRID: 24,             // сетка покрытия кистью — запасная метрика, если пиксели недоступны
    METRIC: null,         // null — авто ('letter' | 'area' | 'grid' можно задать вручную)
    LOG: true,            // вывод событий в консоль
    LOG_ON_SCREEN: null,  // null — авто (?debug=1), true/false — принудительно
    TOWEL_W: 150,         // ширина полотенца на сцене, px
    TOWEL_H: 172,         // высота полотенца на сцене, px
    TOWEL_REST: { x: 365, y: 340 }, // где полотенце «лежит» до взятия
    WIN_DELAY_GO: 1400,   // задержка перед переходом по completeUrl, мс
    RES: 2                // суперсэмплинг canvas буквы
  };

  function $(id) { return document.getElementById(id); }
  var stage = $('stage');
  var letterWrap = $('letterWrap');
  var cleanLetter = $('cleanLetter');
  var foamCanvas = $('foamCanvas');
  var fxCanvas = $('fxCanvas');
  var towelWrap = $('towelWrap');
  var grabTip = $('grabTip');
  var hintEl = $('hint');
  var winOverlay = $('winOverlay');
  var winStars = $('winStars');
  var winSub = $('winSub');

  var fctx = foamCanvas.getContext('2d');
  var xctx = fxCanvas.getContext('2d');

  /* выбранная вручную метрика: ?metric=letter | area | grid или CFG.METRIC */
  function metricOverride() {
    if (CFG.METRIC) return String(CFG.METRIC).toLowerCase();
    var m = /[?&]metric=(letter|area|grid)\b/.exec(String(window.location.search || ''));
    return m ? m[1] : '';
  }

  /* ---------- журнал событий ---------- */
  /* Все важные шаги игры пишутся в консоль браузера в виде
     «[вытри-букву] событие {данные}». При ?debug=1 (или WIPE_LOG = true)
     тот же журнал дублируется в панель на экране. */

  var logLines = [];
  var logPanel = null;
  var logPanelTries = 0;

  function wantScreenLog() {
    if (CFG.LOG_ON_SCREEN === true) return true;
    if (CFG.LOG_ON_SCREEN === false) return false;
    if (window.WIPE_LOG === true) return true;
    return /[?&]debug=1\b/.test(String(window.location.search || ''));
  }

  function ensureLogPanel() {
    if (logPanel || !wantScreenLog()) return;
    if (!document.body) {
      if (logPanelTries++ < 50) setTimeout(ensureLogPanel, 100);
      return;
    }
    logPanel = document.createElement('div');
    logPanel.style.cssText = 'position:fixed;left:8px;bottom:8px;max-width:46vw;max-height:38vh;' +
      'overflow:hidden;z-index:99999;pointer-events:none;font:12px/1.45 monospace;' +
      'color:#d9f3ff;background:rgba(8,20,38,.72);border:1px solid rgba(160,220,255,.35);' +
      'border-radius:8px;padding:8px 10px;white-space:pre-wrap;';
    logPanel.setAttribute('id', 'wipeLogPanel');
    logPanel.setAttribute('aria-hidden', 'true');
    document.body.appendChild(logPanel);
    renderLogPanel();
  }

  function renderLogPanel() {
    if (!logPanel) return;
    var out = [];
    for (var i = 0; i < logLines.length; i++) out.push(logLines[i]);
    logPanel.textContent = out.join('\n');
  }

  function logEvent(name, data) {
    var tail = '';
    if (data !== undefined && data !== null) {
      try { tail = ' ' + (typeof data === 'string' ? data : JSON.stringify(data)); }
      catch (e) { tail = ' ' + String(data); }
    }
    var line = name + tail;
    var stamp = new Date();
    var t = ('0' + stamp.getHours()).slice(-2) + ':' + ('0' + stamp.getMinutes()).slice(-2) +
            ':' + ('0' + stamp.getSeconds()).slice(-2);
    logLines.push(t + '  ' + line);
    if (logLines.length > 40) logLines.shift();
    window.__wipeLog = logLines;
    if (CFG.LOG) {
      try {
        if (window.console && console.log) console.log('[вытри-букву] ' + line);
      } catch (e) { /* консоль может отсутствовать — не мешаем игре */ }
    }
    ensureLogPanel();
    renderLogPanel();
    try { if (typeof window.onWipeEvent === 'function') window.onWipeEvent(name, data || null); }
    catch (e2) { /* внешний обработчик не должен ломать игру */ }
  }

  foamCanvas.width = 460 * CFG.RES;
  foamCanvas.height = CFG.LETTER_H * CFG.RES;
  fxCanvas.width = CFG.STAGE_W;
  fxCanvas.height = CFG.STAGE_H;

  var state = {
    idx: 0,
    letterW: 460,
    letterH: CFG.LETTER_H,
    wiping: false,
    dragging: false,
    interacted: false,
    won: false,
    muted: false,
    startTime: 0,
    initialLeft: 0,       // сколько пены было в начале (0..1 по силуэту буквы)
    initialAll: 0,        // то же по всей площади картинки пены
    useMask: true,        // считать прогресс по силуэту буквы
    pixelsOK: true,       // удаётся ли читать пиксели пены
    pixelsWarned: false,
    stamps: 0,
    checks: 0,
    progress: 0,          // 0..1 — доля стёртого
    lastCheck: 0,
    lastLogT: 0,
    milestone: 0,         // последний отмеченный рубеж 25/50/75 %
    stallWarned: false,
    noFoamWarned: false
  };

  var pointer = { x: CFG.STAGE_W / 2, y: CFG.STAGE_H / 2 };
  var towel = { x: CFG.TOWEL_REST.x, y: CFG.TOWEL_REST.y, px: CFG.TOWEL_REST.x, py: CFG.TOWEL_REST.y, rot: 0 };
  var lastWipe = null;
  var wrapOrigin = { x: 0, y: 0 };
  var stageRect = null;
  var stageScale = 1;
  var particles = [];

  /* ---------- классы (без classList, для старых браузеров) ---------- */

  function hasClass(el, c) { return (' ' + el.className + ' ').indexOf(' ' + c + ' ') >= 0; }
  function addClass(el, c) { if (!hasClass(el, c)) el.className += (el.className ? ' ' : '') + c; }
  function removeClass(el, c) {
    el.className = (' ' + el.className + ' ').split(' ' + c + ' ').join(' ').replace(/\s+/g, ' ').replace(/^ |\s$/g, '');
  }

  /* ---------- геометрия и масштаб ---------- */

  function cacheRects() {
    stageRect = stage.getBoundingClientRect();
    stageScale = stageRect.width / CFG.STAGE_W || 1;
    var wr = letterWrap.getBoundingClientRect();
    wrapOrigin = {
      x: (wr.left - stageRect.left) / stageScale,
      y: (wr.top - stageRect.top) / stageScale
    };
  }

  function fit() {
    var s = Math.min(innerWidth / CFG.STAGE_W, innerHeight / CFG.STAGE_H);
    stage.style.transform = 'translate(-50%, -50%) scale(' + s + ')';
    stage.style.webkitTransform = stage.style.transform;
    cacheRects();
  }

  function toStage(clientX, clientY) {
    return {
      x: (clientX - stageRect.left) / stageScale,
      y: (clientY - stageRect.top) / stageScale
    };
  }
  function toFoam(p) { return { x: p.x - wrapOrigin.x, y: p.y - wrapOrigin.y }; }
  function fromFoam(p) { return { x: p.x + wrapOrigin.x, y: p.y + wrapOrigin.y }; }

  /* ---------- кисть стирания ---------- */

  var brush = document.createElement('canvas');
  brush.width = brush.height = CFG.BRUSH * 2;
  (function () {
    var b = brush.getContext('2d');
    var g = b.createRadialGradient(CFG.BRUSH, CFG.BRUSH, 1, CFG.BRUSH, CFG.BRUSH, CFG.BRUSH);
    g.addColorStop(0, 'rgba(0,0,0,1)');
    g.addColorStop(0.6, 'rgba(0,0,0,0.95)');
    g.addColorStop(1, 'rgba(0,0,0,0)');
    b.fillStyle = g;
    b.fillRect(0, 0, CFG.BRUSH * 2, CFG.BRUSH * 2);
  })();

  function stamp(x, y) {
    fctx.globalCompositeOperation = 'destination-out';
    fctx.drawImage(brush, x - CFG.BRUSH, y - CFG.BRUSH, CFG.BRUSH * 2, CFG.BRUSH * 2);
    fctx.globalCompositeOperation = 'source-over';
    state.stamps += 1;
    markGrid(x, y);
  }

  /* ---------- прогресс: сколько пены осталось ---------- */
  /* Раньше считалось «количество пикселей с альфой > 24» в пробе 48x48, а прогресс
     сравнивался с этим же числом. Из-за жёсткого порога 24 метрика врала в двух
     случаях: очень бледная пена (все пиксели ниже порога => «уже чисто» с первой
     секунды) и мягкая полупрозрачная пена (шкала ползла ступеньками и «залипала»
     на краях). Теперь суммируется альфа, то есть метрика непрерывная, и считается
     она только по силуэту буквы — «вся буква вытерта» означает именно это. */

  var PW = CFG.PROBE, PH = CFG.PROBE;
  var probe = document.createElement('canvas');
  probe.width = PW; probe.height = PH;
  var pctx = probe.getContext('2d');

  var maskW = null;        // веса пикселей силуэта буквы (0..1)
  var maskOK = false;

  function buildMask(img) {
    maskW = null;
    maskOK = false;
    if (!img || !img.naturalWidth) return;
    try {
      pctx.clearRect(0, 0, PW, PH);
      pctx.drawImage(img, 0, 0, PW, PH);
      var d = pctx.getImageData(0, 0, PW, PH).data;
      var w = new Float32Array(PW * PH);
      var sum = 0;
      for (var i = 3, j = 0; i < d.length; i += 4, j++) {
        var a = d[i] / 255;
        w[j] = a;
        sum += a;
      }
      pctx.clearRect(0, 0, PW, PH);
      if (sum > PW * PH * 0.01) {
        maskW = w;
        maskOK = true;
        logEvent('силуэт буквы измерен', { доля_площади: Math.round(100 * sum / (PW * PH)) / 100 });
      } else {
        logEvent('силуэт буквы пуст — считаю прогресс по всей площади', '');
      }
    } catch (e) {
      pctx.clearRect(0, 0, PW, PH);
      logEvent('силуэт буквы недоступен (canvas закрыт чужим доменом) — считаю по всей площади', String(e));
    }
  }

  /* Возвращает {v, vAll, px}:
     v    — доля «закрытой пеной» площади буквы (0 — чисто, 1 — вся в пене),
     vAll — то же самое, но по всей площади картинки пены (без силуэта),
     px   — грубое число плотных пикселей (для справки в журнале). */
  function foamLeft() {
    pctx.clearRect(0, 0, PW, PH);
    pctx.drawImage(foamCanvas, 0, 0, PW, PH);
    var d;
    try {
      d = pctx.getImageData(0, 0, PW, PH).data;
    } catch (e) {
      if (!state.pixelsWarned) {
        state.pixelsWarned = true;
        logEvent('пиксели пены недоступны (canvas закрыт чужим доменом?) — перехожу на сетку покрытия', String(e));
      }
      return null;
    }
    var sum = 0, ref = 0, sumAll = 0, px = 0;
    for (var i = 3, j = 0; i < d.length; i += 4, j++) {
      var a = d[i] / 255;
      var w = maskOK ? maskW[j] : 1;
      sum += a * w;
      ref += w;
      sumAll += a;
      if (d[i] > 24) px++;
    }
    return {
      v: ref > 0 ? sum / ref : 0,
      vAll: sumAll / (PW * PH),
      px: px
    };
  }

  /* ---------- сетка покрытия кистью (запасная метрика) ---------- */
  /* Работает вообще без чтения пикселей: запоминаем, по каким ячейкам прошла кисть.
     Нужна, если браузер не отдаёт getImageData (canvas «закрыт» чужим доменом,
     локальный файл и т.п.) — иначе прогресс и победа недостижимы в принципе. */

  var GRID = CFG.GRID;
  var gridHit = null;
  var gridW = null;

  function resetGrid() {
    gridHit = new Uint8Array(GRID * GRID);
    gridW = null;
    if (!maskOK) return;
    /* веса ячеек берём из силуэта буквы, усредняя маску пробы */
    var cell = PW / GRID;
    var gw = new Float32Array(GRID * GRID);
    var sum = 0;
    for (var gy = 0; gy < GRID; gy++) {
      for (var gx = 0; gx < GRID; gx++) {
        var acc = 0;
        var y0 = Math.floor(gy * cell), y1 = Math.min(PH, Math.ceil((gy + 1) * cell));
        var x0 = Math.floor(gx * cell), x1 = Math.min(PW, Math.ceil((gx + 1) * cell));
        for (var y = y0; y < y1; y++) {
          for (var x = x0; x < x1; x++) acc += maskW[y * PW + x];
        }
        gw[gy * GRID + gx] = acc;
        sum += acc;
      }
    }
    if (sum > 0) gridW = gw;
  }

  function markGrid(x, y) {
    if (!gridHit || state.letterW <= 0) return;
    var cw = state.letterW / GRID, ch = state.letterH / GRID;
    var r = CFG.BRUSH;
    var gx0 = Math.max(0, Math.floor((x - r) / cw)), gx1 = Math.min(GRID - 1, Math.floor((x + r) / cw));
    var gy0 = Math.max(0, Math.floor((y - r) / ch)), gy1 = Math.min(GRID - 1, Math.floor((y + r) / ch));
    for (var gy = gy0; gy <= gy1; gy++) {
      for (var gx = gx0; gx <= gx1; gx++) {
        var dx = (gx + 0.5) * cw - x, dy = (gy + 0.5) * ch - y;
        if (dx * dx + dy * dy <= r * r) gridHit[gy * GRID + gx] = 1;
      }
    }
  }

  function gridProgress() {
    if (!gridHit) return 0;
    var hit = 0, all = 0;
    for (var i = 0; i < gridHit.length; i++) {
      var w = gridW ? gridW[i] : 1;
      all += w;
      if (gridHit[i]) hit += w;
    }
    return all > 0 ? hit / all : 0;
  }

  /* ---------- загрузка букв ---------- */

  var imgs = [];
  (function () {
    for (var i = 0; i < window.LETTERS.length; i++) {
      (function (L) {
        var rec = { foam: new Image(), clean: new Image(), ready: false, left: 2 };
        var done = function () { rec.left -= 1; if (rec.left === 0) rec.ready = true; };
        rec.foam.onload = done;
        rec.clean.onload = done;
        rec.foam.onerror = function () { logEvent('не загрузилась картинка пены', L.foam); };
        rec.clean.onerror = function () { logEvent('не загрузилась картинка буквы', L.clean); };
        rec.foam.src = L.foam;
        rec.clean.src = L.clean;
        imgs.push(rec);
      })(window.LETTERS[i]);
    }
  })();

  function loadLetter(i) {
    state.idx = i;
    var L = window.LETTERS[i];
    var rec = imgs[i];
    var apply = function () {
      cleanLetter.src = L.clean;
      var nw = rec.clean.naturalWidth || 528;
      var nh = rec.clean.naturalHeight || 596;
      state.letterH = CFG.LETTER_H;
      state.letterW = Math.round(CFG.LETTER_H * nw / nh);
      letterWrap.style.width = state.letterW + 'px';
      letterWrap.style.height = state.letterH + 'px';
      foamCanvas.width = state.letterW * CFG.RES;
      foamCanvas.height = state.letterH * CFG.RES;
      fctx.setTransform(CFG.RES, 0, 0, CFG.RES, 0, 0);
      fctx.clearRect(0, 0, state.letterW, state.letterH);
      fctx.drawImage(rec.foam, 0, 0, state.letterW, state.letterH);
      foamCanvas.style.opacity = '1';
      removeClass(letterWrap, 'bounce');
      addClass(winOverlay, 'hidden');
      state.won = false;
      state.wiping = false;
      state.dragging = false;
      state.startTime = 0;
      state.interacted = false;
      removeClass(hintEl, 'gone');
      removeClass(grabTip, 'gone');
      towel.x = towel.px = CFG.TOWEL_REST.x;
      towel.y = towel.py = CFG.TOWEL_REST.y;
      towel.rot = 0;
      addClass(towelWrap, 'idle');
      removeClass(towelWrap, 'drag');
      state.pixelsWarned = false;
      buildMask(rec.clean);
      resetGrid();
      var start = foamLeft();
      state.pixelsOK = !!start;
      state.initialLeft = start ? start.v : 0;
      state.initialAll = start ? start.vAll : 0;
      var mo = metricOverride();
      state.useMask = mo === 'area' ? false : mo === 'letter' ? true : maskOK;
      var maskMismatch = false;
      if (state.useMask && state.initialLeft < 0.01 && state.initialAll >= 0.02) {
        /* пена есть, а на силуэте буквы её нет — значит картинки не совпадают */
        state.useMask = false;
        maskMismatch = true;
      }
      state.progress = 0;
      state.checks = 0;
      state.stamps = 0;
      state.milestone = 0;
      state.stallWarned = false;
      state.noFoamWarned = false;
      state.lastCheck = 0;
      state.lastLogT = 0;
      cacheRects();
      logEvent('буква готова', {
        index: state.idx,
        char: L.char,
        размер: state.letterW + 'x' + state.letterH,
        пена_в_начале: Math.round(state.initialLeft * 1000) / 1000,
        пена_по_площади: Math.round(state.initialAll * 1000) / 1000,
        силуэт: maskOK ? 'есть' : 'нет',
        метрика: mo || (!state.pixelsOK ? 'сетка покрытия' : state.useMask ? 'по силуэту' : 'по площади')
      });
      if (maskMismatch) {
        logEvent('силуэт буквы не совпадает с пеной — считаю прогресс по всей площади буквы', {
          пена_по_силуэту: Math.round(state.initialLeft * 1000) / 1000,
          пена_по_площади: Math.round(state.initialAll * 1000) / 1000
        });
      }
    };

    var started = false;
    var go = function () {
      if (started) return;
      started = true;
      try { apply(); }
      catch (e) { logEvent('ошибка при подготовке буквы', String(e)); }
    };
    var ready = function () { return rec.foam.naturalWidth > 0 && rec.clean.naturalWidth > 0; };

    if (ready()) go();
    else {
      var onone = function () { if (ready()) go(); };
      rec.foam.onload = onone;
      rec.clean.onload = onone;
      /* страховка: если одна из картинок не пришла, через 2,5 с всё равно стартуем,
         иначе игра навсегда осталась бы без буквы (и без проверки победы) */
      setTimeout(function () {
        if (started) return;
        logEvent('спрайты пришли не полностью, стартую с тем, что есть', { index: i });
        go();
      }, 2500);
    }
  }

  /* ---------- звук (WebAudio с защитой для старых браузеров) ---------- */

  var AC = null, master = null, wipeGain = null;

  function initAudio() {
    if (AC) { if (AC.state === 'suspended' && AC.resume) AC.resume(); return; }
    try {
      var Ctx = window.AudioContext || window.webkitAudioContext;
      if (!Ctx) return;
      AC = new Ctx();
      master = AC.createGain();
      master.gain.value = state.muted ? 0 : 0.9;
      master.connect(AC.destination);
      var len = AC.sampleRate;
      var buf = AC.createBuffer(1, len, AC.sampleRate);
      var ch = buf.getChannelData(0);
      for (var i = 0; i < len; i++) ch[i] = Math.random() * 2 - 1;
      var src = AC.createBufferSource();
      src.buffer = buf;
      src.loop = true;
      var bp = AC.createBiquadFilter();
      bp.type = 'bandpass';
      bp.frequency.value = 950;
      bp.Q.value = 0.7;
      wipeGain = AC.createGain();
      wipeGain.gain.value = 0;
      src.connect(bp); bp.connect(wipeGain); wipeGain.connect(master);
      src.start();
    } catch (e) { AC = null; }
  }

  function setWipeSound(on, speed) {
    if (!AC || !wipeGain) return;
    var t = on ? Math.min(0.25, 0.07 + speed * 0.0045) : 0;
    try {
      /* в старых движках setTargetAtTime может отсутствовать: раньше это исключение
         вылетало из step() и обрывало проверку победы на каждом кадре */
      if (typeof wipeGain.gain.setTargetAtTime === 'function') {
        wipeGain.gain.setTargetAtTime(t, AC.currentTime, 0.09);
      } else {
        wipeGain.gain.value = t;
      }
    } catch (e) {
      logEvent('звук стирания недоступен, отключаю его', String(e));
      wipeGain = null;
    }
  }

  function jingle() {
    if (!AC) return;
    try {
      var notes = [523.25, 659.25, 783.99, 1046.5];
      for (var i = 0; i < notes.length; i++) {
        (function (f, i) {
          var o = AC.createOscillator();
          var g = AC.createGain();
          o.type = 'sine';
          o.frequency.value = f;
          var t0 = AC.currentTime + i * 0.09;
          g.gain.setValueAtTime(0.0001, t0);
          g.gain.exponentialRampToValueAtTime(0.28, t0 + 0.03);
          g.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.5);
          o.connect(g); g.connect(master);
          o.start(t0);
          o.stop(t0 + 0.55);
        })(notes[i], i);
      }
    } catch (e) {
      /* в старых движках WebAudio части методов нет — молча пропускаем звук */
      logEvent('победный звук не сыграл', String(e));
    }
  }

  /* ---------- частицы ---------- */

  function spawnBubble(x, y) {
    particles.push({
      type: 'b',
      x: x + (Math.random() - 0.5) * 34,
      y: y + (Math.random() - 0.5) * 24,
      r: 4 + Math.random() * 8,
      vx: (Math.random() - 0.5) * 40,
      vy: 50 + Math.random() * 70,
      age: 0,
      life: 0.7 + Math.random() * 0.6
    });
  }

  function spawnSparks(cx, cy) {
    for (var i = 0; i < 26; i++) {
      var a = Math.random() * Math.PI * 2;
      var v = 120 + Math.random() * 240;
      particles.push({
        type: 's',
        x: cx, y: cy,
        vx: Math.cos(a) * v,
        vy: Math.sin(a) * v - 60,
        age: 0,
        life: 0.7 + Math.random() * 0.5,
        rot: Math.random() * Math.PI,
        col: Math.random() < 0.5 ? '#ffd75e' : '#7ef0a0'
      });
    }
  }

  function drawParticles(dt) {
    xctx.clearRect(0, 0, CFG.STAGE_W, CFG.STAGE_H);
    for (var i = particles.length - 1; i >= 0; i--) {
      var p = particles[i];
      p.age += dt;
      if (p.age >= p.life) { particles.splice(i, 1); continue; }
      var k = 1 - p.age / p.life;
      p.x += p.vx * dt;
      p.y -= (p.type === 'b' ? p.vy : -p.vy) * dt;
      if (p.type === 's') p.vy += 260 * dt;
      xctx.save();
      xctx.globalAlpha = k;
      if (p.type === 'b') {
        xctx.beginPath();
        xctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        xctx.fillStyle = 'rgba(198,236,255,0.55)';
        xctx.fill();
        xctx.strokeStyle = 'rgba(255,255,255,0.9)';
        xctx.lineWidth = 1.6;
        xctx.stroke();
        xctx.beginPath();
        xctx.arc(p.x - p.r * 0.35, p.y - p.r * 0.35, p.r * 0.28, 0, Math.PI * 2);
        xctx.fillStyle = 'rgba(255,255,255,0.95)';
        xctx.fill();
      } else {
        xctx.translate(p.x, p.y);
        xctx.rotate(p.rot + p.age * 4);
        xctx.strokeStyle = p.col;
        xctx.lineWidth = 3;
        xctx.lineCap = 'round';
        var r = 9 * k + 3;
        xctx.beginPath();
        xctx.moveTo(-r, 0); xctx.lineTo(r, 0);
        xctx.moveTo(0, -r); xctx.lineTo(0, r);
        xctx.stroke();
      }
      xctx.restore();
    }
  }

  /* ---------- победа ---------- */

  function now() { return (window.performance && performance.now) ? performance.now() : Date.now(); }

  /* Событие «буква вытерта целиком»: и CustomEvent для слушателей,
     и простой колбэк window.onLetterCleaned(detail) для старых окружений. */
  function dispatchCleaned(detail) {
    try {
      if (typeof window.CustomEvent === 'function') {
        window.dispatchEvent(new window.CustomEvent('letter:cleaned', { detail: detail }));
      } else if (document.createEvent) {
        var e = document.createEvent('Event');
        e.initEvent('letter:cleaned', true, true);
        e.detail = detail;
        window.dispatchEvent(e);
      }
    } catch (err) { window.loopError = err; }
    if (typeof window.onLetterCleaned === 'function') {
      try { window.onLetterCleaned(detail); } catch (err2) { window.loopError = err2; }
    }
  }

  /* Пересчёт прогресса и проверка победы. Вызывается из главного цикла
     и при отпускании полотенца — то есть даже последний «штрих» учитывается. */
  function ratio(left, base) {
    var g = base > 0.001 ? 1 - left / base : 0;
    if (!(g >= 0)) g = 0;
    if (g > 1) g = 1;
    return g;
  }

  /* Пересчёт прогресса и проверка победы. Вызывается из главного цикла
     и при отпускании полотенца — то есть даже последний «штрих» учитывается.
     Метрика выбирается сама: силуэт буквы -> площадь картинки -> сетка покрытия. */
  function checkWin(t) {
    if (state.won) return false;
    state.checks += 1;

    var cur = foamLeft();
    var gone, left, goneAll = 0, source;

    if (!cur) {
      /* пиксели прочитать нельзя — считаем по следу кисти */
      state.pixelsOK = false;
      gone = gridProgress();
      left = 1 - gone;
      source = 'сетка покрытия';
    } else {
      state.pixelsOK = true;
      goneAll = ratio(cur.vAll, state.initialAll);
      if (state.useMask && state.initialLeft >= 0.01) {
        gone = ratio(cur.v, state.initialLeft);
        left = cur.v;
        source = 'силуэт буквы';
        /* самопроверка: если по силуэту прогресс стоит, а по площади растёт,
           значит силуэт не совпадает с пеной (другая картинка, смещение,
           пена вне буквы) — переключаемся, иначе победа недостижима */
        if (state.checks > 8 && gone < 0.05 && goneAll > 0.2) {
          var maskGone = gone;
          state.useMask = false;
          gone = goneAll;
          left = cur.vAll;
          source = 'площадь (силуэт не совпал)';
          state.milestone = 0;
          logEvent('силуэт буквы не совпал с пеной — переключаюсь на подсчёт по всей площади', {
            по_силуэту_процентов: Math.round(maskGone * 1000) / 10,
            по_площади_процентов: Math.round(goneAll * 1000) / 10
          });
        }
      } else {
        gone = goneAll;
        left = cur.vAll;
        source = 'площадь';
      }
    }

    state.progress = gone;
    if (!(state.progress >= 0)) state.progress = 0;

    /* страховка: пены на букве нет вовсе — победу не выдаём, один раз предупреждаем */
    if (state.pixelsOK && state.initialAll < 0.01) {
      if (!state.noFoamWarned) {
        state.noFoamWarned = true;
        logEvent('пена на букве не обнаружена — проверьте картинку foam у буквы', {
          index: state.idx, пена_в_начале: Math.round(state.initialAll * 1000) / 1000
        });
      }
      return false;
    }

    /* подсказка, если игрок водит полотенцем, а прогресса нет */
    if (gone > 0.02) state.stallWarned = false;
    if (!state.stallWarned && state.stamps > 200 && gone < 0.02 && !state.won) {
      state.stallWarned = true;
      logEvent('прогресс не растёт: проверьте, что полотенце проходит именно по пене', {
        штампов: state.stamps,
        метрика: source,
        пена_в_начале: Math.round(state.initialLeft * 1000) / 1000,
        пена_canvas: foamCanvas.width + 'x' + foamCanvas.height,
        поле_буквы: state.letterW + 'x' + state.letterH
      });
    }

    /* журнал: рубежи 25/50/75 % и не чаще двух записей в секунду */
    var ms = gone >= 0.75 ? 75 : gone >= 0.5 ? 50 : gone >= 0.25 ? 25 : 0;
    if (ms > state.milestone) {
      state.milestone = ms;
      logEvent('стёрто ' + ms + '%', {
        стёрто_процентов: Math.round(gone * 1000) / 10,
        осталось_пены: Math.round(left * 1000) / 1000,
        по_площади_процентов: Math.round(goneAll * 1000) / 10,
        метрика: source,
        штампов: state.stamps
      });
    } else if (t - state.lastLogT > 500) {
      state.lastLogT = t;
      logEvent('прогресс', {
        стёрто_процентов: Math.round(gone * 1000) / 10,
        осталось_пены: Math.round(left * 1000) / 1000,
        по_площади_процентов: Math.round(goneAll * 1000) / 10,
        метрика: source,
        штампов: state.stamps
      });
    }

    if (gone >= CFG.WIN_AT) {
      logEvent('условие победы выполнено', {
        стёрто_процентов: Math.round(gone * 1000) / 10, порог: CFG.WIN_AT, метрика: source
      });
      win();
      return true;
    }
    if (left <= CFG.WIN_CLEAN && gone >= 0.5) {
      logEvent('пены практически не осталось — засчитываю победу', {
        осталось_пены: Math.round(left * 1000) / 1000, метрика: source
      });
      win();
      return true;
    }
    return false;
  }

  /* Подробный отчёт в консоль — вызывается как WipeLetter.report() */
  function report() {
    var cur = foamLeft();
    var info = {
      буква: window.LETTERS && window.LETTERS[state.idx] ? window.LETTERS[state.idx].char : null,
      сцена: CFG.STAGE_W + 'x' + CFG.STAGE_H,
      поле_буквы: state.letterW + 'x' + state.letterH,
      пена_canvas: foamCanvas.width + 'x' + foamCanvas.height,
      RES: CFG.RES,
      силуэт: maskOK ? 'есть' : 'нет',
      метрика: state.useMask ? 'по силуэту' : 'по площади',
      пиксели_читаются: state.pixelsOK,
      пена_в_начале_силуэт: Math.round(state.initialLeft * 1000) / 1000,
      пена_в_начале_площадь: Math.round(state.initialAll * 1000) / 1000,
      осталось_силуэт: cur ? Math.round(cur.v * 1000) / 1000 : null,
      осталось_площадь: cur ? Math.round(cur.vAll * 1000) / 1000 : null,
      сетка_процентов: Math.round(gridProgress() * 1000) / 10,
      стёрто_процентов: Math.round(state.progress * 1000) / 10,
      штампов: state.stamps,
      проверок: state.checks,
      победа: state.won
    };
    logEvent('диагностика', info);
    return info;
  }

  function win() {
    if (state.won) return;               // победа возможна только один раз
    logEvent('победа', { index: state.idx, char: window.LETTERS[state.idx].char });
    state.won = true;
    state.wiping = false;
    state.dragging = false;
    removeClass(towelWrap, 'drag');
    setWipeSound(false, 0);
    foamCanvas.style.opacity = '0';
    addClass(letterWrap, 'bounce');
    var c = fromFoam({ x: state.letterW / 2, y: state.letterH / 2 });
    spawnSparks(c.x, c.y);
    jingle();
    var secs = (now() - state.startTime) / 1000;
    var stars = secs < 15 ? 3 : secs < 30 ? 2 : 1;
    var L = window.LETTERS[state.idx];
    winStars.innerHTML = '';
    for (var i = 0; i < 3; i++) {
      var s = document.createElement('span');
      s.textContent = '★';
      s.className = i < stars ? 'star on' : 'star';
      s.style.animationDelay = (0.15 * i) + 's';
      s.style.webkitAnimationDelay = s.style.animationDelay;
      winStars.appendChild(s);
    }
    winSub.textContent = 'Буква «' + L.char + '» сияет чистотой!';

    // внешний контракт: событие + колбэк
    var detail = { char: L.char, index: state.idx, stars: stars, seconds: Math.round(secs) };
    logEvent('событие letter:cleaned', detail);
    dispatchCleaned(detail);

    // переход: поле next у буквы или общий GAME_SETTINGS.completeUrl;
    // плейсхолдеры {char} и {index}. Без URL — победный оверлей как раньше.
    var url = L.next || (window.GAME_SETTINGS && window.GAME_SETTINGS.completeUrl) || '';
    if (url) {
      url = String(url).replace('{char}', encodeURIComponent(L.char)).replace('{index}', String(state.idx));
      setTimeout(function () { window.location.href = url; }, CFG.WIN_DELAY_GO);
    } else {
      setTimeout(function () { removeClass(winOverlay, 'hidden'); }, 700);
    }
  }

  /* ---------- ввод: обычные mouse/touch-события («web 1») ---------- */

  function overTowel(p) {
    return Math.abs(p.x - towel.x) < CFG.TOWEL_W / 2 + 16 &&
           Math.abs(p.y - towel.y) < CFG.TOWEL_H / 2 + 16;
  }

  function press(clientX, clientY) {
    if (state.won) return;
    initAudio();
    cacheRects();
    var p = toStage(clientX, clientY);
    pointer.x = p.x; pointer.y = p.y;
    if (!overTowel(p)) return;               // берём только полотенце
    state.dragging = true;
    state.wiping = true;
    addClass(towelWrap, 'drag');
    removeClass(towelWrap, 'idle');
    logEvent('полотенце взято', { x: Math.round(p.x), y: Math.round(p.y) });
    if (!state.interacted) {
      state.interacted = true;
      addClass(hintEl, 'gone');
      addClass(grabTip, 'gone');
      state.startTime = now();
    }
  }

  function move(clientX, clientY) {
    if (!stageRect) return;
    var p = toStage(clientX, clientY);
    pointer.x = p.x; pointer.y = p.y;
  }

  function release() {
    if (!state.dragging) return;
    state.dragging = false;
    state.wiping = false;
    removeClass(towelWrap, 'drag');
    setWipeSound(false, 0);
    /* финальный пересчёт: если игрок стёр последний кусочек пены и сразу отпустил
       полотенце, победа всё равно засчитывается */
    if (!state.won) {
      try { checkWin(now()); } catch (e) { logEvent('ошибка финальной проверки', String(e)); }
    }
    logEvent('полотенце отпущено', { стёрто_процентов: Math.round(state.progress * 1000) / 10 });
  }

  stage.addEventListener('mousedown', function (e) {
    press(e.clientX, e.clientY);
    if (state.dragging) { if (e.preventDefault) e.preventDefault(); }
  });
  window.addEventListener('mousemove', function (e) { move(e.clientX, e.clientY); });
  window.addEventListener('mouseup', release);

  stage.addEventListener('touchstart', function (e) {
    if (e.touches && e.touches.length) {
      press(e.touches[0].clientX, e.touches[0].clientY);
      if (state.dragging && e.preventDefault) e.preventDefault();
    }
  }, false);
  window.addEventListener('touchmove', function (e) {
    if (e.touches && e.touches.length) {
      move(e.touches[0].clientX, e.touches[0].clientY);
      if (state.dragging && e.preventDefault) e.preventDefault();
    }
  }, false);
  window.addEventListener('touchend', release);
  window.addEventListener('touchcancel', release);

  $('nextBtn').addEventListener('click', function () { logEvent('кнопка «дальше»'); loadLetter((state.idx + 1) % window.LETTERS.length); });
  $('againBtn').addEventListener('click', function () { logEvent('кнопка «заново»'); loadLetter(state.idx); });
  $('homeBtn').addEventListener('click', function () { logEvent('кнопка «домой»'); loadLetter(0); });
  $('muteBtn').addEventListener('click', function () {
    state.muted = !state.muted;
    if (master) master.gain.value = state.muted ? 0 : 0.9;
    $('muteBtn').textContent = state.muted ? 'x' : '♪';
    logEvent('звук', state.muted ? 'выключен' : 'включён');
  });

  /* ---------- главный цикл ---------- */

  var prevT = now();
  var lastLoopT = -1;

  function loop(t) {
    if (t <= lastLoopT + 4) return;   // защита от двойного шага (rAF + таймер)
    lastLoopT = t;
    try {
      step(t);
    } catch (e) {
      window.loopError = e;   // для диагностики; игра продолжает работать
    }
  }

  function step(t) {
    var dt = Math.min(0.05, (t - prevT) / 1000);
    if (dt < 0) dt = 0;
    prevT = t;

    // «живые» подсказки ведутся из JS: работают даже без CSS-анимаций
    if (!state.interacted) {
      hintEl.style.marginLeft = (4 + 4 * Math.sin(t / 300)) + 'px';
      grabTip.style.top = (210 + 4 * Math.sin(t / 260)) + 'px';
    }

    // полотенце следует за курсором, пока держим; иначе лежит и «дышит»
    var idlePulse = 1;
    if (!state.dragging && !state.interacted) idlePulse = 1 + 0.05 * Math.sin(t / 220);
    if (state.dragging) {
      towel.px = towel.x; towel.py = towel.y;
      towel.x += (pointer.x - towel.x) * 0.5;
      towel.y += (pointer.y - towel.y) * 0.5;
      var vx = towel.x - towel.px;
      towel.rot += ((Math.max(-14, Math.min(14, vx * 1.1)) - towel.rot) * 0.25);
    } else {
      towel.rot *= 0.8;
    }
    var squish = state.dragging ? 0.94 : idlePulse;
    towelWrap.style.transform =
      'translate(' + (towel.x - CFG.TOWEL_W / 2) + 'px, ' + (towel.y - CFG.TOWEL_H / 2) + 'px) ' +
      'rotate(' + towel.rot + 'deg) scale(' + squish + ')';
    towelWrap.style.webkitTransform = towelWrap.style.transform;

    // стирание
    if (state.wiping && !state.won) {
      var f = toFoam(towel);
      if (lastWipe) {
        var dx = f.x - lastWipe.x, dy = f.y - lastWipe.y;
        var dist = Math.sqrt(dx * dx + dy * dy);
        var step = CFG.BRUSH * 0.3;
        var n = Math.max(1, Math.ceil(dist / step));
        if (n > 400) n = 400;                    // защита от «телепортов»
        for (var i = 1; i <= n; i++) {
          var x = lastWipe.x + dx * (i / n);
          var y = lastWipe.y + dy * (i / n);
          if (x > -CFG.BRUSH && y > -CFG.BRUSH &&
              x < state.letterW + CFG.BRUSH && y < state.letterH + CFG.BRUSH) {
            stamp(x, y);
          }
        }
        /* эффекты и звук — в отдельном try: если окружение чего-то не поддержало,
           проверка победы ниже всё равно выполнится */
        try {
          if (dist > 2 && Math.random() < 0.5) {
            var s = fromFoam(f);
            spawnBubble(s.x, s.y);
          }
          setWipeSound(true, dist);
        } catch (e) { /* не мешаем игре */ }
      }
      lastWipe = f;

      if (t - state.lastCheck > CFG.CHECK_MS) {
        state.lastCheck = t;
        try {
          checkWin(t);
        } catch (e2) {
          logEvent('ошибка проверки прогресса', String(e2));
        }
      }
    } else if (!state.wiping) {
      lastWipe = null;
    }

    drawParticles(dt);
  }

  /* ---------- старт ---------- */

  window.addEventListener('resize', fit);
  var raf = window.requestAnimationFrame ||
    window.webkitRequestAnimationFrame ||
    window.mozRequestAnimationFrame ||
    null;
  fit();
  logEvent('игра запущена', { букв_в_наборе: window.LETTERS ? window.LETTERS.length : 0 });
  loadLetter(0);

  /* Небольшой отладочный доступ из консоли браузера:
     WipeLetter.progress() — текущий прогресс, WipeLetter.state — состояние. */
  window.WipeLetter = {
    state: state,
    progress: function () { return state.progress; },
    report: report,
    foamLeft: foamLeft,
    gridProgress: gridProgress,
    check: function () { return checkWin(now()); },
    reload: loadLetter
  };
  // цикл живёт и на rAF, и на таймере: если rAF в окружении останавливается
  // (скрытая вкладка, ограниченный WebView), игра всё равно работает;
  // каждый драйвер перепланирует себя сам, дубли гасит защита в loop()
  if (raf) {
    var rafTick = function (t) { loop(t); raf(rafTick); };
    raf(rafTick);
  }
  setInterval(function () { loop(now()); }, 16);
})();
