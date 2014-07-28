/*
 * Copyright 2013 Sony Corporation
 */

package com.sefiremote.sony.cameraremote;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sefiremote.R;
import com.sefiremote.arduino.usbserial.UsbSerialCommunication;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An Activity class of Sample Camera screen.
 */
public class WorkingModeActivity extends Activity {

    private static final String TAG = WorkingModeActivity.class.getSimpleName();

    private UsbSerialCommunication mUsbSerialComm;

    private final byte 			PAN_UP_START	= 25;
	private final byte 			PAN_UP_STOP		= 26;
	private final byte 			PAN_DOWN_START	= 27;
	private final byte 			PAN_DOWN_STOP	= 28;
	private final byte 			PAN_LEFT_START	= 29;
	private final byte 			PAN_LEFT_STOP	= 30;
	private final byte 			PAN_RIGHT_START	= 31;
	private final byte 			PAN_RIGHT_STOP	= 32;

	private final byte 			PAN_SELFI_START = 33;
	private final byte 			PAN_SELFI_STOP 	= 34;
	private final byte 			PAN_RESET_START	= 35;
	private final byte 			PAN_RESET_STOP	= 36;

    private ImageView 			mImagePictureWipe;
//  private Spinner 			mSpinnerShootMode;
    private ImageButton 		mButtonTakePicture;
    private TextView	 		mTextTakePicture;
    private ImageButton 		mButtonRecStartStop;
    private TextView	 		mTextVideo;

    private TextView	 		mTextCountdown;
    private LinearLayout 		mLayoutAction;
    
    private ImageButton 		mButtonPanUp;
    private ImageButton 		mButtonPanDown;
    private ImageButton 		mButtonPanRight;
    private ImageButton 		mButtonPanLeft;
 
    private ImageButton 		mButtonFlashMode;
    private ImageButton 		mButtonRotateSelfi;
    private ImageButton 		mButtonRotateReset;
    private ImageButton 		mButtonSetTimer;
    
    private Button 				mButtonZoomIn;
    private Button 				mButtonZoomOut;
    private TextView 			mTextCameraStatus;

    private ServerDevice 		mTargetServer;
    private RemoteApi 			mRemoteApi;
    private LiveviewSurfaceView mLiveviewSurface;
    private CameraEventObserver mEventObserver;
    private final Set<String> 	mAvailableApiSet = new HashSet<String>();

    private enum EFlashMode{
    	off,
    	auto,
    	on {
            @Override
            public EFlashMode next() {
                return off; // see below for options for this line
            };
        };

        public EFlashMode next() {
            // No bounds checking required here, because the last instance overrides
            return values()[ordinal() + 1];
        }
    }
    
    private enum ETimerMode{
    	off,
    	sec5,
    	sec10 {
            @Override
            public ETimerMode next() {
                return off; // see below for options for this line
            };
        };

        public ETimerMode next() {
            // No bounds checking required here, because the last instance overrides
            return values()[ordinal() + 1];
        }
    }
    
    private enum EVideoMode{
    	off,
    	on{
            @Override
            public EVideoMode next() {
                return off; // see below for options for this line
            };
        };

        public EVideoMode next() {
            // No bounds checking required here, because the last instance overrides
            return values()[ordinal() + 1];
        }
    }
    
    private EFlashMode mFlashMode = EFlashMode.off; 
    private ETimerMode mTimerMode = ETimerMode.off; 
    private EVideoMode mVideoMode = EVideoMode.off; 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      
    	super.onCreate(savedInstanceState);
        
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
        mUsbSerialComm = new UsbSerialCommunication(this);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_working_mode);

        SefiRemoteApplication app = (SefiRemoteApplication) getApplication();
        mTargetServer = app.getTargetServerDevice();
        mRemoteApi = new RemoteApi(mTargetServer);
        mEventObserver = new CameraEventObserver(this, mRemoteApi);

        mImagePictureWipe = (ImageView) findViewById(R.id.image_picture_wipe);
