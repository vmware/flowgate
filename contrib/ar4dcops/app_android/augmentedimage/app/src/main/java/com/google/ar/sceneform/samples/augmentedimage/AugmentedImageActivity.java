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
import android.media.Image;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.samples.augmentedimage.flowgate.flowgateClient;
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
public class AugmentedImageActivity extends AppCompatActivity implements AugmentedImageFragment.OnCompleteListener {
  private ArFragment arFragment;
  private TextView serverTextview;
  private ViewRenderable serverRenderable;
  private AnchorNode serverNode;
  private Boolean isScanSuccess = false;
  private Boolean isPause = true;

  flowgateClient fc = new flowgateClient("202.121.180.32", "admin", "Ar_InDataCenter_450");

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Create new fragment and transaction
    arFragment = new AugmentedImageFragment();
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

    // Replace whatever is in the fragment_container view with this fragment,
    // and add the transaction to the back stack
    transaction.replace(R.id.ux_fragment, arFragment);
    transaction.addToBackStack(null);
    // Commit the transaction
    transaction.commit();

    // arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

    Button reset = findViewById(R.id.button1);
    // Reset the app status.
    reset.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isScanSuccess = false;
        serverNode = null;
        if (serverTextview != null){
          serverTextview.setText("");
        }
        TextView dialog = findViewById(R.id.disp1);
        String text = "Resetting";
        dialog.setText(text);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.detach(arFragment);
        transaction.attach(arFragment);
        transaction.commit();
      }
    });

    Button pause = findViewById(R.id.button2);
    // Pause/continue the current session.
    pause.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String text;
        if (isPause){
          onPause();
          text = "Continue";
        } else {
          onResume();
          text = "Pause";
        }
        isPause = !isPause;
        pause.setText(text);
      }
    });
  }

  @Override
  public void onComplete() {
    TextView dialog = findViewById(R.id.disp1);
    String text = "Detecting";
    dialog.setText(text);
    // Build the 2D renderable for text views of server.
    ViewRenderable.builder()
        .setView(this, R.layout.server_view)
        .build()
        .thenAccept(renderable -> serverRenderable = renderable);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
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

    // Let the fragment update its state first.
    arFragment.onUpdate(frameTime);

    // If ARCore is not tracking yet, then don't process anything.
    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }

    serverTextview = (TextView) serverRenderable.getView();

    // Scan once; and once the information is obtained, no scanning before reset.
    if (!isScanSuccess) {
      /*if (this.serverNode != null) {
        serverNode.getAnchor().detach();
        serverNode.setParent(null);
        serverNode.setRenderable(null);
        serverNode = null;
        augmentedImageMap.forEach((image, node) -> {
          arFragment.getArSceneView().getScene().removeChild(node);
          node = null;
        });
        augmentedImageMap.clear();
      }*/
      // Send frame image and scan barcode.
      try (final Image image = arFragment.getArSceneView().getArFrame().acquireCameraImage()) {
        if (image.getFormat() == ImageFormat.YUV_420_888) {
          Bitmap bitmapImage = YUV420toBitmap.getBitmap(image);
          BarcodeScan barScanning = new BarcodeScan();
          InputImage inputImage = InputImage.fromBitmap(bitmapImage, 0);

          Context context = this.getApplicationContext();

          TextView dialog = findViewById(R.id.disp1);
          barScanning.scanBarcodes(inputImage, fc, context, serverTextview, dialog);
          image.close();
        }
      } catch (NotYetAvailableException e) {
        Log.e("NYA", "Not yet available");
        e.printStackTrace();
      }
    }

    isScanSuccess = (serverTextview.getText().toString().length() != 0);

    // Image recognition after barcode scanning.
    if (isScanSuccess) {
      Collection<AugmentedImage> updatedAugmentedImages =
          frame.getUpdatedTrackables(AugmentedImage.class);
      for (AugmentedImage augmentedImage : updatedAugmentedImages) {
        TextView dialog = findViewById(R.id.disp1);
        String text;
        switch (augmentedImage.getTrackingState()) {
          case PAUSED:
            // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
            // but not yet tracked.
            text = "Detected the paused Image " + augmentedImage.getIndex();
            dialog.setText(text);
            break;

          case TRACKING:
            // Create a new anchor for newly found images.
            if (!augmentedImageMap.containsKey(augmentedImage)) {
              AugmentedImageNode node = new AugmentedImageNode();
              node.setImage(augmentedImage, this);
              augmentedImageMap.put(augmentedImage, node);
              arFragment.getArSceneView().getScene().addChild(node);
              text = "Device detected and frame drawn";
              dialog.setText(text);

              // Put the server's information near the cabinet.
              if (this.serverNode == null) {
                float[] pos = {augmentedImage.getCenterPose().tx() + augmentedImage.getExtentX(), augmentedImage.getCenterPose().ty(), augmentedImage.getCenterPose().tz()};
                float[] rotation = {0, 0, 0, 90};
                Anchor anchor = session.createAnchor(new Pose(pos, rotation));
                serverNode = new AnchorNode(anchor);
                serverNode.setRenderable(serverRenderable);
                serverNode.setParent(arFragment.getArSceneView().getScene());
              }
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
