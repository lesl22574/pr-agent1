const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../1/js/background-preload.js'), 'utf8');
async function check(useIdle) {
  const tasks = [];
  const images = [];
  const window = { setTimeout(fn) { tasks.push(fn); } };
  if (useIdle) window.requestIdleCallback = (fn, options) => {
    assert.equal(options.timeout, 1500);
    tasks.push(fn);
  };
  vm.runInNewContext(source, { window, Promise, Image: class {
    constructor() { images.push(this); }
    decode() { this.decoded = true; return Promise.resolve(); }
  } });
  const first = window.preloadGameBackgrounds();
  assert.equal(window.preloadGameBackgrounds(), first);
  assert.equal(images.length, 0, 'меню не ждёт загрузки фонов');
  assert.equal(tasks.length, 1);
  tasks.shift()();
  assert.equal(images.length, 3);
  assert.deepEqual(images.map(image => image.src), [
    'assets/menu-bg.svg', 'assets/img/51.jpg', 'wipe/assets/bg.png'
  ]);
  for (const image of images) {
    assert.equal(image.fetchPriority, 'low');
    assert.equal(image.decoding, 'async');
  }
  images[0].onload();
  images[1].onerror(); // Нет 51.jpg: меню и игра продолжают работать.
  images[2].onload();
  assert.deepEqual(Array.from(await first), ['assets/menu-bg.svg', null, 'wipe/assets/bg.png']);
  assert.equal(images[0].decoded, true);
  assert.equal(images[2].decoded, true);
  assert.equal(window.preloadGameBackgrounds(), first);
  assert.equal(images.length, 3, 'повторное открытие меню не создаёт новые запросы');
}
(async () => {
  await check(true);
  await check(false);
  console.log('BACKGROUND_PRELOAD_OK');
})().catch(error => { console.error(error); process.exitCode = 1; });
