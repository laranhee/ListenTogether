package com.withs.listentogether;

import android.graphics.Bitmap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class MusicInfo implements Serializable {

	private static final long serialVersionUID = 1L;
	private static ByteBuffer dst;
	private static byte[] bytesar;
	
	public String mArtist;
	public String mTitle;
	public String mOwnerAddress;
	public String mUriPath;
	public Bitmap mAlbumArt;

	public MusicInfo(String artist, String title, String ownerAddress,
			String uriPath, Bitmap albumArt) {
		mArtist = artist;
		mTitle = title;
		mOwnerAddress = ownerAddress;
		mUriPath = uriPath;
		mAlbumArt = albumArt;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(mArtist);
		out.writeObject(mTitle);
		out.writeObject(mOwnerAddress);
		out.writeObject(mUriPath);

		out.writeInt(mAlbumArt.getHeight());
		out.writeInt(mAlbumArt.getWidth());

		int bmSize = mAlbumArt.getRowBytes() * mAlbumArt.getHeight();
		
		if (dst == null || bmSize > dst.capacity()) {
			dst = ByteBuffer.allocate(bmSize);
		}
		
		out.writeInt(dst.capacity());

		dst.position(0);

		mAlbumArt.copyPixelsToBuffer(dst);
		
		if (bytesar == null || bmSize > bytesar.length) {
			bytesar = new byte[bmSize];
		}
		
		dst.position(0);
		dst.get(bytesar);

		out.write(bytesar, 0, bytesar.length);
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		mArtist = (String) in.readObject();
		mTitle = (String) in.readObject();
		mOwnerAddress = (String) in.readObject();
		mUriPath = (String) in.readObject();

		int height = in.readInt();
		int width = in.readInt();
		int bmSize = in.readInt();

		if (bytesar == null || bmSize > bytesar.length) {
			bytesar = new byte[bmSize];
		}
		
		int offset = 0;
		
		while (in.available() > 0) {
			offset = offset + in.read(bytesar, offset, in.available());
		}

		if (dst == null || bmSize > dst.capacity()) {
			dst = ByteBuffer.allocate(bmSize);
		}
		
		dst.position(0);
		
		dst.put(bytesar);
		
		dst.position(0);
		
		mAlbumArt = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		mAlbumArt.copyPixelsFromBuffer(dst);
	}

}
