/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.gltf;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.ImageFormat;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Display text in AR scene using Sceneform
 * Call the function to scan barcode
 */
public class GltfActivity extends AppCompatActivity {
  private static final String TAG = GltfActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  private ArFragment arFragment;
  private ViewRenderable testRenderable;
  private AnchorNode anchorNode;
  // int count = 1;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

    // Build the 2D renderable
    ViewRenderable.builder()
        .setView(this, R.layout.card_view)
        .build()
        .thenAccept(renderable -> testRenderable = renderable);

    // Tap the anchor to place objects
      /*
    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable model and add it to the anchor.
          TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
          model.setParent(anchorNode);
          model.setRenderable(testRenderable);
          model.select();
        });
       */

      arFragment
        .getArSceneView()
        .getScene()
        .addOnUpdateListener(
            this::onSceneUpdate
            /*frameTime -> {
                arFragment.onUpdate(frameTime);
            }*/
            );
  }

  private void onSceneUpdate(FrameTime frameTime) {
      // Set Auto focus of the camera
      Session session = arFragment.getArSceneView().getSession();
      Config config = null;
      if (session != null) {
          config = session.getConfig();
          config.setFocusMode(Config.FocusMode.AUTO);
          session.configure(config);
      }

      // Let the fragment update its state first.
      arFragment.onUpdate(frameTime);

      // If there is no frame then don't process anything.
      if (arFragment.getArSceneView().getArFrame() == null) {
          return;
      }

      // If ARCore is not tracking yet, then don't process anything.
      if (arFragment.getArSceneView().getArFrame().getCamera().getTrackingState() != TrackingState.TRACKING) {
          return;
      }

      // Place the anchor 1m in front of the camera if anchorNode is null.
      if (this.anchorNode == null && session != null) {
          // Session session = arFragment.getArSceneView().getSession();
          float[] pos = {0,0,-1};
          float[] rotation = {0,0,0,1};
          Anchor anchor = session.createAnchor(new Pose(pos, rotation));
          anchorNode = new AnchorNode(anchor);
          anchorNode.setRenderable(testRenderable);
          anchorNode.setParent(arFragment.getArSceneView().getScene());
      }

      // Send frame image and scan barcode
      try (final Image image = arFragment.getArSceneView().getArFrame().acquireCameraImage()) {
          if (image.getFormat() == ImageFormat.YUV_420_888) {
              Bitmap bitmapImage = YUV420toByteArray.getByteArray(image);
              // byte[] byteArray = YUV420toByteArray.getByteArray(image);
              BarcodeScan barScanning = new BarcodeScan();
              InputImage inputImage = InputImage.fromBitmap(bitmapImage, 0);
              /*if (count < 5){
                  String filename = "file" + count + ".jpeg";
                  count++;
                  Log.d("TAG",filename);
                  saveBitmap(bitmapImage, filename);
              }*/
              // InputImage inputImage = InputImage.fromByteArray(byteArray, image.getWidth(), image.getHeight(), 0, IMAGE_FORMAT_YUV_420_888);
              barScanning.scanBarcodes(inputImage);
              image.close();
          }
      } catch (NotYetAvailableException e) {
          Log.e("TAG", e.getMessage());
      }
  }

    /*public void saveBitmap(Bitmap bitmap, String filename) throws IOException {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        File file = new File(appDir, filename);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            MediaStore.Images.Media.insertImage(getContentResolver(),
                file.getAbsolutePath(), filename, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        //sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(file.getAbsolutePath())));
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        sendBroadcast(intent);
        // 图片保存成功，图片路径：
        Log.d("File saved", "path:" + file.getAbsolutePath());
    }*/

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }
}
