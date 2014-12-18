package com.withs.listentogether.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.withs.listentogether.InstructionSocket;
import com.withs.listentogether.MusicInfo;
import com.withs.listentogether.Playlist;
import com.withs.listentogether.PlaylistSocket;
import com.withs.listentogether.R;
import com.withs.listentogether.Utils;
import com.withs.listentogether.activity.PlaybackActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OptionalDataException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class PlaybackService extends Service {

	public static final int MSG_CONNECTED = 0;
	public static final int MSG_START_SEND_MUSIC_OBJECT = 1;
	public static final int MSG_UPDATE_NOTIFICATION_INFO = 2;

	// private static final boolean DEBUG = true;
	private static final int NOTIFICATION_ID = 19880920;
	private static final String TAG = "PlaybackService";

	static class IncomingHandler extends Handler {

		private final WeakReference<PlaybackService> mService;

		private IncomingHandler(PlaybackService service) {
			mService = new WeakReference<PlaybackService>(service);
		}

		@Override
		public void handleMessage(Message msg) {

			PlaybackService service = mService.get();

			switch (msg.what) {
			case MSG_CONNECTED:
				service.mReplyTo = msg.replyTo;
				break;
			case MSG_START_SEND_MUSIC_OBJECT:
				new Thread(service.new SendMusicObjectThread(
						(MusicInfo) msg.obj)).start();
				break;
			case MSG_UPDATE_NOTIFICATION_INFO:
				service.updateNotificationContentInfo();
				break;
			default:
				super.handleMessage(msg);
			}
		}

	}

	Messenger mReplyTo = null;

	final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	// 와이파이다이렉트 초기화에 필요한 멤버
	private WifiP2pManager mManager;
	private Channel mChannel;

	private boolean isGroupOwner;
	private String mMyAddress;
	private WifiP2pInfo mInfo;

	private MediaPlayer mMediaPlayer;

	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;

	private WakeLock mWakeLock;

	private Resources mResources = null;

	private InstructionSocket mInstructionSocket = InstructionSocket
			.getInstance();

	private PlaylistSocket mPlaylistSocket = PlaylistSocket.getInstance();

	private ArrayList<MusicInfo> mListMusicInfo = Playlist.getInstance()
			.getListMusicInfo();

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// 와이파이 다이렉트 초기화
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);

		// 내 주소 초기화
		mMyAddress = mInstructionSocket.getListCustomSockets().get(0)
				.getSocket().getLocalAddress().getHostAddress();

		// Resources 객체 초기화
		mResources = getApplicationContext().getResources();

		// 통지 생성
		startForegroundNotification();

		// WakeLock 초기화
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

		// isGroupOwner 초기화 및 재생목록 리스트 갱신 스레드 초기화
		mManager.requestConnectionInfo(mChannel, new ConnectionInfoListener() {

			@Override
			public void onConnectionInfoAvailable(WifiP2pInfo info) {

				isGroupOwner = info.isGroupOwner;
				mInfo = info;

				// 재생목록 스레드 생성
				if (isGroupOwner == true) {
					new Thread(new PlaylistServerThread()).start();
				} else {
					new Thread(new PlaylistClientThread()).start();
				}

				// 재생 스레드 생성
				new Thread(new PlaybackThread()).start();

				// Instruction Thread 생성
				new Thread(new ReceiveInstructionThread()).start();

			}

		});

		// MediaPlayer 객체 설정
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {

				mListMusicInfo.remove(0);

				mp.reset();

				Utils.sendMessage(mReplyTo,
                        PlaybackActivity.MSG_PLAYLIST_CHANGED);

				updateNotificationContentInfo();

				// 재생 쓰레드 생성
				new Thread(new PlaybackThread()).start();

			}

		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// 상대방에게 종료 알림
		try {
			mInstructionSocket.getListCustomSockets().get(0)
					.getDataOutputStream().writeBytes("quit\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// WakeLock 해제
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}

		// MediaPlayer 반환 및 해제
		mMediaPlayer.release();
		mMediaPlayer = null;

		mInstructionSocket.close();
		mPlaylistSocket.close();

		Utils.clearWiFiP2p(mManager, mChannel);

		stopForeground(true);
		mNotificationManager.cancel(NOTIFICATION_ID);

		android.os.Process.killProcess(android.os.Process.myPid());
	}

	private class PlaylistServerThread implements Runnable {

		@Override
		public void run() {

			ServerSocket serverSocket = null;

			try {
				serverSocket = new ServerSocket(8930);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				serverSocket.setReuseAddress(true);
			} catch (SocketException e) {
				e.printStackTrace();
			}

			try {
				mPlaylistSocket.addSocket(
						Utils.initSocket(serverSocket.accept()),
						PlaylistSocket.SERVER);
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			while (true) {
				new Thread(new SendMusicObjectThread(receiveMusicInfo()))
						.start();
			}

		}

	}

	private class PlaylistClientThread implements Runnable {

		@Override
		public void run() {

			Socket socket = Utils.makeConnectedSocket(
					mInfo.groupOwnerAddress.getHostAddress(), 8930);

			mPlaylistSocket.addSocket(socket, PlaylistSocket.CLIENT);

			while (true) {
				receiveMusicInfo();
			}

		}

	}

	private class PlaybackThread implements Runnable {

		@Override
		public void run() {

			if (mListMusicInfo.size() <= 0) {
				String contentTitle = mResources
						.getString(R.string.notification_title);
				String contentText = mResources
						.getString(R.string.notification_text);
				Bitmap icon = BitmapFactory.decodeResource(mResources,
						R.drawable.ic_launcher);

				mBuilder.setContentTitle(contentTitle)
						.setContentText(contentText)
						.setWhen(System.currentTimeMillis()).setLargeIcon(icon);
				mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
			}

			while (true) {
				if (mListMusicInfo.size() > 0) {
					break;
				}
			}

			updateNotification();

			String MusicOwnerAddress = mListMusicInfo.get(0).mOwnerAddress;

			if (MusicOwnerAddress.equals(mMyAddress)) {
				transferServer();
			} else {
				transferClient();
			}

			// TODO count
			if (isGroupOwner == true) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Socket socket = new Socket();
							socket.setReuseAddress(true);
							socket.connect(new InetSocketAddress(
									"listentogether.iptime.org", 8920), 0);
							// socket.connect(new InetSocketAddress("localhost",
							// 8920), 0);

							// System.out.println("연결성공");

							DataOutputStream dos = new DataOutputStream(
									socket.getOutputStream());

							dos.writeBytes("addCount\r\n");

							// System.out.println("getCount");

							// BufferedReader br = new BufferedReader(new
							// InputStreamReader(socket.getInputStream()));
							// final String result = br.readLine();

						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
						} catch (IOException e) {
							// TODO Auto-generated catch block
						}
					}

				}).start();
			}
		}

	}

	private void transferServer() {

		try {
			Uri uri = Uri.parse(mListMusicInfo.get(0).mUriPath);
			ContentResolver cr = getApplicationContext().getContentResolver();

			// 파일객체의 크기를 알아내기 위해 uri 객체로부터 실제 파일 객체를 찾아냄
			Cursor cursor = cr.query(uri, null, null, null, null);
			cursor.moveToFirst();
			String path = cursor.getString(cursor.getColumnIndex("_data"));
			cursor.close();

			File file = new File(path);

			ServerSocket serverSocket = new ServerSocket(8940);
			serverSocket.setReuseAddress(true);
			Socket socket = Utils.initSocket(serverSocket.accept());
			serverSocket.close();

			long length = file.length();
			DataOutputStream dataOutputStream = new DataOutputStream(
					socket.getOutputStream());

			dataOutputStream.writeLong(length);

			BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(file));
			BufferedOutputStream bos = new BufferedOutputStream(
					socket.getOutputStream());

			sendFileBuffer(bis, bos);

			bis.close();

			mMediaPlayer.setDataSource(file.getPath());
			mMediaPlayer.prepare();

			DataInputStream dataInputStream = new DataInputStream(
					socket.getInputStream());

			dataInputStream.readByte();
			dataOutputStream.writeByte(1);

			mMediaPlayer.start();

			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void transferClient() {

		try {
			File tempFile = new File(getExternalFilesDir(null), "temp.mp3");

			if (!tempFile.exists()) {
				tempFile.delete();
			}
			tempFile.createNewFile();

			Socket socket = Utils.makeConnectedSocket(
					mListMusicInfo.get(0).mOwnerAddress, 8940);

			long length = new DataInputStream(socket.getInputStream())
					.readLong();
			BufferedInputStream bis = new BufferedInputStream(
					socket.getInputStream());
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(tempFile));

			copyFileBuffer(bis, bos, length);
			bos.close();

			mMediaPlayer.setDataSource(tempFile.getPath());
			mMediaPlayer.prepare();

			DataOutputStream dataOutputStream = new DataOutputStream(
					socket.getOutputStream());
			DataInputStream dataInputStream = new DataInputStream(
					socket.getInputStream());

			dataOutputStream.writeByte(1);
			dataInputStream.readByte();

			mMediaPlayer.start();

			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class SendMusicObjectThread implements Runnable {

		private MusicInfo mAddedMusic;

		public SendMusicObjectThread(MusicInfo addedMusic) {
			mAddedMusic = addedMusic;
		}

		@Override
		public void run() {
			try {
				mPlaylistSocket.getListCustomSockets().get(0)
						.getObjectOutputStream().writeObject(mAddedMusic);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private MusicInfo receiveMusicInfo() {

		MusicInfo musicInfo = null;

		try {
			musicInfo = (MusicInfo) mPlaylistSocket.getListCustomSockets()
					.get(0).getObjectInputStream().readObject();
		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		mWakeLock.acquire(3600000);

		mListMusicInfo.add(musicInfo);

		Utils.sendMessage(mReplyTo, PlaybackActivity.MSG_PLAYLIST_CHANGED);

		updateNotificationContentInfo();

		return musicInfo;
	}

	private class ReceiveInstructionThread implements Runnable {

		@Override
		public void run() {

			while (true) {
				String instruction = null;

				try {
					instruction = mInstructionSocket.getListCustomSockets()
							.get(0).getBufferedReader().readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (instruction.equals("quit")) {
					Utils.sendMessage(mReplyTo, PlaybackActivity.MSG_QUIT);
				}
			}

		}

	}

	private boolean sendFileBuffer(BufferedInputStream inputStream,
			BufferedOutputStream out) {

		byte buf[] = new byte[1024];
		int len;

		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
			out.flush();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	private boolean copyFileBuffer(BufferedInputStream inputStream,
			BufferedOutputStream out, long length) {

		byte buf[] = new byte[1024];
		int len;
		long remainLength = length;

		try {
			while (remainLength > 0) {
				len = inputStream.read(buf);
				out.write(buf, 0, len);
				remainLength = remainLength - len;
			}
			out.flush();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	private void startForegroundNotification() {

		Intent intent = new Intent(this, PlaybackActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				intent, 0);

		mBuilder = new Notification.Builder(this)
				.setContentIntent(pendingIntent)
				.setSmallIcon(R.drawable.ic_launcher).setContentText("")
				.setContentTitle("");

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		startForeground(NOTIFICATION_ID, mBuilder.build());

	}

	private void updateNotification() {

		mBuilder.setLargeIcon(mListMusicInfo.get(0).mAlbumArt)
				.setContentTitle(mListMusicInfo.get(0).mTitle)
				.setContentText(mListMusicInfo.get(0).mArtist)
				.setWhen(System.currentTimeMillis());

		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

	}

	private void updateNotificationContentInfo() {

		int number = mListMusicInfo.size();

		if (number >= 1) {
			number = number - 1;
		}

		String contentInfo = mResources.getString(R.string.notification_info);

		mBuilder.setContentInfo(number + contentInfo);
		mBuilder.setWhen(System.currentTimeMillis());
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

	}

}