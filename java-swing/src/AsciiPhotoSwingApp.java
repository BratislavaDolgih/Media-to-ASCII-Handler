import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 * The main and, for now, only class of the graphical application.
 * It is the window, the UI controller, and the wrapper around the Python
 * conversion script at the same time.
 *
 * <p>
 *     This class extends {@link JFrame}, because the application is built as a
 *     regular desktop window. In practice, this object is the whole visible app:
 *     <ul>
 *         <li>the title bar,</li>
 *         <li>the window size,</li>
 *         <li>the buttons,</li>
 *         <li>the input fields,</li>
 *         <li>the log area and close behavior.</li>
 *     </ul>
 * </p>
 *
 * <p>
 *     Architecturally, the class wears several hats. For a small utility this is
 *     acceptable: the app has one core mission, and keeping the tiny control room
 *     in one place makes the flow easy to read. If the GUI grows into a serious
 *     studio, this class should be split into a view, controller, and process
 *     service.
 * </p>
 *
 * <p>
 *     Applied meaning: the user chooses a media victim, selects how detailed the
 *     ASCII output should be, and this class launches {@code ascii_media_tools.py}
 *     as an external Python process. Java does the window magic; Python does the
 *     media alchemy.
 * </p>
 *
 * @author Stephan Kolesnikov
 */
public class AsciiPhotoSwingApp extends JFrame {

    /*
     * [==== MAIN FIELDS ====]
     * The fields below are the persistent state of the window: visible widgets,
     * the currently selected media file, and references to the active background
     * processing task.
     */

    /** Displays the selected media path; it also allows manual path editing. */
    private final JTextField selectedFileField = new JTextField();

    /** Controls ASCII detail: more columns means sharper output and slower work. */
    private final JTextField widthField = new JTextField("210", 6);

    /** Central text area for user-visible process logs and Python output. */
    private final JTextArea logArea = new JTextArea(10, 70);

    /** Visual progress indicator fed by {@code PROGRESS current total percent}. */
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    /** Starts conversion using the configured ASCII width. */
    private final JButton processButton = new JButton("Forge by width");

    /** Starts conversion and asks Python to render into the source pixel size. */
    private final JButton naturalSizeButton = new JButton("Forge at native size");

    /** Attempts to stop the currently running Python conversion process. */
    private final JButton stopButton = new JButton("Stop the forge");

    /** External Python process currently doing the heavy lifting, if any. */
    private volatile Process currentProcess;

    /** Swing background worker that keeps the window responsive during processing. */
    private volatile SwingWorker<Integer, String> currentWorker;

    /** The photo or video chosen by the user. */
    private File selectedFile;


    // -------------------------------------------------------------
    /*
     * [==== CONSTRUCTOR & BUILD METHOD ====]
     */

