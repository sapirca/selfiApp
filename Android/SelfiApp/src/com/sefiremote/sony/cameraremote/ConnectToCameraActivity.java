/*
 * Copyright 2013 Sony Corporation
 */

package com.sefiremote.sony.cameraremote;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sefiremote.R;
import com.sefiremote.arduino.usbserial.UsbSerialCommunication;
import com.sefiremote.sony.cameraremote.ServerDevice.ApiService;

import java.util.ArrayList;
import java.util.List;

/**
 * An Activity class of Device Discovery screen.
 */
public class ConnectToCameraActivity extends Activity {

	
    private static final String TAG = ConnectToCameraActivity.class.getSimpleName();
    private SsdpClient mSsdpClient;
    private DeviceListAdapter mListAdapter;
    private boolean mActivityActive;
    private boolean mWifiCamFlag = false;
    private boolean mSwitchToWork = false;
    
    private TextView mtextWifiSsid; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_discovery);
        setProgressBarIndeterminateVisibility(false);

        mSsdpClient = new SsdpClient();
        mListAdapter = new DeviceListAdapter(this);
         
    	WifiScanReceiver wifiReciever = new WifiScanReceiver();
    	registerReceiver(wifiReciever, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
   	

        // Show Wi-Fi SSID.
        mtextWifiSsid = (TextView) findViewById(R.id.text_wifi_ssid);
        
        Log.d(TAG, "onCreate() completed.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        mActivityActive = true;
        mSwitchToWork  = false;
        ListView listView = (ListView) findViewById(R.id.list_device);
        listView.setAdapter(mListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
            	
                ListView listView = (ListView) parent;
                ServerDevice device = (ServerDevice) listView.getAdapter()
                        .getItem(position);
                launchSampleActivity(device);
            }
        });

        findViewById(R.id.button_search).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Button btn = (Button) v;
                        if (!mSsdpClient.isSearching()) {
                            searchDevices();
                            btn.setEnabled(false);
                        }
                    }
                });

        
        mtextWifiSsid.setText("Building network configuration");
        
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        
        // Enable the WiFi (if disabled)
        wifi.setWifiEnabled(true);
        
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"DIRECT-DZC2:DSC-RX100M3\"";
        wc.preSharedKey  = "\"CM8owHL2\"";
        wc.hiddenSSID = true;
        wc.status = WifiConfiguration.Status.ENABLED;    
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        
        int res = wifi.addNetwork(wc);
        Log.d("WifiPreference", "add Network returned " + res );
        
        mtextWifiSsid.setText("Attempt to connect the camera network");
        boolean b = wifi.enableNetwork(res, true);
        Log.d("WifiPreference", "enableNetwork returned " + b );        
        
    	wifi.disconnect();
    	wifi.reconnect();  
    	
        Log.d(TAG, "onResume() completed.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityActive = false;
        if (mSsdpClient != null && mSsdpClient.isSearching()) {
            mSsdpClient.cancelSearching();
        }

        if (mSwitchToWork == false) {

        	// TODO: Move code to method...
    		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
  		  
    		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
    		for( WifiConfiguration i : list ) {
    			// TODO: Constant/Settings
    			if(i.SSID != null && i.SSID.equals("\"DIRECT-DZC2:DSC-RX100M3\""))
    		  		wifiManager.removeNetwork(i.networkId);
    		  	else
    		  		wifiManager.enableNetwork(i.networkId, false);
    		}
    		  
    		wifiManager.disconnect();
    		wifiManager.reconnect();               
        }
        
        Log.d(TAG, "onPause() completed.");
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
        
    	Log.d(TAG, "onDestroy() completed.");
    }
    
    // Start searching supported devices.
    private void searchDevices() {
        mListAdapter.clearDevices();
        setProgressBarIndeterminateVisibility(true);
        mSsdpClient.search(new SsdpClient.SearchResultHandler() {

            @Override
            public void onDeviceFound(final ServerDevice device) {
                // Called by non-UI thread.
                Log.d(TAG,
                        ">> Search device found: " + device.getFriendlyName());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListAdapter.addDevice(device);
                    }
                });
            }

            @Override
            public void onFinished() {
                // Called by non-UI thread.
                Log.d(TAG, ">> Search finished.");
               
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgressBarIndeterminateVisibility(false);
                        findViewById(R.id.button_search).setEnabled(true);
                        if (mActivityActive) {
                            Toast.makeText(ConnectToCameraActivity.this,
                                    R.string.msg_device_search_finish,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onErrorFinished() {
                // Called by non-UI thread.
                Log.d(TAG, ">> Search Error finished.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setProgressBarIndeterminateVisibility(false);
                        findViewById(R.id.button_search).setEnabled(true);
                        if (mActivityActive) {
                            Toast.makeText(ConnectToCameraActivity.this,
                                    R.string.msg_error_device_searching,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    // Launch a WorkingModeActivity.
    private void launchSampleActivity(ServerDevice device) {
        // Go to CameraSampleActivity.
        Toast.makeText(ConnectToCameraActivity.this,
                device.getFriendlyName(), Toast.LENGTH_SHORT).show();

        mSwitchToWork = true;
        
        // Set target ServerDevice instance to control in Activity.
        SefiRemoteApplication app = (SefiRemoteApplication) getApplication();
        app.setTargetServerDevice(device);
        Intent intent = new Intent(this, WorkingModeActivity.class);
        startActivity(intent);
    }

    // Adapter class for DeviceList
    private static class DeviceListAdapter extends BaseAdapter {

        private List<ServerDevice> mDeviceList;
        private LayoutInflater mInflater;

        public DeviceListAdapter(Context context) {
            mDeviceList = new ArrayList<ServerDevice>();
            mInflater = LayoutInflater.from(context);
        }

        public void addDevice(ServerDevice device) {
            mDeviceList.add(device);
            notifyDataSetChanged();
        }

        public void clearDevices() {
            mDeviceList.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0; // not fine
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TextView textView = (TextView) convertView;
            if (textView == null) {
                textView = (TextView) mInflater.inflate(
                        R.layout.device_list_item, null);
            }
            ServerDevice device = (ServerDevice) getItem(position);
            ApiService apiService = device.getApiService("camera");
            String endpointUrl = null;
            if (apiService != null) {
                endpointUrl = apiService.getEndpointUrl();
            }

            // Label
            String htmlLabel = String.format("%s ", device.getFriendlyName())
                    + String.format(
                            "<br><small>Endpoint URL:  <font color=\"blue\">%s</font></small>",
                            endpointUrl);
            textView.setText(Html.fromHtml(htmlLabel));

            return textView;
        }
    }
    
    
    class WifiScanReceiver extends BroadcastReceiver {
    	
    	public void onReceive(Context c, Intent intent) {

    		if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {

    			NetworkInfo nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
    			
    			if (nwInfo.isConnected()) {
    			
	    	        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
	    	        
	    	        if (wifiManager.getConnectionInfo().getSSID().equals("\"DIRECT-DZC2:DSC-RX100M3\""))
	    	        	
	    	        	mtextWifiSsid.setText("Conntected to camera wifi, looking for the camera's url");
	    	        	
	                    runOnUiThread(new Runnable() {
	                        @Override
	                        public void run() {
	                            ((Button)findViewById(R.id.button_search)).performClick();
	                        }
	                    });
    			}
    		}
    	}
    }
}
