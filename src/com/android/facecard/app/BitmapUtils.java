package com.android.facecard.app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.text.TextPaint;
import android.util.Log;

public class BitmapUtils {
	private static final String LOG_TAG = "ImageUtils";
	private static final float EDGE_START = 0.0f;
	private static final float EDGE_END = 4.0f;
	private static final Paint EDGE_PAINT = new Paint();

	private static final Paint END_EDGE_PAINT = new Paint();

	private static final float FOLD_START = 5.0f;
	private static final float FOLD_END = 20.0f;
	private static final Paint FOLD_PAINT = new Paint();
	private static final int EDGE_COLOR_START = 0x7F000000;
	private static final int EDGE_COLOR_END = 0x00000000;
	private static final int FOLD_COLOR_START = 0x00000000;
	private static final int FOLD_COLOR_END = 0x26000000;
	private static final int END_EDGE_COLOR_START = 0x00000000;
	private static final int END_EDGE_COLOR_END = 0x4F000000;
	private static final float SHADOW_RADIUS = 15.0f;

	private static final int SHADOW_COLOR = 0x99000000;
	private static final Paint SHADOW_PAINT = new Paint();
	static {
		Shader shader = new LinearGradient(EDGE_START, 0.0f, EDGE_END, 0.0f,
				EDGE_COLOR_START, EDGE_COLOR_END, Shader.TileMode.CLAMP);
		EDGE_PAINT.setShader(shader);

		shader = new LinearGradient(EDGE_START, 0.0f, EDGE_END, 0.0f,
				END_EDGE_COLOR_START, END_EDGE_COLOR_END, Shader.TileMode.CLAMP);
		END_EDGE_PAINT.setShader(shader);

		shader = new LinearGradient(
				FOLD_START,
				0.0f,
				FOLD_END,
				0.0f,
				new int[] { FOLD_COLOR_START, FOLD_COLOR_END, FOLD_COLOR_START },
				new float[] { 0.0f, 0.5f, 1.0f }, Shader.TileMode.CLAMP);
		FOLD_PAINT.setShader(shader);

		SHADOW_PAINT.setShadowLayer(SHADOW_RADIUS / 2.0f, 0.0f, 0.0f,
				SHADOW_COLOR);
		SHADOW_PAINT.setAntiAlias(true);
		SHADOW_PAINT.setFilterBitmap(true);
		SHADOW_PAINT.setColor(0xFF000000);
		SHADOW_PAINT.setStyle(Paint.Style.FILL);
	}

	public static Bitmap decodeDMCover(int color, String title, int width,
			int hegith) {
		Bitmap decored = Bitmap.createBitmap(Math.round(width + SHADOW_RADIUS),
				Math.round(hegith + SHADOW_RADIUS), Config.ARGB_8888);
		//
		// Log.d(LOG_TAG, "width " + mWidth + " height " + mHeight);
		final Canvas canvas = new Canvas(decored);
		// canvas.drawBitmap(preview, new Rect(0, 0, preview.getWidth(),
		// preview.getHeight()), new Rect(0, 0, mWidth, mHeight),
		// paint);
		canvas.save();
		canvas.translate(SHADOW_RADIUS / 2, SHADOW_RADIUS / 2);
		Rect r = new Rect(0, 0, width, hegith);
		canvas.drawRect(r, SHADOW_PAINT);

		Paint paint = new Paint();
		paint.setColor(color);
		canvas.drawRect(new Rect(0, 0, width, hegith), paint);
		canvas.restore();

		canvas.translate(SHADOW_RADIUS / 2.0f, SHADOW_RADIUS / 2.0f);

		canvas.drawRect(EDGE_START, 0.0f, EDGE_END, hegith, EDGE_PAINT);
		canvas.drawRect(FOLD_START, 0.0f, FOLD_END, hegith, FOLD_PAINT);
		// // noinspection PointlessArithmeticExpression
		canvas.translate(width - (EDGE_END - EDGE_START), 0.0f);
		canvas.drawRect(EDGE_START, 0.0f, EDGE_END, hegith, END_EDGE_PAINT);

		return decored;
	}