    /**
     * Creates the main window, configures its basic Swing behavior, wires the
     * first always-on listeners, and places the control panel above the log area.
     *
     * @see JFrame
     */
    public AsciiPhotoSwingApp() {
        super("Kolesnikov's MEDIA-to-ASCII translator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        selectedFileField.setEditable(true);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        progressBar.setStringPainted(true);
        progressBar.setString("Ready to forge.");

        stopButton.setEnabled(false);
        stopButton.addActionListener(event -> stopProcessing());

        add(buildControls(), BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        log("Choose the victim (photo or video), then press the right forging button...");
    }

    /**
     * Builds the top control panel of the window:
     * <ul>
     *     <li>the selected file row with a tiny chooser button,</li>
     *     <li>the ASCII width row,</li>
     *     <li>the progress bar,</li>
     *     <li>the action buttons.</li>
     * </ul>
     *
     * <p>
     *     API meaning: {@link GridBagLayout} gives us a compact two-column form
     *     where labels stay small and fields take the available width.
     * </p>
     *
     * <p>
     *     Applied meaning: this is the user's cockpit. Pick the file, choose how
     *     dense the ASCII should be, then press the button and let the terminal
     *     artwork machine wake up.
     * </p>
     *
     * @return the ready-to-insert Swing panel
     */
    private JPanel buildControls() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        JButton chooseButton = new JButton("...");
        chooseButton.setMargin(new Insets(2, 8, 2, 8));
        chooseButton.setToolTipText("Choose a media file...");
        chooseButton.addActionListener(event -> chooseFile());

        processButton.addActionListener(event -> processMedia(processButton, false));
        naturalSizeButton.addActionListener(event -> processMedia(naturalSizeButton, true));

        JPanel filePanel = new JPanel(new BorderLayout(8, 0));
        filePanel.add(selectedFileField, BorderLayout.CENTER);
        filePanel.add(chooseButton, BorderLayout.EAST);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        panel.add(filePanel, c);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttonPanel.add(processButton);
        buttonPanel.add(naturalSizeButton);
        buttonPanel.add(stopButton);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        panel.add(new JLabel("File:"), c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        panel.add(new JLabel("ASCII width:"), c);

        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        JPanel widthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        widthPanel.add(widthField);
        panel.add(widthPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(progressBar, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1;
        panel.add(buttonPanel, c);

        return panel;
    }


    // -------------------------------------------------------------
    /*
     * [==== GENERAL METHODS ====]
     */

    /**
     * Opens the standard Swing file chooser and stores the chosen media file.
     *
     * <p>
     *     Only common image and video formats are shown by default. The selected
     *     file path is copied into {@link #selectedFileField}, then mirrored into
     *     the log so the user can see which victim entered the ASCII machine.
     * </p>
     *
     * @see JFileChooser
     */
    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Content media (" +
                    "*.jpg, *.png, *.mp4, *.avi, *.mkv, *.mov, *.webm)",
            "jpg", "jpeg", "png", "bmp", "webp", "tif", "tiff", "mp4", "avi", "mkv", "mov", "webm"
        ));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            selectedFileField.setText(selectedFile.getAbsolutePath());
            log("Selected victim: " + selectedFile.getAbsolutePath());
        }
    }

    /**
     * Starts media processing through the fork's Python script.
     *
     * <p>
     *     The method validates UI state, creates a {@link SwingWorker}, builds a
     *     command line for {@code ascii_media_tools.py}, and streams Python output
     *     back into the Swing log. The heavy work runs outside the Event Dispatch
     *     Thread, so the window keeps breathing while a video is being chewed into
     *     ASCII.
     * </p>
     *
     * @param processButton the button that initiated processing
     * @param naturalSize whether the saved output should keep the source pixel size
     */
    private void processMedia(JButton processButton, boolean naturalSize) {
        if (currentWorker != null && !currentWorker.isDone()) {
            JOptionPane.showMessageDialog(
                    this,
                    "The forge is already running. Stop it or let it finish its ritual.",
                    "Already forging",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selectedFile == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Choose a photo or video first.",
                    "No victim selected!",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int width;
        try {
            width = Integer.parseInt(widthField.getText().trim());
            if (width < 1) {
                throw new NumberFormatException("Width must be positive");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ASCII width must be a positive number.", "Input mischief", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setProcessingState(true, "Warming up...");
        log(naturalSize
            ? "Forging at native size through the fork's Python script..."
            : "Forging through the fork's Python script...");

        SwingWorker<Integer, String> worker = new SwingWorker<>() {
            private Path outputPath;

            @Override
            protected Integer doInBackground() throws Exception {
                Path forkDir = locateForkDir();
                Path script = forkDir.resolve("ascii_media_tools.py");
                if (!Files.exists(script)) {
                    throw new IOException("ascii_media_tools.py was not found near the app: " + script);
                }

                Path outputDir = forkDir.resolve("ascii_outputs");
                Files.createDirectories(outputDir);
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String mediaType = detectMediaType(selectedFile);
                outputPath = outputDir.resolve(
                    ("video".equals(mediaType) ? "video_ascii_" : "photo_ascii_")
                        + stamp
                        + ("video".equals(mediaType) ? ".mp4" : ".png")
                );

                List<String> command = new ArrayList<>();
                command.add(findPython());
                command.add(script.toString());
                command.add(mediaType);
                command.add(selectedFile.getAbsolutePath());
                command.add("--width");
                command.add(String.valueOf(width));
                command.add("--color");
                command.add("--vivid");
                command.add("--progress");

                if (naturalSize) {
                    Dimension natural = readNaturalSize(forkDir, selectedFile, mediaType);
                    command.add("--output-size");
                    command.add(natural.width + "x" + natural.height);
                    publish("Native size: " + natural.width + "x" + natural.height);
                }

                command.add("video".equals(mediaType) ? "--save-video" : "--save-image");
                command.add(outputPath.toString());

                publish("Command: " + String.join(" ", command));

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(forkDir.toFile());
                builder.redirectErrorStream(true);
                Process process = builder.start();
                currentProcess = process;

                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
                )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        publish(line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    publish("Forged artifact: " + outputPath);
                }
                return exitCode;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    if (!handleProgressLine(chunk)) {
                        log(chunk);
                    }
                }
            }

            @Override
            protected void done() {
                currentProcess = null;
                currentWorker = null;
                String status = "Ready";
                try {
                    int exitCode = get();
                    if (exitCode != 0) {
                        status = "Error";
                        log("Python exited with code: " + exitCode);
                    }
                } catch (CancellationException ex) {
                    status = "Stopped";
                    log("The forge was stopped.");
                } catch (Exception ex) {
                    status = "Error";
                    log("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(AsciiPhotoSwingApp.this, ex.getMessage(), "Something went sideways", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setProcessingState(false, status);
                }
            }
        };

        currentWorker = worker;
        worker.execute();
    }

    /**
     * Switches the GUI between idle and processing modes.
     *
     * @param active {@code true} while conversion is running
     * @param status text displayed inside the progress bar
     */
    private void setProcessingState(boolean active, String status) {
        processButton.setEnabled(!active);
        naturalSizeButton.setEnabled(!active);
        stopButton.setEnabled(active);
        progressBar.setIndeterminate(active);
        if (active) {
            progressBar.setValue(0);
        }
        progressBar.setString(status);
    }

    /**
     * Cancels the active {@link SwingWorker} and asks the Python process to stop.
     *
     * <p>
     *     First it tries a polite shutdown via {@link Process#destroy()}. If the
     *     process ignores the hint for a few seconds, the backup thread uses
     *     {@link Process#destroyForcibly()}. Sometimes even ASCII needs a firm
     *     hand.
     * </p>
     */
    private void stopProcessing() {
        log("Stopping the forge...");
        progressBar.setString("Stopping...");
        stopButton.setEnabled(false);

        SwingWorker<Integer, String> worker = currentWorker;
        if (worker != null) {
            worker.cancel(true);
        }

        Process process = currentProcess;
        if (process != null && process.isAlive()) {
            process.destroy();
            Thread killer = new Thread(() -> {
                try {
                    if (!process.waitFor(3, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                }
            }, "ascii-process-stop");
            killer.setDaemon(true);
            killer.start();
        }
    }

    /**
     * Parses machine-readable progress messages produced by Python.
     *
     * @param line a line from the Python process
     * @return {@code true} if the line was a progress protocol line and should not
     *         be printed as a regular log message
     */
    private boolean handleProgressLine(String line) {
        if (!line.startsWith("PROGRESS ")) {
            return false;
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length != 4) {
            return true;
        }

        try {
            int current = Integer.parseInt(parts[1]);
            int total = Integer.parseInt(parts[2]);
            int percent = Integer.parseInt(parts[3]);

            if (total > 0 && percent >= 0) {
                progressBar.setIndeterminate(false);
                progressBar.setValue(Math.max(0, Math.min(100, percent)));
                progressBar.setString(current + " / " + total + " frames (" + percent + "%)");
            } else {
                progressBar.setIndeterminate(true);
                progressBar.setString("Frame " + current);
            }
        } catch (NumberFormatException ex) {
            progressBar.setIndeterminate(true);
            progressBar.setString("Working...");
        }
        return true;
    }

    /**
     * Detects whether the selected file should be processed as an image or video.
     *
     * @param file selected input media
     * @return {@code image} or {@code video}, matching the Python CLI subcommands
     * @throws IOException if the file extension is not supported
     */
    private String detectMediaType(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
            || name.endsWith(".bmp") || name.endsWith(".webp") || name.endsWith(".tif")
            || name.endsWith(".tiff")) {
            return "image";
        }
        if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv")
            || name.endsWith(".mov") || name.endsWith(".webm")) {
            return "video";
        }
        throw new IOException("Unsupported file format: " + file.getName());
    }

    /**
     * Reads the original pixel dimensions of the selected media file.
     *
     * <p>
     *     Images are handled directly with {@link ImageIO}. Videos are delegated to
     *     a tiny Python/OpenCV probe, because Java's standard library does not know
     *     how to inspect video metadata by itself.
     * </p>
     *
     * @param forkDir directory containing the fork Python tool
     * @param file selected input media
     * @param mediaType {@code image} or {@code video}
     * @return source pixel dimensions
     * @throws Exception if the dimensions cannot be read
     */
    private Dimension readNaturalSize(Path forkDir, File file, String mediaType) throws Exception {
        if ("image".equals(mediaType)) {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Could not read image size: " + file.getAbsolutePath());
            }
            return new Dimension(image.getWidth(), image.getHeight());
        }

        List<String> command = new ArrayList<>();
        command.add(findPython());
        command.add("-c");
        command.add(
            "import cv2,sys;"
                + "cap=cv2.VideoCapture(sys.argv[1]);"
                + "ok=cap.isOpened();"
                + "w=int(cap.get(cv2.CAP_PROP_FRAME_WIDTH));"
                + "h=int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT));"
                + "cap.release();"
                + "print(f'{w}x{h}') if (ok and w>0 and h>0) else sys.exit(1)"
        );
        command.add(file.getAbsolutePath());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(forkDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(
                "Could not read video size. Check that opencv-python is installed. "
                    + output.toString().trim()
            );
        }

        String size = output.toString().trim();
        String[] parts = size.split("x");
        if (parts.length != 2) {
            throw new IOException("Unexpected response while reading video size: " + size);
        }
        return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * Locates the directory that contains {@code ascii_media_tools.py}.
     *
     * <p>
     *     This makes the GUI tolerant to being launched from the source tree, from
     *     the {@code dist} folder, or from one of the parent directories. The app
     *     sniffs around for the Python tool before giving up and using the current
     *     working directory.
     * </p>
     *
     * @return best guess for the fork directory
     */
    private Path locateForkDir() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path appDir = locateAppDir();
        if (appDir != null && Files.exists(appDir.resolve("ascii_media_tools.py"))) {
            return appDir;
        }

        Path cursor = current;
        for (int i = 0; i < 8 && cursor != null; i++) {
            if (Files.exists(cursor.resolve("ascii_media_tools.py"))) {
                return cursor;
            }
            Path nestedFork = cursor.resolve("fork");
            if (Files.exists(nestedFork.resolve("ascii_media_tools.py"))) {
                return nestedFork;
            }
            cursor = cursor.getParent();
        }
        return current;
    }

    /**
     * Finds the directory where this class or JAR is running from.
     *
     * @return application directory, or {@code null} if the runtime location cannot
     *         be resolved
     */
    private Path locateAppDir() {
        try {
            Path location = Paths.get(
                AsciiPhotoSwingApp.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toAbsolutePath();
            if (Files.isRegularFile(location)) {
                return location.getParent();
            }
            return location;
        } catch (URISyntaxException | IllegalArgumentException ex) {
            return null;
        }
    }


    // -------------------------------------------------------------
    /*
     * [==== UTILITIES & main(String[] args) ====]
     */

    /**
     * Finds which Python executable should be used.
     *
     * <p>
     *     If {@code ASCII_FORK_PYTHON} is set, its value wins. Otherwise the app
     *     falls back to {@code python} and expects it to be available in PATH.
     * </p>
     *
     * @return Python executable name or absolute path
     */
    private String findPython() {
        String configured = System.getenv("ASCII_FORK_PYTHON");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return "python";
    }
    /**
     * Appends a message to the log area and scrolls to the latest line.
     *
     * @param message text to show in the local log
     */
    private void log(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Boring main method, noble destiny: wake up Swing on the Event Dispatch
     * Thread and show the ASCII control room.
     *
     * @param args command-line arguments, currently ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AsciiPhotoSwingApp().setVisible(true));
    }
}
