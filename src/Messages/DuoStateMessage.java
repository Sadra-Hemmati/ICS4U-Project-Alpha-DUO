package Messages;

import java.util.HashMap;
import java.util.List;

public class DuoStateMessage {
    public int currentPlayerId;
    public Messages.DuoCard topCard;
    public List<List<Messages.DuoCard>> hands; // hand sizes per player
    public boolean duoRace = false;
    public int winnerId = -1; // if someone has won the game, their id, otherwise -1
}
