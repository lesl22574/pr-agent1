# Судоку — нативное Android-приложение

В репозитории две версии одной игры:

| Путь | Что находится внутри |
| --- | --- |
| `web/index.html` | исходная веб-игра на HTML + JavaScript, сохранена как эталон для переноса |
| `android/` | нативное приложение на **Kotlin + Jetpack Compose 1.0.5** (без WebView) |
| `index.html` | прежняя страница репозитория, оставлена без изменений |

## Что перенесено из веб-версии

- генерация головоломки с единственным решением: перестановки полос, стеков, строк, столбцов и цифр, затем аккуратное удаление клеток с проверкой уникальности;
- три уровня сложности (`Легко` 42, `Средне` 35, `Сложно` 29 данных цифр), нумерация уровней;
- таймер, прогресс, лимит из трёх неточностей, три подсказки;
- заметки (битовая маска 9 цифр), режим заметок, отмена ходов (до 100 шагов), стирание;
- подсветка строки/столбца/квадрата 3×3, одинаковых цифр и выбранной клетки, клавиатура с остатком цифр;
- окна результата, подтверждения новой игры и короткие подсказки;
- переключатель RU / EN прямо в приложении;
- сохранение и восстановление партии, языка и настроек через `SharedPreferences`;
- палитра и атмосфера оригинала: бумажный фон, бирюзовый акцент, коралловые детали, адаптивная сетка 9×9.

Веб-специфичные части (`YaGames.init`, `GameplayAPI`, `LoadingAPI`, `showFullscreenAdv`) заменены на нативные механизмы: жизненный цикл `Activity` останавливает таймер в паузе, а рекламу показывает Yandex Mobile Ads SDK для Android.

## Инструментарий сборки

Проект переведён на связку для Android Studio 2020.3.1 / 2021:

| Компонент | Версия |
| --- | --- |
| Android Studio | Arctic Fox 2020.3.1 / Bumblebee 2021.1.1 |
| Android Gradle Plugin | 7.0.4 |
| Gradle Wrapper | 7.0.2 |
| Kotlin | 1.5.31 |
| Java / `jvmTarget` | 11 |
| `compileSdk` / `targetSdk` | 31 |
| `minSdk` | 21 (Android 5.0) |
| Jetpack Compose | 1.0.5 (Material 2) |
| Compose Compiler | 1.0.5 — релиз, соответствующий Kotlin 1.5.31 |

Сборка использует классические Groovy-скрипты Gradle (`build.gradle`, `settings.gradle`) и `buildscript`-подключение плагинов — так проект открывается в старых версиях студии без сюрпризов. В коде учтены ограничения эпохи: вместо `enum.entries` применяется `values()`, вместо `collectAsStateWithLifecycle()` — `collectAsState()`, `LocalLifecycleOwner` импортируется из `androidx.compose.ui.platform`, вместо Material 3 используется Material 2 (`lightColors`, `ButtonDefaults.buttonColors(backgroundColor = ...)`), битовые операции идут через `java.lang.Integer`, история ходов — через `java.util.ArrayDeque`.

## Интеграция Yandex Mobile Ads SDK

Используется **Yandex Mobile Ads SDK 5.0.0** (`com.yandex.android:mobileads:5.0.0`). Этот релиз собран на Kotlin 1.5.10 и соответствует AGP 7.0 / `compileSdk 31`, поэтому хорошо подходит к выбранному инструментарию.

| Формат | Где | Как работает |
| --- | --- | --- |
| Баннер 320×50 | `ads/YandexStickyBanner.kt` | закреплён в `bottomBar` экрана, по центру, не перекрывает поле; `BannerAdView` через `AndroidView`, `AdSize.BANNER_320x50`, `setAdUnitId`, `AdRequest.Builder().build()` |
| Межстраничная реклама | `ads/AdsController.kt` | `InterstitialAd` с предварительной загрузкой после инициализации SDK, показ один раз на паузе между решённым уровнем и окном результата, после закрытия сразу готовится следующее объявление |

