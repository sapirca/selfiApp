/*
 * Copyright 2013 Sony Corporation
 */

package sony.cameraremote;

import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * A SSDP client class for this sample application. This implementation keeps
 * simple so that many developers understand quickly.
 */
public class SsdpClient {

    private static final String TAG = SsdpClient.class.getSimpleName();

    private final static int SSDP_RECEIVE_TIMEOUT = 10000; // msec
    private final static int PACKET_BUFFER_SIZE = 1024;
    private final static int SSDP_PORT = 1900;
    private final static int SSDP_MX = 1;
    private final static String SSDP_ADDR = "239.255.255.250";
    private final static String SSDP_ST = "urn:schemas-sony-com:service:ScalarWebAPI:1";

    /** Handler interface for SSDP search result. */
    public interface SearchResultHandler {

        /**
         * Called when API server device is found. Note that it's performed by
         * non-UI thread.
         * 
         * @param device API server device that is found by searching
         */
        public void onDeviceFound(ServerDevice device);

        /**
         * Called when searching completes successfully. Note that it's
         * performed by non-UI thread.
         */
        public void onFinished();

        /**
         * Called when searching completes with some errors. Note that it's
         * performed by non-UI thread.
         */
        public void onErrorFinished();
    }

    private boolean mSearching = false;

    /**
     * Search API server device.
     * 
     * @param handler result handler
     * @return true: start successfully, false: already searching now
     */
    public synchronized boolean search(final SearchResultHandler handler) {
        if (mSearching) {
            Log.w(TAG, "search() already searching.");
            return false;
        }
        if (handler == null) {
            throw new NullPointerException("handler is null.");
        }
        Log.i(TAG, "search() Start.");

        final String ssdpRequest = "M-SEARCH * HTTP/1.1\r\n"
                + String.format("HOST: %s:%d\r\n", SSDP_ADDR, SSDP_PORT)
                + String.format("MAN: \"ssdp:discover\"\r\n")
                + String.format("MX: %d\r\n", SSDP_MX)
                + String.format("ST: %s\r\n", SSDP_ST) + "\r\n";
        final byte[] sendData = ssdpRequest.getBytes();

        new Thread() {

            @Override
            public void run() {
                // Send Datagram packets
                DatagramSocket socket = null;
                DatagramPacket receivePacket = null;
                DatagramPacket packet = null;
                try {
                    socket = new DatagramSocket();
                    InetSocketAddress iAddress = new InetSocketAddress(
                            SSDP_ADDR, SSDP_PORT);
                    packet = new DatagramPacket(sendData, sendData.length,
                            iAddress);
                    // send 3 times
                    Log.i(TAG, "search() Send Datagram packet 3 times.");
                    socket.send(packet);
                    Thread.sleep(100);
                    socket.send(packet);
                    Thread.sleep(100);
                    socket.send(packet);
                } catch (InterruptedException e) {
                    // do nothing.
                } catch (SocketException e) {
                    Log.e(TAG, "search() DatagramSocket error:", e);
                    handler.onErrorFinished();
                } catch (IOException e) {
                    Log.e(TAG, "search() IOException:", e);
                    handler.onErrorFinished();
                }

                if (socket == null) {
                    return;
                }

                // Receive reply packets
                mSearching = true;
                long startTime = System.currentTimeMillis();
                List<String> foundDevices = new ArrayList<String>();
                byte[] array = new byte[PACKET_BUFFER_SIZE];
                while (mSearching) {
                    receivePacket = new DatagramPacket(array, array.length);
                    try {
                        socket.setSoTimeout(SSDP_RECEIVE_TIMEOUT);
                        socket.receive(receivePacket);
                        String ssdpReplyMessage = new String(
                                receivePacket.getData(), 0,
                                receivePacket.getLength());
                        String ddUsn = findParameterValue(ssdpReplyMessage,
                                "USN");

                        /*
                         * There is possibility to receive multiple packets from
                         * a individual server.
                         */
                        if (!foundDevices.contains(ddUsn)) {
                            String ddLocation = findParameterValue(
                                    ssdpReplyMessage, "LOCATION");
                            foundDevices.add(ddUsn);

                            // Fetch Device Description XML and parse it.
                            ServerDevice device = ServerDevice
                                    .fetch(ddLocation);
                            // Note that it's a irresponsible rule
                            //   for the sample application.
                            if (device != null
                                    && device.hasApiService("camera")) {
                                handler.onDeviceFound(device);
                            }
                        }
                    } catch (InterruptedIOException e) {
                        Log.d(TAG, "search() Timeout.");
                        break;
                    } catch (IOException e) {
                        Log.d(TAG, "search() IOException.");
                        handler.onErrorFinished();
                        return;
                    }
                    if (SSDP_RECEIVE_TIMEOUT < System.currentTimeMillis() - startTime) {
                        break;
                    }
                }
                mSearching = false;
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                handler.onFinished();
            };
        }.start();

        return true;
    }

    /**
     * Checks whether searching is in progress or not.
     * 
     * @return true: now searching, false: otherwise
     */
    public boolean isSearching() {
        return mSearching;
    }

    /**
     * Cancels searching. Note that it cannot stop the operation immediately.
     * 
     * @return true: now searching, false: otherwise
     */
    public void cancelSearching() {
        mSearching = false;
    }

    /*
     * Find a value string from message line as below. (ex.)
     * "ST: XXXXX-YYYYY-ZZZZZ" -> "XXXXX-YYYYY-ZZZZZ"
     */
    private static String findParameterValue(String ssdpMessage,
            String paramName) {
        String name = paramName;
        if (!name.endsWith(":")) {
            name = name + ":";
        }
        int start = ssdpMessage.indexOf(name);
        int end = ssdpMessage.indexOf("\r\n", start);
        if (start != -1 && end != -1) {
            start += name.length();
            String val = ssdpMessage.substring(start, end);
            if (val != null) {
                return val.trim();
            }
        }
        return null;
    }
}
