package com.kii.kiigatekeeper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaCodecList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.*;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiGroup;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiUserCallBack;
import com.kii.cloud.storage.exception.app.AppException;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;

import java.io.IOException;
import java.util.List;


public class ManageActivity extends Activity {

    private KiiUser mKiiUser, mAddedUser;
    private String mAddedUserName;
    private KiiGroup mSecurityGroup, mEmployeeGroup;
    private EmployeeAdapter mEmployeeAdapter;
    private ListView mListview;
    private KiiBucket mKiiBucket;
    private List<KiiObject> mObjLists;
    private List<KiiUser> mMembers = null;
    private int mListSize = 0;
    private ProgressDialog mProgressDialog = null;
    private KiiObject mKiiObject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        if (!Utils.isCurrentLogined()) {
            Toast.makeText(this, R.string.need_to_login_first, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mProgressDialog = new ProgressDialog(ManageActivity.this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Fetching employee list");
        mProgressDialog.show();

        mListview = (ListView)findViewById(R.id.list);
        mEmployeeAdapter = new EmployeeAdapter(ManageActivity.this);
        mListview.setAdapter(mEmployeeAdapter);

        mKiiUser = KiiUser.getCurrentUser();
        android.util.Log.i("the access token", mKiiUser.getAccessToken());

        mKiiUser.ownerOfGroups(new KiiUserCallBack() {
            @Override
            public void onOwnerOfGroupsCompleted(int token, KiiUser user, List<KiiGroup> ownedGroups, Exception exception) {
                super.onOwnerOfGroupsCompleted(token, user, ownedGroups, exception);
                mSecurityGroup = ownedGroups.get(0);
                mEmployeeGroup = ownedGroups.get(1);
                new EmployeeListTask().execute();

            }
        });
    }

    class EmployeeListTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            KiiQuery all_query = new KiiQuery();

            try {
                KiiQueryResult<KiiObject> result = mSecurityGroup.bucket("secure_profile").query(all_query);
                // Alternatively, you can also do:
                // KiiQueryResult<KiiObject> result = Kii.bucket("myBuckets")
                //         .query(null);

                mObjLists = result.getResult();
            }catch (Exception e){
                Toast.makeText(ManageActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.dismiss();
            mListSize = mObjLists.size();
            mEmployeeAdapter.notifyDataSetChanged();

        }
    }

    class ViewHolder{
        public TextView name;
        public TextView group;
        public CheckBox authority;
    }

    class EmployeeAdapter extends BaseAdapter{

        private Context context;
        private KiiObject kiiObject = null;

        public EmployeeAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getCount() {
            return mListSize;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;

            if(convertView == null){
                holder = new ViewHolder();
                //根据自定义的Item布局加载布局
                convertView = LayoutInflater.from(context).inflate(R.layout.item_employee, null);
                holder.name = (TextView)convertView.findViewById(R.id.name);
                holder.group = (TextView)convertView.findViewById(R.id.group);
                holder.authority = (CheckBox)convertView.findViewById(R.id.authority);

                convertView.setTag(holder);
            }else{
                holder = (ViewHolder)convertView.getTag();
            }

            kiiObject = mObjLists.get(i);
            holder.name.setText(mObjLists.get(i).getString("username"));
            holder.group.setText("No group");
            holder.group.setTextColor(getResources().getColor(R.color.gray));
            try {
                if (mObjLists.get(i).getString("6fya1mdi88fehboc1kvw2nnv7").equals("true")) {
                    holder.group.setText("Security");
                    holder.group.setTextColor(getResources().getColor(R.color.blue));

                }
            }catch (Exception e){
                e.printStackTrace();
            }

            try{
                if (mObjLists.get(i).getString("obpm16lvb00qz950scki51km3").equals("true")) {
                    holder.group.setText("Employee");
                    holder.group.setTextColor(getResources().getColor(R.color.green));

                }
            }catch (Exception e){
                e.printStackTrace();
            }
            if(kiiObject.getString("authority").equals("true")){
                holder.authority.setChecked(true);
            }else{
                holder.authority.setChecked(false);
            }

            holder.authority.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mKiiObject = mObjLists.get(i);
                    new AuthorityChangeTask().execute(String.valueOf(b));
                }
            });
            return convertView;
        }
    }

    class AuthorityChangeTask extends AsyncTask<String, Void, Void>{

        ProgressDialog progressDialog = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(ManageActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Changing authority");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... strings) {
            try{
                mKiiObject.refresh();

                mKiiObject.set("authority", strings[0]);

                mKiiObject.save();
            } catch (AppException e) {
                // handle error
            } catch (IOException e) {
                // handle error
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
        }
    }

    class AddToSecurityTask extends AsyncTask<Void, Void, Void>{

        ProgressDialog progressDialog = null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(ManageActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Add to security group");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mAddedUser = null;
            try {
                mAddedUser = KiiUser.findUserByUserName(mAddedUserName);
            }catch (Exception e){
                e.printStackTrace();
            }
            if(mAddedUser == null){
                return null;
            }

            mSecurityGroup.addUser(mAddedUser);
            try{
                mSecurityGroup.save();
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            if (mAddedUser == null){
                Toast.makeText(ManageActivity.this, R.string.user_not_found, Toast.LENGTH_LONG).show();
            }
            new EmployeeListTask().execute();

        }
    }

    class AddToEmployeeTask extends AsyncTask<Void, Void, Void>{

        ProgressDialog progressDialog = null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(ManageActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Add to employee group");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mAddedUser = null;
            try {
                mAddedUser = KiiUser.findUserByUserName(mAddedUserName);
            }catch (Exception e){
                e.printStackTrace();
            }
            if(mAddedUser == null){
                return null;
            }

            mEmployeeGroup.addUser(mAddedUser);
            try{
                mEmployeeGroup.save();
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            if(mAddedUser == null){
                Toast.makeText(ManageActivity.this, R.string.user_not_found, Toast.LENGTH_LONG).show();
            }
            new EmployeeListTask().execute();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.manage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final EditText inputUserName = new EditText(this);

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.security) {
            inputUserName.setHint("Input user name ");
            inputUserName.setWidth(100);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add to Security").setView(inputUserName).setView(inputUserName).setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mAddedUserName = inputUserName.getText().toString();
                    new AddToSecurityTask().execute();

                }
            }).setNegativeButton("Cancel", null);
            builder.show();
            return true;
        }else if(id == R.id.employee){
            inputUserName.setHint("Input user name ");
            inputUserName.setWidth(100);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add to Employee").setView(inputUserName).setView(inputUserName).setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mAddedUserName = inputUserName.getText().toString();
                    new AddToEmployeeTask().execute();

                }
            }).setNegativeButton("Cancel", null);
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
