/*
 *
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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import butterknife.Bind;
import snapdicto.com.snapdicto.R;
import snapdicto.com.snapdicto.activities.PreferencesActivity;
import snapdicto.com.snapdicto.camera.ShutterButton;
import snapdicto.com.snapdicto.camera.CameraManager;
import snapdicto.com.snapdicto.ocrlib.OcrCharacterHelper;


/**
 * This activity opens the camera and does the actual scanning on a background thread.
 */
public final class MainActivity extends Activity implements SurfaceHolder.Callback,
        ShutterButton.OnShutterButtonListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private MainActivityHandler handler;
    private boolean hasSurface;
    private SharedPreferences prefs;
    private boolean isEngineReady;
    private CameraManager cameraManager;
    /* USING butter knife Dependency INJECTION */
    @Bind(R.id.shutter_button)
    ShutterButton shutterButton;
    @Bind(R.id.camera_button_view)
    View cameraButtonView;
    @Bind(R.id.result_view)
    View resultView;

    @Bind(R.id.status_view_top)
    TextView statusViewTop;

    MainActivityHandler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    /** Flag to enable display of the on-screen shutter button. */
    private static final boolean DISPLAY_SHUTTER_BUTTON = true;

    // Note: These constants will be overridden by any default values defined in preferences.xml.

    /** ISO 639-3 language code indicating the default recognition language. */
    public static final String DEFAULT_SOURCE_LANGUAGE_CODE = "eng";

    /** ISO 639-1 language code indicating the default target language for translation. */
    public static final String DEFAULT_TARGET_LANGUAGE_CODE = "es";

    /** The default online machine translation service to use. */
    public static final String DEFAULT_TRANSLATOR = "Google Translate";

    /** The default OCR engine to use. */
    public static final String DEFAULT_OCR_ENGINE_MODE = "Tesseract";

    /** The default page segmentation mode to use. */
    public static final String DEFAULT_PAGE_SEGMENTATION_MODE = "Auto";

    /** Whether to use autofocus by default. */
    public static final boolean DEFAULT_TOGGLE_AUTO_FOCUS = true;

    /** Whether to initially disable continuous-picture and continuous-video focus modes. */
    public static final boolean DEFAULT_DISABLE_CONTINUOUS_FOCUS = true;

    /** Whether to beep by default when the shutter button is pressed. */
    public static final boolean DEFAULT_TOGGLE_BEEP = false;

    /** Whether to initially show a looping, real-time OCR display. */
    public static final boolean DEFAULT_TOGGLE_CONTINUOUS = false;

    /** Whether to initially reverse the image returned by the camera. */
    public static final boolean DEFAULT_TOGGLE_REVERSED_IMAGE = false;

    /** Whether to enable the use of online translation services be default. */
    public static final boolean DEFAULT_TOGGLE_TRANSLATION = true;

    /** Whether the light should be initially activated by default. */
    public static final boolean DEFAULT_TOGGLE_LIGHT = false;


    /** Flag to display the real-time recognition results at the top of the scanning screen. */
    private static final boolean CONTINUOUS_DISPLAY_RECOGNIZED_TEXT = true;

    /** Flag to display recognition-related statistics on the scanning screen. */
    private static final boolean CONTINUOUS_DISPLAY_METADATA = true;

    /** Flag to enable display of the on-screen shutter button. */
    private static final boolean DISPLAY_SHUTTER_BUTTON = true;

    /** Languages for which Cube data is available. */
    static final String[] CUBE_SUPPORTED_LANGUAGES = {
            "ara", // Arabic
            "eng", // English
            "hin" // Hindi
    };

    /** Languages that require Cube, and cannot run using Tesseract. */
    private static final String[] CUBE_REQUIRED_LANGUAGES = {
            "ara" // Arabic
    };

    /** Resource to use for data file downloads. */
    static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

    /** Download filename for orientation and script detection (OSD) data. */
    static final String OSD_FILENAME = "tesseract-ocr-3.01.osd.tar";

    /** Destination filename for orientation and script detection (OSD) data. */
    static final String OSD_FILENAME_BASE = "osd.traineddata";

    /** Minimum mean confidence score necessary to not reject single-shot OCR result. Currently unused. */
    static final int MINIMUM_MEAN_CONFIDENCE = 0; // 0 means don't reject any scored results

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

    /**
     * Sets default values for preferences. To be called the first time this app is run.
     */
    private void setDefaultPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Continuous preview
        prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, MainActivity.DEFAULT_TOGGLE_CONTINUOUS).commit();

        // Recognition language
        prefs.edit().putString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE).commit();

        // Translation
        prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, MainActivity.DEFAULT_TOGGLE_TRANSLATION).commit();

        // Translation target language
        prefs.edit().putString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_TARGET_LANGUAGE_CODE).commit();

        // Translator
        prefs.edit().putString(PreferencesActivity.KEY_TRANSLATOR, MainActivity.DEFAULT_TRANSLATOR).commit();

        // OCR Engine
        prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, MainActivity.DEFAULT_OCR_ENGINE_MODE).commit();

        // Autofocus
        prefs.edit().putBoolean(PreferencesActivity.KEY_AUTO_FOCUS, MainActivity.DEFAULT_TOGGLE_AUTO_FOCUS).commit();

        // Disable problematic focus modes
        prefs.edit().putBoolean(PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, MainActivity.DEFAULT_DISABLE_CONTINUOUS_FOCUS).commit();

        // Beep
        prefs.edit().putBoolean(PreferencesActivity.KEY_PLAY_BEEP, MainActivity.DEFAULT_TOGGLE_BEEP).commit();

        // Character blacklist
        prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_BLACKLIST,
                OcrCharacterHelper.getDefaultBlacklist(MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();

        // Character whitelist
        prefs.edit().putString(PreferencesActivity.KEY_CHARACTER_WHITELIST,
                OcrCharacterHelper.getDefaultWhitelist(MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE)).commit();

        // Page segmentation mode
        prefs.edit().putString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, MainActivity.DEFAULT_PAGE_SEGMENTATION_MODE).commit();

        // Reversed camera image
        prefs.edit().putBoolean(PreferencesActivity.KEY_REVERSE_IMAGE, MainActivity.DEFAULT_TOGGLE_REVERSED_IMAGE).commit();

        // Light
        prefs.edit().putBoolean(PreferencesActivity.KEY_TOGGLE_LIGHT, MainActivity.DEFAULT_TOGGLE_LIGHT).commit();
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

    /** Initializes the camera and starts the handler to begin previewing. */
    private void initCamera(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "initCamera()");
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        try {

            // Open and initialize the camera
            cameraManager.openDriver(surfaceHolder);

            // Creating the handler starts the preview, which can also throw a RuntimeException.
            handler = new MainActivityHandler(this, cameraManager, isContinuousModeActive);

        } catch (IOException ioe) {
            showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
        }

        // Stop using the camera, to avoid conflicting with other camera-based apps
        cameraManager.closeDriver();

        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { hasSurface = false; }

    @Override
    public void onShutterButtonClick(ShutterButton b) {

    }
}