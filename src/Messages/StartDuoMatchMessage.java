package Messages;

public class StartDuoMatchMessage {
    public long serverStartTimeMillis;
    // If sent to a specific client, the server may set this to tell the client its player index.
    public int assignedPlayerId = -1;
    // initial private hand for the receiving client (may be null)
    public java.util.ArrayList<DuoCard> startingHand;
}
