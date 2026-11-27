const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const events = {};
vm.runInNewContext(fs.readFileSync(path.join(root, '1/js/input-guard.js'), 'utf8'), {
  document: { addEventListener(type, handler, capture) {
    assert.equal(capture, true);
    events[type] = handler;
  } },
});
function fire(type, button) {
  const event = { button, prevented: false, stopped: false,
    preventDefault() { this.prevented = true; },
    stopPropagation() { this.stopped = true; } };
  events[type](event);
  return event;
}
assert.equal(fire('contextmenu', 2).prevented, true);
assert.equal(fire('contextmenu', 0).prevented, true, 'долгое касание без правой кнопки');
assert.equal(fire('contextmenu', undefined).prevented, true, 'клавиатурное меню');
for (const type of ['pointerdown', 'mousedown', 'auxclick']) {
  assert.equal(fire(type, 2).prevented, true);
  assert.equal(fire(type, 2).stopped, true);
  assert.equal(fire(type, 0).prevented, false, 'обычный клик и нажатие пальцем доступны');
}
assert.equal(fire('dragstart', 0).prevented, true);
assert.equal(fire('selectstart', 0).prevented, true);
assert.equal(events.touchstart, undefined, 'сенсорное управление не отменяется');
for (const page of ['1/index.html', '1/wipe/scene.html']) {
  const html = fs.readFileSync(path.join(root, page), 'utf8');
  assert.ok(html.includes('input-guard.js'));
  assert.ok(html.includes('input-guard.css'));
}
console.log('INPUT_GUARD_OK');
