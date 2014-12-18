package com.withs.listentogether.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;

import com.withs.listentogether.activity.PlaybackActivity;

public class PlaybackReceiver extends BroadcastReceiver {
	
	private PlaybackActivity mActivity;
	
	public PlaybackReceiver(PlaybackActivity activity) {
		super();
		mActivity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
		
			int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			
			if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
				
				mActivity.sendQuitMessage();
				mActivity.quit();
				
			}
			
		}
		
	}

}
