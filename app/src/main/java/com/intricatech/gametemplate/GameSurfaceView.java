package com.intricatech.gametemplate;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class GameSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback,
                   Choreographer.FrameCallback,
                   Runnable {

    private String TAG;
    private static final boolean DEBUG = true;

    private Physics physics;
    private Resources resources;
    private SurfaceHolder holder;
    private Choreographer choreographer;

    private PlayAreaInfo playAreaInfo;

    private Thread drawThread;
    private boolean continueRendering;
    private long lastFrameStartTime;

    private boolean drawableObjectsReady;
    private boolean triggerDraw;

    public GameSurfaceView(Context context) {
        super(context);
        initialize();
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public GameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        TAG = getClass().getSimpleName();
        resources = getResources();
        holder = getHolder();
        holder.addCallback(this);
        choreographer = Choreographer.getInstance();
        physics = new Physics();

        continueRendering = false;
        triggerDraw = false;
    }

    @Override
    public void run() {
        while (continueRendering) {
            if (!holder.getSurface().isValid()) {
                continue;
            }
            while (!triggerDraw) {
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

            // Physics update starts here.
            physics.updateObjects();
            float time = 0;
            if (DEBUG) {
                time = (float) (System.nanoTime() - lastFrameStartTime) / 1000000;
                Log.d(TAG, "Time for frame physics == " + String.format("%.2f,", time));
            }


            // Main drawing routine starts here.
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.BLACK);

            physics.drawObjects(canvas);

            holder.unlockCanvasAndPost(canvas);
            triggerDraw = false;

            if (DEBUG) {
                time = (float) (System.nanoTime() - lastFrameStartTime) / 1000000;
                Log.d(TAG, "Time for drawing == " + String.format("%.2f,", time));
            }
        }
    }

    public void onPause() {
        choreographer.removeFrameCallback(this);
        continueRendering = false;

        while (true) {
            try {
                triggerDraw = true;  // Necessary in case thread is waiting.
                drawThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            break;
        }
    }

    public void onResume() {
        choreographer.postFrameCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        playAreaInfo = new PlayAreaInfo(width, height);
        physics.onSurfaceChanged(playAreaInfo);

        drawThread = new Thread(this);
        continueRendering = true;
        drawThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void doFrame(long l) {
        lastFrameStartTime = l;
        choreographer.postFrameCallback(this);
        triggerDraw = true;
    }
}
