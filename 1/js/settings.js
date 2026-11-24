// Только изображения: false — не показывать заглушки; true — включить их.
// Речевой синтез и аудиофайлы не зависят от этой настройки.
// Настройки, заданные до этого файла, сохраняются.
window.GAME_SETTINGS = Object.assign({
  showPlaceholders: false,
  speechFallback: false, // Для Яндекс Игр используем готовые MP3, не синтез речи.
  audio: {}             // Необязательные переопределения общих записей (см. AUDIO_ASSETS.md).
}, window.GAME_SETTINGS || {});
