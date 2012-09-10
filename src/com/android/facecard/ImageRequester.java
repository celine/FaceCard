package com.android.facecard;

import com.android.facecard.app.CacheManager;
import com.android.facecard.app.FaceApp;
import com.android.facecard.app.ThreadPool;
import com.android.facecard.app.ThreadPool.FutureListener;
import com.android.facecard.app.ThreadPool.Job;
import com.android.facecard.app.ThreadPool.Worker;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

public class ImageRequester {
	CacheManager mCacheManager;
	ThreadPool mThreadPool;
	Handler uiHandler;
	String mUrl;

	public ImageRequester(FaceApp app) {
		mCacheManager = app.getCacheManager();
		mThreadPool = app.getThreadPool();
		uiHandler = app.getUIHandler();
	}

	private static final String TAG = "ImageRequester";

	public void submit(final ImageView image, final String url,
			final int targetW, final int targetH) {
		Bitmap bitmap = null;
		if (url != null) {
			image.setTag(String.valueOf(url.hashCode()));
			bitmap = mCacheManager.getFromMem(url, targetW, targetH);
		}
		if (bitmap != null) {
			image.setImageBitmap(bitmap);
		} else {
			image.setImageResource(android.R.color.darker_gray);
		}
		if (url == null) {
			return;
		}
		Job<Bitmap> job = new Job<Bitmap>() {

			@Override
			public Bitmap run() {
				Bitmap bitmap = mCacheManager
						.getFromDisk(url, targetW, targetH);
				if (bitmap == null) {
					bitmap = Utility.getBitmap(url);
					Log.d(TAG, "load bitmap " + bitmap);
					if (bitmap != null) {
						mCacheManager.put(url, bitmap);
						bitmap = mCacheManager.getFromDisk(url, targetW,
								targetH);
					}
				}
				return bitmap;
			}
		};
		FutureListener<Bitmap> listener = new FutureListener<Bitmap>() {
			@Override
			public void onExecuteFinish(final Bitmap bitmap) {
				if (bitmap != null) {
					uiHandler.post(new Runnable() {
						@Override
						public void run() {
							Log.d(TAG, "image tag " + image.getTag().toString());
							Log.d(TAG, "url hash " + url.hashCode());
							if (image.getTag().toString()
									.equals(String.valueOf(url.hashCode()))) {
								Log.d(TAG, "setImage " + bitmap.getWidth());
								image.setImageBitmap(bitmap);
							}

						}
					});
				}
			}
		};
		mThreadPool.submit(job, listener);
	}
}
