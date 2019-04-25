package com.space.remotemusic;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by licht on 2019/3/1.
 */

public class TcpNetUtil {

    private static final String TAG = TcpNetUtil.class.getName();
    private static Socket sSocket;
    private static OutputStream sOutputStream;
    private static InputStream sInputStream;
    private static String sIp;
    private static int sPort;
    private static byte[] buffer;
    private static byte[] sBuff;

    public static void connectTcp(String ip, int port) {
        try {
            if (sSocket == null) {
                synchronized (TcpNetUtil.class) {
                    if (sSocket == null) {
                        sSocket = new Socket(ip, port);
                        sIp = ip;
                        sPort = port;
                        Log.e(TAG, "connectTcp: ");
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void listenData(byte[] bytes, ReceiverDataListener listener) {
        if (sSocket == null) {
            return;
        }
        if (isServerClose()) {
            Log.e(TAG, "listenData: =========");
        }
        int len = 0;
        try {
            if (sInputStream == null) {
                sInputStream = sSocket.getInputStream();
            }
            while ((len = sInputStream.read(bytes)) > 0) {
                listener.receiverSuccess(bytes);
                Log.e(TAG, "listenData: " + bytes.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            closeSocket();
            connectTcp(sIp, sPort);
            listener.receiverFail();
            listener = null;
        }
    }


    public static void sendData(byte[] bytes, SendDataListener listener) {
//        byte[] buff = null;
        if (sSocket == null) {
            connectTcp(sIp, sPort);
        }
//        buff = new byte[4 + bytes.length];
//        int headbyte = bytes.length;
//        byte[] int2Bytes = int2Bytes(headbyte);
//        System.arraycopy(int2Bytes, 0, buff, 0, int2Bytes.length);
//        System.arraycopy(bytes, 0, buff, int2Bytes.length, bytes.length);
        try {
            if (sOutputStream == null) {
                sOutputStream = sSocket.getOutputStream();
            }

//            Log.e(TAG, "sendData: " + new String(buff, 0, buff.length));
            Log.e(TAG, "sendData: " + new String(bytes));

//            sOutputStream.write(buff);
            sOutputStream.write(bytes);
            sOutputStream.flush();
//            listener.sendSuccess(buff);
            listener.sendSuccess(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            closeSocket();
//            listener.sendFail(buff);
            listener.sendFail(bytes);
        } catch (NullPointerException ne) {
            ne.printStackTrace();
            closeSocket();
//            listener.sendFail(buff);
            listener.sendFail(bytes);
        }
    }

    public static byte[] int2Bytes(int integer) {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) ((integer >> 24) & 0xff);
        bytes[1] = (byte) (integer >> 16 & 0xff);
        bytes[2] = (byte) (integer >> 8 & 0xff);
        bytes[3] = (byte) (integer & 0xff);
        return bytes;
    }

    private static boolean isServerClose() {
        if (sSocket == null) {
            return true;
        }
        try {
            sSocket.sendUrgentData(0xff);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    public static void closeSocket() {
        try {
            if (sInputStream != null) {
                sInputStream.close();
            }
            if (sOutputStream != null) {
                sOutputStream.close();
            }
            if (sSocket != null) {
                sSocket.close();
            }
            sInputStream = null;
            sOutputStream = null;
            sSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface ReceiverDataListener {
        void receiverSuccess(byte[] bytes);

        void receiverFail();
    }

    public interface SendDataListener {
        void sendSuccess(byte[] bytes);

        void sendFail(byte[] bytes);
    }
}
