package com.google.ar.sceneform.samples.gltf;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class BarcodeScan {
	private static final String TAG = "Barcode Detect";
	public void scanBarcodes(InputImage image) {
		BarcodeScannerOptions options =
			new BarcodeScannerOptions.Builder()
				.setBarcodeFormats(Barcode.FORMAT_CODE_128)
				.build();

		BarcodeScanner scanner = BarcodeScanning.getClient();
		// Or, to specify the formats to recognize:
		Log.d(TAG, "set options");
//        BarcodeScanner scanner = BarcodeScanning.getClient(options);

		Log.d(TAG, "getClient");
		Task<List<Barcode>> result = scanner.process(image)
			.addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
				@Override
				public void onSuccess(List<Barcode> barcodes) {
					Log.d(TAG, "success");
					// Task completed successfully
					// [START_EXCLUDE]
					// [START get_barcodes]
					for (Barcode barcode: barcodes) {
//                            Rect bounds = barcode.getBoundingBox();
//                            Point[] corners = barcode.getCornerPoints();
//
						String rawValue = barcode.getRawValue();
						Log.d(TAG, "The id is: " + rawValue);

//                            int valueType = barcode.getValueType();
//                            // See API reference for complete list of supported types
//                            switch (valueType) {
//                                case Barcode.TYPE_WIFI:
//                                    String ssid = barcode.getWifi().getSsid();
//                                    String password = barcode.getWifi().getPassword();
//                                    int type = barcode.getWifi().getEncryptionType();
//                                    break;
//                                case Barcode.TYPE_URL:
//                                    String title = barcode.getUrl().getTitle();
//                                    String url = barcode.getUrl().getUrl();
//                                    break;
//                            }
					}
					// [END get_barcodes]
					// [END_EXCLUDE]

				}
			})
			.addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					Log.d(TAG, "Failure");
					// Task failed with an exception
					// ...
				}
			});
	}
}
