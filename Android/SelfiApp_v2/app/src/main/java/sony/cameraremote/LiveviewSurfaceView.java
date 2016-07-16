/*
 * Copyright 2013 Sony Corporation
 */

package sony.cameraremote;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import sony.cameraremote.utils.SimpleLiveviewSlicer;

/**
 * A SurfaceView based class to draw liveview frames serially.
 */
public class LiveviewSurfaceView extends SurfaceView implements
        SurfaceHolder.Callback {

    private static final String TAG = LiveviewSurfaceView.class
            .getSimpleName();

    private RemoteApi mRemoteApi;
    private boolean mWhileFetching;
    private final BlockingQueue<byte[]> mJpegQueue = new ArrayBlockingQueue<byte[]>(2);
    private final boolean mInMutableAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    private Thread mDrawerThread;
    private int mPreviousWidth = 0;
    private int mPreviousHeight = 0;
    private final Paint mFramePaint;

    /**
     * Contractor
     * 
     * @param context
     */
    public LiveviewSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
    }

    /**
     * Contractor
     * 
     * @param context
     * @param attrs
     */
    public LiveviewSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
    }

    /**
     * Contractor
     * 
     * @param context
     * @param attrs
     * @param defStyle
     */
    public LiveviewSurfaceView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // do nothing.
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mWhileFetching = false;
    }

    /**
     * Bind a Remote API object to communicate with Camera device. Need to call
     * this method before calling start() method.
     * 
     * @param remoteApi
     */
    public void bindRemoteApi(RemoteApi remoteApi) {
        mRemoteApi = remoteApi;
    }

    /**
     * Start retrieving and drawing liveview frame data by new threads.
     * 
     * @return true if the starting is completed successfully, false otherwise.
     * @exception IllegalStateException when Remote API object is not set.
     * @see LiveviewSurfaceView#bindRemoteApi(RemoteApi)
     */
    public boolean start() {
        if (mRemoteApi == null) {
            throw new IllegalStateException("RemoteApi is not set.");
        }
        if (mWhileFetching) {
            Log.w(TAG, "start() already starting.");
            return false;
        }

        mWhileFetching = true;

        // A thread for retrieving liveview data from server.
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Starting retrieving liveview data from server.");
                SimpleLiveviewSlicer slicer = null;

                try {
                    // Prepare for connecting.
                    JSONObject replyJson = null;

                    replyJson = mRemoteApi.startLiveview();
                    if (!isErrorReply(replyJson)) {
                        JSONArray resultsObj = replyJson.getJSONArray("result");
                        String liveviewUrl = null;
                        if (1 <= resultsObj.length()) {
                            // Obtain liveview URL from the result.
                            liveviewUrl = resultsObj.getString(0);
                        }
                        if (liveviewUrl != null) {
                            // Create Slicer to open the stream and parse it.
                            slicer = new SimpleLiveviewSlicer();
                            slicer.open(liveviewUrl);
                        }
                    }

                    if (slicer == null) {
                        mWhileFetching = false;
                        return;
                    }

                    while (mWhileFetching) {
                        final SimpleLiveviewSlicer.Payload payload = slicer.nextPayload();
                        if (payload == null) { // never occurs
                            Log.e(TAG, "Liveview Payload is null.");
                            continue;
                        }

                        if (mJpegQueue.size() == 2) {
                            mJpegQueue.remove();
                        }
                        mJpegQueue.add(payload.jpegData);
                    }
                } catch (IOException e) {
                    Log.w(TAG, "IOException while fetching: " + e.getMessage());
                } catch (JSONException e) {
                    Log.w(TAG, "JSONException while fetching");
                } finally {
                    // Finalize
                    try {
                        if (slicer != null) {
                            slicer.close();
                        }
                        mRemoteApi.stopLiveview();
                    } catch (IOException e) {
                        Log.w(TAG, "IOException while closing slicer: " + e.getMessage());
                    }

                    if (mDrawerThread != null) {
                        mDrawerThread.interrupt();
                    }

                    mJpegQueue.clear();
                    mWhileFetching = false;
                }
            }
        }.start();

        // A thread for drawing liveview frame fetched by above thread.
        mDrawerThread = new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Starting drawing liveview frame.");
                Bitmap frameBitmap = null;

                BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
                factoryOptions.inSampleSize = 1;
                if (mInMutableAvailable) {
                    initInBitmap(factoryOptions);
                }

                while (mWhileFetching) {
                    try {
                        byte[] jpegData = mJpegQueue.take();
                        frameBitmap = BitmapFactory.decodeByteArray(
                                jpegData, 0,
                                jpegData.length, factoryOptions);
                    } catch (IllegalArgumentException e) {
                        if (mInMutableAvailable) {
                            clearInBitmap(factoryOptions);
                        }
                        continue;
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Drawer thread is Interrupted.");
                        break;
                    }

                    if (mInMutableAvailable) {
                        setInBitmap(factoryOptions, frameBitmap);
                    }
                    drawFrame(frameBitmap);
                }

                if (frameBitmap != null) {
                    frameBitmap.recycle();
                }
                mWhileFetching = false;
            }
        };
        mDrawerThread.start();
        return true;
    }

    /**
     * Request to stop retrieving and drawing liveview data.
     */
    public void stop() {
        mWhileFetching = false;
    }

    /**
     * Check to see whether start() is already called.
     * 
     * @return true if start() is already called, false otherwise.
     */
    public boolean isStarted() {
        return mWhileFetching;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initInBitmap(BitmapFactory.Options options) {
        options.inBitmap = null;
        options.inMutable = true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void clearInBitmap(BitmapFactory.Options options) {
        if (options.inBitmap != null) {
            options.inBitmap.recycle();
            options.inBitmap = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setInBitmap(BitmapFactory.Options options, Bitmap bitmap) {
        options.inBitmap = bitmap;
    }

    // Draw frame bitmap onto a canvas.
    private void drawFrame(Bitmap frame) {
        if (frame.getWidth() != mPreviousWidth
                || frame.getHeight() != mPreviousHeight) {
            onDetectedFrameSizeChanged(frame.getWidth(), frame.getHeight());
            return;
        }
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }
        int w = frame.getWidth();
        int h = frame.getHeight();
        Rect src = new Rect(0, 0, w, h);

        float by = Math
                .min((float) getWidth() / w, (float) getHeight() / h);
        int offsetX = (getWidth() - (int) (w * by)) / 2;
        int offsetY = (getHeight() - (int) (h * by)) / 2;
        Rect dst = new Rect(offsetX, offsetY, getWidth() - offsetX,
                getHeight() - offsetY);
        canvas.drawBitmap(frame, src, dst, mFramePaint);
        getHolder().unlockCanvasAndPost(canvas);
    }

    // Called when the width or height of liveview frame image is changed.
    private void onDetectedFrameSizeChanged(int width, int height) {
        Log.d(TAG, "Change of aspect ratio detected");
        mPreviousWidth = width;
        mPreviousHeight = height;
        drawBlackFrame();
        drawBlackFrame();
        drawBlackFrame(); // delete triple buffers
    }

    // Draw black screen.
    private void drawBlackFrame() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(new Rect(0, 0, getWidth(), getHeight()), paint);
        getHolder().unlockCanvasAndPost(canvas);
    }

    // Parse JSON and returns a error code.
    private static boolean isErrorReply(JSONObject replyJson) {
        boolean hasError = (replyJson != null && replyJson.has("error"));
        return hasError;
    }
}
