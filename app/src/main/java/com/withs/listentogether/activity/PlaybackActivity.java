package com.withs.listentogether.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

//import com.listentogether.InstructionSocket;
//import com.listentogether.MusicInfo;
//import com.listentogether.Playlist;
//import com.listentogether.R;
//import com.listentogether.MyUtil;
//import com.listentogether.adapter.PlaylistAdapter;
//import com.listentogether.receiver.PlaybackReceiver;
//import com.listentogether.service.PlaybackService;

import com.withs.listentogether.InstructionSocket;
import com.withs.listentogether.MusicInfo;
import com.withs.listentogether.Playlist;
import com.withs.listentogether.R;
import com.withs.listentogether.MyUtil;
import com.withs.listentogether.adapter.PlaylistAdapter;
import com.withs.listentogether.receiver.PlaybackReceiver;
import com.withs.listentogether.service.PlaybackService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class PlaybackActivity extends Activity {

	public static final int MSG_PLAYLIST_CHANGED = 0;
	public static final int MSG_QUIT = 1;

	// private static final String TAG = "PlaybackActivity";
	private static final int REQUEST_ADD_MUSIC = 2;

	static class InComingHandler extends Handler {

		private final WeakReference<PlaybackActivity> mActivity;

		private InComingHandler(PlaybackActivity activity) {
			mActivity = new WeakReference<PlaybackActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {

			PlaybackActivity activity = mActivity.get();

			switch (msg.what) {
			case MSG_PLAYLIST_CHANGED:
				activity.updatePlaylistChange();
				break;
			case MSG_QUIT:
				activity.quit();
				break;
			default:
				super.handleMessage(msg);
			}

		}

	}

	// replyTo
	Messenger mService = null;

	// my Messenger
	Messenger mMessenger = new Messenger(new InComingHandler(this));

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = new Messenger(service);
			MyUtil.sendMessage(mService, PlaybackService.MSG_CONNECTED,
                    mMessenger);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

	};

	// WiFiP2p
	private WifiP2pManager mManager;
	private Channel mChannel;
	private BroadcastReceiver mReceiver;
	
	private ActionBar mActionBar;
	private Resources mResources;
	
	private boolean isGroupOwner;
	private String mMyAddress;

	private final long FINSH_INTERVAL_TIME = 2000;
	private long mBackPressedTime = 0;

	private InstructionSocket mInstructionSocket = InstructionSocket
			.getInstance();

	private ArrayList<MusicInfo> mListMusicInfo = Playlist.getInstance()
			.getListMusicInfo();

	private ListView mPlaylistListView;
	private ArrayAdapter<MusicInfo> mPlaylistAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_playback);

		// 액션바 초기화
		mActionBar = getActionBar();
		mActionBar.setHomeButtonEnabled(false);

		// Resources 초기화
		mResources = getResources();

		// 서비스 바인딩
		bindService(new Intent(PlaybackActivity.this, PlaybackService.class),
				mConnection, Context.BIND_AUTO_CREATE);

		// 와이파이 다이렉트 초기화
		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);

		// 내 주소 초기화
		mMyAddress = mInstructionSocket.getListCustomSockets().get(0)
				.getSocket().getLocalAddress().getHostAddress();

		// isGroupOwner, mInfo 초기화
		mManager.requestConnectionInfo(mChannel, new ConnectionInfoListener() {

			@Override
			public void onConnectionInfoAvailable(WifiP2pInfo info) {
				isGroupOwner = info.isGroupOwner;
			}

		});

		// 액션바 갱신
		updateActionBarTitle();

		//TODO
		// 고양이 배경 갱신
