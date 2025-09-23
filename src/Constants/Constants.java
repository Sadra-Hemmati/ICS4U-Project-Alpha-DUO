package Constants;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.ImageIcon;

public class Constants {
    public static class NetworkConstants {

        public static final int tcpPort = 54555;
        public static final int udpPort = 54777;

        //any class that is to be sent overthe network needs to be put here to be registered
        public static final Class<?>[] registeredClasses = {
            String.class,
            // UNO messages
            Messages.DuoCard.class,
            Messages.PlayDuoCardMessage.class,
            Messages.DrawDuoCardMessage.class,
            Messages.DuoStateMessage.class,
            Messages.StartDuoMatchMessage.class,
            Messages.SkipDuoTurnMessage.class,
            Messages.WonDuoRaceMessage.class,
            java.util.ArrayList.class
        };
    }

    public static class Dimensions {
        private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        public static final int HEIGHT = Math.min(900, screenSize.height - 200);
        public static final int WIDTH = Math.min(500, screenSize.width - 200);

        public static final int TITLE_WIDTH = 400;
        public static final int TITLE_HEIGHT = 200;

        public static final int CARD_WIDTH = 50;
        public static final int CARD_HEIGHT = CARD_WIDTH * 3/2; // standard playing card aspect ratio 2.5:3.5

        public static final int RACE_TARGET_WIDTH = 200;
        public static final int RACE_TARGET_HEIGHT = 100;
    }

    public final class Images {

        private Images() {} // prevent instantiation

        // cache to avoid reloading the same image
        private static final Map<String, Image> imageCache = new HashMap<>();

        public static final Image ROYAL_BATTLE_BG = loadImage("/Assets/Royal_Battle_BG.jpg")
                .getScaledInstance(Dimensions.WIDTH, Dimensions.HEIGHT, Image.SCALE_DEFAULT);

        public static final Image DUO_TITLE = loadImage("/Assets/Duo_Title.png")
                .getScaledInstance(Dimensions.TITLE_WIDTH, Dimensions.TITLE_HEIGHT, Image.SCALE_DEFAULT);

        public static final Image RACE_TARGET = loadImage("/Assets/Duo_Title.png")
                .getScaledInstance(Dimensions.RACE_TARGET_WIDTH, Dimensions.RACE_TARGET_HEIGHT, Image.SCALE_DEFAULT);

        public static final Image MATCH_BG = loadImage("/Assets/Match_BG.png")
                .getScaledInstance(Dimensions.WIDTH, Dimensions.HEIGHT, Image.SCALE_DEFAULT);

        public static final Image DUO_CARD_BACK = loadImage("/Assets/Duo_Back.png");

        // Card images, format: "red_5", "blue_skip", "wild_draw4", etc.
        public static Image getDUOCardImage(String cardName) {
            if (cardName == null || cardName.isEmpty()) {
                throw new IllegalArgumentException("Invalid card name: " + cardName);
            }
            String path = "/Assets/DUO_CARDS/" + cardName + "_1.png";
            return loadImage(path);
        }

        private static Image loadImage(String path) {
            synchronized (imageCache) {
                Image img = imageCache.get(path);
                if (img != null) return img;

                try {
                    var url = Images.class.getResource(path);
                    if (url == null) {
                        throw new IllegalArgumentException("Resource not found: " + path);
                    }
                    img = new ImageIcon(url).getImage();
                } catch (Exception e) {
                    System.err.println("Failed to load image: " + path);
                    img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB); // fallback
                }

                imageCache.put(path, img);
                return img;
            }
        }
    }
}