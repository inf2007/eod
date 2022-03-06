package com.singaporetech.eod.components.ai;

import com.badlogic.gdx.math.Vector2;
import com.singaporetech.eod.GameObject;
import com.singaporetech.eod.SETTINGS;
import com.singaporetech.eod.components.Component;
import com.singaporetech.eod.components.Movement;
import com.singaporetech.eod.components.Transform;

/**
 * Created by mrchek on 22/1/17.
 */

public abstract class Steering extends Component {
    private static final String TAG ="Steering:C";

    protected Transform transform;
    protected Movement movement;

    protected Vector2 destPos;
    protected Vector2 dir = new Vector2();
    protected float dist;

    // steering params
    protected float stopRadius = SETTINGS.STOP_RADIUS;

    public Steering(String name) {
        super(name);
    }

    @Override
    public void init(GameObject owner) {
        super.init(owner);

        // setup links
        transform = (Transform) owner.getComponent("Transform");
        movement = (Movement) owner.getComponent("Movement");

        // init vectors
        destPos = new Vector2(transform.getX(), transform.getY());
    }

    @Override
    public void update(float dt) {
        super.update(dt);
    }

    public void setDestPos(float x, float y) {
        // todo: steering
        destPos.set(x, y);

        // calc dir and dist
        updateDirAndDist();
    }

    public void setDestPos(Vector2 v) {
        setDestPos(v.x, v.y);
    }

    public Vector2 getDestPos() {
        return destPos;
    }

    public boolean reachedDestPos() {
        //Gdx.app.log(TAG, "dist=" + dist + " pos=" + transform.pos + " destPos=" + destPos);

        if (dist < stopRadius) {
            return true;
        }
        else {
            return false;
        }
    }

    protected void updateDirAndDist() {
        dir.set(destPos).sub(transform.getPos());
        dist = dir.len();
        dir.nor();
    }

    public abstract Vector2 getForce();
    public abstract Vector2 getBaseForce();
}
