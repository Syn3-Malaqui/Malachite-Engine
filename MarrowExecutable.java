import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MarrowExecutable extends JFrame {
    // Constants
    private static final int INIT_WIDTH = 1080;
    private static final int INIT_HEIGHT = 810;
    private static final double WINDOW_SCALE_FACTOR = 0.5; // 50% of screen size
    private static final double MAX_HEIGHT_RATIO = 0.7;    // 70% of screen height

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private MenuPanel menuPanel;
    private GamePanel gamePanel;
    private GameLogic gameLogic;

    // Main constructor and window setup
    public MarrowExecutable() {
        gameLogic = GameLogic.getInstance();
        setupWindow();
        setupContent();
        setupWindowStateListener();
    }

    private void setupWindow() {
        setTitle("Marrow");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
    }

    private void setupContent() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        menuPanel = new MenuPanel(this);
        gamePanel = new GamePanel();
        
        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(gamePanel, "GAME");
        
        setContentPane(mainPanel);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    public void startGame() {
        cardLayout.show(mainPanel, "GAME");
        gamePanel.initializeCenterSpriteAnimation();
        gamePanel.requestFocusInWindow();
    }

    public void returnToMenu() {
        // Reset game state completely
        gameLogic.completeReset();
        // Reset the game panel
        gamePanel = new GamePanel();
        mainPanel.removeAll();
        mainPanel.add(menuPanel, "MENU");
        mainPanel.add(gamePanel, "GAME");
        // Show menu
        cardLayout.show(mainPanel, "MENU");
        // Force layout update
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void setupWindowStateListener() {
        addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                if ((e.getOldState() & Frame.MAXIMIZED_BOTH) != 0 &&
                    (e.getNewState() & Frame.MAXIMIZED_BOTH) == 0) {
                    resizeWindow();
                }
            }
        });
    }

    private void resizeWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        
        int newWidth = (int)(screenWidth * WINDOW_SCALE_FACTOR);
        int newHeight = newWidth * 3 / 4;
        
        if (newHeight > screenHeight * MAX_HEIGHT_RATIO) {
            newHeight = (int)(screenHeight * MAX_HEIGHT_RATIO);
            newWidth = newHeight * 4 / 3;
        }
        
        setSize(newWidth, newHeight);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MarrowExecutable::new);
    }
}
