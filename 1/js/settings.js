// Только изображения: false — не показывать заглушки; true — включить их.
// Речевой синтез и аудиофайлы не зависят от этой настройки.
// Настройки, заданные до этого файла, сохраняются.
window.GAME_SETTINGS = Object.assign({
  showPlaceholders: false,
  fireworksDuration: 4000, // Длительность салюта после верной карточки, мс.
  speechFallback: false, // Для Яндекс Игр используем готовые MP3, не синтез речи.
  audio: {
    // Только выбор карточек в изучении букв (пути от 1/index.html).
    chooseCorrect: 'assets/audio/common/choose-correct.mp3',
    chooseWrong: 'assets/audio/common/choose-wrong.mp3',
    fireworks: 'assets/audio/common/fireworks.mp3'
    // Остальные записи настраиваются здесь же — см. AUDIO_ASSETS.md.
  }
}, window.GAME_SETTINGS || {});
