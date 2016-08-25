package org.efidroid.efidroidmanager;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;

import org.efidroid.efidroidmanager.models.DeviceInfo;

public class DataHelper {
    private static DeviceInfo mDeviceInfoCache = null;

    public interface DeviceInfoLoadCallback {
        void onDeviceInfoLoadError(Exception e);
        void onDeviceInfoLoaded(DeviceInfo deviceInfo);
    }

    private static String streamToString(InputStream is) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer);
        return writer.toString();
    }

    private static String internalFileToString(Context context, String path) throws IOException {
        FileInputStream fis = context.openFileInput(path);
        String data = streamToString(fis);
        fis.close();
        return data;
    }

    public static void loadDeviceInfo(final Context context, DeviceInfoLoadCallback callback) {
        DeviceInfo deviceInfo = new DeviceInfo();
        boolean reload = false;

        // try to load from internal storage
        try {
            deviceInfo.parseDeviceList(internalFileToString(context, AppConstants.PATH_INTERNAL_DEVICES));
            deviceInfo.parseFSTab(internalFileToString(context, AppConstants.PATH_INTERNAL_FSTAB));
            callback.onDeviceInfoLoaded(deviceInfo);

            // create new deviceinfo object
            deviceInfo = new DeviceInfo();
            callback = null;
        }
        catch (Exception e) {
            // show loading screen

            reload = true;
        }
        finally {
            // we had success loading, check if it's time to update
            if(callback==null) {
                SharedPreferences sp = context.getSharedPreferences(AppConstants.SHAREDPREFS_GLOBAL, Context.MODE_PRIVATE);
                long timeLastUpdate = sp.getLong(AppConstants.SHAREDPREFS_GLOBAL_LAST_DEVICEINFO_UPDATE, 0);

                // update once a day
                if(System.currentTimeMillis()-timeLastUpdate >= 1*24*60*60*1000) {
                    reload = true;
                }
            }

            // start loading
            if(reload) {
                loadDeviceInfoInternal(deviceInfo, context, callback);
            }
        }
    }

    private static void loadDeviceInfoInternal(final DeviceInfo deviceInfo, final Context context, final DeviceInfoLoadCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest;

        switch (deviceInfo.mLoadingState) {
            case STATE_LOAD_DEVICEINFO:
                // try to load from server
                stringRequest = new StringRequest(Request.Method.GET, AppConstants.getUrlDeviceList(context),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String json) {
                                try {
                                    // store file
                                    FileOutputStream fos = context.openFileOutput(AppConstants.PATH_INTERNAL_DEVICES, Context.MODE_PRIVATE);
                                    fos.write(json.getBytes());
                                    fos.close();

                                    // add to deviceinfo
                                    deviceInfo.parseDeviceList(json);

                                    // next state
                                    deviceInfo.mLoadingState = DeviceInfo.LoadingState.STATE_LOAD_FSTAB;
                                    loadDeviceInfoInternal(deviceInfo, context, callback);
                                } catch (Exception e) {
                                    if(callback!=null)
                                        callback.onDeviceInfoLoadError(e);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        try {
                            // try to load from internal storage
                            FileInputStream fis = context.openFileInput(AppConstants.PATH_INTERNAL_DEVICES);
                            String json = streamToString(fis);

                            // add to deviceinfo
                            deviceInfo.parseDeviceList(json);

                            // next state
                            deviceInfo.mLoadingState = DeviceInfo.LoadingState.STATE_LOAD_FSTAB;
                            loadDeviceInfoInternal(deviceInfo, context, callback);
                        } catch (Exception e) {
                            if(callback!=null)
                                callback.onDeviceInfoLoadError(e);
                        }
                    }
                });
                queue.add(stringRequest);
                break;

            case STATE_LOAD_FSTAB:
                // try to load from server
                stringRequest = new StringRequest(Request.Method.GET, AppConstants.getUrlDeviceFsTab(context, deviceInfo),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    // store file
                                    FileOutputStream fos = context.openFileOutput(AppConstants.PATH_INTERNAL_FSTAB, Context.MODE_PRIVATE);
                                    fos.write(response.getBytes());
                                    fos.close();

                                    // add to deviceinfo
                                    deviceInfo.parseFSTab(response);

                                    // store time of last update
                                    Date date = new Date();
                                    SharedPreferences sp = context.getSharedPreferences(AppConstants.SHAREDPREFS_GLOBAL, Context.MODE_PRIVATE);
                                    sp.edit().putLong(AppConstants.SHAREDPREFS_GLOBAL_LAST_DEVICEINFO_UPDATE, date.getTime()).apply();

                                    // done
                                    if(callback!=null)
                                        callback.onDeviceInfoLoaded(deviceInfo);
                                } catch (Exception e) {
                                    if(callback!=null)
                                        callback.onDeviceInfoLoadError(e);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        try {
                            // try to load from internal storage
                            FileInputStream fis = context.openFileInput(AppConstants.PATH_INTERNAL_DEVICES);
                            String fstab = streamToString(fis);

                            // add to deviceinfo
                            deviceInfo.parseFSTab(fstab);

                            // done
                            if(callback!=null)
                                callback.onDeviceInfoLoaded(deviceInfo);
                        } catch (Exception e) {
                            if(callback!=null)
                                callback.onDeviceInfoLoadError(e);
                        }
                    }
                });
                queue.add(stringRequest);
                break;
        }
    }
}