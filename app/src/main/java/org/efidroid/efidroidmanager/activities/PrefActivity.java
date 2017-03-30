package org.efidroid.efidroidmanager.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import org.efidroid.efidroidmanager.R;

import java.net.MalformedURLException;
import java.net.URL;

public class PrefActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
        final Activity callingActivity = this;

        EditTextPreference otaServerUrl = (EditTextPreference) getPreferenceScreen().findPreference("ota_server_url");
        otaServerUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean isValidUrl = true;
                if (!validateHTTP_HTTPS_URI(newValue.toString())) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(callingActivity);
                    builder.setTitle(R.string.invalid_input);
                    builder.setMessage(R.string.invalid_url_message);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                    isValidUrl = false;
                }
                return isValidUrl;
            }
        });
    }

    // valid url must have http or https schemes and non-empty host
    private static boolean validateHTTP_HTTPS_URI(String uri) {
        final URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            return false;
        }
        return ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) && !"".equals(url.getHost());
    }
}
