# Media-to-ASCII Translator/Handler

![Java](https://img.shields.io/badge/java-23.0.2+-gold.svg)
![Java](https://img.shields.io/badge/javac-23.0.2+-gold.svg)
![Python](https://img.shields.io/badge/python-3.8+-blue.svg)
![OpenCV](https://img.shields.io/badge/OpenCV-4.x-green.svg)
![NumPy](https://img.shields.io/badge/NumPy-1.20+-blue.svg)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)

English | [Russian README](README.md)

Media-to-ASCII Translator/Handler is a local AI-assisted fork layer inspired by
the public repository
[stepanussaruran/ASCII-Video-Player](https://github.com/stepanussaruran/ASCII-Video-Player).

The original **ASCII Video Player v4** project plays videos as ASCII animation
inside a command-line interface by converting video frames into terminal text.
It supports fast black-and-white rendering and full-color output with a separate
processing thread for better playback performance.

This fork adds a separate tool layer around that idea. The most important
addition is a move beyond pure CLI usage: the project includes Python tools for
image/video conversion and a simple Java Swing GUI wrapper.

The fork can:

- convert photos into ASCII;
- save ASCII photos as `.png` and `.txt`;
- save ASCII videos as `.mp4`;
- print colored ANSI output in compatible terminals;
- boost brightness, contrast, saturation, and add a glow effect;
- run through a simple English Java Swing window with file selection, logs, a
  progress bar, and a stop button.

This project was planned and directed by the maintainer and implemented with
assistance from OpenAI Codex. Human review, publication choices, and project
responsibility remain with the repository maintainer.

## Fork Structure

```text
.
  ascii_media_tools.py           # main Python tool for photos/videos
  requirements.txt               # Python dependencies
  run_color_preview.ps1          # colored PowerShell preview helper
  LICENSE.md                     # MIT License for GitHub detection
  README.md                      # Russian README
  README-EN.md                   # English README
  licences/
    LICENSE-EN.md                # MIT license in English
    LICENSE-RU.md                # Russian translation
    NOTICE-EN.md                 # attribution and ethical notice in English
    NOTICE-RU.md                 # attribution and ethical notice in Russian
  java-swing/
    src/AsciiPhotoSwingApp.java  # Swing GUI
    build.ps1                    # JAR build script
    run_gui.bat                  # double-click GUI launcher on Windows
    run_gui.ps1                  # PowerShell GUI launcher
    dist/AsciiPhotoSwingApp.jar  # built GUI JAR
```

## Requirements

For Python tools:

```bash
python -m pip install -r requirements.txt
```

Dependencies:

- `opencv-python` - video reading and writing;
- `numpy` - pixel processing;
- `pillow` - image reading and ASCII image rendering.

For the GUI:

- Java Runtime for launching the app;
- JDK for rebuilding `AsciiPhotoSwingApp.jar`;
- Python with the dependencies above, because the GUI calls
  `ascii_media_tools.py` under the hood.

## Supported Formats

Format support ultimately depends on Pillow and OpenCV, but the fork's GUI and
media detector explicitly allow:

| Type | Extensions | Used by |
| :-- | :-- | :-- |
| Images | `.jpg`, `.jpeg`, `.png`, `.bmp`, `.webp`, `.tif`, `.tiff` | CLI `image`, Java Swing GUI |
| Videos | `.mp4`, `.avi`, `.mkv`, `.mov`, `.webm` | CLI `video`, Java Swing GUI |

Saved outputs:

| Output | Format |
| :-- | :-- |
| ASCII photo as rendered image | `.png` |
| ASCII photo as text | `.txt` |
| ASCII video | `.mp4` |
| ASCII video frames | a folder of `.txt` files |

If a file has a rare extension but Pillow/OpenCV can read it, the CLI may still
work when called directly. The GUI keeps the allowed list short and predictable
on purpose.

## Quick Start: Photos

Enter the cloned fork directory:

```powershell
cd "C:\<PATH WHERE IT WAS CLONED>\fork"
```

Print a colored ASCII photo in the terminal:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 120 --color --print
```

Save a colored ASCII image:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 160 --color --vivid --save-image "ascii_outputs\photo_ascii.png"
```

Save with the original pixel dimensions:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 240 --color --vivid --output-size 1920x1080 --save-image "ascii_outputs\photo_ascii_1920x1080.png"
```

Save ASCII text:

```powershell
python ascii_media_tools.py image "C:\<PATH TO YOUR PHOTO>\photo.jpg" --width 160 --save-text "ascii_outputs\photo_ascii.txt"
```

## Quick Start: Videos

Preview a colored ASCII video in the terminal:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 100 --color --preview
```

Save a colored ASCII video:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 120 --color --vivid --save-video "ascii_outputs\video_ascii.mp4"
```

Save faster by skipping frames:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 120 --color --vivid --skip 3 --save-video "ascii_outputs\video_fast.mp4"
```

Save with progress lines for GUI/log integrations:

```powershell
python ascii_media_tools.py video "C:\<PATH TO YOUR VIDEO>\video.mp4" --width 120 --color --vivid --progress --save-video "ascii_outputs\video_ascii.mp4"
```

## Main Parameters

| Parameter | Purpose |
| :-- | :-- |
| `image` / `video` | process a photo or video |
| `--width` | ASCII grid width in characters |
| `--height` | ASCII grid height in rows |
| `--color` | colored ANSI/rendered output |
| `--vivid` | stronger brightness, contrast, and saturation preset on top of bright defaults |
| `--brightness` | brightness multiplier, default `1.12` |
| `--contrast` | contrast multiplier, default `1.25` |
| `--saturation` | saturation multiplier, default `1.35` |
| `--gamma` | midtone brightening/darkening, default `1.12` |
| `--glow` | soft glow for saved PNG/MP4 outputs, default `0.6` |
| `--output-size WIDTHxHEIGHT` | final PNG/MP4 pixel size |
| `--save-image` | save PNG |
| `--save-video` | save MP4 |
| `--save-text` | save ASCII text |
| `--save-frames` | save video as text frame files |
| `--skip` | process every Nth video frame |
| `--max-frames` | limit frame count for testing |
| `--progress` | print `PROGRESS current total percent` lines |

## Java Swing GUI

The GUI is located in `java-swing/`.

Double-click launcher on Windows:

```text
java-swing\run_gui.bat
```

Run through PowerShell:

```powershell
cd "C:\<PATH WHERE IT WAS CLONED>\fork\java-swing"
.\run_gui.ps1
```

Build the JAR:

```powershell
cd "C:\<PATH WHERE IT WAS CLONED>\fork\java-swing"
.\build.ps1
```

The GUI includes:

- photo or video file selection;
- `Forge by width` processing through the selected ASCII width;
- `Forge at native size` processing through the source pixel dimensions;
- editable ASCII width;
- progress bar;
- log area;
- `Stop the forge` button for the active conversion process.

The interface stays intentionally simple: choose the media victim through the
`...` button, set `ASCII width`, then send the file into the forge.

## Natural Size

Natural size means that the final PNG/MP4 keeps a target pixel size such as
`1920x1080`.

It does not mean "one ASCII character per source pixel". ASCII detail is still
controlled by `--width`. This keeps the final file at the original pixel size
without turning the render into an extremely heavy grid of thousands of
characters.

## Colored Output

Terminal color uses ANSI 24-bit escape codes.

Recommended environments:

- Windows Terminal;
- PowerShell 7;
- VS Code terminal;
- iTerm2, Alacritty, and modern Linux terminals.

Older consoles may show escape codes as raw text or render slowly.

## Packaging And Platforms

The Python part is mostly cross-platform: Windows, Linux, and macOS should work
when Python and the required packages are installed.

The Java Swing GUI is also portable as a Java application, but the current
launchers `run_gui.bat` and `run_gui.ps1` are Windows/PowerShell-oriented.

`jpackage` can build an app image with a bundled Java runtime, but it does not
bundle Python, OpenCV, or Pillow. A fully standalone desktop release would need
an additional Python runtime packaging step.

## License

This repository uses the MIT License for the fork-layer code and documentation.

The root [`LICENSE.md`](LICENSE.md) is included for standard GitHub license
detection. Additional bilingual license files and attribution notices are stored
in [`licences/`](licences/):

- [`LICENSE-EN.md`](licences/LICENSE-EN.md) - MIT License in English;
- [`LICENSE-RU.md`](licences/LICENSE-RU.md) - Russian translation;
- [`NOTICE-EN.md`](licences/NOTICE-EN.md) - attribution and ethical notice in English;
- [`NOTICE-RU.md`](licences/NOTICE-RU.md) - attribution and ethical notice in Russian.

The license applies to code and documentation created specifically for this
fork layer. It does not relicense the original upstream project or any
third-party dependency.

## Credits

The maintainer first noticed the appeal of terminal-based ASCII video playback
through social media examples. The original idea and base video player are
credited to:

[stepanussaruran/ASCII-Video-Player](https://github.com/stepanussaruran/ASCII-Video-Player)

This fork keeps a visible connection to the original project while adding a
separate layer for photos, saved outputs, and a GUI.