//        mSpinnerShootMode = (Spinner) findViewById(R.id.spinner_shoot_mode);
        mButtonTakePicture = (ImageButton) findViewById(R.id.button_take_picture);
        mTextTakePicture	= (TextView) findViewById(R.id.button_take_picture_text);
        mButtonRecStartStop = (ImageButton) findViewById(R.id.button_rec_start_stop);
        mTextVideo 			= (TextView) findViewById(R.id.button_rec_start_stop_text);

        mTextCountdown = (TextView) findViewById(R.id.text_countdown);

        mButtonPanUp = (ImageButton) findViewById(R.id.button_pan_up);
        mButtonPanDown = (ImageButton) findViewById(R.id.button_pan_down);
        mButtonPanRight = (ImageButton) findViewById(R.id.button_pan_right);
        mButtonPanLeft = (ImageButton) findViewById(R.id.button_pan_left);
        
        mButtonZoomIn = (Button) findViewById(R.id.button_zoom_in);
        mButtonZoomOut = (Button) findViewById(R.id.button_zoom_out);
        mTextCameraStatus = (TextView) findViewById(R.id.text_camera_status);
        mLiveviewSurface = (LiveviewSurfaceView) findViewById(R.id.surfaceview_liveview);

        mButtonFlashMode = (ImageButton) findViewById(R.id.button_flash_mode);
        mButtonRotateSelfi = (ImageButton) findViewById(R.id.button_rotate_selfi);
        mButtonRotateReset = (ImageButton) findViewById(R.id.button_rotate_reset);
        mButtonSetTimer = (ImageButton) findViewById(R.id.button_timer);
        
        
        mLayoutAction = (LinearLayout) findViewById(R.id.layout_actions);
        
        mLiveviewSurface.bindRemoteApi(mRemoteApi);

        
         // TODO: Why did they put "False"?
//        mSpinnerShootMode.setEnabled(false);
        
        
        Log.d(TAG, "onCreate() completed.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUsbSerialComm.start();

//        mSpinnerShootMode.setFocusable(true);
        
        
//        mButtonRotateSelfi
//        mButtonRotateReset
        
        mButtonFlashMode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	 
            	mFlashMode = mFlashMode.next();
            	 
            	 switch(mFlashMode)
            	 {
            	 case off:
            		 WorkingModeActivity.this.mButtonFlashMode.setImageResource(R.drawable.mcp_flash_off);
            		 break;
            	 case on:
            		 WorkingModeActivity.this.mButtonFlashMode.setImageResource(R.drawable.mcp_flash_on);
            		 break;
            	 case auto:
            		 WorkingModeActivity.this.mButtonFlashMode.setImageResource(R.drawable.mcp_flash_automatic);
            		 break;
            	 }
            	 
            	 setFlashMode(mFlashMode);
            }
        });

        
        mButtonSetTimer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	
            	mTimerMode = mTimerMode.next();
            	
            	switch(mTimerMode)
	           	 {
	           	 case off:
	           		 WorkingModeActivity.this.mButtonSetTimer.setImageResource(R.drawable.mcp_timer_off);
//	           		setSelfTimer(0, true);
	           		 
	           		 break;
	           	 case sec5: 
	           		 WorkingModeActivity.this.mButtonSetTimer.setImageResource(R.drawable.mcp_timer_5);
	           		 // The program will suspend itself 3 seconds and then uses the 2 seconds (because of the sounds, it beeps)
//	           		 setSelfTimer(2, true);
	           		 break;

	           	 case sec10: 
	           		 // The program will suspend itself 8 seconds and then uses the 2 seconds (because of the sounds, it beeps)
	           		 WorkingModeActivity.this.mButtonSetTimer.setImageResource(R.drawable.mcp_timer_10);
//	           		setSelfTimer(2, true);
	           		 break;
	           	 }
            	
            	
            	// if no need of 2 and 10, use just 10 and this icon
