package com.withs.listentogether;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class PlaylistSocket {

	public static final int SERVER = 0;
	public static final int CLIENT = 1;

	private static PlaylistSocket instance;

	private ArrayList<CustomSocket> mListPlaylistSocket = new ArrayList<CustomSocket>();

	private PlaylistSocket() {
	};

	public static PlaylistSocket getInstance() {
		if (instance == null) {
			instance = new PlaylistSocket();
		}
		return instance;
	}

	public ArrayList<CustomSocket> getListCustomSockets() {
		return mListPlaylistSocket;
	}

	public void addSocket(Socket socket, int i) {
		mListPlaylistSocket.add(new CustomSocket(socket, i));
	}

	public void close() {
		for (int i = 0; i < mListPlaylistSocket.size(); i++) {
			try {
				mListPlaylistSocket.get(i).getObjectOutputStream().flush();
				mListPlaylistSocket.get(i).getObjectOutputStream().close();
				mListPlaylistSocket.get(i).getObjectInputStream().close();
				mListPlaylistSocket.get(i).getSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mListPlaylistSocket.clear();
	}

	public class CustomSocket {

		private Socket mSocket;
		private ObjectInputStream mObjectInputStream;
		private ObjectOutputStream mObjectOutputStream;

		public CustomSocket(Socket socket, int i) {
			mSocket = socket;

			if (i == SERVER) {
				try {
					mObjectInputStream = new ObjectInputStream(
							mSocket.getInputStream());
					mObjectOutputStream = new ObjectOutputStream(
							mSocket.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (i == CLIENT) {
				try {
					mObjectOutputStream = new ObjectOutputStream(
							mSocket.getOutputStream());
					mObjectInputStream = new ObjectInputStream(
							mSocket.getInputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public Socket getSocket() {
			return mSocket;
		}

		public ObjectInputStream getObjectInputStream() {
			return mObjectInputStream;
		}

		public ObjectOutputStream getObjectOutputStream() {
			return mObjectOutputStream;
		}

	}

}
