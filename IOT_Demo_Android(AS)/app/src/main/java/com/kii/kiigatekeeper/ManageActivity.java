package com.kii.kiigatekeeper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.*;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
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
import com.kii.cloud.storage.exception.GroupOperationException;
import com.kii.cloud.storage.exception.app.AppException;
import com.kii.cloud.storage.query.KiiClause;
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
    private String mUserType = null;
    private String mGroupChoice = null;
    private boolean isFirst = true;
    private Button deleteBtn;
    private String mUri;
    private KiiGroup mFinalGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        if (!Utils.isCurrentLogined()) {
            Toast.makeText(this, R.string.need_to_login_first, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = this.getIntent();
        mUserType = intent.getStringExtra("UserType");
        mGroupChoice = intent.getStringExtra("group");

        mProgressDialog = new ProgressDialog(ManageActivity.this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Fetching employee list");
        mProgressDialog.show();

        mListview = (ListView)findViewById(R.id.list);
        mEmployeeAdapter = new EmployeeAdapter(ManageActivity.this);
        mListview.setAdapter(mEmployeeAdapter);

        mKiiUser = KiiUser.getCurrentUser();
        Log.i("the access token", mKiiUser.getAccessToken());

        mKiiUser.memberOfGroups(new KiiUserCallBack() {
            @Override
            public void onMemberOfGroupsCompleted(int token, KiiUser user, List<KiiGroup> groupList, Exception exception) {
                for( KiiGroup group:groupList){
                    if(group.getGroupName().equals("security") && isFirst == true){
                        mSecurityGroup = group;

                        isFirst = false;
                    }else if(group.getGroupName().equals("employee")){
                        mEmployeeGroup = group;
                    }
                    mFinalGroup = group;

                }
                new EmployeeListTask().execute();

            }
        });
    }

    public void onDeleteGroupClicked(View view){
        new DeleteGroupTask().execute();
    }

    class DeleteGroupTask extends AsyncTask<String, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {

            try {
                // Delete the group.
                mFinalGroup.delete();
            } catch (GroupOperationException e) {
                // Deleting the group failed for some reasons.
                // Please check GroupOperationException to see what went wrong...
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    class EmployeeListTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            KiiQuery query;
            if( mGroupChoice.equals("security")){
                query = new KiiQuery(KiiClause.and(
                        KiiClause.equals("security", true)));
            }else if(mGroupChoice.equals("employee")){
                query = new KiiQuery(KiiClause.and(
                        KiiClause.equals("employee", true)));
            }else{
                query = new KiiQuery(KiiClause.and(
                        KiiClause.notEquals("security", true),
                        KiiClause.notEquals("employee", true)));
            }

            try {
                KiiQueryResult<KiiObject> result = mSecurityGroup.bucket("secure_profile").query(query);
                // Alternatively, you can also do:
                // KiiQueryResult<KiiObject> result = Kii.bucket("myBuckets")
                //         .query(null);

                mObjLists = result.getResult();
            }catch (Exception e){
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
        private String groupName;

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

            if (convertView == null) {
                holder = new ViewHolder();
                //根据自定义的Item布局加载布局
                convertView = LayoutInflater.from(context).inflate(R.layout.item_employee, null);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.group = (TextView) convertView.findViewById(R.id.group);
                holder.authority = (CheckBox) convertView.findViewById(R.id.authority);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            kiiObject = mObjLists.get(i);
            holder.name.setText(mObjLists.get(i).getString("username"));
            holder.group.setText("No group");
            holder.group.setTextColor(getResources().getColor(R.color.gray));
            try {
                if (mObjLists.get(i).getBoolean(Constants.SECURITY_GROUP_NAME) == true
                        && mGroupChoice.equals("security")) {
                    holder.group.setText("Security");
                    holder.group.setTextColor(getResources().getColor(R.color.blue));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (mObjLists.get(i).getBoolean(Constants.EMPLOYEE_GROUP_NAME) == true
                        && mGroupChoice.equals("employee")) {
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

            if(mUserType.equals("admin")) {
                holder.name.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        new RemoveMemberTask().execute(mObjLists.get(i).getString("username"));

                        return false;
                    }
                });
            }

            return convertView;
        }
    }

    class RemoveMemberTask extends AsyncTask< String, Void, Void>{

        ProgressDialog progressDialog = null;
        String errorMsg = null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(ManageActivity.this);
            progressDialog.setMessage("Removing member");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                KiiUser kiiUser = KiiUser.findUserByUserName(params[0]);
                if (mGroupChoice.equals("security")) {
                    mSecurityGroup.removeUser(kiiUser);
                    mSecurityGroup.save();
                } else if (mGroupChoice.equals("employee")) {
                    mEmployeeGroup.removeUser(kiiUser);
                    mEmployeeGroup.save();
                }

            } catch (Exception e) {
                errorMsg = e.getLocalizedMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            if(errorMsg == null){
                new EmployeeListTask().execute();
            }else{
                Toast.makeText(ManageActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
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
        MenuItem securityItem, employeeItem;
        securityItem = menu.findItem(R.id.item_security);
        employeeItem = menu.findItem(R.id.item_employee);
        if(mUserType.equals("security")){
            securityItem.setVisible(false);
            employeeItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final EditText inputUserName = new EditText(this);
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.item_security) {
            inputUserName.setHint(R.string.input_user_name);
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
        }else if(id == R.id.item_employee){
            inputUserName.setHint(R.string.input_user_name);
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
