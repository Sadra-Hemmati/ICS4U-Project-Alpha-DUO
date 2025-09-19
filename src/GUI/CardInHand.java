package GUI;

//represents a playable card in the player's hand, uses the same GUI copde as Card 
//but has additional functionality for being in the hand
import Util.Log;

public class CardInHand extends Card {
    public Messages.DuoCard cardData;
    private boolean selected = false;

    public CardInHand(Messages.DuoCard data) {
        this.cardData = data;
        setSize(Constants.Constants.Dimensions.CARD_WIDTH, Constants.Constants.Dimensions.CARD_HEIGHT);
        setPreferredSize(getSize());
    }

    public Messages.DuoCard getCardData() { return cardData; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean s) { selected = s; repaint(); }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        // draw the card image (log if missing)
        java.awt.Image img = null;
        try {
            if (cardData.color.equals("WILD")) {
                img = Constants.Constants.Images.getDUOCardImage(
                    cardData.color.toLowerCase() + (cardData.value.equals("WILD4")? "4" : ""));
            }
            else if (cardData.value.equals("0")) {
                img = Constants.Constants.Images.getDUOCardImage(
                    cardData.color.toLowerCase() + "_" + cardData.value.toLowerCase());
            }
            else{
                img = Constants.Constants.Images.getDUOCardImage(
                    cardData.color.toLowerCase() + "_" + cardData.value.toLowerCase());
            }
        } catch (Exception ex) {
            img = null;
        }
        if (img != null) {
            // Log.d("CardInHand", "paint: drawing " + cardData.color + "_" + cardData.value + "; w=" + getWidth() + ",h=" + getHeight());
            g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
        } else {
            // Log.d("CardInHand", "paint: img==null for " + cardData.color + "_" + cardData.value + "; w=" + getWidth() + ",h=" + getHeight());
            // fallback: draw colored rectangle with text
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(java.awt.Color.BLACK);
            g.drawRect(0,0,getWidth()-1,getHeight()-1);
            g.drawString(cardData.color + " " + cardData.value, 5, 15);
        }
        if (selected) {
            g.setColor(java.awt.Color.BLACK);
            ((java.awt.Graphics2D)g).setStroke(new java.awt.BasicStroke(3));
            g.drawRect(1,1,getWidth()-3,getHeight()-3);
        }
    }
}
