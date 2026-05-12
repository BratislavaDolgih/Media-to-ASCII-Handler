# 🎬🌇 Media-to-ASCII Translator/Handler (fork of "ASCII Video Player v4 — Ultimate Edition")

![Java](https://img.shields.io/badge/java-23.0.2+-gold.svg)
![Java](https://img.shields.io/badge/javac-23.0.2+-gold.svg)
![Python](https://img.shields.io/badge/python-3.8+-blue.svg)
![OpenCV](https://img.shields.io/badge/OpenCV-4.x-green.svg)
![NumPy](https://img.shields.io/badge/NumPy-1.20+-blue.svg)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)

Русский | [English README](README-EN.md)

Локальный форк Media-to-ASCII Translator/Handler на базе публичного репозитория
[stepanussaruran/ASCII-Video-Player](https://github.com/stepanussaruran/ASCII-Video-Player).

Оригинальный проект **ASCII Video Player v4** умеет воспроизводить видео в виде ASCII-анимации прямо на базе CLI, который преобразует каждый кадр видео в терминальное отображение видео. Та версия поддерживает как сверхбыстрый чёрно-белый рендеринг, так и полноцветный режим, используя отдельный поток обработки для лучшей производительности.

Этот же форк добавляет (и слегка переосмысляет) отдельный слой инструментов. Самое важное — отступление от CLI-подхода к графическому интерфейсу на базе Java Swing API.

Форк умеет проводить:

- конвертацию фотографий в ASCII;
- сохранение ASCII-фото как `.png` (в разработке `.txt`);
- сохранение ASCII-видео как `.mp4`;
- цветной ANSI-вывод;
- усиление яркости, контраста, насыщенности и glow-эффект;
- простую Java Swing-обёртку с англоязычным UI, логом, прогресс-баром и остановкой обработки.

Проект был спланирован и направлен владельцем репозитория, а реализован при помощи OpenAI Codex. Человеческая проверка, решения о публикации и ответственность за репозиторий остаются за владельцем репозитория.

## Структура форка

```text
.
  ascii_media_tools.py           # основной Python-инструмент для фото/видео
  requirements.txt               # Python-зависимости
  run_color_preview.ps1          # цветной предпросмотр в PowerShell
  LICENSE.md                     # MIT License для GitHub-распознавания
  README.md                      # README на русском языке
  README-EN.md                   # README на английском языке
  licences/
    LICENSE-EN.md                # MIT License на английском языке
    LICENSE-RU.md                # русский перевод лицензии
    NOTICE-EN.md                 # атрибуция и этическое уведомление на английском
    NOTICE-RU.md                 # атрибуция и этическое уведомление на русском
  java-swing/
    src/AsciiPhotoSwingApp.java  # Swing GUI
    build.ps1                    # сборка JAR
    run_gui.bat                  # запуск GUI двойным кликом на Windows
    run_gui.ps1                  # запуск GUI через PowerShell
    dist/AsciiPhotoSwingApp.jar  # собранный GUI
```

## Требования

Для Python-инструментов:

```bash
python -m pip install -r requirements.txt
```

Зависимости:

- `opencv-python` - чтение и запись видео;
- `numpy` - обработка пикселей;
- `pillow` - чтение изображений и рендер ASCII-картинок.

Для GUI:

- Java Runtime для запуска;
- JDK для пересборки `AsciiPhotoSwingApp.jar`;
- Python с зависимостями выше, потому что GUI вызывает `ascii_media_tools.py`.

## Поддерживаемые форматы

Поддержка форматов зависит от Pillow и OpenCV, но в интерфейсе и детекторе форка сейчас явно разрешены:

| Тип | Расширения | Где используется |
| :-- | :-- | :-- |
| Изображения | `.jpg`, `.jpeg`, `.png`, `.bmp`, `.webp`, `.tif`, `.tiff` | CLI-команда `image`, Java Swing GUI |
| Видео | `.mp4`, `.avi`, `.mkv`, `.mov`, `.webm` | CLI-команда `video`, Java Swing GUI |

Сохранение результатов:

| Результат | Формат |
| :-- | :-- |
| ASCII-фото как изображение | `.png` |
| ASCII-фото как текст | `.txt` |
| ASCII-видео | `.mp4` |
| ASCII-кадры видео | набор `.txt` файлов |

Если файл имеет редкое расширение, но Pillow/OpenCV умеют его читать, CLI может сработать при прямом запуске. GUI намеренно держит список форматов коротким и понятным.

## Быстрый старт: фото

Перейди в папку склонированного форка:

```powershell
cd "C:\<PATH WHERE WAS IT CLONED>\fork"
```

Показать фотографию цветным ASCII в терминале:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 120 --color --print
```

Сохранить цветную ASCII-картинку:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 160 --color --vivid --save-image "ascii_outputs\photo_ascii.png"
```

Сохранить ASCII-текст:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 160 --save-text "ascii_outputs\photo_ascii.txt"
```

Сохранить в исходном пиксельном размере:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 240 --color --vivid --output-size 1920x1080 --save-image "ascii_outputs\photo_ascii_1920x1080.png"
```

## Быстрый старт: видео

Предпросмотр цветного ASCII-видео в терминале:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 100 --color --preview
```

Сохранить цветное ASCII-видео:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 120 --color --vivid --save-video "ascii_outputs\video_ascii.mp4"
```

Сохранить быстрее, пропуская кадры:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 120 --color --vivid --skip 3 --save-video "ascii_outputs\video_fast.mp4"
```

Сохранить с прогрессом для GUI/логов:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 120 --color --vivid --progress --save-video "ascii_outputs\video_ascii.mp4"
```

## Основные параметры

| Параметр | Для чего нужен |
| :-- | :-- |
| `image` / `video` | режим обработки фото или видео |
| `--width` | ширина ASCII-сетки в символах |
| `--height` | высота ASCII-сетки в строках |
| `--color` | цветной ANSI/рендер |
| `--vivid` | усиленный пресет яркости, контраста и насыщенности поверх ярких дефолтов |
| `--brightness` | множитель яркости, по умолчанию `1.12` |
| `--contrast` | множитель контраста, по умолчанию `1.25` |
| `--saturation` | множитель насыщенности, по умолчанию `1.35` |
| `--gamma` | осветление/затемнение средних тонов, по умолчанию `1.12` |
| `--glow` | мягкое свечение для сохранённых PNG/MP4, по умолчанию `0.6` |
| `--output-size WIDTHxHEIGHT` | итоговый размер PNG/MP4 в пикселях |
| `--save-image` | сохранить PNG |
| `--save-video` | сохранить MP4 |
| `--save-text` | сохранить ASCII-текст |
| `--save-frames` | сохранить видео как набор текстовых кадров |
| `--skip` | обрабатывать каждый N-й кадр видео |
| `--max-frames` | ограничить число кадров для теста |
| `--progress` | печатать строки прогресса `PROGRESS current total percent` |

## Java Swing GUI

GUI находится в `java-swing/`.

Запуск двойным кликом на Windows:

```text
java-swing\run_gui.bat
```

Запуск через PowerShell:

```powershell
cd "C:\<PATH WHERE WAS IT CLONED>\fork\java-swing"
.\run_gui.ps1
```

Сборка JAR:

```powershell
cd "C:\<PATH WHERE WAS IT CLONED>\fork\java-swing"
.\build.ps1
```

В GUI есть:

- выбор фото или видео;
- кнопка `Forge by width` для обработки по заданной ASCII-ширине;
- кнопка `Forge at native size` для обработки в исходном пиксельном размере;
- поле ширины ASCII;
- прогресс-бар;
- лог;
- кнопка `Stop the forge` для остановки текущей обработки.

Интерфейс специально оставлен простым: выбрать “жертву” медиафайла через кнопку `...`, задать `ASCII width`, а затем отправить файл в forge.

## Про натуральный размер

Натуральный размер означает размер итогового PNG/MP4 в пикселях, например `1920x1080`.

Это не значит “один ASCII-символ на один пиксель”. Детализация ASCII по-прежнему задаётся через `--width`.
Такой подход сохраняет оригинальный размер файла, но не превращает рендер в слишком тяжёлую сетку из тысяч символов.

## Цветной вывод

Цвет в терминале работает через ANSI 24-bit escape codes.

Лучше всего использовать:

- Windows Terminal;
- PowerShell 7;
- терминал VS Code;
- iTerm2 / Alacritty / современные Linux-терминалы.

Старые консоли могут показывать escape-коды текстом или тормозить.

## Упаковка и платформы

Python-часть почти кроссплатформенная: Windows, Linux, macOS при наличии Python и зависимостей.

Java Swing GUI тоже переносимый как Java-приложение, но текущие launchers `run_gui.bat` и `run_gui.ps1` ориентированы на Windows/PowerShell.

`jpackage` может собрать приложение с Java runtime, но он не упаковывает Python и OpenCV/Pillow. Для полностью standalone-версии нужен отдельный шаг упаковки Python-runtime.

## Лицензия

Этот репозиторий использует MIT License для кода и документации fork-слоя.

Корневой [`LICENSE.md`](LICENSE.md) добавлен для стандартного GitHub-распознавания лицензии.
Дополнительные bilingual-файлы лицензии и уведомления об атрибуции лежат в [`licences/`](licences/):

- [`LICENSE-EN.md`](licences/LICENSE-EN.md) - MIT License на английском;
- [`LICENSE-RU.md`](licences/LICENSE-RU.md) - русский перевод лицензии;
- [`NOTICE-EN.md`](licences/NOTICE-EN.md) - атрибуция и этическое уведомление на английском;
- [`NOTICE-RU.md`](licences/NOTICE-RU.md) - атрибуция и этическое уведомление на русском.

Лицензия применяется к коду и документации, созданным именно для этого fork-слоя. Она не перелицензирует оригинальный upstream-проект и сторонние зависимости.

## Благодарности

На просторах TikTok автор заметил привлекательную идею терминального просмотра видео в ASCII. За оригинальную идею и базовый видеоплеер отдельная благодарность:

[stepanussaruran/ASCII-Video-Player](https://github.com/stepanussaruran/ASCII-Video-Player)

Этот форк сохраняет связь с оригиналом, но добавляет отдельный слой для фото, сохранения результатов и GUI (+ отходит от CLI-подхода).
