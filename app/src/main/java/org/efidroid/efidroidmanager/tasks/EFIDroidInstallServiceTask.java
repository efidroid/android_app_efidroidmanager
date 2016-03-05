package org.efidroid.efidroidmanager.tasks;

import android.os.Bundle;

import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.types.ProgressServiceTask;

public class EFIDroidInstallServiceTask extends ProgressServiceTask {
    private boolean mSuccess = false;

    @SuppressWarnings("unused")
    public EFIDroidInstallServiceTask(GenericProgressIntentService service) {
        super(service);
    }

    public void onProcess(Bundle extras) {
        mSuccess = false;

        int progress = 0;
        try {
            for(int i=0; i<=100; i++) {
                publishProgress(1, "Progess "+i+"/100");
                Thread.sleep(100, 0);
            }

            mSuccess = true;
        }
        catch (Exception e) {
            mSuccess = false;
            publishProgress(progress, e.getLocalizedMessage());
        }

        // publish status
        if(mSuccess)
            publishProgress(100, "Done");
        publishFinish(mSuccess);
    }

    @Override
    public String getNotificationProgressTitle() {
        return  "Installing EFIDroid";
    }

    @Override
    public String getNotificationResultTitle() {
        if (mSuccess) {
            return "Installing EFIDroid successful";
        } else {
            return "Error installing EFIDroid";
        }
    }
}
