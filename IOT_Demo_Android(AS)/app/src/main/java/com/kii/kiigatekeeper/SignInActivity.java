package com.kii.kiigatekeeper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kii.cloud.storage.KiiUser;

public class SignInActivity extends Activity {

    private EditText mUsernameEdit;
    private EditText mPasswordEdit;
    private Button mLoginBtn, mRegisterBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        mUsernameEdit = (EditText)findViewById(R.id.username);
        mPasswordEdit = (EditText)findViewById(R.id.password);

        mLoginBtn = (Button)findViewById(R.id.login);

        mLoginBtn.setOnClickListener(onClickListener);

    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String[] text = new String[3];
            text[0] = mUsernameEdit.getText().toString();
            text[1] = mPasswordEdit.getText().toString();
            if (!KiiUser.isValidUserName(text[0])) {
                Toast.makeText(SignInActivity.this, "Username is not valid", Toast.LENGTH_LONG).show();
                return;
            }
            if (!KiiUser.isValidPassword(text[1])) {
                Toast.makeText(SignInActivity.this, "Password is not valid", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            switch (view.getId()) {
                case R.id.login:
                    text[2] = "login";
                    break;
                case R.id.register:
                    text[2] = "register";
                    break;
            }
            new LoginOrRegTask().execute(text[0], text[1], text[2]);
        }
    };

    class LoginOrRegTask extends AsyncTask<String, Void, Void> {

        ProgressDialog progressDialog = null;
        String token = null;
        KiiUser user = null;

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressDialog.dismiss();

            Activity activity = SignInActivity.this;

            if (TextUtils.isEmpty(token)) {
                Toast.makeText(activity,
                        "Reg or login failed, please try again later",
                        Toast.LENGTH_LONG).show();
            } else {
                Utils.saveToken(activity.getApplicationContext(), token);
                Toast.makeText(activity, "Login successful",
                        Toast.LENGTH_LONG).show();

                Intent intent = new Intent(SignInActivity.this, UserActivity.class);
                startActivity(intent);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(SignInActivity.this);
            progressDialog.setMessage("In progressing");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            String username = params[0];
            String password = params[1];
            String type = params[2];
            try {
                if (type.equals("register")) {
                    KiiUser.Builder builder = KiiUser.builderWithName(username);
                    user = builder.build();
                    user.register(password);
                } else {
                    user = KiiUser.logIn(username, password);

                }
                token = user.getAccessToken();
            } catch (Exception e) {
                token = null;
            }
            return null;
        }
    }

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
