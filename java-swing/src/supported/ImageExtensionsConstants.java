package supported;

import java.nio.file.Path;

/**
 * Supported image file extensions for the Media-to-ASCII desktop application.
 *
 * <p>
 *     The enum keeps image format knowledge in one place, so the UI, file
 *     chooser filters, and media-type detection do not have to repeat the same
 *     string literals over and over. Tiny enum, useful discipline.
 * </p>
 *
 * <p>
 *     Extension values are stored without a leading dot. Public helper methods
 *     accept both {@code "jpg"} and {@code ".jpg"}, and compare values in a
 *     case-insensitive way.
 * </p>
 *
 * @author Stephan Kolesnikov
 * @since 1.1.0
 */
public enum ImageExtensionsConstants {
    /** JPEG image extension, short form. */
    JPG("jpg"),

    /** JPEG image extension, long form. */
    JPEG("jpeg"),

    /** Portable Network Graphics image extension. */
    PNG("png"),

    /** Bitmap image extension. */
    BMP("bmp"),

    /** WebP image extension. */
    WEBP("webp"),

    /** Tagged Image File extension, short form. */
    TIF("tif"),

    /** Tagged Image File extension, long form. */
    TIFF("tiff");

    private final String imageExtensionName;

    /**
     * Creates an image extension constant.
     *
     * @param extensionName extension text without the leading dot
     */
    ImageExtensionsConstants(String extensionName) {
        this.imageExtensionName = extensionName;
    }

    /**
     * Checks whether a file extension is supported as an image input.
     *
     * <p>
     *     The method is intentionally forgiving: it trims whitespace, ignores a
     *     leading dot, and accepts uppercase/lowercase variations.
     * </p>
     *
     * @param checkableExtension extension to check, for example {@code "png"} or
     *                           {@code ".PNG"}
     * @return {@code true} if the extension belongs to a supported image format
     */
    public static boolean isSupportedImageExtension(String checkableExtension) {
        String normalizedExtension = normalizeExtension(checkableExtension);
        for (ImageExtensionsConstants extension : values()) {
            if (extension.imageExtensionName.equals(normalizedExtension)) {
                return true;
            }
        }
        return false;
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

    /**
     * Checks whether a file-system path points to a supported image file.
     *
     * <p>
     *     The method extracts the extension from the last dot in the file name and
     *     delegates comparison to {@link #isSupportedImageExtension(String)}.
     * </p>
     *
     * @param path file-system path to inspect
     * @return {@code true} if the file name has a supported image extension
     */
    public static boolean isSupportedImagePath(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }
        return isSupportedImageExtension(fileName.substring(dotIndex + 1));
    }

    /**
     * Returns the extension text without a leading dot.
     *
     * @return normalized extension name
     */
    public String getExtensionName() {
        return imageExtensionName;
    }
}
