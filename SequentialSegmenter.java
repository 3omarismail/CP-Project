package Project;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class SequentialSegmenter {
    public static void segment(BufferedImage input, BufferedImage output, int threshold, String type, int delayMs, LiveImageDisplay panel) {
        int width = input.getWidth();
        int height = input.getHeight();

        WritableRaster inputRaster = input.getRaster();
        WritableRaster outputRaster = output.getRaster();

        int numBands = inputRaster.getNumBands();
        int[] pixels = new int[numBands];
        int[] newPixel = new int[numBands];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                inputRaster.getPixel(x, y, pixels);

                int r = pixels[0];
                int g = pixels[1];
                int b = pixels[2];
                int alpha = (numBands == 4) ? pixels[3] : 255;

                int[] result;
                switch (type) {
                    case "grayscale" -> {
                        int avg = (r + g + b) / 3;
                        int binary = (avg < threshold) ? 0 : 255;
                        result = new int[]{binary, binary, binary, alpha};
                    }
                    case "red" -> result = (r > g && r > b && r > threshold) ? new int[]{255, 0, 0, alpha} : new int[]{0, 0, 0, alpha};
                    case "green" -> result = (g > r && g > b && g > threshold) ? new int[]{0, 255, 0, alpha} : new int[]{0, 0, 0, alpha};
                    case "custom" -> result = (r > 100 && g < 150 && b > 50) ? new int[]{r, g, b, alpha} : new int[]{0, 0, 0, alpha};
                    default -> result = new int[]{0, 0, 0, alpha};
                };

                System.arraycopy(result, 0, newPixel, 0, numBands);
                outputRaster.setPixel(x, y, newPixel);
            }
            // Add delay and repaint here for visualization
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Sequential segmentation was interrupted during delay.");
                    return;
                }
            }
            // Trigger repaint on the sequential live panel after each row
            if (panel != null) {
                panel.repaint();
            }
        }
    }
}