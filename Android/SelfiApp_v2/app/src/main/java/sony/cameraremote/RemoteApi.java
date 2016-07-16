/*
 * Copyright 2013 Sony Corporation
 */

package sony.cameraremote;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import sony.cameraremote.utils.SimpleHttpClient;

/**
 * Simple Camera Remote API wrapper class. (JSON based API <--> Java API)
 */
public class RemoteApi {

    private static final String TAG = RemoteApi.class.getSimpleName();

    // If you'd like to suppress detailed log output, change this value into
    // false.
    private static final boolean FULL_LOG = true;

    // API server device you want to send requests.
    private ServerDevice mTargetServer;

    // Request ID of API calling. This will be counted up by each API calling.
    private int mRequestId;

    /**
     * Constructor.
     * 
     * @param target server device of Remote API
     */
    public RemoteApi(ServerDevice target) {
        mTargetServer = target;
        mRequestId = 1;
    }

    // Retrieves Action List URL from Server information.
    private String findActionListUrl(String service) {
        List<ServerDevice.ApiService> services = mTargetServer.getApiServices();
        for (ServerDevice.ApiService apiService : services) {
            if (apiService.getName().equals(service)) {
                return apiService.getActionListUrl();
            }
        }
        throw new IllegalStateException("actionUrl not found.");
    }

    // Request ID. Counted up after calling.
    private int id() {
        return mRequestId++;
    }

    // Output a log line.
    private void log(String msg) {
        if (FULL_LOG) {
            Log.d(TAG, msg);
        }
    }

    // Camera Service APIs

    /**
     * Calls getAvailableApiList API to the target server. Request JSON data is
     * such like as below.
     * 
     * <pre>
     * {
     *   "method": "getAvailableApiList",
     *   "params": [""],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject getAvailableApiList() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "getAvailableApiList")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls getApplicationInfo API to the target server. Request JSON data is
     * such like as below.
     * 
     * <pre>
     * {
     *   "method": "getApplicationInfo",
     *   "params": [""],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject getApplicationInfo() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "getApplicationInfo")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls getShootMode API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "getShootMode",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject getShootMode() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "getShootMode")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls setShootMode API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "setShootMode",
     *   "params": ["still"],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @param shootMode shoot mode (ex. "still")
     * @return JSON data of response
     */
    public JSONObject setShootMode(String shootMode) throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "setShootMode")
                    .put("params", new JSONArray().put(shootMode))
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls getAvailableShootMode API to the target server. Request JSON data
     * is such like as below.
     * 
     * <pre>
     * {
     *   "method": "getAvailableShootMode",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject getAvailableShootMode() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "getAvailableShootMode")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("getAvailableShootMode Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("getAvailableShootMode Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls getSupportedShootMode API to the target server. Request JSON data
     * is such like as below.
     * 
     * <pre>
     * {
     *   "method": "getSupportedShootMode",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject getSupportedShootMode() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "getSupportedShootMode")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls startLiveview API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "startLiveview",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject startLiveview() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "startLiveview")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls stopLiveview API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "stopLiveview",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject stopLiveview() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "stopLiveview")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls startRecMode API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "startRecMode",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject startRecMode() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "startRecMode")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls stopRecMode API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "stopRecMode",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject stopRecMode() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "stopRecMode")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls actTakePicture API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "actTakePicture",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject actTakePicture() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "actTakePicture")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls startMovieRec API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "startMovieRec",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject startMovieRec() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "startMovieRec")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls stopMovieRec API to the target server. Request JSON data is such
     * like as below.
     * 
     * <pre>
     * {
     *   "method": "stopMovieRec",
     *   "params": [],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject stopMovieRec() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "stopMovieRec")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls actZoom API to the target server. Request JSON data is such like as
     * below.
     * 
     * <pre>
     * {
     *   "method": "actZoom",
     *   "params": ["in","stop"],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @return JSON data of response
     */
    public JSONObject actZoom(String direction, String movement) throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "actZoom")
                    .put("params", new JSONArray().put(direction).put(movement))
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    /**
     * Calls getEvent API to the target server. Request JSON data is such like
     * as below.
     * 
     * <pre>
     * {
     *   "method": "getEvent",
     *   "params": [true],
     *   "id": 2,
     *   "version": "1.0"
     * }
     * </pre>
     * 
     * @param longPollingFlag true means long polling request.
     * @return JSON data of response
     */
    public JSONObject getEvent(boolean longPollingFlag) throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject().put("method", "getEvent")
                    .put("params", new JSONArray().put(longPollingFlag))
                    .put("id", id()).put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;
            int longPollingTimeout = (longPollingFlag) ? 20000 : 8000; // msec

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString(), longPollingTimeout);
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }
    

    public JSONObject setFlashMode(String strMode) throws IOException {
        String service = "camera";
        
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "setFlashMode")
                    .put("params", new JSONArray().put(strMode))
                    .put("id", 1)
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    
    
    public JSONObject setSelfTimer(int timeInSeconds) throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "setSelfTimer")
                    .put("params", new JSONArray().put(timeInSeconds))
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    
    public JSONObject getSelfTimer() throws IOException {
        String service = "camera";
        try {
            JSONObject requestJson = new JSONObject()
                    .put("method", "setSelfTimer")
                    .put("params", new JSONArray())
                    .put("id", id())
                    .put("version", "1.0");
            String url = findActionListUrl(service) + "/" + service;

            log("Request:  " + requestJson.toString());
            String responseJson = SimpleHttpClient.httpPost(url,
                    requestJson.toString());
            log("Response: " + responseJson);
            return new JSONObject(responseJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    
}
