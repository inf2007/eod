package com.singaporetech.eod;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.singaporetech.eod.components.render.Renderable;
import com.singaporetech.eod.components.render.RenderableDebug;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by chek on 19/1/17.
 * NOTE THAT THIS IS LEGACY CODE THAT HAS NO PROPER COMMENTS
 */

public class RenderEngine implements Engine{
    private List<com.singaporetech.eod.components.render.Renderable> renderables = new LinkedList<com.singaporetech.eod.components.render.Renderable>();
    private List<com.singaporetech.eod.components.render.RenderableDebug> renderableDebugs = new LinkedList<com.singaporetech.eod.components.render.RenderableDebug>();

    private static RenderEngine instance = new RenderEngine();
    public static RenderEngine i(){
        return instance;
    }
    private RenderEngine() {}

    private SpriteBatch spriteBatch;
    private Camera cam;
    private com.singaporetech.eod.Hud hud;
    private Viewport viewport;

    // debug renderer
    protected ShapeRenderer shapeRenderer;

    @Override
    public void init() {
        // create sprite drawer
        spriteBatch = new SpriteBatch();

        // create camera
        cam = new OrthographicCamera();

        // create viewport
        viewport = new FitViewport(SETTINGS.VIEWPORT_WIDTH, SETTINGS.VIEWPORT_HEIGHT, cam);
        cam.position.set(viewport.getWorldWidth()/2, viewport.getWorldHeight()/2, 0);

        // create heads up display
        hud = new Hud();

        // debug renderer
        initDebugRenderer();
    }

    public void tick() {
        // perform updates
        cam.update();
        hud.update();

        // clear screen
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.08f, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // draw all game objects
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        for (com.singaporetech.eod.components.render.Renderable r: renderables) {
            r.draw();
        }
        spriteBatch.end();

        // draw debug graphics
        shapeRenderer.setProjectionMatrix(cam.combined);
        for (com.singaporetech.eod.components.render.RenderableDebug r: renderableDebugs) {
            r.draw();
        }

        // draw hud
        spriteBatch.setProjectionMatrix(hud.getStageCam().combined);
        hud.draw();
    }

    public void addRenderable(com.singaporetech.eod.components.render.Renderable r) {
        renderables.add(r);
    }
    public void removeRenderable(Renderable r) {
        renderables.remove(r);
    }

    public void addRenderableDebug(com.singaporetech.eod.components.render.RenderableDebug r) {
        renderableDebugs.add(r);
    }
    public void removeRenderableDebug(RenderableDebug r) {
        renderableDebugs.remove(r);
    }

    public void setCam (Camera cam) {
        this.cam = cam;
    }

    public Camera getCam () {
        return cam;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public ShapeRenderer getDebugRenderer() {
        return shapeRenderer;
    }

    public void initDebugRenderer() {
        shapeRenderer = new ShapeRenderer();
    }

    public void shutdownDebugRenderer() {
        shapeRenderer.dispose();
    }

    public void setViewport(int width, int height) {
        viewport.update(width, height);
    }

    public void showEndGameMenu() {
        hud.showEndGameMenu();
    }

    public void hideEndGameMenu() {
        hud.hideEndGameMenu();
    }

    public void clearRenderables() {
        renderables.clear();
        renderableDebugs.clear();
    }

    @Override
    public void finalize() {
        clearRenderables();

        // BUG says already disposed
//        spriteBatch.dispose();
//        shapeRenderer.dispose();
    }
}