//            	setSelfTimer(10);
//            	WorkingModeActivity.this.mButtonSetTimer.setBackgroundResource(R.drawable.mcp_data_usage);  	- on
//            	WorkingModeActivity.this.mButtonSetTimer.setBackgroundResource(R.drawable.mcp_time);  			- off
            }
        });
        
        mButtonTakePicture.setOnClickListener(new View.OnClickListener() {

	        @Override
	        public void onClick(View v) {
            	
	        	disableButtons_still();
	        	
            	// We were called from the takePicture method
      			mTextTakePicture.setText("Switching");
      			
				new Thread() {
					@Override
					public void run() {
	            	
		      			// Force "still" shoot mode
		            	setShootMode("still", false);
		            	
		            	runOnUiThread(new Runnable() { public void run() {
		            			
		            			mTextTakePicture.setText(""); // Done switching modes
					    	}});
		            	
		            	switch(mTimerMode) {
		
		            	case off:
		             		setSelfTimer(0, false);
			           		takeAndFetchPicture();
			           		 break;
		           	
		            	case sec5: 
		            		setSelfTimer(2, false);
		            		
	            				
	            				for (int i=5; i>3; i--){
	            					final Integer val = i;
	            					
	            					WorkingModeActivity.this.runOnUiThread(new Runnable() { public void run() {
				            					    	mTextTakePicture.setText(val.toString());
				            					    	mTextCountdown.setText(val.toString());
				            					    	} });
	            					try {
										sleep(1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
	            				}
	            			
	            				takeAndFetchPicture();
	            				
	
	            				for (int i=3; i>0; i--){
	
	            					final Integer val = i;
	            					
	            					WorkingModeActivity.this.runOnUiThread(new Runnable() { public void run() {
				            					    	mTextTakePicture.setText(val.toString());
				            					    	mTextCountdown.setText(val.toString());
				            					    	} });
	            					try {
										sleep(1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
	            				}
	            			
	            			
	    					WorkingModeActivity.this.runOnUiThread(new Runnable() { public void run() {
		            					    	mTextTakePicture.setText(""); 
		            					    	mTextCountdown.setText("");
		            					    	} });
	    					
	    					

		                 break;
		                 
	            	case sec10: 
	
	            		setSelfTimer(2, false);
	            		
	            				
	            				for (int i=10; i>3; i--){
	            					final Integer val = i;
	            					
	            					WorkingModeActivity.this.runOnUiThread(new Runnable() { public void run() {
				            					    	mTextTakePicture.setText(val.toString());
				            					    	mTextCountdown.setText(val.toString());
				            					    	} });
	            					try {
										sleep(1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
	            				}
	            			
	            				takeAndFetchPicture();
	            				
	
	            				for (int i=3; i>0; i--){
	
	            					final Integer val = i;
	            					
	            					WorkingModeActivity.this.runOnUiThread(new Runnable() { public void run() {
				            					    	mTextTakePicture.setText(val.toString()); 
				            					    	mTextCountdown.setText(val.toString());
				            					    	} });
	            					try {
										sleep(1000);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
	            				}
	            			
	            			
	
	    					WorkingModeActivity.this.runOnUiThread(new Runnable() { public void run() {
		            					    	mTextTakePicture.setText("");
		            					    	mTextCountdown.setText("");
		            					    	} });
	    					
	            		
		                 break;
		           	
	            	default: break;
		           	}
	            	
	              }

              }.start();

            }
        });
        
        mButtonRecStartStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
        
            	mVideoMode = mVideoMode.next();
            	
            	switch(mVideoMode)
	           	 {
	           	 case off:
	           		 enableButtons_video();
	           		 
//	           		WorkingModeActivity.this.mButtonRecStartStop.setImageResource(R.drawable.mcp_video);
	           		new Thread() {
	           			
		           		@Override
		           		public void run() {
//			           		try {
				           		
			           			stopMovieRec();
//			           			sleep(5000);
				            	setShootMode("still", false);
				            	
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
		           		}
	           		}.start();
	           		
	           		 break;
	           		 
	           	 case on: 
 	           		disableButtons_video();
 	           		
 	           		mTextVideo.setText("Switching..");
 	           		
//	           		WorkingModeActivity.this.mButtonRecStartStop.setImageResource(R.drawable.mcp_stop);
	           		new Thread() {
	           		
	           			@Override
	           			public void run() {
	           				
//	    	           		try {
		    	           		setShootMode("movie", false);
//			           			sleep(5000);
		    	           		
		    	           		startMovieRec();
//	    					} catch (InterruptedException e) {
//	    						// TODO Auto-generated catch block
//	    						e.printStackTrace();
//	    					}
	           			}
	           		}.start();
	           		
	           		 break;
	           	 }
            	
//                if ("MovieRecording".equals(mEventObserver.getCameraStatus())) {
//                	WorkingModeActivity.this.mButtonRecStartStop.setImageResource(R.drawable.mcp_video);
//                	setShootMode("movie");
//                    stopMovieRec();
//                } else if ("IDLE".equals(mEventObserver.getCameraStatus())) {
//                	WorkingModeActivity.this.mButtonRecStartStop.setImageResource(R.drawable.mcp_stop);
//                    startMovieRec();
//                }
            }
        });
        
        mEventObserver
                .setEventChangeListener(new CameraEventObserver.ChangeListener() {

                    @Override
                    public void onShootModeChanged(String shootMode) {
                        Log.d(TAG, "onShootModeChanged() called: " + shootMode);
                        refreshUi();
                    }

                    @Override
                    public void onCameraStatusChanged(String status) {
                        Log.d(TAG, "onCameraStatusChanged() called: " + status);
                        refreshUi();
                    }

                    @Override
                    public void onApiListModified(List<String> apis) {
                        Log.d(TAG, "onApiListModified() called");
                        synchronized (mAvailableApiSet) {
                            mAvailableApiSet.clear();
                            for (String api : apis) {
                                mAvailableApiSet.add(api);
                            }
                            if (!mEventObserver.getLiveviewStatus()
                                    && isApiAvailable("startLiveview")) {
                                if (!mLiveviewSurface.isStarted()) {
                                    mLiveviewSurface.start();
                                }
                            }
                            if (isApiAvailable("actZoom")) {
                                Log.d(TAG,
                                        "onApiListModified(): prepareActZoomButtons()");
                                prepareActZoomButtons(true);
                            } else {
                                prepareActZoomButtons(false);
                            }
                        }
                    }

                    @Override
                    public void onZoomPositionChanged(int zoomPosition) {
                        Log.d(TAG, "onZoomPositionChanged() called = " + zoomPosition);
                        if (zoomPosition == 0) {
                            mButtonZoomIn.setEnabled(true);
                            mButtonZoomOut.setEnabled(false);
                        } else if (zoomPosition == 100) {
                            mButtonZoomIn.setEnabled(false);
                            mButtonZoomOut.setEnabled(true);
                        } else {
                            mButtonZoomIn.setEnabled(true);
                            mButtonZoomOut.setEnabled(true);
                        }
                    }

                    @Override
                    public void onLiveviewStatusChanged(boolean status) {
                        Log.d(TAG, "onLiveviewStatusChanged() called = " + status);
                    }
                });
        mImagePictureWipe.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mImagePictureWipe.setVisibility(View.INVISIBLE);
            }
        });

        mButtonPanUp.setOnTouchListener(new View.OnTouchListener() {
	
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mUsbSerialComm.write(PAN_UP_START);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mUsbSerialComm.write(PAN_UP_STOP);
                }

                return false;
            }
		});
        
        mButtonPanDown.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mUsbSerialComm.write(PAN_DOWN_START);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mUsbSerialComm.write(PAN_DOWN_STOP);
                }
                
                return false;
            }
		});
        
        
        mButtonPanRight.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mUsbSerialComm.write(PAN_RIGHT_START);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mUsbSerialComm.write(PAN_RIGHT_STOP);
                }
                
                return false;
            }
		});
        

        mButtonPanLeft.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mUsbSerialComm.write(PAN_LEFT_START);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mUsbSerialComm.write(PAN_LEFT_STOP);
                }
                
                return false;
            }
		});
        
        mButtonRotateSelfi.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mUsbSerialComm.write(PAN_SELFI_START);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mUsbSerialComm.write(PAN_SELFI_STOP);
                }
                return false;
            }
		});
        
        mButtonRotateReset.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                	mUsbSerialComm.write(PAN_RESET_START);
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                		   event.getAction() == MotionEvent.ACTION_CANCEL) {
                	mUsbSerialComm.write(PAN_RESET_STOP);
                }
                return false;
            }
		});
        
        
        mButtonZoomIn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                actZoom("in", "1shot");
            }
        });

        mButtonZoomOut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                actZoom("out", "1shot");
            }
        });

        mButtonZoomIn.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                actZoom("in", "start");
                return true;
            }
        });

        mButtonZoomOut.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                actZoom("out", "start");
                return true;
            }
        });

        mButtonZoomIn.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        actZoom("in", "stop");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        mButtonZoomOut.setOnTouchListener(new View.OnTouchListener() {

            long downTime = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - downTime > 500) {
                        actZoom("out", "stop");
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis();
                }
                return false;
            }
        });

        openConnection();
              
        

        Log.d(TAG, "onResume() completed.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeConnection();

        mUsbSerialComm.stop();

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
        
        Log.d(TAG, "onPause() completed.");
        
        finish();
    }
    
    // Open connection to the camera device to start monitoring Camera events
    // and showing liveview.
    private void openConnection() {
        setProgressBarIndeterminateVisibility(true);
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "openConnection(): exec.");
                try {
                    JSONObject replyJson = null;

                    // getAvailableApiList
                    replyJson = mRemoteApi.getAvailableApiList();
                    loadAvailableApiList(replyJson);

                    // check version of the server device
                    if (isApiAvailable("getApplicationInfo")) {
                        Log.d(TAG, "openConnection(): getApplicationInfo()");
                        replyJson = mRemoteApi.getApplicationInfo();
                        if (!isSupportedServerVersion(replyJson)) {
                            toast(R.string.msg_error_non_supported_device);
                            WorkingModeActivity.this.finish();
                            return;
                        }
                    } else {
                        // never happens;
                        return;
                    }

                    // startRecMode if necessary.
                    if (isApiAvailable("startRecMode")) {
                        Log.d(TAG, "openConnection(): startRecMode()");
                        replyJson = mRemoteApi.startRecMode();

                        // Call again.
                        replyJson = mRemoteApi.getAvailableApiList();
                        loadAvailableApiList(replyJson);
                    }

                    // getEvent start
                    if (isApiAvailable("getEvent")) {
                        Log.d(TAG, "openConnection(): EventObserver.start()");
                        mEventObserver.start();
                    }

                    // Liveview start
                    if (isApiAvailable("startLiveview")) {
                        Log.d(TAG, "openConnection(): LiveviewSurface.start()");
                        mLiveviewSurface.start();
                    }

//                    // prepare UIs
//                    if (isApiAvailable("getAvailableShootMode")) {
//                        Log.d(TAG,
//                                "openConnection(): prepareShootModeSpinner()");
//                        prepareShootModeSpinner();
//                        // Note: hide progress bar on title after this calling.
//                    }

                    // prepare UIs
                    if (isApiAvailable("actZoom")) {
                        Log.d(TAG,
                                "openConnection(): prepareActZoomButtons()");
                        prepareActZoomButtons(true);
                    } else {
                        prepareActZoomButtons(false);
                    }

                    // Disable the timer
                    setSelfTimer(0, true);
                    
                    Log.d(TAG, "openConnection(): completed.");
                } catch (IOException e) {
                    Log.w(TAG, "openConnection: IOException: " + e.getMessage());
                    setProgressIndicator(false);
                    toast(R.string.msg_error_connection);
                }
            }
        }.start();
    }

    // Close connection to stop monitoring Camera events and showing liveview.
    private void closeConnection() {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "closeConnection(): exec.");
                try {
                    // Liveview stop
                    Log.d(TAG, "closeConnection(): LiveviewSurface.stop()");
                    mLiveviewSurface.stop();

                    // getEvent stop
                    Log.d(TAG, "closeConnection(): EventObserver.stop()");
                    mEventObserver.stop();

                    // stopRecMode if necessary.
                    if (isApiAvailable("stopRecMode")) {
                        Log.d(TAG, "closeConnection(): stopRecMode()");
                        mRemoteApi.stopRecMode();
                    }

                    Log.d(TAG, "closeConnection(): completed.");
                } catch (IOException e) {
                    Log.w(TAG,
                            "closeConnection: IOException: " + e.getMessage());
                }
            }
        }.start();
    }

    // Refresh UI appearance along current "cameraStatus" and "shootMode".
    private void refreshUi() {
        String cameraStatus = mEventObserver.getCameraStatus();
        String shootMode = mEventObserver.getShootMode();

        // CameraStatus TextView
        mTextCameraStatus.setText(cameraStatus);

        mButtonRecStartStop.setEnabled(true);
        
        // Recording Start/Stop Button
        if ("MovieRecording".equals(cameraStatus)) {
//            mButtonRecStartStop.setEnabled(true);
            
              mButtonRecStartStop.setBackgroundResource(R.drawable.mcp_stop);
              mButtonRecStartStop.setBackgroundColor(Color.TRANSPARENT);
            
        } else if ("IDLE".equals(cameraStatus) && "movie".equals(shootMode)) {
//            mButtonRecStartStop.setEnabled(true);

            mButtonRecStartStop.setBackgroundResource(R.drawable.mcp_video);
            mButtonRecStartStop.setBackgroundColor(Color.TRANSPARENT);
        }
//      } else {
//            mButtonRecStartStop.setEnabled(false);
//        }

//        // Take picture Button
//        if ("still".equals(shootMode) && "IDLE".equals(cameraStatus)) {
//            mButtonTakePicture.setEnabled(true);
//        } else {
//            mButtonTakePicture.setEnabled(false);
//        }

        // Picture wipe Image
        if (!"still".equals(shootMode)) {
            mImagePictureWipe.setVisibility(View.INVISIBLE);
        }

        // Shoot Mode Buttons
//        if ("IDLE".equals(cameraStatus) || "MovieRecording".equals(cameraStatus)) {
//            mSpinnerShootMode.setEnabled(true);
//            selectionShootModeSpinner(mSpinnerShootMode, shootMode);
//        } else {
//            mSpinnerShootMode.setEnabled(false);
//        }
    }

    // Retrieve a list of APIs that are available at present.
    private void loadAvailableApiList(JSONObject replyJson) {
        synchronized (mAvailableApiSet) {
            mAvailableApiSet.clear();
            try {
                JSONArray resultArrayJson = replyJson.getJSONArray("result");
                JSONArray apiListJson = resultArrayJson.getJSONArray(0);
                for (int i = 0; i < apiListJson.length(); i++) {
                    mAvailableApiSet.add(apiListJson.getString(i));
                }
            } catch (JSONException e) {
                Log.w(TAG, "loadAvailableApiList: JSON format error.");
            }
        }
    }

    // Check if the indicated API is available at present.
    private boolean isApiAvailable(String apiName) {
        boolean isAvailable = false;
        synchronized (mAvailableApiSet) {
            isAvailable = mAvailableApiSet.contains(apiName);
        }
        return isAvailable;
    }

    // Check if the version of the server is supported in this application.
    private boolean isSupportedServerVersion(JSONObject replyJson) {
        try {
            JSONArray resultArrayJson = replyJson.getJSONArray("result");
            String version = resultArrayJson.getString(1);
            String[] separated = version.split("\\.");
            int major = Integer.valueOf(separated[0]);
            if (2 <= major) {
                return true;
            }
        } catch (JSONException e) {
            Log.w(TAG, "isSupportedServerVersion: JSON format error.");
        } catch (NumberFormatException e) {
            Log.w(TAG, "isSupportedServerVersion: Number format error.");
        }
        return false;
    }

    // Check if the shoot mode is supported in this application.
    private boolean isSupportedShootMode(String mode) {
        if ("still".equals(mode) || "movie".equals(mode)) {
            return true;
        }
        return false;
    }

    

    
    // Prepare for Spinner to select "shootMode" by user.
    private void prepareShootModeSpinner() {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "prepareShootModeSpinner(): exec.");
                JSONObject replyJson = null;
                try {
                    replyJson = mRemoteApi.getAvailableShootMode();

                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    final String currentMode = resultsObj.getString(0);
                    JSONArray availableModesJson = resultsObj.getJSONArray(1);
                    final ArrayList<String> availableModes = new ArrayList<String>();

                    for (int i = 0; i < availableModesJson.length(); i++) {
                        String mode = availableModesJson.getString(i);
                        if (!isSupportedShootMode(mode)) {
                            mode = "";
                        }

                        availableModes.add(mode);
                        

                    }
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                           prepareShootModeSpinnerUi(
                                    availableModes.toArray(new String[0]),
                                    currentMode);
                            // Hide progress indeterminate on title bar.
                            setProgressBarIndeterminateVisibility(false);
                        }
                    });
                } catch (IOException e) {
                    Log.w(TAG, "prepareShootModeRadioButtons: IOException: "
                            + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG,
                            "prepareShootModeRadioButtons: JSON format error.");
                }
            };
        }.start();
    }

    // Selection for Spinner UI of Shoot Mode.
    private void selectionShootModeSpinner(Spinner spinner, String mode) {
//        if (!isSupportedShootMode(mode)) {
//            mode = "";
//        }
//        @SuppressWarnings("unchecked")
//        ArrayAdapter<String> adapter = (ArrayAdapter<String>)spinner.getAdapter();
//        if (adapter != null) {
//            mSpinnerShootMode.setSelection(adapter.getPosition(mode));
//        }
    }

    // Prepare for Spinner UI of Shoot Mode.
    private void prepareShootModeSpinnerUi(String[] availableShootModes,
            String currentMode) {

//        ArrayAdapter<String> adapter
//            = new ArrayAdapter<String>(this,
//                    android.R.layout.simple_spinner_item, availableShootModes);
//        
    	// This is brute force setting of all modes due to unclear reponds from the RX100M3 about its available modes
    	// TODO: not that pretty.
//    	ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(WorkingModeActivity.this,
//        		R.array.Spinner_modes, android.R.layout.simple_list_item_1);
//        
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        mSpinnerShootMode.setAdapter(adapter);
//        mSpinnerShootMode.setPrompt(getString(R.string.prompt_shoot_mode));
//        selectionShootModeSpinner(mSpinnerShootMode, currentMode);
//        
//        mSpinnerShootMode.setOnItemSelectedListener(new OnItemSelectedListener(){
//            // selected Spinner dropdown item
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Spinner spinner = (Spinner)parent;
//                if (spinner.isFocusable() == false) {
//                    // ignored the first call, because shoot mode has not changed
//                    spinner.setFocusable(true);
//                } else {
//                    String mode = spinner.getSelectedItem().toString();
//                    String currentMode = mEventObserver.getShootMode();
//                    if (mode.isEmpty()) {
//                        toast(R.string.msg_error_no_supported_shootmode);
//                        // now state that can not be changed
//                        selectionShootModeSpinner(spinner, currentMode);
//                    } else {
//                        if ("IDLE".equals(mEventObserver.getCameraStatus())
//                                && !mode.equals(currentMode)) {
//                            setShootMode(mode);
//                        } else {
//                            // now state that can not be changed
//                            selectionShootModeSpinner(spinner, currentMode);
//                        }
//                    }
//                }
//            }
//            // not selected Spinner dropdown item
//            public void onNothingSelected(AdapterView<?> parent) {
//            }
//        });
//
    }

    // Prepare for Button to select "actZoom" by user.
    private void prepareActZoomButtons(final boolean flag) {
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "prepareActZoomButtons(): exec.");
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        prepareActZoomButtonsUi(flag);
                    }
                });
            };
        }.start();
    }

    // Prepare for ActZoom Button UI.
    private void prepareActZoomButtonsUi(boolean flag) {
        if (flag) {
            mButtonZoomOut.setVisibility(View.VISIBLE);
            mButtonZoomIn.setVisibility(View.VISIBLE);
        } else {
            mButtonZoomOut.setVisibility(View.GONE);
            mButtonZoomIn.setVisibility(View.GONE);
        }
    }
    
    
    private void setSelfTimer(final int TimeInSeconds, boolean inThreadContext) {
    	
    	if (inThreadContext)
    	{
    			new Thread() {
				
				@Override
				public void run() {
					sendSelfTimerRequest(TimeInSeconds);
				
				}}.start();
    		
    	}
    	else
    	{
    		sendSelfTimerRequest(TimeInSeconds);
    	}
    	
    }
    
    private void sendSelfTimerRequest(final int TimeInSeconds){

	try {
                JSONObject replyJson = mRemoteApi.setSelfTimer(TimeInSeconds);
                JSONArray resultsObj = replyJson.getJSONArray("result");
                int resultCode = resultsObj.getInt(0);
                if (resultCode == 0) {
                    // Success, but no refresh UI at the point.
                } else {
                    Log.w(TAG, "setSelfTimer: error: " + resultCode);
                    toast(R.string.msg_error_api_calling);
                }
            } catch (IOException e) {
                Log.w(TAG, "setSelfTimer: IOException: " + e.getMessage());
            } catch (JSONException e) {
                Log.w(TAG, "setSelfTimer: JSON format error.");
            }
    }
    
    private void setFlashMode(final EFlashMode mode) {

    	new Thread() {
            @Override
            public void run() {
                try {
                    JSONObject replyJson = mRemoteApi.setFlashMode(mode.toString());
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        // Success, but no refresh UI at the point.
                    } else {
                        Log.w(TAG, "setFlashMode: error: " + resultCode);
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "setFlashMode: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "setFlashMode: JSON format error.");
                }
            }
        }.start();
    }

    // Call setShootMode
    private void setShootMode(final String mode, boolean inThreadContext) {

    	if (inThreadContext)
    	{
    			new Thread() {
				
				@Override
				public void run() {
					sendShootModeRequest(mode);
				
				}}.start();
    		
    	}
    	else
    	{
    		sendShootModeRequest(mode);
    	}
    }

    
    void sendShootModeRequest(final String mode) {

        try {
            JSONObject replyJson = mRemoteApi.setShootMode(mode);
            JSONArray resultsObj = replyJson.getJSONArray("result");
            int resultCode = resultsObj.getInt(0);
            if (resultCode == 0) {
                // Success, but no refresh UI at the point.
            } else {
                Log.w(TAG, "setShootMode: error: " + resultCode);
                toast(R.string.msg_error_api_calling);
            }
            } catch (IOException e) {
                Log.w(TAG, "setShootMode: IOException: " + e.getMessage());
            } catch (JSONException e) {
                Log.w(TAG, "setShootMode: JSON format error.");
            }
 	}
    

    // Take a picture and retrieve the image data.
    private void takeAndFetchPicture() {
        if (!mLiveviewSurface.isStarted()) {
            toast(R.string.msg_error_take_picture);
            return;
        }

        new Thread() {

            @Override
            public void run() {
                try {
                	
                	
                    JSONObject replyJson = mRemoteApi.actTakePicture();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    JSONArray imageUrlsObj = resultsObj.getJSONArray(0);


                    // Few buttons were disabled from the onClick() of the take picture button 
                    // Enable them back
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            enableButtons_still();
                        }
                    });
                    
                    String postImageUrl = null;
                    if (1 <= imageUrlsObj.length()) {
                        postImageUrl = imageUrlsObj.getString(0);
                    }
                    if (postImageUrl == null) {
                        Log.w(TAG,
                                "takeAndFetchPicture: post image URL is null.");
                        toast(R.string.msg_error_take_picture);
                        return;
                    }
                    setProgressIndicator(true); // Show progress indicator
                    URL url = new URL(postImageUrl);
                    InputStream istream = new BufferedInputStream(
                            url.openStream());
                    
                    
            
                    
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // irresponsible value
                    
                    Bitmap bmp = BitmapFactory.decodeStream(istream, null, options);
                    
                    final Drawable pictureDrawable = new BitmapDrawable(
                            getResources(), bmp);
                    
                    
                    istream.close();

                    /**
                     *  Save image to file
                     */
                    savePhoto(bmp);
                    
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mImagePictureWipe.setVisibility(View.VISIBLE);
                            mImagePictureWipe.setImageDrawable(pictureDrawable);
                        
                        }
                    });
                    

                } catch (IOException e) {
                    Log.w(TAG, "IOException while closing slicer: " + e.getMessage());
                    toast(R.string.msg_error_take_picture);
                } catch (JSONException e) {
                    Log.w(TAG, "JSONException while closing slicer");
                    toast(R.string.msg_error_take_picture);
                } finally {
                    setProgressIndicator(false);
                }
                
                
            }
        }.start();
    }
    

    // Save bitmap to gallery
    private void savePhoto(Bitmap bmp){
    	
     	String appDirectoryName  = getResources().getString(R.string.save_to_folder_name);

		String file_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+ File.separator + appDirectoryName;
		File dir = new File(file_path);
		
		if(!dir.exists())
			dir.mkdirs();	

		// Image file
		File file = new File(dir, "IMG" + "_" +     System.currentTimeMillis() + ".jpg");
		FileOutputStream out = null;
		
		try
		{
		 out = new FileOutputStream(file);
		 bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
		 out.flush();
		 out.close();

		 // ???
		 MediaScannerConnection.scanFile(this, new String[] { file.getAbsolutePath() }, null, null);
		 
		 out = null;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
    }

    // Call startMovieRec
    private void startMovieRec() {
//        new Thread() {
//
//            @Override
//            public void run() {
    	

                try {
                    Log.d(TAG, "startMovieRec: exec.");
                    JSONObject replyJson = mRemoteApi.startMovieRec();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        	// In any case just erase the text
                        	mTextVideo.setText(""); }});

                    
                    // If recording successfully started
                    if (resultCode == 0) {
                    	runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            	
                            	mButtonRecStartStop.setEnabled(true);
                            	// Change the button image to stop button
                            	mButtonRecStartStop.setImageResource(R.drawable.mcp_stop);
                            }
                        });
                    	
                        toast(R.string.msg_rec_start);
                    } else {
                        Log.w(TAG, "startMovieRec: error: " + resultCode);
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "startMovieRec: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "startMovieRec: JSON format error.");
                }
