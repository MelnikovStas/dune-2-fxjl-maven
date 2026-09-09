package com.dune.rts;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.user.MouseButton;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;

/**
 * Dune-inspired Real-Time Strategy Game
 * Build buildings, gather resources, train units, and defeat the enemy AI
 */
public class DuneRTSApp extends GameApplication {

    // Game settings
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    
    // Resource types
    public enum ResourceType { SPICE, MONEY }
    
    // Player factions
    public enum Faction { PLAYER, ENEMY }
    
    // Building types
    public enum BuildingType {
        BASE(100, 60, Color.BLUE, 500),
        REFINERY(40, 40, Color.ORANGE, 300),
        BARRACKS(30, 30, Color.GREEN, 200),
        FACTORY(50, 40, Color.GRAY, 400),
        AIRFIELD(60, 50, Color.CYAN, 600);
        
        public final int width;
        public final int height;
        public final Color color;
        public final int cost;
        
        BuildingType(int width, int height, Color color, int cost) {
            this.width = width;
            this.height = height;
            this.color = color;
            this.cost = cost;
        }
    }
    
    // Unit types
    public enum UnitType {
        INFANTRY(15, 15, Color.LIME, 50, 10, 5, 1.0),
        TANK(25, 20, Color.DARKGREEN, 150, 30, 15, 0.6),
        PLANE(20, 15, Color.YELLOW, 200, 25, 20, 2.0),
        HELICOPTER(18, 18, Color.GOLD, 180, 20, 18, 1.5);
        
        public final int width;
        public final int height;
        public final Color color;
        public final int cost;
        public final int health;
        public final int damage;
        public final double speed;
        
        UnitType(int width, int height, Color color, int cost, int health, int damage, double speed) {
            this.width = width;
            this.height = height;
            this.color = color;
            this.cost = cost;
            this.health = health;
            this.damage = damage;
            this.speed = speed;
        }
    }
    
    // Game state
    private Map<Faction, Map<ResourceType, Integer>> resources;
    private Map<Faction, List<Entity>> buildings;
    private Map<Faction, List<Entity>> units;
    private Entity selectedUnit = null;
    private Rectangle selectionBox;
    private Point2D selectionStart;
    private boolean isSelecting = false;
    
    // UI elements
    private Text spiceText;
    private Text moneyText;
    private Text unitCountText;
    private Text buildingInfoText;
    
    @Override
    protected void initSettings() {
        setWidth(WINDOW_WIDTH);
        setHeight(WINDOW_HEIGHT);
        setTitle("Dune RTS - Spice Wars");
    }
    
    @Override
    protected void onPreInit() {
        // Initialize game state
        resources = new HashMap<>();
        buildings = new HashMap<>();
        units = new HashMap<>();
        
        for (Faction f : Faction.values()) {
            resources.put(f, new HashMap<>());
            resources.get(f).put(ResourceType.SPICE, 1000);
            resources.get(f).put(ResourceType.MONEY, 1000);
            buildings.put(f, new ArrayList<>());
            units.put(f, new ArrayList<>());
        }
    }
    
