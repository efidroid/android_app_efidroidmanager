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
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.view.ColorArcProgressBar;

public class GenericProgressActivity extends AppCompatActivity {
    // argument values
    private Class<?> mServiceClass = null;
    private Bundle mServiceBundle = null;
    private Class<?> mServiceHandler = null;
    private String mTitle = null;
    private int mAnimSuccessEnter = 0;
    private int mAnimSuccessExit = 0;
    private int mAnimErrorEnter = 0;
    private int mAnimErrorExit = 0;

    // argument names
    public static final String ARG_SERVICE_CLASS = "service_class";
    public static final String ARG_SERVICE_BUNDLE = "service_bundle";
    public static final String ARG_SERVICE_HANDLER = "service_handler";
    public static final String ARG_TITLE = "activity_title";
    public static final String ARG_ANIM_SUCCESS_ENTER = "animation_success_enter";
    public static final String ARG_ANIM_SUCCESS_EXIT = "animation_success_exit";
    public static final String ARG_ANIM_ERROR_ENTER = "animation_error_enter";
    public static final String ARG_ANIM_ERROR_EXIT = "animation_error_exit";

    // private argument names
    private static final String ARG_PROGRESS = "progress";
    private static final String ARG_PROGRESS_TEXT = "progress_text";
    private static final String ARG_SUCCESS = "success";
    private static final String ARG_FINISHED = "finished";

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
    protected TextView mTextTitle;
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

    public static Intent makeIntent(Context context, Class<?> serviceHandler, Bundle serviceBundle, String title, int animSuccessEnter, int animSuccessExit, int animErrorEnter, int animErrorExit) {
        Intent intent = new Intent(context, GenericProgressActivity.class);
        intent.putExtra(ARG_SERVICE_CLASS, GenericProgressIntentService.class);
        intent.putExtra(ARG_SERVICE_BUNDLE, serviceBundle);
        intent.putExtra(ARG_SERVICE_HANDLER, serviceHandler);
        intent.putExtra(ARG_TITLE, title);
        intent.putExtra(ARG_ANIM_SUCCESS_ENTER, animSuccessEnter);
        intent.putExtra(ARG_ANIM_SUCCESS_EXIT, animSuccessExit);
        intent.putExtra(ARG_ANIM_ERROR_ENTER, animErrorEnter);
        intent.putExtra(ARG_ANIM_ERROR_EXIT, animErrorExit);
        return intent;
    }

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

        Bundle extras;
        if(savedInstanceState==null)
            extras = getIntent().getExtras();
        else
            extras = savedInstanceState;

        // get data
        mServiceClass = (Class<?>) extras.getSerializable(ARG_SERVICE_CLASS);
        mServiceBundle = extras.getBundle(ARG_SERVICE_BUNDLE);
        mServiceHandler = (Class<?>) extras.getSerializable(ARG_SERVICE_HANDLER);
        mTitle = extras.getString(ARG_TITLE);
        mAnimSuccessEnter = extras.getInt(ARG_ANIM_SUCCESS_ENTER, 0);
        mAnimSuccessExit = extras.getInt(ARG_ANIM_SUCCESS_EXIT, 0);
        mAnimErrorEnter = extras.getInt(ARG_ANIM_ERROR_ENTER, 0);
        mAnimErrorExit = extras.getInt(ARG_ANIM_ERROR_EXIT, 0);

        if(mServiceClass==null)
            mServiceClass = GenericProgressIntentService.class;
        if(mServiceBundle==null)
            mServiceBundle = new Bundle();

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
                GenericProgressIntentService.stopCurrentTask(GenericProgressActivity.this, mServiceClass);
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
            Intent intent = new Intent(this, mServiceClass);
            intent.putExtra(GenericProgressIntentService.ARG_BUNDLE, mServiceBundle);
            intent.putExtra(GenericProgressIntentService.ARG_HANDLER, mServiceHandler);
            startService(intent);
        }

        // restore ui status
        else {
            mProgress = savedInstanceState.getInt(ARG_PROGRESS);
            mSuccess = savedInstanceState.getBoolean(ARG_SUCCESS);
            mFinished = savedInstanceState.getBoolean(ARG_FINISHED);

            mTextHint.setText(savedInstanceState.getString(ARG_PROGRESS_TEXT));
            mProgressCircle.setCurrentValues(mProgress);
            if(mFinished)
                finishProgressbar();
        }

        mTextTitle.setText(mTitle);
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

        if(mSuccess) {
            overridePendingTransition(mAnimSuccessEnter, mAnimSuccessExit);
        } else {
            overridePendingTransition(mAnimErrorEnter, mAnimErrorExit);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // restore public args
        outState.putSerializable(ARG_SERVICE_CLASS, mServiceClass);
        outState.putBundle(ARG_SERVICE_BUNDLE, mServiceBundle);
        outState.putSerializable(ARG_SERVICE_HANDLER, mServiceHandler);
        outState.putString(ARG_TITLE, mTitle);
        outState.putInt(ARG_ANIM_SUCCESS_ENTER, mAnimSuccessEnter);
        outState.putInt(ARG_ANIM_SUCCESS_EXIT, mAnimSuccessExit);
        outState.putInt(ARG_ANIM_ERROR_ENTER, mAnimErrorEnter);
        outState.putInt(ARG_ANIM_ERROR_EXIT, mAnimErrorExit);

        // restore private args
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
            GenericProgressIntentService.showNotification(this, mServiceClass, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsForeground = true;

        // hide notification
        if(!mFinished)
            GenericProgressIntentService.showNotification(this, mServiceClass, false);

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
