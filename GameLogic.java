import java.io.File;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Timer;

/**
 * GameLogic class manages the core game mechanics and state.
 * It follows the Singleton pattern to ensure a single instance manages the game state.
 */
public class GameLogic {
    // Game mechanics constants
    private static final double CRITICAL_HIT_CHANCE = 0.01;  // 1% chance for player
    private static final double ENEMY_CRIT_CHANCE = 0.20;    // 20% chance for enemy
    private static final double ENEMY_CRIT_MULTIPLIER = 1.5; // 1.5x damage for enemy crits
    private static final double DEFENSE_PENETRATION_CHANCE = 0.10;  // 10% chance
    private static final double NO_DEFENSE_DAMAGE_CHANCE = 0.30;  // 30% chance
    private static final double NEXT_CARD_ATTACK_CHANCE = 0.50;  // 50% chance for attack card
    
    // Damage and defense ranges
    private static final int MIN_DAMAGE = 3;
    private static final int MAX_DAMAGE = 5;
    private static final int MIN_ENEMY_DAMAGE = 1;
    private static final int MAX_ENEMY_DAMAGE = 3;
    private static final int MIN_DEFENSE = 2;
    private static final int MAX_DEFENSE = 4;
    
    // Initial game state
    private static final int INITIAL_ENEMY_HP = 15;
    private static final int INITIAL_PLAYER_HP = 10;
    private static final int INITIAL_PLAYER_DEFENSE = 5;
    private static final int INITIAL_MIN_DAMAGE = 3;
    private static final int INITIAL_MAX_DAMAGE = 5;
    private static final int INITIAL_MIN_DEFENSE = 2;
    private static final int INITIAL_MAX_DEFENSE = 4;
    
    // Enemy scaling
    private int enemiesDefeated = 0;
    private int enemyArmor = 0;
    private boolean hasArmor = false;
    private boolean hasArmorPenetration = false; // Track if this enemy has armor penetration
    private int enemyDamageBonus = 0;  // Track enemy damage scaling
    
    // Singleton instance
    private static GameLogic instance;
    
    // Game state
    private final Random random;
    private int hpCounter;
    private int defCounter;
    private int enemyHP;
    private String currentSpriteName;
    private String currentSpritePath;
    private final Map<String, String> spriteNameToPath;
    private boolean isGameOver;
    private boolean isPlayerDead;  // New state for player death
    private String nextCardType;
    private String nextCardPath;
    
    // Upgrade tracking
    private int baseMinDamage;
    private double baseMaxDamage;  // Changed to double to handle decimal increases
    private double baseMinDefense;  // Changed to double for decimal increases
    private double baseMaxDefense;  // Changed to double for decimal increases
    private int basePlayerHP;
    private int basePlayerDefense;
    private int attackUpgrades = 0;  // Track number of attack upgrades
    private int shieldUpgrades = 0;  // Track number of shield upgrades
    
    private GameLogic() {
        random = new Random();
        spriteNameToPath = new HashMap<>();
        initializeSpriteMap();
        // Initialize base stats
        baseMinDamage = INITIAL_MIN_DAMAGE;
        baseMaxDamage = INITIAL_MAX_DAMAGE;
        baseMinDefense = INITIAL_MIN_DEFENSE;
        baseMaxDefense = INITIAL_MAX_DEFENSE;
        basePlayerHP = INITIAL_PLAYER_HP;
        basePlayerDefense = INITIAL_PLAYER_DEFENSE;
        attackUpgrades = 0;
        shieldUpgrades = 0;
        resetGame();
    }
    
    /**
     * Returns the singleton instance of GameLogic.
     * Creates a new instance if one doesn't exist.
     */
    public static GameLogic getInstance() {
        if (instance == null) {
            instance = new GameLogic();
        }
        return instance;
    }
    
    /**
     * Initializes the sprite map with all available enemy and card sprites.
     */
    private void initializeSpriteMap() {
        // Enemy sprites
        spriteNameToPath.put("voidling", "sprites/Enemies/voidling.png");
        spriteNameToPath.put("yellow frederick", "sprites/Enemies/yellow frederick.png");
        spriteNameToPath.put("tung ahur", "sprites/Enemies/tung ahur.png");
        spriteNameToPath.put("tung", "sprites/Enemies/tung.png");
        spriteNameToPath.put("voidmage", "sprites/Enemies/voidmage.png");
        spriteNameToPath.put("rage", "sprites/Enemies/rage.png");
        spriteNameToPath.put("sobbing son", "sprites/Enemies/sobbing son.png");
        spriteNameToPath.put("blakeye", "sprites/Enemies/blakeye.png");
        spriteNameToPath.put("smile", "sprites/Enemies/smile.png");
        spriteNameToPath.put("blacklight", "sprites/Enemies/blacklight.png");
        
        // Card sprites
        spriteNameToPath.put("attack_card", "sprites/Cards/attack.png");
        spriteNameToPath.put("defense_card", "sprites/Cards/defense.png");
    }
    
