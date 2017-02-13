package org.efidroid.efidroidmanager.patching;

import android.content.Context;

import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.types.FSTabEntry;

public abstract class Patcher {
    private final Context mContext;
    private final DeviceInfo mDeviceInfo;

    Patcher(DeviceInfo deviceInfo, Context context) {
        mContext = context;
        mDeviceInfo = deviceInfo;
    }

    protected Context getContext() {
        return mContext;
    }

    protected DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    public abstract void prepareEnvironment(String updateDir) throws Exception;

    public abstract boolean isPatchRequired(FSTabEntry entry);

    // replaces original image with patched
    public abstract void patchImage(FSTabEntry destEntry, String image) throws Exception;

    public abstract void cleanupEnvironment();
}
