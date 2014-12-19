package com.kii.kiigatekeeper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class GroupChoiceActivity extends Activity {

    private String mUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_choice);

        Intent intent = this.getIntent();
        mUserType = intent.getStringExtra("UserType");

    }

    public void onManageSecurityClicked(View view){
        StartActivity("security");
    }

    public void onManageEmployeeClicked(View view){
        StartActivity("employee");
    }

    public void onManageAnonymousClicked(View view){
        StartActivity("anonymous");
    }

    private void StartActivity(String group){
        Intent intent = new Intent(GroupChoiceActivity.this, ManageActivity.class);
        intent.putExtra("UserType", mUserType);
        intent.putExtra("group", group);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group_choice, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
