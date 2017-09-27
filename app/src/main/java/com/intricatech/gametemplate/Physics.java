package com.intricatech.gametemplate;

import android.graphics.Canvas;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class Physics {

    private Ball ball;

    Physics() {
        ball = new Ball();
    }

    public void updateObjects() {
        ball.update();
    }

    public void drawObjects(Canvas canvas) {
        ball.draw(canvas);
    }

    public void onSurfaceChanged(PlayAreaInfo playAreaInfo) {
        ball.onSurfaceChanged(playAreaInfo);
    }
}
