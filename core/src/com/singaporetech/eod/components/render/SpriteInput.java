package com.singaporetech.eod.components.render;

import com.singaporetech.eod.SETTINGS;

/**
 * Created by mrboliao on 3/2/17.
 */

public class SpriteInput extends Sprite implements RenderableDebug {
    public SpriteInput(String spritePath) {
        super("SpriteInput", spritePath, SETTINGS.X_SIZE, SETTINGS.X_SIZE);
    }

    @Override
    public void update(float dt) {
        // NO FRAME UPDATES, changes only by request
    }
}
