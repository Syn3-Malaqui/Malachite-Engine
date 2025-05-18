import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MenuPanel extends JPanel {
    private MarrowExecutable parent;

    public MenuPanel(MarrowExecutable parent) {
        this.parent = parent;
        setLayout(new GridBagLayout());
        setupButton();
    }

    private void setupButton() {
        JButton startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        startButton.setPreferredSize(new Dimension(200, 60));
        startButton.setFocusPainted(false);
        startButton.setBackground(Color.WHITE);
        startButton.setForeground(Color.BLACK);
        
        // Add hover effect
        startButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                startButton.setBackground(Color.LIGHT_GRAY);
            }
            public void mouseExited(MouseEvent e) {
                startButton.setBackground(Color.WHITE);
            }
        });

        startButton.addActionListener(e -> parent.startGame());
        add(startButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.BLACK);
    }
} 