package Network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import com.esotericsoftware.kryonet.*;

import Constants.Constants;
import Constants.Constants.NetworkConstants;
import Util.Helpers;
import Util.Log;

public class GameClient {
    private final Client client;
    // simple callback interfaces for UNO messages
    public interface DuoStateHandler { void onState(Messages.DuoStateMessage m); }
    public interface DuoStartHandler { void onStart(Messages.StartDuoMatchMessage m); }
    

    // allow multiple listeners so UI and other components can all react to messages
    private final java.util.List<DuoStateHandler> stateHandlers = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<DuoStartHandler> startHandlers = new java.util.concurrent.CopyOnWriteArrayList<>();

    public GameClient() {
        client = new Client();
        Helpers.registerClasses(client.getKryo());
        client.start();
        Log.i("GameClient", "client created");
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Messages.DuoStateMessage) {
                    Messages.DuoStateMessage msg = (Messages.DuoStateMessage) object;
                    for (DuoStateHandler h : stateHandlers) {
                        try { h.onState(msg); } catch (Exception ignored) {}
                    }
                }
                if (object instanceof Messages.StartDuoMatchMessage) {
                    Messages.StartDuoMatchMessage msg = (Messages.StartDuoMatchMessage) object;
                    Log.d("GameClient", "received StartUnoMatchMessage assignedPlayerId=" + msg.assignedPlayerId + " startingHandSize=" + (msg.startingHand==null?0:msg.startingHand.size()));
                    for (DuoStartHandler h : startHandlers) {
                        try { h.onStart(msg); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    public void connectLocal() throws IOException{
        connect("localhost");
    }

    public void connect(String address) throws IOException{
        client.connect(5000, address, Constants.NetworkConstants.tcpPort, Constants.NetworkConstants.udpPort);
        Log.i("GameClient", "connected to " + address);
    }

    public void connect(InetAddress address) throws IOException{
        client.connect(5000, address, Constants.NetworkConstants.tcpPort, Constants.NetworkConstants.udpPort);   
    }

    public void setName(String name) {
        // Inform server of username using a simple String protocol
        client.setName(name);
        client.sendTCP("USERNAME:" + name);
    } 

    public List<InetAddress> getLanAdresses() {
        return client.discoverHosts(Constants.NetworkConstants.udpPort, 5000);
    }

    public void close() {
        try {
            client.stop();
        } catch (Exception ignored) {}
    }

    // Backwards-compatible single-set semantics
    public void setUnoStateHandler(DuoStateHandler h) {
        stateHandlers.clear();
        if (h != null) stateHandlers.add(h);
    }
    public void setUnoStartHandler(DuoStartHandler h) {
        startHandlers.clear();
        if (h != null) startHandlers.add(h);
    }

    // Prefer these: register additional listeners without replacing existing ones
    public void addUnoStateHandler(DuoStateHandler h) { if (h != null && !stateHandlers.contains(h)) stateHandlers.add(h); }
    public void removeUnoStateHandler(DuoStateHandler h) { if (h != null) stateHandlers.remove(h); }
    public void addUnoStartHandler(DuoStartHandler h) { if (h != null && !startHandlers.contains(h)) startHandlers.add(h); }
    public void removeUnoStartHandler(DuoStartHandler h) { if (h != null) startHandlers.remove(h); }

    public boolean isRunning() {
        // Client has an internal thread; assume "started" if not null. There's no direct isRunning API so
        // we approximate by checking whether the tcp/udp ports are still open via exceptions would surface on connect.
        // For our usage this simple check is sufficient.
        return client != null;
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void sendTCP(Object obj) {
        try { client.sendTCP(obj); } catch (Exception ignored) {}
    }
}
