package com.singaporetech.eod;

import com.singaporetech.eod.components.Component;
import com.singaporetech.eod.components.render.Renderable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chek on 17/1/17.
 * NOTE THAT THIS IS LEGACY CODE THAT HAS NO PROPER COMMENTS
 */

public class GameObject {
    protected String name;
    protected boolean isDestroyed = false;
    protected List<com.singaporetech.eod.components.Component> components = new ArrayList<com.singaporetech.eod.components.Component>();

    public GameObject (String name) {
        this.name = name;
    }

    /**
     * I know there are very little components in an object, so no need for hash table.
     * @param name  name of component
     * @return the component object, or null if does not exist
     */
    public com.singaporetech.eod.components.Component getComponent(String name) {
        for (com.singaporetech.eod.components.Component c: components) {
            if (c.getName() == name) {
                return c;
            }
        }
        return null;
    }

    public com.singaporetech.eod.components.render.Renderable getRenderable() {
        for (com.singaporetech.eod.components.Component c: components) {
            if (c instanceof com.singaporetech.eod.components.render.Renderable) {
                return (Renderable)c;
            }
        }
        return null;
    }

    /**
     * This can only be called after added all components
     */
    public void init() {
        //todo: throw exception if no components
//        if (components.isEmpty()) {
//            throw new Exception("Components need to be added before calling GameObject::init().");
//        }

        for (com.singaporetech.eod.components.Component c: components) {
            c.init(this);
        }
    }

    public void addComponent(com.singaporetech.eod.components.Component component) {
        components.add(component);
    }

    public void update(float dt) {
        for (com.singaporetech.eod.components.Component c: components) {
            c.update(dt);
        }
    }

    public String getName() {
        return name;
    }

    public void setDestroyed() {
        isDestroyed = true;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void finalize() {
        for (Component c: components) {
            c.finalize();
        }
    }
}
