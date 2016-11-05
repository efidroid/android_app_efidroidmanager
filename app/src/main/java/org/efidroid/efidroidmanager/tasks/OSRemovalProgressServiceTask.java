package org.efidroid.efidroidmanager.tasks;

import android.os.Bundle;
import android.support.annotation.Keep;

import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.types.ProgressServiceTask;

public class OSRemovalProgressServiceTask extends ProgressServiceTask {
    // args
    public static final String ARG_OPERATING_SYSTEM = "operatingsystem";

    private OperatingSystem mOperatingSystem = null;
    private boolean mSuccess = false;

    @Keep
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
                throw new Exception(getService().getString(R.string.os_does_not_exist));
            }

            publishProgress(1, getService().getString(R.string.removing_system, os.getName()));
            if(!RootTools.deleteFileOrDirectory(romDir, false)) {
                throw new Exception(getService().getString(R.string.cant_delete_os));
            }

            mSuccess = true;
        }
        catch (Exception e) {
            mSuccess = false;
            publishProgress(progress, e.getLocalizedMessage());
        }

        // publish status
        if(mSuccess)
            publishProgress(100, getService().getString(R.string.md_done_label));
        publishFinish(mSuccess);
    }

    @Override
    public String getNotificationProgressTitle() {
        return  getService().getString(R.string.removing_system, mOperatingSystem.getName());
    }

    @Override
    public String getNotificationResultTitle() {
        if (mSuccess) {
            return getService().getString(R.string.removed_system, mOperatingSystem.getName());
        } else {
            return getService().getString(R.string.error_removing_system, mOperatingSystem.getName());
        }
    }
}
