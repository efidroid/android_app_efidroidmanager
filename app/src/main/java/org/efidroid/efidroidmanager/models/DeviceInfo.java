package org.efidroid.efidroidmanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.efidroid.efidroidmanager.AppConstants;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.types.FSTabEntry;
import org.efidroid.efidroidmanager.types.MountEntry;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DeviceInfo implements Parcelable {

    // data
    private String mDeviceName = null;
    private boolean mUseLoki;
    private FSTab mFSTab = null;

    // state
    public LoadingState mLoadingState = LoadingState.STATE_LOAD_DEVICEINFO;

    public enum LoadingState {
        STATE_LOAD_DEVICEINFO,
        STATE_LOAD_FSTAB,
    }

    public DeviceInfo() {
    }

    protected DeviceInfo(Parcel in) {
        mDeviceName = in.readString();
        mUseLoki = in.readByte() != 0;
        mFSTab = in.readParcelable(FSTab.class.getClassLoader());
    }

    public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
        @Override
        public DeviceInfo createFromParcel(Parcel in) {
            return new DeviceInfo(in);
        }

        @Override
        public DeviceInfo[] newArray(int size) {
            return new DeviceInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mDeviceName);
        dest.writeByte(mUseLoki ? (byte)1 : (byte)0);
        dest.writeParcelable(mFSTab, flags);
    }

    public void parseDeviceList(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        mDeviceName = jsonObject.getString(AppConstants.DEVICE_NAME);
        mUseLoki = jsonObject.getBoolean(AppConstants.USE_LOKI);
    }

    public void parseFSTab(String data) throws IOException {
        mFSTab = new FSTab(data);
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public FSTab getFSTab() {
        return mFSTab;
    }

    public boolean useLoki() { return mUseLoki; }

    public String getESPDir(boolean requireUEFIESPDir) {
        for (FSTabEntry entry : mFSTab.getFSTabEntries()) {
            String espFlag = entry.getESP();
            if (espFlag == null)
                continue;

            try {
                // load mount info
                MountInfo mountInfo = RootToolsEx.getMountInfo();

                int[] majmin = RootToolsEx.getDeviceNode(entry.getBlkDevice());
                MountEntry mountEntry = mountInfo.getByMajorMinor(majmin[0], majmin[1]);
                if (mountEntry == null)
                    return null;
                String mountPoint = mountEntry.getMountPoint();

                // absolute path
                if (espFlag.startsWith("/"))
                    return mountPoint + espFlag;

                if (espFlag.equals("datamedia")) {
                    String path;

                    if (requireUEFIESPDir)
                        path = mountPoint + "/media/0/UEFIESP";
                    else
                        path = mountPoint + "/media/0";
                    if (RootToolsEx.isDirectory(path))
                        return path;

                    if (requireUEFIESPDir)
                        path = mountPoint + "/media/UEFIESP";
                    else
                        path = mountPoint + "/media";
                    if (RootToolsEx.isDirectory(path))
                        return path;
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }
}
