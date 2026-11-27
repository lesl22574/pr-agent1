/* Неблокирующая предзагрузка общих фонов. Пути — от 1/index.html. */
(function () {
  'use strict';
  var files = [
    'assets/menu-bg.svg',
    'assets/img/51.jpg', // Общий фон изучения букв, шариков и чтения.
    'wipe/assets/bg.png'
  ];
  var pending = null;
  var retained = []; // Сохраняем декодированные изображения между режимами.

  window.preloadGameBackgrounds = function () {
    if (pending) return pending;
    pending = new Promise(function (resolve) {
      function start() {
        var loads = files.map(function (url) {
          return new Promise(function (done) {
            var image = new Image();
            retained.push(image);
            image.decoding = 'async';
            image.fetchPriority = 'low';
            image.onload = function () {
              // Подготавливаем не только сеть/кеш, но и декодирование картинки.
              var decoded;
              try { decoded = image.decode ? image.decode() : null; } catch (e) {}
              Promise.resolve(decoded).catch(function () {}).then(function () { done(url); });
            };
            image.onerror = function () { done(null); };
            image.src = url;
          });
        });
        Promise.all(loads).then(resolve);
      }
      if (typeof window.requestIdleCallback === 'function') {
        window.requestIdleCallback(start, { timeout: 1500 });
      } else {
        window.setTimeout(start, 0);
      }
    });
    return pending;
  };
})();
