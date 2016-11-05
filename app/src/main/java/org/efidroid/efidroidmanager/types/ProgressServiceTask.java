package org.efidroid.efidroidmanager.types;

import android.content.Intent;
import android.os.Bundle;

import org.efidroid.efidroidmanager.activities.GenericProgressActivity;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;

public abstract class ProgressServiceTask {
    private GenericProgressIntentService mService;

    public ProgressServiceTask(GenericProgressIntentService service) {
        mService = service;
    }

    protected GenericProgressIntentService getService() {
        return mService;
    }

    public int publishProgress(int percent, String text) {
        return mService.publishProgress(percent, text);
    }

    public void publishFinish(boolean success) {
        mService.publishFinish(success);
    }

    public boolean shouldStop() {
        return mService.shouldStop();
    }

    abstract public void onProcess(Bundle extras);

    abstract public String getNotificationProgressTitle();

    abstract public String getNotificationResultTitle();
}
