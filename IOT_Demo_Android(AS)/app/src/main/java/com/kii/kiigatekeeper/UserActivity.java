package com.kii.kiigatekeeper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kii.cloud.storage.KiiGroup;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.exception.app.AppException;

import java.io.IOException;
import java.util.List;


public class UserActivity extends Activity {

    private TextView mUsername;
    private Button mManageBtn;
    private String mUserType;
    private ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        mProgressDialog = new ProgressDialog(UserActivity.this);
        mProgressDialog.setMessage("Checking user");
        mProgressDialog.setCancelable(false);

        mUsername = (TextView)findViewById(R.id.username);
        mManageBtn = (Button)findViewById(R.id.btn_manage);

        if(Utils.isCurrentLogined() == true) {
            mUsername.setText("Username: "+KiiUser.getCurrentUser().getUsername());
        }else{
            mUsername.setText("Username: No user");
        }

        //Check if the user is admin or in the security group, only these two kind of user
        //have the manage function
        new OwnedGroupTask().execute();

    }

    //Check the user's owned group
    class OwnedGroupTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            KiiUser user = KiiUser.getCurrentUser();

            // Get a list of groups in which the current user is a owner.
            try {
                List<KiiGroup> ownerGroups = user.ownerOfGroups();
                for (KiiGroup group : ownerGroups) {
                    // If the user owns any group, means he is the admin
                    if( ownerGroups.size() != 0){
                        mUserType = "admin";
                    }
                }
            } catch (IOException e) {
                // Getting a list failed for some reasons
                // Please check IOExecption to see what went wrong...
            } catch (AppException e) {
                // Getting a list failed for some reasons
                // Please check AppException to see what went wrong...
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(mUserType == "admin") {
                mManageBtn.setVisibility(View.VISIBLE);
                mProgressDialog.dismiss();
            }else{
                new MemberGroupTask().execute();
            }
        }
    }

    class MemberGroupTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Get the current login user
            KiiUser user = KiiUser.getCurrentUser();

            // Get a list of groups in which the current user is a owner.
            try {
                List<KiiGroup> memberGroups = user.memberOfGroups();
                for (KiiGroup group : memberGroups) {
                    // do something with each group
                    if(group.getGroupName().equals("security")){
                        mUserType = "security";
                    }
                }
            } catch (IOException e) {
                // Getting a list failed for some reasons
                // Please check IOExecption to see what went wrong...
            } catch (AppException e) {
                // Getting a list failed for some reasons
                // Please check AppException to see what went wrong...
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(mUserType == "security"){
                mManageBtn.setVisibility(View.VISIBLE);
            }
            mProgressDialog.dismiss();
        }
    }

    public void onManageClicked(View view){
        Intent intent = new Intent(UserActivity.this, GroupChoiceActivity.class);
        intent.putExtra("UserType", mUserType);
        startActivity(intent);
    }

    public void onAccessLogClicked(View view){
        Intent intent = new Intent(UserActivity.this, AccessLogActivity.class);
        startActivity(intent);
    }

    public void onLogoutClicked(View view){
        KiiUser.logOut();
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user, menu);
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
