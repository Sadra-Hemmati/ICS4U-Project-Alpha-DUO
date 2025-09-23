package GUI.Panels;

import javax.imageio.ImageIO;
import javax.swing.*;

import Constants.Constants.Dimensions;
import Constants.Constants.Images;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

public class HomePanel extends JPanel{
    private final JLabel titleLabel;
    private final JButton hostButton, joinButton;
    private final ScrollingImagePanel topScrollPanel = new ScrollingImagePanel("/Assets/Duo_Back.png", true);
    private final ScrollingImagePanel bottomScrollPanel = new ScrollingImagePanel("/Assets/Duo_Back.png", false);


    public HomePanel() {
        setLayout(new BorderLayout(0, 50));
        setMaximumSize(new Dimension(Dimensions.WIDTH, Dimensions.HEIGHT));

        titleLabel = new JLabel(new ImageIcon(Images.DUO_TITLE));
        titleLabel.setVisible(true);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBackground(new Color(0, 0, 0, 0));
        hostButton = new JButton("Host Match");
        joinButton = new JButton("Join Match");
        buttonsPanel.add(hostButton);
        buttonsPanel.add(joinButton);
        // buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setAlignmentX(CENTER_ALIGNMENT);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(titleLabel, BorderLayout.NORTH);
        centerPanel.add(buttonsPanel, BorderLayout.CENTER);


        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(centerPanel, new GridBagConstraints());

        add(topScrollPanel, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);
        add(bottomScrollPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    public JButton getHostButton() { return hostButton; }
    public JButton getJoinButton() { return joinButton; }

    Image backgroundImage = Images.ROYAL_BATTLE_BG;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // g.drawImage(backgroundImage, 0, 0, this);
    }

    
    public class ScrollingImagePanel extends JPanel implements ActionListener {

        private BufferedImage image;
        private int scrollX = 0;
        private int scrollSpeed = 2; // positive = left, negative = right
        private Timer timer;
        private int preferredHeight = 100;

        public ScrollingImagePanel(String imagePath, boolean scrollLeft) {
            try {
                // resourcePath should look like "/Assets/yourImage.png"
                var stream = getClass().getResourceAsStream(imagePath);
                if (stream == null) {
                    throw new IllegalArgumentException("Resource not found: " + imagePath);
                }
                BufferedImage original = ImageIO.read(stream);

                // Rotate 90 degrees clockwise
                image = transform(original);

                if (!scrollLeft) {
                    scrollSpeed = -scrollSpeed;
                }

                timer = new Timer(16, this); // ~60 FPS
                timer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private BufferedImage transform(BufferedImage src) {
            int w = src.getWidth();
            int h = src.getHeight();
            BufferedImage rotated = new BufferedImage(h, w, src.getType());
            Graphics2D g2d = rotated.createGraphics();
            AffineTransform at = new AffineTransform();
            at.translate(h, 0);
            at.rotate(Math.toRadians(90));
            g2d.drawImage(src, at, null);
            g2d.dispose();
            return scaleToHeight(rotated, preferredHeight);
        }

        private BufferedImage scaleToHeight(BufferedImage src, int targetHeight) {
            int srcW = src.getWidth();
            int srcH = src.getHeight();
            double scale = (double) targetHeight / srcH;
            int newW = (int) (srcW * scale);

            BufferedImage scaled = new BufferedImage(newW, targetHeight, src.getType());
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(src, 0, 0, newW, targetHeight, null);
            g2d.dispose();
            return scaled;
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (image == null) return;

            int imgW = image.getWidth() + 20;
            int imgH = image.getHeight();
            int panelW = getWidth();

            // Ensure scrollX wraps around
            if (scrollSpeed > 0) { // moving left
                if (scrollX <= -imgW) scrollX += imgW;
            } else { // moving right
                if (scrollX >= imgW) scrollX -= imgW;
            }

            // Draw enough images to cover the panel
            for (int x = scrollX - imgW; x < panelW; x += imgW) {
                g.drawImage(image, x, (getHeight() - imgH) / 2, null);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            scrollX -= scrollSpeed; // move
            repaint();
        }
        
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, preferredHeight);
        }
    }
}