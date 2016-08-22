package org.efidroid.efidroidmanager;

import android.os.Build;

import org.efidroid.efidroidmanager.models.DeviceInfo;

public final class AppConstants {
    private static final String URL_OTA = "https://raw.githubusercontent.com/efidroid/ota/master";
    public static final String URL_DEVICES = URL_OTA+"/devices.json";

    public static final String DEVICE_NAME = Build.MANUFACTURER.toLowerCase()+"/"+Build.DEVICE.toLowerCase();
    public static final String PATH_INTERNAL_DEVICES = "devices.json";
    public static final String PATH_INTERNAL_FSTAB = "fstab.multiboot";

    public static final String SHAREDPREFS_GLOBAL = "org.efidroid.efidroidmanager";
    public static final String SHAREDPREFS_GLOBAL_LAST_DEVICEINFO_UPDATE = "last_deviceinfo_update";

    public static String getDeviceRepoUrl(DeviceInfo deviceInfo) {
        return "https://raw.githubusercontent.com/efidroid/device/"+deviceInfo.getDeviceName();
    }

    public static String getUpdatesUrl(DeviceInfo deviceInfo) {
        return URL_OTA+"/"+deviceInfo.getDeviceName()+"/info.json";
    }
}
