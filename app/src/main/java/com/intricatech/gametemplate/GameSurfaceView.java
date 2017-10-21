package com.intricatech.gametemplate;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class GameSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback,
                   Choreographer.FrameCallback,
                   Runnable,
                   SurfaceInfoDirector {

    private String TAG;
    private static final boolean DEBUG = false;

    private ArrayList<SurfaceInfoObserver> observers;

    private Physics physics;
    private DrawDataPacket drawDataPacket;
    private Resources resources;
    private SurfaceHolder holder;
    private Choreographer choreographer;

    private SurfaceInfo surfaceInfo;

    private Thread drawThread;
    private Paint textPaint;
    private Paint ballPaint;
    private boolean continueRendering;
    private long lastFrameStartTime;
    private int missedFrames;

    private boolean triggerDraw;

    /**
     * An enum delineating the possible states for the drawThread. Accessor is provided so that
     * Physics.physicsThread can keep track of the drawThread's state.
     * <p>
     * Until the surface is valid, the drawThread waits at WAITING_FOR_VALID_SURFACE.
     * <p>
     * With a valid surface, the drawThread waits for the callback doFrame() from Choreographer,
     * in WAITING_FOR_CHOREOGRAPHER.
     * <p>
     * When doFrame() is called, the drawThread grabs the previous frames data from the physicsThread
     * and stores it in the list of ShardDrawingPackets, in GRABBING_DATA.
     * <p>
     * When drawing is complete, the drawThread switches back to waiting for the next doFrame() call
     * in WAITING_FOR_CHOREOGRAPHER.
     */
    enum DrawThreadStatus {
        WAITING_FOR_VALID_SURFACE,
        WAITING_FOR_CHOREOGRAPHER,
        GRABBING_DATA,
        GRAB_COMPLETE_DRAWING_FRAME
    }

    private volatile DrawThreadStatus drawThreadStatus;

    public GameSurfaceView(Context context) {
        super(context);
    }

    public GameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(Context context, TouchDirector touchDirector) {
        TAG = getClass().getSimpleName();

        observers = new ArrayList<>();
        resources = getResources();
        holder = getHolder();
        holder.addCallback(this);
        choreographer = Choreographer.getInstance();
        physics = new Physics(this, touchDirector);
        drawDataPacket = new DrawDataPacket();

        continueRendering = false;

        textPaint = new Paint();
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(70);
        ballPaint = new Paint();
        ballPaint.setStyle(Paint.Style.FILL);
        ballPaint.setAntiAlias(true);
        ballPaint.setColor(Color.WHITE);

        drawThread = new Thread(this);
        continueRendering = false;
        drawThreadStatus = DrawThreadStatus.WAITING_FOR_VALID_SURFACE;
    }

    @Override
    public void run() {
        // Outer while loop - continueRendering set by Activity.onResume() and Activity.onPause().
        outerloop:
        while (continueRendering) {

            // If the surface isn't available yet, skip the frame.
            if (!holder.getSurface().isValid()) {
                continue;
            } else drawThreadStatus = DrawThreadStatus.WAITING_FOR_CHOREOGRAPHER;

            // Wait for the Choreographer to initiate the frame via callback to doFrame().
            while (drawThreadStatus == DrawThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                if (!continueRendering) {
                    break outerloop;
                }
            }

            // Grab the drawing data from bitmapBoss.shardList. If physics is not ready for the grab,
            // redraw the previous frame using the old data.
            if (physics.getPhysicsThreadStatus() == Physics.PhysicsThreadStatus.WAITING_FOR_DATA_GRAB_COMPLETE) {
                grabDrawingPackets();
            }
            drawThreadStatus = DrawThreadStatus.GRAB_COMPLETE_DRAWING_FRAME;


            // MAIN DRAWING ROUTINE STARTS HERE.
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.BLACK);

            canvas.drawCircle(
                    drawDataPacket.ballxPos,
                    drawDataPacket.ballyPos,
                    drawDataPacket.ballRadius,
                    ballPaint);

            if (DEBUG) {
                canvas.drawText(
                        "drawThread misses : " +
                                +missedFrames,
                        100, 100, textPaint
                );
            }
            if (DEBUG) {
                canvas.drawText(
                        "physics Thread misses : " +
                                +physics.missedFrames,
                        100, 160, textPaint
                );
            }

            holder.unlockCanvasAndPost(canvas);

            if (DEBUG) {
                float time = (float) (System.nanoTime() - lastFrameStartTime) / 1000000;
                Log.d(TAG, "Time for drawing == " + String.format("%.2f,", time));
            }
        }
    }

    private void grabDrawingPackets() {
        drawDataPacket.ballxPos = physics.getBall().getxPos();
        drawDataPacket.ballyPos = physics.getBall().getyPos();
        drawDataPacket.ballRadius = physics.getBall().getRadius();
    }

    public void onPause() {
        choreographer.removeFrameCallback(this);
        continueRendering = false;

        while (true) {
            try {
                triggerDraw = true;  // Necessary in case thread is waiting.
                if (drawThread != null) {
                    drawThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            break;
        }
    }

    public void onResume() {
        choreographer.postFrameCallback(this);
    }

    private void startThreads() {
        physics.startPhysicsThread();

        continueRendering = true;
        drawThread.start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated() invoked");
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        Log.d(TAG, "surfaceChanged() invoked");
        surfaceInfo = new SurfaceInfo(width, height);
        publishSurfaceInfo();

        startThreads();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed() invoked");
    }

    @Override
    public void doFrame(long l) {
        lastFrameStartTime = l;
        choreographer.postFrameCallback(this);
        if (drawThreadStatus == DrawThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
            drawThreadStatus = DrawThreadStatus.GRABBING_DATA;
        } else {
            missedFrames++;
            Log.d(TAG, "missed frame ..... total == " + missedFrames);
        }
        physics.doFrame(l);
    }

    @Override
    public void register(SurfaceInfoObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregister(SurfaceInfoObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void publishSurfaceInfo() {
        for (SurfaceInfoObserver observer : observers) {
            observer.onSurfaceChanged(surfaceInfo);
        }
    }

    /**
     * A private inner class representing the data on each BitmapShard calculated by the physics
     * object during the previous frame.
     */
    private class DrawDataPacket {

        float ballxPos, ballyPos, ballRadius;

        DrawDataPacket() {}
    }
    public DrawThreadStatus getDrawThreadStatus() {
        return drawThreadStatus;
    }
}
