package org.efidroid.efidroidmanager.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.efidroid.efidroidmanager.services.OperatingSystemUpdateIntentService;

public class NotificationReceiverActivity extends AppCompatActivity {

    public static final String ARG_NOTIFICATION_ID = "notification_id";
    public static final int NOTIFICATION_ID_OP_UPDATE_SERVICE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch(getIntent().getIntExtra(ARG_NOTIFICATION_ID, -1)) {
            case NOTIFICATION_ID_OP_UPDATE_SERVICE:
                OperatingSystemUpdateIntentService.handleNotificationIntent(this);
        }

        finish();
    }
}
