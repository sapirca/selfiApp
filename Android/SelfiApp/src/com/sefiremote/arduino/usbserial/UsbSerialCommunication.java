package com.sefiremote.arduino.usbserial;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class UsbSerialCommunication {

		  private final String TAG = UsbSerialCommunication.class.getSimpleName();

	    /**
	     * Driver instance, passed in statically via
	     * {@link #show(Context, UsbSerialPort)}.
	     *
	     * <p/>
	     * This is a devious hack; it'd be cleaner to re-create the driver using
	     * arguments passed in with the {@link #startActivity(Intent)} intent. We
	     * can get away with it because both activities will run in the same
	     * process, and this is a simple demo.
	     */
	    private static 	UsbSerialPort sPort = null;
	    private final 	ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	    private 		SerialInputOutputManager mSerialIoManager;
	    private String 	m_strLine = new String();
	    private Activity m_activity;
	    
	    private final SerialInputOutputManager.Listener mListener =
	            new SerialInputOutputManager.Listener() {

	        @Override
	        public void onRunError(Exception e) {
	            Log.d(TAG, "Runner stopped.");
	        }

	        @Override
	        public void onNewData(final byte[] data) {
	        	m_activity.runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                	UsbSerialCommunication.this.updateReceivedData(data);
	                }
	            });
	        }
	    };

	    public UsbSerialCommunication(Activity activiy){
	    	m_activity = activiy;
	    }
	
	    public void stop() {
	        stopIoManager();
	        if (sPort != null) {
	            try {
	                sPort.close();
	            } catch (IOException e) {
	                // Ignore.
	            }
	            sPort = null;
	        }
	    }

	    public void start() {
	        Log.d(TAG, "Resumed, port=" + sPort);
	        if (sPort == null) {
	            Toast.makeText(m_activity, "Arduino was disconnected!", Toast.LENGTH_SHORT).show();
	        } else {
	            final UsbManager usbManager = (UsbManager) m_activity.getSystemService(Context.USB_SERVICE);

	            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());

	            if (connection == null) {
	            	Toast.makeText(m_activity, "Opening usb port failed!", Toast.LENGTH_SHORT).show();
	                return;
	            }

	            try {
	                sPort.open(connection);
	                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
	            } catch (IOException e) {
	                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
//	                mTitleTextView.setText("Error opening device: " + e.getMessage());
	                try {
	                    sPort.close();
	                } catch (IOException e2) {
	                    // Ignore.
	                }
	                sPort = null;
	                return;
	            }
//	            mTitleTextView.setText("Serial device: " + sPort.getClass().getSimpleName());
	        }
	        onDeviceStateChange();
	    }

	    private void stopIoManager() {
	        if (mSerialIoManager != null) {
	            Log.i(TAG, "Stopping io manager ..");
	            mSerialIoManager.stop();
	            mSerialIoManager = null;
	        }
	    }

	    private void startIoManager() {
	        if (sPort != null) {
	            Log.i(TAG, "Starting io manager ..");
	            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
	            mExecutor.submit(mSerialIoManager);
	        }
	    }

	    private void onDeviceStateChange() {
	        stopIoManager();
	        startIoManager();
	    }

	    public void write(final byte[] src, final int timeoutMillis)
	    {
	    	try{
	    		sPort.write(src, 100);
	    	}
	    	catch(IOException e){
	    		Log.e(TAG, e.toString());
	    	}
	    }

	    public void write(byte src)
	    {
//	    	byte[] data = {src};
	    	byte[] data = new byte[1];
	    	data[0] = src;
	    	
	    	try{
	    		sPort.write(data, 100);
	    	}
	    	catch(IOException e){
	    		Log.e(TAG, e.toString());
	    	}
	    }
	    
	    private void updateReceivedData(byte[] data) {

//	        final String message = "Read " + data.length + " bytes: \n"
//	                + HexDump.dumpHexString(data) + "\n\n";

	    	
	    	for (int i = 0; i < data.length; i++) {
	    		if (data[i] == 0x0A) {
	    			
//	    	        final String message = "Read " + data.length + " bytes: \n"
//	    	                + HexDump.dumpHexString(data) + "\n\n";
	    	        final String message = "Read " + m_strLine.length() + " bytes: \n"
	    	                + HexDump.dumpHexString(m_strLine.getBytes()) + "\n\n";
	    	        
//	    	        Toast.makeText(m_activity, "Data: " + message, Toast.LENGTH_SHORT).show();
	    	        Log.d(TAG, "Msg Received:" + message);

	    	        m_strLine = "";

	    		} else {
	    			if (data[i] != 0x0D)
	    				m_strLine += ((char)data[i]);//Byte.toString(data[i]);
	    		}
	    	}
	    }


	    static void setPort(Context context, UsbSerialPort port) {
	        
	    	sPort = port;
//	        final Intent intent = new Intent(context, UsbSerialCommunication.class);
//	        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
//	        
////	        context.startActivity(intent);
//	        context.startService(intent);
	    }
}
