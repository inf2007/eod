package com.singaporetech.eod;

import com.badlogic.gdx.math.Vector2;
import com.singaporetech.eod.components.collision.Collidable;
import com.singaporetech.eod.components.collision.Collider;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by mrboliao on 24/1/17.
 * NOTE THAT THIS IS LEGACY CODE THAT HAS NO PROPER COMMENTS
 */
public class CollisionEngine implements Engine {
    private List<com.singaporetech.eod.components.collision.Collidable> collidables = new LinkedList<com.singaporetech.eod.components.collision.Collidable>();

    private static CollisionEngine instance = new CollisionEngine();
    public static CollisionEngine i(){
        return instance;
    }
    private CollisionEngine() {}

    public void tick() {
        // do collision responses to prevent overlapping objects
        // todo: quadtrees when things grow big
        /*
        for (Collidable c1: collidables) {
            for (Collidable c2: collidables) {
                if (c1 != c2 && c2.isStatic()) {
                    c1.checkCollisionAndRespond(c2);
                }
            }
        }
        */
    }

    @Override
    public void init() {

    }

    /**
     * Check if collider has collided with any other collidables
     * @param collider
     * @return
     */
    public Vector2 getCollisionNorm(com.singaporetech.eod.components.collision.Collider collider) {
        for (com.singaporetech.eod.components.collision.Collidable c: collidables) {
            if (!collider.equals((com.singaporetech.eod.components.collision.Collider)c) && c.isCollidable()) {
                Vector2 collisionNorm = collider.getCollisionNorm(c);
                if (collisionNorm != null) {
                    return collisionNorm;
                }
            }
        }
        return null;
    }

    /**
     * Get a new target offset from the obstacle, for steering purposes
     * @param collider
     * @return
     */
    public Vector2 getCollisionAvoidTarget(com.singaporetech.eod.components.collision.Collider collider) {
        for (com.singaporetech.eod.components.collision.Collidable c: collidables) {
            if (!collider.equals((Collider)c) && c.isCollidable()) {
                Vector2 target = collider.getCollisionAvoidTarget(c);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    public GameObject getObjectCollidedWithPos(Vector2 pos){
        for (com.singaporetech.eod.components.collision.Collidable c:collidables) {
            if (c.collidedWithPos(pos)) {
                return c.getOwner();
            }
        }
        return null;
    }

    public boolean isFreeOfCollisions(Vector2 pos) {
        return (getObjectCollidedWithPos(pos) == null);
    }

    public void addCollidable(com.singaporetech.eod.components.collision.Collidable c) {
        collidables.add(c);
    }

    public void removeCollidable(Collidable c) {
        collidables.remove(c);
    }

    public void clearCollidables() {
        collidables.clear();
    }

    @Override
    public void finalize() {

    }
}
