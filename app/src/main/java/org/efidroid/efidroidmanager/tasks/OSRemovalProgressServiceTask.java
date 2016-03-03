package org.efidroid.efidroidmanager.tasks;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.types.ProgressServiceTask;

import java.util.List;

public class OSRemovalProgressServiceTask extends ProgressServiceTask {
    // args
    public static final String ARG_OPERATING_SYSTEM = "operatingsystem";

    private OperatingSystem mOperatingSystem = null;
    private boolean mSuccess = false;

    @SuppressWarnings("unused")
    public OSRemovalProgressServiceTask(GenericProgressIntentService service) {
        super(service);
    }

    public void onProcess(Bundle extras) {
        OperatingSystem os = extras.getParcelable(ARG_OPERATING_SYSTEM);
        if(os==null) {
            mSuccess = false;
            publishProgress(0, "NULL Operating System");
            publishFinish(mSuccess);
            return;
        }

        mOperatingSystem = os;
        mSuccess = false;

        int progress = 0;
        try {
            String romDir = os.getDirectory();
            if(!RootToolsEx.isDirectory(romDir)) {
                throw new Exception("Operating System does not exist");
            }

            publishProgress(1, "removing system");
            if(!RootTools.deleteFileOrDirectory(romDir, false)) {
                throw new Exception("Can't delete Operating system");
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

        mOperatingSystem = null;
    }

    @Override
    public String getNotificationProgressTitle() {
        return  "Removing System '" + mOperatingSystem.getName() + "'";
    }

    @Override
    public String getNotificationResultTitle() {
        if (mSuccess) {
            return "Removed System '" + mOperatingSystem.getName() + "'";
        } else {
            return "Error removing System '" + mOperatingSystem.getName() + "'";
        }
    }
}
