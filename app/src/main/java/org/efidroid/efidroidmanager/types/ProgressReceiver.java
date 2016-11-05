package org.efidroid.efidroidmanager.types;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import org.efidroid.efidroidmanager.services.GenericProgressIntentService;

public class ProgressReceiver {
    // argument values
    private Class<?> mServiceClass = null;
    private Bundle mServiceBundle = null;
    private Class<?> mServiceHandler = null;

    // broadcast actions
    public static final String ACTION_OPUPDATE_PROGRESS = "progressreceiver_action_opupdate_progress";
    public static final String ARG_OPUPDATE_PROGRESS = "progressreceiver_arg_opupdate_progress";
    public static final String ARG_OPUPDATE_TEXT = "progressreceiver_arg_opupdate_text";
    public static final String ACTION_OPUPDATE_FINISH = "progressreceiver_action_opupdate_finish";
    public static final String ARG_OPUPDATE_SUCCESS = "progressreceiver_arg_opupdate_success";

    // private argument names
    private static final String ARG_PROGRESS = "progressreceiver_progress";
    private static final String ARG_PROGRESS_TEXT = "progressreceiver_progress_text";
    private static final String ARG_SUCCESS = "progressreceiver_success";
    private static final String ARG_FINISHED = "progressreceiver_finished";
    private static final String ARG_RUNNING = "progressreceiver_running";
    private static final String ARG_SERVICE_CLASS = "progressreceiver_service_class";
    private static final String ARG_SERVICE_HANDLER = "progressreceiver_service_handler";

    // status
    private int mProgress = 0;
    private String mProgressText = "";
    private boolean mSuccess = false;
    private boolean mFinished = false;
    private boolean mIsRunning = false;

    // context, listener
    private Context mContext;
    private OnStatusChangeListener mListener;

    public void reset() {
        if (mIsRunning)
            throw new RuntimeException("service is still running");

        mProgress = 0;
        mProgressText = "";
        mSuccess = false;
        mFinished = false;
    }

    public interface OnStatusChangeListener {
        void onStatusUpdate(int progress, String text);

        void onCompleted(boolean success);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_OPUPDATE_PROGRESS:
                    if (!mFinished) {
                        mProgress = intent.getIntExtra(ARG_OPUPDATE_PROGRESS, -1);
                        mProgressText = intent.getStringExtra(ARG_OPUPDATE_TEXT);

                        mListener.onStatusUpdate(mProgress, mProgressText);
                    }
                    break;

                case ACTION_OPUPDATE_FINISH:
                    mSuccess = intent.getBooleanExtra(ARG_OPUPDATE_SUCCESS, false);
                    mFinished = true;
                    mIsRunning = false;

                    mListener.onCompleted(mSuccess);
                    break;
            }
        }
    };

    public ProgressReceiver(Context context, OnStatusChangeListener listener, Class<?> serviceClass, Class<?> serviceHandler, Bundle serviceBundle) {
        // initialize variables
        mFinished = false;
        mSuccess = false;
        mIsRunning = false;
        mContext = context;
        mListener = listener;

        // get data
        mServiceClass = serviceClass;
        mServiceBundle = serviceBundle;
        mServiceHandler = serviceHandler;

        if (mServiceClass == null)
            mServiceClass = GenericProgressIntentService.class;
        if (mServiceBundle == null)
            mServiceBundle = new Bundle();

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OPUPDATE_PROGRESS);
        filter.addAction(ACTION_OPUPDATE_FINISH);
        mContext.registerReceiver(receiver, filter);
    }

    public void notifyDestroy() {
        mContext.unregisterReceiver(receiver);
    }

    public void notifyPause() {
        // show notification
        if (!mFinished)
            GenericProgressIntentService.showNotification(mContext, mServiceClass, true);
    }

    public void notifyResume() {
        // hide notification
        if (!mFinished)
            GenericProgressIntentService.showNotification(mContext, mServiceClass, false);
    }

    public void setServiceClass(Class<?> serviceClass) {
        if (mIsRunning)
            throw new RuntimeException("service is still running");

        mServiceClass = serviceClass;
    }

    public void setServiceHandler(Class<?> serviceHandler) {
        if (mIsRunning)
            throw new RuntimeException("service is still running");

        mServiceHandler = serviceHandler;
    }

    public void setServiceBundle(Bundle bundle) {
        if (mIsRunning)
            throw new RuntimeException("service is still running");

        mServiceBundle = bundle;
    }

    public void onSaveInstanceState(Bundle outState) {
        // store private data
        outState.putInt(ARG_PROGRESS, mProgress);
        outState.putString(ARG_PROGRESS_TEXT, mProgressText);
        outState.putBoolean(ARG_SUCCESS, mSuccess);
        outState.putBoolean(ARG_FINISHED, mFinished);
        outState.putBoolean(ARG_RUNNING, mIsRunning);

        outState.putSerializable(ARG_SERVICE_CLASS, mServiceClass);
        outState.putSerializable(ARG_SERVICE_HANDLER, mServiceHandler);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // restore private data
        mProgress = savedInstanceState.getInt(ARG_PROGRESS);
        mProgressText = savedInstanceState.getString(ARG_PROGRESS_TEXT);
        mSuccess = savedInstanceState.getBoolean(ARG_SUCCESS);
        mFinished = savedInstanceState.getBoolean(ARG_FINISHED);
        mIsRunning = savedInstanceState.getBoolean(ARG_RUNNING);

        mServiceClass = (Class<?>) savedInstanceState.getSerializable(ARG_SERVICE_CLASS);
        mServiceHandler = (Class<?>) savedInstanceState.getSerializable(ARG_SERVICE_HANDLER);

        mListener.onStatusUpdate(mProgress, mProgressText);
        if (mFinished)
            mListener.onCompleted(mSuccess);
    }

    public void startService() {
        if (mIsRunning)
            throw new RuntimeException("service is still running");

        // start service
        Intent serviceIntent = new Intent(mContext, mServiceClass);
        serviceIntent.putExtra(GenericProgressIntentService.ARG_BUNDLE, mServiceBundle);
        serviceIntent.putExtra(GenericProgressIntentService.ARG_HANDLER, mServiceHandler);
        mIsRunning = true;
        mContext.startService(serviceIntent);
    }

    public boolean wasSuccessful() {
        return mSuccess;
    }

    public boolean isFinished() {
        return mFinished;
    }

    public int getProgress() {
        return mProgress;
    }

    public String getProgressText() {
        return mProgressText;
    }
}