    @Override
    protected void initInput() {
        Input input = getGameScene().getInput();
        
        // Left click - select unit or issue command
        input.onMouseButtonPressed(MouseButton.PRIMARY, e -> {
            Point2D mousePos = new Point2D(e.getX(), e.getY());
            
            if (isInUIArea(mousePos)) {
                handleUIClick(mousePos);
                return;
            }
            
            selectionStart = mousePos;
            isSelecting = true;
            
            // Create selection box
            selectionBox = new Rectangle(0, 0);
            selectionBox.setFill(Color.rgb(0, 255, 0, 0.3));
            selectionBox.setStroke(Color.LIME);
            getGameScene().addUINode(selectionBox);
        });
        
        input.onMouseButtonDragged(MouseButton.PRIMARY, e -> {
            if (isSelecting && selectionBox != null) {
                double x = Math.min(selectionStart.getX(), e.getX());
                double y = Math.min(selectionStart.getY(), e.getY());
                double w = Math.abs(e.getX() - selectionStart.getX());
                double h = Math.abs(e.getY() - selectionStart.getY());
                
                selectionBox.setX(x);
                selectionBox.setY(y);
                selectionBox.setWidth(w);
                selectionBox.setHeight(h);
            }
        });
        
        input.onMouseButtonReleased(MouseButton.PRIMARY, e -> {
            if (isSelecting) {
                isSelecting = false;
                
                if (selectionBox != null) {
                    getGameScene().removeUINode(selectionBox);
                    
                    // Select units in selection box
                    selectUnitsInBox(selectionBox);
                    selectionBox = null;
                } else {
                    // Click without drag - try to select single unit
                    selectUnitAtPoint(selectionStart);
                }
            }
        });
        
        // Right click - move/attack command
        input.onMouseButtonPressed(MouseButton.SECONDARY, e -> {
            Point2D mousePos = new Point2D(e.getX(), e.getY());
            
            if (selectedUnit != null && !isInUIArea(mousePos)) {
                issueCommand(selectedUnit, mousePos);
            }
        });
        
        // Keyboard shortcuts for building
        input.onKey(KeyCode.B, () -> showBuildMenu());
        input.onKey(KeyCode.U, () -> showUnitMenu());
        input.onKey(KeyCode.SPACE, () -> centerOnSelectedUnit());
    }
    
    @Override
    protected void initGame() {
        // Create battlefield background
        FXGL.entityBuilder()
            .view(new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT, Color.rgb(210, 180, 140)))
            .buildAndAttach();
        
        // Spawn player base
        spawnBuilding(BuildingType.BASE, 100, 500, Faction.PLAYER);
        
        // Spawn enemy base
        spawnBuilding(BuildingType.BASE, WINDOW_WIDTH - 200, 100, Faction.ENEMY);
        
        // Add some spice fields (resource nodes)
        spawnSpiceField(400, 300);
        spawnSpiceField(800, 400);
        spawnSpiceField(600, 200);
        
        // Initialize UI
        initUI();
        
