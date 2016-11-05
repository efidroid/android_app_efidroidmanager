package org.efidroid.efidroidmanager.tasks;

import android.os.Bundle;

import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.AppConstants;
import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.types.FSTabEntry;
import org.efidroid.efidroidmanager.types.InstallationEntry;
import org.efidroid.efidroidmanager.types.InstallationStatus;
import org.efidroid.efidroidmanager.types.ProgressServiceTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class EFIDroidUninstallServiceTask extends ProgressServiceTask {
    // args
    public static final String ARG_DEVICE_INFO = "device_info";
    public static final String ARG_INSTALL_STATUS = "installation_status";

    // data
    private DeviceInfo mDeviceInfo = null;
    private InstallationStatus mInstallationStatus = null;

    // status
    private boolean mSuccess = false;
    private int mProgress = 0;


    @SuppressWarnings("unused")
    public EFIDroidUninstallServiceTask(GenericProgressIntentService service) {
        super(service);
    }

    private void doUninstall() throws Exception {
        // get esp parent directory
        String espParent = mDeviceInfo.getESPDir(false);
        if (espParent == null)
            throw new Exception(getService().getString(R.string.cant_find_esp_partition));

        // create UEFIESP dir
        String espDir = espParent + "/UEFIESP";
        RootToolsEx.mkdir(espDir, true);

        // restore backups
        for (FSTabEntry entry : mDeviceInfo.getFSTab().getFSTabEntries()) {
            if (!entry.isUEFI())
                continue;

            RootToolsEx.dd(espDir + "/partition_" + entry.getName() + ".img", entry.getBlkDevice());
        }

        // remove backups
        for (FSTabEntry entry : mDeviceInfo.getFSTab().getFSTabEntries()) {
            if (!entry.isUEFI())
                continue;

            RootTools.deleteFileOrDirectory(espDir + "/partition_" + entry.getName() + ".img", false);
        }
    }

    public void onProcess(Bundle extras) {
        mDeviceInfo = extras.getParcelable(ARG_DEVICE_INFO);
        mInstallationStatus = extras.getParcelable(ARG_INSTALL_STATUS);

        mProgress = 0;
        mSuccess = false;
        try {
            mProgress = publishProgress(50, getService().getString(R.string.uninstalling));
            doUninstall();

            mSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            mSuccess = false;
            publishProgress(mProgress, e.getLocalizedMessage());
        }

        // publish status
        if (mSuccess)
            publishProgress(100, "Done");
        publishFinish(mSuccess);
    }

    @Override
    public String getNotificationProgressTitle() {
        return getService().getString(R.string.uninstalling_efidroid);
    }

    @Override
    public String getNotificationResultTitle() {
        if (mSuccess) {
            return getService().getString(R.string.uninstalling_efidroid_successful);
        } else {
            return getService().getString(R.string.error_uninstalliing_efidroid);
        }
    }
}
