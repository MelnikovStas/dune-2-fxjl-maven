# Dune RTS - Spice Wars

A real-time strategy game inspired by Dune 2, built with Java and FXGL game engine.

## Features

- **Resource Management**: Collect spice and convert it to money
- **Building Construction**: Build various structures including:
  - Base (main building)
  - Refinery (generates spice income)
  - Barracks (trains infantry)
  - Factory (produces tanks)
  - Airfield (creates aircraft)
  
- **Unit Types**:
  - Infantry - cheap and fast
  - Tanks - heavy armor and damage
  - Planes - fast air units
  - Helicopters - versatile air support
  
- **Enemy AI**: Computer opponent that builds bases, trains units, and attacks
- **Combat System**: Move, attack, and destroy enemy forces
- **Win/Lose Conditions**: Destroy enemy base to win, lose if your base is destroyed

## Requirements

- Java 17 or higher
- Maven 3.6+
- FXGL 17.3 (automatically downloaded by Maven)

## Building the Project

```bash
cd dune-rts-game
mvn clean compile
```

## Running the Game

```bash
mvn javafx:run
```

Or run directly from IDE by executing `com.dune.rts.DuneRTSApp` main class.

## Controls

- **Left Mouse Button (Drag)**: Select multiple units
- **Left Mouse Button (Click)**: Select single unit
- **Right Mouse Button**: Move selected unit or attack target
- **B Key**: Show build menu
- **U Key**: Show unit menu
- **Space**: Center on selected unit

## Gameplay Tips

1. Start by building a Refinery near your base to generate spice income
2. Convert spice to money automatically when you have 100+ spice
3. Build Barracks to train infantry units
4. Create a mix of ground and air units for effective attacks
5. Protect your base while attacking the enemy
6. The AI will periodically build units and attack when it has enough forces

## Project Structure

```
dune-rts-game/
├── pom.xml                          # Maven configuration
└── src/main/java/com/dune/rts/
    └── DuneRTSApp.java             # Main game application
```

## Technology Stack

- **Java 17**: Programming language
- **FXGL 17.3**: Game engine based on JavaFX
- **Maven**: Build automation and dependency management

## License

This is a fan project inspired by the classic Dune 2 game. All rights to the Dune franchise belong to their respective owners.
