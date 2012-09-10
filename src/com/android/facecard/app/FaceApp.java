package com.android.facecard.app;

import android.app.Application;
import android.os.Handler;

public class FaceApp extends Application {
	public CacheManager mCacheManager;
	public ThreadPool mThreadPool;
	public Handler uiHandler;

	public CacheManager getCacheManager() {
		if (mCacheManager == null) {
			mCacheManager = new CacheManager(this);
		}
		return mCacheManager;
	}

	public ThreadPool getThreadPool() {
		if (mThreadPool == null) {
			mThreadPool = new ThreadPool();
		}
		return mThreadPool;
	}

	public Handler getUIHandler() {
		if (uiHandler == null) {
			uiHandler = new Handler(getMainLooper());
		}
		return uiHandler;
	}
}
