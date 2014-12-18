package com.withs.listentogether;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class InstructionSocket {

	private static InstructionSocket instance;

	private ArrayList<CustomSocket> mListInstructionSocket = new ArrayList<CustomSocket>();

	private InstructionSocket() {
	};

	public static InstructionSocket getInstance() {
		if (instance == null) {
			instance = new InstructionSocket();
		}
		return instance;
	}

	public ArrayList<CustomSocket> getListCustomSockets() {
		return mListInstructionSocket;
	}

	public void addSocket(Socket socket) {
		mListInstructionSocket.add(new CustomSocket(socket));
	}

	public void close() {
		for (int i = 0; i < mListInstructionSocket.size(); i++) {
			try {
				mListInstructionSocket.get(i).getDataOutputStream().flush();
				mListInstructionSocket.get(i).getDataOutputStream().close();
				mListInstructionSocket.get(i).getBufferedReader().close();
				mListInstructionSocket.get(i).getSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mListInstructionSocket.clear();
	}

	public class CustomSocket {

		private Socket mSocket;
		private BufferedReader mBufferedReader;
		private DataOutputStream mDataOutputStream;

		public CustomSocket(Socket socket) {
			mSocket = socket;
			
			try {
				mBufferedReader = new BufferedReader(new InputStreamReader(
						mSocket.getInputStream()));
				mDataOutputStream = new DataOutputStream(
						mSocket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public Socket getSocket() {
			return mSocket;
		}

		public BufferedReader getBufferedReader() {
			return mBufferedReader;
		}

		public DataOutputStream getDataOutputStream() {
			return mDataOutputStream;
		}

	}

}
