package GUI;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.*;

import Constants.Constants.Dimensions;
import Constants.Constants.Images;

public class DuoRaceTarget extends JComponent {

    private double scale = 1.0;
    private double time = 0;           // a time counter for sine wave
    private final double frequency = 0.17; // speed of pulsation
    private final double minScale = 0.7;
    private final double maxScale = 1.3;
    public final Dimension maxSize = new Dimension((int)(Dimensions.RACE_TARGET_WIDTH * maxScale), (int)(Dimensions.RACE_TARGET_HEIGHT * maxScale));

    public DuoRaceTarget() {
        setSize(Dimensions.RACE_TARGET_WIDTH, Dimensions.RACE_TARGET_HEIGHT);
        setPreferredSize(getSize());

        Timer timer = new Timer(30, e -> {
            time += 1;

            // Sine wave oscillation between minScale and maxScale
            scale = minScale + (maxScale - minScale) * (0.5 * (1 + Math.sin(time * frequency)));

            // Update preferred size (optional if you want layout to adjust)
            int newWidth = (int)(Dimensions.RACE_TARGET_WIDTH * scale);
            int newHeight = (int)(Dimensions.RACE_TARGET_HEIGHT * scale);
            setPreferredSize(new Dimension(newWidth, newHeight));
            revalidate();
            repaint();
        });

        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int drawWidth = (int)(Dimensions.RACE_TARGET_WIDTH * scale);
        int drawHeight = (int)(Dimensions.RACE_TARGET_HEIGHT * scale);

        int x = (getWidth() - drawWidth) / 2;
        int y = (getHeight() - drawHeight) / 2;

        g.drawImage(Images.RACE_TARGET, x, y, drawWidth, drawHeight, this);
    }
}
