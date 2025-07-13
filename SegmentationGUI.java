package Project;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SegmentationGUI extends JFrame {

    private JLabel imagePreviewLabel;
    private JTextField thresholdField;
    private JComboBox<String> typeComboBox;
    private JTextField numThreadsField;
    private JTextField delayMsField;
    private JTextArea metricsTextArea;
    private JScrollPane metricsScrollPane;
    private JButton uploadButton;
    private JButton startSegmentationButton;
    private JButton runScalabilityTestButton;

    private BufferedImage originalImage;
    private BufferedImage seqOutputImage;
    private BufferedImage parOutputImage;

    private LiveImageDisplay seqLivePanel;
    private LiveImageDisplay parLivePanel;

    public SegmentationGUI() {
        super("Image Segmentation Performance Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Set a preferred starting size for the main window
        setPreferredSize(new Dimension(1200, 800)); // Increased window size
        setLayout(new BorderLayout(15, 15)); // Increased outer layout gaps

        initComponents();
        setupLayout();
        addListeners();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // --- Controls Panel Components ---
        uploadButton = new JButton("Upload Photo");
        imagePreviewLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2)); // Thicker, lighter border
        imagePreviewLabel.setPreferredSize(new Dimension(250, 180)); // Larger preview placeholder

        thresholdField = new JTextField("128", 5);
        typeComboBox = new JComboBox<>(new String[]{"grayscale", "red", "green", "custom"});
        numThreadsField = new JTextField(String.valueOf(Runtime.getRuntime().availableProcessors()), 5);
        delayMsField = new JTextField("0", 5);

        startSegmentationButton = new JButton("Start Segmentation");
        runScalabilityTestButton = new JButton("Run Scalability Test");

        // --- Metrics Display ---
        metricsTextArea = new JTextArea(15, 60); // Increased rows and columns for more text
        metricsTextArea.setEditable(false);
        metricsTextArea.setLineWrap(true);
        metricsTextArea.setWrapStyleWord(true);
        metricsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Monospaced for better readability
        metricsTextArea.setBackground(new Color(240, 240, 240)); // Light gray background
        metricsScrollPane = new JScrollPane(metricsTextArea);
        metricsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // Always show scrollbar
        metricsScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Performance Metrics")); // Titled border

        // --- Image Display Panels ---
        seqLivePanel = new LiveImageDisplay(null);
        parLivePanel = new LiveImageDisplay(null);
        seqLivePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sequential Progress"));
        parLivePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Parallel Progress"));
        // Set preferred size for live panels so they start larger
        seqLivePanel.setPreferredSize(new Dimension(400, 300));
        parLivePanel.setPreferredSize(new Dimension(400, 300));
    }

    private void setupLayout() {
        // --- Controls Panel (Using GridBagLayout for more flexibility) ---
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10), // Outer padding
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1) // Thin border
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding for components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Upload Button and Image Preview
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        controlPanel.add(uploadButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span two columns for the preview
        gbc.anchor = GridBagConstraints.CENTER;
        controlPanel.add(imagePreviewLabel, gbc);

        // Add a vertical rigid area for spacing
        gbc.gridy = 1;
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);


        // Row 2: Input fields and labels
        gbc.gridwidth = 1; // Reset gridwidth
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 2; controlPanel.add(new JLabel("Threshold (0-255):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; controlPanel.add(thresholdField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; controlPanel.add(new JLabel("Segmentation Type:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; controlPanel.add(typeComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 4; controlPanel.add(new JLabel("Number of Threads:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; controlPanel.add(numThreadsField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; controlPanel.add(new JLabel("Delay (ms/update):"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; controlPanel.add(delayMsField, gbc);

        // Row 6: Buttons for segmentation and scalability test
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); // Horizontal flow for buttons
        buttonPanel.add(startSegmentationButton);
        buttonPanel.add(runScalabilityTestButton);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2; // Span across two columns
        gbc.fill = GridBagConstraints.NONE; // Don't stretch buttons
        gbc.anchor = GridBagConstraints.CENTER; // Center the panel
        controlPanel.add(buttonPanel, gbc);

        // Add the main control panel to the NORTH
        add(controlPanel, BorderLayout.WEST); // Place controls on the left side

        // --- Center Panel for Live Image Displays ---
        JPanel imageDisplaysPanel = new JPanel(new GridLayout(1, 2, 15, 0)); // Increased gap between panels
        imageDisplaysPanel.add(seqLivePanel);
        imageDisplaysPanel.add(parLivePanel);
        add(imageDisplaysPanel, BorderLayout.CENTER);

        // --- Bottom Panel for Metrics ---
        add(metricsScrollPane, BorderLayout.SOUTH);
    }

    private void addListeners() {
        uploadButton.addActionListener(e -> uploadPhoto());
        startSegmentationButton.addActionListener(e -> startSegmentation());
        runScalabilityTestButton.addActionListener(e -> runScalabilityTest());
    }

    private void uploadPhoto() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select an Image File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "bmp", "gif"));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                originalImage = ImageIO.read(selectedFile);
                if (originalImage == null) {
                    JOptionPane.showMessageDialog(this, "Could not read image from " + selectedFile.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Resize image for preview, maintaining aspect ratio
                ImageIcon icon = new ImageIcon(originalImage);
                Image image = icon.getImage();
                // Scale to fit label's preferred size, not its current actual size which might be 0,0 initially
                Image scaledImage = image.getScaledInstance(imagePreviewLabel.getPreferredSize().width, imagePreviewLabel.getPreferredSize().height, Image.SCALE_SMOOTH);
                imagePreviewLabel.setIcon(new ImageIcon(scaledImage));
                imagePreviewLabel.setText(""); // Clear text

                // Initialize output images
                seqOutputImage = deepCopy(originalImage);
                parOutputImage = deepCopy(originalImage);

                // Update live display panels with initial image
                seqLivePanel.setImage(seqOutputImage);
                parLivePanel.setImage(parOutputImage);
                seqLivePanel.repaint();
                parLivePanel.repaint();

                metricsTextArea.setText("Image loaded: " + selectedFile.getName() + "\n" +
                                        "Resolution: " + originalImage.getWidth() + "x" + originalImage.getHeight() + "\n" +
                                        "Ready for segmentation.");

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startSegmentation() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Please upload an image first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Disable buttons during computation
        setControlsEnabled(false);
        metricsTextArea.setText("Starting segmentation...\n");

        // Run in a separate thread to keep GUI responsive
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                int threshold = Integer.parseInt(thresholdField.getText());
                String type = (String) typeComboBox.getSelectedItem();
                int numThreads = Integer.parseInt(numThreadsField.getText());
                int delayMs = Integer.parseInt(delayMsField.getText());

                // Reset output images for a fresh run
                seqOutputImage = deepCopy(originalImage);
                parOutputImage = deepCopy(originalImage);
                seqLivePanel.setImage(seqOutputImage);
                parLivePanel.setImage(parOutputImage);
                seqLivePanel.repaint();
                parLivePanel.repaint();

                // Measure Memory Overhead before sequential
                Runtime runtime = Runtime.getRuntime();
                System.gc(); // Request GC
                long memUsedBeforeSeq = runtime.totalMemory() - runtime.freeMemory();

                metricsTextArea.append("Running sequential segmentation...\n");
                long startSeq = System.nanoTime();
                // Pass seqLivePanel for live updates
                SequentialSegmenter.segment(originalImage, seqOutputImage, threshold, type, delayMs, seqLivePanel);
                long endSeq = System.nanoTime();
                double seqTimeMs = (endSeq - startSeq) / 1e6;
                seqLivePanel.repaint(); // Final repaint

                System.gc(); // Request GC
                long memUsedAfterSeq = runtime.totalMemory() - runtime.freeMemory();


                metricsTextArea.append("Running parallel segmentation...\n");
                long startPar = System.nanoTime();
                // Pass parLivePanel for live updates
                Parralel_segmenter.segment(originalImage, parOutputImage, threshold, type, numThreads, parLivePanel, delayMs);
                long endPar = System.nanoTime();
                double parTimeMs = (endPar - startPar) / 1e6;
                parLivePanel.repaint(); // Final repaint

                System.gc(); // Request GC
                long memUsedAfterPar = runtime.totalMemory() - runtime.freeMemory();

                // Calculate Metrics
                double speedup = seqTimeMs / parTimeMs;
                // Approximate memory used *by the segmentation process* (change in heap usage)
                // This is a rough estimate due to GC and other JVM activities
                long seqMemFootprint = memUsedAfterSeq - memUsedBeforeSeq;
                long parMemFootprint = memUsedAfterPar - memUsedBeforeSeq;
                double memoryOverheadRatio = (seqMemFootprint == 0) ? 1.0 : (double) parMemFootprint / seqMemFootprint; // Avoid div by zero

                // Display Results
                SwingUtilities.invokeLater(() -> {
                    metricsTextArea.append("\n--- Segmentation Results ---\n");
                    metricsTextArea.append(String.format("Sequential Time: %.2f ms%n", seqTimeMs));
                    metricsTextArea.append(String.format("Parallel Time: %.2f ms (Threads: %d)%n", parTimeMs, numThreads));
                    metricsTextArea.append(String.format("Speed-up: %.2fx%n", speedup));
                    metricsTextArea.append(String.format("Memory Footprint (Approx): Sequential %.2f MB, Parallel %.2f MB%n",
                            seqMemFootprint / (1024.0 * 1024.0), parMemFootprint / (1024.0 * 1024.0)));
                    metricsTextArea.append(String.format("Memory Overhead Ratio: %.2fx%n", memoryOverheadRatio));

                    // Goals
                    if (numThreads >= 8) { // Assuming 8-core CPU goal
                        double idealSpeedup = (double) numThreads;
                        double efficiency = (speedup / idealSpeedup) * 100; // Percentage of ideal speedup
                        metricsTextArea.append(String.format("Efficiency (vs ideal speedup for %d cores): %.2f%%%n", numThreads, efficiency));
                        if (speedup >= 3.0) {
                            metricsTextArea.append("Goal: Speed-up >= 3x on 8-core CPU - ACHIEVED\n");
                        } else {
                            metricsTextArea.append("Goal: Speed-up >= 3x on 8-core CPU - NOT YET ACHIEVED (aim for 70% of ideal)\n");
                        }
                    } else {
                        metricsTextArea.append("Note: Speed-up goal evaluation applies best on an 8-core or higher CPU.\n");
                    }

                    if (memoryOverheadRatio <= 2.0) {
                        metricsTextArea.append("Goal: Memory Overhead <= 2x sequential footprint - ACHIEVED\n");
                    } else {
                        metricsTextArea.append("Goal: Memory Overhead <= 2x sequential footprint - NOT YET ACHIEVED\n");
                    }
                    metricsTextArea.append("CPU Utilization Goal: Aim for ≥ 85% during compute phase (requires external monitoring for true validation).\n");
                    metricsTextArea.append("Scalability Goal: Better with more cores (shown below in Scalability Test results).\n");


                    // Display combined image
                    displayCombinedImage(seqOutputImage, parOutputImage);
                });
                return null;
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                metricsTextArea.append("\nSegmentation complete.\n");
            }
        };
        worker.execute();
    }

    private void runScalabilityTest() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Please upload an image first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setControlsEnabled(false);
        metricsTextArea.setText("Running scalability test...\n");

        SwingWorker<List<String>, String> worker = new SwingWorker<List<String>, String>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                List<String> results = new ArrayList<>();
                int threshold = Integer.parseInt(thresholdField.getText());
                String type = (String) typeComboBox.getSelectedItem();
                int delayMs = 0; // Scalability test should run with no delay for accurate timing

                // Run sequential once for baseline
                BufferedImage currentSeqOutput = deepCopy(originalImage);
                publish("Running sequential baseline...\n");
                long startSeq = System.nanoTime();
                SequentialSegmenter.segment(originalImage, currentSeqOutput, threshold, type, delayMs, null); // No live panel for baseline
                long endSeq = System.nanoTime();
                double seqTimeMs = (endSeq - startSeq) / 1e6;
                results.add(String.format("Sequential Time (Baseline): %.2f ms%n", seqTimeMs));
                publish("Sequential baseline done.\n");


                publish("Starting parallel runs for scalability...\n");
                // Test with various thread counts (e.g., 1, 2, 4, 8, max_cores)
                int maxAvailableCores = Runtime.getRuntime().availableProcessors();
                // Ensure thread counts are sorted and unique
                List<Integer> threadCountsList = new ArrayList<>();
                threadCountsList.add(1);
                if (maxAvailableCores >= 2) threadCountsList.add(2);
                if (maxAvailableCores >= 4) threadCountsList.add(4);
                if (maxAvailableCores >= 8) threadCountsList.add(8);
                if (!threadCountsList.contains(maxAvailableCores)) { // Add max cores if not already included
                    threadCountsList.add(maxAvailableCores);
                }
                int[] threadCountsToTest = threadCountsList.stream().mapToInt(Integer::intValue).sorted().toArray();


                for (int numThreads : threadCountsToTest) {
                    if (numThreads == 0) continue; // Avoid 0 threads
                    publish(String.format("Testing with %d threads...\n", numThreads));

                    BufferedImage currentParOutput = deepCopy(originalImage);
                    long startPar = System.nanoTime();
                    Parralel_segmenter.segment(originalImage, currentParOutput, threshold, type, numThreads, null, delayMs); // Pass null for live panel for pure timing
                    long endPar = System.nanoTime();
                    double parTimeMs = (endPar - startPar) / 1e6;

                    double speedup = seqTimeMs / parTimeMs;
                    results.add(String.format("  Threads: %d, Parallel Time: %.2f ms, Speed-up: %.2fx%n",
                                              numThreads, parTimeMs, speedup));
                    results.add(String.format("  (Ideal Speed-up for %d cores: %.2fx)%n", numThreads, (double)numThreads));
                }
                return results;
            }

            @Override
            protected void process(List<String> chunks) {
                // Update GUI with progress
                for (String chunk : chunks) {
                    metricsTextArea.append(chunk);
                }
            }

            @Override
            protected void done() {
                try {
                    List<String> finalResults = get();
                    metricsTextArea.append("\n--- Scalability Test Results ---\n");
                    for (String result : finalResults) {
                        metricsTextArea.append(result);
                    }
                    metricsTextArea.append("\n**Scalability Note:** You should generally see speed-up increase with more cores until a **plateau** is reached. This occurs due to factors like Amdahl's Law (inherent sequential parts of the algorithm), diminishing returns from too many threads (context switching overhead), and the fixed overhead of setting up and managing parallel tasks.\n");
                } catch (Exception ex) {
                    metricsTextArea.append("Error during scalability test: " + ex.getMessage() + "\n");
                } finally {
                    setControlsEnabled(true);
                }
            }
        };
        worker.execute();
    }


    private void setControlsEnabled(boolean enabled) {
        uploadButton.setEnabled(enabled);
        thresholdField.setEnabled(enabled);
        typeComboBox.setEnabled(enabled);
        numThreadsField.setEnabled(enabled);
        delayMsField.setEnabled(enabled);
        startSegmentationButton.setEnabled(enabled);
        runScalabilityTestButton.setEnabled(enabled);
    }

    private BufferedImage deepCopy(BufferedImage bi) {
        if (bi == null) return null; // Handle null input
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    private void displayCombinedImage(BufferedImage seqImg, BufferedImage parImg) {
        if (seqImg == null || parImg == null) return;

        int width = seqImg.getWidth();
        int height = seqImg.getHeight();

        BufferedImage combinedImage = new BufferedImage(width * 2, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combinedImage.createGraphics();
        g.drawImage(seqImg, 0, 0, null);
        g.drawImage(parImg, width, 0, null);
        g.dispose();

        JFrame combinedFrame = new JFrame("Combined Output: Sequential vs Parallel");
        combinedFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JLabel combinedLabel = new JLabel(new ImageIcon(combinedImage));
        combinedFrame.add(combinedLabel);
        combinedFrame.pack();
        combinedFrame.setLocationRelativeTo(this); // Center relative to main GUI
        combinedFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SegmentationGUI());
    }
}