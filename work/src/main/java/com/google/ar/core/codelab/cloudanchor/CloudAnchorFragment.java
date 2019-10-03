/*
 * Copyright 2019 Google Inc. All Rights Reserved.
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

package com.google.ar.core.codelab.cloudanchor;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Session;
import com.google.ar.core.codelab.cloudanchor.helpers.CloudAnchorManager;
import com.google.ar.core.codelab.cloudanchor.helpers.SnackbarHelper;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.core.Config.CloudAnchorMode;

/**
 * Main Fragment for the Cloud Anchors Codelab.
 *
 * <p>This is where the AR Session and the Cloud Anchors are managed.
 */
public class CloudAnchorFragment extends ArFragment {

  private Scene arScene;
  private AnchorNode anchorNode;
  private ModelRenderable andyRenderable;
  private final CloudAnchorManager cloudAnchorManager = new CloudAnchorManager(); ///Bunz
  private final SnackbarHelper snackbarHelper = new SnackbarHelper(); //Bunz

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void onAttach(Context context) {
    super.onAttach(context);
    ModelRenderable.builder()
        //.setSource(context, R.raw.andy)
            .setSource(context, R.raw.fence)
        .build()
        .thenAccept(renderable -> andyRenderable = renderable);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    // Inflate from the Layout XML file.
    View rootView = inflater.inflate(R.layout.cloud_anchor_fragment, container, false);
    LinearLayout arContainer = rootView.findViewById(R.id.ar_container);

    // Call the ArFragment's implementation to get the AR View.
    View arView = super.onCreateView(inflater, arContainer, savedInstanceState);
    arContainer.addView(arView);

    Button clearButton = rootView.findViewById(R.id.clear_button);
    clearButton.setOnClickListener(v -> onClearButtonPressed());

    arScene = getArSceneView().getScene();
    arScene.addOnUpdateListener(frameTime -> cloudAnchorManager.onUpdate());////Bunz
    setOnTapArPlaneListener((hitResult, plane, motionEvent) -> onArPlaneTap(hitResult));
    return rootView;
  }

  private synchronized void onArPlaneTap(HitResult hitResult) {
    if (anchorNode != null) {
      // Do nothing if there was already an anchor in the Scene.
      Anchor anchor = hitResult.createAnchor();
      setNewAnchor(anchor);
      //return;
      ////BUNZ
    }
    Anchor anchor = hitResult.createAnchor();
    setNewAnchor(anchor);
    /////BUNZ
    snackbarHelper.showMessage(getActivity(), "Now hosting anchor...");
    cloudAnchorManager.hostCloudAnchor(
            getArSceneView().getSession(), anchor, this::onHostedAnchorAvailable);
  }

  private synchronized void onClearButtonPressed() {
    // Clear the anchor from the scene.
    cloudAnchorManager.clearListeners();/////Bunz
    setNewAnchor(null);
  }

  // Modify the renderables when a new anchor is available.
  private synchronized void setNewAnchor(@Nullable Anchor anchor) {
    if (anchorNode != null) {
      // If an AnchorNode existed before, remove and nullify it.
      //arScene.removeChild(anchorNode);
      anchorNode = null;
    }
    if (anchor != null) {
      if (andyRenderable == null) {
        // Display an error message if the renderable model was not available.
        Toast toast = Toast.makeText(getContext(), "Andy model was not loaded.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        return;
      }
      // Create the Anchor.
      anchorNode = new AnchorNode(anchor);
      arScene.addChild(anchorNode);

      // Create the transformable andy and add it to the anchor.
      TransformableNode andy = new TransformableNode(getTransformationSystem());

      andy.getScaleController().setMinScale(10f);
      andy.getScaleController().setMaxScale(20f);

      // Set the local scale of the node BEFORE setting its parent
      andy.setLocalScale(new Vector3(10f, 10f, 10f));
      andy.setParent(anchorNode);
      andy.setRenderable(andyRenderable);
      andy.select();
    }
  }
  /////Bunz
  @Override
  protected Config getSessionConfiguration(Session session) {
    Config config = super.getSessionConfiguration(session);
    config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
    return config;
  }
  ///////BUNZ
  private synchronized void onHostedAnchorAvailable(Anchor anchor) {
    Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
    if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
      snackbarHelper.showMessage(
              getActivity(), "Cloud Anchor Hosted. ID: " + anchor.getCloudAnchorId());
      setNewAnchor(anchor);
    } else {
      snackbarHelper.showMessage(getActivity(), "Error while hosting: " + cloudState.toString());
    }
  }
}
