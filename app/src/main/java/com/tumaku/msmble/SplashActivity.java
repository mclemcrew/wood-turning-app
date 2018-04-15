package com.tumaku.msmble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
    }

    public void onButtonClick(View view) {
        Intent splashIntent = new Intent(SplashActivity.this,MainActivity.class);
        startActivity(splashIntent);
    }
}
