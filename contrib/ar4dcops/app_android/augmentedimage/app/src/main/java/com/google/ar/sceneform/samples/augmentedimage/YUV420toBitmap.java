package com.google.ar.sceneform.samples.augmentedimage;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.exceptions.NotYetAvailableException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Convert the YUV420 image captured by acquireCameraImage() into bitmap
 */
public class YUV420toBitmap {
	public static Bitmap getBitmap(Image image) {
		byte[] byteArray;
		byteArray = NV21toJPEG(YUV420toNV21(image), image.getWidth(), image.getHeight());
		// return byteArray;
		return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
	}

	private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
		yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
		return out.toByteArray();
	}

	private static byte[] YUV420toNV21(Image image) {
		byte[] nv21;
		// Get the three planes.
		ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
		ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
		ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

		int ySize = yBuffer.remaining();
		int uSize = uBuffer.remaining();
		int vSize = vBuffer.remaining();

		nv21 = new byte[ySize + uSize + vSize];

		// U and V are swapped
		yBuffer.get(nv21, 0, ySize);
		vBuffer.get(nv21, ySize, vSize);
		uBuffer.get(nv21, ySize + vSize, uSize);

		return nv21;
	}
}