// Проверка порядка букв и передачи настройки во встроенное занятие.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const elements = new Map();
function element(id) {
  if (!elements.has(id)) {
    const classes = new Set();
    elements.set(id, {
      checked: false, style: {}, handlers: {},
      play() { this.playing = true; return Promise.resolve(); },
      pause() { this.playing = false; },
      classList: {
        add: c => classes.add(c), remove: c => classes.delete(c),
        contains: c => classes.has(c),
        toggle(c, on) { if (on) classes.add(c); else classes.delete(c); },
      },
      addEventListener(type, fn) { this.handlers[type] = fn; },
      setAttribute(name, value) { this[name] = value; },
      removeAttribute(name) { delete this[name]; },
      querySelectorAll() { return []; },
      appendChild(child) { this.child = child; },
      remove() {},
      contains(target) { return target === this || target === element('btnHome'); },
    });
  }
  return elements.get(id);
}
const storage = new Map([['azbuka_progress_v1', '1']]);
let random = 0;
const math = Object.create(Math);
math.random = () => random;
const images = [];
const timers = [];
const windowEvents = {};
const documentEvents = {};
const context = vm.createContext({
  Image: class { constructor() { images.push(this); } },
  document: { createElement: () => element('created-' + Math.random()), getElementById: element, addEventListener(type, fn) { documentEvents[type] = fn; } },
  window: { addEventListener(type, fn) { (windowEvents[type] ||= []).push(fn); } },
  localStorage: { getItem: k => storage.get(k), setItem: (k, v) => storage.set(k, v) },
  LEVELS: ['А', 'Б', 'В', 'Г'].map(letter => ({ letter, word: letter, image: letter + '.png', sound: letter + '.mp3', letterSound: 'letter-' + letter + '.mp3' })),
  READ_WORDS: [{ word: 'а', sound: 'assets/audio/words/а.mp3' }], Math: math, clearTimeout() {},
  requestAnimationFrame() { return 1; }, cancelAnimationFrame() {}, setTimeout(fn) { timers.push(fn); },
});
let source = fs.readFileSync(path.join(__dirname, '../1/js/game.js'), 'utf8');
// Не запускаем Canvas/озвучку; тестируем реальные функции выбора и переходов.
source = source.slice(0, source.indexOf('  // ---------- Старт ----------')) + `
  const renderActualLevel = renderLevel;
  renderLevel = function () {};
  window.test = { startLetters, advanceLevel, startWipe, randomLetterIndex, renderLetterPicture,
    index: () => levelIdx, renderActualLevel, correct, startPop, startRead, wordFile, letterFile,
    wrong,
    wrongBalloon() { onBalloonTap({ target: false, ball: document.getElementById('testBall') }); },
    wrongRead() { onTileTap({ letter: '!', used: false, el: document.getElementById('testTile') }); },
    tapReadTarget() { onTileTap(read.tiles.find(tile => !tile.used && tile.letter === read.letters[read.pos])); },
    tapTarget() { onBalloonTap({ target: true, letter: pop.target, el: { remove() {} }, ball: document.getElementById("testBall") }); },
    initSdk, showInterstitial, playVoice, canSound,
    setSdk(sdk) { ysdk = sdk; },
    finishBalloons() { pop = { raf: 0 }; finishPop(); } };
})();`;
vm.runInContext(source, context);
const test = context.window.test;
const flag = element('treats');
flag.checked = true;
test.startLetters(false);
assert.equal(test.index(), 1, 'продолжение последовательного прогресса');
test.advanceLevel();
assert.equal(test.index(), 2);
assert.equal(storage.get('azbuka_progress_v1'), '2');
test.startLetters(true);
assert.equal(test.index(), 0);
test.startWipe(false);
assert.equal(element('wipe').src, 'wipe/scene.html#%D0%90&ordered');
flag.checked = false;
random = 0.99;
test.startLetters(false);
assert.equal(test.index(), 3, 'случайная стартовая буква');
random = 0;
test.advanceLevel();
assert.equal(test.index(), 0, 'случайная следующая буква');
assert.equal(storage.get('azbuka_progress_v1'), '0', 'случайная игра не меняет сохранение');
for (let previous = 0; previous < 4; previous++) {
  for (const value of [0, 0.25, 0.5, 0.99]) {
    random = value;
    const next = test.randomLetterIndex(4, previous);
    assert.notEqual(next, previous);
    assert.ok(next >= 0 && next < 4);
  }
}
random = 0.99;
test.startWipe(false);
assert.equal(element('wipe').src, 'wipe/scene.html#%D0%92&random');
test.startWipe(true);
assert.equal(element('wipe').src, 'wipe/scene.html#%D0%90&random');
assert.equal(context.window.azbukaWipe('next'), true);
assert.equal(test.index(), 3, 'возврат из полотенца к случайной букве');
assert.equal(element('wipe').src, undefined);
console.log('ORDER_OK');

