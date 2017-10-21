package com.intricatech.gametemplate;

import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class Physics implements Runnable, TouchObserver{

    private final String TAG;
    private static boolean DEBUG = false;
    private Ball ball;

    /**
     * Enum delineating possible states for the Thread implemented here. Allows the drawThread in
     * GameSurfaceView to modify its behaviour in response to changes in this thread's behaviour.
     *
     * When a physics update is complete, the physicsThread switches to WAITING_FOR_CHOREOGRAPHER,
     * until the Choreographer callback in GameSurfaceView calls through.
     *
     * The drawThread then (deep copies the data it needs before the next update starts, during
     * which the physicsThread's state is WAITING_FOR_DATA_GRAB_COMPLETE.
     *
     * Once the drawThread has grabbed the data, the physicsThread
     * can then switch to calculating the next frame, when the process repeats.
     *
     * This structure removes the need for multiple flags, and simplifies the coordination between
     * the 2 threads.
     */
    enum PhysicsThreadStatus {
        WAITING_FOR_CHOREOGRAPHER,
        WAITING_FOR_DATA_GRAB_COMPLETE,
        CALCULATING_PHYSICS
    }
    private volatile PhysicsThreadStatus physicsThreadStatus;

    private GameSurfaceView gameSurfaceView;
    private Thread physicsThread;

    /**
     * Main flag governing whether the physicsThread is running. Set ultimately by the
     * Activity lifecycle callbacks.
     */
    private boolean continueRunning;
    private long timeOfLastCallback;
    int missedFrames;

    Physics(
            GameSurfaceView gameSurfaceView,
            TouchDirector touchDirector) {

        TAG = getClass().getSimpleName();

        touchDirector.register(this);
        this.gameSurfaceView = gameSurfaceView;
        ball = new Ball(gameSurfaceView);
    }

    @Override
    public void run() {

        outerloop:
        while(continueRunning) {

            if (DEBUG) {
                Log.d(TAG, "physicsThread is running");
            }

            // Wait for doFrame to signal that frame should start.
            while (physicsThreadStatus == PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
                if (DEBUG) {
                    Log.d(TAG, "waiting for choreographer == true");
                }
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                if (!continueRunning) {
                    break outerloop;
                }
            }

            // Wait for gameSurfaceView to signal that it has grabbed the last frame's draw data.
            while (physicsThreadStatus == PhysicsThreadStatus.WAITING_FOR_DATA_GRAB_COMPLETE) {
                if (DEBUG) {
                    Log.d(TAG, "waiting for drawDataGrabComplete == true");
                }
                try {
                    Thread.sleep(0, 1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                if (!continueRunning) {
                    break outerloop;
                }
                if (gameSurfaceView.getDrawThreadStatus() == GameSurfaceView.DrawThreadStatus.GRAB_COMPLETE_DRAWING_FRAME) {
                    physicsThreadStatus = PhysicsThreadStatus.CALCULATING_PHYSICS;
                }
            }

            physicsThreadStatus = PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER;

            if (DEBUG) {
                float t = System.nanoTime() - timeOfLastCallback;
                Log.d(TAG, "Time for update == " + String.format("%.2f,", t));
            }
        }
    }

    /**
     * Choreographer callback calls through to this method from GameSurfaceView, to signal that
     * it's time to start processing a new frame.
     * @param callbackTime
     */
    public void doFrame(long callbackTime) {

        timeOfLastCallback = callbackTime;
        if (physicsThreadStatus == PhysicsThreadStatus.WAITING_FOR_CHOREOGRAPHER) {
            physicsThreadStatus = PhysicsThreadStatus.WAITING_FOR_DATA_GRAB_COMPLETE;
        } else {
            missedFrames++;
        }
    }

    public void startPhysicsThread() {
        continueRunning = true;
        physicsThread.start();
    }

    @Override
    public void updateTouch(MotionEvent me) {

    }

    public PhysicsThreadStatus getPhysicsThreadStatus() {
        return physicsThreadStatus;
    }

    public void setContinueRunning(boolean continueRunning) {
        this.continueRunning = continueRunning;
    }
}
