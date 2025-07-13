package Project;

import java.awt.Color;
import java.awt.image.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Parralel_segmenter {
    private static final Set<String> threadNames = ConcurrentHashMap.newKeySet();
    private static LiveImageDisplay livePanelRef; // Note: This will be null for scalability test runs
    private static int LEAF_TASK_THRESHOLD;

    private static class SegmentTask extends RecursiveAction {
        private final Raster inputRaster;
        private final WritableRaster outputRaster;
        private final int startY, endY;
        private final int threshold;
        private final String type;
        private final AtomicInteger processedRowsCounter;
        private final int delayMs;

        public SegmentTask(Raster inputRaster, WritableRaster outputRaster, int startY, int endY,
                           int threshold, String type, AtomicInteger processedRowsCounter, int delayMs) {
            this.inputRaster = inputRaster;
            this.outputRaster = outputRaster;
            this.startY = startY;
            this.endY = endY;
            this.threshold = threshold;
            this.type = type;
            this.processedRowsCounter = processedRowsCounter;
            this.delayMs = delayMs;
        }

        @Override
        protected void compute() {
            threadNames.add(Thread.currentThread().getName());

            if (endY - startY <= LEAF_TASK_THRESHOLD) {
                int width = inputRaster.getWidth();
                int numBands = inputRaster.getNumBands();
                int[] pixels = new int[numBands];
                int[] newPixel = new int[numBands];

                for (int y = startY; y < endY; y++) {
                    for (int x = 0; x < width; x++) {
                        inputRaster.getPixel(x, y, pixels);

                        int r = pixels[0];
                        int g = pixels[1];
                        int b = pixels[2];
                        int alpha = (numBands == 4) ? pixels[3] : 255; // Handle alpha if present

                        int[] result;
                        switch (type) {
                            case "grayscale":
                                int avg = (r + g + b) / 3;
                                int bin = (avg < threshold) ? 0 : 255;
                                result = new int[]{bin, bin, bin, alpha};
                                break;
                            case "red":
                                result = (r > g && r > b && r > threshold) ? new int[]{255, 0, 0, alpha} : new int[]{0, 0, 0, alpha};
                                break;
                            case "green":
                                result = (g > r && g > b && g > threshold) ? new int[]{0, 255, 0, alpha} : new int[]{0, 0, 0, alpha};
                                break;
                            case "custom":
                                result = (r > 100 && g < 150 && b > 50) ? new int[]{r, g, b, alpha} : new int[]{0, 0, 0, alpha};
                                break;
                            default:
                                result = new int[]{0, 0, 0, alpha};
                                break;
                        }
                        System.arraycopy(result, 0, newPixel, 0, numBands);
                        outputRaster.setPixel(x, y, newPixel);
                    }

                    int currentProcessedRows = processedRowsCounter.incrementAndGet();

                    // Repaint and delay for every 10 globally processed rows.
                    // This is a heuristic to balance update frequency and performance.
                    if (currentProcessedRows % 10 == 0 && livePanelRef != null) {
                        livePanelRef.repaint();

                        if (delayMs > 0) {
                            try {
                                Thread.sleep(delayMs);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                System.err.println("SegmentTask was interrupted during delay.");
                                return;
                            }
                        }
                    }
                }
            } else {
                int mid = startY + (endY - startY) / 2;
                SegmentTask leftTask = new SegmentTask(inputRaster, outputRaster, startY, mid, threshold, type, processedRowsCounter, delayMs);
                SegmentTask rightTask = new SegmentTask(inputRaster, outputRaster, mid, endY, threshold, type, processedRowsCounter, delayMs);
                invokeAll(leftTask, rightTask);
            }
        }
    }

    public static void segment(BufferedImage input, BufferedImage output, int threshold,
                               String type, int numThreads, LiveImageDisplay panel, int delayMs) {

        Raster inputRaster = input.getRaster();
        WritableRaster outputRaster = output.getRaster();
        threadNames.clear();
        livePanelRef = panel; // Store reference to the panel (can be null for scalability test)

        int imageHeight = input.getHeight();
        // Calculate LEAF_TASK_THRESHOLD dynamically, ensuring it's at least 10 rows
        LEAF_TASK_THRESHOLD = Math.max(10, imageHeight / (numThreads * 4));

        AtomicInteger processedRowsCounter = new AtomicInteger(0);

        ForkJoinPool pool = new ForkJoinPool(numThreads);

        pool.invoke(new SegmentTask(inputRaster, outputRaster, 0, imageHeight, threshold, type, processedRowsCounter, delayMs));

        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("ForkJoinPool did not terminate in 60 seconds.");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.err.println("Parallel segmentation interrupted while waiting for tasks to complete.");
        }

        livePanelRef = null; // Clear reference after completion
    }
}