// В рисовании доступна та же кнопка общего HUD, что и в изучении.
for (const lesson of [false, true]) {
  test.startWipe(lesson);
  assert.equal(element('hud').classList.contains('hidden'), false);
  assert.equal(element('wipe').classList.contains('hidden'), false);
  const savedProgress = storage.get('azbuka_progress_v1');
  element('btnHome').handlers.click();
  assert.equal(element('start').classList.contains('hidden'), false);
  assert.equal(element('wipe').classList.contains('hidden'), true);
  assert.equal(element('hud').classList.contains('hidden'), true);
  assert.equal(element('wipe').src, undefined, 'сцена выгружена при возврате');
  assert.equal(storage.get('azbuka_progress_v1'), savedProgress);
}
console.log('WIPE_HOME_OK');

// Полноэкранный фон меняется независимо от масштаба игрового поля.
test.startLetters(true);
assert.equal(element('bg').classList.contains('learning'), true);
assert.equal(element('menuMusic').playing, false);
test.startWipe(false);
assert.equal(element('bg').classList.contains('wiping'), true);
assert.equal(element('bg').classList.contains('learning'), false);
element('btnHome').handlers.click();
assert.equal(element('bg').classList.contains('wiping'), false);
assert.equal(element('bg').classList.contains('learning'), false);
assert.equal(element('menuMusic').playing, true);
console.log('BACKGROUND_MUSIC_OK');

// Буква видна до загрузки картинки и при отсутствии файла.
const picture = { letter: 'А', word: 'ананас', emoji: '🍍', image: 'a.png' };
test.renderLetterPicture(picture);
assert.equal(element('letter').classList.contains('hidden'), false);
images.at(-1).onload();
assert.equal(element('letter').classList.contains('hidden'), true);
assert.equal(element('pic').child, images.at(-1));
test.renderLetterPicture({ ...picture, letter: 'Б', image: 'missing.png' });
assert.equal(element('letter').classList.contains('hidden'), false);
const previousImage = images.at(-1);
test.renderLetterPicture({ ...picture, letter: 'В', image: undefined });
previousImage.onload();
assert.equal(element('letter').textContent, 'В');
assert.equal(element('letter').classList.contains('hidden'), false);
console.log('LETTER_PICTURE_OK');

// После шариков идёт следующая буква изучения, а не рисование.
flag.checked = true;
for (let index = 0; index < 3; index++) {
  storage.set('azbuka_progress_v1', String(index));
  test.startLetters(false);
  test.finishBalloons();
  assert.equal(timers.length, 1);
  timers.shift()();
  assert.equal(test.index(), index + 1);
  assert.equal(element('game').classList.contains('hidden'), false);
  assert.equal(element('wipe').classList.contains('hidden'), true);
}
assert.equal(element('menuMusic').volume, 0.5);
console.log('BALLOONS_NEXT_LETTER_OK');

