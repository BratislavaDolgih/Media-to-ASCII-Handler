package supported;

import java.nio.file.Path;

/**
 * Supported video file extensions for the Media-to-ASCII desktop application.
 *
 * <p>
 *     This enum mirrors {@link ImageExtensionsConstants}, but for video input.
 *     It gives the Swing layer one shared source of truth for video formats
 *     accepted by the file chooser and by media-type detection.
 * </p>
 *
 * <p>
 *     Extension values are stored without a leading dot. Public helper methods
 *     accept both {@code "mp4"} and {@code ".mp4"}, and compare values in a
 *     case-insensitive way.
 * </p>
 *
 * @author Stephan Kolesnikov
 * @since 1.1.0
 */
public enum VideoExtensionsConstants {
    /** MPEG-4 video container extension. */
    MP4("mp4"),

    /** Audio Video Interleave container extension. */
    AVI("avi"),

    /** Matroska video container extension. */
    MKV("mkv"),

    /** QuickTime movie container extension. */
    MOV("mov"),

    /** WebM video container extension. */
    WEBM("webm");

    private final String videoExtensionName;

    /**
     * Creates a video extension constant.
     *
     * @param extensionName extension text without the leading dot
     */
    VideoExtensionsConstants(String extensionName) {
        this.videoExtensionName = extensionName;
    }

    /**
     * Checks whether a file extension is supported as a video input.
     *
     * <p>
     *     The method is intentionally forgiving: it trims whitespace, ignores a
     *     leading dot, and accepts uppercase/lowercase variations.
     * </p>
     *
     * @param checkableExtension extension to check, for example {@code "mp4"} or
     *                           {@code ".WEBM"}
     * @return {@code true} if the extension belongs to a supported video format
     */
    public static boolean isSupportedVideoExtension(String checkableExtension) {
        String normalizedExtension = normalizeExtension(checkableExtension);
        for (VideoExtensionsConstants extension : values()) {
            if (extension.videoExtensionName.equals(normalizedExtension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a path points to a supported video file by inspecting its
     * extension.
     *
     * @param path file-system path to inspect
     * @return {@code true} if the file name has a supported video extension
     */
    public static boolean isSupportedVideoPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }
        return isSupportedVideoExtension(fileName.substring(dotIndex + 1));
    }

    /**
     * Returns the extension text without a leading dot.
     *
     * @return normalized extension name
     */
    public String getExtensionName() {
        return videoExtensionName;
    }

    /**
     * Normalizes user/file-system extension input for predictable comparison.
     *
     * @param extension raw extension value
     * @return lowercase extension without leading dot, or an empty string for
     *         {@code null}
     */
    private static String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        String normalized = extension.trim().toLowerCase();
        if (normalized.startsWith(".")) {
            return normalized.substring(1);
        }
        return normalized;
    }
}