//		int screenWidth = getScreenWidthPixels();
		int screenWidth = MyUtil.getScreenWidthPixels(this);
		initCatImageView(screenWidth);
		
		//TODO 도움말 레이아웃 사이즈갱신
		LinearLayout layout = (LinearLayout) findViewById(R.id.playback_help);
		layout.setLayoutParams(new android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, screenWidth / 2));
		
		// 리스트뷰 어댑터 설정
		mPlaylistAdapter = new PlaylistAdapter(this, R.layout.row_playlist,
				mListMusicInfo, screenWidth);
		mPlaylistListView = (ListView) findViewById(R.id.playback_playlist);
		mPlaylistListView.setAdapter(mPlaylistAdapter);

		// receiver
		mReceiver = new PlaybackReceiver(this);
		IntentFilter intentFilter = new IntentFilter(
				WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		registerReceiver(mReceiver, intentFilter);

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
		unregisterReceiver(mReceiver);
	}

	@Override
	public void onBackPressed() {
		// back 버튼을 누르면 액티비티가 종료되지않고 홈 화면으로 넘어간다
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_playback, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.playback_action_add:
			startActivityForAddMusic();
			break;
		case R.id.playback_action_help:
			setHelpVisibility(item);
			break;
		case R.id.playback_action_quit:
			startQuitTimer();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != RESULT_CANCELED) {
			if (requestCode == REQUEST_ADD_MUSIC) {
				addMusic(data);
			}
		}
	}

	private void startActivityForAddMusic() {
		Intent intent = new Intent(
				Intent.ACTION_PICK,
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, REQUEST_ADD_MUSIC);
	}

	private void setHelpVisibility(MenuItem item) {
		View helpView = findViewById(R.id.playback_help);
		if (helpView.getVisibility() == View.VISIBLE) {
			helpView.setVisibility(View.INVISIBLE);
			item.setIcon(R.drawable.ic_action_help);
		} else {
			helpView.setVisibility(View.VISIBLE);
			item.setIcon(R.drawable.ic_action_help_on);
		}
	}

	private void addMusic(Intent data) {
		Uri uri = data.getData();
		ContentResolver contentResolver = getContentResolver();
		String[] cols = new String[] { AudioColumns.ARTIST, MediaColumns.TITLE,
				AudioColumns.ALBUM_ID };
		Cursor cursor = contentResolver.query(uri, cols, null, null, null);
		cursor.moveToFirst();
	
		int artistColumn = cursor.getColumnIndex(AudioColumns.ARTIST);
		int titleColumn = cursor.getColumnIndex(MediaColumns.TITLE);
		int albumIdColumn = cursor.getColumnIndex(AudioColumns.ALBUM_ID);
	
		String artist = cursor.getString(artistColumn);
		String title = cursor.getString(titleColumn);
		Bitmap albumArt = getAlbumArt(cursor.getLong(albumIdColumn));
	
		MusicInfo musicInfo = new MusicInfo(artist, title, mMyAddress,
				uri.toString(), albumArt);
	
		if (isGroupOwner == true) {
			addMusicToPlaylist(musicInfo);
			MyUtil.sendMessage(mService,
                    PlaybackService.MSG_START_SEND_MUSIC_OBJECT, musicInfo);
			MyUtil.sendMessage(mService,
                    PlaybackService.MSG_UPDATE_NOTIFICATION_INFO);
		} else {
			MyUtil.sendMessage(mService,
                    PlaybackService.MSG_START_SEND_MUSIC_OBJECT, musicInfo);
		}
	}

	private void addMusicToPlaylist(MusicInfo addedMusic) {
		mListMusicInfo.add(addedMusic);
		updatePlaylistChange();
	}

	private int getScreenWidthPixels() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics.widthPixels;
	}

	private void initCatImageView(int screenWidth) {
		int px = screenWidth / 2;

		LayoutParams layoutParams = new LayoutParams(px, px);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

		ImageView imageView = (ImageView) findViewById(R.id.playback_cats);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		imageView.setLayoutParams(layoutParams);
	}

	private void updatePlaylistChange() {
		mPlaylistAdapter.notifyDataSetChanged();
		updateActionBarTitle();
	}

	private void updateActionBarTitle() {
		if (mListMusicInfo.size() > 0) {
			getActionBar().setIcon(
					new BitmapDrawable(getResources(),
							mListMusicInfo.get(0).mAlbumArt));
			getActionBar().setTitle(mListMusicInfo.get(0).mTitle);
			getActionBar().setSubtitle(mListMusicInfo.get(0).mArtist);
		} else {
			String title = mResources
					.getString(R.string.playback_actionbar_title);
			String subTitle = mResources
					.getString(R.string.playback_actionbar_subtitle);
			getActionBar().setIcon(R.drawable.ic_launcher);
			getActionBar().setTitle(title);
			getActionBar().setSubtitle(subTitle);
		}
	}

	private Bitmap getAlbumArt(long albumId) {
		Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
		Uri uri = ContentUris.withAppendedId(artworkUri, albumId);
		ContentResolver cr = getApplicationContext().getContentResolver();
		InputStream in = null;

		try {
			in = cr.openInputStream(uri);
		} catch (FileNotFoundException e) {
			return BitmapFactory.decodeResource(mResources,
					R.drawable.ic_launcher);
		}

		return BitmapFactory.decodeStream(in);
	}

	private void startQuitTimer() {
		long tempTime = System.currentTimeMillis();
		long intervalTime = tempTime - mBackPressedTime;
	
		if (0 <= intervalTime && FINSH_INTERVAL_TIME >= intervalTime) {
			sendQuitMessage();
			quit();
		} else {
			mBackPressedTime = tempTime;
			Toast.makeText(this, R.string.playback_toast_quit,
					Toast.LENGTH_SHORT).show();
		}
	}

	public void sendQuitMessage() {
		try {
			mInstructionSocket.getListCustomSockets().get(0)
					.getDataOutputStream().writeBytes("quit\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void quit() {
		moveTaskToBack(true);
		finish();
	}

}
