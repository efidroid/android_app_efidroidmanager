package org.efidroid.efidroidmanager.tasks;

import android.os.Bundle;
import android.support.annotation.Keep;

import org.efidroid.efidroidmanager.AppConstants;
import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
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

public class EFIDroidInstallServiceTask extends ProgressServiceTask {
    // args
    public static final String ARG_DEVICE_INFO = "device_info";
    public static final String ARG_INSTALL_STATUS = "installation_status";

    // data
    private DeviceInfo mDeviceInfo = null;
    private InstallationStatus mInstallationStatus = null;

    // status
    private boolean mSuccess = false;
    private int mProgress = 0;

    @Keep
    @SuppressWarnings("unused")
    public EFIDroidInstallServiceTask(GenericProgressIntentService service) {
        super(service);
    }

    private JSONArray getUpdateList() throws Exception {
        StringBuilder sb = new StringBuilder();

        // connect
        URL url = new URL(AppConstants.getUrlUpdates(getService().getBaseContext(), mDeviceInfo));
        URLConnection connection = url.openConnection();
        connection.connect();

        // open stream
        InputStream input = new BufferedInputStream(connection.getInputStream());

        // copy data
        byte data[] = new byte[1024];
        int count;
        publishProgress(mProgress, getService().getString(R.string.downloading));
        while ((count = input.read(data)) != -1) {
            if (shouldStop()) {
                input.close();
                throw new Exception(getService().getString(R.string.aborted));
            }

            sb.append(new String(data, 0, count));
        }
        input.close();


        return new JSONArray(sb.toString());
    }

    private String downloadUpdate(String urlString) throws Exception {
        // connect
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.connect();

        // open streams
        InputStream input = new BufferedInputStream(connection.getInputStream());
        String downloadFile = getService().getCacheDir().getAbsolutePath() + "/update.zip";
        OutputStream output = new FileOutputStream(downloadFile);

        // copy data
        byte data[] = new byte[1024];
        //long total = 0;
        int count;
        //publishProgress(mProgress, "Downloading");
        while ((count = input.read(data)) != -1) {
            if (shouldStop()) {
                output.close();
                input.close();
                throw new Exception(getService().getString(R.string.aborted));
            }

            //total += count;

            // publishing the progress....
            //mProgress = (int) (total * 100 / fileLength);
            //publishProgress(mProgress, "Downloading");

            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();

        // extract
        String downloadDir = getService().getCacheDir() + "/update";
        RootToolsEx.unzip(downloadFile, downloadDir);

        return downloadDir;
    }

    private void doInstall(String updateDir) throws Exception {
        // get esp parent directory
        String espParent = mDeviceInfo.getESPDir(false);
        if (espParent == null)
            throw new Exception(getService().getString(R.string.cant_find_esp_partition));

        // create UEFIESP dir
        String espDir = espParent + "/UEFIESP";
        RootToolsEx.mkdir(espDir, true);

        // don't create backups on reinstall or update
        if (!mInstallationStatus.isInstalled() || mInstallationStatus.isBroken()) {
            // create backups
            for (FSTabEntry entry : mDeviceInfo.getFSTab().getFSTabEntries()) {
                if (!entry.isUEFI())
                    continue;

                if (!RootToolsEx.isFile(updateDir + "/" + entry.getName() + ".img"))
                    throw new Exception(getService().getString(R.string.invalid_update));

                // repair
                if (mInstallationStatus.isBroken()) {
                    InstallationEntry installationEntry = mInstallationStatus.getEntryByName(entry.getName());
                    int status = installationEntry.getStatus();
                    switch (status) {
                        case InstallationEntry.STATUS_OK:
                        case InstallationEntry.STATUS_WRONG_DEVICE:
                            // nothing to do
                            continue;

                        case InstallationEntry.STATUS_ESP_MISSING:
                            // esp doesn't exist but EFIDroid is installed already, so create an empty image
                            RootToolsEx.createLoopImage(getService(), espDir + "/partition_" + entry.getName() + ".img", RootToolsEx.getDeviceSize(entry.getBlkDevice()));
                            continue;

                        case InstallationEntry.STATUS_NOT_INSTALLED:
                        case InstallationEntry.STATUS_ESP_ONLY:
                            // continue with normal ESP creation
                            break;
                    }

                }

                RootToolsEx.createPartitionBackup(getService(), entry.getBlkDevice(), espDir + "/partition_" + entry.getName() + ".img", -1);
            }
        }

        // install
        for (FSTabEntry entry : mDeviceInfo.getFSTab().getFSTabEntries()) {
            if (!entry.isUEFI())
                continue;

            String file = updateDir + "/" + entry.getName() + ".img";
            RootToolsEx.dd(file, entry.getBlkDevice());
        }
    }

    public void onProcess(Bundle extras) {
        mDeviceInfo = extras.getParcelable(ARG_DEVICE_INFO);
        mInstallationStatus = extras.getParcelable(ARG_INSTALL_STATUS);

        mProgress = 0;
        mSuccess = false;
        try {
            if (Util.isDeviceEncryptionEnabled(getService().getBaseContext())) {
                throw new Exception(getService().getString(R.string.device_is_encrypted));
            }

            // search for update
            mProgress = publishProgress(1, getService().getString(R.string.search_for_update));
            JSONArray updateList = getUpdateList();
            JSONObject latestUpdate = null;
            for (int i = 0; i < updateList.length(); i++) {
                JSONObject o = updateList.getJSONObject(i);

                if (latestUpdate == null || o.getLong("timestamp") > latestUpdate.getLong("timestamp"))
                    latestUpdate = o;
            }

            if (latestUpdate == null)
                throw new Exception(getService().getString(R.string.no_update_available));

            // download update
            mProgress = publishProgress(30, getService().getString(R.string.downloading_update));
            String updateDir = downloadUpdate(latestUpdate.getString("file"));

            mProgress = publishProgress(50, getService().getString(R.string.installing));
            doInstall(updateDir);

            mSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            mSuccess = false;
            publishProgress(mProgress, e.getLocalizedMessage());
        }

        // publish status
        if (mSuccess)
            publishProgress(100, getService().getString(R.string.md_done_label));
        publishFinish(mSuccess);
    }

    @Override
    public String getNotificationProgressTitle() {
        return getService().getString(R.string.installing_efidroid);
    }

    @Override
    public String getNotificationResultTitle() {
        if (mSuccess) {
            return getService().getString(R.string.installing_efidroid_successful);
        } else {
            return getService().getString(R.string.error_installling_efidroid);
        }
    }
}
