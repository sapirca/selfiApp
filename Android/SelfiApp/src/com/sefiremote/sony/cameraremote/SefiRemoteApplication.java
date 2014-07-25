/*
 * Copyright 2013 Sony Corporation
 */

package com.sefiremote.sony.cameraremote;

import android.app.Application;

/**
 * A Application class for the sample application.
 */
public class SefiRemoteApplication extends Application {

    private ServerDevice mTargetDevice;
//    public UsbserialDriver mUsbDriver = null;
//    
//    
//
//    public UsbserialDriver getUsbDriver() {
//		return mUsbDriver;
//	}


	/**
     * Sets a target server object to transmit to WorkingModeActivity.
     * 
     * @param device
     */
    public void setTargetServerDevice(ServerDevice device) {
        mTargetDevice = device;
    }

    /**
     * Returns a target server object to get from SampleDeviceSearchActivity.
     * 
     * @param device
     */
    public ServerDevice getTargetServerDevice() {
        return mTargetDevice;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        mUsbDriver = new UsbserialDriver();
    }
    
    
    // TODO: Should we make this app singelton?
    protected void initSingletons()
    {
    }
    
    @Override
    public void onLowMemory() {
    	// TODO Auto-generated method stub
    	super.onLowMemory();
    	
    	// TODO: Do not save pictures on the phone
    	// TODO: TOAST no media can be saved
    }
}
