package com.withs.listentogether.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.widget.Toast;

import com.withs.listentogether.R;
import com.withs.listentogether.activity.MainActivity;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "WiFiDirectBroadcastReceiver";

	private WifiP2pManager mManager;
	private Channel mChannel;
	private MainActivity mActivity;

	public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       MainActivity activity) {
		super();
		this.mManager = manager;
		this.mChannel = channel;
		this.mActivity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();

		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

			if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
				mActivity.showWifiP2pDialog();
			}
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            mManager.requestPeers(mChannel, mActivity);
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
				.equals(action)) {
            //TODO
            /*mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null) {
                        WifiP2pGroup test = group;
                        WifiP2pDevice device = test.getOwner();

                        int a = 2;
                    }
                }
            });*/

            //TODO
            WifiP2pInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
            testInfo(info);
//            intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);

		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
				.equals(action)) {
			WifiP2pDevice mMyDevice = (WifiP2pDevice) intent
					.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
			mActivity.setMyWiFiP2pDevice(mMyDevice);
			mActivity.setMyDeviceStatus(mMyDevice);
		}

	}

    private void testInfo(WifiP2pInfo info) {
        mActivity.setWiFiP2pInfo(info);

        if (info.groupFormed && info.isGroupOwner) {
            mActivity.setIsGroupOwner(true);

            if (mActivity.serverThread == null) {
                mActivity.startServerThread();
            }

            Toast.makeText(mActivity,
                    R.string.main_toast_group_owner,
                    Toast.LENGTH_SHORT).show();

            mActivity.setHintText(R.string.main_hint_grouped);

        } else if (info.groupFormed) {
            if (mActivity.clientThread == null) {
                mActivity.startClientThread();
            }

            Toast.makeText(mActivity,
                    R.string.main_toast_group_member,
                    Toast.LENGTH_SHORT).show();

            mActivity.setHintText(R.string.main_hint_grouped);

        } else if (info.groupFormed == false) {
            mActivity.setIsGroupOwner(false);

            if (mActivity.serverThread != null) {
                mActivity.serverThread.interrupt();
                mActivity.serverThread = null;
            }

            if (mActivity.clientThread != null) {
                mActivity.clientThread.interrupt();
                mActivity.clientThread = null;
            }

            mActivity.instructionSocket.close();

            mActivity.showStartButton();

            mActivity.setHintText(R.string.main_hint_start);

        }

        mActivity.setGroupStatus();
    }

}
