package GUI.Panels;

import java.awt.*;

import javax.swing.*;

public class ResultPanel extends JPanel {
    JLabel resultLabel = new JLabel();
    JButton backToHomeButton = new JButton("Back to Home");

    public ResultPanel() {
        setLayout(new BorderLayout());
        add(resultLabel, BorderLayout.NORTH);
        add(backToHomeButton, BorderLayout.SOUTH);
    }

    public void setResult(boolean won) {
        if (won) {
            resultLabel.setText("You won the match!");
        } else {
            resultLabel.setText("You lost the match.");
        }
    }
    
}
