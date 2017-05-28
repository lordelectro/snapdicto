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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import snapdicto.com.snapdicto.R;
import snapdicto.com.snapdicto.activities.PreferencesActivity;
import snapdicto.com.snapdicto.camera.ShutterButton;
import snapdicto.com.snapdicto.camera.CameraManager;
import snapdicto.com.snapdicto.handler.DecodeHandler;
import snapdicto.com.snapdicto.handler.MainActivityHandler;
import snapdicto.com.snapdicto.imageprocessing.language.LanguageCodeHelper;
import snapdicto.com.snapdicto.ocrlib.OcrCharacterHelper;
import snapdicto.com.snapdicto.ocrlib.OcrInitAsyncTask;
import snapdicto.com.snapdicto.ocrlib.OcrResult;
import snapdicto.com.snapdicto.ocrlib.OcrResultFailure;
import snapdicto.com.snapdicto.ocrlib.OcrResultText;

import static android.R.id.message;


/**
 * This activity opens the camera and does the actual scanning on a background thread.
 */
public final class MainActivity extends Activity implements SurfaceHolder.Callback,
        ShutterButton.OnShutterButtonListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    //    private MainActivityHandler handler;
//    private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
//    private boolean hasSurface;
//    private SharedPreferences prefs;
//    private boolean isEngineReady;
//    private BeepManager beepManager;
//    private CameraManager cameraManager;
    // Options menu, for copy to clipboard
    private static final int OPTIONS_COPY_RECOGNIZED_TEXT_ID = Menu.FIRST;
    private static final int OPTIONS_COPY_TRANSLATED_TEXT_ID = Menu.FIRST + 1;
    private static final int OPTIONS_SHARE_RECOGNIZED_TEXT_ID = Menu.FIRST + 2;
    private static final int OPTIONS_SHARE_TRANSLATED_TEXT_ID = Menu.FIRST + 3;

    private CameraManager cameraManager;
    private MainActivityHandler handler;
    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView statusViewBottom;
    private TextView statusViewMeaning;
    private TextView statusViewOtherWord;
    //private TextView statusViewTop;
    private TextView ocrResultView;
    private TextView translationView;
    // private View cameraButtonView;
    //   private View resultView;
    private View progressView;
    private OcrResult lastResult;
    private Bitmap lastBitmap;
    private boolean hasSurface;
    private BeepManager beepManager;
    private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
    private String sourceLanguageCodeOcr; // ISO 639-3 language code
    private String sourceLanguageReadable; // Language name, for example, "English"
    private String sourceLanguageCodeTranslation; // ISO 639-1 language code
    private String targetLanguageCodeTranslation; // ISO 639-1 language code
    private String targetLanguageReadable; // Language name, for example, "English"
    private int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;


    public Handler getHandler() {
        return handler;
    }

    public TessBaseAPI getBaseApi() {
        return baseApi;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }


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

    // Note: These constants will be overridden by any default values defined in preferences.xml.

    /** ISO 639-3 language code indicating the default recognition language. */
    public static final String DEFAULT_SOURCE_LANGUAGE_CODE = "eng";

    /** ISO 639-1 language code indicating the default target language for translation. */
    public static final String DEFAULT_TARGET_LANGUAGE_CODE = "es";

    /** The default online machine translation service to use. */
    public static final String DEFAULT_TRANSLATOR = "Google Translate";

    /** The default OCR engine to use. */
    public static final String DEFAULT_OCR_ENGINE_MODE = "Tesseract";
    //public static final String DEFAULT_OCR_ENGINE_MODE = "Both";

    /** The default page segmentation mode to use. */
    public static final String DEFAULT_PAGE_SEGMENTATION_MODE = "Auto";

    /** Whether to use autofocus by default. */
    public static final boolean DEFAULT_TOGGLE_AUTO_FOCUS = true;

    /** Whether to initially disable continuous-picture and continuous-video focus modes. */
    public static final boolean DEFAULT_DISABLE_CONTINUOUS_FOCUS = true;

    /** Whether to beep by default when the shutter button is pressed. */
    public static final boolean DEFAULT_TOGGLE_BEEP = true;

    /** Whether to initially show a looping, real-time OCR display. */
    public static final boolean DEFAULT_TOGGLE_CONTINUOUS = true;

    /** Whether to initially reverse the image returned by the camera. */
    public static final boolean DEFAULT_TOGGLE_REVERSED_IMAGE = false;

    /** Whether to enable the use of online translation services be default. */
    public static final boolean DEFAULT_TOGGLE_TRANSLATION = true;

    /** Whether the light should be initially activated by default. */
    public static final boolean DEFAULT_TOGGLE_LIGHT = true;


    /** Flag to display the real-time recognition results at the top of the scanning screen. */
    private static final boolean CONTINUOUS_DISPLAY_RECOGNIZED_TEXT = true;

    /** Flag to display recognition-related statistics on the scanning screen. */
    private static final boolean CONTINUOUS_DISPLAY_METADATA = true;




    /** Languages for which Cube data is available. */
    public static final String[] CUBE_SUPPORTED_LANGUAGES = {
            "ara", // Arabic
            "eng", // English
            "hin" // Hindi
    };

    /** Languages that require Cube, and cannot run using Tesseract. */
    private static final String[] CUBE_REQUIRED_LANGUAGES = {
            "ara" // Arabic
    };

    /** Resource to use for data file downloads. */
    public static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

    /** Download filename for orientation and script detection (OSD) data. */
    public static final String OSD_FILENAME = "tesseract-ocr-3.02.osd.tar";

    /** Destination filename for orientation and script detection (OSD) data. */
    public static final String OSD_FILENAME_BASE = "osd.traineddata";

    /** Minimum mean confidence score necessary to not reject single-shot OCR result. Currently unused. */
    static final int MINIMUM_MEAN_CONFIDENCE = 0; // 0 means don't reject any scored results


    private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
    //private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED;
    private String characterBlacklist;
    private String characterWhitelist;
    // private ShutterButton shutterButton;
    private boolean isTranslationActive; // Whether we want to show translations
    private boolean isContinuousModeActive; // Whether we are doing OCR in continuous mode
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private ProgressDialog dialog; // for initOcr - language download & unzip
    private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
    private boolean isEngineReady;
    private boolean isPaused;
    private static boolean isFirstLaunch; // True if this is the first time the app is being run

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);


        //stating the view to display meaning.
        statusViewMeaning = (TextView) findViewById(R.id.ocr_dicto_text_view);
        registerForContextMenu(statusViewMeaning);

        //stating the view to display other word to mean sameword.
        statusViewOtherWord = (TextView) findViewById(R.id.ocr_othermeaning_text_view);
        registerForContextMenu(statusViewOtherWord);


        cameraButtonView = findViewById(R.id.camera_button_view);

        resultView = findViewById(R.id.result_view);


        statusViewTop = (TextView) findViewById(R.id.status_view_top);

        registerForContextMenu(statusViewTop);
        hasSurface = false;

        isEngineReady = false;

        if (DISPLAY_SHUTTER_BUTTON) {

            //TODO replace with butter knife
            shutterButton = (ShutterButton) findViewById(R.id.shutter_button);



            shutterButton.setOnShutterButtonListener(this);
        }

        handler = null;
        lastResult = null;
        hasSurface = false;
        beepManager = new BeepManager(this);

        // Camera shutter button
        if (DISPLAY_SHUTTER_BUTTON) {
            // shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
            shutterButton.setOnShutterButtonListener(this);
        }

        ocrResultView = (TextView) findViewById(R.id.ocr_result_text_view);
        registerForContextMenu(ocrResultView);
        translationView = (TextView) findViewById(R.id.translation_text_view);
        registerForContextMenu(translationView);

        statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
        registerForContextMenu(statusViewBottom);
        statusViewTop = (TextView) findViewById(R.id.status_view_top);
        progressView = (View) findViewById(R.id.indeterminate_progress_indicator_view);

        cameraManager = new CameraManager(getApplication());
        viewfinderView.setCameraManager(cameraManager);

        // Set listener to change the size of the viewfinder rectangle.

        //Sets adjustable size of the view finder interface.
        viewfinderView.setOnTouchListener(new View.OnTouchListener() {
            int lastX = -1;
            int lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = -1;
                        lastY = -1;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();

                        try {
                            Rect rect = cameraManager.getFramingRect();

                            final int BUFFER = 50;
                            final int BIG_BUFFER = 60;
                            if (lastX >= 0) {
                                // Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
                                if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top left corner: adjust both top and left sides
                                    cameraManager.adjustFramingRect( 2 * (lastX - currentX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top right corner: adjust both top and right sides
                                    cameraManager.adjustFramingRect( 2 * (currentX - lastX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom left corner: adjust both bottom and left sides
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom right corner: adjust both bottom and right sides
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
                                    cameraManager.adjustFramingRect(0, 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
                                    cameraManager.adjustFramingRect(0, 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                }
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Framing rect not available", e);
                        }
                        v.invalidate();
                        lastX = currentX;
                        lastY = currentY;
                        return true;
                    case MotionEvent.ACTION_UP:
                        lastX = -1;
                        lastY = -1;
                        return true;
                }
                return false;
            }
        });

        isEngineReady = false;
    }


    @Override
    protected void onResume() {
        super.onResume();
        resetStatusView();

        String previousSourceLanguageCodeOcr = sourceLanguageCodeOcr;
        int previousOcrEngineMode = ocrEngineMode;

        retrievePreferences();

        // Set up the camera preview surface.
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();
        if (!hasSurface) {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        // Comment out the following block to test non-OCR functions without an SD card

        // Do OCR engine initialization, if necessary
        boolean doNewInit = (baseApi == null) || !sourceLanguageCodeOcr.equals(previousSourceLanguageCodeOcr) ||
                ocrEngineMode != previousOcrEngineMode;
        if (doNewInit) {
            // Initialize the OCR engine
            File storageDirectory = getStorageDirectory();
            if (storageDirectory != null) {
                initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
            }
        } else {
            // We already have the engine initialized, so just start the camera.
            resumeOCR();
        }
    }

    /**
     * Method to start or restart recognition after the OCR engine has been initialized,
     * or after the app regains focus. Sets state related settings and OCR engine parameters,
     * and requests camera initialization.
     */
    public void resumeOCR() {
        Log.d(TAG, "resumeOCR()");

        // This method is called when Tesseract has already been successfully initialized, so set
        // isEngineReady = true here.
        isEngineReady = true;

        isPaused = false;

        if (handler != null) {
            handler.resetState();
        }
        if (baseApi != null) {
            baseApi.setPageSegMode(pageSegmentationMode);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, characterBlacklist);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, characterWhitelist);
        }

        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        }
    }

    /** Called when the shutter button is pressed in continuous mode. */
    void onShutterButtonPressContinuous() {
        isPaused = true;
        handler.stop();
        beepManager.playBeepSoundAndVibrate();

        //if the returned result is a success
        if (lastResult != null) {
            handleOcrDecode(lastResult);
            //get dictionary meaning of text
            getmeaning(lastResult);
            //getothermeaning(lastResult);

            if(lastResult != null && statusViewMeaning.getText().toString() != null){
                getothermeaning(lastResult);
            }
           //getothermeaning(lastResult);

        } else {
            Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            resumeContinuousDecoding();
        }
    }

    /** Called to resume recognition after translation in continuous mode. */
    @SuppressWarnings("unused")
    void resumeContinuousDecoding() {
        isPaused = false;
        resetStatusView();
        setStatusViewForContinuous();
        DecodeHandler.resetDecodeState();
        handler.resetState();
        if (shutterButton != null && DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
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
            initCamera(holder);
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

    public void stopHandler() {
        if (handler != null) {
            handler.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (baseApi != null) {
            baseApi.end();
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // First check if we're paused in continuous mode, and if so, just unpause.
            if (isPaused) {
                Log.d(TAG, "only resuming continuous recognition, not quitting...");
                resumeContinuousDecoding();
                return true;
            }

            // Exit the app if we're not viewing an OCR result.
            if (lastResult == null) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            } else {
                // Go back to previewing in regular OCR mode.
                resetStatusView();
                if (handler != null) {
                    handler.sendEmptyMessage(R.id.restart_preview);
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (isContinuousModeActive) {
                onShutterButtonPressContinuous();
            } else {
                handler.hardwareShutterButtonClick();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            // Only perform autofocus if user is not holding down the button.
            if (event.getRepeatCount() == 0) {
                cameraManager.requestAutoFocus(500L);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /** Sets the necessary language code values for the given OCR language. */
    private boolean setSourceLanguage(String languageCode) {
        sourceLanguageCodeOcr = languageCode;
        sourceLanguageCodeTranslation = LanguageCodeHelper.mapLanguageCode(languageCode);
        sourceLanguageReadable = LanguageCodeHelper.getOcrLanguageName(this, languageCode);
        return true;
    }

    /** Sets the necessary language code values for the translation target language. */
    private boolean setTargetLanguage(String languageCode) {
        targetLanguageCodeTranslation = languageCode;
        targetLanguageReadable = LanguageCodeHelper.getTranslationLanguageName(this, languageCode);
        return true;
    }

    /** Finds the proper location on the SD card where we can save files. */
    private File getStorageDirectory() {
        //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));

        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            Log.e(TAG, "Is the SD card visible?", e);
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            // We can read and write the media
            //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
            // For Android 2.2 and above

            try {
                return getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                Log.e(TAG, "External storage is unavailable");
                showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
            }

            //        } else {
            //          // For Android 2.1 and below, explicitly give the path as, for example,
            //          // "/mnt/sdcard/Android/data/edu.sfsu.cs.orange.ocr/files/"
            //          return new File(Environment.getExternalStorageDirectory().toString() + File.separator +
            //                  "Android" + File.separator + "data" + File.separator + getPackageName() +
            //                  File.separator + "files" + File.separator);
            //        }

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.e(TAG, "External storage is read-only");
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            Log.e(TAG, "External storage is unavailable");
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
        }
        return null;
    }

    /**
     * Requests initialization of the OCR engine with the given parameters.
     *
     * @param storageRoot Path to location of the tessdata directory to use
     * @param languageCode Three-letter ISO 639-3 language code for OCR
     * @param languageName Name of the language for OCR, for example, "English"
     */
    private void initOcrEngine(File storageRoot, String languageCode, String languageName) {
        isEngineReady = false;

        // Set up the dialog box for the thermometer-style download progress indicator
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = new ProgressDialog(this);

        // If we have a language that only runs using Cube, then set the ocrEngineMode to Cube
        if (ocrEngineMode != TessBaseAPI.OEM_CUBE_ONLY) {
            for (String s : CUBE_REQUIRED_LANGUAGES) {
                if (s.equals(languageCode)) {
                    ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
                }
            }
        }

        // If our language doesn't support Cube, then set the ocrEngineMode to Tesseract
        if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {
            boolean cubeOk = false;
            for (String s : CUBE_SUPPORTED_LANGUAGES) {
                if (s.equals(languageCode)) {
                    cubeOk = true;
                }
            }
            if (!cubeOk) {
                ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().putString(PreferencesActivity.KEY_OCR_ENGINE_MODE, getOcrEngineModeName()).commit();
            }
        }

        // Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        String ocrEngineModeName = getOcrEngineModeName();
        if (ocrEngineModeName.equals("Both")) {
            indeterminateDialog.setMessage("Initializing Cube and Tesseract OCR engines for " + languageName + "...");
        } else {
            indeterminateDialog.setMessage("Initializing " + ocrEngineModeName + " OCR engine for " + languageName + "...");
        }
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();

        if (handler != null) {
            handler.quitSynchronously();
        }

        // Disable continuous mode if we're using Cube. This will prevent bad states for devices
        // with low memory that crash when running OCR with Cube, and prevent unwanted delays.
        if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY || ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
            Log.d(TAG, "Disabling continuous preview");
            isContinuousModeActive = false;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, false);
        }

        // Start AsyncTask to install language data and init OCR
        baseApi = new TessBaseAPI();
        new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
                .execute(storageRoot.toString());
    }

    /**
     * Displays information relating to the result of OCR, and requests a translation if necessary.
     *
     * @param ocrResult Object representing successful OCR results
     * @return True if a non-null result was received for OCR
     */
    public boolean handleOcrDecode(OcrResult ocrResult) {
        String waiting = "Retrieving Meaning ...";
        String waiting_other_word = "Retrieving Other Word ...";
        lastResult = ocrResult;
        //Testing if there is a result retrieved. or No result
        Log.d("ocrResult", ocrResult.getText());
        // Test whether the result is null
        if (ocrResult.getText() == null || ocrResult.getText().equals("")) {

            //Display warning information
            Log.w("No-OCR-Result:",ocrResult.getText());
            //if there is no result: toast.
            Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return false;
        }

        // Turn off capture-related UI elements
        shutterButton.setVisibility(View.GONE);
        statusViewBottom.setVisibility(View.GONE);
        statusViewTop.setVisibility(View.GONE);
        cameraButtonView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        //display translated image
        ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);
        lastBitmap = ocrResult.getBitmap();
        if (lastBitmap == null) {
            bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher));
        } else {
            bitmapImageView.setImageBitmap(lastBitmap);
        }

        // Display the recognized text returned from recognition of image to text

        TextView sourceLanguageTextView = (TextView) findViewById(R.id.source_language_text_view);
        sourceLanguageTextView.setText(sourceLanguageReadable);

        TextView ocrResultTextView = (TextView) findViewById(R.id.ocr_result_text_view);
        ocrResultTextView.setText(ocrResult.getText());
        statusViewMeaning.setText(waiting);

        statusViewOtherWord.setText(waiting_other_word);

        //Testing what display has been displayed to user
        Log.d("ocrResult_Test View", ocrResult.getText());
        // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
        int scaledSize = Math.max(22, 32 - ocrResult.getText().length() / 4);
        ocrResultTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);



//        TextView translationLanguageLabelTextView = (TextView) findViewById(R.id.translation_language_label_text_view);
//        TextView translationLanguageTextView = (TextView) findViewById(R.id.translation_language_text_view);
        TextView translationTextView = (TextView) findViewById(R.id.translation_text_view);
        if (isTranslationActive) {
            // Handle translation text fields
//            translationLanguageLabelTextView.setVisibility(View.VISIBLE);
//            translationLanguageTextView.setText(targetLanguageReadable);
//            translationLanguageTextView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL), Typeface.NORMAL);
//            translationLanguageTextView.setVisibility(View.VISIBLE);

            // Activate/re-activate the indeterminate progress indicator
            translationTextView.setVisibility(View.GONE);
            progressView.setVisibility(View.VISIBLE);
            setProgressBarVisibility(true);

            // Get the translation asynchronously
            // new TranslateAsyncTask(this, sourceLanguageCodeTranslation, targetLanguageCodeTranslation,
            //   ocrResult.getText()).execute();
        } else {
//            translationLanguageLabelTextView.setVisibility(View.GONE);
//            translationLanguageTextView.setVisibility(View.GONE);
            translationTextView.setVisibility(View.GONE);
            progressView.setVisibility(View.GONE);
            setProgressBarVisibility(false);
        }
        return true;
    }


    //Retrieving Dictionary Meaning of the Result got from the image.
    private void getmeaning(final OcrResult ocrResult) {


        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder builder = new StringBuilder();
                lastResult = ocrResult;
                String fetchedresult = ocrResult.getText().toString();
                //final String retrieval = "Retrieving Meaning .... ";

                try {

                    //Using Jsoup Library for WebScripping for information.
                    Document doc = Jsoup.connect("http://www.dictionary.com/browse/"+fetchedresult).get();

                    //Using Jsoup Library for WebScripping for other word.
                    Document docOtherWord = Jsoup.connect("http://www.thesaurus.com/browse/"+fetchedresult).get();

                    //Testing Whether data is returned.
                    Log.d("Returned Data or Result", fetchedresult);


                    //selecting a specific div to be fetched
                    String Element = doc.select("div.def-content").first().text();

                    //String Element = doc.select("div.relevancy-list.ul.li.common-class").first().text();
                    //String ElementOtherWord = docOtherWord.select("a.common-word").first().text();


                    //Testing the value of Element returned.
                    Log.d("Hello", Element);

                    //String Builder to append the String.

                    builder.append(Element);
                    //builder.append(ElementOtherWord);


                } catch (IOException e) {
                    //e.printStackTrace();

                    Log.w("Error:" , e.getMessage());
                    //Display error.
                    builder.append("Error :").append(e.getMessage()).append("\n");


                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //Append Dictionary meaning of returned result.
                        statusViewMeaning.setText(builder.toString());

                        //Append Dictionary meaning of returned result.
                        //statusViewOtherWord.setText(builder.toString());

                        Log.i("Success:" , "Result successfully retrieved");


                    }
                });
            }
        }).start();


    }



    //Retrieving Dictionary Meaning of the Result got from the image.
    private void getothermeaning(final OcrResult ocrResult) {


        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder builder = new StringBuilder();
                lastResult = ocrResult;
                String fetchedresult = ocrResult.getText().toString();
                //final String retrieval = "Retrieving Meaning .... ";
//
                if(fetchedresult == null){
                    String found = "No Other Word Found";
                    builder.append(found);

                }

                try {




                    if(fetchedresult != null) {

                        //Using Jsoup Library for WebScripping for information.
                        Document doc = Jsoup.connect("http://www.thesaurus.com/browse/"+fetchedresult).get();

                        //Testing Whether data is returned.
                        Log.d("Returned Data or Result", fetchedresult);
                        //selecting a specific div to be fetched
                        //String Element = doc.select("div.relevancy-list.ul.li.common-class").first().text();
                        String Element = doc.select("a.common-word").first().text();


                        //Testing the value of Element returned.
                        Log.d("Value returned", Element);


                        //String Builder to append the String.

                        //builder.append(Element);

                        String s = Element.substring(0, Element.length() - 4);
                        builder.append(s);

                    }else{
                        String found = "No Other Word Found";
                        builder.append(found);

                    }



                } catch (IOException e) {
                    //e.printStackTrace();

                    Log.w("Error:" , e.getMessage());
                    //Display error.
                    builder.append("Error :").append(e.getMessage()).append("\n");


                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //Append Dictionary meaning of returned result.
                        statusViewOtherWord.setText("\n" + "Other Word:" +  builder.toString());



                        Log.i("Success:" , "Other Word successfully retrieved");

                    }
                });
            }
        }).start();


    }







    /**
     * Displays information relating to the results of a successful real-time OCR request.
     *
     * @param ocrResult Object representing successful OCR results
     */
    public void handleOcrContinuousDecode(OcrResult ocrResult) {

        lastResult = ocrResult;

        // Send an OcrResultText object to the ViewfinderView for text rendering
        viewfinderView.addResultText(new OcrResultText(ocrResult.getText(),
                ocrResult.getWordConfidences(),
                ocrResult.getMeanConfidence(),
                ocrResult.getBitmapDimensions(),
                ocrResult.getRegionBoundingBoxes(),
                ocrResult.getTextlineBoundingBoxes(),
                ocrResult.getStripBoundingBoxes(),
                ocrResult.getWordBoundingBoxes(),
                ocrResult.getCharacterBoundingBoxes()));

        Integer meanConfidence = ocrResult.getMeanConfidence();

        if (CONTINUOUS_DISPLAY_RECOGNIZED_TEXT) {
            // Display the recognized text on the screen
            statusViewTop.setText(ocrResult.getText());
            int scaledSize = Math.max(22, 32 - ocrResult.getText().length() / 4);
            statusViewTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
            statusViewTop.setTextColor(Color.BLACK);
            statusViewTop.setBackgroundResource(R.color.status_top_text_background);

            statusViewTop.getBackground().setAlpha(meanConfidence * (255 / 100));
            String RecognizedView = ocrResult.getText();
            Log.i("Recognized Text:", RecognizedView);
        }

        if (CONTINUOUS_DISPLAY_METADATA) {
            // Display recognition-related metadata at the bottom of the screen
            long recognitionTimeRequired = ocrResult.getRecognitionTimeRequired();
            statusViewBottom.setTextSize(14);
            statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - Mean confidence: " +
                    meanConfidence.toString() + " - Time required: " + recognitionTimeRequired + " ms");

            String meanconfidence = meanConfidence.toString();
            Log.i("Meanconfidence:", meanconfidence);

        }
    }

    /**
     * Version of handleOcrContinuousDecode for failed OCR requests. Displays a failure message.
     *
     * @param obj Metadata for the failed OCR request.
     */
    public void handleOcrContinuousDecode(OcrResultFailure obj) {
        lastResult = null;
        viewfinderView.removeResultText();

        // Reset the text in the recognized text box.
        statusViewTop.setText("");

        if (CONTINUOUS_DISPLAY_METADATA) {
            // Color text delimited by '-' as red.
            statusViewBottom.setTextSize(14);
            CharSequence cs = setSpanBetweenTokens("OCR: " + sourceLanguageReadable + " - OCR failed - Time required: "
                    + obj.getTimeRequired() + " ms", "-", new ForegroundColorSpan(0xFFFF0000));
            statusViewBottom.setText(cs);
        }
    }



    /**
     * Given either a Spannable String or a regular String and a token, apply
     * the given CharacterStyle to the span between the tokens.
     *
     * NOTE: This method was adapted from:
     *  http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
     *
     * <p>
     * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##", new
     * ForegroundColorSpan(0xFFFF0000));} will return a CharSequence {@code
     * "Hello world!"} with {@code world} in red.
     *
     */
    private CharSequence setSpanBetweenTokens(CharSequence text, String token,
                                              CharacterStyle... cs) {
        // Start and end refer to the points where the span will apply
        int tokenLen = token.length();
        int start = text.toString().indexOf(token) + tokenLen;
        int end = text.toString().indexOf(token, start);

        if (start > -1 && end > -1) {
            // Copy the spannable string to a mutable spannable string
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            for (CharacterStyle c : cs)
                ssb.setSpan(c, start, end, 0);
            text = ssb;
        }
        return text;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.equals(ocrResultView)) {
            menu.add(Menu.NONE, OPTIONS_COPY_RECOGNIZED_TEXT_ID, Menu.NONE, "Copy recognized text");
            menu.add(Menu.NONE, OPTIONS_SHARE_RECOGNIZED_TEXT_ID, Menu.NONE, "Share recognized text");
        } else if (v.equals(translationView)){
            menu.add(Menu.NONE, OPTIONS_COPY_TRANSLATED_TEXT_ID, Menu.NONE, "Copy translated text");
            menu.add(Menu.NONE, OPTIONS_SHARE_TRANSLATED_TEXT_ID, Menu.NONE, "Share translated text");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        switch (item.getItemId()) {

            case OPTIONS_COPY_RECOGNIZED_TEXT_ID:
                clipboardManager.setText(ocrResultView.getText());
                if (clipboardManager.hasText()) {
                    Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                }
                return true;
            case OPTIONS_SHARE_RECOGNIZED_TEXT_ID:
                Intent shareRecognizedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareRecognizedTextIntent.setType("text/plain");
                shareRecognizedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, ocrResultView.getText());
                startActivity(Intent.createChooser(shareRecognizedTextIntent, "Share via"));
                return true;
            case OPTIONS_COPY_TRANSLATED_TEXT_ID:
                clipboardManager.setText(translationView.getText());
                if (clipboardManager.hasText()) {
                    Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                }
                return true;
            case OPTIONS_SHARE_TRANSLATED_TEXT_ID:
                Intent shareTranslatedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareTranslatedTextIntent.setType("text/plain");
                shareTranslatedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, translationView.getText());
                startActivity(Intent.createChooser(shareTranslatedTextIntent, "Share via"));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Resets view elements.
     */
    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        if (CONTINUOUS_DISPLAY_METADATA) {
            statusViewBottom.setText("");
            statusViewBottom.setTextSize(14);
            statusViewBottom.setTextColor(getResources().getColor(R.color.status_text));
            statusViewBottom.setVisibility(View.VISIBLE);
        }
        if (CONTINUOUS_DISPLAY_RECOGNIZED_TEXT) {
            statusViewTop.setText("");
            statusViewTop.setTextSize(14);
            statusViewTop.setVisibility(View.VISIBLE);
        }
        viewfinderView.setVisibility(View.VISIBLE);
        cameraButtonView.setVisibility(View.VISIBLE);
        if (DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
        }
        lastResult = null;
        viewfinderView.removeResultText();
    }

    /** Displays a pop-up message showing the name of the current OCR source language. */
    public void showLanguageName() {
        Toast toast = Toast.makeText(this, "OCR: " + sourceLanguageReadable, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
    }

    /**
     * Displays an initial message to the user while waiting for the first OCR request to be
     * completed after starting realtime OCR.
     */
    public void setStatusViewForContinuous() {
        viewfinderView.removeResultText();
        if (CONTINUOUS_DISPLAY_METADATA) {
            statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - waiting for OCR...");
        }
    }

    @SuppressWarnings("unused")
    public void setButtonVisibility(boolean visible) {
        if (shutterButton != null && visible == true && DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
        } else if (shutterButton != null) {
            shutterButton.setVisibility(View.GONE);
        }
    }

    /**
     * Enables/disables the shutter button to prevent double-clicks on the button.
     *
     * @param clickable True if the button should accept a click
     */
    public void setShutterButtonClickable(boolean clickable) {
        shutterButton.setClickable(clickable);
    }

    /** Request the viewfinder to be invalidated. */
    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public void onShutterButtonClick(ShutterButton b) {
        if (isContinuousModeActive) {
            onShutterButtonPressContinuous();
        } else {
            if (handler != null) {
                handler.shutterButtonClick();
            }
        }
    }

    @Override
    public void onShutterButtonFocus(ShutterButton b, boolean pressed) {
        requestDelayedAutoFocus();
    }

    /**
     * Requests autofocus after a 350 ms delay. This delay prevents requesting focus when the user
     * just wants to click the shutter button without focusing. Quick button press/release will
     * trigger onShutterButtonClick() before the focus kicks in.
     */
    private void requestDelayedAutoFocus() {
        // Wait 350 ms before focusing to avoid interfering with quick button presses when
        // the user just wants to take a picture without focusing.
        cameraManager.requestAutoFocus(350L);
    }

    static boolean getFirstLaunch() {
        return isFirstLaunch;
    }

    /**
     * We want the help screen to be shown automatically the first time a new version of the app is
     * run. The easiest way to do this is to check android:versionCode from the manifest, and compare
     * it to a value stored as a preference.
     */
//    private boolean checkFirstLaunch() {
//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
//            int currentVersion = info.versionCode;
//            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//            int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);
//            if (lastVersion == 0) {
//                isFirstLaunch = true;
//            } else {
//                isFirstLaunch = false;
//            }
//            if (currentVersion > lastVersion) {
//
//                // Record the last version for which we last displayed the What's New (Help) page
//                prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
//                Intent intent = new Intent(this, HelpActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//
//                // Show the default page on a clean install, and the what's new page on an upgrade.
//                String page = lastVersion == 0 ? HelpActivity.DEFAULT_PAGE : HelpActivity.WHATS_NEW_PAGE;
//                intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
//                startActivity(intent);
//                return true;
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//            Log.w(TAG, e);
//        }
//        return false;
//    }

    /**
     * Returns a string that represents which OCR engine(s) are currently set to be run.
     *
     * @return OCR engine mode
     */
    String getOcrEngineModeName() {
        String ocrEngineModeName = "";
        String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
        if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_ONLY) {
            ocrEngineModeName = ocrEngineModes[0];
        } else if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY) {
            ocrEngineModeName = ocrEngineModes[1];
        } else if (ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
            ocrEngineModeName = ocrEngineModes[2];
        }
        return ocrEngineModeName;
    }

    /**
     * Gets values from shared preferences and sets the corresponding data members in this activity.
     */

    private void retrievePreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieve from preferences, and set in this Activity, the language preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setSourceLanguage(prefs.getString(PreferencesActivity.KEY_SOURCE_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_SOURCE_LANGUAGE_CODE));
        setTargetLanguage(prefs.getString(PreferencesActivity.KEY_TARGET_LANGUAGE_PREFERENCE, MainActivity.DEFAULT_TARGET_LANGUAGE_CODE));
        isTranslationActive = prefs.getBoolean(PreferencesActivity.KEY_TOGGLE_TRANSLATION, false);

        // Retrieve from preferences, and set in this Activity, the capture mode preference
        if (prefs.getBoolean(PreferencesActivity.KEY_CONTINUOUS_PREVIEW, MainActivity.DEFAULT_TOGGLE_CONTINUOUS)) {
            isContinuousModeActive = true;
        } else {
            isContinuousModeActive = false;
        }

        // Retrieve from preferences, and set in this Activity, the page segmentation mode preference
        String[] pageSegmentationModes = getResources().getStringArray(R.array.pagesegmentationmodes);
        String pageSegmentationModeName = prefs.getString(PreferencesActivity.KEY_PAGE_SEGMENTATION_MODE, pageSegmentationModes[0]);
        if (pageSegmentationModeName.equals(pageSegmentationModes[0])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[1])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_AUTO;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[2])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[3])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[4])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[5])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[6])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_WORD;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[7])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT;
        } else if (pageSegmentationModeName.equals(pageSegmentationModes[8])) {
            pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT;
        }

        // Retrieve from preferences, and set in this Activity, the OCR engine mode
        String[] ocrEngineModes = getResources().getStringArray(R.array.ocrenginemodes);
        String ocrEngineModeName = prefs.getString(PreferencesActivity.KEY_OCR_ENGINE_MODE, ocrEngineModes[0]);
        if (ocrEngineModeName.equals(ocrEngineModes[0])) {
            ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
        } else if (ocrEngineModeName.equals(ocrEngineModes[1])) {
            ocrEngineMode = TessBaseAPI.OEM_CUBE_ONLY;
        } else if (ocrEngineModeName.equals(ocrEngineModes[2])) {
            ocrEngineMode = TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED;
        }

        // Retrieve from preferences, and set in this Activity, the character blacklist and whitelist
        characterBlacklist = OcrCharacterHelper.getBlacklist(prefs, sourceLanguageCodeOcr);
        characterWhitelist = OcrCharacterHelper.getWhitelist(prefs, sourceLanguageCodeOcr);

        prefs.registerOnSharedPreferenceChangeListener(listener);

        beepManager.updatePrefs();
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

    public void displayProgressDialog() {
        // Set up the indeterminate progress dialog box
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        String ocrEngineModeName = getOcrEngineModeName();
        if (ocrEngineModeName.equals("Both")) {
            indeterminateDialog.setMessage("Performing OCR using Cube and Tesseract...");
        } else {
            indeterminateDialog.setMessage("Performing OCR using " + ocrEngineModeName + "...");
        }
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();
    }

    public ProgressDialog getProgressDialog() {
        return indeterminateDialog;
    }

    /**
     * Displays an error message dialog box to the user on the UI thread.
     *
     * @param title The title for the dialog box
     * @param message The error message to be displayed
     */
    public void showErrorMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(new FinishListener(this))
                .setPositiveButton( "Done", new FinishListener(this))
                .show();
    }
}