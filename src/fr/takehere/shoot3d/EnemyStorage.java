package fr.takehere.shoot3d;

import fr.takehere.ethereal.utils.Dimension;
import fr.takehere.ethereal.utils.Rectangle;
import fr.takehere.ethereal.utils.Vector2;

public class EnemyStorage {

    public double distance;
    public int rayCount;
    public Vector2 location;
    public Dimension dimension;
    public Rectangle boundingBox;

    public EnemyStorage(Vector2 location, Dimension dimension) {
        this.location = location;
        this.dimension = dimension;

        boundingBox = new Rectangle(location.x, location.y, dimension.width, dimension.height);
    }
}