const homeIcon = element('homeIcon');
homeIcon.complete = true;
homeIcon.naturalWidth = 128;
homeIcon.handlers.load();
assert.equal(element('btnHome').classList.contains('has-icon'), true);
homeIcon.naturalWidth = 0;
homeIcon.handlers.error();
assert.equal(element('btnHome').classList.contains('has-icon'), false);
console.log('HOME_ICON_OK');

(async () => {
  const audio = [];
  let synthesisCalls = 0;
  context.window.speechSynthesis = {
    cancel() {}, resume() {},
    speak() { synthesisCalls++; throw new Error('синтез не должен использоваться'); },
  };
  context.Audio = class {
    constructor(src) { this.src = src; this.handlers = {}; this.currentTime = 7; audio.push(this); }
    addEventListener(type, fn) { this.handlers[type] = fn; }
    play() { this.playing = true; return Promise.resolve(); }
    pause() { this.playing = false; }
  };
  const sdkEvents = {};
  let callbacks;
  context.window.YaGames = { init: async () => ({
    environment: { i18n: { lang: 'en' } },
    on(type, fn) { sdkEvents[type] = fn; },
    adv: { showFullscreenAdv(options) { callbacks = options.callbacks; } },
  }) };
  await test.initSdk();
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../1/js/settings.js'), 'utf8'), context);
  assert.equal(context.window.GAME_SETTINGS.speechFallback, false);
  assert.equal(context.window.GAME_LANGUAGE, 'ru');
  assert.equal(context.window.YANDEX_LANGUAGE, 'en');
  const lastFile = () => audio.at(-1).src;
  const gesture = () => windowEvents.pointerdown.forEach(fn => fn({ isTrusted: true, target: element('btnHome') }));
  const finishAd = () => {
    callbacks.onOpen(); sdkEvents.game_api_pause(); callbacks.onClose(); sdkEvents.game_api_resume();
  };
  test.startLetters(true);
  test.renderActualLevel();
  assert.equal(lastFile(), 'А.mp3', 'озвучка карточки сохраняется');
  test.wrong(element('testCard'));
  assert.equal(lastFile(), 'assets/audio/common/wrong.mp3');
  test.correct(element('testCard'));
  assert.equal(lastFile(), 'assets/audio/common/correct.mp3');
  finishAd();
  assert.equal(lastFile(), 'assets/audio/common/pop-start.mp3');
  test.tapTarget();
  assert.equal(lastFile(), 'letter-А.mp3');
  test.wrongBalloon();
  assert.equal(lastFile(), 'assets/audio/common/wrong.mp3');
  // У всех букв один и тот же вступительный файл, без инструкции карточек.
  context.window.GAME_SETTINGS.audio.popStart = 'custom-pop.mp3';
  test.startPop();
  assert.equal(lastFile(), 'custom-pop.mp3');
  delete context.window.GAME_SETTINGS.audio.popStart;
  delete context.LEVELS[0].letterSound;
  assert.equal(test.letterFile('А'), 'assets/audio/letters/А.mp3');
  assert.notEqual(test.letterFile('А'), context.LEVELS[0].sound);
  context.LEVELS[0].letterSound = 'letter-А.mp3';

  for (const previousMode of ['wipe', 'letters']) {
    element('btnHome').handlers.click();
    if (previousMode === 'wipe') {
      test.startWipe(false);
      element('wipe').contentWindow = {};
      context.window.azbukaWipeComplete({ char: 'А' }, () => {});
    } else {
      test.startLetters(true); test.correct(element('testCard'));
    }
    callbacks.onOpen(); sdkEvents.game_api_pause(); callbacks.onClose();
    element('btnHome').handlers.click();
    test.startRead(true);
    gesture();
    assert.equal(lastFile(), 'assets/audio/common/read-start.mp3');
    assert.equal(element('read').classList.contains('hidden'), false);
    sdkEvents.game_api_resume();
    assert.equal(element('read').classList.contains('hidden'), false);
    test.wrongRead();
    assert.equal(lastFile(), 'assets/audio/common/wrong.mp3');
    test.tapReadTarget();
    assert.equal(lastFile(), 'letter-А.mp3');
    const timersBeforeWord = timers.length;
    element('readLayer').child.handlers.transitionend();
    assert.equal(timers.length, timersBeforeWord, 'переход ждёт окончания записи слова');
    assert.equal(lastFile(), 'assets/audio/words/а.mp3', 'готовое слово читается только после сборки');
    audio.at(-1).handlers.ended();
    assert.equal(timers.length, timersBeforeWord + 1);
    element('readTiger').handlers.click();
    assert.equal(lastFile(), 'assets/audio/words/а.mp3');
  }
  context.READ_WORDS[0].sound = 'custom-word.mp3';
  assert.equal(test.wordFile('а'), 'custom-word.mp3');
  delete context.READ_WORDS[0].sound;
  assert.equal(test.wordFile('а'), 'assets/audio/words/а.mp3');
  assert.notEqual(test.wordFile('а'), context.LEVELS[0].sound);
  test.startWipe(false);
  const scene = element('wipe').contentWindow;
  for (const char of ['А', 'Б', 'В']) {
    context.window.azbukaWipeStarted(scene, { char });
    assert.equal(lastFile(), 'assets/audio/wipe-start.mp3');
  }
  let result = 0;
  context.window.azbukaWipeComplete({ char: 'А' }, () => result++);
  finishAd();
  assert.equal(result, 1);
  assert.equal(audio.at(-1).playing, false, 'после очистки нет речевой озвучки');

  // Отсутствующий файл не включает синтез, режимы не смешивают реплики.
  test.playVoice('missing.mp3', 'Не должно синтезироваться');
  audio.at(-1).handlers.error();
  assert.equal(synthesisCalls, 0);
  test.playVoice(null, 'Тоже без синтеза');
  assert.equal(synthesisCalls, 0);
  // Музыка и записи по-прежнему выключаются при потере активности.
  test.playVoice('pause-test.mp3', '');
  const record = audio.at(-1);
  sdkEvents.game_api_pause();
  assert.equal(record.playing, false);
  context.document.hidden = true; documentEvents.visibilitychange();
  sdkEvents.game_api_resume();
  assert.equal(record.playing, false);
  context.document.hidden = false; documentEvents.visibilitychange();
  assert.equal(record.playing, true);
  assert.equal(record.currentTime, 7);
  element('btnHome').handlers.click();
  assert.equal(element('menuMusic').playing, true);
  assert.equal(synthesisCalls, 0);
  console.log('RECORDED_AUDIO_MODES_ADS_AND_SETTINGS_OK');
})().catch(error => { console.error(error); process.exitCode = 1; });

