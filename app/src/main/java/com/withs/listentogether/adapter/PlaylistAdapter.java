package com.withs.listentogether.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

//import com.listentogether.MusicInfo;
//import com.listentogether.R;

import com.withs.listentogether.MusicInfo;
import com.withs.listentogether.R;

import java.util.List;

public class PlaylistAdapter extends ArrayAdapter<MusicInfo> {

	private Context mContext;
	private List<MusicInfo> items;
	private int mScreenWidth;

	public PlaylistAdapter(Context context, int textViewResourceId,
			List<MusicInfo> objects, int screenWidth) {
		super(context, textViewResourceId, objects);
		mContext = context;
		items = objects;
		mScreenWidth = screenWidth;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.row_playlist, parent, false);
		}
		
		MusicInfo addedMusic = items.get(position);
		
		if(addedMusic != null) {
			
			TextView textTitle = (TextView) v.findViewById(R.id.playlist_title);
			TextView textArtist = (TextView) v.findViewById(R.id.playlist_artist);
			ImageView imageAlbumArt = (ImageView) v.findViewById(R.id.playlist_albumart);
			
			textTitle.setText(addedMusic.mTitle);
			textArtist.setText(addedMusic.mArtist);
			imageAlbumArt.setImageBitmap(addedMusic.mAlbumArt);
			
			if (position == 0) {
				int px = mScreenWidth * 8 / 16;
				imageAlbumArt.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px));
			} else {
				int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, mContext.getResources().getDisplayMetrics());
				imageAlbumArt.setLayoutParams(new LayoutParams(px, px));
			}
			
		}

		return v;
	}

}
