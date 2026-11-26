// Уровни режима «Изучение букв».
// Один уровень = одна буква. На каждом уровне показывается картинка предмета и буква.
//
// Поля:
//   letter — буква (заглавная)
//   word   — название предмета (используется в озвучке и подписи alt)
//   emoji  — временная картинка (показывается, пока нет файла image)
//   cardImage — файл готовой карточки с буквой; null — автоматический поиск
//   image  — файл картинки (замените на финальную графику)
//   letterSound — необязательная запись только буквы для шариков и чтения
//   sound  — файл озвучки «Предмет. Буква ...» (если файла нет — используется синтез речи)
window.LEVELS = [
  { letter: 'А', cardImage: null, letterSound: 'assets/audio/a_l.mp3', word: 'ананас',    emoji: '🍉', image: 'assets/img/ananas.png',    sound: 'assets/audio/ananas_l.mp3' },
  { letter: 'Б', cardImage: null, word: 'белка',    emoji: '🍌', image: 'assets/img/belka.png',    sound: 'assets/audio/belka_l.mp3' },
  { letter: 'В', cardImage: null, word: 'волк', emoji: '🍇', image: 'assets/img/volk.png', sound: 'assets/audio/v1_l.mp3' },
  { letter: 'Г', cardImage: null, word: 'гриб',     emoji: '🍄', image: 'assets/img/grib.png',     sound: 'assets/audio/x_l.mp3' },
  { letter: 'Д', cardImage: null, word: 'дом',      emoji: '🏠', image: 'assets/img/dom.png',      sound: 'assets/audio/xx_l.mp3' },
  { letter: 'Е', cardImage: null, word: 'ель',     emoji: '🦔', image: 'assets/img/el.png',    sound: 'assets/audio/e_l.mp3' },
  { letter: 'Ё', cardImage: null, word: 'ёлка',     emoji: '🎄', image: 'assets/img/eg.png',    sound: 'assets/audio/yo_l.mp3' },
  { letter: 'Ж', cardImage: null, word: 'жираф',    emoji: '🦒', image: 'assets/img/gi.png',   sound: 'assets/audio/xxx_l.mp3' },
  { letter: 'З', cardImage: null, word: 'заяц',    emoji: '🐰', image: 'assets/img/za.png',    sound: 'assets/audio/z_l.mp3' },
  { letter: 'И', cardImage: null, word: 'игла',    emoji: '🦃', image: 'assets/img/igla.png',   sound: 'assets/audio/i_l.mp3' },
  { letter: 'Й', cardImage: null, word: 'йогурт',   emoji: '🥛', image: 'assets/img/yog.png',   sound: 'assets/audio/yi_l.mp3' },
  { letter: 'К', cardImage: null, word: 'кошка',    emoji: '🐱', image: 'assets/img/ko.png',   sound: 'assets/audio/k_l.mp3' },
  { letter: 'Л', cardImage: null, word: 'лук',    emoji: '🍋', image: 'assets/img/luk.png',    sound: 'assets/audio/l_l.mp3' },
  { letter: 'М', cardImage: null, word: 'мышь',      emoji: '⚽', image: 'assets/img/mish.png',    sound: 'assets/audio/m_l.mp3' },
  { letter: 'Н', cardImage: null, word: 'нос',      emoji: '👃', image: 'assets/img/nos.png',      sound: null },
  { letter: 'О', cardImage: null, word: 'облако',   emoji: '☁️', image: 'assets/img/oblako.png',   sound: null },
  { letter: 'П', cardImage: null, word: 'петух',    emoji: '🐓', image: 'assets/img/petuh.png',    sound: null },
  { letter: 'Р', cardImage: null, word: 'рыба',     emoji: '🐟', image: 'assets/img/ryba.png',     sound: null },
  { letter: 'С', cardImage: null, word: 'слон',     emoji: '🐘', image: 'assets/img/slon.png',     sound: null },
  { letter: 'Т', cardImage: null, word: 'тигр',     emoji: '🐯', image: 'assets/img/tigr.png',     sound: null },
  { letter: 'У', cardImage: null, word: 'утка',     emoji: '🦆', image: 'assets/img/utka.png',     sound: null },
  { letter: 'Ф', cardImage: null, word: 'фонарь',   emoji: '🏮', image: 'assets/img/fonar.png',    sound: null },
  { letter: 'Х', cardImage: null, word: 'хлеб',     emoji: '🍞', image: 'assets/img/hleb.png',     sound: null },
  { letter: 'Ц', cardImage: null, word: 'цветок',   emoji: '🌸', image: 'assets/img/cvetok.png',   sound: null },
  { letter: 'Ч', cardImage: null, word: 'чашка',    emoji: '☕', image: 'assets/img/chashka.png',  sound: null },
  { letter: 'Ш', cardImage: null, word: 'шар',      emoji: '🎈', image: 'assets/img/shar.png',     sound: null },
  { letter: 'Щ', cardImage: null, word: 'щенок',    emoji: '🐶', image: 'assets/img/shenok.png',   sound: null },
  { letter: 'Э', cardImage: null, word: 'эскимо',   emoji: '🍦', image: 'assets/img/eskimo.png',   sound: null },
  { letter: 'Ю', cardImage: null, word: 'юла',      emoji: '🪀', image: 'assets/img/yula.png',     sound: null },
  { letter: 'Я', cardImage: null, word: 'ягода',    emoji: '🍓', image: 'assets/img/yagoda.png',   sound: null }
];

// Слова для режима «Чтение».
// Буквы слова перемешиваются на верхнем ряду, игрок нажимает их по порядку.
// Без sound запись берётся из assets/audio/words/<слово>.mp3.
// Измените поле sound у нужного слова, чтобы задать любой файл: 'assets/audio/words/<слово>.mp3'.
// Для отдельной буквы задайте letterSound у записи LEVELS.
window.READ_WORDS = [
  { word: 'дом', sound: 'assets/audio/words/дом.mp3' },
  { word: 'кот', sound: 'assets/audio/words/кот.mp3' },
  { word: 'сок', sound: 'assets/audio/words/сок.mp3' },
  { word: 'мак', sound: 'assets/audio/words/мак.mp3' },
  { word: 'лук', sound: 'assets/audio/words/лук.mp3' },
  { word: 'мяч', sound: 'assets/audio/words/мяч.mp3' },

  { word: 'ёлка', sound: 'assets/audio/words/ёлка.mp3' },
  { word: 'кошка', sound: 'assets/audio/words/кошка.mp3' },
  { word: 'арбуз', sound: 'assets/audio/words/арбуз.mp3' },
  { word: 'тигр', sound: 'assets/audio/words/тигр.mp3' },
  { word: 'ягода', sound: 'assets/audio/words/ягода.mp3' }
];
