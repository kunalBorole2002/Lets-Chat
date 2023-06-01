package com.chatproject.LetsChat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class Full_Screen extends AppCompatActivity {
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    ImageView fullscreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen);

        fullscreen = findViewById(R.id.fullimages);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        Intent intent = getIntent();

        String url = intent.getStringExtra("url");
        Picasso.get().load(url).into(fullscreen);
    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));
            fullscreen.setScaleX(mScaleFactor);
            fullscreen.setScaleY(mScaleFactor);
            return true;
        }
    }
}
