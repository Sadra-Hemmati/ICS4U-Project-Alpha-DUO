package Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import Constants.Constants;
import Util.Helpers;
import Util.Log;

/**
 * Lightweight GameServer wrapper that keeps a map of connection IDs -> usernames.
 * Clients should send a String starting with "USERNAME:" followed by the username
 * right after connecting; this server will record that and expose the list.
 */
public class GameServer {
    private final Server server;
    private final ConcurrentHashMap<Integer, String> connectionNames = new ConcurrentHashMap<>();
    // mapping from connection id -> player index used by DuoMatch
    private final ConcurrentHashMap<Integer, Integer> connectionPlayerId = new ConcurrentHashMap<>();
    private Game.DuoMatch duoMatch = null;

    public GameServer() {
        server = new Server();
        server.start();

        Log.i("GameServer", "server started");

        try {
            server.bind(Constants.NetworkConstants.tcpPort, Constants.NetworkConstants.udpPort);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        Helpers.registerClasses(server.getKryo());

        server.addListener(new Listener() {

            @Override
            public void received(Connection connection, Object object) {
                // simple username protocol via String
                if (object instanceof String) {
                    String s = (String) object;
                    if (s.startsWith("USERNAME:")) {
                        String name = s.substring("USERNAME:".length());
                        connectionNames.put(connection.getID(), name);
                        Log.d("GameServer", "registered username '" + name + "' for conn=" + connection.getID());
                    }
                    return;
                }

                // Start UNO match
                if (object instanceof Messages.StartDuoMatchMessage) {
                    Log.i("GameServer", "received StartUnoMatchMessage -> starting match");
                    startUnoMatch();
                    return;
                }

                // Play card
                if (object instanceof Messages.PlayDuoCardMessage) {
                    Messages.PlayDuoCardMessage pcm = (Messages.PlayDuoCardMessage) object;
                    Integer pid = connectionPlayerId.get(connection.getID());
                    if (pid != null && duoMatch != null) {
                        // attempt to play; server authoritative
                        if(duoMatch.playCard(pid, pcm.card)) {
                            Log.d("GameServer", "player " + pid + " played " + pcm.card.toString() + " (conn=" + connection.getID() + ")");
                        }
                        else {
                            Log.d("GameServer", "player " + pid + " attempted invalid play of " + pcm.card.toString() + " (conn=" + connection.getID() + ")");
                        }
                        broadcastDuoState();
                    }
                    return;
                }

                // Draw card request
                if (object instanceof Messages.DrawDuoCardMessage) {
                    Messages.DrawDuoCardMessage dm = (Messages.DrawDuoCardMessage) object;
                    Integer pid = connectionPlayerId.get(connection.getID());
                    if (pid != null && duoMatch != null) {
                        for (int i = 0; i < dm.count; i++) {
                            Messages.DuoCard c = duoMatch.draw();
                            if (c != null) {
                                duoMatch.getPlayerHand(pid).add(c);
                            }
                        }
                    }
                    Log.d("GameServer", "player " + pid + " drew " + dm.count + " cards (conn=" + connection.getID() + ")");
                        
                    broadcastDuoState();
                    return;
                }

                if (object instanceof Messages.SkipDuoTurnMessage) {
                    Messages.SkipDuoTurnMessage sm = (Messages.SkipDuoTurnMessage) object;
                    Integer pid = connectionPlayerId.get(connection.getID());
                    if (pid != null && duoMatch != null) {
                        if(sm.playerId == pid){
                            duoMatch.advanceTurn();
                            Log.d("GameServer", "player " + pid + " skipped their turn (conn=" + connection.getID() + ")");
                            broadcastDuoState();
                        }
                    }
                    return;
                }

                if (object instanceof Messages.WonDuoRaceMessage) {
                    Messages.WonDuoRaceMessage wm = (Messages.WonDuoRaceMessage) object;
                    Integer pid = connectionPlayerId.get(connection.getID());
                    if (pid != null && duoMatch != null) {
                        if(wm.playerId == pid){
                            duoMatch.winDuoRace(pid);
                            Log.d("GameServer", "player " + pid + " won the Duo Race (conn=" + connection.getID() + ")");
                            broadcastDuoState();
                        }
                    }
                    return;
                }

            }

            @Override
            public void connected(Connection connection) {
                if (server.getConnections().length > 4) {
                    System.out.println("Rejecting connection with ID " + connection.getID() + ": server full.");
                    kickConnection(connection, "SERVER FULL");
                }
                if (duoMatch != null) {
                    System.out.println("Rejecting connection with ID " + connection.getID() + ": match already in progress.");
                    kickConnection(connection, "MATCH ALREADY IN PROGRESS");
                    return;
                }
                // nothing immediate; wait for USERNAME string
                connection.sendTCP("CONNECTION ACCEPTED");
                Log.i("GameServer", "connection connected id=" + connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {
                connectionNames.remove(connection.getID());
                connectionPlayerId.remove(connection.getID());
                Log.i("GameServer", "connection disconnected id=" + connection.getID());
            }
        });
    }

    public String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getConnectedUsernames() {
        return new ArrayList<>(connectionNames.values());
    }

    public int getConnectedCount() {
        return connectionNames.size();
    }

    public void close() {
        server.close();
        connectionNames.clear();
    }

    public void kickConnection(Connection connection, String msg) {
        if (connection != null) {
            connection.sendTCP(msg);
            connection.close();
        }
    }

    private synchronized void startUnoMatch() {
        if (duoMatch != null) return; // already running

        // Determine active connections (use server.getConnections() so we include any connected clients
        // whether or not they've yet sent a USERNAME string).
        com.esotericsoftware.kryonet.Connection[] conns = server.getConnections();
        
        if (conns.length > 4) {
            for (int i = 4; i < conns.length; i++) {
                System.out.println("Kicking extra player: " + conns[i].getID());
                kickConnection(conns[i], "SERVER FULL");
            }
        }

        // if (players < 2) players = Math.max(2, players); // ensure at least 2
        duoMatch = new Game.DuoMatch(conns.length);

        // assign player ids to current connections deterministically in the order returned by server.getConnections()
        connectionPlayerId.clear();
        int id = 0;
        for (com.esotericsoftware.kryonet.Connection c : conns) {
            connectionPlayerId.put(c.getID(), id);
            id++;
        }

        Log.d("GameServer", "assigned player ids: " + connectionPlayerId.toString());

        // send start message per-connection so each client learns its assigned player id
        long now = System.currentTimeMillis();
        for (com.esotericsoftware.kryonet.Connection c : conns) {
            int connId = c.getID();
            Messages.StartDuoMatchMessage msg = new Messages.StartDuoMatchMessage();
            msg.serverStartTimeMillis = now;
            Integer assigned = connectionPlayerId.get(connId);
            msg.assignedPlayerId = (assigned == null) ? -1 : assigned;
            // include the starting hand for that player so client can render its cards
            if (duoMatch != null && msg.assignedPlayerId >= 0) {
                java.util.List<Messages.DuoCard> hand = duoMatch.getPlayerHand(msg.assignedPlayerId);
                msg.startingHand = new java.util.ArrayList<>(hand);
            }
            server.sendToTCP(connId, msg);
            Log.d("GameServer", "sent StartUnoMatchMessage to conn=" + connId + " assignedPlayerId=" + msg.assignedPlayerId + " startingHandSize=" + (msg.startingHand==null?0:msg.startingHand.size()));
        }

        // broadcast initial state
        broadcastDuoState();
    }

    private synchronized void broadcastDuoState() {
        if (duoMatch == null) return;
        Messages.DuoStateMessage s = new Messages.DuoStateMessage();
        s.currentPlayerId = duoMatch.getCurrentPlayer();
        s.topCard = duoMatch.getTopCard();
        Log.d("GameServer", "broadcasting DuoState: currentPlayer=" + s.currentPlayerId + " topCard=" + (s.topCard==null?"null":s.topCard.toString()));
        s.hands = duoMatch.getHands();
        s.duoRace = duoMatch.isDuoRace();
        s.winnerId = duoMatch.getWinnerId();
        server.sendToAllTCP(s);
    }
}