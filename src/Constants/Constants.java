package Constants;

import java.awt.Image;
import java.util.HashMap;
import java.util.HashSet;

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
        public static final int HEIGHT = 900;
        public static final int WIDTH = 500;

        public static final int TITLE_WIDTH = 400;
        public static final int TITLE_HEIGHT = 200;

        public static final int CARD_WIDTH = 50;
        public static final int CARD_HEIGHT = CARD_WIDTH * 3/2; // standard playing card aspect ratio 2.5:3.5

        public static final int RACE_TARGET_WIDTH = 200;
        public static final int RACE_TARGET_HEIGHT = 100;
    }

    public static class Images {
        public static final Image ROYAL_BATTLE_BG = new ImageIcon(
            "src\\Assets\\Royal_Battle_BG.jpg").getImage()
            .getScaledInstance(Dimensions.WIDTH, Dimensions.HEIGHT, Image.SCALE_DEFAULT);
        public static final Image DUO_TITLE = new ImageIcon(
            "src\\Assets\\Duo_Title.png").getImage()
            .getScaledInstance(Dimensions.TITLE_WIDTH, Dimensions.TITLE_HEIGHT, Image.SCALE_DEFAULT);
        public static final Image RACE_TARGET = new ImageIcon(
            "src\\Assets\\Duo_Title.png").getImage()
            .getScaledInstance(Dimensions.RACE_TARGET_WIDTH, Dimensions.RACE_TARGET_HEIGHT, Image.SCALE_DEFAULT);
        public static final Image MATCH_BG = new ImageIcon(
            "src\\Assets\\Match_BG.png").getImage()
            .getScaledInstance(Dimensions.WIDTH, Dimensions.HEIGHT, Image.SCALE_DEFAULT);
        // load images via ImageIcon synchronously and cache them
        private static final java.util.Map<String, Image> imageCache = new java.util.HashMap<>();

        public static final Image DUO_CARD_BACK = loadImage("src\\Assets\\Duo_Back.png");

        // cardName format: color_value e.g. red_5, blue_skip, green_reverse, yellow_draw2, wild_draw4
        // The image files are expected in src/Assets/DUO_CARDS/ with names like <cardName>_1.png
        public static Image getDUOCardImage(String cardName) throws IllegalArgumentException {
            if (cardName == null || cardName.isEmpty()) {
                throw new IllegalArgumentException("Invalid card name: " + cardName);
            }
            String path = "src\\Assets\\DUO_CARDS\\" + cardName + "_1.png";
            return loadImage(path);
        }

        private static Image loadImage(String path) {
            // simple cache to avoid reloading from disk every paint
            synchronized (imageCache) {
                Image img = imageCache.get(path);
                if (img != null) return img;
                try {
                    img = new ImageIcon(path).getImage();
                } catch (Exception e) {
                    img = null;
                }
                imageCache.put(path, img);
                return img;
            }
        }
    }
}