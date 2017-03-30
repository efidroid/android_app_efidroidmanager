package org.efidroid.efidroidmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.efidroid.efidroidmanager.models.DeviceInfo;

public final class AppConstants {
    private static final String URL_EFIDROID_SERVER = "https://raw.githubusercontent.com/efidroid";

    public static final String DEVICE_NAME = Build.MANUFACTURER.toLowerCase() + "/" + Build.DEVICE.toLowerCase();
    public static final String SHAREDPREFS_GLOBAL = "org.efidroid.efidroidmanager";
    public static final String SHAREDPREFS_GLOBAL_LAST_DEVICEINFO_UPDATE = "last_deviceinfo_update";
    public static final String SHAREDPREFS_GLOBAL_LAST_APP_VERSION = "last_app_version";

    public static final String PATH_INTERNAL_DEVICES = "devices.json";
    public static final String PATH_INTERNAL_FSTAB = "fstab.multiboot";

    private static String getUrlServer(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return sp.getBoolean("use_custom_ota_server", false) ? sp.getString("ota_server_url", "") : URL_EFIDROID_SERVER;
    }

    public static String getUrlOta(Context context) {
        return getUrlServer(context) + "/ota/master";
    }

    public static String getUrlDeviceList(Context context) {
        return getUrlOta(context) + "/devices.json";
    }

    public static String getUrlDeviceRepo(Context context, DeviceInfo deviceInfo) {
        return getUrlServer(context) + "/device/" + deviceInfo.getDeviceName();
    }

    public static String getUrlDeviceFsTab(Context context, DeviceInfo deviceInfo) {
        return getUrlDeviceRepo(context, deviceInfo) + "/fstab.multiboot";
    }

    public static String getUrlUpdates(Context context, DeviceInfo deviceInfo) {
        return getUrlOta(context) + "/" + deviceInfo.getDeviceName() + "/info.json";
    }
}
