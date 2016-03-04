package org.efidroid.efidroidmanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.melnykov.fab.FloatingActionButton;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.DeviceInfo;

public class InstallFragment extends Fragment {
    private OnInstallFragmentInteractionListener mListener;

    public InstallFragment() {
    }

    private AppCompatActivity getCompatActivity() {
        return (AppCompatActivity)getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Util.setToolBarHeight(appBarLayout, 100, true);

        // show FAB
        mListener.getFAB().setVisibility(View.VISIBLE);

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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnInstallFragmentInteractionListener {
        DeviceInfo getDeviceInfo();
        FloatingActionButton getFAB();
        Toolbar getToolbar();
        CollapsingToolbarLayout getCollapsingToolbarLayout();
        AppBarLayout getAppBarLayout();
    }
}
