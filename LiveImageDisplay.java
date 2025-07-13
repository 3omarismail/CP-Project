package Project;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LiveImageDisplay extends JPanel {
    private BufferedImage image;

    public LiveImageDisplay(BufferedImage image) {
        this.image = image;
        if (image != null) {
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        } else {
            setPreferredSize(new Dimension(300, 200)); // Default size if no image is provided initially
        }
    }

    public void setImage(BufferedImage newImage) {
        synchronized (this) {
            this.image = newImage;
        }
        if (newImage != null && (getPreferredSize().width != newImage.getWidth() || getPreferredSize().height != newImage.getHeight())) {
            setPreferredSize(new Dimension(newImage.getWidth(), newImage.getHeight()));
            revalidate();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized (this) {
            if (image != null) {
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null); // Scale image to panel size
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.BLACK);
                String text = "Image Not Loaded";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(text, x, y);
            }
        }
    }
}