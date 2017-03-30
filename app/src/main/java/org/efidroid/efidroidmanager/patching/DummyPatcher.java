package org.efidroid.efidroidmanager.patching;


import android.content.Context;

import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.types.FSTabEntry;

final class DummyPatcher extends Patcher {
    DummyPatcher(DeviceInfo deviceInfo, Context context) {
        super(deviceInfo, context);
    }

    @Override
    public void prepareEnvironment(String updateDir) throws Exception {

    }

    @Override
    public boolean isPatchRequired(FSTabEntry entry) {
        return false;
    }

    @Override
    public void patchImage(FSTabEntry destEntry, String image) throws Exception {

    }

    @Override
    public void cleanupEnvironment() {

    }
}
