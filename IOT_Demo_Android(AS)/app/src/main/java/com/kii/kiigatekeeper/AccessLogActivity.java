package com.kii.kiigatekeeper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.exception.app.AppException;
import com.kii.cloud.storage.query.KiiQuery;
import com.kii.cloud.storage.query.KiiQueryResult;
import com.kii.fb.android.Util;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessLogActivity extends Activity {

    public static KiiUser mKiiUser = null;
    private LogAdapter mLogAdapter = null;
    private ListView mListView = null;
    private List<KiiObject> mObjLists;
    private int mListSize = 0;
    private JSONObject mJsonObject;
    private JSONArray mJsonArray;
    private List<Map<String, Object>> mAccessLogList;
    private int mAccessLogNum = 0;
    private ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_log);

        if (!Utils.isCurrentLogined()) {
            Toast.makeText(this, R.string.need_to_login_first, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mKiiUser = KiiUser.getCurrentUser();

        android.util.Log.i("the access token", mKiiUser.getAccessToken());

        mListView =(ListView)findViewById(R.id.log);
        mLogAdapter = new LogAdapter(AccessLogActivity.this);
        mListView.setAdapter(mLogAdapter);

        //parse JSONObject
        JSONObject jsonObject = new JSONObject();
        JSONObject bucketQuery = new JSONObject();
        JSONObject clause = new JSONObject();

        try {
            clause.put("type", "all");
            bucketQuery.put("clause", clause);
            bucketQuery.put("orderBy", "_created");
            bucketQuery.put("descending", true);
            jsonObject.put("bucketQuery", bucketQuery);
        }catch (JSONException e){
            e.printStackTrace();
        }

        mProgressDialog = new ProgressDialog(AccessLogActivity.this);
        mProgressDialog.setMessage("Fetching access log...");
        mProgressDialog.setCancelable(false);
        FetchAccessLog(jsonObject);
    }

    private void FetchAccessLog(final JSONObject jsonObject){

        RequestParams params = new RequestParams();

        params.setHeader("x-kii-appid", Constants.KII_APP_ID);
        params.setHeader("x-kii-appkey", Constants.KII_APP_KEY);
        params.setHeader("content-type", "application/vnd.kii.QueryRequest+json");
        params.setHeader("Authorization", "Bearer "+ mKiiUser.getAccessToken());

        try {
            params.setBodyEntity(new StringEntity(jsonObject.toString()));
        }catch (UnsupportedEncodingException e){
            Toast.makeText(AccessLogActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

        HttpUtils http = new HttpUtils();
        http.send(HttpRequest.HttpMethod.POST, ServerUrl.url,params, new RequestCallBack<Object>() {
            @Override
            public void onStart() {
                mProgressDialog.show();
            }

            //数据成功后开始绑定
            @Override
            public void onSuccess(ResponseInfo<Object> objectResponseInfo) {
                try {
                    mJsonObject = new JSONObject((String) objectResponseInfo.result);

                    mJsonArray = mJsonObject.getJSONArray("results");
                    mAccessLogNum = mJsonArray.length();

                    getData(mJsonArray);

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(HttpException error, String msg) {
                Toast.makeText(AccessLogActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getData(JSONArray jsonArray){
        mAccessLogList = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        JSONObject jsonObject;

        try{
            for (int i=0; i<jsonArray.length(); i++){
                map = new HashMap<String, Object>();
                jsonObject = jsonArray.getJSONObject(i);
                map.put("name", jsonObject.getString("username"));
                map.put("time", Utils.convertTimestampToString(Long.parseLong(jsonObject.getString("_created"))));
                map.put("authority", jsonObject.getString("authority"));
                mAccessLogList.add(map);
            }

        }catch (Exception e){

        }

        mLogAdapter.notifyDataSetChanged();
        mProgressDialog.dismiss();
    }

    static class ViewHolder{
        public TextView name;
        public TextView time;
        public TextView authority;
    }

    class LogAdapter extends BaseAdapter{

        private Context context;

        public LogAdapter(Context context){
            this.context = context;

        }

        @Override
        public int getCount() {
            return mAccessLogNum;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;

            if(convertView == null){
                holder = new ViewHolder();
                //根据自定义的Item布局加载布局
                convertView = LayoutInflater.from(context).inflate(R.layout.item_log, null);
                holder.name = (TextView)convertView.findViewById(R.id.name);
                holder.time = (TextView)convertView.findViewById(R.id.time);
                holder.authority = (TextView)convertView.findViewById(R.id.authority);

                convertView.setTag(holder);
            }else{
                holder = (ViewHolder)convertView.getTag();
            }

            holder.name.setText(mAccessLogList.get(i).get("name").toString());
            holder.time.setText(mAccessLogList.get(i).get("time").toString());
            holder.authority.setText(mAccessLogList.get(i).get("authority").toString());

            return convertView;
        }
    }

    class FetchDataTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(AccessLogActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Fetching access logs…");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
    try{
        KiiQuery all_query = new KiiQuery();
        KiiQueryResult<KiiObject> result = Kii.bucket("access_log").query(all_query);
        mObjLists = result.getResult();
    }catch (IOException e) {
        // handle error
    } catch (AppException e) {
        // handle error
    }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            mListSize = mObjLists.size();
            mLogAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.access_log, menu);
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
