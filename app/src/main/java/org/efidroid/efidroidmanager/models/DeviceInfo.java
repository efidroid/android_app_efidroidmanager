package org.efidroid.efidroidmanager.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import org.efidroid.efidroidmanager.AppConstants;
import org.efidroid.efidroidmanager.types.MountEntry;

public class DeviceInfo {
    public enum LoadingState {
        STATE_LOAD_DEVICEINFO,
        STATE_LOAD_FSTAB,
    }

    public LoadingState mLoadingState = LoadingState.STATE_LOAD_DEVICEINFO;

    private String mDeviceName = null;
    private FSTab mFSTab = null;

    public void parseDeviceList(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        mDeviceName = jsonObject.getString(AppConstants.DEVICE_NAME);
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

    public List<String> getMultibootDirectories(List<MountEntry> mountInfo) {
        for(MountEntry mountEntry : mountInfo) {
            //if(mountEntry.getMountSource().startsWith("/dev/block/mmcblk"))
        }
        return null;
    }
}
