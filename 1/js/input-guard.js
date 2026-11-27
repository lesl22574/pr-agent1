/* Общая защита игрового поля от контекстного меню и перетаскивания браузером.
   Подключается отдельно в главном документе и во встроенном рисовании. */
(function () {
  'use strict';
  function preventBrowserAction(event) {
    event.preventDefault();
  }
  function preventSecondaryButton(event) {
    if (typeof event.button === 'number' && event.button > 0) {
      event.preventDefault();
      event.stopPropagation();
    }
  }
  // contextmenu приходит и от правой кнопки, и от долгого касания на Android.
  document.addEventListener('contextmenu', preventBrowserAction, true);
  document.addEventListener('selectstart', preventBrowserAction, true);
  document.addEventListener('dragstart', preventBrowserAction, true);
  // Правая кнопка не должна одновременно выбирать букву или брать полотенце.
  document.addEventListener('pointerdown', preventSecondaryButton, true);
  document.addEventListener('mousedown', preventSecondaryButton, true);
  document.addEventListener('auxclick', preventSecondaryButton, true);
  // touchstart/touchend не отменяем: это сохранило обычные клики по кнопкам.
})();
