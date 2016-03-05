package org.efidroid.efidroidmanager.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.melnykov.fab.FloatingActionButton;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.tasks.EFIDroidInstallServiceTask;
import org.efidroid.efidroidmanager.types.ProgressReceiver;
import org.efidroid.efidroidmanager.view.ProgressCircle;

public class InstallFragment extends Fragment implements AppBarLayout.OnOffsetChangedListener, ProgressReceiver.OnStatusChangeListener {
    // listener
    private OnInstallFragmentInteractionListener mListener;

    // UI
    private ProgressReceiver mProgressReceiver;
    private ProgressCircle mProgressCircle;

    public InstallFragment() {
    }

    private AppCompatActivity getCompatActivity() {
        return (AppCompatActivity)getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_install, menu);
    }

    private int getColor(int id) {
        return ResourcesCompat.getColor(getResources(), id, getActivity().getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_install, container, false);

        int colorToolBarGrey = ResourcesCompat.getColor(getResources(), R.color.colorToolBarGrey, getActivity().getTheme());
        int colorToolBarGreyDark = ResourcesCompat.getColor(getResources(), R.color.colorToolBarGreyDark, getActivity().getTheme());

        // configure toolbar
        CollapsingToolbarLayout collapsingToolbarLayout = mListener.getCollapsingToolbarLayout();
        collapsingToolbarLayout.setContentScrimColor(colorToolBarGrey);
        collapsingToolbarLayout.setBackgroundColor(colorToolBarGrey);
        collapsingToolbarLayout.setStatusBarScrimColor(colorToolBarGreyDark);

        AppBarLayout appBarLayout = mListener.getAppBarLayout();
        Util.setToolBarHeight(appBarLayout, 250, true);

        // show FAB
        FloatingActionButton fab = mListener.getFAB();
        fab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_delete, getActivity().getTheme()));
        fab.setVisibility(View.VISIBLE);
        appBarLayout.addOnOffsetChangedListener(this);

        // inflate toolbar layout
        FrameLayout toolbarFrameLayout = mListener.getToolbarFrameLayout();
        LayoutInflater tbInflater = LayoutInflater.from(toolbarFrameLayout.getContext());

        View toolbarView = tbInflater.inflate(R.layout.toolbar_layout_install, toolbarFrameLayout, true);

        // progress circle
        mProgressCircle = (ProgressCircle) toolbarView.findViewById(R.id.progressCircle);
        mProgressCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mProgressReceiver.isFinished()) {
                    mListener.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    mProgressCircle.setClickable(false);
                    mProgressCircle.setValue(0, false, 0);
                    mProgressCircle.setProgressHidden(false, true, 200);
                    mProgressReceiver.startService();
                }
            }
        });

        // menu
        setHasOptionsMenu(true);

        // create progress receiver
        mProgressReceiver = new ProgressReceiver(getContext(), this, null, EFIDroidInstallServiceTask.class, null);
        // restore status
        if (savedInstanceState != null) {
            mProgressReceiver.onRestoreInstanceState(savedInstanceState);
        }

        // circle button
        //GradientDrawable bgShape = (GradientDrawable)mCircleButton.getIm();
        //bgShape.setColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, getActivity().getTheme()));
        //bgShape.setColor(Color.parseColor("#4CAF50")); // green - installed + updated
        //bgShape.setColor(Color.parseColor("#FFC107")); // orange - installed + update available
        //bgShape.setColor(Color.parseColor("#FF5722")); // red - not installed
        mProgressCircle.setFillColor(Color.parseColor("#9E9E9E"), false, 0); // grey - loading

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnInstallFragmentInteractionListener) {
            mListener = (OnInstallFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnInstallFragmentInteractionListener");
        }
    }

    @Override
    public void onPause() {
        mProgressReceiver.notifyPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mProgressReceiver.notifyResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.getAppBarLayout().removeOnOffsetChangedListener(this);
        mListener = null;
    }

    @Override
    public void onDestroy() {
        mProgressReceiver.notifyDestroy();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mProgressReceiver.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        FloatingActionButton fab = mListener.getFAB();
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        layoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
        layoutParams.setMargins(layoutParams.leftMargin, Util.getStatusBarHeight(getContext()) + Util.getToolBarHeight(getContext()) + 250 + fab.getHeight() / 2 + verticalOffset, layoutParams.rightMargin, layoutParams.bottomMargin);
        fab.setLayoutParams(layoutParams);
    }

    @Override
    public void onStatusUpdate(int progress, String text) {
        mProgressCircle.setValue(progress, true, 100);
        mProgressCircle.setContentText(text);
    }

    @Override
    public void onCompleted(boolean success) {
        mProgressCircle.setClickable(true);
        mListener.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        mProgressCircle.setProgressHidden(true, true, 1000);
        mProgressCircle.setFillColor(Color.parseColor("#4CAF50"), true, 1000);
        mProgressReceiver.reset();
    }

    public interface OnInstallFragmentInteractionListener {
        DeviceInfo getDeviceInfo();
        FloatingActionButton getFAB();
        Toolbar getToolbar();
        CollapsingToolbarLayout getCollapsingToolbarLayout();
        AppBarLayout getAppBarLayout();
        FrameLayout getToolbarFrameLayout();
        DrawerLayout getDrawerLayout();
    }
}
