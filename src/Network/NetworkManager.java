package Network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * Application-wide network manager. Keeps a single GameServer and GameClient
 * instance so the host can run a server and a local client, and UI panels
 * reuse the same client instance.
 */
public class NetworkManager {
    private static NetworkManager instance;

    private GameServer server;
    private GameClient client;
    // listeners to be notified when a new client instance is created
    public interface ClientCreatedListener { void onClientCreated(GameClient client); }
    private java.util.List<ClientCreatedListener> clientCreatedListeners = new java.util.ArrayList<>();

    private NetworkManager() {}

    public static synchronized NetworkManager getInstance() {
        if (instance == null) instance = new NetworkManager();
        return instance;
    }

    public synchronized GameServer startServer() {
        if (server == null) {
            server = new GameServer();
            Util.Log.i("NetworkManager", "server started via NetworkManager.startServer()");
        }
        return server;
    }

    public synchronized void stopServer() {
        if (server != null) {
            server.close();
            server = null;
            Util.Log.i("NetworkManager", "server stopped");
        }
    }

    public synchronized GameServer getServer() { return server; }

    public synchronized GameClient getClient() { return client; }

    /**
     * Start a local client that connects to the locally-hosted server and sets the username.
     */
    public synchronized GameClient startLocalClient(String username) throws IOException {
        if (client == null) {
            client = new GameClient();
            client.connect("localhost");
            client.setName(username);
            Util.Log.i("NetworkManager", "started local client username='" + username + "'");
        }
        // notify listeners
        for (ClientCreatedListener l : clientCreatedListeners) try { l.onClientCreated(client); } catch (Exception ignored) {}
        return client;
    }

    public synchronized void stopClient() {
        if (client != null) {
            client.close();
            client = null;
            Util.Log.i("NetworkManager", "client stopped");
        }
    }

    /**
     * Discover LAN hosts (will create a temporary client if needed).
     */
    public synchronized List<InetAddress> discoverHosts() {
        if (client == null) {
            client = new GameClient();
            for (ClientCreatedListener l : clientCreatedListeners) try { l.onClientCreated(client); } catch (Exception ignored) {}
            Util.Log.d("NetworkManager", "created temporary client for discovery");
        }
        return client.getLanAdresses();
    }

    public synchronized void connectTo(InetAddress address, String username) throws IOException {
        if (client == null) client = new GameClient();
        client.connect(address);
        client.setName(username);
        for (ClientCreatedListener l : clientCreatedListeners) try { l.onClientCreated(client); } catch (Exception ignored) {}
    }

    public synchronized void connectTo(String host, String username) throws IOException {
        if (client == null) client = new GameClient();
        client.connect(host);
        client.setName(username);
        for (ClientCreatedListener l : clientCreatedListeners) try { l.onClientCreated(client); } catch (Exception ignored) {}
    }

    public synchronized void addClientCreatedListener(ClientCreatedListener l) { clientCreatedListeners.add(l); }
    public synchronized void removeClientCreatedListener(ClientCreatedListener l) { clientCreatedListeners.remove(l); }
}
