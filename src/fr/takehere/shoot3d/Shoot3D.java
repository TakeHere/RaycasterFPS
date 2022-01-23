package fr.takehere.shoot3d;

import fr.takehere.ethereal.Game;
import fr.takehere.ethereal.GameWindow;
import fr.takehere.ethereal.components.Actor;
import fr.takehere.ethereal.utils.*;
import fr.takehere.ethereal.utils.Dimension;
import fr.takehere.ethereal.utils.Rectangle;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Shoot3D extends Game {
    private static Shoot3D instance;

    public Actor player;
    public AnimationPlayer gunAnim;

    public boolean drawMap = false;
    boolean ignoreMouseEvent = false;
    boolean focused = true;

    public ParticleGenerator particleGenerator;

    Image wallTexture;
    Image cobblestoneTexture;
    public List<Image> wallCuts = new ArrayList<>();
    public List<Image> cobblestoneCuts = new ArrayList<>();

    @Override
    public void init() {
        cobblestoneTexture = RessourcesManager.addImage("cobblestone", "/resources/cobblestone.png");
        wallTexture = RessourcesManager.addImage("bricks", "/resources/bricks.png");
        RessourcesManager.addImage("enemy", "/resources/enemy.png");
        RessourcesManager.addImage("crosshair", "/resources/crosshair.png");
        RessourcesManager.addImage("smoke", "/resources/smoke.png");
        RessourcesManager.addImage("placeholder", "/resources/placeholder.png");

        RessourcesManager.addSound("shoot", "/resources/sounds/shotgun.wav", getClass());


        gunAnim = new AnimationPlayer("/resources/shootAnim/", 1);

        player = new Actor("player", new Vector2(60,60), new Dimension(25,25), RessourcesManager.getImage("placeholder"), this);
        player.visible = false;

        particleGenerator = new ParticleGenerator(new Vector2(0,0), new Dimension(30,30), RessourcesManager.getImage("smoke"), false, 20, 1,3,700,this);

        PixelReader wallReader = wallTexture.getPixelReader();
        for (int i = 0; i < wallTexture.getWidth(); i++) {
            WritableImage mappedWall = new WritableImage(wallReader, i, 0, 1, (int) wallTexture.getHeight());
            wallCuts.add(mappedWall);
        }
        PixelReader cobblestoneReader = cobblestoneTexture.getPixelReader();
        for (int i = 0; i < cobblestoneTexture.getWidth(); i++) {
            WritableImage mappedWall = new WritableImage(cobblestoneReader, i, 0, 1, (int) cobblestoneTexture.getHeight());
            cobblestoneCuts.add(mappedWall);
        }

        GameWindow.scene.setCursor(Cursor.NONE);

        EventHandler mouseMouvement = new EventHandler<MouseEvent>() {
            Robot mouseMover = new Robot();

            double mouseDeltaX = 0;
            double mouseDeltaY = 0;

            @Override
            public void handle(MouseEvent event) {
                if (focused){
                    if(ignoreMouseEvent) {
                        ignoreMouseEvent = false;
                        return;
                    }

                    mouseDeltaX = (Math.round(event.getScreenX() - (GameWindow.stage.getX() + (width / 2.0))) / 5.0);
                    mouseDeltaY = (-Math.round((event.getScreenY() - (GameWindow.stage.getY() + (height / 2.0)))) / 5.0);

                    player.addRotation(mouseDeltaX);

                    ignoreMouseEvent = true;
                    mouseMover.mouseMove((int) (GameWindow.stage.getX() + (width / 2.0)), (int) (GameWindow.stage.getY() + (height / 2.0)));
                }
            }
        };

        GameWindow.scene.setOnMouseMoved(mouseMouvement);
        GameWindow.scene.setOnMouseDragged(mouseMouvement);

        GameWindow.scene.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
            focused = true;
            GameWindow.scene.setCursor(Cursor.NONE);

            if (mouseEvent.getButton() == MouseButton.PRIMARY){

                if (!gunAnim.isPlaying){
                    Shoot3D.getInstance().particleGenerator.location = new Vector2(Game.width/2, Game.height/2 + 50);
                    Shoot3D.getInstance().particleGenerator.generate();

                    SoundUtils.playSound("shoot", 0.1f, false);
                    Raycaster.get().shoot(player);
                    gunAnim.play();
                }
            }
        });

        GameWindow.scene.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode().getCode() == KeyCode.ESCAPE.getCode()){
                focused = false;
                GameWindow.scene.setCursor(Cursor.DEFAULT);
            }else if (ke.getCode().getCode() == KeyCode.SHIFT.getCode()){
                playerSpeed = 5;
            }
        });

        GameWindow.scene.addEventFilter(KeyEvent.KEY_RELEASED, ke -> {
            if (ke.getCode().getCode() == KeyCode.SHIFT.getCode()){
                playerSpeed = 2;
            }
        });

        Enemy.spawnEnemies();
    }

    float playerSpeed = 2;

    int up = KeyCode.Z.getCode();
    int down = KeyCode.S.getCode();
    int right = KeyCode.D.getCode();
    int left = KeyCode.Q.getCode();

    @Override
    public void gameLoop(double v) {
        Vector2 forwardVector = new Vector2(Math.cos(Math.toRadians(player.getRotation())), Math.sin(Math.toRadians(player.getRotation()))).normalize();
        Vector2 rightVector = MathUtils.rotateVector(forwardVector, 90).normalize();
        Vector2 leftVector = MathUtils.rotateVector(forwardVector, -90).normalize();

        if (isPressed(up)){
            player.velocity = player.velocity.add(forwardVector.multiply(playerSpeed));
        }else if (isPressed(down)){
            player.velocity = player.velocity.add(forwardVector.multiply(playerSpeed * -1));
        }

        if (isPressed(right)){
            player.velocity = player.velocity.add(rightVector.multiply(playerSpeed));
        }else if (isPressed(left)){
            player.velocity = player.velocity.add(leftVector.multiply(playerSpeed));
        }

        player.velocity = player.velocity.normalize().multiply(playerSpeed);

        if (!isPressed(up) && !isPressed(down) && !isPressed(right) && !isPressed(left)) player.velocity = new Vector2(0,0);

        //Collision detection
        Vector2 nextLocation = MathUtils.getCenterOfPawn(player).add(player.velocity);

        Maps.get().rectanglesData.forEach((rectangle, integer) -> {
            if (MathUtils.isColliding(new Rectangle(nextLocation.x, nextLocation.y, 1,1), rectangle)){
                player.velocity = new Vector2(0,0);
            }
        });

        if (drawMap){
            Raycaster.get().drawMap();
            Raycaster.get().castRay(0,player);

            //Draw grid
            GraphicsContext gc = getGc();

            gc.setStroke(Color.GRAY);
            gc.setLineWidth(2);
            for(int i = 0 ; i < 1050 ; i+=50){
                gc.strokeLine(i, 0, i, Game.height - (Game.height%50));
            }
            for(int i = 0 ; i < Game.height ; i+=50){
                gc.strokeLine(0, i, Game.width, i);
            }
        }else {
            Raycaster.get().render();
        }

        MathUtils.drawImageCenter(new Dimension(Game.width, Game.height), RessourcesManager.getImage("crosshair"), new Dimension(50,50), getGc());

        for (Enemy enemy : Enemy.enemies) {
            if (!MathUtils.isColliding(enemy.boundingBox, new Rectangle(0,0,Game.width, Game.height))){
                enemy.destroy();
            }
        }

        GraphicsContext gc = getGc();
        int fontSize = 60;

        gc.setFont(new Font("Bahnschrift", fontSize));
        gc.setFill(Color.YELLOW);
        gc.fillText("FPS: " + GameWindow.lastFps, 0, fontSize);

        Text enemiesLeft = new Text("Enemies left: " + Enemy.enemies.size());
        enemiesLeft.setFont(gc.getFont());
        gc.fillText(enemiesLeft.getText(), Game.width - enemiesLeft.getBoundsInLocal().getWidth(), fontSize);
    }

    public Shoot3D(String title, int width, int height, boolean titleBar) {
        super(title, width, height, titleBar);
    }
    public static void main(String[] args) {
        java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        instance = new Shoot3D("Shoot3D", (int) screenSize.getWidth(), (int) screenSize.getHeight(), false);
        instance.launch();
    }

    public static Shoot3D getInstance() {
        return instance;
    }
}
