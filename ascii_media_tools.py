"""
ASCII media tools for images and videos.

This companion script keeps the original video player intact and adds:
  - terminal preview for images and videos
  - saving images as ASCII text and rendered image files
  - saving videos as rendered ASCII video files
  - optional colored ANSI terminal output and colored rendered files
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
from pathlib import Path
from typing import Iterable

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont


ASCII_CHARS = (
    " `.-':_,^=;><+!rc*/z?sLTv)J7(|Fi{C}fI31tlu"
    "[neoZ5Yxjya]2ESwqkP6h9d4VpOGbUAKXHm8RD#$Bg0MNWQ%&@"
)
_CHARS_ARRAY = np.array(list(ASCII_CHARS))

DEFAULT_BRIGHTNESS = 1.12
DEFAULT_CONTRAST = 1.25
DEFAULT_SATURATION = 1.35
DEFAULT_GAMMA = 1.12
DEFAULT_GLOW = 0.6

CURSOR_HOME = "\033[H"
CLEAR_SCREEN = "\033[2J"
HIDE_CURSOR = "\033[?25l"
SHOW_CURSOR = "\033[?25h"
RESET_COLOR = "\033[0m"


def enable_ansi_windows() -> None:
    if os.name != "nt":
        return
    try:
        import ctypes

        kernel32 = ctypes.windll.kernel32
        kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
    except Exception:
        pass


def parse_output_size(value: str | None) -> tuple[int, int] | None:
    if not value:
        return None
    match = re.fullmatch(r"(\d+)x(\d+)", value.strip().lower())
    if not match:
        raise argparse.ArgumentTypeError("Use WIDTHxHEIGHT, for example 1280x720.")
    width_px = int(match.group(1))
    height_px = int(match.group(2))
    if width_px < 1 or height_px < 1:
        raise argparse.ArgumentTypeError("Output size must be positive.")
    return width_px, height_px


def ensure_parent(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)


def derive_ascii_height(frame_shape: tuple[int, ...], width: int, height: int | None) -> int:
    if height is not None:
        return max(1, height)
    source_h, source_w = frame_shape[:2]
    return max(1, int(source_h * width / max(source_w, 1) / 2))


def require_cv2():
    try:
        import cv2
    except ModuleNotFoundError as exc:
        raise SystemExit(
            "Video operations require OpenCV. Install dependencies with: "
            "python -m pip install -r requirements.txt"
        ) from exc
    return cv2


def resize_rgb(frame_rgb: np.ndarray, size: tuple[int, int]) -> np.ndarray:
    resampling = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
    return np.array(Image.fromarray(frame_rgb).resize(size, resampling))


def enhance_frame_rgb(
    frame_rgb: np.ndarray,
    brightness: float = 1.0,
    contrast: float = 1.0,
    saturation: float = 1.0,
    gamma: float = 1.0,
) -> np.ndarray:
    image = Image.fromarray(frame_rgb)
    if brightness != 1.0:
        image = ImageEnhance.Brightness(image).enhance(brightness)
    if contrast != 1.0:
        image = ImageEnhance.Contrast(image).enhance(contrast)
    if saturation != 1.0:
        image = ImageEnhance.Color(image).enhance(saturation)

    enhanced = np.array(image).astype(np.float32)
    if gamma != 1.0:
        inv_gamma = 1.0 / gamma
        enhanced = 255.0 * np.power(np.clip(enhanced / 255.0, 0.0, 1.0), inv_gamma)
    return np.clip(enhanced, 0, 255).astype(np.uint8)


def frame_to_ascii_data(
    frame_rgb: np.ndarray,
    width: int,
    height: int | None = None,
    invert: bool = False,
) -> tuple[np.ndarray, np.ndarray]:
    rows = derive_ascii_height(frame_rgb.shape, width, height)
    resized = resize_rgb(frame_rgb, (width, rows))
    colors = resized.astype(np.float32)
    gray = 0.299 * colors[:, :, 0] + 0.587 * colors[:, :, 1] + 0.114 * colors[:, :, 2]
    if invert:
        gray = 255 - gray

    char_indices = np.clip(
        (gray.astype(np.float32) / 255.0 * (len(ASCII_CHARS) - 1)).astype(np.int32),
        0,
        len(ASCII_CHARS) - 1,
    )
    return _CHARS_ARRAY[char_indices], resized.astype(np.uint8)


def ascii_chars_to_plain_text(chars: np.ndarray) -> str:
    return "\n".join("".join(row.tolist()) for row in chars)


def ascii_chars_to_ansi_text(chars: np.ndarray, colors_rgb: np.ndarray) -> str:
    lines: list[str] = []
    rows, cols = chars.shape
    for row_i in range(rows):
        parts: list[str] = []
        for col_i in range(cols):
            r, g, b = colors_rgb[row_i, col_i]
            parts.append(f"\033[38;2;{int(r)};{int(g)};{int(b)}m{chars[row_i, col_i]}")
        parts.append(RESET_COLOR)
        lines.append("".join(parts))
    return "\n".join(lines)


def render_ascii_image(
    chars: np.ndarray,
    colors_rgb: np.ndarray,
    color: bool = False,
    cell_width: int = 8,
    cell_height: int = 12,
    font_size: int = 10,
    font_path: str | None = None,
    thickness: int = 1,
    background: tuple[int, int, int] = (0, 0, 0),
    foreground: tuple[int, int, int] = (255, 255, 255),
    output_size: tuple[int, int] | None = None,
    glow: float = 0.0,
) -> Image.Image:
    rows, cols = chars.shape
    canvas_h = max(1, rows * cell_height)
    canvas_w = max(1, cols * cell_width)
    image = Image.new("RGB", (canvas_w, canvas_h), background)
    draw = ImageDraw.Draw(image)
    font = load_font(font_path, font_size)
    bbox = font.getbbox("M")
    glyph_height = max(1, bbox[3] - bbox[1])
    y_offset = max(0, (cell_height - glyph_height) // 2 - bbox[1])
    stroke_width = max(0, thickness - 1)

    for row_i in range(rows):
        y = row_i * cell_height + y_offset
        for col_i in range(cols):
            x = col_i * cell_width
            char = str(chars[row_i, col_i])
            draw_color = tuple(int(v) for v in colors_rgb[row_i, col_i]) if color else foreground
            draw.text(
                (x, y),
                char,
                fill=draw_color,
                font=font,
                stroke_width=stroke_width,
                stroke_fill=draw_color,
            )

    if output_size:
        resampling = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
        image = image.resize(output_size, resampling)
    if glow > 0:
        glow_layer = image.filter(ImageFilter.GaussianBlur(radius=glow))
        image = Image.blend(glow_layer, image, 0.68)
    return image


def load_font(font_path: str | None, font_size: int) -> ImageFont.ImageFont:
    if font_path:
        try:
            return ImageFont.truetype(font_path, font_size)
        except Exception as exc:
            raise SystemExit(f"Cannot load font: {font_path}") from exc

    candidates = [
        "C:/Windows/Fonts/consola.ttf",
        "C:/Windows/Fonts/cour.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
        "/System/Library/Fonts/Menlo.ttc",
    ]
    for candidate in candidates:
        if Path(candidate).exists():
            try:
                return ImageFont.truetype(candidate, font_size)
            except Exception:
                pass
    return ImageFont.load_default()


def read_image(path: Path) -> np.ndarray:
    try:
        image = Image.open(path).convert("RGB")
    except Exception as exc:
        raise SystemExit(f"Cannot open image: {path}")
    return np.array(image)


def write_text(path: Path, text: str) -> None:
    ensure_parent(path)
    path.write_text(text, encoding="utf-8")


def save_rendered_image(path: Path, image: Image.Image) -> None:
    ensure_parent(path)
    try:
        image.save(path)
    except Exception as exc:
        raise SystemExit(f"Cannot write image: {path}")


def print_terminal_frame(text: str, clear: bool = False) -> None:
    if clear:
        sys.stdout.write(CURSOR_HOME)
    sys.stdout.write(text)
    sys.stdout.write(RESET_COLOR)
    sys.stdout.flush()


def build_text(chars: np.ndarray, colors_rgb: np.ndarray, color: bool) -> str:
    if color:
        return ascii_chars_to_ansi_text(chars, colors_rgb)
    return ascii_chars_to_plain_text(chars)


def emit_progress(enabled: bool, current: int, total: int | None) -> None:
    if not enabled:
        return
    total_value = total or 0
    percent = int(current * 100 / total_value) if total_value > 0 else -1
    print(f"PROGRESS {current} {total_value} {percent}", flush=True)


def convert_image(args: argparse.Namespace) -> None:
    frame = read_image(Path(args.input))
    emit_progress(args.progress, 0, 1)
    frame = enhance_frame_rgb(frame, args.brightness, args.contrast, args.saturation, args.gamma)
    chars, colors = frame_to_ascii_data(frame, args.width, args.height, args.invert)
    text = build_text(chars, colors, args.color)
    did_save = False

    if args.save_text:
        write_text(Path(args.save_text), text + "\n")
        did_save = True

    if args.save_image:
        rendered = render_ascii_image(
            chars,
            colors,
            color=args.color,
            cell_width=args.cell_width,
            cell_height=args.cell_height,
            font_size=args.font_size,
            font_path=args.font_path,
            thickness=args.thickness,
            output_size=args.output_size,
            glow=args.glow,
        )
        save_rendered_image(Path(args.save_image), rendered)
        did_save = True

    if args.print or not did_save:
        enable_ansi_windows()
        print_terminal_frame(text + "\n")

    emit_progress(args.progress, 1, 1)


def iter_video_frames(
    cap: cv2.VideoCapture,
    skip: int,
    max_frames: int | None,
) -> Iterable[np.ndarray]:
    source_index = 0
    emitted = 0
    while True:
        ok, frame = cap.read()
        if not ok:
            break
        if skip > 1 and source_index % skip != 0:
            source_index += 1
            continue
        source_index += 1
        emitted += 1
        yield frame
        if max_frames is not None and emitted >= max_frames:
            break


def save_video_frames_as_text(
    frame_index: int,
    chars: np.ndarray,
    colors: np.ndarray,
    color: bool,
    output_dir: Path,
) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    text = build_text(chars, colors, color)
    frame_path = output_dir / f"frame_{frame_index:06d}.txt"
    frame_path.write_text(text + "\n", encoding="utf-8")


def convert_video(args: argparse.Namespace) -> None:
    cv2 = require_cv2()
    input_path = Path(args.input)
    if not input_path.exists():
        raise SystemExit(f"Video file not found: {input_path}")

    cap = cv2.VideoCapture(str(input_path))
    if not cap.isOpened():
        raise SystemExit(f"Cannot open video: {input_path}")

    source_fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    output_fps = args.fps if args.fps else max(1.0, source_fps / max(1, args.skip))
    source_frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    total_expected = None
    if source_frame_count > 0:
        total_expected = max(1, (source_frame_count + max(1, args.skip) - 1) // max(1, args.skip))
        if args.max_frames is not None:
            total_expected = min(total_expected, args.max_frames)
    elif args.max_frames is not None:
        total_expected = args.max_frames
    writer = None
    wrote_anything = False

    if args.save_video:
        output_path = Path(args.save_video)
        ensure_parent(output_path)

    if args.preview:
        enable_ansi_windows()
        sys.stdout.write(HIDE_CURSOR + CLEAR_SCREEN)
        sys.stdout.flush()

    try:
        emit_progress(args.progress, 0, total_expected)
        for frame_number, frame_bgr in enumerate(
            iter_video_frames(cap, max(1, args.skip), args.max_frames),
            start=1,
        ):
            frame = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
            frame = enhance_frame_rgb(frame, args.brightness, args.contrast, args.saturation, args.gamma)
            chars, colors = frame_to_ascii_data(frame, args.width, args.height, args.invert)

            if args.save_frames:
                save_video_frames_as_text(
                    frame_number,
                    chars,
                    colors,
                    args.color,
                    Path(args.save_frames),
                )
                wrote_anything = True

            if args.save_video:
                rendered = render_ascii_image(
                    chars,
                    colors,
                    color=args.color,
                    cell_width=args.cell_width,
                    cell_height=args.cell_height,
                    font_size=args.font_size,
                    font_path=args.font_path,
                    thickness=args.thickness,
                    output_size=args.output_size,
                    glow=args.glow,
                )
                if writer is None:
                    width_px, height_px = rendered.size
                    fourcc = cv2.VideoWriter_fourcc(*args.fourcc)
                    writer = cv2.VideoWriter(str(output_path), fourcc, output_fps, (width_px, height_px))
                    if not writer.isOpened():
                        raise SystemExit(f"Cannot write video: {output_path}")
                rendered_bgr = cv2.cvtColor(np.array(rendered), cv2.COLOR_RGB2BGR)
                writer.write(rendered_bgr)
                wrote_anything = True

            if args.preview or not (args.save_video or args.save_frames):
                text = build_text(chars, colors, args.color)
                print_terminal_frame(text, clear=args.preview)
                if args.preview:
                    sys.stdout.write(f"\n{RESET_COLOR}Frame {frame_number} | Ctrl+C to stop")
                    sys.stdout.flush()
                    time.sleep(1.0 / output_fps)
                else:
                    sys.stdout.write("\n\n")
                wrote_anything = True

            emit_progress(args.progress, frame_number, total_expected)

    except KeyboardInterrupt:
        pass
    finally:
        cap.release()
        if writer is not None:
            writer.release()
        if args.preview:
            sys.stdout.write(SHOW_CURSOR + RESET_COLOR + "\n")
            sys.stdout.flush()

    if not wrote_anything:
        raise SystemExit("No frames were processed.")


def add_common_ascii_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("input", help="Input image or video path.")
    parser.add_argument(
        "--width",
        "-w",
        type=int,
        default=120,
        help="ASCII width in characters.",
    )
    parser.add_argument(
        "--height",
        type=int,
        default=None,
        help="ASCII height in rows. If omitted, height is derived from the source aspect ratio.",
    )
    parser.add_argument(
        "--color",
        action="store_true",
        help="Use ANSI color in terminal/text output and source colors in rendered files.",
    )
    parser.add_argument(
        "--invert",
        action="store_true",
        help="Invert brightness before mapping pixels to ASCII characters.",
    )
    parser.add_argument(
        "--brightness",
        type=float,
        default=DEFAULT_BRIGHTNESS,
        help=f"Brightness multiplier before ASCII conversion. Default: {DEFAULT_BRIGHTNESS}.",
    )
    parser.add_argument(
        "--contrast",
        type=float,
        default=DEFAULT_CONTRAST,
        help=f"Contrast multiplier before ASCII conversion. Default: {DEFAULT_CONTRAST}.",
    )
    parser.add_argument(
        "--saturation",
        type=float,
        default=DEFAULT_SATURATION,
        help=f"Color saturation multiplier. Default: {DEFAULT_SATURATION}.",
    )
    parser.add_argument(
        "--gamma",
        type=float,
        default=DEFAULT_GAMMA,
        help=f"Gamma correction. Values above 1 brighten midtones. Default: {DEFAULT_GAMMA}.",
    )
    parser.add_argument(
        "--vivid",
        action="store_true",
        help="Shortcut preset for bright, saturated, high-contrast output.",
    )
    parser.add_argument(
        "--progress",
        action="store_true",
        help="Print machine-readable progress lines: PROGRESS current total percent.",
    )


def add_render_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--output-size",
        type=parse_output_size,
        default=None,
        metavar="WIDTHxHEIGHT",
        help="Resize rendered image/video output to an exact pixel size, for example 1280x720.",
    )
    parser.add_argument("--cell-width", type=int, default=8, help="Pixel width per ASCII cell.")
    parser.add_argument("--cell-height", type=int, default=12, help="Pixel height per ASCII cell.")
    parser.add_argument("--font-size", type=int, default=10, help="Rendered text font size in pixels.")
    parser.add_argument("--font-path", default=None, help="Optional path to a monospace .ttf/.ttc font.")
    parser.add_argument("--thickness", type=int, default=1, help="Rendered text stroke thickness.")
    parser.add_argument(
        "--glow",
        type=float,
        default=DEFAULT_GLOW,
        help=f"Soft glow radius for saved PNG/MP4 output. Default: {DEFAULT_GLOW}.",
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="ascii_media_tools.py",
        description="Convert images and videos to ASCII in the terminal or saved files.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    image_parser = subparsers.add_parser("image", help="Convert an image to ASCII.")
    add_common_ascii_args(image_parser)
    add_render_args(image_parser)
    image_parser.add_argument("--print", action="store_true", help="Print ASCII to the terminal.")
    image_parser.add_argument("--save-text", help="Save ASCII text to a .txt file.")
    image_parser.add_argument("--save-image", help="Save rendered ASCII image, for example output.png.")
    image_parser.set_defaults(func=convert_image)

    video_parser = subparsers.add_parser("video", help="Convert a video to ASCII.")
    add_common_ascii_args(video_parser)
    add_render_args(video_parser)
    video_parser.add_argument("--preview", action="store_true", help="Preview ASCII video in the terminal.")
    video_parser.add_argument("--save-video", help="Save rendered ASCII video, for example output.mp4.")
    video_parser.add_argument("--save-frames", help="Save ASCII text frames into this directory.")
    video_parser.add_argument("--skip", type=int, default=1, help="Render every Nth source frame.")
    video_parser.add_argument("--fps", type=float, default=None, help="Output FPS. Default is source FPS / skip.")
    video_parser.add_argument("--max-frames", type=int, default=None, help="Stop after this many rendered frames.")
    video_parser.add_argument(
        "--fourcc",
        default="mp4v",
        help="OpenCV video codec fourcc. Default: mp4v.",
    )
    video_parser.set_defaults(func=convert_video)

    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()

    if getattr(args, "width", 1) < 1:
        parser.error("--width must be greater than zero.")
    if getattr(args, "height", None) is not None and args.height < 1:
        parser.error("--height must be greater than zero.")
    if hasattr(args, "skip") and args.skip < 1:
        parser.error("--skip must be greater than zero.")
    if hasattr(args, "max_frames") and args.max_frames is not None and args.max_frames < 1:
        parser.error("--max-frames must be greater than zero.")
    if getattr(args, "cell_width", 1) < 1 or getattr(args, "cell_height", 1) < 1:
        parser.error("--cell-width and --cell-height must be greater than zero.")
    if getattr(args, "font_size", 1) < 1:
        parser.error("--font-size must be greater than zero.")
    if getattr(args, "thickness", 1) < 1:
        parser.error("--thickness must be greater than zero.")
    if getattr(args, "brightness", 1.0) <= 0:
        parser.error("--brightness must be greater than zero.")
    if getattr(args, "contrast", 1.0) <= 0:
        parser.error("--contrast must be greater than zero.")
    if getattr(args, "saturation", 1.0) < 0:
        parser.error("--saturation must be zero or greater.")
    if getattr(args, "gamma", 1.0) <= 0:
        parser.error("--gamma must be greater than zero.")
    if getattr(args, "glow", 0.0) < 0:
        parser.error("--glow must be zero or greater.")

    if getattr(args, "vivid", False):
        args.brightness *= 1.18
        args.contrast *= 1.45
        args.saturation *= 1.85
        args.gamma *= 1.22
        if getattr(args, "glow", 0.0) == 0.0:
            args.glow = 1.2

    args.func(args)


if __name__ == "__main__":
    main()