	public static int[] calculateTargetSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		float factor = Math.max((float) reqWidth / options.outWidth,
				(float) reqHeight / options.outHeight);
		return new int[] { (int) (options.outWidth * factor),
				(int) (options.outHeight * factor) };
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		return inSampleSize;
	}

	public static Bitmap resizeAndCrop(Bitmap source, int newWidth,
			int newHeight) {
		Matrix matrix = new Matrix();
		int originalWidth = source.getWidth();
		int originalHeight = source.getHeight();
		float scale = Math.max((float) newWidth / originalWidth,
				(float) newHeight / originalHeight);
		matrix.setScale(scale, scale);
		// RectF rect = new RectF(0, 0, originalWidth, originalHeight);
		// matrix.mapRect(rect);
		Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight,
				source.getConfig());
		Canvas canvas = new Canvas();
		canvas.concat(matrix);
		canvas.setBitmap(bitmap);
		bitmap.setDensity(source.getDensity());
		Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
		if (!matrix.rectStaysRect()) {
			paint.setAntiAlias(true);
		}

		int tmpWidth = Math.round(newWidth / scale);
		int tmpHeight = Math.round(newHeight / scale);
		int marginLeft = (originalWidth - tmpWidth) / 2;
		int marginTop = (originalHeight - tmpHeight) / 2;
		Rect srcBound = new Rect(marginLeft, marginTop, originalWidth
				- marginLeft, originalHeight - marginTop);
		Log.d(LOG_TAG, "marginLeft " + marginLeft + " marginTop " + marginTop);
		RectF targetBound = new RectF(0, 0, tmpWidth, tmpHeight);
		canvas.drawBitmap(source, srcBound, targetBound, paint);
		source.recycle();
		return bitmap;
	}

	public static Bitmap cropCenter(Bitmap source, int targetWidth,
			int targetHeight) {
		int marginLeft = (source.getWidth() - targetWidth) / 2;
		int marginTop = (source.getHeight() - targetHeight) / 2;
		Bitmap newBitmap = Bitmap.createBitmap(source, marginLeft, marginTop,
				targetWidth, targetHeight);
		return newBitmap;
	}

	public static Bitmap decodeSampledBitmapFromUriWithMaxEdgeAndRatio(
			ContentResolver resolver, Uri uri, int maxEdge, float ratio)
			throws FileNotFoundException {
		BitmapFactory.Options options = getBitmapSize(resolver, uri);
		if (options.outWidth > options.outHeight) {
			return decodeSampledBitmap(resolver, uri, options, maxEdge,
					Math.round(maxEdge * ratio));
		} else {
			return decodeSampledBitmap(resolver, uri, options,
					Math.round(maxEdge * ratio), maxEdge);
		}

	}

	public static Bitmap decodeSampledBitmapFromUri(ContentResolver resolver,
			Uri uri, int targetWidth, int targetHeight)
			throws FileNotFoundException {
		return decodeSampledBitmap(resolver, uri, getBitmapSize(resolver, uri),
				targetWidth, targetHeight);
	}

	public static BitmapFactory.Options getBitmapSize(ContentResolver resolver,
			Uri uri) throws FileNotFoundException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		InputStream input = resolver.openInputStream(uri);
		BitmapFactory.decodeStream(input, null, options);

		try {
			input.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return options;
	}

	public static Bitmap decodeSampledBitmap(ContentResolver resolver, Uri uri,
			BitmapFactory.Options options, int targetWidth, int targetHeight)
			throws FileNotFoundException {
		options.inSampleSize = BitmapUtils.calculateInSampleSize(options,
				targetWidth, targetHeight);
		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		InputStream input = resolver.openInputStream(uri);
		Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
		try {
			input.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(LOG_TAG, "bitmap " + bitmap);
		bitmap = resizeAndCrop(bitmap, targetWidth, targetHeight);
		return bitmap;
	}

	public static final int getBitmapByteCount(Bitmap bitmap) {
		return bitmap.getRowBytes() * bitmap.getHeight();
	}
}
