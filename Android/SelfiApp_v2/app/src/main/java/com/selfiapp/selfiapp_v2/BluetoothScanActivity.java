package com.selfiapp.selfiapp_v2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TwoLineListItem;

//import com.sefiremote.sony.cameraremote.ConnectToCameraActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import sony.cameraremote.ConnectToCameraActivity;

public class BluetoothScanActivity extends Activity {

    private final String TAG = BluetoothScanActivity.class.getSimpleName();

//    private UsbManager mUsbManager;
    private ListView mListView;
    private TextView mProgressBarTitle;
    private ProgressBar mProgressBar;

    /**
     * Member fields
     */
    private BluetoothAdapter mBtAdapter;

    private static final int    MESSAGE_REFRESH = 101;
    private static final long   REFRESH_TIMEOUT_MILLIS = 5000;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    refreshDeviceList();
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    private List<String> mEntries = new ArrayList<String>();
    private ArrayAdapter<String> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btscan);

        mListView = (ListView) findViewById(R.id.deviceList);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBarTitle = (TextView) findViewById(R.id.progressBarTitle);

        // Register for bluetooth broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        // Request location permission to allow BT Scan on Android 6+
        if (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION )
                != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {
                android.Manifest.permission.ACCESS_COARSE_LOCATION  }, 1234);
        }

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Enable Bluetooth if disabled
        if (!mBtAdapter.isEnabled())
            mBtAdapter.enable();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_2, mEntries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TwoLineListItem row;
                if (convertView == null){
                    final LayoutInflater inflater =
                            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    row = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = (TwoLineListItem) convertView;
                }

                String[] arrEntry = mEntries.get(position).toString().split("\n");

                row.getText1().setText(arrEntry[0]);
                row.getText2().setText(arrEntry[1]);

                return row;
            }

        };

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        mListView.setAdapter(mAdapter);

        // Stop the BT service (if running)
        stopService(new Intent(getApplicationContext(), BluetoothArduinoService.class));

        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Pressed item " + position);
                if (position >= mEntries.size()) {
                    Log.w(TAG, "Illegal position.");
                    return;
                }

//                if (BluetoothArduinoService.IsRunning() == false) {
                    Intent intent = new Intent(getApplicationContext(), BluetoothArduinoService.class);
                    intent.putExtra("CMD", BluetoothArduinoService.CMD_CONNECT);
                    intent.putExtra("DEV", mEntries.get(position));
                    startService(intent);
//                }

                // Make sure we're not doing discovery anymore
                if (mBtAdapter != null)
                    mBtAdapter.cancelDiscovery();

                mProgressBarTitle.setText("Connecting...");

//                if (BluetoothArduinoService.IsRunning() == false)
//                    startService(new Intent(getApplicationContext(), BluetoothArduinoService.class));
//                else
//                    stopService(new Intent(getApplicationContext(), BluetoothArduinoService.class));

//                final UsbSerialPort port = mEntries.get(position);
//                showConsoleActivity(port);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.sendEmptyMessage(MESSAGE_REFRESH);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(MESSAGE_REFRESH);
    }

    private void refreshDeviceList() {

        // Current BT service state
        int iState = BluetoothArduinoService.STATE_NONE;

        if (BluetoothArduinoService.mInstance != null)
            iState = BluetoothArduinoService.mInstance.getState();

        switch (iState) {
            case BluetoothArduinoService.STATE_NONE:
            case BluetoothArduinoService.STATE_LISTEN: {

                mProgressBarTitle.setText("Scanning...");

                // If we're already discovering, stop it
                if (mBtAdapter.isDiscovering()) {
                    mBtAdapter.cancelDiscovery();
                }

                // Request discover from BluetoothAdapter
                mBtAdapter.startDiscovery();

            } break;

            case BluetoothArduinoService.STATE_CONNECTING: {

                mProgressBarTitle.setText("Connecting...");

            } break;

            case BluetoothArduinoService.STATE_CONNECTED: {

                mProgressBarTitle.setText("Connected!");

                // Start the next activity
                this.startActivity(new Intent(this, ConnectToCameraActivity.class));

//                BluetoothArduinoService.mInstance.write(new String("Hello from Android!").getBytes());

            } break;
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, "BT Action: " + action);

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

                    String strDevice = device.getName() + "\n" + device.getAddress();

                    // Add non-existing items
                    if (mAdapter.getPosition(strDevice) < 0) {
                        Log.d(TAG, "Found: " + device.getName() + " " + device.getAddress());
                        mAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
//                }
                // When discovery is started
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                showProgressBar();
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                hideProgressBar();
            }
        }
    };

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBarTitle.setText(R.string.refreshing);
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }

}
