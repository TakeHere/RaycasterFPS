package fr.takehere.shoot3d;

import fr.takehere.ethereal.Game;
import fr.takehere.ethereal.GameScene;
import fr.takehere.ethereal.GameWindow;
import fr.takehere.ethereal.components.Actor;
import fr.takehere.ethereal.components.Pawn;
import fr.takehere.ethereal.utils.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class Enemy extends Actor{

    public static List<Enemy> enemies = new ArrayList<>();

    public Enemy(Vector2 location, Dimension dimension) {
        super("enemy", location, dimension, RessourcesManager.getImage("enemy"), Shoot3D.getInstance());

        enemies.add(this);
    }

    public void destroy(){
        Game.runNextFrame(() -> {
            enemies.remove(this);
            if (enemies.size() == 0 && spawningEnemies == false){
                Maps.get().nextMap();
                Shoot3D.getInstance().player.location = new Vector2(60,60);
            }
        });
        super.destroy();
    }

    public static boolean spawningEnemies = true;

    public static void spawnEnemies(){
        spawningEnemies = true;
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(2000), event -> {
            Enemy enemy = new Enemy(new Vector2(MathUtils.randomNumberBetween(125,625),MathUtils.randomNumberBetween(125,625)), new Dimension(50,50));
            enemy.velocity = MathUtils.randomDirection();
            enemy.visible = false;
        }));

        timeline.setOnFinished(event -> {
            Enemy.spawningEnemies = false;
        });

        timeline.setCycleCount(MathUtils.randomNumberBetween(10,15));
        timeline.play();
    }
}
