/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
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

package snapdicto.com.snapdicto.UI;

import android.app.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.Bind;
import snapdicto.com.snapdicto.R;
import snapdicto.com.snapdicto.camera.ShutterButton;


/**
 * This activity opens the camera and does the actual scanning on a background thread.
 */
public final class MainActivity extends Activity implements SurfaceHolder.Callback,
        ShutterButton.OnShutterButtonListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean hasSurface;
    private boolean isEngineReady;
    /* USING butter knife Dependency INJECTION */
    @Bind(R.id.shutter_button)
    ShutterButton shutterButton;
    @Bind(R.id.camera_button_view)
    View cameraButtonView;
    @Bind(R.id.result_view)
    View resultView;

    @Bind(R.id.status_view_top)
    TextView statusViewTop;


    /** Flag to enable display of the on-screen shutter button. */
    private static final boolean DISPLAY_SHUTTER_BUTTON = true;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        registerForContextMenu(statusViewTop);
        hasSurface = false;

        isEngineReady = false;



        // Camera shutter button
        if (DISPLAY_SHUTTER_BUTTON) {

            shutterButton.setOnShutterButtonListener(this);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");

        if (holder == null) {
            Log.e(TAG, "surfaceCreated gave us a null surface");
        }

        // Only initialize the camera if the OCR engine is ready to go.
        if (!hasSurface && isEngineReady) {
            Log.d(TAG, "surfaceCreated(): calling initCamera()...");

        }
        hasSurface = true;

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onShutterButtonClick(ShutterButton b) {

    }
}