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

public class AsciiPhotoSwingApp extends JFrame {
    private final JTextField selectedFileField = new JTextField();
    private final JTextField widthField = new JTextField("160", 6);
    private final JTextArea logArea = new JTextArea(10, 70);
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JButton processButton = new JButton("Обработать фотографию");
    private final JButton naturalSizeButton = new JButton("По натуральному размеру");
    private final JButton stopButton = new JButton("Остановить");
    private volatile Process currentProcess;
    private volatile SwingWorker<Integer, String> currentWorker;
    private File selectedFile;

    public AsciiPhotoSwingApp() {
        super("ASCII Photo Fork");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        selectedFileField.setEditable(false);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        progressBar.setStringPainted(true);
        progressBar.setString("Готово");
        stopButton.setEnabled(false);
        stopButton.addActionListener(event -> stopProcessing());

        add(buildControls(), BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        log("Выбери фотографию или видео, затем нажми кнопку обработки.");
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        JButton chooseButton = new JButton("Выбрать файл");
        chooseButton.addActionListener(event -> chooseFile());

        processButton.addActionListener(event -> processMedia(processButton, false));

        naturalSizeButton.addActionListener(event -> processMedia(naturalSizeButton, true));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(chooseButton);
        buttonPanel.add(processButton);
        buttonPanel.add(naturalSizeButton);
        buttonPanel.add(stopButton);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        panel.add(new JLabel("Файл:"), c);

        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        panel.add(selectedFileField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        panel.add(new JLabel("Ширина ASCII:"), c);

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

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Images and videos (*.jpg, *.png, *.mp4, *.avi, *.mkv, *.mov, *.webm)",
            "jpg", "jpeg", "png", "bmp", "webp", "tif", "tiff", "mp4", "avi", "mkv", "mov", "webm"
        ));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            selectedFileField.setText(selectedFile.getAbsolutePath());
            log("Выбран файл: " + selectedFile.getAbsolutePath());
        }
    }

    private void processMedia(JButton processButton, boolean naturalSize) {
        if (currentWorker != null && !currentWorker.isDone()) {
            JOptionPane.showMessageDialog(this, "Обработка уже идёт. Останови её или дождись завершения.", "Уже работаю", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Сначала выбери фотографию или видео.", "Нет файла", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int width;
        try {
            width = Integer.parseInt(widthField.getText().trim());
            if (width < 1) {
                throw new NumberFormatException("Width must be positive");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ширина должна быть положительным числом.", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setProcessingState(true, "Запуск...");
        log(naturalSize
            ? "Запускаю обработку в натуральном размере через Python-скрипт форка..."
            : "Запускаю обработку через Python-скрипт форка...");

        SwingWorker<Integer, String> worker = new SwingWorker<>() {
            private Path outputPath;

            @Override
            protected Integer doInBackground() throws Exception {
                Path forkDir = locateForkDir();
                Path script = forkDir.resolve("ascii_media_tools.py");
                if (!Files.exists(script)) {
                    throw new IOException("Не найден ascii_media_tools.py рядом с приложением: " + script);
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
                    publish("Натуральный размер: " + natural.width + "x" + natural.height);
                }

                command.add("video".equals(mediaType) ? "--save-video" : "--save-image");
                command.add(outputPath.toString());

                publish("Команда: " + String.join(" ", command));

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
                    publish("Готово: " + outputPath);
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
                String status = "Готово";
                try {
                    int exitCode = get();
                    if (exitCode != 0) {
                        status = "Ошибка";
                        log("Python завершился с кодом: " + exitCode);
                    }
                } catch (CancellationException ex) {
                    status = "Остановлено";
                    log("Обработка остановлена.");
                } catch (Exception ex) {
                    status = "Ошибка";
                    log("Ошибка: " + ex.getMessage());
                    JOptionPane.showMessageDialog(AsciiPhotoSwingApp.this, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setProcessingState(false, status);
                }
            }
        };

        currentWorker = worker;
        worker.execute();
    }

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

    private void stopProcessing() {
        log("Останавливаю обработку...");
        progressBar.setString("Остановка...");
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
                progressBar.setString(current + " / " + total + " кадров (" + percent + "%)");
            } else {
                progressBar.setIndeterminate(true);
                progressBar.setString("Кадр " + current);
            }
        } catch (NumberFormatException ex) {
            progressBar.setIndeterminate(true);
            progressBar.setString("Работаю...");
        }
        return true;
    }

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
        throw new IOException("Неподдерживаемый формат файла: " + file.getName());
    }

    private Dimension readNaturalSize(Path forkDir, File file, String mediaType) throws Exception {
        if ("image".equals(mediaType)) {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Не удалось прочитать размер изображения: " + file.getAbsolutePath());
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
                "Не удалось прочитать размер видео. Проверь, что установлен opencv-python. "
                    + output.toString().trim()
            );
        }

        String size = output.toString().trim();
        String[] parts = size.split("x");
        if (parts.length != 2) {
            throw new IOException("Неожиданный ответ при чтении размера видео: " + size);
        }
        return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

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

    private String findPython() {
        String configured = System.getenv("ASCII_FORK_PYTHON");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return "python";
    }

    private void log(String message) {
        logArea.append(message + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AsciiPhotoSwingApp().setVisible(true));
    }
}
