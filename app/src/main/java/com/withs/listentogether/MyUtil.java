package com.withs.listentogether;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class MyUtil {

	public static void sendMessage(Messenger target, int what) {
		Message msg = Message.obtain(null, what);
		try {
			target.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static void sendMessage(Messenger target, int what, Object obj) {
		Message msg = Message.obtain(null, what, obj);
		try {
			target.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public static void sendMessage(Messenger target, int what, Messenger replyTo) {
		Message msg = Message.obtain(null, what);
		msg.replyTo = replyTo;
		try {
			target.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public static void clearWifiP2pConnection(WifiP2pManager manager, Channel channel) {
		ActionListener actionListener = new ActionListener() {

			@Override
			public void onSuccess() {
				// success
			}

			@Override
			public void onFailure(int reason) {
				//Log.d("clearWifiP2pConnection", "error : " + reason);
			}
		};

		manager.cancelConnect(channel, actionListener);
		manager.removeGroup(channel, actionListener);
	}

	public static Socket initSocket(Socket socket) {
		try {
			socket.setReuseAddress(true);
			socket.setKeepAlive(true);
			socket.setSoLinger(true, 0);
			socket.setSoTimeout(0);
			socket.setTcpNoDelay(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return socket;
	}

	public static Socket makeConnectedSocket(String host, int port) {
		Socket socket = null;

		while (true) {
			try {
				socket = initSocket(new Socket());
				socket.connect(new InetSocketAddress(host, port));
				break;
			} catch (IOException e) {
				socket = null;
			}
		}

		return socket;
	}
	
	public static String getDeviceStatus(int deviceStatus) {
		switch (deviceStatus) {
		case WifiP2pDevice.AVAILABLE:
			return "Available";
		case WifiP2pDevice.INVITED:
			return "Invited";
		case WifiP2pDevice.CONNECTED:
			return "Connected";
		case WifiP2pDevice.FAILED:
			return "Failed";
		case WifiP2pDevice.UNAVAILABLE:
			return "Unavailable";
		default:
			return "Unknown";
		}
	}
	
	public static int getScreenWidthPixels(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics.widthPixels;
	}

    public static void showShortToast(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showShortToast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }
}
