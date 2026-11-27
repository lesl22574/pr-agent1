// Проверка наличия записей без запуска браузера и без изменения файлов.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..', '1');
const context = vm.createContext({ window: {} });
for (const file of ['settings.js', 'levels.js']) {
  vm.runInContext(fs.readFileSync(path.join(root, 'js', file), 'utf8'), context);
}
const { GAME_SETTINGS: settings, LEVELS: levels, READ_WORDS: words } = context.window;
const defaults = {
  popStart: 'assets/audio/common/pop-start.mp3', readStart: 'assets/audio/common/read-start.mp3',
  wipeStart: 'assets/audio/wipe-start.mp3', wrong: 'assets/audio/common/wrong.mp3',
  fireworks: 'assets/audio/common/fireworks.mp3',
  chooseCorrect: 'assets/audio/common/choose-correct.mp3',
  chooseWrong: 'assets/audio/common/choose-wrong.mp3', chooseStart: 'assets/audio/common/choose-start.mp3',
};
const cues = Object.assign(defaults, settings.audio || {});
const required = new Set(Object.values(cues).filter(Boolean));
for (const level of levels) {
  if (level.sound) required.add(level.sound);
}
const letters = new Set(levels.map(level => level.letter.toUpperCase()));
for (const word of words) {
  required.add(word.sound || `assets/audio/words/${word.word.toLowerCase()}.mp3`);
  for (const letter of word.word.toUpperCase()) letters.add(letter);
}
for (const letter of letters) {
  const level = levels.find(level => level.letter.toUpperCase() === letter);
  required.add((level && level.letterSound) || `assets/audio/letters/${letter}.mp3`);
}
let missing = 0;
for (const file of [...required].sort()) {
  if (!fs.existsSync(path.join(root, file))) {
    console.log(`Добавьте: 1/${file}`);
    missing++;
  }
}
console.log(`Проверено файлов: ${required.size}; нужно добавить: ${missing}.`);
process.exitCode = missing ? 1 : 0;
