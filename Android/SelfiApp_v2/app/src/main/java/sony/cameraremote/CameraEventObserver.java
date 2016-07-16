/*
 * Copyright 2013 Sony Corporation
 */

package sony.cameraremote;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple observer class for some status values in Camera. This class supports
 * only a few of values of getEvent result, so please add implementation for the
 * rest of values you want to handle.
 */
public class CameraEventObserver {

    private static final String TAG = CameraEventObserver.class
            .getSimpleName();

    /**
     * A listener interface to receive these changes. These methods will be
     * called by UI thread.
     */
    public interface ChangeListener {

        /**
         * Called when the list of available APIs is modified.
         * 
         * @param apis a list of available APIs
         */
        void onApiListModified(List<String> apis);

        /**
         * Called when the value of "Camera Status" is changed.
         * 
         * @param status camera status (ex."IDLE")
         */
        void onCameraStatusChanged(String status);

        /**
         * Called when the value of "Liveview Status" is changed.
         * 
         * @param status liveview status (ex.true)
         */
        void onLiveviewStatusChanged(boolean status);

        /**
         * Called when the value of "Shoot Mode" is changed.
         * 
         * @param shootMode shoot mode (ex."still")
         */
        void onShootModeChanged(String shootMode);

        /**
         * Called when the value of "zoomPosition" is changed.
         * 
         * @param zoomPosition zoom position (ex.12)
         */
        void onZoomPositionChanged(int zoomPosition);

        // :
        // : add methods for Event data as necessary.
    }

    private Activity mActivity;
    private RemoteApi mRemoteApi;
    private ChangeListener mListener;
    private boolean mWhileEventMonitoring = false;

    // Current Camera Status value.
    private String mCameraStatus;

    // Current Liveview Status value.
    private boolean mLiveviewStatus;

    // Current Shoot Mode value.
    private String mShootMode;

    // Current Zoom Position value.
    private int mZoomPosition;

    // :
    // : add attributes for Event data as necessary.

    /**
     * Constructor.
     * 
     * @param context context to notify the changes by UI thread.
     * @param apiClient API client
     */
    public CameraEventObserver(Context context, RemoteApi apiClient) {
        if (context == null ) {
            throw new IllegalArgumentException("context is null.");
        }
        if (apiClient == null) {
            throw new IllegalArgumentException("apiClient is null.");
        }
        mActivity = (Activity)context;
        mRemoteApi = apiClient;
    }

    /**
     * Starts monitoring by continuously calling getEvent API.
     * 
     * @return true if it successfully started, false if a monitoring is already
     *         started.
     */
    public boolean start() {
        if (mWhileEventMonitoring) {
            Log.w(TAG, "start() already starting.");
            return false;
        }

        mWhileEventMonitoring = true;
        new Thread() {

            @Override
            public void run() {
                Log.d(TAG, "start() exec.");
                // Call getEvent API continuously.
                boolean firstCall = true;
                MONITORLOOP: while (mWhileEventMonitoring) {

                    // At first, call as non-Long Polling.
                    boolean longPolling = firstCall ? false : true;

                    try {
                        // Call getEvent API.
                        JSONObject replyJson = mRemoteApi.getEvent(longPolling);

                        // Check error code at first.
                        int errorCode = findErrorCode(replyJson);
                        Log.d(TAG, "getEvent errorCode: " + errorCode);
                        switch (errorCode) {
                            case 0: // no error
                                // Pass through.
                                break;
                            case 1: // "Any" error
                            case 12: // "No such method" error
                                break MONITORLOOP; // end monitoring.
                            case 2: // "Timeout" error
                                // Re-call immediately.
                                continue MONITORLOOP;
                            case 40402: // "Already polling" error
                                // Retry after 5 sec.
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    // do nothing.
                                }
                                continue MONITORLOOP;
                            default:
                                Log.w(TAG,
                                        "CameraEventObserver: Unexpected error: "
                                                + errorCode);
                                break MONITORLOOP; // end monitoring.
                        }

                        List<String> availableApis = findAvailableApiList(replyJson);
                        if (!availableApis.isEmpty()) {
                            fireApiListModifiedListener(availableApis);
                        }

                        // CameraStatus
                        String cameraStatus = findCameraStatus(replyJson);
                        Log.d(TAG, "getEvent cameraStatus: " + cameraStatus);
                        if (cameraStatus != null
                                && !cameraStatus.equals(mCameraStatus)) {
                            mCameraStatus = cameraStatus;
                            fireCameraStatusChangeListener(cameraStatus);
                        }

                        // LiveviewStatus
                        Boolean liveviewStatus = findLiveviewStatus(replyJson);
                        Log.d(TAG, "getEvent liveviewStatus: " + liveviewStatus);
                        if (liveviewStatus != null
                                && !liveviewStatus.equals(mLiveviewStatus)) {
                            mLiveviewStatus = liveviewStatus;
                            fireLiveviewStatusChangeListener(liveviewStatus);
                        }

                        // ShootMode
                        String shootMode = findShootMode(replyJson);
                        Log.d(TAG, "getEvent shootMode: " + shootMode);
                        if (shootMode != null && !shootMode.equals(mShootMode)) {
                            mShootMode = shootMode;
                            fireShootModeChangeListener(shootMode);
                        }

                        // zoomPosition
                        int zoomPosition = findZoomInformation(replyJson);
                        Log.d(TAG, "getEvent zoomPosition: " + zoomPosition);
                        if (zoomPosition != -1) {
                            mZoomPosition = zoomPosition;
                            fireZoomInformationChangeListener(0, 0, zoomPosition, 0);
                        }

                        // :
                        // : add implementation for Event data as necessary.

                    } catch (IOException e) {
                        // Occurs when the server is not available now.
                        Log.d(TAG, "getEvent timeout by client trigger.");
                        break MONITORLOOP;
                    } catch (JSONException e) {
                        Log.w(TAG,
                                "getEvent: JSON format error. "
                                        + e.getMessage());
                        break MONITORLOOP;
                    }

                    firstCall = false;
                } // MONITORLOOP end.

                mWhileEventMonitoring = false;
            }
        }.start();