Детали поведения:

- SDK инициализируется вручную в `LittleLogicApp.onCreate()` через `MobileAds.initialize(...)`; реклама запрашивается после сигнала готовности из `AdsSdk`.
- Межстраничная реклама не показывается во время партии, при проигрыше, при смене сложности, при нажатии «Новая игра» или «Следующий уровень»; решение уровня — естественная пауза между уровнями.
- Если объявление ещё не загрузилось, закрылось или недоступно, игра сразу продолжается: результат уровня появляется без задержки.
- Все вызовы SDK выполняются на главном потоке; слушатели и объекты рекламы освобождаются при уничтожении экрана.
- Таймер останавливается, когда приложение уходит в фон или открывается полноэкранное объявление.

### Идентификаторы площадок

В `android/app/src/main/java/com/littlelogic/sudoku/ads/AdsConfig.kt` указаны демонстрационные площадки из сэмплов SDK 5.0.0:

```kotlin
const val BANNER_AD_UNIT_ID = "R-M-DEMO-320x50"
const val INTERSTITIAL_AD_UNIT_ID = "R-M-DEMO-interstitial"
```

Перед публикацией замените оба значения на реальные `R-M-...` идентификаторы из интерфейса Рекламной сети Яндекса. Настройки согласия пользователя, COPPA и геолокации при необходимости добавляются в `AdsSdk.initialize()` до вызова `MobileAds.initialize(...)`.

## Сборка

Понадобятся Android Studio 2020.3.1 / 2021, JDK 11 и Android SDK с `compileSdk 31`.

```bash
cd android
./gradlew :app:assembleDebug       # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest   # JVM-тесты генератора и правил игры
```

Откройте папку `android/` в Android Studio и запустите конфигурацию `app` на устройстве или эмуляторе.

## Структура проекта

```
android/
├── build.gradle, settings.gradle, gradle.properties, gradlew
├── app/build.gradle, app/proguard-rules.pro
├── app/src/main/AndroidManifest.xml
├── app/src/main/java/com/littlelogic/sudoku/
│   ├── MainActivity.kt                 # ComponentActivity + Compose
│   ├── LittleLogicApp.kt               # инициализация рекламного SDK
│   ├── ads/
│   │   ├── AdsConfig.kt                # идентификаторы площадок
│   │   ├── AdsSdk.kt                   # MobileAds.initialize + состояние готовности
│   │   ├── AdsController.kt            # межстраничная реклама между уровнями
│   │   └── YandexStickyBanner.kt       # баннер внизу экрана
│   ├── game/
│   │   ├── GameModels.kt               # состояние, сложность, язык, формат времени
│   │   ├── SudokuGenerator.kt          # генерация и подсчёт решений
│   │   ├── SudokuGame.kt               # чистые правила игры
│   │   ├── GameRepository.kt           # SharedPreferences + валидация
│   │   └── SudokuViewModel.kt          # состояние, таймер, события для UI
│   └── ui/
│       ├── Theme.kt, Strings.kt        # цвета Material 2 и RU/EN тексты
│       ├── SudokuApp.kt                # экран, диалоги, таймер, показы рекламы
│       └── GameComponents.kt           # поле, клавиатура, статистика, кнопки
└── app/src/test/java/.../game/         # JVM-тесты логики
```

## Проверки

В репозитории подготовлены модульные тесты на генерацию головоломок (валидность сетки, единственность решения, неизменяемость массива), подсчёт соседних клеток, формат времени и игровые правила (победа, три неточных хода, отмена, подсказки, заметки, новый уровень). Финальная сборка APK выполняется в Android Studio, где доступен Android SDK; в текущем окружении проверены синтаксис Kotlin и Gradle-скриптов, XML-ресурсы, состав зависимостей и алгоритм генерации отдельным прогоном.
