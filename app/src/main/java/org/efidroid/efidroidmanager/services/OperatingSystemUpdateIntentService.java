package org.efidroid.efidroidmanager.services;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.activities.NotificationReceiverActivity;
import org.efidroid.efidroidmanager.models.OperatingSystem;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class OperatingSystemUpdateIntentService extends IntentServiceEx {
    private static final String ACTION_UPDATE_OPERATING_SYSTEM = "org.efidroid.efidroidmanager.services.action.update_operating_system";
    private static final String ARG_OPERATING_SYSTEM = "operatingsystem";
    private static final String BUTTON_ACTION_STOP = "button_stop";

    private static NotificationCompat.Builder mNotificationBuilder = null;

    public OperatingSystemUpdateIntentService() {
        super("OperatingSystemUpdateIntentService");
    }

    public static void startActionUpdateOperatingSystem(Context context, OperatingSystem os) {
        Intent intent = new Intent(context, OperatingSystemUpdateIntentService.class);
        intent.setAction(ACTION_UPDATE_OPERATING_SYSTEM);
        intent.putExtra(ARG_OPERATING_SYSTEM, os);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_OPERATING_SYSTEM.equals(action)) {
                OperatingSystem os = intent.getParcelableExtra(ARG_OPERATING_SYSTEM);
                handleActionUpdateOperatingSystem(os);
            }
        }
    }

    private void handleActionUpdateOperatingSystem(OperatingSystem os) {
        // get notification manager
        NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // build notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getBaseContext().getResources().getString(R.string.app_name))
                .setOngoing(true)
                .setContentText("Updating System '" + os.getName() + "'");

        // create intent
        Intent intent = new Intent(this, NotificationReceiverActivity.class);
        intent.putExtra(NotificationReceiverActivity.ARG_NOTIFICATION_ID, NotificationReceiverActivity.NOTIFICATION_ID_OP_UPDATE_SERVICE);
        intent.setAction(BUTTON_ACTION_STOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        mBuilder.addAction(R.drawable.ic_action_delete, "Stop", pIntent);

        // show notification
        mNotificationBuilder = mBuilder;
        notifyManager.notify(NotificationReceiverActivity.NOTIFICATION_ID_OP_UPDATE_SERVICE, mBuilder.build());

        try {
            RootToolsEx.die(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        notifyManager.cancel(NotificationReceiverActivity.NOTIFICATION_ID_OP_UPDATE_SERVICE);
    }

    public static void handleNotificationIntent(Activity activity) {
        Log.e("TAG", "action: "+activity.getIntent().getAction());
        switch(activity.getIntent().getAction()) {
            case BUTTON_ACTION_STOP:
                stopCurrentTask(activity);
                break;
        }
    }
}
