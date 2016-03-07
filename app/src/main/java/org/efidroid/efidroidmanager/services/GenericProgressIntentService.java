package org.efidroid.efidroidmanager.services;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.activities.MainActivity;
import org.efidroid.efidroidmanager.activities.NotificationReceiverActivity;
import org.efidroid.efidroidmanager.types.ProgressReceiver;
import org.efidroid.efidroidmanager.types.ProgressServiceTask;

public class GenericProgressIntentService extends IntentServiceEx {
    // start argument
    public static final String ARG_BUNDLE = "argument_bundle";
    public static final String ARG_HANDLER = "argument_handler";

    // button_actions
    private static final String BUTTON_ACTION_STOP = "button_stop";

    // message actions
    public static final String MESSAGE_ACTION_NOTIFICATION_SHOW = "message_action_notification_show";
    public static final String MESSAGE_ACTION_NOTIFICATION_HIDE = "message_action_notification_hide";

    // status
    private boolean mShowNotifications = false;
    private ProgressServiceTask mHandler = null;

    public GenericProgressIntentService() {
        super("GenericProgressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mShowNotifications = false;

        Bundle extras = intent.getBundleExtra(ARG_BUNDLE);
        Class<ProgressServiceTask> handlerClass = (Class<ProgressServiceTask>) intent.getSerializableExtra(ARG_HANDLER);
        try {
            mHandler = handlerClass.getDeclaredConstructor(GenericProgressIntentService.class).newInstance(this);
            mHandler.onProcess(extras);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if(mShowNotifications) {
            // remove progress notification
            cancelNotification();

            // show result if user didn't cancel(the activity will be opened anyway in that case)
            if (!shouldStop()) {
                showResultNotification();
            }
        }

        mHandler = null;
    }

    protected void showProgressNotification() {
        Intent intent;
        PendingIntent pIntent;

        // get notification manager
        NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getBaseContext().getResources().getString(R.string.app_name))
                .setOngoing(true)
                .setContentText(mHandler.getNotificationProgressTitle());

        // create button intent
        intent = new Intent(this, NotificationReceiverActivity.class);
        intent.putExtra(NotificationReceiverActivity.ARG_NOTIFICATION_ID, NotificationReceiverActivity.NOTIFICATION_ID_OP_UPDATE_SERVICE);
        intent.putExtra(NotificationReceiverActivity.ARG_SERVICE_CLASS, getClass());
        intent.setAction(BUTTON_ACTION_STOP);
        pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        mBuilder.addAction(R.drawable.ic_action_delete, "Stop", pIntent);

        // create content intent
        intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        mBuilder.setContentIntent(pIntent);

        // show notification
        notifyManager.notify(NotificationReceiverActivity.NOTIFICATION_ID_OP_UPDATE_SERVICE, mBuilder.build());
    }

    protected void showResultNotification() {
        Intent intent;
        PendingIntent pIntent;

        // get notification manager
        NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getBaseContext().getResources().getString(R.string.app_name))
                .setAutoCancel(true)
                .setContentText(mHandler.getNotificationResultTitle());

        // create content intent
        intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        mBuilder.setContentIntent(pIntent);

        notifyManager.notify(NotificationReceiverActivity.NOTIFICATION_ID_OP_UPDATE_SERVICE, mBuilder.build());
    }

    private void cancelNotification() {
        // get notification manager
        NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notifyManager.cancel(NotificationReceiverActivity.NOTIFICATION_ID_OP_UPDATE_SERVICE);
    }

    public int publishProgress(int percent, String text) {
        Intent intent = new Intent();
        intent.setAction(ProgressReceiver.ACTION_OPUPDATE_PROGRESS);
        intent.putExtra(ProgressReceiver.ARG_OPUPDATE_PROGRESS, percent);
        intent.putExtra(ProgressReceiver.ARG_OPUPDATE_TEXT, text);
        sendBroadcast(intent);

        return percent;
    }

    public void publishFinish(boolean success) {
        Intent intent = new Intent();
        intent.setAction(ProgressReceiver.ACTION_OPUPDATE_FINISH);
        intent.putExtra(ProgressReceiver.ARG_OPUPDATE_SUCCESS, success);
        sendBroadcast(intent);
    }

    @Override
    protected void onHandleMessage(Intent intent) {
        if(mHandler==null) {
            super.onHandleMessage(intent);
            return;
        }

        switch (intent.getAction()) {
            case MESSAGE_ACTION_NOTIFICATION_SHOW:
                if(!mShowNotifications) {
                    showProgressNotification();
                    mShowNotifications = true;
                }
                break;

            case MESSAGE_ACTION_NOTIFICATION_HIDE:
                if(mShowNotifications) {
                    cancelNotification();
                    mShowNotifications = false;
                }
                break;

            default:
                super.onHandleMessage(intent);
        }
    }

    @Override
    protected void earlyStop() {
        publishProgress(0, "Aborted");
        publishFinish(false);
    }

    public static void handleNotificationIntent(Activity activity, Class<?> clazz) {
        switch(activity.getIntent().getAction()) {
            case BUTTON_ACTION_STOP:
                stopCurrentTask(activity, clazz);
                break;
        }
    }

    public static void showNotification(Context context, Class<?> clazz, boolean show) {
        Intent intent = new Intent();
        intent.setAction(show?MESSAGE_ACTION_NOTIFICATION_SHOW:MESSAGE_ACTION_NOTIFICATION_HIDE);
        sendMessage(context, clazz, intent);
    }
}