    /**
     * Resets the game state to initial values.
     */
    public void resetGame() {
        // Only reset the current game state, not the base stats
        resetCounters();
        loadRandomSprite();
        randomizeNextCard();
        isGameOver = false;
        isPlayerDead = false;
        
        System.out.println("Game reset - Current stats:");
        System.out.println("HP: " + hpCounter + " (Base: " + basePlayerHP + ")");
        System.out.println("Defense: " + defCounter + " (Base: " + basePlayerDefense + ")");
        System.out.println("Attack range: " + baseMinDamage + "-" + baseMaxDamage);
        System.out.println("Defense gain range: " + baseMinDefense + "-" + baseMaxDefense);
    }
    
    /**
     * Completely resets the game, including all upgrades.
     * This should only be called when returning to the main menu.
     */
    public void completeReset() {
        // Reset all base stats to initial values
        baseMinDamage = INITIAL_MIN_DAMAGE;
        baseMaxDamage = INITIAL_MAX_DAMAGE;
        baseMinDefense = INITIAL_MIN_DEFENSE;
        baseMaxDefense = INITIAL_MAX_DEFENSE;
        basePlayerHP = INITIAL_PLAYER_HP;
        basePlayerDefense = INITIAL_PLAYER_DEFENSE;
        
        // Reset upgrade counters
        attackUpgrades = 0;
        shieldUpgrades = 0;
        
        // Reset enemy tracking
        enemiesDefeated = 0;
        enemyArmor = 0;
        hasArmor = false;
        hasArmorPenetration = false;
        enemyDamageBonus = 0;
        
        // Reset game state
        resetCounters();
        loadRandomSprite();
        randomizeNextCard();
        isGameOver = false;
        isPlayerDead = false;
        
        System.out.println("Game completely reset - All stats and upgrades reset to initial values");
    }

    /**
     * Resets all counters to their initial values.
     */
    private void resetCounters() {
        hpCounter = basePlayerHP;
        // Scale enemy HP based on enemies defeated
        int hpIncrease = (enemiesDefeated / 2) * 3;  // Every 2 enemies, add 3 HP
        enemyHP = INITIAL_ENEMY_HP + hpIncrease;
        
        // Scale enemy damage every 3 enemies
        enemyDamageBonus = enemiesDefeated / 3;  // Every 3 enemies, increase damage range by 1
        
        // Scale armor every 2 enemies
        if (enemiesDefeated > 0 && enemiesDefeated % 2 == 0) {
            hasArmor = true;
            enemyArmor = 1 + ((enemiesDefeated - 2) / 2); // Start at 1 armor, then scale every 2 enemies
            hasArmorPenetration = (enemiesDefeated % 4 == 2); // Every other armored enemy has penetration
            System.out.println("\n=== Enemy Status ===");
            System.out.println("Armor: " + enemyArmor + (hasArmorPenetration ? " (with 100% penetration)" : ""));
        } else {
            hasArmor = false;
            enemyArmor = 0;
            hasArmorPenetration = false;
        }
        
        defCounter = basePlayerDefense;
        
        System.out.println("\n=== Game State ===");
        System.out.println("Player HP: " + hpCounter + " (Base: " + basePlayerHP + ")");
        System.out.println("Player Defense: " + defCounter + " (Base: " + basePlayerDefense + ")");
        System.out.println("Attack Range: " + baseMinDamage + "-" + baseMaxDamage);
        System.out.println("Defense Gain Range: " + baseMinDefense + "-" + baseMaxDefense);
        System.out.println("Enemy HP: " + enemyHP);
        System.out.println("Enemy Damage Range: " + MIN_ENEMY_DAMAGE + "-" + (MAX_ENEMY_DAMAGE + enemyDamageBonus));
        if (hasArmor) {
            System.out.println("Enemy Armor: " + enemyArmor + (hasArmorPenetration ? " (with 100% penetration)" : ""));
        }
    }
    
