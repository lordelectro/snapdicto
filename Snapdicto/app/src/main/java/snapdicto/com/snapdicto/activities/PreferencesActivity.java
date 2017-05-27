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
package snapdicto.com.snapdicto.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import snapdicto.com.snapdicto.R;

/**
 * Class to handle preferences that are saved across sessions of the app. Shows
 * a hierarchy of preferences to the user, organized into sections. These
 * preferences are displayed in the options menu that is shown when the user
 * presses the MENU button.
 * 
 *
 */
public class PreferencesActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
  
  // Preference keys not carried over from ZXing project
  public static final String KEY_SOURCE_LANGUAGE_PREFERENCE = "sourceLanguageCodeOcrPref";
  public static final String KEY_TARGET_LANGUAGE_PREFERENCE = "targetLanguageCodeTranslationPref";
  public static final String KEY_TOGGLE_TRANSLATION = "preference_translation_toggle_translation";
  public static final String KEY_CONTINUOUS_PREVIEW = "preference_capture_continuous";
  public static final String KEY_PAGE_SEGMENTATION_MODE = "preference_page_segmentation_mode";
  public static final String KEY_OCR_ENGINE_MODE = "preference_ocr_engine_mode";
  public static final String KEY_CHARACTER_BLACKLIST = "preference_character_blacklist";
  public static final String KEY_CHARACTER_WHITELIST = "preference_character_whitelist";
  public static final String KEY_TOGGLE_LIGHT = "preference_toggle_light";
  public static final String KEY_TRANSLATOR = "preference_translator";
  
  // Preference keys carried over from ZXing project
  public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
  public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
  public static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";
  public static final String KEY_NOT_OUR_RESULTS_SHOWN = "preferences_not_our_results_shown";
  public static final String KEY_REVERSE_IMAGE = "preferences_reverse_image";
  public static final String KEY_PLAY_BEEP = "preferences_play_beep";
  public static final String KEY_VIBRATE = "preferences_vibrate";

  public static final String TRANSLATOR_BING = "Bing Translator";
  public static final String TRANSLATOR_GOOGLE = "Google Translate";
  
  private ListPreference listPreferenceSourceLanguage;
  private ListPreference listPreferenceTargetLanguage;
  private ListPreference listPreferenceTranslator;
  private ListPreference listPreferenceOcrEngineMode;
  private EditTextPreference editTextPreferenceCharacterBlacklist;
  private EditTextPreference editTextPreferenceCharacterWhitelist;
  private ListPreference listPreferencePageSegmentationMode;
  
  private static SharedPreferences sharedPreferences;
  
  /**
   * Set the default preference values.
   * 
   *
   *            savedInstanceState the current Activity's state, as passed by
   *            Android
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
    
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    
    listPreferenceSourceLanguage = (ListPreference) getPreferenceScreen().findPreference(KEY_SOURCE_LANGUAGE_PREFERENCE);
    listPreferenceTargetLanguage = (ListPreference) getPreferenceScreen().findPreference(KEY_TARGET_LANGUAGE_PREFERENCE);
    listPreferenceTranslator = (ListPreference) getPreferenceScreen().findPreference(KEY_TRANSLATOR);
    listPreferenceOcrEngineMode = (ListPreference) getPreferenceScreen().findPreference(KEY_OCR_ENGINE_MODE);
    editTextPreferenceCharacterBlacklist = (EditTextPreference) getPreferenceScreen().findPreference(KEY_CHARACTER_BLACKLIST);
    editTextPreferenceCharacterWhitelist = (EditTextPreference) getPreferenceScreen().findPreference(KEY_CHARACTER_WHITELIST);
    listPreferencePageSegmentationMode = (ListPreference) getPreferenceScreen().findPreference(KEY_PAGE_SEGMENTATION_MODE);
    

  }
  


  /**
   * Called when Activity is about to lose focus. Unregisters the
   * OnSharedPreferenceChangeListener.
   */
  @Override
  protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

  }
}