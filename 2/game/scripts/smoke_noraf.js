/* Тест окружения «rAF не работает» (скрытая вкладка / ограниченный WebView):
   requestAnimationFrame не вызывает колбэк никогда, игра должна жить на setInterval. */
const fs = require('fs');
const path = require('path');
const ROOT = path.join(__dirname, '..');

function makeCtx() {
  const grad = { addColorStop() {} };
  return {
    globalCompositeOperation: 'source-over', globalAlpha: 1,
    fillStyle: '', strokeStyle: '', lineWidth: 1, lineCap: '',
    setTransform() {}, clearRect() {}, fillRect() {}, drawImage() {},
    createRadialGradient: () => grad,
    getImageData: (x, y, w, h) => ({ data: new Uint8ClampedArray(w * h * 4) }),
    beginPath() {}, arc() {}, fill() {}, stroke() {}, save() {}, restore() {},
    translate() {}, rotate() {}, moveTo() {}, lineTo() {},
  };
}

function makeEl(id) {
  const el = {
    id, style: {}, children: [], handlers: {}, textContent: '', className: '',
    width: 0, height: 0, _html: '',
    addEventListener(t, f) { (this.handlers[t] = this.handlers[t] || []).push(f); },
    appendChild(c) { this.children.push(c); },
    getBoundingClientRect() {
      if (id === 'stage') return { left: 0, top: 0, width: 1280, height: 720 };
      if (id === 'letterWrap') return { left: 405, top: 125, width: 470, height: 470 };
      return { left: 0, top: 0, width: 100, height: 100 };
    },
    getContext() { this._ctx = this._ctx || makeCtx(); return this._ctx; },
    fire(t, e) { (this.handlers[t] || []).forEach((f) => f(e || { preventDefault() {}, stopPropagation() {} })); },
  };
  Object.defineProperty(el, 'innerHTML', {
    get: () => el._html,
    set: (v) => { el._html = v; if (v === '') el.children.length = 0; },
  });
  return el;
}

const registry = {};
const get = (id) => (registry[id] = registry[id] || makeEl(id));
const has = (id, c) => (' ' + get(id).className + ' ').indexOf(' ' + c + ' ') >= 0;

global.document = {
  getElementById: get,
  createElement: (tag) => makeEl('_' + tag + Math.random()),
};
let Tnow = 0;
const winH = {};
global.window = {
  LETTERS: null,
  requestAnimationFrame: () => 0,            // <-- никогда не вызывает колбэк
  addEventListener: (t, f) => { (winH[t] = winH[t] || []).push(f); },
  dispatchEvent: (e) => { (winH[e.type] || []).forEach((f) => f(e)); return true; },
  CustomEvent: class { constructor(type, opts) { this.type = type; this.detail = opts && opts.detail; } },
  performance: { now: () => Tnow },
  location: { href: 'http://test/index.html' },
};
const cleaned = [];
winH['letter:cleaned'] = [(e) => cleaned.push(e.detail)];
global.innerWidth = 1280;
global.innerHeight = 720;
global.Image = class {
  constructor() { this.naturalWidth = 528; this.naturalHeight = 596; }
  set src(v) { this._src = v; setTimeout(() => this.onload && this.onload(), 0); }
  get src() { return this._src; }
};
global.addEventListener = (t, f) => { (winH[t] = winH[t] || []).push(f); };
const winFire = (t, e) => (winH[t] || []).forEach((f) => f(e));

// синтетические часы: идут быстрее реального времени
setInterval(() => { Tnow += 16; }, 8);

eval(fs.readFileSync(path.join(ROOT, 'js', 'letters.js'), 'utf8'));
eval(fs.readFileSync(path.join(ROOT, 'js', 'game.js'), 'utf8'));
// режим «переход после очистки»: оверлея нет, игре указан URL
global.window.GAME_SETTINGS = { completeUrl: 'done.html?letter={char}&i={index}' };

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const ev = (x, y) => ({ clientX: x, clientY: y, preventDefault() {}, stopPropagation() {} });

(async () => {
  await sleep(60);
  const t0 = get('towelWrap').style.transform;
  await sleep(120);
  if (get('towelWrap').style.transform === t0) throw new Error('цикл стоит без rAF');
  get('stage').fire('mousedown', ev(365, 340));   // берём полотенце
  if (!has('towelWrap', 'drag')) throw new Error('полотенце не схвачено');
  winFire('mousemove', ev(620, 320));
  await sleep(400);                                // стирание и проверка прогресса
  if (!has('letterWrap', 'bounce')) throw new Error('win() не сработал без rAF');
  if (get('foamCanvas').style.opacity !== '0') throw new Error('пена не скрыта');
  winFire('mouseup', ev(620, 320));
  if (!cleaned.length || cleaned[0].char !== 'А') throw new Error('событие letter:cleaned не вызвано');
  await sleep(1700);                           // ждём WIN_DELAY_GO = 1400 мс
  if (!global.window.location || !/done\.html\?letter=%D0%90&i=0/.test(global.window.location.href)) {
    throw new Error('переход по completeUrl не выполнен: ' + (global.window.location && global.window.location.href));
  }
  if (!has('winOverlay', 'hidden')) throw new Error('оверлей показан несмотря на заданный URL');
  get('nextBtn').fire('click');
  await sleep(80);
  if (!get('cleanLetter').src.includes('b_clean')) throw new Error('следующая буква не загрузилась');
  if (window.loopError) throw new Error('ошибка в цикле: ' + window.loopError.message);
  console.log('NORAF_SMOKE_OK');
  process.exit(0);
})().catch((e) => { console.error('NORAF_SMOKE_FAIL:', e.message); process.exit(1); });