context.window.GAME_SETTINGS = { showPlaceholders: false };
test.renderLetterPicture(picture);
assert.equal(element('pic').innerHTML, '');
assert.equal(element('letter').classList.contains('hidden'), true);
images.at(-1).onload();
assert.equal(element('pic').child, images.at(-1));
test.renderLetterPicture({ ...picture, image: undefined });
assert.equal(element('pic').innerHTML, '');
assert.equal(element('letter').classList.contains('hidden'), true);
context.window.GAME_SETTINGS.showPlaceholders = true;
test.renderLetterPicture(picture);
assert.ok(element('pic').innerHTML.includes('emoji'));
assert.equal(element('letter').classList.contains('hidden'), false);
console.log('PLACEHOLDERS_OPTION_OK');

// Нет emoji при ожидании; при отсутствии файлов остаётся настоящая буква задания.
context.window.GAME_SETTINGS.showPlaceholders = false;
test.renderLetterPicture({ ...picture, image: 'not-present.png' });
assert.equal(element('letter').classList.contains('hidden'), true);
images.at(-1).onerror();
assert.equal(images.at(-1).src, 'assets/img/cards/А.png');
images.at(-1).onerror();
assert.equal(element('letter').classList.contains('hidden'), false);
assert.equal(element('letter').textContent, 'А');
assert.equal(element('pic').innerHTML, '');
console.log('MISSING_ART_LETTER_OK');
