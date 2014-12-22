package com.kii.kiigatekeeper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiServerCodeEntry;
import com.kii.cloud.storage.KiiServerCodeEntryArgument;
import com.kii.cloud.storage.KiiServerCodeExecResult;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.UserFields;
import com.kii.cloud.storage.exception.app.AppException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;


public class RegisterActivity extends Activity {

    private final static String TAG = DoorControlActivity.class.getSimpleName();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;

    private boolean mScanning;
    private Handler mHandler;
    private Boolean mDistanceCheck = true;
    private String mBeaconMajorMinor = null;
    private ListView mListView = null;
    private EditText mUsernameEdit, mPasswordEdit;
    private KiiUser user;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 600000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mListView = (ListView)findViewById(R.id.list);
        mUsernameEdit = (EditText)findViewById(R.id.username);
        mPasswordEdit = (EditText)findViewById(R.id.password);

        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //开启蓝牙
        mBluetoothAdapter.enable();
    }

    public void onRegisterClicked(View view){
        if(mBeaconMajorMinor == null){
            Toast.makeText(RegisterActivity.this, R.string.no_beacon_detected, Toast.LENGTH_LONG).show();
        }else {
            String[] text = new String[2];
            text[0] = mUsernameEdit.getText().toString();
            text[1] = mPasswordEdit.getText().toString();
            if (!KiiUser.isValidUserName(text[0])) {
                Toast.makeText(RegisterActivity.this, "Username is not valid", Toast.LENGTH_LONG).show();
                return;
            }
            if (!KiiUser.isValidPassword(text[1])) {
                Toast.makeText(RegisterActivity.this, "Password is not valid", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            new RegTask().execute(text[0], text[1]);
        }
    }

    //Register
    class RegTask extends AsyncTask<String, Void, Void> {
        ProgressDialog progressDialog = null;
        String token = null;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(RegisterActivity.this);
            progressDialog.setMessage("Registering");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            KiiUser.Builder builder = KiiUser.builderWithName(username);
            user = builder.build();
            try {
                user.register(password);

                UserFields userFields = new UserFields();
                userFields.set("bluetooth_addr", mBeaconMajorMinor);
                user.update(userFields);

            }catch (Exception e){
                e.printStackTrace();
            }

            token = user.getAccessToken();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();

            Activity activity = RegisterActivity.this;

            if (TextUtils.isEmpty(token)) {
                Toast.makeText(activity,
                        "Reg or login failed, please try again later",
                        Toast.LENGTH_LONG).show();
            } else {
                Utils.saveToken(activity.getApplicationContext(), token);
                Toast.makeText(activity, "Login successful",
                        Toast.LENGTH_LONG).show();

                Intent intent = new Intent(RegisterActivity.this, UserActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
//                }
//            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    final iBeaconClass.iBeacon ibeacon = iBeaconClass.fromScanData(device,rssi,scanRecord);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(ibeacon);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    public class LeDeviceListAdapter extends BaseAdapter {

        // Adapter for holding devices found through scanning.

        private ArrayList<iBeaconClass.iBeacon> mLeDevices;
        private LayoutInflater mInflator;
        private Activity mContext;

        public LeDeviceListAdapter(Activity c) {
            super();
            mContext = c;
            mLeDevices = new ArrayList<iBeaconClass.iBeacon>();
            mInflator = mContext.getLayoutInflater();
        }

        public void addDevice(iBeaconClass.iBeacon device) {
            if(device==null)
                return;

            for(int i=0;i<mLeDevices.size();i++){
                String btAddress = mLeDevices.get(i).bluetoothAddress;
                if(btAddress.equals(device.bluetoothAddress)){
                    mLeDevices.add(i+1, device);
                    mLeDevices.remove(i);
                    return;
                }
            }
            mLeDevices.add(device);

        }

        public iBeaconClass.iBeacon getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceUUID= (TextView)view.findViewById(R.id.device_beacon_uuid);
                viewHolder.deviceMajor_Minor=(TextView)view.findViewById(R.id.device_major_minor);
                viewHolder.devicetxPower_RSSI=(TextView)view.findViewById(R.id.device_txPower_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            iBeaconClass.iBeacon device = mLeDevices.get(i);
            final String deviceName = device.name;
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            viewHolder.deviceAddress.setText(device.bluetoothAddress);
            viewHolder.deviceUUID.setText(device.proximityUuid);
            viewHolder.deviceMajor_Minor.setText("major:"+device.major+",minor:"+device.minor);
            viewHolder.devicetxPower_RSSI.setText("txPower:"+device.txPower+",rssi:"+device.rssi);
            mBeaconMajorMinor = String.valueOf(device.major) + String.valueOf(device.minor);

            return view;
        }

        class ViewHolder {
            TextView deviceName;
            TextView deviceAddress;
            TextView deviceUUID;
            TextView deviceMajor_Minor;
            TextView devicetxPower_RSSI;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scan, menu);
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
