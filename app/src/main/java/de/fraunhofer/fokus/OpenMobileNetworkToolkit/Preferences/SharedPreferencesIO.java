package de.fraunhofer.fokus.OpenMobileNetworkToolkit.Preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import de.fraunhofer.fokus.OpenMobileNetworkToolkit.DataProvider.BuildInformation;

public class SharedPreferencesIO {

    private static final String TAG = "SharedPreferencesIO";

    public static String exportPreferences(Context context) {
        HashMap<SPType,SharedPreferences> sharedPreferences = SharedPreferencesGrouper.getInstance(context).getAllSharedPreferences();
        JSONObject preferencesJson = new JSONObject();
        for(SPType key : sharedPreferences.keySet()) {
            SharedPreferences prefs = sharedPreferences.get(key);
            JSONObject spJSON = new JSONObject(prefs.getAll());
            try {
                preferencesJson.put(key.toString(), spJSON);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to export preference "+key, e);
            }
        }
        try {
            preferencesJson.put("BuildInformation", new BuildInformation().toJSON());
            return preferencesJson.toString(4);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to Preference JSON to String", e);
            return "{}";
        }
    }

    public static void importPreferences(Context context, String jsonString) {
        try {
            JSONObject spJSON = new JSONObject(jsonString);

            spJSON.keys().forEachRemaining(key -> {
                SPType spType = SPType.fromString(key);
                if (spType == null) {
                    Log.e(TAG, "Unknown preference type: " + key);
                    return;
                }
                SharedPreferences prefs = SharedPreferencesGrouper.getInstance(context).getSharedPreference(spType);
                SharedPreferences.Editor editor = prefs.edit();
                @NonNull JSONObject preferences;
                try {
                    preferences = spJSON.getJSONObject(key);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to get preferences for: " + key, e);
                    return;
                }
                preferences.keys().forEachRemaining(preferenceKey -> {
                    @NonNull Object value;
                    try {
                        value = preferences.get(preferenceKey);
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to get value for: " + preferenceKey, e);
                        return;
                    }
                    if (value instanceof Boolean) {
                        editor.putBoolean(preferenceKey, (Boolean) value);
                    } else if (value instanceof Float) {
                        editor.putFloat(preferenceKey, (Float) value);
                    } else if (value instanceof Integer) {
                        editor.putInt(preferenceKey, (Integer) value);
                    } else if (value instanceof Long) {
                        editor.putLong(preferenceKey, (Long) value);
                    } else if (value instanceof String) {
                        editor.putString(preferenceKey, (String) value);
                    }
                });
                editor.apply();
                Log.d(TAG, "Imported: " + key);
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to import preferences", e);
            Toast.makeText(context, "Failed to import preferences", Toast.LENGTH_SHORT).show();
        }
    }


}
