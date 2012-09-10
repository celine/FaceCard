package com.android.facecard.app;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class CacheManager {
	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
	LruCache<String, Bitmap> mMemCache;
	DiskLruCache mDiskCache;

	public CacheManager(Context context) {
		mDiskCache = DiskLruCache.openCache(context, context.getCacheDir(),
				DISK_CACHE_SIZE);
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		int memClass = am.getMemoryClass();
		final int cacheSize = 1024 * 1024 * memClass / 8;
		mMemCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				// The cache size will be measured in bytes rather than number
				// of items.
				return BitmapUtils.getBitmapByteCount(bitmap);
			}
		};
	}

	public void put(String url, Bitmap bitmap) {
		String key = generateKey(url);
		Log.d(TAG, "put to disk cache " + bitmap);
		mDiskCache.put(key, bitmap);

	}

	private String generateKey(String url) {
		return String.valueOf(url.hashCode());
	}

	private static final String TAG = "ImageRequester";

	public Bitmap getFromMem(String url, int targetW, int targetH) {
		String newKey = generateKey(url) + targetW + "x" + targetH;
		Bitmap bitmap = mMemCache.get(newKey);
		return bitmap;
	}

	public Bitmap getFromDisk(String url, int targetW, int targetH) {
		String newKey = generateKey(url) + targetW + "x" + targetH;
		Bitmap bitmap = mDiskCache.get(generateKey(url), targetW, targetH);
		Log.d(TAG, "get from disk " + bitmap);
		if (bitmap != null) {
			mMemCache.put(newKey, bitmap);
		}

		return bitmap;
	}

	public void evictAll() {
		mMemCache.evictAll();
		mDiskCache.clearCache();

	}
}
