package GUI.Panels;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.util.List;

import Constants.Constants.Dimensions;
import Network.NetworkManager;
import Network.GameServer;

public class HostPanel extends JPanel {
    private final JButton backButton = new JButton("Back to Home");
    public JButton getBackButton() { return backButton; }

    private final JButton startButton = new JButton("Start Server");
    private final JButton stopButton = new JButton("Stop Server");
    private final JButton startMatchButton = new JButton("Start Match");
    private final JLabel ipLabel = new JLabel("Server IP: -");
    private final DefaultListModel<String> playersModel = new DefaultListModel<>();
    private final JList<String> playersList = new JList<>(playersModel);
    private final JTextField usernameField = new JTextField(12);

    private String hostName = null;
    private Runnable onStartMatch = null;
    public void setOnStartMatch(Runnable r) { this.onStartMatch = r; }

    public HostPanel() {
        setLayout(new BorderLayout(0, 10));
        setMaximumSize(new Dimension(Dimensions.WIDTH, Dimensions.HEIGHT));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Host a Match"));
        top.add(ipLabel);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userRow.add(new JLabel("Your username:"));
        userRow.add(usernameField);
        center.add(userRow);

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsRow.add(startButton);
        buttonsRow.add(stopButton);
        center.add(buttonsRow);

        JLabel connectedPlayesHeader = new JLabel("Connected players (max 2):");
        connectedPlayesHeader.setAlignmentX(CENTER_ALIGNMENT);

        center.add(connectedPlayesHeader);
        playersList.setVisibleRowCount(4);
        center.add(new JScrollPane(playersList));
        JPanel startRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startRow.add(startMatchButton);
        center.add(startRow);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(backButton);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        stopButton.setEnabled(false);
        startMatchButton.setEnabled(false);
        // require username before allowing server start
        startButton.setEnabled(false);

        usernameField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                String text = usernameField.getText().trim();
                boolean has = !text.isEmpty();
                startButton.setEnabled(has && NetworkManager.getInstance().getServer() == null);
                if (NetworkManager.getInstance().getServer() != null) {
                    hostName = has ? text : null;
                    SwingUtilities.invokeLater(() -> rebuildPlayerList());
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        startButton.addActionListener(e -> openServer());
        stopButton.addActionListener(e -> closeServer());

        startMatchButton.addActionListener(e -> {
            GameServer server = NetworkManager.getInstance().getServer();
            if (server == null) {
                JOptionPane.showMessageDialog(this, "Server is not running.");
                return;
            }
            if (server.getConnectedCount() < 1) {
                JOptionPane.showMessageDialog(this, "Need 2 players to start the match.");
                return;
            }
            if (onStartMatch != null) SwingUtilities.invokeLater(onStartMatch);
        });
    }

    public boolean isServerRunning() { return NetworkManager.getInstance().getServer() != null; }

    public void openServer() {
        if (NetworkManager.getInstance().getServer() != null) return;
        String name = usernameField.getText().trim();
        hostName = name.isEmpty() ? null : name;

        GameServer server = NetworkManager.getInstance().startServer();
        String hostAddress = server.getHostAddress();
        if (hostAddress != null) {
            ipLabel.setText("Server IP: " + hostAddress);
            JOptionPane.showMessageDialog(this, "Server started at IP: " + hostAddress);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            // start a local client for the host so they appear like any other player
            try {
                if (hostName != null) {
                    Network.GameClient gc = NetworkManager.getInstance().startLocalClient(hostName);
                    // if MatchPanel is present in the GUI it will have attached handlers when constructed; otherwise
                    // callers (GUI) can re-attach.
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to start local client: " + ex.getMessage());
            }

            // refresh connected players periodically
            new Thread(() -> {
                while (NetworkManager.getInstance().getServer() != null) {
                    SwingUtilities.invokeLater(() -> rebuildPlayerList());
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }, "HostPanel-player-refresh").start();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to retrieve host address.");
        }
    }

    private void rebuildPlayerList() {
        playersModel.clear();
        GameServer server = NetworkManager.getInstance().getServer();
        java.util.List<String> names = server != null ? server.getConnectedUsernames() : java.util.Collections.emptyList();

        // Deduplicate while preserving order. Ensure host appears once (marked as Host).
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>(names);
        // if (hostName != null && !hostName.isEmpty()) {
        //     // If the host already appears in the connected list, remove it so we can add it with the Host label
        //     unique.remove(hostName);
        // }

        int added = 0;
        // if (hostName != null && !hostName.isEmpty()) {
        //     playersModel.addElement(hostName + " (Host)");
        //     added++;
        // }

        for (String n : unique) {
            if (added >= 2) break;
            playersModel.addElement(n);
            added++;
        }

        // Enable start when two distinct players (host + other) are present
        startMatchButton.setEnabled(added >= 2);
    }

    public void closeServer() {
        if (NetworkManager.getInstance().getServer() != null) {
            NetworkManager.getInstance().stopServer();
            NetworkManager.getInstance().stopClient();
            ipLabel.setText("Server IP: -");
            hostName = null;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    public void promptUsername() {
        SwingUtilities.invokeLater(() -> {
            String s = JOptionPane.showInputDialog(this, "Enter your username:", "Host Username", JOptionPane.PLAIN_MESSAGE);
            if (s != null) usernameField.setText(s);
        });
    }
}
