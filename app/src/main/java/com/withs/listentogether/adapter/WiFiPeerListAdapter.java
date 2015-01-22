package com.withs.listentogether.adapter;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.withs.listentogether.R;
import com.withs.listentogether.MyUtil;

import java.util.List;

public class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

	List<WifiP2pDevice> items;

	public WiFiPeerListAdapter(Context context, int textViewResourceId,
			List<WifiP2pDevice> objects) {
		super(context, textViewResourceId, objects);
		items = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;

		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			 v = vi.inflate(R.layout.row_devices, parent, false);
		}

		WifiP2pDevice device = items.get(position);

		if (device != null) {
			TextView top = (TextView) v.findViewById(R.id.device_name);
			TextView bottom = (TextView) v.findViewById(R.id.device_details);
			if (top != null) {
				top.setText(device.deviceName);
			}
			if (bottom != null) {
				bottom.setText(MyUtil.getDeviceStatus(device.status));
			}
		}

		return v;

	}

}
