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
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableDefinition;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Vertex;
import com.google.ar.sceneform.utilities.AndroidPreconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  // Two kinds of bars: one vertical, one horizontal;
  // two vertical bars & two horizontal bars form a frame
  private static ModelRenderable[] bar = new ModelRenderable[2];
  private static ModelRenderable[] dividerBar = new ModelRenderable[1];

  public AugmentedImageNode() {
    // Do nothing upon construction
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image, Context context) {
    this.image = image;

    Log.d(TAG, "setImage begins");

    setAnchor(image.createAnchor(image.getCenterPose()));

    // width of frame bar
    float frameWidth = image.getExtentX()/20;
    ArrayList<Integer> triangleIndices = getTriangleIndices();
    // position of vertices of bars
    ArrayList<Vertex>[] vertices= new ArrayList[2];
    vertices[0] = getCubeVertices(new Vector3(image.getExtentX() + 2 * frameWidth, 0.0f, frameWidth), new Vector3(0.0f, 0.0f, 0.0f));
    vertices[1] = getCubeVertices(new Vector3(frameWidth, 0.0f, image.getExtentZ() + 2 * frameWidth), new Vector3(0.0f, 0.0f, 0.0f));
    ArrayList<Vertex> dividerVertices = getCubeVertices(new Vector3(image.getExtentX(), 0.0f, frameWidth/5), new Vector3(0.0f, 0.0f, 0.0f));
    // color of frame
    Color c = new Color(android.graphics.Color.BLUE);
    MaterialFactory.makeTransparentWithColor(context, c).thenAccept(
            // get the bars
            material -> {
              Log.d(TAG, "materials set");

              RenderableDefinition.Submesh submesh =
                      RenderableDefinition.Submesh.builder().setTriangleIndices(triangleIndices).setMaterial(material).build();

              RenderableDefinition[] renderableDefinition = new RenderableDefinition[2];
              renderableDefinition[0] =
                      RenderableDefinition.builder()
                              .setVertices(vertices[0])
                              .setSubmeshes(Arrays.asList(submesh))
                              .build();
              renderableDefinition[1] =
                      RenderableDefinition.builder()
                              .setVertices(vertices[1])
                              .setSubmeshes(Arrays.asList(submesh))
                              .build();
              RenderableDefinition[] dividerRenderableDefinition = new RenderableDefinition[1];
              dividerRenderableDefinition[0] =
                      RenderableDefinition.builder()
                              .setVertices(dividerVertices)
                              .setSubmeshes(Arrays.asList(submesh))
                              .build();

              try {

                bar[0] = ModelRenderable.builder().setSource(renderableDefinition[0]).build().get();
                bar[1] = ModelRenderable.builder().setSource(renderableDefinition[1]).build().get();
                dividerBar[0] = ModelRenderable.builder().setSource(dividerRenderableDefinition[0]).build().get();
              } catch (ExecutionException e) {
                Log.e(TAG, "ExecutionException when set image");
                e.printStackTrace();
              } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException when set image");
                e.printStackTrace();
              }

              // draw the frame
              Vector3 localPosition = new Vector3();
              Node cornerNode;

              localPosition.set(0, 0.0f, 0.5f * image.getExtentZ());
              cornerNode = new Node();
              cornerNode.setParent(this);
              cornerNode.setLocalPosition(localPosition);
              cornerNode.setRenderable(bar[0]);

              localPosition.set(0, 0.0f, -0.5f * image.getExtentZ());
              cornerNode = new Node();
              cornerNode.setParent(this);
              cornerNode.setLocalPosition(localPosition);
              cornerNode.setRenderable(bar[0]);

              localPosition.set(0.5f * image.getExtentX(), 0.0f, 0);
              cornerNode = new Node();
              cornerNode.setParent(this);
              cornerNode.setLocalPosition(localPosition);
              cornerNode.setRenderable(bar[1]);

              localPosition.set(-0.5f * image.getExtentX(), 0.0f, 0);
              cornerNode = new Node();
              cornerNode.setParent(this);
              cornerNode.setLocalPosition(localPosition);
              cornerNode.setRenderable(bar[1]);


              float intervalNum = 14;
              float interval = image.getExtentZ()/intervalNum;
              float bottom = -0.5f * image.getExtentZ() + interval;

              for(int i = 1; i < intervalNum; i++){
                  localPosition.set(0, 0.0f, bottom + i * interval);
                  cornerNode = new Node();
                  cornerNode.setParent(this);
                  cornerNode.setLocalPosition(localPosition);
                  cornerNode.setRenderable(dividerBar[0]);
              }

            });
  }

  public AugmentedImage getImage() {
    return image;
  }

  // copy from ShapeFactory
  // get eight vertices of the cube according to its size and center
  private static ArrayList<Vertex> getCubeVertices(Vector3 size, Vector3 center){
    AndroidPreconditions.checkMinAndroidApiLevel();

    Vector3 extents = size.scaled(0.5f);

    Vector3 p0 = Vector3.add(center, new Vector3(-extents.x, -extents.y, extents.z));
    Vector3 p1 = Vector3.add(center, new Vector3(extents.x, -extents.y, extents.z));
    Vector3 p2 = Vector3.add(center, new Vector3(extents.x, -extents.y, -extents.z));
    Vector3 p3 = Vector3.add(center, new Vector3(-extents.x, -extents.y, -extents.z));
    Vector3 p4 = Vector3.add(center, new Vector3(-extents.x, extents.y, extents.z));
    Vector3 p5 = Vector3.add(center, new Vector3(extents.x, extents.y, extents.z));
    Vector3 p6 = Vector3.add(center, new Vector3(extents.x, extents.y, -extents.z));
    Vector3 p7 = Vector3.add(center, new Vector3(-extents.x, extents.y, -extents.z));

    Vector3 up = Vector3.up();
    Vector3 down = Vector3.down();
    Vector3 front = Vector3.forward();
    Vector3 back = Vector3.back();
    Vector3 left = Vector3.left();
    Vector3 right = Vector3.right();

    Vertex.UvCoordinate uv00 = new Vertex.UvCoordinate(0.0f, 0.0f);
    Vertex.UvCoordinate uv10 = new Vertex.UvCoordinate(1.0f, 0.0f);
    Vertex.UvCoordinate uv01 = new Vertex.UvCoordinate(0.0f, 1.0f);
    Vertex.UvCoordinate uv11 = new Vertex.UvCoordinate(1.0f, 1.0f);

    ArrayList<Vertex> vertices =
            new ArrayList<>(
                    Arrays.asList(
                            // Bottom
                            Vertex.builder().setPosition(p0).setNormal(down).setUvCoordinate(uv01).build(),
                            Vertex.builder().setPosition(p1).setNormal(down).setUvCoordinate(uv11).build(),
                            Vertex.builder().setPosition(p2).setNormal(down).setUvCoordinate(uv10).build(),
                            Vertex.builder().setPosition(p3).setNormal(down).setUvCoordinate(uv00).build(),
                            // Left
                            Vertex.builder().setPosition(p7).setNormal(left).setUvCoordinate(uv01).build(),
                            Vertex.builder().setPosition(p4).setNormal(left).setUvCoordinate(uv11).build(),
                            Vertex.builder().setPosition(p0).setNormal(left).setUvCoordinate(uv10).build(),
                            Vertex.builder().setPosition(p3).setNormal(left).setUvCoordinate(uv00).build(),
                            // Front
                            Vertex.builder().setPosition(p4).setNormal(front).setUvCoordinate(uv01).build(),
                            Vertex.builder().setPosition(p5).setNormal(front).setUvCoordinate(uv11).build(),
                            Vertex.builder().setPosition(p1).setNormal(front).setUvCoordinate(uv10).build(),
                            Vertex.builder().setPosition(p0).setNormal(front).setUvCoordinate(uv00).build(),
                            // Back
                            Vertex.builder().setPosition(p6).setNormal(back).setUvCoordinate(uv01).build(),
                            Vertex.builder().setPosition(p7).setNormal(back).setUvCoordinate(uv11).build(),
                            Vertex.builder().setPosition(p3).setNormal(back).setUvCoordinate(uv10).build(),
                            Vertex.builder().setPosition(p2).setNormal(back).setUvCoordinate(uv00).build(),
                            // Right
                            Vertex.builder().setPosition(p5).setNormal(right).setUvCoordinate(uv01).build(),
                            Vertex.builder().setPosition(p6).setNormal(right).setUvCoordinate(uv11).build(),
                            Vertex.builder().setPosition(p2).setNormal(right).setUvCoordinate(uv10).build(),
                            Vertex.builder().setPosition(p1).setNormal(right).setUvCoordinate(uv00).build(),
                            // Top
                            Vertex.builder().setPosition(p7).setNormal(up).setUvCoordinate(uv01).build(),
                            Vertex.builder().setPosition(p6).setNormal(up).setUvCoordinate(uv11).build(),
                            Vertex.builder().setPosition(p5).setNormal(up).setUvCoordinate(uv10).build(),
                            Vertex.builder().setPosition(p4).setNormal(up).setUvCoordinate(uv00).build()));

    return vertices;
  }

  // copy from ShapeFactory
  private static ArrayList<Integer> getTriangleIndices(){
    final int COORDS_PER_TRIANGLE = 3;
    final int numSides = 6;
    final int verticesPerSide = 4;
    final int trianglesPerSide = 2;

    ArrayList<Integer> triangleIndices =
            new ArrayList<>(numSides * trianglesPerSide * COORDS_PER_TRIANGLE);
    for (int i = 0; i < numSides; i++) {
      // First triangle for this side.
      triangleIndices.add(3 + verticesPerSide * i);
      triangleIndices.add(1 + verticesPerSide * i);
      triangleIndices.add(0 + verticesPerSide * i);

      // Second triangle for this side.
      triangleIndices.add(3 + verticesPerSide * i);
      triangleIndices.add(2 + verticesPerSide * i);
      triangleIndices.add(1 + verticesPerSide * i);
    }
    return triangleIndices;
  }

}
