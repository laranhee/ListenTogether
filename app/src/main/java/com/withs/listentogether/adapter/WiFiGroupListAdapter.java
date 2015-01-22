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
import com.withs.listentogether.activity.MainActivity;

import java.util.List;

public class WiFiGroupListAdapter extends ArrayAdapter<WifiP2pDevice> {

	private List<WifiP2pDevice> items;
	private MainActivity mActivity;

	public WiFiGroupListAdapter(Context context, int textViewResourceId,
			List<WifiP2pDevice> objects, MainActivity activity) {
		super(context, textViewResourceId, objects);

		items = objects;
		mActivity = activity;
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
			if (device.deviceAddress == mActivity.ownerDeviceAddress) {
				bottom.setText("GROUP OWNER");
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				icon.setImageResource(R.drawable.owner);
			} else {
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				icon.setImageResource(R.drawable.connected);
			}
		}

		return v;

	}

}
