package com.singaporetech.eod.components.render;

import com.singaporetech.eod.SETTINGS;

/**
 * Created by mrchek on 6/2/17.
 */

public class SpritePlusOne extends Sprite {
    public SpritePlusOne(String spritePath) {
        super("SpritePlusOne", spritePath, SETTINGS.PLUSONE_SIZE, SETTINGS.PLUSONE_SIZE);
    }

    @Override
    public void update(float dt) {
        // NO FRAME UPDATES, changes only by request
    }
}
