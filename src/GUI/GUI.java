package GUI;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.*;

import Constants.Constants.Dimensions;
import GUI.Panels.HomePanel;
import GUI.Panels.HostPanel;
import GUI.Panels.JoinPanel;
import GUI.Panels.MatchPanel;
import Network.NetworkManager;


public class GUI extends JFrame implements ActionListener{
    private final HomePanel homePanel = new HomePanel();
    private final HostPanel hostPanel = new HostPanel();
    private final JoinPanel joinPanel = new JoinPanel();
    private final MatchPanel matchPanel = new MatchPanel();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    public GUI() {
       // JFrame setup
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("DUO");
        setSize(Dimensions.WIDTH, Dimensions.HEIGHT);
        setResizable(false);

        // wire back buttons
        homePanel.getHostButton().addActionListener(this);
        homePanel.getJoinButton().addActionListener(this);
        hostPanel.getBackButton().addActionListener(this);
        joinPanel.getBackButton().addActionListener(this);

        cardPanel.add(homePanel, "Home");
        cardPanel.add(hostPanel, "Host");
        cardPanel.add(joinPanel, "Join");
        cardPanel.add(matchPanel, "Match");

        // when host presses Start Match, ask the server to start the match by sending a StartUnoMatchMessage
        hostPanel.setOnStartMatch(() -> {
            // send the start request via our client so the server processes it and notifies all clients
            try {
                Network.GameClient gc = Network.NetworkManager.getInstance().getClient();
                if (gc != null && gc.isConnected()) {
                    Messages.StartDuoMatchMessage m = new Messages.StartDuoMatchMessage();
                    gc.sendTCP(m);
                } else {
                    // no client available — tell user to start client/local connection first
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GUI.this, "You must be connected as a client to request match start."));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GUI.this, "Failed to request match start: " + ex.getMessage()));
            }
        });

        matchPanel.setOnEndMatch(() -> {
            // when the match ends, return to the home panel
            SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, "Home"));
            // ensure servers stopped when returning home
            NetworkManager.getInstance().stopServer();
            NetworkManager.getInstance().stopClient();
        });

        // When a client is created (host's local client or a joining client), attach a StartUnoMatch handler
        Network.NetworkManager.getInstance().addClientCreatedListener(client -> {
            client.addUnoStartHandler(msg -> SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, "Match")));
            matchPanel.attachClient(client);
        });

        // if a client already exists (e.g. host started server before GUI creation), attach handlers now
        try {
            Network.GameClient existing = Network.NetworkManager.getInstance().getClient();
            if (existing != null) {
                existing.addUnoStartHandler(msg -> SwingUtilities.invokeLater(() -> cardLayout.show(cardPanel, "Match")));
                matchPanel.attachClient(existing);
            }
        } catch (Exception ignored) {}

        add(cardPanel);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == homePanel.getHostButton()) {
            cardLayout.show(cardPanel, "Host");
            // Do not automatically start the server — prompt user to enter username and click Start Server
            hostPanel.promptUsername();
        } else if (e.getSource() == homePanel.getJoinButton()) {
            cardLayout.show(cardPanel, "Join");
        } else if (e.getSource() == hostPanel.getBackButton() || e.getSource() == joinPanel.getBackButton()) {
            cardLayout.show(cardPanel, "Home");
            // ensure servers stopped when returning home
            if (e.getSource() == hostPanel.getBackButton()) {
                hostPanel.closeServer();
            }
        }
    }
}