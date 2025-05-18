import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.awt.image.BufferedImage;
import java.util.Scanner;
import java.io.FileWriter;
import java.util.Map;
import java.util.HashMap;

public class GamePanel extends JPanel {
    // Constants
    private static final double ANIMATION_SPEED = 0.026;
    private static final double TARGET_SCALE = 1.1;
    private static final double BASE_SCALE = 1.0;
    private static final double BASE_ROTATION = 7.0;
    private static final long FADE_CYCLE_DURATION = 1500;
    private static final double PARALLAX_FACTOR = -0.02;
    private static final long FLASH_DELAY = 400;
    private static final long FLASH_DURATION = 100;
    private static final int NUM_CARDS = 4;
    private static final int ANIMATION_FRAME_RATE = 8;
    private static final double CARD_SCALE_FACTOR = 0.8;
    private static final double SPRITE_SCALE_FACTOR = 0.8;
    private static final long DEATH_SCREEN_DURATION = 2000; // 2 seconds 
    private static final long ENEMY_FLASH_DURATION = 200; // 0.2 seconds for enemy flash
    private static final int SHAKE_INTENSITY = 2; // Maximum pixels to shake 
    private static final int SHAKE_FREQUENCY = 2; // How many times to shake per flash
    private static final long DAMAGE_DISPLAY_DURATION = 300; // 0.3 seconds display time
    private static final String HIGH_SCORE_FILE = "highscore.txt";

    // Image resources
    private Image bgImage;
    private Image centerImage;
    private Image eImage;
    private Image hpCardImage;
    private Image teethOverlayImage;
    private Image heartImage;
    private Image defImage;
    private Image attackCardImage;
    private Image defenseCardImage;
    private GameLogic gameLogic;
    private String centerSpriteName = "";
    private ArrayList<String> enemySprites = new ArrayList<>();
    private Random random = new Random();
    private Rectangle[] clickableAreas;
    private int hoveredArea = -1;
    private double[] scaleFactors;
    private double[] rotationFactors;
    private double[] alphaFactors;
    private long[] clickStartTimes;
    private boolean[] isDefenseCard;
    private boolean[] lastCardType = new boolean[NUM_CARDS]; // Track last card type
    private int[] consecutiveCount = new int[NUM_CARDS]; // Track consecutive same types
    private int counterValue = 20;
    private long lastUpdateTime = System.currentTimeMillis();
    private double centerSpriteScale = 1.0;
    private boolean centerSpriteInitialized = true;
    private Timer centerSpriteTimer;
    private int mouseX = 0;
    private int mouseY = 0;
    private boolean isFlashing = false;
    private boolean isArmorBreak = false;
    private long flashStartTime = -1;
    private boolean isDeathScreen = false;
    private long deathScreenStartTime = -1;
    private boolean isEnemyFlashing = false;
    private long enemyFlashStartTime = -1;
    private int shakeOffsetX = 0;
    private int shakeOffsetY = 0;
    private int lastDamageDealt = 0;
    private long damageDisplayStartTime = -1;
    
    // Image cache
    private final Map<String, Image> imageCache = new HashMap<>();
    
    // High score tracking
    private int currentHighScore = -1; // -1 indicates not loaded yet

    public GamePanel() {
        gameLogic = GameLogic.getInstance();
        random = new Random();
        initializeArrays();
        loadImages();
        setupClickableAreas();
        setupMouseListener();
        setupMouseMotionListener();
        initializeCards();
        setupAnimationTimer();
        setupDeathCheckTimer();
    }

    private void initializeArrays() {
        scaleFactors = new double[NUM_CARDS];
        rotationFactors = new double[NUM_CARDS];
        alphaFactors = new double[NUM_CARDS];
        clickStartTimes = new long[NUM_CARDS];
        isDefenseCard = new boolean[NUM_CARDS];
        
        for (int i = 0; i < NUM_CARDS; i++) {
            scaleFactors[i] = BASE_SCALE;
            rotationFactors[i] = i < 2 ? -BASE_ROTATION : BASE_ROTATION;
            alphaFactors[i] = 1.0;
            clickStartTimes[i] = -1;
        }
    }

    private void initializeCards() {
        // Create a list of indices
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < NUM_CARDS; i++) {
            indices.add(i);
        }
        
        // Shuffle the indices
        java.util.Collections.shuffle(indices);
        
