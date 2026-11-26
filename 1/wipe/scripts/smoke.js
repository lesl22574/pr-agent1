/* Дымовой тест логики игры без браузера: stub-DOM + stub-canvas. */
const fs = require('fs');
const path = require('path');
const ROOT = path.join(__dirname, '..');

function makeCtx() {
  const grad = { addColorStop() {} };
  return {
    canvas: null,
    globalCompositeOperation: 'source-over',
    globalAlpha: 1,
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
    id,
    style: {},
    children: [],
    handlers: {},
    textContent: '',
    className: '',
    width: 0,
    height: 0,
    _html: '',
    addEventListener(t, f) { (this.handlers[t] = this.handlers[t] || []).push(f); },
    appendChild(c) { this.children.push(c); },
    setPointerCapture() {},
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
let rafQueue = [];
let Tnow = 0;
const winH = {};
global.window = {
  LETTERS: null,
  location: { hash: process.env.RANDOM_ORDER ? (process.env.DYNAMIC_LETTERS ? '#&random' : '#%D0%90&random') : '#&ordered', href: '' },
  requestAnimationFrame: (cb) => { rafQueue.push(cb); return rafQueue.length; },
  addEventListener: (t, f) => { (winH[t] = winH[t] || []).push(f); },
  dispatchEvent: (e) => { (winH[e.type] || []).forEach((f) => f(e)); return true; },
  CustomEvent: class { constructor(type, opts) { this.type = type; this.detail = opts && opts.detail; } },
  performance: { now: () => Tnow },
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
global.requestAnimationFrame = global.window.requestAnimationFrame;
const winFire = (t, e) => (winH[t] || []).forEach((f) => f(e));

if (process.env.RANDOM_ORDER) Math.random = () => 0.99;

eval(fs.readFileSync(path.join(ROOT, 'js', 'letters.js'), 'utf8'));
if (process.env.DYNAMIC_LETTERS) {
  window.LETTERS.push(
    { char: 'Г', foam: 'test/g_foam.png', clean: 'test/g_clean.png' },
    { char: 'Д', foam: 'test/d_foam.png', clean: 'test/d_clean.png' }
  );
}
const initialLetter = process.env.DYNAMIC_LETTERS && process.env.RANDOM_ORDER ? 'Д' : 'А';

let adContinuation;
let adCalls = 0;
if (process.env.AD_BRIDGE) {
  window.parent = { azbukaWipeComplete(detail, done) {
    if (detail.char !== initialLetter) throw new Error('буква для рекламы');
    adCalls++;
    adContinuation = done;
  } };
}
const startedLetters = [];
window.parent = window.parent || {};
window.parent.azbukaWipeStarted = function (scene, detail) {
  if (scene !== window) throw new Error('источник вступления');
  startedLetters.push(detail.char);
};
const gameSource = fs.readFileSync(path.join(ROOT, 'js', 'game.js'), 'utf8');
eval(gameSource.replace('function jingle() {', 'function jingle() { window.jingles = (window.jingles || 0) + 1;'));

global.window.onLetterCleaned = (d) => cleaned.push(['cb', d]);

let T = 0;
function frames(n) {
  for (let i = 0; i < n; i++) {
    T += 16;
    Tnow = T;
    const q = rafQueue; rafQueue = [];
    q.forEach((cb) => cb(T));
  }
}
const ev = (x, y) => ({ clientX: x, clientY: y, pointerId: 1, preventDefault() {}, stopPropagation() {} });

(async () => {
  await new Promise((r) => setTimeout(r, 20));   // догрузка Image-стабов
  frames(5);
  get('stage').fire('mousedown', ev(300, 600));  // мимо полотенца — ничего не происходит
  frames(2);
  if (has('letterWrap', 'bounce')) throw new Error('среагировало на клик мимо полотенца');
  get('stage').fire('mousedown', ev(365, 340));  // берём полотенце
  if (!has('towelWrap', 'drag')) throw new Error('полотенце не схвачено');
  frames(2);
  winFire('mousemove', ev(600, 300));
  frames(20);                                    // > 120 ms синт. времени => проверка прогресса => победа
  if (!has('letterWrap', 'bounce')) throw new Error('win() не сработал');
  if (get('foamCanvas').style.opacity !== '0') throw new Error('пена не скрыта');
  if (has('towelWrap', 'drag')) throw new Error('полотенце не отпущено после победы');
  if (cleaned.length !== 2) throw new Error('событие letter:cleaned и колбэк не вызваны');
  if (cleaned[0].char !== initialLetter || typeof cleaned[0].stars !== 'number') throw new Error('detail события некорректен');
  if (cleaned[1][0] !== 'cb' || cleaned[1][1].char !== initialLetter) throw new Error('onLetterCleaned не вызван');
  if (process.env.AD_BRIDGE) {
    if (adCalls !== 1 || window.jingles) throw new Error('звук до рекламы');
    if (!has('winOverlay', 'hidden')) throw new Error('результат до рекламы');
    adContinuation();
    adContinuation();
    if (window.jingles !== 1) throw new Error('звук после рекламы должен быть однократным');
  }
  winFire('mouseup', ev(600, 300));
  frames(10);
  await new Promise((r) => setTimeout(r, 800));  // победный оверлей через 700 мс
  if (has('winOverlay', 'hidden')) throw new Error('оверлей не показан');
  if (get('winStars').children.length !== 3) throw new Error('звёзды не построены');
  get('nextBtn').fire('click');
  frames(5);
  if (!get('cleanLetter').src.includes(process.env.RANDOM_ORDER ? (process.env.DYNAMIC_LETTERS ? 'g_clean' : 'v_clean') : 'b_clean')) throw new Error('следующая буква не загрузилась');
  const navigation = [];
  window.parent.azbukaWipe = (action) => { navigation.push(action); return false; };
  get('homeBtn').fire('click');
  get('nextBtn').fire('click');
  if (navigation.join(',') !== 'home,next') throw new Error('навигация общего меню');
  get('muteBtn').fire('click');
  get('againBtn').fire('click');
  frames(10);
  if (startedLetters[0] !== initialLetter || startedLetters.length < 3) throw new Error('вступление новой буквы и повтора');
  if (process.env.DYNAMIC_LETTERS) {
    for (let i = 0; i < window.LETTERS.length + 1; i++) {
      const previous = startedLetters.at(-1);
      get('nextBtn').fire('click');
      const current = startedLetters.at(-1);
      if (process.env.RANDOM_ORDER) {
        if (current === previous) throw new Error('повтор случайной буквы');
      } else {
        const index = window.LETTERS.findIndex(letter => letter.char === previous);
        if (current !== window.LETTERS[(index + 1) % window.LETTERS.length].char) throw new Error('расширенный набор');
      }
    }
    if (!startedLetters.includes('Г') || !startedLetters.includes('Д')) throw new Error('добавленные буквы не доступны');
    console.log('DYNAMIC_LETTERS_OK');
  }
  console.log('SMOKE_OK');
  process.exit(0);
})().catch((e) => { console.error('SMOKE_FAIL:', e); process.exit(1); });