        // Start enemy AI
        startEnemyAI();
    }
    
    private void initUI() {
        // Resource display
        spiceText = new Text("Spice: 1000");
        spiceText.setFont(Font.font("Arial", 16));
        spiceText.setFill(Color.ORANGE);
        spiceText.setX(10);
        spiceText.setY(30);
        getGameScene().addUINode(spiceText);
        
        moneyText = new Text("Money: 1000");
        moneyText.setFont(Font.font("Arial", 16));
        moneyText.setFill(Color.GOLD);
        moneyText.setX(10);
        moneyText.setY(50);
        getGameScene().addUINode(moneyText);
        
        unitCountText = new Text("Units: 0");
        unitCountText.setFont(Font.font("Arial", 14));
        unitCountText.setFill(Color.WHITE);
        unitCountText.setX(10);
        unitCountText.setY(70);
        getGameScene().addUINode(unitCountText);
        
        buildingInfoText = new Text("");
        buildingInfoText.setFont(Font.font("Arial", 14));
        buildingInfoText.setFill(Color.CYAN);
        buildingInfoText.setX(10);
        buildingInfoText.setY(90);
        getGameScene().addUINode(buildingInfoText);
        
        // Instructions
        Text instructions = new Text("LMB: Select | RMB: Move/Attack | B: Build | U: Units");
        instructions.setFont(Font.font("Arial", 12));
        instructions.setFill(Color.WHITE);
        instructions.setX(WINDOW_WIDTH - 400);
        instructions.setY(30);
        getGameScene().addUINode(instructions);
    }
    
    private boolean isInUIArea(Point2D pos) {
        return pos.getY() < 120 || pos.getX() > WINDOW_WIDTH - 200;
    }
    
    private void handleUIClick(Point2D pos) {
        // Handle UI interactions (build menu, unit menu, etc.)
        if (pos.getX() > WINDOW_WIDTH - 200 && pos.getY() < 120) {
            // Right panel clicked
        }
    }
    
    private void selectUnitsInBox(Rectangle box) {
        List<Entity> selected = new ArrayList<>();
        
        for (Entity unit : units.get(Faction.PLAYER)) {
            Point2D unitPos = unit.getPosition();
            if (box.contains(unitPos.getX(), unitPos.getY())) {
                selected.add(unit);
            }
        }
        
        if (selected.size() == 1) {
            selectedUnit = selected.get(0);
            highlightUnit(selectedUnit);
        } else if (selected.size() > 1) {
            selectedUnit = selected.get(0); // Select first as primary
        } else {
            selectedUnit = null;
        }
    }
    
    private void selectUnitAtPoint(Point2D pos) {
        for (Entity unit : units.get(Faction.PLAYER)) {
            Point2D unitPos = unit.getPosition();
            if (pos.distance(unitPos) < 30) {
                selectedUnit = unit;
                highlightUnit(unit);
                return;
            }
        }
        selectedUnit = null;
    }
    
    private void highlightUnit(Entity unit) {
        // Visual feedback for selected unit
        unit.getViewComponent().getChildren().forEach(node -> {
            if (node instanceof Rectangle) {
                ((Rectangle) node).setStroke(Color.YELLOW);
                ((Rectangle) node).setStrokeWidth(2);
            }
        });
    }
    
    private void issueCommand(Entity unit, Point2D targetPos) {
        // Check if clicking on enemy unit/building
        Entity target = getEntityAtPosition(targetPos);
        
        if (target != null && isEnemy(unit, target)) {
            // Attack command
            unit.with("target", target);
            unit.with("command", "ATTACK");
        } else {
            // Move command
            unit.with("targetPos", targetPos);
            unit.with("command", "MOVE");
        }
    }
    
    private Entity getEntityAtPosition(Point2D pos) {
        // Check enemy units
        for (Entity unit : units.get(Faction.ENEMY)) {
            if (pos.distance(unit.getPosition()) < 30) {
                return unit;
            }
        }
        
        // Check enemy buildings
        for (Entity building : buildings.get(Faction.ENEMY)) {
            if (building.getViewComponent().getBoundsInParent().contains(pos)) {
                return building;
            }
        }
        
        return null;
    }
    
    private boolean isEnemy(Entity unit, Entity target) {
        Faction unitFaction = unit.getObject("faction");
        Faction targetFaction = target.getObject("faction");
        return unitFaction != targetFaction;
    }
    
    private void spawnBuilding(BuildingType type, double x, double y, Faction faction) {
        Rectangle view = new Rectangle(type.width, type.height, type.color);
        
        Entity building = FXGL.entityBuilder()
            .at(x, y)
            .view(view)
            .with("type", type)
            .with("faction", faction)
            .with("health", 100)
            .with("maxHealth", 100)
            .buildAndAttach();
        
        buildings.get(faction).add(building);
        
        // Add building label
        Text label = new Text(type.name());
        label.setFont(Font.font("Arial", 10));
        label.setFill(Color.WHITE);
        building.getViewComponent().addChild(label);
        
        updateUI();
    }
    
    private void spawnUnit(UnitType type, double x, double y, Faction faction) {
        Rectangle view = new Rectangle(type.width, type.height, type.color);
        
        Entity unit = FXGL.entityBuilder()
            .at(x, y)
            .view(view)
            .with("type", type)
            .with("faction", faction)
            .with("health", type.health)
            .with("maxHealth", type.health)
            .with("damage", type.damage)
            .with("speed", type.speed)
            .with("command", "IDLE")
            .buildAndAttach();
        
        units.get(faction).add(unit);
        updateUI();
    }
    
    private void spawnSpiceField(double x, double y) {
        Entity spice = FXGL.entityBuilder()
            .at(x, y)
            .view(new Rectangle(60, 60, Color.rgb(255, 140, 0, 0.7)))
            .with("type", "SPICE_FIELD")
            .with("amount", 5000)
            .buildAndAttach();
        
        // Add label
        Text label = new Text("Spice");
        label.setFont(Font.font("Arial", 12));
        label.setFill(Color.WHITE);
        spice.getViewComponent().addChild(label);
    }
    
    private void showBuildMenu() {
        buildingInfoText.setText("Build: [1]Refinery [2]Barracks [3]Factory [4]Airfield");
    }
    
    private void showUnitMenu() {
        buildingInfoText.setText("Train: [1]Infantry [2]Tank [3]Plane [4]Helicopter");
    }
    
    private void centerOnSelectedUnit() {
        if (selectedUnit != null) {
            Point2D pos = selectedUnit.getPosition();
            // Center camera logic would go here
        }
    }
    
    private void updateUI() {
        int spice = resources.get(Faction.PLAYER).get(ResourceType.SPICE);
        int money = resources.get(Faction.PLAYER).get(ResourceType.MONEY);
        int unitCount = units.get(Faction.PLAYER).size();
        
        spiceText.setText("Spice: " + spice);
        moneyText.setText("Money: " + money);
        unitCountText.setText("Units: " + unitCount);
    }
    
    private void startEnemyAI() {
        // Simple AI that periodically builds and attacks
        FXGL.run(() -> {
            aiTurn();
        }, Duration.seconds(2)); // Every 2 seconds
    }
    
    private void aiTurn() {
        Faction ai = Faction.ENEMY;
        int money = resources.get(ai).get(ResourceType.MONEY);
        int spice = resources.get(ai).get(ResourceType.SPICE);
        
        Random rand = new Random();
        
        // AI decision making
        if (money >= 300 && rand.nextDouble() < 0.3) {
            // Build refinery if affordable
            Entity base = buildings.get(ai).stream()
                .filter(b -> b.getObject("type") == BuildingType.BASE)
                .findFirst().orElse(null);
            
            if (base != null) {
                Point2D basePos = base.getPosition();
                spawnBuilding(BuildingType.REFINERY, 
                    basePos.getX() + 50 + rand.nextInt(100), 
                    basePos.getY() - 50 + rand.nextInt(100), 
                    ai);
                resources.get(ai).put(ResourceType.MONEY, money - 300);
            }
        }
        
        if (money >= 50 && rand.nextDouble() < 0.4) {
            // Train infantry
            Entity barracks = buildings.get(ai).stream()
                .filter(b -> b.getObject("type") == BuildingType.BARRACKS)
                .findFirst().orElse(null);
            
            if (barracks != null) {
                Point2D barracksPos = barracks.getPosition();
                spawnUnit(UnitType.INFANTRY, 
                    barracksPos.getX() + 30, 
                    barracksPos.getY() + 30, 
                    ai);
                resources.get(ai).put(ResourceType.MONEY, money - 50);
            }
        }
        
        // AI attack logic
        if (units.get(ai).size() >= 3) {
            Entity playerBase = buildings.get(Faction.PLAYER).stream()
                .filter(b -> b.getObject("type") == BuildingType.BASE)
                .findFirst().orElse(null);
            
            if (playerBase != null) {
                for (Entity unit : units.get(ai)) {
                    unit.with("target", playerBase);
                    unit.with("command", "ATTACK");
                }
            }
        }
    }
    
    @Override
    protected void onUpdate(double tpf) {
        // Update all units
        for (Faction faction : Faction.values()) {
            for (Entity unit : units.get(faction)) {
                updateUnit(unit, tpf);
            }
        }
        
        // Resource generation from refineries
        generateResources();
        
        // Check win/lose conditions
        checkGameEnd();
    }
    
    private void updateUnit(Entity unit, double tpf) {
        String command = unit.getObject("command");
        UnitType type = unit.getObject("type");
        Faction faction = unit.getObject("faction");
        
        switch (command) {
            case "MOVE":
                Point2D targetPos = unit.getObject("targetPos");
                if (targetPos != null) {
                    moveUnitTowards(unit, targetPos, type.speed, tpf);
                }
                break;
                
            case "ATTACK":
                Entity target = unit.getObject("target");
                if (target != null && target.isFromWorld()) {
                    double distance = unit.getPosition().distance(target.getPosition());
                    if (distance <= 100) {
                        // In range - attack
                        attackTarget(unit, target);
                    } else {
                        // Move towards target
                        moveUnitTowards(unit, target.getPosition(), type.speed, tpf);
                    }
                } else {
                    unit.with("command", "IDLE");
                }
                break;
        }
    }
    
    private void moveUnitTowards(Entity unit, Point2D target, double speed, double tpf) {
        Point2D currentPos = unit.getPosition();
        Point2D direction = target.subtract(currentPos).normalize();
        
        double moveDistance = speed * tpf;
        double newX = currentPos.getX() + direction.getX() * moveDistance;
        double newY = currentPos.getY() + direction.getY() * moveDistance;
        
        // Boundary check
        newX = Math.max(0, Math.min(WINDOW_WIDTH, newX));
        newY = Math.max(120, Math.min(WINDOW_HEIGHT, newY));
        
        unit.setPosition(newX, newY);
        
        // Check if reached destination
        if (currentPos.distance(newX, newY) < 5) {
            unit.with("command", "IDLE");
        }
    }
    
    private void attackTarget(Entity attacker, Entity target) {
        int damage = attacker.getObject("damage");
        int currentHealth = target.getObject("health");
        
        int newHealth = currentHealth - damage;
        target.with("health", newHealth);
        
        if (newHealth <= 0) {
            destroyEntity(target);
        }
    }
    
    private void destroyEntity(Entity entity) {
        // Determine if it's a building or unit
        Object type = entity.getObject("type");
        if (type instanceof BuildingType) {
            Faction faction = entity.getObject("faction");
            buildings.get(faction).remove(entity);
        } else if (type instanceof UnitType) {
            Faction faction = entity.getObject("faction");
            units.get(faction).remove(entity);
        }
        
        entity.removeFromWorld();
        updateUI();
    }
    
    private void generateResources() {
        for (Faction faction : Faction.values()) {
            int spiceIncome = 0;
            
            // Count refineries
            for (Entity building : buildings.get(faction)) {
                BuildingType bType = building.getObject("type");
                if (bType == BuildingType.REFINERY) {
                    spiceIncome += 10;
                }
            }
            
            // Add income
            int currentSpice = resources.get(faction).get(ResourceType.SPICE);
            resources.get(faction).put(ResourceType.SPICE, currentSpice + spiceIncome);
            
            // Convert spice to money periodically
            if (currentSpice >= 100) {
                int convertAmount = Math.min(currentSpice, 100);
                resources.get(faction).put(ResourceType.SPICE, currentSpice - convertAmount);
                int currentMoney = resources.get(faction).get(ResourceType.MONEY);
                resources.get(faction).put(ResourceType.MONEY, currentMoney + convertAmount);
            }
        }
        
        updateUI();
    }
    
    private void checkGameEnd() {
        // Check if player lost all buildings
        if (buildings.get(Faction.PLAYER).isEmpty()) {
            showMessage("DEFEAT! The enemy has destroyed your base.");
        }
        
        // Check if enemy lost all buildings
        if (buildings.get(Faction.ENEMY).isEmpty()) {
            showMessage("VICTORY! You have destroyed the enemy base.");
        }
    }
    
    private void showMessage(String message) {
        Text msg = new Text(message);
        msg.setFont(Font.font("Arial", 32));
        msg.setFill(Color.RED);
        msg.setX(WINDOW_WIDTH / 2 - 200);
        msg.setY(WINDOW_HEIGHT / 2);
        FXGL.addUINode(msg);
        
        // Remove after 3 seconds
        FXGL.runOnce(() -> FXGL.removeUINode(msg), Duration.seconds(3));
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
