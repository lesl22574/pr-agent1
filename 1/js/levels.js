// Уровни режима «Изучение букв».
// Один уровень = одна буква. На каждом уровне показывается картинка предмета и буква.
//
// Поля:
//   letter — буква (заглавная)
//   word   — название предмета (используется в озвучке и подписи alt)
//   emoji  — временная картинка (показывается, пока нет файла image)
//   image  — файл картинки (замените на финальную графику)
//   sound  — файл озвучки «Предмет. Буква ...» (если файла нет — используется синтез речи)
window.LEVELS = [
  { letter: 'А', word: 'ананас',    emoji: '🍉', image: 'assets/img/ananas.png',    sound: 'assets/audio/ananas_l.mp3' },
  { letter: 'Б', word: 'белка',    emoji: '🍌', image: 'assets/img/belka.png',    sound: 'assets/audio/belka_l.mp3' },
  { letter: 'В', word: 'волк', emoji: '🍇', image: 'assets/img/volk.png', sound: 'assets/audio/v1_l.mp3' },
  { letter: 'Г', word: 'гриб',     emoji: '🍄', image: 'assets/img/grib.png',     sound: 'assets/audio/x_l.mp3' },
  { letter: 'Д', word: 'дом',      emoji: '🏠', image: 'assets/img/dom.png',      sound: 'assets/audio/xx_l.mp3' },
  { letter: 'Е', word: 'ель',     emoji: '🦔', image: 'assets/img/el.png',    sound: 'assets/audio/e_l.mp3' },
  { letter: 'Ё', word: 'ёлка',     emoji: '🎄', image: 'assets/img/eg.png',    sound: 'assets/audio/yo_l.mp3' },
  { letter: 'Ж', word: 'жираф',    emoji: '🦒', image: 'assets/img/gi.png',   sound: 'assets/audio/xxx_l.mp3' },
  { letter: 'З', word: 'заяц',    emoji: '🐰', image: 'assets/img/za.png',    sound: 'assets/audio/z_l.mp3' },
  { letter: 'И', word: 'игла',    emoji: '🦃', image: 'assets/img/igla.png',   sound: 'assets/audio/i_l.mp3' },
  { letter: 'Й', word: 'йогурт',   emoji: '🥛', image: 'assets/img/yog.png',   sound: 'assets/audio/yi_l.mp3' },
  { letter: 'К', word: 'кошка',    emoji: '🐱', image: 'assets/img/ko.png',   sound: 'assets/audio/k_l.mp3' },
  { letter: 'Л', word: 'лук',    emoji: '🍋', image: 'assets/img/luk.png',    sound: 'assets/audio/l_l.mp3' },
  { letter: 'М', word: 'мышь',      emoji: '⚽', image: 'assets/img/mish.png',    sound: 'assets/audio/m_l.mp3' },
  { letter: 'Н', word: 'нос',      emoji: '👃', image: 'assets/img/nos.png',      sound: 'assets/audio/nos.mp3' },
  { letter: 'О', word: 'облако',   emoji: '☁️', image: 'assets/img/oblako.png',   sound: 'assets/audio/oblako.mp3' },
  { letter: 'П', word: 'петух',    emoji: '🐓', image: 'assets/img/petuh.png',    sound: 'assets/audio/petuh.mp3' },
  { letter: 'Р', word: 'рыба',     emoji: '🐟', image: 'assets/img/ryba.png',     sound: 'assets/audio/ryba.mp3' },
  { letter: 'С', word: 'слон',     emoji: '🐘', image: 'assets/img/slon.png',     sound: 'assets/audio/slon.mp3' },
  { letter: 'Т', word: 'тигр',     emoji: '🐯', image: 'assets/img/tigr.png',     sound: 'assets/audio/tigr.mp3' },
  { letter: 'У', word: 'утка',     emoji: '🦆', image: 'assets/img/utka.png',     sound: 'assets/audio/utka.mp3' },
  { letter: 'Ф', word: 'фонарь',   emoji: '🏮', image: 'assets/img/fonar.png',    sound: 'assets/audio/fonar.mp3' },
  { letter: 'Х', word: 'хлеб',     emoji: '🍞', image: 'assets/img/hleb.png',     sound: 'assets/audio/hleb.mp3' },
  { letter: 'Ц', word: 'цветок',   emoji: '🌸', image: 'assets/img/cvetok.png',   sound: 'assets/audio/cvetok.mp3' },
  { letter: 'Ч', word: 'чашка',    emoji: '☕', image: 'assets/img/chashka.png',  sound: 'assets/audio/chashka.mp3' },
  { letter: 'Ш', word: 'шар',      emoji: '🎈', image: 'assets/img/shar.png',     sound: 'assets/audio/shar.mp3' },
  { letter: 'Щ', word: 'щенок',    emoji: '🐶', image: 'assets/img/shenok.png',   sound: 'assets/audio/shenok.mp3' },
  { letter: 'Э', word: 'эскимо',   emoji: '🍦', image: 'assets/img/eskimo.png',   sound: 'assets/audio/eskimo.mp3' },
  { letter: 'Ю', word: 'юла',      emoji: '🪀', image: 'assets/img/yula.png',     sound: 'assets/audio/yula.mp3' },
  { letter: 'Я', word: 'ягода',    emoji: '🍓', image: 'assets/img/yagoda.png',   sound: 'assets/audio/yagoda.mp3' }
];

// Слова для режима «Чтение».
// Буквы слова перемешиваются на верхнем ряду, игрок нажимает их по порядку.
// Озвучка: слово — assets/audio/words/<слово>.mp3, буквы — assets/audio/letters/<буква>.mp3
window.READ_WORDS = [
  { word: 'дом' },
  { word: 'кот' },
  { word: 'сок' },
  { word: 'мак' },
  { word: 'лук' },
  { word: 'мяч' },

  { word: 'ёлка' },
  { word: 'кошка' },
  { word: 'арбуз' },
  { word: 'тигр' },
  { word: 'ягода' }
];