        // Assign 2 defense and 2 attack cards
        for (int i = 0; i < NUM_CARDS; i++) {
            isDefenseCard[indices.get(i)] = i < 2; // First 2 are defense, last 2 are attack
        }
    }

    public void initializeCenterSpriteAnimation() {
        centerSpriteScale = 1.2;
        centerSpriteInitialized = false;
        
        // Reload the sprite image
        eImage = Toolkit.getDefaultToolkit().getImage(gameLogic.getCurrentSpritePath());
        
        if (centerSpriteTimer != null) {
            centerSpriteTimer.stop();
        }
        
        centerSpriteTimer = new Timer(16, e -> {
            if (!centerSpriteInitialized) {
                if (centerSpriteScale > 1.0) {
                    double diff = 1.0 - centerSpriteScale;
                    double change = diff * Math.min(1.0, ANIMATION_SPEED * 16);
                    centerSpriteScale += change;
                    repaint();
                } else {
                    centerSpriteScale = 1.0;
                    centerSpriteInitialized = true;
                    centerSpriteTimer.stop();
                }
            }
        });
        centerSpriteTimer.start();
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleGridClick(e.getX(), e.getY());
            }
        });
    }

    private void setupMouseMotionListener() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                int previousHover = hoveredArea;
                hoveredArea = -1;
                
                if (clickableAreas != null) {
                    for (int i = 0; i < clickableAreas.length; i++) {
                        if (clickableAreas[i] != null && clickableAreas[i].contains(e.getX(), e.getY())) {
                            hoveredArea = i;
                            break;
                        }
                    }
                }
                
                if (previousHover != hoveredArea) {
                    repaint();
                }
                repaint();
            }
        });
    }

    private void setupAnimationTimer() {
        Timer timer = new Timer(ANIMATION_FRAME_RATE, e -> {
            boolean needsRepaint = false;
            long currentTime = System.currentTimeMillis();
            double deltaTime = (currentTime - lastUpdateTime) / 1000.0;
            lastUpdateTime = currentTime;

            // Only update animations if they're active
            if (isFlashing) {
                needsRepaint |= handleFlashEffect(currentTime);
            }
            if (isEnemyFlashing) {
                needsRepaint |= handleEnemyFlashEffect(currentTime);
            }
            if (damageDisplayStartTime != -1) {
                needsRepaint |= true; // Damage display needs repaint
            }
            needsRepaint |= updateCardAnimations(currentTime, deltaTime);

            if (needsRepaint) {
                repaint();
            }
        });
        timer.start();
    }

    private boolean handleFlashEffect(long currentTime) {
        if (!isFlashing) return false;
        
        long timeSinceFlash = currentTime - flashStartTime;
        if (timeSinceFlash >= FLASH_DURATION) {
            isFlashing = false;
            flashStartTime = -1;
            return true;
        }
        return false;
    }

    private boolean handleEnemyFlashEffect(long currentTime) {
        if (!isEnemyFlashing) {
            shakeOffsetX = 0;
            shakeOffsetY = 0;
            return false;
        }
        
        long timeSinceFlash = currentTime - enemyFlashStartTime;
        if (timeSinceFlash >= ENEMY_FLASH_DURATION) {
            isEnemyFlashing = false;
            enemyFlashStartTime = -1;
            shakeOffsetX = 0;
            shakeOffsetY = 0;
            return true;
        }

        // Calculate shake offset
        double progress = (double)timeSinceFlash / ENEMY_FLASH_DURATION;
        double shakeProgress = progress * SHAKE_FREQUENCY * Math.PI * 2;
        shakeOffsetX = (int)(Math.sin(shakeProgress) * SHAKE_INTENSITY);
        shakeOffsetY = (int)(Math.cos(shakeProgress) * SHAKE_INTENSITY);
        
        return true;
    }

    private boolean updateCardAnimations(long currentTime, double deltaTime) {
        boolean needsRepaint = false;
        
        for (int i = 0; i < NUM_CARDS; i++) {
            needsRepaint |= updateCardScale(i, deltaTime);
            needsRepaint |= updateCardRotation(i, deltaTime);
            needsRepaint |= updateCardFade(i, currentTime, deltaTime);
        }
        
        return needsRepaint;
    }

    private boolean updateCardScale(int index, double deltaTime) {
        double targetScale = (index == hoveredArea && clickStartTimes[index] == -1) ? TARGET_SCALE : BASE_SCALE;
        double currentScale = scaleFactors[index];
        
        if (Math.abs(currentScale - targetScale) > 0.001) {
            double diff = targetScale - currentScale;
            double change = diff * Math.min(1.0, ANIMATION_SPEED * deltaTime * 1000);
            scaleFactors[index] = currentScale + change;
            return true;
        }
        return false;
    }

    private boolean updateCardRotation(int index, double deltaTime) {
        double targetRotation = (index == hoveredArea && clickStartTimes[index] == -1) ? 0.0 : (index < 2 ? -BASE_ROTATION : BASE_ROTATION);
        double currentRotation = rotationFactors[index];
        
        if (Math.abs(currentRotation - targetRotation) > 0.001) {
            double diff = targetRotation - currentRotation;
            double change = diff * Math.min(1.0, ANIMATION_SPEED * deltaTime * 1000);
            rotationFactors[index] = currentRotation + change;
            return true;
        }
        return false;
    }

    private boolean updateCardFade(int index, long currentTime, double deltaTime) {
        if (clickStartTimes[index] == -1) return false;
        
        long timeSinceClick = currentTime - clickStartTimes[index];
        if (timeSinceClick >= FADE_CYCLE_DURATION) {
            alphaFactors[index] = 1.0;
            clickStartTimes[index] = -1;
            return true;
        }
        
        double cycleProgress = (double)timeSinceClick / FADE_CYCLE_DURATION;
        double targetAlpha = calculateTargetAlpha(cycleProgress, timeSinceClick, index);
        
        if (Math.abs(alphaFactors[index] - targetAlpha) > 0.001) {
            double diff = targetAlpha - alphaFactors[index];
            double change = diff * Math.min(1.0, ANIMATION_SPEED * deltaTime * 1000);
            alphaFactors[index] = alphaFactors[index] + change;
            return true;
        }
        return false;
    }

    private double calculateTargetAlpha(double cycleProgress, long timeSinceClick, int index) {
        if (cycleProgress < 0.2) {
            return 1.0 - (cycleProgress * 5);
        } else if (cycleProgress < 0.8) {
            if (timeSinceClick >= FADE_CYCLE_DURATION - 500 && clickStartTimes[index] != -1) {
                randomizeCard(index);
                clickStartTimes[index] = -2;
            }
            return 0.0;
        } else {
            return (cycleProgress - 0.8) * 5;
        }
    }

    private void loadImages() {
        try {
            // Load images with caching
            bgImage = getCachedImage("bgframe.png");
            eImage = getCachedImage(gameLogic.getCurrentSpritePath());
            hpCardImage = getCachedImage("sprites/HPCARD.png");
            teethOverlayImage = getCachedImage("sprites/TeethOverlay.png");
            heartImage = getCachedImage("sprites/heart.png");
            defImage = getCachedImage("sprites/def.png");
            attackCardImage = getCachedImage("sprites/attack.png");
            defenseCardImage = getCachedImage("sprites/defense.png");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Image getCachedImage(String path) {
        return imageCache.computeIfAbsent(path, k -> {
            try {
                return Toolkit.getDefaultToolkit().getImage(k);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private void setupClickableAreas() {
        clickableAreas = new Rectangle[NUM_CARDS];
    }

    private void updateClickableAreas(DrawingArea area) {
        int bottomRowY = area.y + area.height * 2 / 3;
        int cellWidth = area.width / 5;
        int cellHeight = area.height / 3;

        for (int i = 0; i < NUM_CARDS; i++) {
            int cellX = area.x + i * cellWidth;
            clickableAreas[i] = new Rectangle(cellX, bottomRowY, cellWidth, cellHeight);
        }
    }

    private void randomizeCard(int index) {
        System.out.println("Randomizing card " + index);
        // 60% chance for attack (false), 40% chance for defense (true)
        isDefenseCard[index] = random.nextDouble() < 0.4;
        System.out.println("New card type: " + (isDefenseCard[index] ? "Defense" : "Attack"));
    }

    private void handleGridClick(int x, int y) {
        if (clickableAreas == null || gameLogic.isGameOver()) return;

        // Check for player death first
        if (gameLogic.isPlayerDead()) {
            isDeathScreen = true;
            deathScreenStartTime = System.currentTimeMillis();
            Timer deathTimer = new Timer((int)DEATH_SCREEN_DURATION, e -> {
                ((Timer)e.getSource()).stop();
                // Return to menu using the parent window's method
                JFrame frame = (JFrame)SwingUtilities.getWindowAncestor(this);
                if (frame instanceof MarrowExecutable) {
                    ((MarrowExecutable)frame).returnToMenu();
                }
            });
            deathTimer.setRepeats(false);
            deathTimer.start();
            return;
        }

        for (int i = 0; i < clickableAreas.length; i++) {
            if (clickableAreas[i] != null && clickableAreas[i].contains(x, y)) {
                if (clickStartTimes[i] == -1) {
                    System.out.println("Card " + i + " clicked - Current type: " + (isDefenseCard[i] ? "Defense" : "Attack"));
                    int oldDef = gameLogic.getDefCounter();
                    int oldEnemyHP = gameLogic.getEnemyHP();
                    gameLogic.handleCardClick(i, isDefenseCard[i]);
                    
                    // Store damage dealt if it was an attack card
                    if (!isDefenseCard[i]) {
                        lastDamageDealt = oldEnemyHP - gameLogic.getEnemyHP();
                        damageDisplayStartTime = System.currentTimeMillis();
                        isEnemyFlashing = true;
                        enemyFlashStartTime = System.currentTimeMillis();
                    }
                    
                    // Check for player death after card effect
                    if (gameLogic.isPlayerDead()) {
                        isDeathScreen = true;
                        deathScreenStartTime = System.currentTimeMillis();
                        Timer deathTimer = new Timer((int)DEATH_SCREEN_DURATION, e -> {
                            ((Timer)e.getSource()).stop();
                            // Return to menu using the parent window's method
                            JFrame frame = (JFrame)SwingUtilities.getWindowAncestor(this);
                            if (frame instanceof MarrowExecutable) {
                                ((MarrowExecutable)frame).returnToMenu();
                            }
                        });
                        deathTimer.setRepeats(false);
                        deathTimer.start();
                    }
                    // Only schedule flash effect if enemy is not defeated
                    else if (!gameLogic.isGameOver()) {
                        Timer flashTimer = new Timer(400, e -> {
                            ((Timer)e.getSource()).stop();
                            // Check if armor break occurred after both card effect and enemy attack
                            int currentDef = gameLogic.getDefCounter();
                            isArmorBreak = (oldDef > 0 && currentDef == 0);
                            isFlashing = true;
                            flashStartTime = System.currentTimeMillis();
                        });
                        flashTimer.setRepeats(false);
                        flashTimer.start();
                    } else {
                        // If enemy is defeated, show victory buttons immediately
                        showVictoryButtons();
                    }
                    
                    clickStartTimes[i] = System.currentTimeMillis();
                    repaint();
                }
                break;
            }
        }
    }

    private void showVictoryButtons() {
        Timer delayTimer = new Timer(1000, e -> {
            ((Timer)e.getSource()).stop();
            
            JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Victory!", true);
            dialog.setLayout(new GridLayout(2, 2, 10, 10));
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            
            // Create 4 buttons with different colors
            Color[] colors = {Color.RED, Color.BLUE, Color.RED, Color.BLUE};  // Changed colors for attack and shield
            String[] buttonTexts = {"Health Upgrade", "Defense Upgrade", "Attack Upgrade", "Shield Upgrade"};
            
            for (int i = 0; i < 4; i++) {
                JButton button = new JButton(buttonTexts[i]);
                button.setBackground(colors[i]);
                button.setForeground(Color.WHITE);
                button.setFont(new Font("Arial", Font.BOLD, 20));
                button.setFocusPainted(false);
                
                final int index = i;
                button.addActionListener(e2 -> {
                    dialog.dispose();
                    gameLogic.resetGame();
                    gameLogic.applyUpgrade(index);
                    initializeCenterSpriteAnimation();
                    initializeCards();
                });
                
                dialog.add(button);
            }
            
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    private void setupDeathCheckTimer() {
        Timer deathCheckTimer = new Timer(100, e -> {
            if (gameLogic.isPlayerDead() && !isDeathScreen) {
                System.out.println("Death detected in GamePanel!");
                isDeathScreen = true;
                deathScreenStartTime = System.currentTimeMillis();
                Timer deathTimer = new Timer((int)DEATH_SCREEN_DURATION, e2 -> {
                    ((Timer)e2.getSource()).stop();
                    // Return to menu using the parent window's method
                    JFrame frame = (JFrame)SwingUtilities.getWindowAncestor(this);
                    if (frame instanceof MarrowExecutable) {
                        ((MarrowExecutable)frame).returnToMenu();
                    }
                });
                deathTimer.setRepeats(false);
                deathTimer.start();
            }
        });
        deathCheckTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        setupRenderingHints(g2d);

        DrawingArea area = calculateDrawingArea();
        updateClickableAreas(area);
        
        drawBackground(g2d, area);
        drawGrid(g2d, area);
        drawContent(g2d, area);
        
        // Draw death screen
        if (gameLogic.isPlayerDead() || isDeathScreen) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
            
            // Get current score and high score
            int currentScore = gameLogic.getEnemiesDefeated() + 1; // Current round number
            int highScore = getHighScore();
            saveHighScore(currentScore);
            
            // Draw death text
            Font deathFont = new Font("Arial", Font.BOLD, 48);
            g2d.setFont(deathFont);
            FontMetrics fm = g2d.getFontMetrics();
            String deathText = "YOU DIED";
            int textWidth = fm.stringWidth(deathText);
            int textHeight = fm.getHeight();
            int x = (getWidth() - textWidth) / 2;
            int y = (getHeight() + textHeight) / 2;
            g2d.drawString(deathText, x, y);
            
            // Draw score information
            Font scoreFont = new Font("Arial", Font.BOLD, 24);
            g2d.setFont(scoreFont);
            FontMetrics scoreFm = g2d.getFontMetrics();
            String scoreText = "Round Reached: " + currentScore;
            String highScoreText = "Highest Round: " + highScore;
            
            int scoreWidth = scoreFm.stringWidth(scoreText);
            int highScoreWidth = scoreFm.stringWidth(highScoreText);
            int scoreX = (getWidth() - Math.max(scoreWidth, highScoreWidth)) / 2;
            int scoreY = y + textHeight + 40;
            
            g2d.drawString(scoreText, scoreX, scoreY);
            g2d.drawString(highScoreText, scoreX, scoreY + 30);
        }
        // Draw flash effect only within the grid area
        else if (isFlashing) {
            if (isArmorBreak) {
                g2d.setColor(new Color(255, 0, 0, 255)); // Full red flash for armor break
            } else {
                g2d.setColor(new Color(255, 255, 255, 200)); // White flash for normal hits
            }
            g2d.fillRect(area.x, area.y, area.width, area.height);
        }
        
        drawOverlays(g2d, area);
        
        if (!centerSpriteInitialized) {
            repaint();
        }
    }

    private void setupRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private DrawingArea calculateDrawingArea() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int targetWidth = panelWidth;
        int targetHeight = panelWidth * 3 / 4;

        if (targetHeight > panelHeight) {
            targetHeight = panelHeight;
            targetWidth = panelHeight * 4 / 3;
        }

        int x = (panelWidth - targetWidth) / 2;
        int y = (panelHeight - targetHeight) / 2;

        return new DrawingArea(x, y, targetWidth, targetHeight);
    }

    private void drawBackground(Graphics2D g2d, DrawingArea area) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.fillRect(area.x, area.y, area.width, area.height);
    }

    private void drawGrid(Graphics2D g2d, DrawingArea area) {
        // Grid lines removed
    }

    private void drawContent(Graphics2D g2d, DrawingArea area) {
        Font font = new Font("SansSerif", Font.BOLD, 48);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        drawGridCells(g2d, area, fm);
        drawHPCards(g2d, area);
        drawTeethOverlay(g2d, area);
        drawCounters(g2d, area, fm);
    }

    private void drawCounters(Graphics2D g2d, DrawingArea area, FontMetrics fm) {
        g2d.setColor(Color.WHITE);
        
        int cellWidth = area.width / 5;
        int cellHeight = area.height / 3;
        int halfCellWidth = cellWidth / 2;
        int halfCellHeight = cellHeight / 2;
        
        // Position for K2 (HP counter)
        int k2X = area.x + cellWidth * 4 + halfCellWidth;
        int k2Y = area.y + area.height * 2 / 3;
        
        // Position for L2 (Defense counter)
        int l2X = area.x + cellWidth * 4 + halfCellWidth;
        int l2Y = area.y + area.height * 2 / 3 + halfCellHeight;

        Font counterFont = new Font("SansSerif", Font.BOLD, Math.min(halfCellWidth / 3, halfCellHeight / 2));
        g2d.setFont(counterFont);
        FontMetrics counterFm = g2d.getFontMetrics();

        // Get counter values from GameLogic
        String hpCounterStr = String.valueOf(gameLogic.getHpCounter());
        String defCounterStr = String.valueOf(gameLogic.getDefCounter());
        
        // Draw HP counter in K2
        int textWidth = counterFm.stringWidth(hpCounterStr);
        int textHeight = counterFm.getAscent();
        int k2CenterX = k2X + (halfCellWidth - textWidth) / 2;
        int k2CenterY = k2Y + (halfCellHeight + textHeight) / 2 - 5;
        g2d.drawString(hpCounterStr, k2CenterX, k2CenterY);

        // Draw Defense counter in L2
        textWidth = counterFm.stringWidth(defCounterStr);
        int l2CenterX = l2X + (halfCellWidth - textWidth) / 2;
        int l2CenterY = l2Y + (halfCellHeight + textHeight) / 2 - 5;
        g2d.drawString(defCounterStr, l2CenterX, l2CenterY);
    }

    private void drawGridCells(Graphics2D g2d, DrawingArea area, FontMetrics fm) {
        int[] rowHeights = {area.height / 6, area.height * 3 / 6, area.height - (area.height / 6 + area.height * 3 / 6)};
        int[] rowYs = {area.y, area.y + rowHeights[0], area.y + rowHeights[0] + rowHeights[1]};
        int[] colsPerRow = {3, 3, 5};

        char letter = 'A';
        for (int row = 0; row < 3; row++) {
            drawRow(g2d, area, row, rowHeights[row], rowYs[row], colsPerRow[row], letter, fm);
            letter += colsPerRow[row];
        }
    }

    private void drawRow(Graphics2D g2d, DrawingArea area, int row, int rowHeight, int rowY, int cols, char startLetter, FontMetrics fm) {
        int cellWidth = area.width / cols;
        char letter = startLetter;

        for (int col = 0; col < cols; col++) {
            int cellX = area.x + col * cellWidth;
            
            if (row == 2 && col == cols - 1) {
                drawSplitCell(g2d, cellX, rowY, cellWidth, rowHeight, letter, fm);
            } else if (row == 1 && col == 1) {
                drawCenterCell(g2d, cellX, rowY, cellWidth, rowHeight);
            } else {
                drawNormalCell(g2d, cellX, rowY, cellWidth, rowHeight, letter, fm, row);
            }
            letter++;
        }
    }

    private void drawSplitCell(Graphics2D g2d, int x, int y, int width, int height, char letter, FontMetrics fm) {
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        
        // Draw top-left (K1)
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, halfWidth, halfHeight);
        if (heartImage != null) {
            drawScaledImage(g2d, heartImage, x, y, halfWidth, halfHeight);
        }
        
        // Draw top-right (K2)
        g2d.setColor(Color.RED);
        g2d.fillRect(x + halfWidth, y, halfWidth, halfHeight);
        
        // Draw bottom-left (L1)
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x, y + halfHeight, halfWidth, halfHeight);
        if (defImage != null) {
            drawScaledImage(g2d, defImage, x, y + halfHeight, halfWidth, halfHeight);
        }
        
        // Draw bottom-right (L2)
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x + halfWidth, y + halfHeight, halfWidth, halfHeight);
    }

    private void drawCenterCell(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x, y, width, height);
        
        if (eImage != null) {
            int imgWidth = eImage.getWidth(this);
            int imgHeight = eImage.getHeight(this);
            
            if (imgWidth > 0 && imgHeight > 0) {
                double widthScale = (double)width / imgWidth;
                double heightScale = (double)height / imgHeight;
                double scale = Math.min(widthScale, heightScale) * 0.8 * centerSpriteScale;
                
                int drawWidth = (int)(imgWidth * scale);
                int drawHeight = (int)(imgHeight * scale);
                
                // Calculate parallax offset
                int centerX = x + width / 2;
                int centerY = y + height / 2;
                int offsetX = (int)((mouseX - centerX) * PARALLAX_FACTOR);
                int offsetY = (int)((mouseY - centerY) * PARALLAX_FACTOR);
                
                // Add shake offset to the drawing position
                int drawX = x + (width - drawWidth) / 2 + offsetX + shakeOffsetX;
                int drawY = y + (height - drawHeight) / 2 + offsetY + shakeOffsetY;
                
                // Save the original composite
                Composite oldComposite = g2d.getComposite();
                
                // If enemy is flashing, apply red tint to non-transparent parts
                if (isEnemyFlashing) {
                    // Create a red-tinted version of the sprite
                    BufferedImage redSprite = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D redG2d = redSprite.createGraphics();
                    
                    // Draw the original sprite first to preserve transparency
                    redG2d.drawImage(eImage, 0, 0, imgWidth, imgHeight, this);
                    
                    // Apply red tint only to non-transparent pixels
                    redG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 0.7f));
                    redG2d.setColor(Color.RED);
                    redG2d.fillRect(0, 0, imgWidth, imgHeight);
                    redG2d.dispose();
                    
                    // Draw the red-tinted sprite
                    g2d.drawImage(redSprite, drawX, drawY, drawWidth, drawHeight, this);
                } else {
                    // Just draw the sprite normally if not flashing
                    g2d.drawImage(eImage, drawX, drawY, drawWidth, drawHeight, this);
                }
                
                // Restore the original composite
                g2d.setComposite(oldComposite);

                // Draw damage counter if active
                if (damageDisplayStartTime != -1) {
                    long currentTime = System.currentTimeMillis();
                    long timeSinceDamage = currentTime - damageDisplayStartTime;
                    
                    if (timeSinceDamage < DAMAGE_DISPLAY_DURATION) {
                        // Calculate fade out
                        float alpha = 1.0f - ((float)timeSinceDamage / DAMAGE_DISPLAY_DURATION);
                        
                        // Set up font for damage text - make it larger
                        Font damageFont = new Font("Arial", Font.BOLD, 48);
                        g2d.setFont(damageFont);
                        
                        // Draw damage text with white color and fade
                        Composite oldDamageComposite = g2d.getComposite();
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        g2d.setColor(Color.WHITE);
                        
                        String damageText = "-" + lastDamageDealt;
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(damageText);
                        int textHeight = fm.getHeight();
                        
                        // Position text in the center of the sprite
                        int textX = drawX + (drawWidth - textWidth) / 2;
                        int textY = drawY + (drawHeight + textHeight) / 2;
                        
                        // Draw text with a black outline for better visibility
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(damageText, textX - 1, textY);
                        g2d.drawString(damageText, textX + 1, textY);
                        g2d.drawString(damageText, textX, textY - 1);
                        g2d.drawString(damageText, textX, textY + 1);
                        
                        // Draw the white text
                        g2d.setColor(Color.WHITE);
                        g2d.drawString(damageText, textX, textY);
                        
                        g2d.setComposite(oldDamageComposite);
                    } else {
                        damageDisplayStartTime = -1;
                    }
                }
            }
        }
    }

    private void drawNormalCell(Graphics2D g2d, int x, int y, int width, int height, char letter, FontMetrics fm, int row) {
        if (letter == 'K') {
            g2d.setColor(Color.RED);
        } else if (letter == 'L') {
            g2d.setColor(Color.BLUE);
        } else if (letter == 'C') {
            g2d.setColor(Color.GREEN);
        } else {
            g2d.setColor(row == 0 || row == 2 ? Color.DARK_GRAY : Color.BLACK);
        }
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.WHITE);

        if (letter == 'B' && eImage != null) {
            Font filenameFont = new Font("SansSerif", Font.BOLD, Math.min(width / 10, height / 3));
            g2d.setFont(filenameFont);
            FontMetrics filenameFm = g2d.getFontMetrics();
            
            String spriteName = gameLogic.getCurrentSpriteName();
            int textWidth = filenameFm.stringWidth(spriteName);
            int textHeight = filenameFm.getAscent();
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height + textHeight) / 2 - filenameFm.getDescent();
            g2d.drawString(spriteName, textX, textY);
        } else if (letter == 'C') {
            Font counterFont = new Font("SansSerif", Font.BOLD, Math.min(width / 10, height / 3));
            g2d.setFont(counterFont);
            FontMetrics counterFm = g2d.getFontMetrics();
            
            String enemyHPStr = String.valueOf(gameLogic.getEnemyHP());
            int textWidth = counterFm.stringWidth(enemyHPStr);
            int textHeight = counterFm.getAscent();
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height + textHeight) / 2 - counterFm.getDescent();
            g2d.drawString(enemyHPStr, textX, textY);
        }else if (letter == 'A') {
            Font counterFont = new Font("SansSerif", Font.BOLD, Math.min(width / 10, height / 3));
            g2d.setFont(counterFont);
            FontMetrics counterFm = g2d.getFontMetrics();
            
            String roundCtr = "Round " + (gameLogic.getEnemiesDefeated() + 1);
            int textWidth = counterFm.stringWidth(roundCtr);
            int textHeight = counterFm.getAscent();
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height + textHeight) / 2 - counterFm.getDescent();
            g2d.drawString(roundCtr, textX, textY);
        }
    }

    private void drawScaledImage(Graphics2D g2d, Image image, int x, int y, int width, int height) {
        if (image == null) return;
        
        int imgWidth = image.getWidth(this);
        int imgHeight = image.getHeight(this);
        
        if (imgWidth <= 0 || imgHeight <= 0) return;

        double widthScale = (double)width / imgWidth;
        double heightScale = (double)height / imgHeight;
        double scale = Math.min(widthScale, heightScale) * CARD_SCALE_FACTOR;
        
        int drawWidth = (int)(imgWidth * scale);
        int drawHeight = (int)(imgHeight * scale);
        
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        
        g2d.drawImage(image, drawX, drawY, drawWidth, drawHeight, this);
    }

    private void drawHPCards(Graphics2D g2d, DrawingArea area) {
        if (hpCardImage == null) return;

        int bottomRowY = area.y + area.height * 2 / 3;
        int cellWidth = area.width / 5;
        int cellHeight = area.height / 3;

        for (int col = 0; col < NUM_CARDS; col++) {
            int cellX = area.x + col * cellWidth;
            drawHPCard(g2d, cellX, bottomRowY, cellWidth, cellHeight, col);
        }
    }

    private void drawHPCard(Graphics2D g2d, int x, int y, int width, int height, int col) {
        Image cardImage = isDefenseCard[col] ? defenseCardImage : attackCardImage;
        
        if (cardImage == null) return;
        
        int imgWidth = cardImage.getWidth(this);
        int imgHeight = cardImage.getHeight(this);
        
        if (imgWidth <= 0 || imgHeight <= 0) return;

        double widthScale = (double)width / imgWidth;
        double heightScale = (double)height / imgHeight;
        double scale = Math.min(widthScale, heightScale) * 0.8;
        
        scale *= scaleFactors[col];
        
        int drawWidth = (int)(imgWidth * scale);
        int drawHeight = (int)(imgHeight * scale);
        
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;

        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(Math.toRadians(rotationFactors[col]), drawX + drawWidth / 2, drawY + drawHeight / 2);
        
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)alphaFactors[col]));
        g2d.drawImage(cardImage, drawX, drawY, drawWidth, drawHeight, this);
        g2d.setComposite(oldComposite);
        
        g2d.setTransform(oldTransform);
    }

    private void drawTeethOverlay(Graphics2D g2d, DrawingArea area) {
        if (teethOverlayImage == null) return;

        int middleRowY = area.y + area.height / 6;
        int cellWidth = area.width / 3;
        int cellHeight = area.height * 3 / 6;
        int imgWidth = teethOverlayImage.getWidth(this);
        int imgHeight = teethOverlayImage.getHeight(this);

        for (int col = 0; col < 3; col++) {
            int cellX = area.x + col * cellWidth;
            double widthScale = (double)cellWidth / imgWidth;
            double scale = widthScale;
            
            int drawWidth = (int)(imgWidth * scale);
            int drawHeight = (int)(imgHeight * scale);
            
            int drawX = cellX + (cellWidth - drawWidth) / 2;
            int drawY = middleRowY;
            
            g2d.drawImage(teethOverlayImage, drawX, drawY, drawWidth, drawHeight, this);
        }
    }

    private void drawOverlays(Graphics2D g2d, DrawingArea area) {
        if (bgImage != null) {
            g2d.drawImage(bgImage, area.x, area.y, area.width, area.height, this);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1080, 810);
    }

    private int getHighScore() {
        if (currentHighScore == -1) {
            try {
                File file = new File(HIGH_SCORE_FILE);
                if (!file.exists()) {
                    currentHighScore = 0;
                    return 0;
                }
                Scanner scanner = new Scanner(file);
                if (scanner.hasNextInt()) {
                    currentHighScore = scanner.nextInt();
                    scanner.close();
                    return currentHighScore;
                }
                scanner.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentHighScore = 0;
        }
        return currentHighScore;
    }

    private void saveHighScore(int score) {
        if (score > currentHighScore) {
            currentHighScore = score;
            try {
                File file = new File(HIGH_SCORE_FILE);
                FileWriter writer = new FileWriter(file);
                writer.write(String.valueOf(score));
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 