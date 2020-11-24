/*
 * Copyright 2018 Google LLC
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

package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.samples.augmentedimage.flowgate.flowgateClient;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.mlkit.vision.common.InputImage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * ArAugmentedImage_getTrackingMethod() and render only when the tracking method equals to
 * AR_AUGMENTED_IMAGE_TRACKING_METHOD_FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/c/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity {
  private ArFragment arFragment;
  private ImageView fitToScanView;
  private Button button;

  private TextView serverTextview, cabinetTextview;
  private ViewRenderable serverRenderable, cabinetRenderable;

  private AnchorNode serverNode, cabinetNode;

  private Boolean isScanSuccess = false;

  flowgateClient fc = new flowgateClient("202.121.180.32", "admin", "Ar_InDataCenter_450");

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);
    button = findViewById(R.id.button1);

    // Reset the app status.
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isScanSuccess = false;
        if (serverTextview != null){
          serverTextview.setText("");
        }
        if (cabinetTextview != null){
          cabinetTextview.setText("");
        }
      }
    });

    // Build the 2D renderable for text views of server & cabinet.
    ViewRenderable.builder()
            .setView(this, R.layout.server_view)
            .build()
            .thenAccept(renderable -> serverRenderable = renderable);
    ViewRenderable.builder()
        .setView(this, R.layout.cabinet_view)
        .build()
        .thenAccept(renderable -> cabinetRenderable = renderable);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      //fitToScanView.setVisibility(View.VISIBLE);
      fitToScanView.setVisibility(View.GONE);
    }
  }

  /**
   * Registered with the Sceneform Scene object, this method is called at the start of each frame.
   *
   * @param frameTime - time since last frame.
   */
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();
    Session session = arFragment.getArSceneView().getSession();

    if (session == null || frame == null) {
      return;
    }

    // Set Auto focus of the camera.
    Config config = session.getConfig();
    config.setFocusMode(Config.FocusMode.AUTO);
    session.configure(config);

    // Let the fragment update its state first.
    arFragment.onUpdate(frameTime);

    // If ARCore is not tracking yet, then don't process anything.
    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }

    serverTextview = (TextView) serverRenderable.getView();
    cabinetTextview = (TextView) cabinetRenderable.getView();
    // Rect barcodePos = null;
    // Scan once; and once the information is obtained, no scanning before reset.
    if (!isScanSuccess) {
      if (this.serverNode != null) {
        serverNode.getAnchor().detach();
        serverNode.setParent(null);
        serverNode.setRenderable(null);
        serverNode = null;
      }
      // Send frame image and scan barcode.
      try (final Image image = arFragment.getArSceneView().getArFrame().acquireCameraImage()) {
        if (image.getFormat() == ImageFormat.YUV_420_888) {
          Bitmap bitmapImage = YUV420toBitmap.getBitmap(image);
          BarcodeScan barScanning = new BarcodeScan();
          InputImage inputImage = InputImage.fromBitmap(bitmapImage, 0);

          Context context = this.getApplicationContext();

          barScanning.scanBarcodes(inputImage, fc, context, serverTextview);
          image.close();
        }
      } catch (NotYetAvailableException e) {
        Log.e("Barcode", e.getMessage());
      }
    }

    isScanSuccess = (serverTextview.getText().toString().length() != 0);
    // Image recognition after barcode scanning.
    if (isScanSuccess) {
      // Put the server's information near the barcode.
      if (this.serverNode == null) {
        // float[] pos = {barcodePos.right, barcodePos.top, -1};
        float[] pos = {0, 0, -1};
        float[] rotation = {0, 0, 0, 1};
        Anchor anchor = session.createAnchor(new Pose(pos, rotation));
        serverNode = new AnchorNode(anchor);
        serverNode.setRenderable(serverRenderable);
        serverNode.setParent(arFragment.getArSceneView().getScene());
      }

      Collection<AugmentedImage> updatedAugmentedImages =
          frame.getUpdatedTrackables(AugmentedImage.class);
      for (AugmentedImage augmentedImage : updatedAugmentedImages) {
        switch (augmentedImage.getTrackingState()) {
          case PAUSED:
            // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
            // but not yet tracked.
            String text = "Detected Image " + augmentedImage.getIndex();
            SnackbarHelper.getInstance().showMessage(this, text);
            break;

          case TRACKING:
            // Have to switch to UI Thread to update View.
            fitToScanView.setVisibility(View.GONE);

            // Create a new anchor for newly found images.
            if (!augmentedImageMap.containsKey(augmentedImage)) {
              AugmentedImageNode node = new AugmentedImageNode();
              node.setImage(augmentedImage, this);
              augmentedImageMap.put(augmentedImage, node);
              arFragment.getArSceneView().getScene().addChild(node);

              /*// Create the anchor near the cabinet to show the text.
              // 传fc, context, cabinetTextview
              if (this.cabinetNode == null) {
                float[] pos = {0, 0.0f, 0.5f * augmentedImage.getExtentZ()};
                float[] rotation = {0, 0, 0, 1};
                Anchor anchor = session.createAnchor(new Pose(pos, rotation));
                cabinetNode = new AnchorNode(anchor);
                cabinetNode.setRenderable(cabinetRenderable);
                cabinetNode.setParent(arFragment.getArSceneView().getScene());
              }*/
            }
            break;

          case STOPPED:
            augmentedImageMap.remove(augmentedImage);
            break;
        }
      }
    }
  }
}
