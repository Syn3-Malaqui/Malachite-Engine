# Malachite Engine

A turn-based card battle game engine built in Java, featuring strategic combat mechanics and progressive difficulty scaling.

## Overview

Malachite Engine is a Java-based game engine that implements a strategic card battle system with unique mechanics including armor penetration, critical hits, and progressive enemy scaling. The engine provides a robust framework for creating engaging turn-based combat games.

## Features

- **Strategic Card System**: 
  - Attack and Defense cards with unique mechanics
  - Card randomization and prediction system
  - Visual card animations and effects

- **Combat Mechanics**:
  - Critical hit system (1% player chance, 20% enemy chance)
  - Defense penetration mechanics
  - Armor system with scaling difficulty
  - Damage and defense ranges with upgradeable stats

- **Enemy System**:
  - Progressive difficulty scaling
  - Enemy HP scaling (increases every 2 enemies)
  - Damage scaling (increases every 3 enemies)
  - Armor mechanics (introduced every 2 enemies)
  - Armor penetration abilities

- **Visual Effects**:
  - Card animations and transitions
  - Damage display effects
  - Screen shake effects
  - Flash effects for critical hits
  - Death screen animations

## Game Mechanics

### Core Stats
- Initial Player HP: 10
- Initial Player Defense: 5
- Initial Enemy HP: 15
- Damage Range: 3-5
- Defense Range: 2-4

### Special Mechanics
- Critical Hits: 1% chance for player, 20% chance for enemy (1.5x damage)
- Defense Penetration: 10% chance to ignore defense
- No Defense Damage: 30% chance to deal damage even with defense
- Next Card Prediction: 50% chance for attack card

## Requirements

- Java Runtime Environment (JRE) 8 or later
- Java Development Kit (JDK) 8 or later for development

## Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/malachite-engine.git
```

2. Navigate to the project directory:
```bash
cd malachite-engine
```

3. Compile the project:
```bash
javac *.java
```

4. Run the game:
```bash
java MarrowExecutable
```

## Project Structure

- `GamePanel.java`: Main game interface and rendering
- `GameLogic.java`: Core game mechanics and state management
- `MarrowExecutable.java`: Game entry point
- `MenuPanel.java`: Main menu interface
- `DrawingArea.java`: Drawing utilities
- `sprites/`: Directory containing game assets

## Development

The project uses a singleton pattern for game logic management and implements various design patterns for maintainability and scalability. The codebase is structured to allow easy addition of new features and modifications to existing mechanics.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
