package org.efidroid.efidroidmanager.types;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.efidroid.efidroidmanager.AppConstants;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.FSTab;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

public class InstallationStatus implements Parcelable {
    private ArrayList<InstallationEntry> mInstallationEntries = new ArrayList<>();
    private JSONObject mUpdate = null;

    public InstallationStatus() {
    }

    private JSONArray getUpdateList(Context context, DeviceInfo deviceInfo) throws Exception {
        StringBuilder sb = new StringBuilder();

        // connect
        URL url = new URL(AppConstants.getUrlUpdates(context, deviceInfo));
        URLConnection connection = url.openConnection();
        connection.connect();

        // open stream
        InputStream input = new BufferedInputStream(connection.getInputStream());

        // copy data
        byte data[] = new byte[1024];
        int count;
        while ((count = input.read(data)) != -1) {
            sb.append(new String(data, 0, count));
        }
        input.close();

        return new JSONArray(sb.toString());
    }

    public void doLoad(Context context, DeviceInfo deviceInfo) {

        // load installation info
        FSTab fsTab = deviceInfo.getFSTab();
        for (FSTabEntry entry : fsTab.getFSTabEntries()) {
            if (!entry.isUEFI())
                continue;

            // build entry
            InstallationEntry installationEntry = new InstallationEntry(entry, deviceInfo);
            mInstallationEntries.add(installationEntry);
        }

        // get update list
        try {
            if (isInstalled() && !isBroken()) {
                JSONArray updateList = getUpdateList(context, deviceInfo);
                JSONObject latestUpdate = null;
                for (int i = 0; i < updateList.length(); i++) {
                    JSONObject o = updateList.getJSONObject(i);

                    if (latestUpdate == null || o.getLong("timestamp") > latestUpdate.getLong("timestamp"))
                        latestUpdate = o;
                }

                if (latestUpdate != null && latestUpdate.getLong("timestamp") > getWorkingEntry().getTimeStamp())
                    mUpdate = latestUpdate;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<InstallationEntry> getInstallationEntries() {
        return mInstallationEntries;
    }

    public InstallationEntry getWorkingEntry() {
        for (InstallationEntry entry : mInstallationEntries) {
            int status = entry.getStatus();
            if (status == InstallationEntry.STATUS_OK)
                return entry;
        }

        return null;
    }

    public InstallationEntry getEntryByName(String name) {
        for (InstallationEntry entry : mInstallationEntries) {
            if (entry.getFsTabEntry().getName().equals(name))
                return entry;
        }

        return null;
    }

    private int getInstalledCount() {
        int installedCount = 0;
        for (InstallationEntry entry : mInstallationEntries) {
            if (entry.getStatus() == InstallationEntry.STATUS_OK)
                installedCount++;
        }
        return installedCount;
    }

    private int getEspOnlyCount() {
        int count = 0;
        for (InstallationEntry entry : mInstallationEntries) {
            int status = entry.getStatus();
            if (status == InstallationEntry.STATUS_ESP_ONLY)
                count++;
        }
        return count;
    }

    public boolean isInstalled() {
        return getInstalledCount() > 0 || getEspOnlyCount() > 0;
    }

    public boolean isBroken() {
        return getInstalledCount() != mInstallationEntries.size();
    }

    public boolean isUpdateAvailable() {
        return mUpdate != null;
    }

    public Date getUpdateDate() {
        try {
            return new Date(mUpdate.getLong("timestamp") * 1000l);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    protected InstallationStatus(Parcel in) {
        in.readList(mInstallationEntries, InstallationEntry.class.getClassLoader());
        try {
            String jsonString = (String) in.readValue(String.class.getClassLoader());
            if (jsonString != null)
                mUpdate = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static final Creator<InstallationStatus> CREATOR = new Creator<InstallationStatus>() {
        @Override
        public InstallationStatus createFromParcel(Parcel in) {
            return new InstallationStatus(in);
        }

        @Override
        public InstallationStatus[] newArray(int size) {
            return new InstallationStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(mInstallationEntries);
        dest.writeValue(mUpdate == null ? null : mUpdate.toString());
    }
}