    /**
     * Loads a random enemy sprite.
     */
    public void loadRandomSprite() {
        String[] spriteNames = spriteNameToPath.keySet().stream()
            .filter(name -> !name.endsWith("_card"))
            .toArray(String[]::new);
            
        if (spriteNames.length > 0) {
            currentSpriteName = spriteNames[random.nextInt(spriteNames.length)];
            currentSpritePath = spriteNameToPath.get(currentSpriteName);
        } else {
            setDefaultSprite();
        }
    }
    
    /**
     * Sets the default sprite when no other sprites are available.
     */
    private void setDefaultSprite() {
        currentSpriteName = "yellow frederick";
        currentSpritePath = spriteNameToPath.get(currentSpriteName);
    }
    
    /**
     * Randomizes the next card type and updates its path.
     */
    private void randomizeNextCard() {
        nextCardType = random.nextDouble() < NEXT_CARD_ATTACK_CHANCE ? "attack" : "defense";
        nextCardPath = spriteNameToPath.get(nextCardType + "_card");
    }
    
    /**
     * Handles a card click event.
     * @param cardIndex The index of the clicked card
     * @param isDefenseCard Whether the clicked card is a defense card
     */
    public void handleCardClick(int cardIndex, boolean isDefenseCard) {
        if (isGameOver) return;
        
        if (isDefenseCard) {
            handleDefenseCard();
        } else {
            handleAttackCard();
        }
        
        randomizeNextCard();
        scheduleEnemyAttack();
    }
    
