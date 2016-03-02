package org.efidroid.efidroidmanager.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.services.OperatingSystemUpdateIntentService;
import org.efidroid.efidroidmanager.view.ColorArcProgressBar;

public class OSUpdateProgressActivity extends AppCompatActivity {
    // argument names
    public static final String ARG_OPERATING_SYSTEM = "operatingsystem";
    private static final String ARG_PROGRESS = "progress";
    private static final String ARG_PROGRESS_TEXT = "progress_text";
    private static final String ARG_SUCCESS = "success";
    private static final String ARG_FINISHED = "finished";

    // argument values
    private OperatingSystem mOperatingSystem;

    // result codes
    public static final int RESULT_CODE_OK = 0;
    public static final int RESULT_CODE_ERROR = 1;

    // broadcast actions
    public static final String ACTION_OPUPDATE_PROGRESS = "action_opupdate_progress";
    public static final String ARG_OPUPDATE_PROGRESS = "arg_opupdate_progress";
    public static final String ARG_OPUPDATE_TEXT = "arg_opupdate_text";
    public static final String ACTION_OPUPDATE_FINISH = "action_opupdate_finish";
    public static final String ARG_OPUPDATE_SUCCESS = "arg_opupdate_success";

    // status
    private boolean mIsForeground = false;
    private boolean mSuccess = false;
    private boolean mFinished = false;
    private boolean mFinishOnResume = false;
    private int mProgress = 0;

    // UI
    private TextView mTextTitle;
    private TextView mTextHint;
    private ColorArcProgressBar mProgressCircle;
    private Button mButtonCancel;
    private Button mButtonBack;
    private Button mButtonOk;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case ACTION_OPUPDATE_PROGRESS:
                    if(!mFinished) {
                        mProgress = intent.getIntExtra(ARG_OPUPDATE_PROGRESS, -1);
                        mProgressCircle.setCurrentValues(mProgress);

                        mTextHint.setText(intent.getStringExtra(ARG_OPUPDATE_TEXT));
                    }
                    break;

                case ACTION_OPUPDATE_FINISH:
                    mSuccess = intent.getBooleanExtra(ARG_OPUPDATE_SUCCESS, false);
                    mFinished = true;
                    finishProgressbar();

                    if(mSuccess) {
                        // finish now
                        if (mIsForeground) {
                            finishDelayed(1000);
                        }

                        // finish on resume
                        else
                            mFinishOnResume = true;
                    }
                    break;
            }
        }
    };

    private void finishProgressbar() {
        // hide cancel button
        mButtonCancel.setVisibility(View.GONE);

        // indicate status
        if(mSuccess) {
            mProgressCircle.setFrontArcColor(Color.parseColor("#E6EE9C"));
            mProgressCircle.setUnitColor(Color.parseColor("#E6EE9C"));
        } else {
            mProgressCircle.setFrontArcColor(Color.parseColor("#FFAB91"));
            mProgressCircle.setUnitColor(Color.parseColor("#FFAB91"));

            // show back/ok buttons
            mButtonBack.setVisibility(View.VISIBLE);
            mButtonOk.setVisibility(View.VISIBLE);
        }
        mProgressCircle.invalidate();
    }

    private void finishDelayed(long delayMillis) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, delayMillis);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // reset variables
        mFinished = false;
        mSuccess = false;
        mFinishOnResume = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osupdate_progress);

        // get views
        mTextTitle = (TextView) findViewById(R.id.textTitle);
        mTextHint = (TextView) findViewById(R.id.textHint);
        mProgressCircle = (ColorArcProgressBar) findViewById(R.id.progressBar);
        mButtonCancel = (Button)findViewById(R.id.button_cancel);
        mButtonBack= (Button)findViewById(R.id.button_back);
        mButtonOk = (Button)findViewById(R.id.button_ok);

        // set statusbar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.rgb(0x02, 0x74, 0xB3));
        }

        // buttons
        mButtonCancel.setVisibility(View.VISIBLE);
        mButtonBack.setVisibility(View.GONE);
        mButtonOk.setVisibility(View.GONE);

        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OperatingSystemUpdateIntentService.stopCurrentTask(OSUpdateProgressActivity.this, OperatingSystemUpdateIntentService.class);
            }
        });
        mButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mButtonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSuccess = true;
                finish();
            }
        });

        // register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OPUPDATE_PROGRESS);
        filter.addAction(ACTION_OPUPDATE_FINISH);
        registerReceiver(receiver, filter);

        // start service
        if (savedInstanceState == null) {
            mOperatingSystem = getIntent().getParcelableExtra(ARG_OPERATING_SYSTEM);

            OperatingSystemUpdateIntentService.startActionUpdateOperatingSystem(this, mOperatingSystem);
        }

        // restore ui status
        else {
            mOperatingSystem = savedInstanceState.getParcelable(ARG_OPERATING_SYSTEM);
            mProgress = savedInstanceState.getInt(ARG_PROGRESS);
            mSuccess = savedInstanceState.getBoolean(ARG_SUCCESS);
            mFinished = savedInstanceState.getBoolean(ARG_FINISHED);

            mTextHint.setText(savedInstanceState.getString(ARG_PROGRESS_TEXT));
            mProgressCircle.setCurrentValues(mProgress);
            if(mFinished)
                finishProgressbar();
        }

        // title
        mTextTitle.setText((mOperatingSystem.isCreationMode()?"Creating":"Updating") + " system\n"+mOperatingSystem.getName());
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void finish() {
        // set result
        if(mSuccess) {
            setResult(RESULT_CODE_OK);
        } else {
            setResult(RESULT_CODE_ERROR);
        }

        // finish
        super.finish();

        // set animation
        if(mSuccess) {
            overridePendingTransition(R.anim.hold, R.anim.abc_slide_out_bottom_full);
        } else {
            overridePendingTransition(R.anim.abc_slide_in_left_full, R.anim.abc_slide_out_right_full);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ARG_OPERATING_SYSTEM, mOperatingSystem);
        outState.putInt(ARG_PROGRESS, mProgress);
        outState.putString(ARG_PROGRESS_TEXT, mTextHint.getText().toString());
        outState.putBoolean(ARG_SUCCESS, mSuccess);
        outState.putBoolean(ARG_FINISHED, mFinished);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;

        // show notification
        if(!mFinished)
            OperatingSystemUpdateIntentService.showNotification(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsForeground = true;

        // hide notification
        if(!mFinished)
            OperatingSystemUpdateIntentService.showNotification(this, false);

        // finish
        if(mFinishOnResume) {
            mFinishOnResume = false;
            finishDelayed(1000);
        }
    }

    @Override
    public void onBackPressed() {
        // block back button
    }
}
