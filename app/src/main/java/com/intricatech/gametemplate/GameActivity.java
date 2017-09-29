package com.intricatech.gametemplate;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Bolgbolg on 26/09/2017.
 */

public class GameActivity extends FragmentActivity
                implements View.OnTouchListener {

    private GameSurfaceView gameSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_layout);

        gameSurfaceView = findViewById(R.id.game_surfaceview);
        gameSurfaceView.setOnTouchListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameSurfaceView.onResume();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}