    /**
     * Schedules the enemy attack with a delay.
     */
    private void scheduleEnemyAttack() {
        Timer timer = new Timer(500, e -> {
            ((Timer)e.getSource()).stop();
            handleEnemyAttack();
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    /**
     * Handles the defense card effect.
     */
    private void handleDefenseCard() {
        int defRoll = random.nextInt(3); // 0, 1, or 2
        double defense = defRoll + baseMinDefense;
        int defenseGain = (int)Math.floor(defense);
        defCounter += defenseGain;
        
        System.out.println("\n=== Defense Card Used ===");
        System.out.println("Base Defense: " + baseMinDefense);
        System.out.println("Max Defense: " + baseMaxDefense);
        System.out.println("Roll: " + defRoll);
        System.out.println("Defense Gained: " + defenseGain);
        System.out.println("Current Defense: " + defCounter);
    }
    
    /**
     * Handles the attack card effect.
     */
    private void handleAttackCard() {
        int damage = calculateDamage();
        
        // Check for armor break with penetration chance
        if (hasArmor) {
            if (hasArmorPenetration) {
                // Armor penetration - deal damage minus armor value
                int finalDamage = Math.max(0, damage - enemyArmor);
                System.out.println("Armor penetration! Base damage: " + damage + ", Armor: " + enemyArmor + ", Final damage: " + finalDamage);
                enemyHP = Math.max(0, enemyHP - finalDamage);
            } else {
                // Normal armor break
                hasArmor = false;
                enemyArmor = 0;
                System.out.println("Enemy armor broken!");
            }
        } else {
            enemyHP = Math.max(0, enemyHP - damage);
        }
        
        if (enemyHP <= 0) {
            isGameOver = true;
            //System.out.println("Enemies defeated: " + enemiesDefeated);
        }
    }
    
    /**
     * Calculates damage for an attack, including critical hit chance.
     */
    private int calculateDamage() {
        double damage = random.nextDouble() * (baseMaxDamage - baseMinDamage) + baseMinDamage;
        int finalDamage = (int)Math.floor(damage);
        
        System.out.println("\n=== Attack Damage ===");
        if (random.nextDouble() < CRITICAL_HIT_CHANCE) {
            finalDamage *= 2;
            System.out.println("Critical Hit!");
            System.out.println("Base Damage Range: " + baseMinDamage + "-" + baseMaxDamage);
            System.out.println("Final Damage: " + finalDamage);
        } else {
            System.out.println("Normal Hit");
            System.out.println("Damage Range: " + baseMinDamage + "-" + baseMaxDamage);
            System.out.println("Damage Dealt: " + finalDamage);
        }
        return finalDamage;
    }
    
    /**
     * Handles the enemy's attack.
     */
    public void handleEnemyAttack() {
        if (enemyHP <= 0) return;
        
        int damage = calculateEnemyDamage();
        applyDamage(damage);
    }
    
    /**
     * Calculates damage for an enemy attack, including critical hit chance.
     */
    private int calculateEnemyDamage() {
        int maxDamage = MAX_ENEMY_DAMAGE + enemyDamageBonus;
        int damage = random.nextInt(maxDamage - MIN_ENEMY_DAMAGE + 1) + MIN_ENEMY_DAMAGE;
        
        System.out.println("\n=== Enemy Attack ===");
        if (random.nextDouble() < ENEMY_CRIT_CHANCE) {
            double critDamage = damage * ENEMY_CRIT_MULTIPLIER;
            damage = (int)Math.ceil(critDamage);
            System.out.println("Critical Hit!");
            System.out.println("Base Damage: " + (damage / ENEMY_CRIT_MULTIPLIER));
            System.out.println("Crit Multiplier: " + ENEMY_CRIT_MULTIPLIER + "x");
            System.out.println("Final Damage: " + damage);
        } else {
            System.out.println("Normal Hit");
            System.out.println("Damage Range: " + MIN_ENEMY_DAMAGE + "-" + maxDamage);
            System.out.println("Damage Dealt: " + damage);
        }
        return damage;
    }
    
    /**
     * Applies damage to the player, considering defense.
     * @param damage The amount of damage to apply
     */
    private void applyDamage(int damage) {
        if (damage <= 0) return;
        
        // First try to block with defense
        while (damage > 0 && defCounter > 0) {
            defCounter--;
            damage--;
        }
        
        // Then apply remaining damage to HP
        while (damage > 0 && hpCounter > 0) {
            reduceHP();
            damage--;
        }
    }
    
    /**
     * Reduces player HP and checks for game over.
     */
    private void reduceHP() {
        if (hpCounter > 0) {
            hpCounter--;
            System.out.println("\n=== Player HP Update ===");
            System.out.println("HP Reduced to: " + hpCounter);
            if (hpCounter <= 0) {
                System.out.println("Player has died!");
                isPlayerDead = true;
                isGameOver = true;
            }
        }
    }
    
    /**
     * Applies an upgrade based on the selected type.
     */
    public void applyUpgrade(int upgradeType) {

        enemiesDefeated++;
        
        System.out.println("\n=== Applying Upgrade ===");
        System.out.println("Type: " + upgradeType);
        System.out.println("\nBefore Upgrade:");
        System.out.println("HP: " + basePlayerHP);
        System.out.println("Defense: " + basePlayerDefense);
        System.out.println("Attack Range: " + baseMinDamage + "-" + baseMaxDamage);
        System.out.println("Defense Range: " + baseMinDefense + "-" + baseMaxDefense);
        System.out.println("Enemies Defeated: " + enemiesDefeated);

        switch (upgradeType) {
            case 0: // Health Upgrade
                basePlayerHP++;
                hpCounter = basePlayerHP;
                System.out.println("\nHealth upgraded to: " + basePlayerHP);
                break;
            case 1: // Defense Upgrade
                basePlayerDefense++;
                defCounter = basePlayerDefense;
                System.out.println("\nDefense upgraded to: " + basePlayerDefense);
                break;
            case 2: // Attack Upgrade
                attackUpgrades++;
                baseMaxDamage = INITIAL_MAX_DAMAGE + (attackUpgrades * 0.5);
                System.out.println("\nAttack upgraded to: " + baseMinDamage + "-" + baseMaxDamage);
                System.out.println("Upgrade Level: " + attackUpgrades);
                break;
            case 3: // Shield Upgrade
                shieldUpgrades++;
                baseMinDefense = INITIAL_MIN_DEFENSE + (shieldUpgrades * 0.5);
                baseMaxDefense = INITIAL_MAX_DEFENSE + (shieldUpgrades * 0.5);
                System.out.println("\nShield upgraded to: " + baseMinDefense + "-" + baseMaxDefense);
                System.out.println("Upgrade Level: " + shieldUpgrades);
                break;
        }

        System.out.println("\nAfter Upgrade:");
        System.out.println("HP: " + basePlayerHP + " (Current: " + hpCounter + ")");
        System.out.println("Defense: " + basePlayerDefense + " (Current: " + defCounter + ")");
        System.out.println("Attack Range: " + baseMinDamage + "-" + baseMaxDamage);
        System.out.println("Defense Range: " + baseMinDefense + "-" + baseMaxDefense);
    }
    
    // Getters
    public String getCurrentSpritePath() { return currentSpritePath; }
    public String getCurrentSpriteName() { return currentSpriteName; }
    public String getNextCardPath() { return nextCardPath; }
    public String getNextCardType() { return nextCardType; }
    public int getHpCounter() { return hpCounter; }
    public int getDefCounter() { return defCounter; }
    public int getEnemyHP() { return enemyHP; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isPlayerDead() { return isPlayerDead; }
    public int getEnemiesDefeated() { return enemiesDefeated;}
} 