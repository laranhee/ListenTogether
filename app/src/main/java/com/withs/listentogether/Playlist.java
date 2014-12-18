package com.withs.listentogether;

import java.util.ArrayList;

public class Playlist {

	private static Playlist instance;

	private ArrayList<MusicInfo> mListMusicInfo = new ArrayList<MusicInfo>();

	private Playlist() {
	}
	
	public static Playlist getInstance() {
		if (instance == null) {
			instance = new Playlist();
		}
		return instance;
	}
	
	public ArrayList<MusicInfo> getListMusicInfo() {
		return mListMusicInfo;
	}

}