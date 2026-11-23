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
  document: { getElementById: element, addEventListener(type, fn) { documentEvents[type] = fn; } },
  window: { addEventListener(type, fn) { (windowEvents[type] ||= []).push(fn); } },
  localStorage: { getItem: k => storage.get(k), setItem: (k, v) => storage.set(k, v) },
  LEVELS: ['А', 'Б', 'В', 'Г'].map(letter => ({ letter })),
  READ_WORDS: [], Math: math, clearTimeout() {},
  cancelAnimationFrame() {}, setTimeout(fn) { timers.push(fn); },
});
let source = fs.readFileSync(path.join(__dirname, '../1/js/game.js'), 'utf8');
// Не запускаем Canvas/озвучку; тестируем реальные функции выбора и переходов.
source = source.slice(0, source.indexOf('  // ---------- Старт ----------')) + `
  renderLevel = function () {};
  window.test = { startLetters, advanceLevel, startWipe, randomLetterIndex, renderLetterPicture,
    index: () => levelIdx, initSdk, showInterstitial, playVoice, canSound,
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
  const spoken = [];
  let resumes = 0;
  context.window.speechSynthesis = {
    cancel() {}, resume() { resumes++; }, speak(utterance) { spoken.push(utterance.text); },
  };
  context.window.SpeechSynthesisUtterance = class { constructor(text) { this.text = text; } };
  context.Audio = class {
    addEventListener() {}
    pause() {}
    play() { return Promise.reject(new Error('play interrupted')); }
  };
  let callbacks;
  test.setSdk({ adv: { showFullscreenAdv(options) { callbacks = options.callbacks; } } });
  let completed = 0;
  for (const result of ['onClose', 'onError']) {
    test.showInterstitial(() => { completed++; test.playVoice(null, 'После рекламы'); });
    callbacks.onOpen();
    assert.equal(test.canSound(), false);
    assert.equal(element('menuMusic').playing, false);
    callbacks[result]();
    callbacks.onOpen(); // позднее событие не должно вновь выключить звук
    callbacks.onClose(); // повторное закрытие не вызывает продолжение дважды
    assert.equal(test.canSound(), true);
  }
  assert.equal(completed, 2);
  assert.equal(spoken.length, 2);
  assert.ok(resumes >= 2);
  test.playVoice('letter.mp3', 'Резервная озвучка');
  await Promise.resolve();
  assert.equal(spoken.at(-1), 'Резервная озвучка');
  // В рисовании озвучка и продолжение происходят только после рекламы.
  test.startWipe(false);
  let afterAd = 0;
  const beforeAdSpeech = spoken.length;
  context.window.azbukaWipeComplete({ char: 'А' }, () => { afterAd++; });
  callbacks.onOpen();
  assert.equal(afterAd, 0);
  assert.equal(spoken.length, beforeAdSpeech);
  callbacks.onClose();
  assert.equal(afterAd, 1);
  await Promise.resolve();
  assert.equal(spoken.at(-1), 'Буква А. Молодец!');
  const sdkEvents = {};
  context.window.YaGames = { init: async () => ({
    environment: { i18n: { lang: 'en' } },
    on(type, fn) { sdkEvents[type] = fn; },
    adv: { showFullscreenAdv(options) { callbacks = options.callbacks; } },
  }) };
  await test.initSdk();
  assert.equal(context.window.YANDEX_LANGUAGE, 'en');
  assert.equal(context.window.GAME_LANGUAGE, 'ru');
  // Проверяем пересечение причин паузы: SDK, реклама, видимость и фокус.
  let speechPauses = 0;
  context.window.speechSynthesis.pause = () => { speechPauses++; };
  const audio = [];
  context.Audio = class {
    constructor() { this.currentTime = 7; audio.push(this); }
    addEventListener() {}
    play() { this.playing = true; return Promise.resolve(); }
    pause() { this.playing = false; }
  };
  const wipePauses = [];
  element('wipe').contentWindow = { azbukaSetAudioPaused(value) { wipePauses.push(value); } };
  test.playVoice('a.mp3', 'А');
  const activeVoice = audio.at(-1);
  sdkEvents.game_api_pause();
  assert.equal(test.canSound(), false);
  assert.equal(activeVoice.playing, false);
  assert.equal(wipePauses.at(-1), true);
  assert.ok(speechPauses > 0);
  context.document.hidden = true;
  documentEvents.visibilitychange();
  sdkEvents.game_api_resume();
  assert.equal(activeVoice.playing, false, 'SDK resume не снимает паузу скрытой вкладки');
  context.document.hidden = false;
  documentEvents.visibilitychange();
  assert.equal(activeVoice.playing, true);
  assert.equal(activeVoice.currentTime, 7, 'позиция озвучки сохраняется');
  assert.equal(wipePauses.at(-1), false);
  let focused = true;
  context.document.hasFocus = () => focused;
  windowEvents.blur.forEach(fn => fn());
  timers.pop()();
  assert.equal(test.canSound(), true, 'фокус в iframe не выключает звук');
  focused = false;
  windowEvents.blur.forEach(fn => fn());
  timers.pop()();
  assert.equal(activeVoice.playing, false);
  focused = true;
  windowEvents.focus.forEach(fn => fn());
  assert.equal(activeVoice.playing, true);
  let adDone = 0;
  test.showInterstitial(() => { adDone++; });
  callbacks.onOpen();
  context.document.hidden = true;
  documentEvents.visibilitychange();
  sdkEvents.game_api_pause();
  callbacks.onClose();
  assert.equal(adDone, 0, 'рекламное продолжение ждёт активности');
  context.document.hidden = false;
  documentEvents.visibilitychange();
  assert.equal(activeVoice.playing, false, 'возврат вкладки не снимает паузу SDK');
  sdkEvents.game_api_resume();
  assert.equal(adDone, 1);
  assert.equal(activeVoice.playing, true);
  element('btnSound').handlers.click();
  sdkEvents.game_api_pause();
  sdkEvents.game_api_resume();
  assert.equal(test.canSound(), false, 'пользовательское отключение сохраняется');
  assert.equal(wipePauses.at(-1), true);
  element('btnSound').handlers.click();
  element('btnHome').handlers.click();
  element('menuMusic').currentTime = 12;
  context.document.hidden = true;
  documentEvents.visibilitychange();
  assert.equal(element('menuMusic').playing, false);
  context.document.hidden = false;
  documentEvents.visibilitychange();
  assert.equal(element('menuMusic').playing, true);
  assert.equal(element('menuMusic').currentTime, 12);
  console.log('WINDOW_SDK_AUDIO_PAUSE_OK');
  delete context.window.YaGames;
  await test.initSdk();
  assert.equal(context.window.YANDEX_LANGUAGE, 'ru');
  console.log('ADS_AUDIO_LANGUAGE_OK');
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
