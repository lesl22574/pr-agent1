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
const context = vm.createContext({
  Image: class { constructor() { images.push(this); } },
  document: { getElementById: element, addEventListener() {} },
  window: { addEventListener() {} },
  localStorage: { getItem: k => storage.get(k), setItem: (k, v) => storage.set(k, v) },
  LEVELS: ['А', 'Б', 'В', 'Г'].map(letter => ({ letter })),
  READ_WORDS: [], Math: math, clearTimeout() {},
});
let source = fs.readFileSync(path.join(__dirname, '../1/js/game.js'), 'utf8');
// Не запускаем Canvas/озвучку; тестируем реальные функции выбора и переходов.
source = source.slice(0, source.indexOf('  // ---------- Старт ----------')) + `
  renderLevel = function () {};
  window.test = { startLetters, advanceLevel, startWipe, randomLetterIndex, renderLetterPicture,
    index: () => levelIdx };
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
