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

    // status
    private int mProgress = 0;
    private String mProgressText = "";
    private boolean mSuccess = false;
    private boolean mFinished = false;

    // context, listener
    private Context mContext;
    private OnStatusChangeListener mListener;

    public interface OnStatusChangeListener {
        void onStatusUpdate(int progress, String text);
        void onCompleted(boolean success);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case ACTION_OPUPDATE_PROGRESS:
                    if(!mFinished) {
                        mProgress = intent.getIntExtra(ARG_OPUPDATE_PROGRESS, -1);
                        mProgressText = intent.getStringExtra(ARG_OPUPDATE_TEXT);

                        mListener.onStatusUpdate(mProgress, mProgressText);
                    }
                    break;

                case ACTION_OPUPDATE_FINISH:
                    mSuccess = intent.getBooleanExtra(ARG_OPUPDATE_SUCCESS, false);
                    mFinished = true;

                    mListener.onCompleted(mSuccess);
                    break;
            }
        }
    };

    public ProgressReceiver(Context context, OnStatusChangeListener listener, Class<?> serviceClass, Class<?> serviceHandler, Bundle serviceBundle) {
        // initialize variables
        mFinished = false;
        mSuccess = false;
        mContext = context;
        mListener = listener;

        // get data
        mServiceClass = serviceClass;
        mServiceBundle = serviceBundle;
        mServiceHandler = serviceHandler;

        if(mServiceClass==null)
            mServiceClass = GenericProgressIntentService.class;
        if(mServiceBundle==null)
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
        if(!mFinished)
            GenericProgressIntentService.showNotification(mContext, mServiceClass, true);
    }

    public void notifyResume() {
        // hide notification
        if(!mFinished)
            GenericProgressIntentService.showNotification(mContext, mServiceClass, false);
    }

    public void onSaveInstanceState(Bundle outState) {
        // store private data
        outState.putInt(ARG_PROGRESS, mProgress);
        outState.putString(ARG_PROGRESS_TEXT, mProgressText);
        outState.putBoolean(ARG_SUCCESS, mSuccess);
        outState.putBoolean(ARG_FINISHED, mFinished);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // restore private dta
        mProgress = savedInstanceState.getInt(ARG_PROGRESS);
        mProgressText = savedInstanceState.getString(ARG_PROGRESS_TEXT);
        mSuccess = savedInstanceState.getBoolean(ARG_SUCCESS);
        mFinished = savedInstanceState.getBoolean(ARG_FINISHED);

        mListener.onStatusUpdate(mProgress, mProgressText);
        if(mFinished)
            mListener.onCompleted(mSuccess);
    }

    public void startService() {
        if(!mFinished) {
            // start service
            Intent serviceIntent = new Intent(mContext, mServiceClass);
            serviceIntent.putExtra(GenericProgressIntentService.ARG_BUNDLE, mServiceBundle);
            serviceIntent.putExtra(GenericProgressIntentService.ARG_HANDLER, mServiceHandler);
            mContext.startService(serviceIntent);
        }
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