        return true;
    }

    /**
     * Requests to stop the monitoring.
     */
    public void stop() {
        mWhileEventMonitoring = false;
    }

    /**
     * Checks to see whether a monitoring is already started.
     * 
     * @return true when monitoring is started.
     */
    public boolean isStarted() {
        return mWhileEventMonitoring;
    }

    /**
     * Sets a listener object.
     * 
     * @param listener
     */
    public void setEventChangeListener(ChangeListener listener) {
        mListener = listener;
    }

    /**
     * Clears a listener object.
     */
    public void clearEventChangeListener() {
        mListener = null;
    }

    /**
     * Returns the current Camera Status value.
     * 
     * @return camera status
     */
    public String getCameraStatus() {
        return mCameraStatus;
    }

    /**
     * Returns the current Camera Status value.
     * 
     * @return camera status
     */
    public boolean getLiveviewStatus() {
        return mLiveviewStatus;
    }

    /**
     * Returns the current Shoot Mode value.
     * 
     * @return shoot mode
     */
    public String getShootMode() {
        return mShootMode;
    }

    /**
     * Returns the current Zoom Position value.
     * 
     * @return zoom position
     */
    public int getZoomPosition() {
        return mZoomPosition;
    }

    // Notifies the listener of available APIs change.
    private void fireApiListModifiedListener(final List<String> availableApis) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onApiListModified(availableApis);
                }
            }
        });
    }

    // Notifies the listener of Camera Status change.
    private void fireCameraStatusChangeListener(final String status) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onCameraStatusChanged(status);
                }
            }
        });
    }

    // Notifies the listener of Liveview Status change.
    private void fireLiveviewStatusChangeListener(final boolean status) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onLiveviewStatusChanged(status);
                }
            }
        });
    }

    // Notifies the listener of Shoot Mode change.
    private void fireShootModeChangeListener(final String shootMode) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onShootModeChanged(shootMode);
                }
            }
        });
    }

    // Notifies the listener of Zoom Information change.
    private void fireZoomInformationChangeListener(final int zoomIndexCurrentBox,
            final int zoomNumberBox, final int zoomPosition, final int zoomPositionCurrentBox) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onZoomPositionChanged(zoomPosition);
                }
            }
        });
    }

    // Finds and extracts an error code from reply JSON data.
    private static int findErrorCode(JSONObject replyJson) throws JSONException {
        int code = 0; // 0 means no error.
        if (replyJson.has("error")) {
            JSONArray errorObj = replyJson.getJSONArray("error");
            code = errorObj.getInt(0);
        }
        return code;
    }

    // Finds and extracts a list of available APIs from reply JSON data.
    // As for getEvent v1.0, results[0] => "availableApiList"
    private static List<String> findAvailableApiList(JSONObject replyJson)
            throws JSONException {
        List<String> availableApis = new ArrayList<String>();
        int indexOfAvailableApiList = 0;
        JSONArray resultsObj = replyJson.getJSONArray("result");
        if (!resultsObj.isNull(indexOfAvailableApiList)) {
            JSONObject availableApiListObj = resultsObj
                    .getJSONObject(indexOfAvailableApiList);
            String type = availableApiListObj.getString("type");
            if ("availableApiList".equals(type)) {
                JSONArray apiArray = availableApiListObj.getJSONArray("names");
                for (int i = 0; i < apiArray.length(); i++) {
                    availableApis.add(apiArray.getString(i));
                }
            } else {
                Log.w(TAG, "Event reply: Illegal Index (0: AvailableApiList) "
                        + type);
            }
        }
        return availableApis;
    }

    // Finds and extracts a value of Camera Status from reply JSON data.
    // As for getEvent v1.0, results[1] => "cameraStatus"
    private static String findCameraStatus(JSONObject replyJson)
            throws JSONException {
        String cameraStatus = null;
        int indexOfCameraStatus = 1;
        JSONArray resultsObj = replyJson.getJSONArray("result");
        if (!resultsObj.isNull(indexOfCameraStatus)) {
            JSONObject cameraStatusObj = resultsObj
                    .getJSONObject(indexOfCameraStatus);
            String type = cameraStatusObj.getString("type");
            if ("cameraStatus".equals(type)) {
                cameraStatus = cameraStatusObj.getString("cameraStatus");
            } else {
                Log.w(TAG, "Event reply: Illegal Index (1: CameraStatus) "
                        + type);
            }
        }
        return cameraStatus;
    }

    // Finds and extracts a value of Liveview Status from reply JSON data.
    // As for getEvent v1.0, results[3] => "liveviewStatus"
    private static Boolean findLiveviewStatus(JSONObject replyJson)
            throws JSONException {
        Boolean liveviewStatus = null;
        int indexOfLiveviewStatus = 3;
        JSONArray resultsObj = replyJson.getJSONArray("result");
        if (!resultsObj.isNull(indexOfLiveviewStatus)) {
            JSONObject liveviewStatusObj = resultsObj
                    .getJSONObject(indexOfLiveviewStatus);
            String type = liveviewStatusObj.getString("type");
            if ("liveviewStatus".equals(type)) {
                liveviewStatus = liveviewStatusObj.getBoolean("liveviewStatus");
            } else {
                Log.w(TAG, "Event reply: Illegal Index (3: LiveviewStatus) "
                        + type);
            }
        }
        return liveviewStatus;
    }

    // Finds and extracts a value of Shoot Mode from reply JSON data.
    // As for getEvent v1.0, results[21] => "shootMode"
    private static String findShootMode(JSONObject replyJson)
            throws JSONException {
        String shootMode = null;
        int indexOfShootMode = 21;
        JSONArray resultsObj = replyJson.getJSONArray("result");
        if (!resultsObj.isNull(indexOfShootMode)) {
            JSONObject shootModeObj = resultsObj
                    .getJSONObject(indexOfShootMode);
            String type = shootModeObj.getString("type");
            if ("shootMode".equals(type)) {
                shootMode = shootModeObj.getString("currentShootMode");
            } else {
                Log.w(TAG, "Event reply: Illegal Index (21: ShootMode) " + type);
            }
        }
        return shootMode;
    }

    // Finds and extracts a value of Zoom Information from reply JSON data.
    // As for getEvent v1.0, results[2] => "zoomInformation"
    private static int findZoomInformation(JSONObject replyJson)
            throws JSONException {
        int zoomPosition = -1;
        int indexOfZoomInformation = 2;
        JSONArray resultsObj = replyJson.getJSONArray("result");
        if (!resultsObj.isNull(indexOfZoomInformation)) {
            JSONObject zoomInformationObj = resultsObj
                    .getJSONObject(indexOfZoomInformation);
            String type = zoomInformationObj.getString("type");
            if ("zoomInformation".equals(type)) {
                zoomPosition = zoomInformationObj.getInt("zoomPosition");
            } else {
                Log.w(TAG, "Event reply: Illegal Index (2: zoomInformation) " + type);
            }
        }
        return zoomPosition;
    }

}
