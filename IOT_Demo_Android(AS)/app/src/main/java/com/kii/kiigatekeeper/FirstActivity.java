package com.kii.kiigatekeeper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class FirstActivity extends Activity {

    private Button mScanBtn, mAppBtn, mRegBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        mScanBtn = (Button)findViewById(R.id.scan);
        mAppBtn = (Button)findViewById(R.id.app);
        mRegBtn = (Button)findViewById(R.id.register);

        mScanBtn.setOnClickListener(mOnClickListener);
        mAppBtn.setOnClickListener(mOnClickListener);
        mRegBtn.setOnClickListener(mOnClickListener);

    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent;
            switch (view.getId()) {
                case R.id.scan:
                    intent = new Intent(FirstActivity.this, DoorControlActivity.class);
                    startActivity(intent);
                    break;
                case R.id.app:
                    intent = new Intent(FirstActivity.this, LogInActivity.class);
                    startActivity(intent);
                    break;
                case R.id.register:
                    intent = new Intent(FirstActivity.this, RegisterActivity.class);
                    startActivity(intent);

            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.first, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
