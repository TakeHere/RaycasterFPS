package fr.takehere.shoot3d;

import fr.takehere.ethereal.Game;
import fr.takehere.ethereal.components.Actor;
import fr.takehere.ethereal.utils.*;
import fr.takehere.ethereal.utils.Rectangle;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class Raycaster {

    private static Raycaster instance;
    public Maps map;

    private Raycaster() {
        map = Maps.get();
    }

    int wallHeightScale = 60;

    int rayCount = 0;

    HashMap<Rectangle, EnemyStorage> seenEnemies = new HashMap<>();

    public void render(){
        GraphicsContext gc = Shoot3D.getInstance().getGc();
        Image enemyTexture = RessourcesManager.getImage("enemy");
        Actor player = Shoot3D.getInstance().player;

        float resolutionMultiplicator = 5;
        int fov = 90;
        int rayNumber = (int) (fov * resolutionMultiplicator);
        float rayAngle = fov / 2 * -1;
        double lineThickness = (double) Game.width / rayNumber;
        double lastRayX = 0;

        seenEnemies.clear();
        rayCount = 0;

        for (int i = 0; i < rayNumber; i++) {
            Object[] ray = castRay(rayAngle, player);

            Vector2 hitLocation = (Vector2) ray[0];
            double distance = (double) ray[1];
            int hitType = (int) ray[2];
            boolean isVerticalHit = (boolean) ray[3];

            float lineH = (float) (Game.height / distance * wallHeightScale);
            float lineUp = (Game.height/2) - (lineH/2);

            //Draw Collider
            double width = Shoot3D.getInstance().wallTexture.getWidth();
            double offset;
            if (isVerticalHit){
                offset = (hitLocation.y%map.rectSize)*(width/map.rectSize);
            }else {
                offset = (hitLocation.x%map.rectSize)*(width/map.rectSize);
            }

            if (hitType == 1){
                gc.drawImage(Shoot3D.getInstance().wallCuts.get((int) offset), lastRayX, lineUp, lineThickness, lineH);
            }else if (hitType == 2){
                gc.drawImage(Shoot3D.getInstance().cobblestoneCuts.get((int) offset), lastRayX, lineUp, lineThickness, lineH);
            }

            //gc.drawImage(wallTexture, lastRayX, lineUp, lineThickness, lineH);


            //Draw Sky
            gc.setFill(new RGBColor(32, 126, 230).color);
            gc.fillRect(lastRayX, 0, lastRayX + lineThickness, lineUp + 1);

            //Draw floor
            gc.setFill(new RGBColor(40, 40, 48).color);
            gc.fillRect(lastRayX, (lineH + lineUp), lastRayX + lineThickness, lineH + lineUp + lineUp);

            lastRayX = lastRayX + lineThickness;
            rayAngle = rayAngle + (1f/resolutionMultiplicator);
            rayCount++;
        }

        //Draw objects
        seenEnemies.forEach((rectangle, enemy) -> {
            float lineH = (float) (Game.height / enemy.distance * wallHeightScale);
            float lineUp = (Game.height/2) - (lineH/2);
            float width = (float) ((Game.width/enemy.distance) * 25);

            gc.drawImage(enemyTexture, lineThickness*enemy.rayCount, lineUp, width,lineH);
        });
    }

    public void drawMap(){
        GraphicsContext gc = Shoot3D.getInstance().getGc();

        for (Map.Entry<Rectangle, Integer> entry : map.rectanglesData.entrySet()) {
            Color color = Color.YELLOW;

            gc.setFill(color);
            gc.fillRect(entry.getKey().location.x, entry.getKey().location.y, entry.getKey().dimension.width, entry.getKey().dimension.height);
        }

        Shoot3D.getInstance().player.visible = true;
    }

    public Object[] castRay(double rayAngle, Actor player){
        Vector2 center = MathUtils.getCenterOfPawn(player);
        double angle = fixDegrees(((rayAngle + player.getRotation()) + 36000)%360);
        if (angle == 0 || angle == 360) angle = 0.001;

        double tan = Math.tan(Math.toRadians(angle));

        //------< Horizontal >------
        double Ay = 0;
        double Ax = 0;

        double Xa = 0;
        double Ya = 0;

        //Check if player is looking up or down
        if (angle > 0 && angle < 180){
            Ay = Math.floor(center.y/map.rectSize) * (map.rectSize) - 1;
            Ya = -map.rectSize;
        }else {
            Ay = Math.floor(center.y/map.rectSize) * (map.rectSize) + map.rectSize;
            Ya = map.rectSize;
        }
        Ax = center.x + (center.y-Ay)/tan;
        Xa = -Ya/tan;

        int repetitionHorizontal = 0;
        int horizontalHitType = 0;
        Rectangle horizontalEnemyRectangle = null;
        EnemyStorage horizontalEnemy = null;

        while (repetitionHorizontal < map.mapSize/map.rectSize){
            for (Enemy enemy : Enemy.enemies) {
                if (MathUtils.isColliding(new Rectangle(Ax, Ay, 1, 1), enemy.boundingBox)){
                    horizontalEnemyRectangle = enemy.boundingBox;
                    horizontalEnemy = new EnemyStorage(enemy.location, enemy.dimension);
                    horizontalEnemy.rayCount = rayCount;
                }
            }
            for (Map.Entry<Rectangle, Integer> entry : map.rectanglesData.entrySet()) {
                if (MathUtils.isColliding(new Rectangle(Ax, Ay, 1, 1), entry.getKey())){
                    repetitionHorizontal = map.mapSize/map.rectSize;
                    horizontalHitType = entry.getValue();
                    break;
                }
            }
            if (repetitionHorizontal < map.mapSize/map.rectSize){
                Ax = Ax + Xa;
                Ay = Ay + Ya;
                repetitionHorizontal++;
            }else break;
        }
        Vector2 horizontalHit = new Vector2(Ax, Ay);

        //------< Vertical >------
        double Bx;
        double By;
        double Xb;
        double Yb;

        if (angle > 90 && angle < 270){
            Bx = Math.floor(center.x/map.rectSize) * map.rectSize - 1;
            Xb = -map.rectSize;
        }else {
            Bx = Math.floor(center.x/map.rectSize) * (map.rectSize) + map.rectSize;
            Xb = map.rectSize;
        }
        Yb = -Xb*tan;
        By = center.y + (center.x-Bx) * tan;

        int repetitionVertical = 0;
        int verticalHitType = 0;
        Rectangle verticalEnemyRectangle = null;
        EnemyStorage verticalEnemy = null;

        while (repetitionVertical < map.mapSize/map.rectSize){
            for (Enemy enemy : Enemy.enemies) {
                if (MathUtils.isColliding(new Rectangle(Bx, By, 1, 1), enemy.boundingBox)){
                    verticalEnemyRectangle = enemy.boundingBox;
                    verticalEnemy = new EnemyStorage(enemy.location, enemy.dimension);
                    verticalEnemy.rayCount = rayCount;
                }
            }
            for (Map.Entry<Rectangle, Integer> entry : map.rectanglesData.entrySet()) {
                if (MathUtils.isColliding(new Rectangle(Bx, By, 1, 1), entry.getKey())){
                    repetitionVertical = map.mapSize/map.rectSize;
                    verticalHitType = entry.getValue();
                    break;
                }
            }
            if (repetitionVertical < map.mapSize/map.rectSize){
                Bx = Bx + Xb;
                By = By + Yb;
                repetitionVertical++;
            }else break;
        }
        Vector2 verticalHit = new Vector2(Bx, By);

        //------< Calculate distance and choose hit >------
        GraphicsContext gc = Shoot3D.getInstance().getGc();
        double horizontalDistance = Math.pow(center.x-horizontalHit.x,2) + Math.pow(center.y-horizontalHit.y,2);
        double verticalDistance = Math.pow(center.x-verticalHit.x,2) + Math.pow(center.y-verticalHit.y,2);

        if (horizontalDistance < verticalDistance){
            horizontalDistance = (center.y-horizontalHit.y)/Math.sin(Math.toRadians(angle));
            horizontalDistance = horizontalDistance * Math.cos(Math.toRadians(rayAngle));

            if (horizontalEnemy != null){
                if (!seenEnemies.containsKey(horizontalEnemyRectangle)){
                    Vector2 rectangleCenter = MathUtils.getCenterOfRectangle(horizontalEnemyRectangle);


                    double distance = fastSqrt(Math.pow(center.x-rectangleCenter.x,2) + Math.pow(center.y-rectangleCenter.y,2));
                    distance = distance * Math.cos(Math.toRadians(rayAngle));

                    horizontalEnemy.distance = distance;

                    seenEnemies.put(horizontalEnemyRectangle, horizontalEnemy);
                }
            }

            return new Object[] {horizontalHit, horizontalDistance, horizontalHitType, false};
        }else {
            verticalDistance = (center.y-verticalHit.y)/Math.sin(Math.toRadians(angle));
            verticalDistance = verticalDistance * Math.cos(Math.toRadians(rayAngle));

            if (verticalEnemy != null){
                if (!seenEnemies.containsKey(verticalEnemyRectangle)){
                    Vector2 rectangleCenter = MathUtils.getCenterOfRectangle(verticalEnemyRectangle);

                    double distance = fastSqrt(Math.pow(center.x-rectangleCenter.x,2) + Math.pow(center.y-rectangleCenter.y,2));
                    distance = distance * Math.cos(Math.toRadians(rayAngle));

                    verticalEnemy.distance = distance;

                    seenEnemies.put(verticalEnemyRectangle, verticalEnemy);
                }
            }

            return new Object[] {verticalHit,verticalDistance, verticalHitType, true};
        }
    }

    public void shoot(Actor player){
        Vector2 startLocation = MathUtils.getCenterOfPawn(player);
        Vector2 direction = new Vector2(Math.cos(Math.toRadians(player.getRotation())) * 20, Math.sin(Math.toRadians(player.getRotation())) * 20).normalize();

        Vector2 targetVector = startLocation;
        double reach = 1000;
        double distance = 5;

        for (int i = 0; i < reach/distance; i++) {
            for (Enemy enemy : Enemy.enemies) {
                if (MathUtils.isColliding(new Rectangle(targetVector.x, targetVector.y, 1, 1), enemy.boundingBox)){
                    enemy.destroy();
                    return;
                }
            }

            for (Map.Entry<Rectangle, Integer> entry : map.rectanglesData.entrySet()) {
                if (MathUtils.isColliding(new Rectangle(targetVector.x, targetVector.y, 1, 1), entry.getKey())){
                    return;
                }
            }

            targetVector = targetVector.add(direction.multiply(distance));
        }
    }

    public double fixDegrees(double angle){
        return 360-angle;
    }

    public double fastSqrt(double value){
        return Double.longBitsToDouble( ( ( Double.doubleToLongBits( value )-(1l<<52) )>>1 ) + ( 1l<<61 ) );
    }

    public static Raycaster get(){
        if (instance == null)
            instance = new Raycaster();
        return instance;
    }
}
