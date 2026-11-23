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
    initialAlpha: 1,
    lastCheck: 0
  };

  var pointer = { x: CFG.STAGE_W / 2, y: CFG.STAGE_H / 2 };
  var towel = { x: CFG.TOWEL_REST.x, y: CFG.TOWEL_REST.y, px: CFG.TOWEL_REST.x, py: CFG.TOWEL_REST.y, rot: 0 };
  var lastWipe = null;
  var wrapOrigin = { x: 0, y: 0 };
  var stageRect = null;
  var stageScale = 1;
  var particles = [];

  // Если фокус был внутри iframe, сообщаем родителю и об уходе в другое окно.
  function notifyWindowActivity() {
    try {
      if (window.parent && window.parent !== window && window.parent.azbukaWindowActivity) {
        window.parent.azbukaWindowActivity();
      }
    } catch (e) {}
  }
  window.addEventListener('blur', notifyWindowActivity);
  window.addEventListener('focus', notifyWindowActivity);

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
  }

  /* ---------- прогресс ---------- */

  var probe = document.createElement('canvas');
  probe.width = probe.height = 48;
  var pctx = probe.getContext('2d');

  function foamCoverage() {
    pctx.clearRect(0, 0, 48, 48);
    pctx.drawImage(foamCanvas, 0, 0, 48, 48);
    var d = pctx.getImageData(0, 0, 48, 48).data;
    var n = 0;
    for (var i = 3; i < d.length; i += 4) if (d[i] > 24) n++;
    return n;
  }

  /* ---------- загрузка букв ---------- */

  var imgs = [];
  (function () {
    for (var i = 0; i < window.LETTERS.length; i++) {
      (function (L) {
        var rec = { foam: new Image(), clean: new Image(), ready: false, left: 2 };
        var done = function () {
          rec.left -= 1;
          if (rec.left === 0) {
            rec.ready = true;
            if (rec.onReady) rec.onReady();
          }
        };
        rec.foam.onload = done;
        rec.clean.onload = done;
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
      state.initialAlpha = Math.max(1, foamCoverage());
      cacheRects();
    };
    if (rec.ready) apply();
    else { rec.onReady = function () { if (state.idx === i) apply(); }; }
  }

  /* ---------- звук (WebAudio с защитой для старых браузеров) ---------- */

  var AC = null, master = null, wipeGain = null;
  var adAudioPaused = false;
  window.azbukaSetAudioPaused = function (paused) {
    adAudioPaused = paused;
    if (master) master.gain.value = paused || state.muted ? 0 : 0.9;
    if (AC && paused && AC.suspend) {
      try { var suspended = AC.suspend(); if (suspended && suspended.catch) suspended.catch(function () {}); } catch (e) {}
    }
    if (!paused && AC) initAudio();
  };
  window.azbukaSetAdPaused = window.azbukaSetAudioPaused;

  function initAudio() {
    if (adAudioPaused) return;
    if (AC) {
      if ((AC.state === 'suspended' || AC.state === 'interrupted') && AC.resume) {
        try { var resumed = AC.resume(); if (resumed && resumed.catch) resumed.catch(function () {}); } catch (e) {}
      }
      return;
    }
    try {
      var Ctx = window.AudioContext || window.webkitAudioContext;
      if (!Ctx) return;
      AC = new Ctx();
      master = AC.createGain();
      master.gain.value = state.muted || adAudioPaused ? 0 : 0.9;
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
    wipeGain.gain.setTargetAtTime(t, AC.currentTime, 0.09);
  }

  function jingle() {
    if (!AC) return;
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

  function win() {
    state.won = true;
    state.wiping = false;
    state.dragging = false;
    removeClass(towelWrap, 'drag');
    setWipeSound(false, 0);
    foamCanvas.style.opacity = '0';
    addClass(letterWrap, 'bounce');
    var c = fromFoam({ x: state.letterW / 2, y: state.letterH / 2 });
    spawnSparks(c.x, c.y);
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
    dispatchCleaned({ char: L.char, index: state.idx, stars: stars, seconds: Math.round(secs) });

    var completed = false;
    function afterAd() {
      if (completed) return;
      completed = true;
      initAudio();
      jingle();
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
    // До закрытия рекламы не воспроизводим ни фанфару, ни озвучку.
    var bridge = null;
    try { if (window.parent && window.parent !== window) bridge = window.parent.azbukaWipeComplete; } catch (e) {}
    if (typeof bridge === 'function') {
      bridge({ char: L.char, index: state.idx }, afterAd);
    } else {
      afterAd();
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

  $('nextBtn').addEventListener('click', function () {
    if (window.parent && window.parent !== window && window.parent.azbukaWipe && window.parent.azbukaWipe('next')) return;
    var nextIndex = (state.idx + 1) % window.LETTERS.length;
    if (launchHash.indexOf('&random') !== -1 && window.LETTERS.length > 1) {
      nextIndex = Math.floor(Math.random() * (window.LETTERS.length - 1));
      if (nextIndex >= state.idx) nextIndex++;
    }
    loadLetter(nextIndex);
  });
  $('againBtn').addEventListener('click', function () { loadLetter(state.idx); });
  // Во встроенной сцене используется общая кнопка меню поверх iframe.
  if (window.parent && window.parent !== window) $('homeBtn').style.display = 'none';
  $('homeBtn').addEventListener('click', function () {
    if (window.parent && window.parent !== window && window.parent.azbukaWipe) window.parent.azbukaWipe('home');
    else window.location.href = '../index.html';
  });
  $('muteBtn').addEventListener('click', function () {
    state.muted = !state.muted;
    if (master) master.gain.value = state.muted || adAudioPaused ? 0 : 0.9;
    $('muteBtn').textContent = state.muted ? 'x' : '♪';
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
        if (dist > 2 && Math.random() < 0.5) {
          var s = fromFoam(f);
          spawnBubble(s.x, s.y);
        }
        setWipeSound(true, dist);
      }
      lastWipe = f;

      if (t - state.lastCheck > 120) {
        state.lastCheck = t;
        var left = foamCoverage();
        if (1 - left / state.initialAlpha >= CFG.WIN_AT) win();
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
  var launchHash = (window.location && window.location.hash) || '';
  var requestedChar = '';
  try { requestedChar = decodeURIComponent(launchHash.slice(1).split('&')[0]); } catch (e) {}
  var initialIndex = 0;
  for (var li = 0; li < window.LETTERS.length; li++) {
    if (window.LETTERS[li].char === requestedChar) initialIndex = li;
  }
  if (launchHash.indexOf('&muted') !== -1) $('muteBtn').click();
  loadLetter(initialIndex);
  // цикл живёт и на rAF, и на таймере: если rAF в окружении останавливается
  // (скрытая вкладка, ограниченный WebView), игра всё равно работает;
  // каждый драйвер перепланирует себя сам, дубли гасит защита в loop()
  if (raf) {
    var rafTick = function (t) { loop(t); raf(rafTick); };
    raf(rafTick);
  }
  setInterval(function () { loop(now()); }, 16);
})();
