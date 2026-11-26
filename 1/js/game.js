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

  const AUDIO_CUES = {
    popStart: 'assets/audio/common/pop-start.mp3',
    readStart: 'assets/audio/common/read-start.mp3',
    wipeStart: 'assets/audio/wipe-start.mp3',
    wrong: 'assets/audio/common/wrong.mp3',
    chooseCorrect: 'assets/audio/common/choose-correct.mp3',
    chooseWrong: 'assets/audio/common/choose-wrong.mp3',
    chooseStart: 'assets/audio/common/choose-start.mp3'
  };
  const cueFile = (name) => {
    const custom = window.GAME_SETTINGS && window.GAME_SETTINGS.audio;
    return custom && Object.prototype.hasOwnProperty.call(custom, name) ? custom[name] : AUDIO_CUES[name];
  };

  const $ = (id) => document.getElementById(id);
  const ui = {
    stage: $('stage'), bg: $('bg'), menuMusic: $('menuMusic'),
    game: $('game'), pop: $('pop'), read: $('read'), hud: $('hud'),
    start: $('start'), end: $('end'), wipe: $('wipe'),
    count: $('count'), pic: $('pic'), letter: $('letter'), cards: $('cards'),
    popTarget: $('popTarget'),
    readCount: $('readCount'), readLayer: $('readLayer'),
    settings: $('settings'), btnSound: $('btnSound'), btnReset: $('btnReset'),
    btnCloseSettings: $('btnCloseSettings'), btnSettings: $('btnSettings'),
    btnHome: $('btnHome'), btnPlay: $('btnPlay'), btnRead: $('btnRead'),
    btnRestart: $('btnRestart'), btnEndAgain: $('btnEndAgain'), endSub: $('endSub')
  };

  let selectedMode = 'letters';
  let wipeLesson = false;
  let ysdk = null;
  // Игра одноязычная: язык SDK читается, интерфейс и озвучка — русские.
  window.GAME_LANGUAGE = 'ru';
  window.YANDEX_LANGUAGE = 'ru';
  let mode = 'letters';    // 'letters' | 'read'
  let levelIdx = 0;        // текущая буква (режим 1)
  let wordIdx = 0;         // текущее слово (режим 2)
  let busy = false;        // блокировка ввода во время перехода/рекламы
  let soundOn = true;      // настройка игрока
  let adPause = false;     // пауза звука, пока открыта реклама
  let currentAudio = null;
  let currentFallback = '';
  let currentUtterance = null;
  let pendingVoice = null;
  let sdkPaused = false;
  let windowInactive = false;
  let audioPaused = false;
  const deferredAdDone = [];
  let pop = null;          // состояние закрепления
  let read = null;         // состояние текущего слова
  let readTimer = 0;
  let pictureRequest = 0;
  let sceneSession = 0;

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
  function backgroundPaused() { return sdkPaused || windowInactive || !!document.hidden; }
  function canSound() { return soundOn && !adPause && !backgroundPaused(); }

  function syncAudioState() {
    const paused = !canSound();
    if (paused) {
      if (currentAudio) currentAudio.pause();
      // Не используем нативный pause(): в некоторых WebView после рекламы
      // он блокирует и все следующие реплики. Сохраняем текст и отменяем речь.
      if (currentUtterance) {
        const text = currentUtterance.text;
        currentUtterance = null;
        if (soundOn && !pendingVoice) pendingVoice = { file: null, text };
        if (window.speechSynthesis) window.speechSynthesis.cancel();
      }
    } else if (audioPaused) {
      if (currentAudio && !currentAudio.ended && !pendingVoice) {
        const audio = currentAudio;
        const attempt = audio.play();
        if (attempt && attempt.catch) attempt.catch((error) => {
          if (currentAudio === audio && error && error.name === 'NotAllowedError') {
            currentAudio = null;
            pendingVoice = audio.retryVoice;
            return;
          }
          if (currentAudio === audio && canSound()) {
            currentAudio = null;
            speak(currentFallback);
          }
        });
      }
    }
    audioPaused = paused;
    setWipeAdPaused(paused);
    updateMenuMusic();
    // Сначала продолжаем текущее задание после рекламы: оно может заменить
    // сохранённую реплику старого экрана своей новой инструкцией.
    if (!adPause && !backgroundPaused()) {
      while (deferredAdDone.length && !adPause && !backgroundPaused()) deferredAdDone.shift()();
    }
    if (!paused && pendingVoice) {
      const voice = pendingVoice;
      pendingVoice = null;
      playVoice(voice.file, voice.text, voice.onComplete);
    }
  }

  function stopSound() {
    pendingVoice = null;
    if (currentAudio) { currentAudio.pause(); currentAudio = null; }
    currentUtterance = null;
    if ('speechSynthesis' in window) window.speechSynthesis.cancel();
  }

  function speak(text) {
    if (window.GAME_SETTINGS && window.GAME_SETTINGS.speechFallback === false) return;
    if (!canSound()) { if (soundOn) pendingVoice = { file: null, text }; return; }
    if (!('speechSynthesis' in window)) return;
    const u = new window.SpeechSynthesisUtterance(text);
    u.lang = 'ru-RU';
    u.rate = 0.9;
    u.pitch = 1.1;
    // Сохраняем ссылку до окончания: WebView может преждевременно освободить
    // локальный объект реплики после перехода между режимами.
    currentUtterance = u;
    const session = sceneSession;
    u.onend = () => { if (currentUtterance === u) currentUtterance = null; };
    u.onerror = (event) => {
      if (currentUtterance !== u) return;
      currentUtterance = null;
      if (session === sceneSession && soundOn && event && event.error === 'not-allowed') {
        pendingVoice = { file: null, text };
      }
    };
    // Только восстанавливаем уже приостановленный внешним кодом движок.
    // Обычный запуск — как в исходной игре: новая реплика с lang=ru-RU.
    if (window.speechSynthesis.paused) window.speechSynthesis.resume();
    window.speechSynthesis.speak(u);
  }

  // Проигрывает файл; если файла нет — синтез речи с текстом fallbackText
  function playVoice(file, fallbackText, onComplete) {
    stopSound();
    if (!canSound()) { if (soundOn) pendingVoice = { file, text: fallbackText, onComplete }; return; }
    if (!file) { speak(fallbackText); return; }
    const a = new Audio(file);
    currentAudio = a;
    currentFallback = fallbackText;
    a.retryVoice = { file, text: fallbackText, onComplete };
    let completed = false;
    const finish = () => {
      if (completed) return;
      completed = true;
      if (onComplete) onComplete();
    };
    a.addEventListener('ended', () => {
      if (currentAudio !== a) return;
      currentAudio = null;
      finish();
    });
    a.addEventListener('error', () => {
      if (currentAudio === a) { currentAudio = null; speak(fallbackText); finish(); }
    });
    a.play().catch((error) => {
      if (currentAudio !== a) return;
      if (error && error.name === 'NotAllowedError') {
        currentAudio = null;
        pendingVoice = a.retryVoice;
        return; // Повторим по нажатию, не пропуская озвучку и уровень.
      }
      if (canSound()) { currentAudio = null; speak(fallbackText); finish(); }
    });
  }

  const letterFile = (letter) => {
    const level = LEVELS.find((item) => item.letter === letter.toUpperCase());
    return (level && level.letterSound) || `assets/audio/letters/${letter.toUpperCase()}.mp3`;
  };
  const wordFile = (word) => {
    const normalized = word.toLowerCase();
    const entry = READ_WORDS.find((item) => item.word.toLowerCase() === normalized);
    return (entry && entry.sound) || `assets/audio/words/${normalized}.mp3`;
  };
  const cap = (s) => s.charAt(0).toUpperCase() + s.slice(1);

  // ---------- Яндекс Игры SDK ----------
  function initSdk() {
    const timeout = new Promise((resolve) => setTimeout(() => resolve(null), 4000));
    const init = Promise.resolve().then(() => {
      return window.YaGames && window.YaGames.init ? window.YaGames.init() : null;
    }).then((sdk) => {
      ysdk = sdk;
      const language = sdk && sdk.environment && sdk.environment.i18n && sdk.environment.i18n.lang;
      window.YANDEX_LANGUAGE = language || 'ru';
      window.GAME_LANGUAGE = 'ru';
      if (document.documentElement) document.documentElement.lang = 'ru';
      if (sdk && typeof sdk.on === 'function') {
        sdk.on('game_api_pause', () => { sdkPaused = true; syncAudioState(); });
        sdk.on('game_api_resume', () => { sdkPaused = false; syncAudioState(); });
      }
      try { const loading = feature('LoadingAPI'); if (loading) loading.ready(); } catch (e) {}
      return sdk;
    }).catch(() => null);
    // Поздний ответ SDK тоже сохраняется, даже если ожидание загрузки истекло.
    return Promise.race([init, timeout]);
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
      deferredAdDone.push(done);
      syncAudioState();
    };
    if (!ysdk || !ysdk.adv) { finish(); return; }
    try {
      ysdk.adv.showFullscreenAdv({
        callbacks: {
          onOpen: () => {
            if (finished) return;
            adPause = true;
            syncAudioState();
          },
          onClose: () => finish(),
          onError: () => finish(),
          onOffline: () => finish()
        }
      });
    } catch (e) {
      finish();
    }
  }

  function setWipeAdPaused(paused) {
    try {
      const scene = ui.wipe.contentWindow;
      if (scene && scene.azbukaSetAudioPaused) scene.azbukaSetAudioPaused(paused);
      else if (scene && scene.azbukaSetAdPaused) scene.azbukaSetAdPaused(paused);
    } catch (e) { /* Сцена уже закрыта. */ }
  }

  // Вступление запускается после готовности изображений каждой новой буквы.
  window.azbukaWipeStarted = function (scene, detail) {
    if (ui.wipe.classList.contains('hidden') || ui.wipe.contentWindow !== scene) return;
    playVoice(cueFile('wipeStart'), `Возьми полотенце и вытри букву ${detail.char}.`);
  };

  // Реклама запускается родителем: SDK един для всех режимов игры.
  window.azbukaWipeComplete = function (detail, afterAd) {
    if (ui.wipe.classList.contains('hidden')) return;
    const scene = ui.wipe.contentWindow;
    const source = ui.wipe.src;
    const session = sceneSession;
    stopSound();
    gameplayStop();
    showInterstitial(() => {
      if (session !== sceneSession || ui.wipe.classList.contains('hidden') || ui.wipe.contentWindow !== scene || ui.wipe.src !== source) return;
      gameplayStart();
      afterAd();
    });
  };

  // Музыка звучит только в открытом меню; отказ автозапуска не мешает игре.
  function updateMenuMusic() {
    const music = ui.menuMusic;
    music.volume = 0.5;
    if (!ui.start.classList.contains('hidden') && canSound() && !document.hidden) {
      const attempt = music.play();
      if (attempt && attempt.catch) attempt.catch(() => {});
    } else {
      music.pause();
    }
  }
  function restoreOnInteraction(event) {
    if (!event || !event.isTrusted || document.hidden || adPause) return;
    // Реальное нажатие внутри игры подтверждает возвращение из рекламного
    // iframe, даже если WebView не прислал парное событие focus/resume.
    if (!ui.stage.contains(event.target)) return;
    windowInactive = false;
    sdkPaused = false;
    syncAudioState();
  }
  window.addEventListener('pointerdown', restoreOnInteraction, true);
  window.addEventListener('keydown', restoreOnInteraction, true);
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && typeof document.hasFocus === 'function') windowInactive = !document.hasFocus();
    syncAudioState();
  });
  function checkWindowActivity() {
    // Фокус внутри iframe рисования не означает переход в другое окно.
    setTimeout(() => {
      windowInactive = typeof document.hasFocus === 'function' ? !document.hasFocus() : document.activeElement !== ui.wipe;
      syncAudioState();
    }, 0);
  }
  window.azbukaWindowActivity = checkWindowActivity;
  window.addEventListener('blur', checkWindowActivity);
  window.addEventListener('focus', () => { windowInactive = false; syncAudioState(); });
  ui.wipe.addEventListener('load', syncAudioState);

  // ---------- Экраны ----------
  function showScreen(screen) {
    [ui.game, ui.pop, ui.read, ui.start, ui.end, ui.wipe].forEach((el) => el.classList.toggle('hidden', el !== screen));
    ui.bg.classList.toggle('learning', screen === ui.game || screen === ui.pop || screen === ui.read || (screen === ui.end && mode !== 'wipe'));
    ui.bg.classList.toggle('wiping', screen === ui.wipe);
    updateMenuMusic();
    const inGame = (screen === ui.game || screen === ui.pop || screen === ui.read || screen === ui.wipe);
    ui.hud.classList.toggle('hidden', !inGame);
    ui.btnSettings.classList.toggle('hidden', screen === ui.wipe);
    if (!inGame || screen === ui.wipe) ui.settings.classList.add('hidden');
  }

  function showStart() {
    sceneSession++;
    ui.wipe.removeAttribute('src');
    wipeLesson = false;
    stopPop();
    stopRead();
    gameplayStop();
    stopSound();
    showScreen(ui.start);
    const p = loadIdx(SAVE_KEY, LEVELS.length);
    $('btnLaunch').textContent = $('treats').checked && p > 0 && selectedMode === 'letters' ? 'Продолжить' : 'Играть';
    ui.btnRestart.classList.toggle('hidden', !$('treats').checked || p === 0);
  }

  function startLetters(fromZero) {
    sceneSession++;
    stopPop();
    stopRead();
    mode = 'letters';
    if (!$('treats').checked) levelIdx = randomLetterIndex(LEVELS.length);
    else if (fromZero) { levelIdx = 0; saveIdx(SAVE_KEY, 0); }
    else { levelIdx = loadIdx(SAVE_KEY, LEVELS.length); }
    busy = false;
    showScreen(ui.game);
    gameplayStart();
    renderLevel();
  }

  function startRead(fromZero) {
    sceneSession++;
    stopPop();
    stopRead();
    stopSound();
    mode = 'read';
    if (fromZero) { wordIdx = 0; saveIdx(READ_KEY, 0); }
    else { wordIdx = loadIdx(READ_KEY, READ_WORDS.length); }
    busy = false;
    showScreen(ui.read);
    gameplayStart();
    renderWord();
  }

  // Случайный выбор без повтора предыдущей буквы.
  function randomLetterIndex(length, previous) {
    if (length <= 1) return 0;
    if (previous === undefined) return Math.floor(Math.random() * length);
    const index = Math.floor(Math.random() * (length - 1));
    return index >= previous ? index + 1 : index;
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
    renderLetterPicture(lv);

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
    playVoice(lv.sound || cueFile('chooseStart'), `${cap(lv.word)}. Буква ${lv.letter}.`);
  }

  function placeholdersEnabled() {
    return !window.GAME_SETTINGS || window.GAME_SETTINGS.showPlaceholders !== false;
  }

  function renderLetterPicture(lv) {
    const request = ++pictureRequest;
    ui.letter.textContent = lv.letter;
    ui.letter.classList.toggle('hidden', !placeholdersEnabled());
    ui.pic.innerHTML = placeholdersEnabled() ? `<span class="emoji">${lv.emoji}</span>` : '';
    const showLetter = () => {
      if (request === pictureRequest) ui.letter.classList.remove('hidden');
    };
    const load = (url, fallback) => {
      const img = new Image();
      img.alt = lv.word;
      img.onload = () => {
        if (request !== pictureRequest) return;
        ui.pic.innerHTML = '';
        ui.pic.appendChild(img);
        ui.letter.classList.add('hidden');
      };
      img.onerror = () => {
        if (request !== pictureRequest) return;
        if (fallback) load(fallback, null);
        else showLetter();
      };
      img.src = url;
    };
    const card = `assets/img/cards/${lv.letter}.png`;
    if (lv.image) load(lv.image, card);
    else load(card, null);
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
    const level = LEVELS.find((item) => item.letter === letter);
    const custom = level && level.cardImage;
    const key = JSON.stringify([letter, custom || null]);
    if (!cardCache[key]) {
      cardCache[key] = (async () => {
        if (custom) {
          const url = await probeImage(custom);
          if (url) return { url, full: true };
        }
        const full = await probeAny(`assets/img/cards/${letter}`);
        if (full) return { url: full, full: true };
        if (!placeholdersEnabled()) return null;
        const bg = await probeAny('assets/img/cards/card');
        if (bg) return { url: bg, full: false };
        return null;
      })();
    }
    return cardCache[key];
  }

  function applyCardSkin(btn, letter) {
    if (!placeholdersEnabled()) btn.classList.add('asset-loading');
    cardSkin(letter).then((skin) => {
      btn.classList.remove('asset-loading');
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
    playVoice(cueFile('chooseWrong'), 'Попробуй ещё!');
  }

  function correct(btn) {
    busy = true;
    btn.classList.add('right');
    ui.cards.querySelectorAll('.card').forEach((c) => { c.disabled = true; });
    const session = sceneSession;
    const isCurrent = () => session === sceneSession && !ui.game.classList.contains('hidden');
    const afterFeedback = () => {
      if (isCurrent()) startPop();
    };
    stopSound();
    gameplayStop();
    showInterstitial(() => {
      if (!isCurrent()) return;
      gameplayStart();
      if (!soundOn || !cueFile('chooseCorrect')) { afterFeedback(); return; }
      playVoice(cueFile('chooseCorrect'), 'Молодец!', afterFeedback);
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

    playVoice(cueFile('popStart'), `Лопай шарики с буквой ${lv.letter}`);
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
      playVoice(letterFile(b.letter), b.letter);
      if (p.popped >= p.needed) finishPop();
    } else {
      b.ball.classList.remove('wrong');
      void b.ball.offsetWidth;
      b.ball.classList.add('wrong');
      playVoice(cueFile('wrong'), 'Попробуй ещё!');
    }
  }

  function finishPop() {
    const p = pop;
    p.done = true;
    cancelAnimationFrame(p.raf);
    setTimeout(() => {
      if (pop !== p) return; // игрок ушёл в меню
      stopPop();
      advanceLevel();
    }, 1600);
  }

  function advanceLevel() {
    if (!$('treats').checked) {
      levelIdx = randomLetterIndex(LEVELS.length, levelIdx);
      showScreen(ui.game);
      renderLevel();
      busy = false;
      return;
    }
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

    // Полупрозрачное слово-образец, закрываемое перенесёнными буквами.
    const slots = letters.map((letter, i) => {
      const s = el('div', 'slot', letter);
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
    playVoice(cueFile('readStart'), 'Составь слово из букв.');
  }

  function onTileTap(t) {
    const r = read;
    if (!r || r.busy || t.used) return;

    const expected = r.letters[r.pos];
    if (t.letter !== expected) {
      t.el.classList.remove('wrong');
      void t.el.offsetWidth;
      t.el.classList.add('wrong');
      playVoice(cueFile('wrong'), 'Попробуй ещё!');
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
    if (r.finished) return;
    r.finished = true;
    clearTimeout(readTimer);
    readTimer = 0;
    r.busy = true;
    const session = sceneSession;
    stopSound();
    gameplayStop();
    showInterstitial(() => {
      if (read !== r || session !== sceneSession || ui.read.classList.contains('hidden')) return;
      gameplayStart();
      const afterWord = () => {
        if (read !== r || session !== sceneSession) return;
        readTimer = setTimeout(() => {
          if (read === r && session === sceneSession) nextWord();
        }, 400);
      };
      if (!soundOn) { afterWord(); return; }
      playVoice(wordFile(r.word.word), cap(r.word.word), afterWord);
    });
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
    sceneSession++;
    stopPop();
    stopRead();
    stopSound();
    wipeLesson = lesson;
    showScreen(ui.wipe);
    gameplayStart();
    // Состав и размер набора известны самой сцене из wipe/js/letters.js.
    const letter = lesson ? LEVELS[levelIdx].letter : '';
    ui.wipe.src = 'wipe/scene.html#' + encodeURIComponent(letter) + ($('treats').checked ? '&ordered' : '&random') + (soundOn ? '' : '&muted');
  }

  // Прямой колбэк работает и при открытии единого index.html с диска.
  window.azbukaWipe = function (action) {
    if (ui.wipe.classList.contains('hidden')) return;
    if (action === 'home') showStart();
    else if (action === 'next' && wipeLesson) {
      ui.wipe.removeAttribute('src');
      wipeLesson = false;
      advanceLevel();
      return true;
    }
    return false;
  };
  const choices = [[ui.btnPlay, 'letters'], [$('btnWipe'), 'wipe'], [ui.btnRead, 'read']];
  choices.forEach(([button, value]) => button.addEventListener('click', () => {
    selectedMode = value;
    choices.forEach(([item, key]) => {
      item.classList.toggle('selected', key === value);
      item.setAttribute('aria-pressed', String(key === value));
    });
    $('btnLaunch').textContent = $('treats').checked && value === 'letters' && loadIdx(SAVE_KEY, LEVELS.length) > 0 ? 'Продолжить' : 'Играть';
  }));
  $('treats').addEventListener('change', showStart);
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
  const homeIcon = $('homeIcon');
  function updateHomeIcon() {
    ui.btnHome.classList.toggle('has-icon', homeIcon.complete && homeIcon.naturalWidth > 0);
  }
  homeIcon.addEventListener('load', updateHomeIcon);
  homeIcon.addEventListener('error', updateHomeIcon);
  updateHomeIcon();

  ui.btnSettings.addEventListener('click', () => ui.settings.classList.toggle('hidden'));
  ui.btnCloseSettings.addEventListener('click', () => ui.settings.classList.add('hidden'));
  ui.btnSound.addEventListener('click', () => {
    soundOn = !soundOn;
    ui.btnSound.textContent = soundOn ? 'Звук: вкл' : 'Звук: выкл';
    if (!soundOn) stopSound();
    syncAudioState();
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
      levelIdx = $('treats').checked ? 0 : randomLetterIndex(LEVELS.length);
      if ($('treats').checked) saveIdx(SAVE_KEY, 0);
      renderLevel();
    }
  });

  // ---------- Старт ----------
  fitStage();
  showStart();
  initSdk();
})();
