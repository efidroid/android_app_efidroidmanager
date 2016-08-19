package org.efidroid.efidroidmanager.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.types.ProgressReceiver;
import org.efidroid.efidroidmanager.view.ProgressCircle;

public class GenericProgressActivity extends AppCompatActivity implements ProgressReceiver.OnStatusChangeListener {
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

    // result codes
    public static final int RESULT_CODE_OK = 0;
    public static final int RESULT_CODE_ERROR = 1;

    // status
    private boolean mIsForeground = false;
    private boolean mFinishOnResume = false;
    private ProgressReceiver mProgressReceiver;
    private int mResultCode = RESULT_CODE_ERROR;

    // UI
    protected TextView mTextTitle;
    private TextView mTextHint;
    private ProgressCircle mProgressCircle;
    private Button mButtonCancel;
    private Button mButtonBack;
    private Button mButtonOk;
    private ViewGroup mButtonContainer;

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

    private int getColorSimple(int id) {
        return ResourcesCompat.getColor(getResources(), id, getTheme());
    }

    private void finishProgressbar() {
        // hide cancel button
        mButtonCancel.setVisibility(View.GONE);

        // indicate status
        if(mProgressReceiver.wasSuccessful()) {
            mButtonContainer.setVisibility(View.GONE);
            mProgressCircle.setProgressStrokeColor(getColorSimple(R.color.colorCircleProgressSuccess), true, 200);
            mProgressCircle.setProgressStrokeColor(getColorSimple(R.color.colorCircleProgressSuccess), true, 200);
        } else {
            mButtonContainer.setVisibility(View.VISIBLE);
            mProgressCircle.setProgressStrokeColor(getColorSimple(R.color.colorCircleProgressError), true, 200);
            mProgressCircle.setProgressStrokeColor(getColorSimple(R.color.colorCircleProgressError), true, 200);

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
        RootToolsEx.init(this);

        // reset variables
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
        mProgressCircle = (ProgressCircle) findViewById(R.id.progressCircle);
        mButtonCancel = (Button)findViewById(R.id.button_cancel);
        mButtonBack= (Button)findViewById(R.id.button_back);
        mButtonOk = (Button)findViewById(R.id.button_ok);
        mButtonContainer = (ViewGroup)findViewById(R.id.button_container);

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
                // this gets us back to the main activity
                mResultCode = RESULT_CODE_OK;
                finish();
            }
        });

        // create progress receiver
        mProgressReceiver = new ProgressReceiver(this, this, mServiceClass, mServiceHandler, mServiceBundle);

        // start service
        if (savedInstanceState == null) {
            mProgressReceiver.startService();
        }

        // restore status
        else {
            mProgressReceiver.onRestoreInstanceState(savedInstanceState);
        }

        mTextTitle.setText(mTitle);
    }

    @Override
    protected void onDestroy() {
        mProgressReceiver.notifyDestroy();
        super.onDestroy();
    }

    @Override
    public void finish() {
        // set result
        setResult(mResultCode);

        // finish
        super.finish();

        if(mResultCode==RESULT_CODE_OK) {
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

        mProgressReceiver.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
        mProgressReceiver.notifyPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsForeground = true;
        mProgressReceiver.notifyResume();

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

    @Override
    public void onStatusUpdate(int progress, String text) {
        mProgressCircle.setValue(progress, true, 100);
        mProgressCircle.setContentText(progress+"%");
        mTextHint.setText(text);
    }

    @Override
    public void onCompleted(boolean success) {
        finishProgressbar();

        if(success) {
            mResultCode = RESULT_CODE_OK;

            // finish now
            if (!mFinishOnResume && mIsForeground) {
                finishDelayed(1000);
            }

            // finish on resume
            else
                mFinishOnResume = true;
        }
    }
}
