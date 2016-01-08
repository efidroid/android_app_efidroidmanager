package manager.efidroid.org.efidroidmanager.models;

import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import manager.efidroid.org.efidroidmanager.AppConstants;
import manager.efidroid.org.efidroidmanager.types.FSTabEntry;
import manager.efidroid.org.efidroidmanager.types.MountEntry;

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
