package GUI.Panels;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import Constants.Constants.Dimensions;
import Network.NetworkManager;

public class JoinPanel extends JPanel {
    private final JTextField usernameField = new JTextField(12);
    private final DefaultListModel<String> serversModel = new DefaultListModel<>();
    private final JList<String> serversList = new JList<>(serversModel);
    private final JButton scanButton = new JButton("Scan for servers");
    private final JButton joinButton = new JButton("Join Selected");
    private final JButton backButton = new JButton("Back to Home");

    public JoinPanel() {
        setLayout(new BorderLayout(0, 10));
        setMaximumSize(new Dimension(Dimensions.WIDTH, Dimensions.HEIGHT));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Join a Match"));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userRow.add(new JLabel("Your username:"));
        userRow.add(usernameField);
        center.add(userRow);

        serversList.setVisibleRowCount(6);
        center.add(new JScrollPane(serversList));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(scanButton);
        controls.add(joinButton);
        center.add(controls);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(backButton);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        scanButton.addActionListener(e -> scanServers());
        joinButton.addActionListener(e -> joinSelected());
    }

    public JButton getBackButton() {
        return backButton;
    }

    private void scanServers() {
        serversModel.clear();

        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Scanning for hosts", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(new JLabel("Scanning for hosts on LAN (this may take a few seconds)..."), BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(bar, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.getContentPane().add(p);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        new SwingWorker<List<InetAddress>, Void>() {
            @Override
            protected List<InetAddress> doInBackground() throws Exception {
                return NetworkManager.getInstance().discoverHosts();
            }

            @Override
            protected void done() {
                try {
                    List<InetAddress> found = get();
                    for (InetAddress a : found) {
                        serversModel.addElement(a.getHostAddress());
                    }
                    if (found.isEmpty()) {
                        serversModel.addElement("(no servers found)");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(JoinPanel.this, "Failed to discover hosts: " + ex.getMessage());
                } finally {
                    dialog.dispose();
                }
            }
        }.execute();
    }

    private void joinSelected() {
        String selected = serversList.getSelectedValue();
        if (selected == null || selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a server to join.");
            return;
        }
        String name = usernameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username.");
            return;
        }

        try {
            NetworkManager.getInstance().connectTo(selected, name);
            JOptionPane.showMessageDialog(this, "Connected to " + selected);
            try {
                Network.GameClient gc = NetworkManager.getInstance().getClient();
                if (gc != null) {
                    // MatchPanel registers handlers when constructed; attach here as well to be safe
                    // No-op if MatchPanel already set them.
                    // Handlers are lightweight; MatchPanel will update UI on messages.
                }
            } catch (Exception ignored) {}
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to connect: " + ex.getMessage());
        }
    }
}
