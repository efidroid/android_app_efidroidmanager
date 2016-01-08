package manager.efidroid.org.efidroidmanager.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ResourceLoaderIntentService extends IntentService {
    private static final String ACTION_LOAD_DEVICE_INFO = "manager.efidroid.org.efidroidmanager.services.action.load_device_info";
    private static final String ACTION_FOO = "manager.efidroid.org.efidroidmanager.services.action.FOO";
    private static final String ACTION_BAZ = "manager.efidroid.org.efidroidmanager.services.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "manager.efidroid.org.efidroidmanager.services.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "manager.efidroid.org.efidroidmanager.services.extra.PARAM2";

    public ResourceLoaderIntentService() {
        super("ResourceLoaderIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, ResourceLoaderIntentService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public static void startActionLoadDeviceInfo(Context context) {
        Intent intent = new Intent(context, ResourceLoaderIntentService.class);
        intent.setAction(ACTION_LOAD_DEVICE_INFO);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, ResourceLoaderIntentService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LOAD_DEVICE_INFO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            } else if (ACTION_LOAD_DEVICE_INFO.equals(action)) {
                handleActionLoadDeviceInfo();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        //throw new UnsupportedOperationException("Not yet implemented");
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        publishResults("Foo", 0);
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        //throw new UnsupportedOperationException("Not yet implemented");
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        publishResults("Baz", 0);
    }

    private void handleActionLoadDeviceInfo() {

    }

    private void publishResults(String outputPath, int result) {
        Intent intent = new Intent("NOTIFICATION");
        intent.putExtra("FILEPATH", outputPath);
        sendStickyBroadcast(intent);
    }
}
