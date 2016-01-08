package manager.efidroid.org.efidroidmanager.models;

import org.json.JSONException;
import org.json.JSONObject;

import manager.efidroid.org.efidroidmanager.AppConstants;

public class DeviceInfo {
    public enum LoadingState {
        STATE_LOAD_DEVICEINFO,
        STATE_LOAD_FSTAB,
    }

    private String mDeviceName = null;
    public LoadingState mLoadingState = LoadingState.STATE_LOAD_DEVICEINFO;

    public void parseDeviceList(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        mDeviceName = jsonObject.getString(AppConstants.DEVICE_NAME);
    }

    public void parseFSTab(String data) {
    }

    public String getDeviceName() {
        return mDeviceName;
    }
}
