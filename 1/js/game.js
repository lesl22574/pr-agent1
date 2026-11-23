(function () {
  'use strict';

  const W = 1280, H = 720;
  const SAVE_KEY = 'azbuka_progress_v1';   // прогресс режима «Изучение букв»
  const READ_KEY = 'azbuka_read_v1';       // прогресс режима «Чтение»
  const CARD_X = [425, 585, 745];          // позиции карточек режима 1
  const BALLOON_COLORS = ['#ffb3d1', '#b3e5ff', '#c7f5a8', '#ffe39a', '#d9c4ff', '#ffc8a8'];
  const POP_TARGETS = 5;                   // нужных шариков в закреплении
  const POP_DISTRACTORS = 9;               // других шариков в закреплении
  const TILE = 110, GAP = 24;              // размеры плиток в режиме «Чтение»
  const TILE_Y = 470, SLOT_Y = 195;   // буквы внизу, слово собирается наверху

  const $ = (id) => document.getElementById(id);
  const ui = {
    stage: $('stage'),
    game: $('game'), pop: $('pop'), read: $('read'), hud: $('hud'),
    start: $('start'), end: $('end'), wipe: $('wipe'),
    count: $('count'), pic: $('pic'), letter: $('letter'), cards: $('cards'),
    popTarget: $('popTarget'),
    readCount: $('readCount'), readLayer: $('readLayer'), readTiger: $('readTiger'),
    settings: $('settings'), btnSound: $('btnSound'), btnReset: $('btnReset'),
    btnCloseSettings: $('btnCloseSettings'), btnSettings: $('btnSettings'),
    btnHome: $('btnHome'), btnPlay: $('btnPlay'), btnRead: $('btnRead'),
    btnRestart: $('btnRestart'), btnEndAgain: $('btnEndAgain'), endSub: $('endSub')
  };

  let selectedMode = 'letters';
  let wipeLesson = false;
  let ysdk = null;
  let mode = 'letters';    // 'letters' | 'read'
  let levelIdx = 0;        // текущая буква (режим 1)
  let wordIdx = 0;         // текущее слово (режим 2)
  let busy = false;        // блокировка ввода во время перехода/рекламы
  let soundOn = true;      // настройка игрока
  let adPause = false;     // пауза звука, пока открыта реклама
  let currentAudio = null;
  let pop = null;          // состояние закрепления
  let read = null;         // состояние текущего слова
  let readTimer = 0;

  // ---------- Масштабирование под окно ----------
  function fitStage() {
    const s = Math.min(window.innerWidth / W, window.innerHeight / H);
    ui.stage.style.transform = `translate(-50%, -50%) scale(${s})`;
  }
  window.addEventListener('resize', fitStage);

  // ---------- Прогресс ----------
  function loadIdx(key, len) {
    let v;
    try { v = parseInt(localStorage.getItem(key), 10); } catch (e) { return 0; }
    return (Number.isInteger(v) && v >= 0 && v < len) ? v : 0;
  }
  function saveIdx(key, v) {
    try { localStorage.setItem(key, String(v)); } catch (e) { /* нет доступа к хранилищу */ }
  }

  // ---------- Звук ----------
  function canSound() { return soundOn && !adPause; }

  function stopSound() {
    if (currentAudio) { currentAudio.pause(); currentAudio = null; }
    if ('speechSynthesis' in window) speechSynthesis.cancel();
  }

  function speak(text) {
    if (!canSound() || !('speechSynthesis' in window)) return;
    speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = 'ru-RU';
    u.rate = 0.9;
    u.pitch = 1.1;
    speechSynthesis.speak(u);
  }

  // Проигрывает файл; если файла нет — синтез речи с текстом fallbackText
  function playVoice(file, fallbackText) {
    stopSound();
    if (!canSound()) return;
    if (!file) { speak(fallbackText); return; }
    const a = new Audio(file);
    currentAudio = a;
    a.addEventListener('error', () => {
      if (currentAudio === a) { currentAudio = null; speak(fallbackText); }
    });
    a.play().catch(() => { /* файл не найден — обработает событие error */ });
  }

  const letterFile = (l) => `assets/audio/letters/${l}.mp3`;
  const wordFile = (w) => `assets/audio/words/${w}.mp3`;
  const cap = (s) => s.charAt(0).toUpperCase() + s.slice(1);

  // ---------- Яндекс Игры SDK ----------
  function initSdk() {
    const timeout = new Promise((resolve) => setTimeout(() => resolve(null), 4000));
    const init = (window.YaGames && YaGames.init)
      ? YaGames.init().then((s) => s).catch(() => null)
      : Promise.resolve(null);
    return Promise.race([init, timeout]).then((s) => { ysdk = s; });
  }

  function feature(name) {
    return (ysdk && ysdk.features && ysdk.features[name]) || null;
  }

  function gameplayStart() {
    try { const g = feature('GameplayAPI'); if (g) g.start(); } catch (e) {}
  }
  function gameplayStop() {
    try { const g = feature('GameplayAPI'); if (g) g.stop(); } catch (e) {}
  }

  // Межстраничная реклама. done() вызывается ровно один раз — после закрытия или ошибки.
  function showInterstitial(done) {
    let finished = false;
    const finish = () => {
      if (finished) return;
      finished = true;
      adPause = false;
      done();
    };
    if (!ysdk || !ysdk.adv) { finish(); return; }
    try {
      ysdk.adv.showFullscreenAdv({
        callbacks: {
          onOpen: () => { adPause = true; stopSound(); },
          onClose: () => finish(),
          onError: () => finish()
        }
      });
    } catch (e) {
      finish();
    }
  }

  // ---------- Экраны ----------
  function showScreen(screen) {
    [ui.game, ui.pop, ui.read, ui.start, ui.end, ui.wipe].forEach((el) => el.classList.toggle('hidden', el !== screen));
    const inGame = (screen === ui.game || screen === ui.pop || screen === ui.read);
    ui.hud.classList.toggle('hidden', !inGame);
    if (!inGame) ui.settings.classList.add('hidden');
  }

  function showStart() {
    ui.wipe.removeAttribute('src');
    wipeLesson = false;
    stopPop();
    stopRead();
    gameplayStop();
    stopSound();
    showScreen(ui.start);
    const p = loadIdx(SAVE_KEY, LEVELS.length);
    $('btnLaunch').textContent = p > 0 && selectedMode === 'letters' ? 'Продолжить' : 'Играть';
    ui.btnRestart.classList.toggle('hidden', p === 0);
  }

  function startLetters(fromZero) {
    stopPop();
    stopRead();
    mode = 'letters';
    if (fromZero) { levelIdx = 0; saveIdx(SAVE_KEY, 0); }
    else { levelIdx = loadIdx(SAVE_KEY, LEVELS.length); }
    busy = false;
    showScreen(ui.game);
    gameplayStart();
    renderLevel();
  }

  function startRead(fromZero) {
    stopPop();
    stopRead();
    mode = 'read';
    if (fromZero) { wordIdx = 0; saveIdx(READ_KEY, 0); }
    else { wordIdx = loadIdx(READ_KEY, READ_WORDS.length); }
    busy = false;
    showScreen(ui.read);
    gameplayStart();
    renderWord();
  }

  // ---------- Утилиты ----------
  function shuffle(arr) {
    const a = arr.slice();
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text !== undefined) e.textContent = text;
    return e;
  }

  // ---------- Режим 1: выбор карточки ----------
  function renderLevel() {
    const lv = LEVELS[levelIdx];
    showScreen(ui.game);
    ui.count.textContent = `Буква ${levelIdx + 1} из ${LEVELS.length}`;
    ui.letter.textContent = lv.letter;

    // картинка: сначала emoji-заглушка, потом файл (если он есть)
    ui.pic.innerHTML = `<span class="emoji">${lv.emoji}</span>`;
    const img = new Image();
    img.alt = lv.word;
    img.onload = () => {
      if (LEVELS[levelIdx] !== lv) return;
      ui.pic.innerHTML = '';
      ui.pic.appendChild(img);
    };
    img.src = lv.image;

    // карточки: правильная буква + 2 случайные другие
    const others = shuffle(LEVELS.map((l) => l.letter).filter((l) => l !== lv.letter)).slice(0, 2);
    const options = shuffle([lv.letter, ...others]);
    ui.cards.innerHTML = '';
    options.forEach((letter, i) => {
      const b = el('button', 'card', letter);
      b.style.left = CARD_X[i] + 'px';
      b.addEventListener('click', () => onCardClick(b, letter));
      ui.cards.appendChild(b);
      applyCardSkin(b, letter);
    });

    // озвучка: предмет и буква
    playVoice(lv.sound, `${cap(lv.word)}. Буква ${lv.letter}.`);
  }

  // ---------- Картинки карточек (необязательно) ----------
  // Ищем файл для буквы: assets/img/cards/<буква>.png|jpg|webp — картинка целиком заменяет карточку
  // (буква уже нарисована на картинке). Если его нет — ищем общий фон assets/img/cards/card.png|jpg|webp,
  // тогда буква выводится поверх. Если и его нет — обычная карточка.
  const CARD_EXTS = ['png', 'jpg', 'webp'];
  const cardCache = {};

  function probeImage(url) {
    return new Promise((resolve) => {
      const i = new Image();
      i.onload = () => resolve(url);
      i.onerror = () => resolve(null);
      i.src = url;
    });
  }

  async function probeAny(base) {
    for (const ext of CARD_EXTS) {
      const url = `${base}.${ext}`;
      const ok = await probeImage(url);
      if (ok) return url;
    }
    return null;
  }

  function cardSkin(letter) {
    if (!cardCache[letter]) {
      cardCache[letter] = (async () => {
        const full = await probeAny(`assets/img/cards/${letter}`);
        if (full) return { url: full, full: true };
        const bg = await probeAny('assets/img/cards/card');
        if (bg) return { url: bg, full: false };
        return null;
      })();
    }
    return cardCache[letter];
  }

  function applyCardSkin(btn, letter) {
    cardSkin(letter).then((skin) => {
      if (!skin || !btn.isConnected) return;
      btn.style.backgroundImage = `url("${skin.url}")`;
      btn.classList.add('skin');
      if (skin.full) btn.classList.add('full');
    });
  }

  function onCardClick(btn, letter) {
    if (busy) return;
    if (letter === LEVELS[levelIdx].letter) correct(btn);
    else wrong(btn);
  }

  function wrong(btn) {
    btn.classList.remove('wrong');
    void btn.offsetWidth; // перезапуск анимации
    btn.classList.add('wrong');
    playVoice(null, 'Попробуй ещё!');
  }

  function correct(btn) {
    busy = true;
    btn.classList.add('right');
    if ($('treats').checked) {
      ui.game.querySelector('.tiger').textContent = '🐯🍬';
      setTimeout(() => { ui.game.querySelector('.tiger').textContent = '🐯'; }, 1500);
    }
    ui.cards.querySelectorAll('.card').forEach((c) => { c.disabled = true; });
    playVoice(null, 'Молодец!');
    gameplayStop();

    // межстраничная реклама сразу после правильного ответа,
    // потом закрепление — лопаем шарики с этой буквой
    showInterstitial(() => {
      gameplayStart();
      startPop();
    });
  }

  // ---------- Закрепление: лопаем шарики ----------
  function startPop() {
    stopPop();
    const lv = LEVELS[levelIdx];
    busy = true;
    ui.popTarget.textContent = lv.letter;
    showScreen(ui.pop);

    const others = shuffle(LEVELS.map((l) => l.letter).filter((l) => l !== lv.letter));
    const queue = [
      ...Array(POP_TARGETS).fill(lv.letter),
      ...others.slice(0, POP_DISTRACTORS)
    ];

    pop = {
      target: lv.letter,
      queue: shuffle(queue),
      items: [],
      popped: 0,
      needed: POP_TARGETS,
      spawnAt: 0,
      last: null,
      raf: 0,
      done: false
    };

    playVoice(null, `Лопай шарики с буквой ${lv.letter}`);
    pop.raf = requestAnimationFrame(popFrame);
  }

  function stopPop() {
    if (!pop) return;
    cancelAnimationFrame(pop.raf);
    pop = null;
    ui.pop.querySelectorAll('.bWrap').forEach((e) => e.remove());
  }

  function spawnBalloon(letter, isTarget) {
    const wrap = el('div', 'bWrap');
    const ball = el('div', 'balloon', letter);
    ball.style.setProperty('--c', BALLOON_COLORS[Math.floor(Math.random() * BALLOON_COLORS.length)]);
    wrap.appendChild(ball);
    ui.pop.appendChild(wrap);

    const b = {
      el: wrap, ball, letter, target: isTarget, popped: false,
      x: 60 + Math.random() * (W - 240),
      y: H + 20,
      speed: 0.07 + Math.random() * 0.03,   // px в мс
      sway: Math.random() * Math.PI * 2
    };
    ball.addEventListener('pointerdown', (e) => { e.preventDefault(); onBalloonTap(b); });
    pop.items.push(b);
    return b;
  }

  function popFrame(t) {
    const p = pop;
    if (!p || p.done) return;

    const dt = (p.last === null) ? 16 : Math.min(64, t - p.last);
    p.last = t;

    if (p.queue.length && t >= p.spawnAt) {
      const letter = p.queue.shift();
      spawnBalloon(letter, letter === p.target);
      p.spawnAt = t + 900;
    }

    for (const b of p.items.slice()) {
      b.y -= b.speed * dt;
      const dx = Math.sin(t / 700 + b.sway) * 25;
      b.el.style.transform = `translate(${b.x + dx}px, ${b.y}px)`;
      if (b.y < -200) {
        // улетел за экран: убираем, а нужную букву возвращаем в очередь
        b.el.remove();
        p.items = p.items.filter((i) => i !== b);
        if (b.target && p.popped < p.needed) p.queue.push(p.target);
      }
    }

    p.raf = requestAnimationFrame(popFrame);
  }

  function onBalloonTap(b) {
    const p = pop;
    if (!p || p.done || b.popped) return;

    if (b.target) {
      b.popped = true;
      p.popped++;
      p.items = p.items.filter((i) => i !== b);
      b.ball.classList.add('pop');
      setTimeout(() => b.el.remove(), 260);
      playVoice(null, b.letter);
      if (p.popped >= p.needed) finishPop();
    } else {
      b.ball.classList.remove('wrong');
      void b.ball.offsetWidth;
      b.ball.classList.add('wrong');
      playVoice(null, 'Не эта буква');
    }
  }

  function finishPop() {
    const p = pop;
    p.done = true;
    cancelAnimationFrame(p.raf);
    playVoice(null, 'Молодец!');
    setTimeout(() => {
      if (pop !== p) return; // игрок ушёл в меню
      stopPop();
      if (['А', 'Б', 'В'].includes(LEVELS[levelIdx].letter)) startWipe(true);
      else advanceLevel();
    }, 1600);
  }

  function advanceLevel() {
    levelIdx++;
    if (levelIdx >= LEVELS.length) {
      levelIdx = 0;
      saveIdx(SAVE_KEY, 0);
      stopSound();
      gameplayStop();
      ui.endSub.textContent = 'Ты выучил все буквы!';
      showScreen(ui.end);
      return;
    }
    saveIdx(SAVE_KEY, levelIdx);
    showScreen(ui.game);
    renderLevel();
    busy = false;
  }

  // ---------- Режим 2: чтение слов ----------
  function slotX(n, i) {
    const total = n * TILE + (n - 1) * GAP;
    return (W - total) / 2 + i * (TILE + GAP);
  }

  function stopRead() {
    clearTimeout(readTimer);
    readTimer = 0;
    read = null;
    ui.readLayer.innerHTML = '';
  }

  function renderWord() {
    stopRead();
    const w = READ_WORDS[wordIdx];
    const letters = [...w.word.toUpperCase()];
    const n = letters.length;
    ui.readCount.textContent = `Слово ${wordIdx + 1} из ${READ_WORDS.length}`;

    // пустые места для букв (внизу, на доске)
    const slots = letters.map((_, i) => {
      const s = el('div', 'slot');
      s.style.left = slotX(n, i) + 'px';
      ui.readLayer.appendChild(s);
      return s;
    });

    // буквы слова перемешиваем (чтобы не совпадали с порядком слова)
    let order = shuffle(letters.map((_, i) => i));
    while (n > 1 && order.every((v, i) => v === i)) order = shuffle(order);

    const tiles = order.map((idx, pos) => {
      const t = { letter: letters[idx], x: slotX(n, pos), y: TILE_Y, used: false, el: null };
      t.el = el('div', 'tile', t.letter);
      t.el.style.left = t.x + 'px';
      t.el.style.top = t.y + 'px';
      t.el.addEventListener('click', () => onTileTap(t));
      ui.readLayer.appendChild(t.el);
      return t;
    });

    read = { word: w, letters, slots, tiles, pos: 0, busy: false };
    playVoice(wordFile(w.word), cap(w.word));
  }

  function onTileTap(t) {
    const r = read;
    if (!r || r.busy || t.used) return;

    const expected = r.letters[r.pos];
    if (t.letter !== expected) {
      t.el.classList.remove('wrong');
      void t.el.offsetWidth;
      t.el.classList.add('wrong');
      playVoice(null, 'Попробуй ещё!');
      return;
    }

    t.used = true;
    t.el.classList.add('used');
    const slotIdx = r.pos;
    r.pos++;
    r.busy = true;
    playVoice(letterFile(t.letter), t.letter);

    flyTile(t, slotIdx, r.letters.length, () => {
      if (read !== r) return;
      const s = r.slots[slotIdx];
      s.textContent = t.letter;
      s.classList.add('filled');
      r.busy = false;
      if (r.pos >= r.letters.length) finishWord(r);
    });
  }

  // Буква перелетает с верхнего ряда на своё место внизу
  function flyTile(t, slotIdx, n, done) {
    const f = el('div', 'tile flyer', t.letter);
    f.style.left = t.x + 'px';
    f.style.top = t.y + 'px';
    ui.readLayer.appendChild(f);

    const tx = slotX(n, slotIdx) - t.x;
    const ty = SLOT_Y - t.y;
    let fin = false;
    const end = () => {
      if (fin) return;
      fin = true;
      f.remove();
      done();
    };
    f.addEventListener('transitionend', end);
    setTimeout(end, 1000); // страховка, если transitionend не придёт
    requestAnimationFrame(() => requestAnimationFrame(() => {
      f.style.transform = `translate(${tx}px, ${ty}px)`;
    }));
  }

  function finishWord(r) {
    r.busy = true;
    playVoice(wordFile(r.word.word), cap(r.word.word));
    readTimer = setTimeout(() => {
      if (read !== r) return;
      nextWord();
    }, 2800);
  }

  function nextWord() {
    wordIdx++;
    if (wordIdx >= READ_WORDS.length) {
      wordIdx = 0;
      saveIdx(READ_KEY, 0);
      stopRead();
      stopSound();
      gameplayStop();
      ui.endSub.textContent = 'Ты прочитал все слова!';
      showScreen(ui.end);
      return;
    }
    saveIdx(READ_KEY, wordIdx);
    renderWord();
  }

  // ---------- Кнопки ----------
  function startWipe(lesson) {
    stopPop();
    stopRead();
    stopSound();
    wipeLesson = lesson;
    showScreen(ui.wipe);
    gameplayStart();
    const letter = lesson ? LEVELS[levelIdx].letter : 'А';
    ui.wipe.src = 'wipe/scene.html#' + encodeURIComponent(letter) + (soundOn ? '' : '&muted');
  }

  // Прямой колбэк работает и при открытии единого index.html с диска.
  window.azbukaWipe = function (action) {
    if (ui.wipe.classList.contains('hidden')) return;
    if (action === 'home') showStart();
    else if (action === 'next' && wipeLesson) {
      ui.wipe.removeAttribute('src');
      wipeLesson = false;
      advanceLevel();
    }
  };
  const choices = [[ui.btnPlay, 'letters'], [$('btnWipe'), 'wipe'], [ui.btnRead, 'read']];
  choices.forEach(([button, value]) => button.addEventListener('click', () => {
    selectedMode = value;
    choices.forEach(([item, key]) => {
      item.classList.toggle('selected', key === value);
      item.setAttribute('aria-pressed', String(key === value));
    });
    $('btnLaunch').textContent = value === 'letters' && loadIdx(SAVE_KEY, LEVELS.length) > 0 ? 'Продолжить' : 'Играть';
  }));
  $('btnLaunch').addEventListener('click', () => {
    if (selectedMode === 'wipe') startWipe(false);
    else if (selectedMode === 'read') startRead(false);
    else startLetters(false);
  });
  ui.btnRestart.addEventListener('click', () => startLetters(true));
  ui.btnEndAgain.addEventListener('click', () => {
    if (mode === 'read') startRead(true);
    else startLetters(true);
  });
  ui.btnHome.addEventListener('click', () => showStart());

  ui.readTiger.addEventListener('click', () => {
    if (read) playVoice(wordFile(read.word.word), cap(read.word.word));
  });

  ui.btnSettings.addEventListener('click', () => ui.settings.classList.toggle('hidden'));
  ui.btnCloseSettings.addEventListener('click', () => ui.settings.classList.add('hidden'));
  ui.btnSound.addEventListener('click', () => {
    soundOn = !soundOn;
    ui.btnSound.textContent = soundOn ? 'Звук: вкл' : 'Звук: выкл';
    if (!soundOn) stopSound();
  });
  ui.btnReset.addEventListener('click', () => {
    if (busy && mode === 'letters') return;
    ui.settings.classList.add('hidden');
    stopPop();
    if (mode === 'read') {
      wordIdx = 0;
      saveIdx(READ_KEY, 0);
      renderWord();
    } else {
      levelIdx = 0;
      saveIdx(SAVE_KEY, 0);
      renderLevel();
    }
  });

  // ---------- Старт ----------
  fitStage();
  showStart();
  initSdk().then(() => {
    try { const l = feature('LoadingAPI'); if (l) l.ready(); } catch (e) {}
  });
})();
