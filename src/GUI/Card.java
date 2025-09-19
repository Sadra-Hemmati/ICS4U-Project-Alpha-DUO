package GUI;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;

//represents a card in the game, either in the player's hand or on the table
public class Card extends JComponent {
    public Card() {
        setSize(Constants.Constants.Dimensions.CARD_WIDTH, Constants.Constants.Dimensions.CARD_HEIGHT);
        setPreferredSize(getSize());
        // default: clickable but no drag behavior for now
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // subclasses may override
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // default: draw an empty card placeholder
        g.setColor(java.awt.Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(java.awt.Color.DARK_GRAY);
        g.drawRect(0, 0, getWidth()-1, getHeight()-1);
    }

    // create a back-facing card (for opponents)
    public static Card createBack() {
        return new Card() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image img = Constants.Constants.Images.DUO_CARD_BACK;
                if (img != null) g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                else {
                    g.setColor(java.awt.Color.BLUE);
                    g.fillRect(0,0,getWidth(),getHeight());
                }
            }
        };
    }
}