//            }
//        }.start();
    }

    // Call stopMovieRec
    private void stopMovieRec() {
//        new Thread() {
//
//            @Override
//            public void run() {
                try {
                    Log.d(TAG, "stopMovieRec: exec.");
                    JSONObject replyJson = mRemoteApi.stopMovieRec();
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    String thumbnailUrl = resultsObj.getString(0);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        	mButtonRecStartStop.setImageResource(R.drawable.mcp_video);
                        }
                    });
                    
                    if (thumbnailUrl != null) {
                        toast(R.string.msg_rec_stop);
                    } else {
                        Log.w(TAG, "stopMovieRec: error");
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "stopMovieRec: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "stopMovieRec: JSON format error.");
                }
//            }
//        }.start();
    }

    // Call actZoom
    private void actZoom(final String direction, final String movement) {
        new Thread() {

            @Override
            public void run() {
                try {
                    JSONObject replyJson = mRemoteApi.actZoom(direction, movement);
                    JSONArray resultsObj = replyJson.getJSONArray("result");
                    int resultCode = resultsObj.getInt(0);
                    if (resultCode == 0) {
                        // Success, but no refresh UI at the point.
                    } else {
                        Log.w(TAG, "actZoom: error: " + resultCode);
                        toast(R.string.msg_error_api_calling);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "actZoom: IOException: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "actZoom: JSON format error.");
                }
            }
        }.start();
    }
    
    private void disableButtons_still(){
    	mButtonTakePicture.setEnabled(false);
    	mButtonRecStartStop.setEnabled(false);
    	mButtonSetTimer.setEnabled(false);
    	mButtonFlashMode.setEnabled(false);
    	
    }
    
    private void enableButtons_still(){
    	mButtonTakePicture.setEnabled(true);
    	mButtonRecStartStop.setEnabled(true);
    	mButtonSetTimer.setEnabled(true);
    	mButtonFlashMode.setEnabled(true);
    	
    }
    
    
    private void disableButtons_video(){
    	mButtonTakePicture.setEnabled(false);
    	mButtonSetTimer.setEnabled(false);
    	mButtonFlashMode.setEnabled(false);
    	mButtonRecStartStop.setEnabled(false);
    	
    }
    
    private void enableButtons_video(){
    	mButtonTakePicture.setEnabled(true);
    	mButtonSetTimer.setEnabled(true);
    	mButtonFlashMode.setEnabled(true);
    	mButtonRecStartStop.setEnabled(true);
    }
    

    // Show or hide progress indicator on title bar
    private void setProgressIndicator(final boolean visible) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(visible);
            }
        });
    }

    // show toast
    private void toast(final int msgId) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(WorkingModeActivity.this, msgId,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
