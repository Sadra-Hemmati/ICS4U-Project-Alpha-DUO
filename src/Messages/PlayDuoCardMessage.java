package Messages;

public class PlayDuoCardMessage {
    public int playerId;
    public DuoCard card;

    @Override
    public String toString() {
        return "PlayDuoCardMessage{" +
                "playerId=" + playerId +
                ", card=" + card +
                '}';
    }
}
