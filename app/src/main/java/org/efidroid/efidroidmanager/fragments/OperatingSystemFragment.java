package org.efidroid.efidroidmanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import org.efidroid.efidroidmanager.R;

import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;

import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnOperatingSystemFragmentInteractionListener}
 * interface.
 */
public class OperatingSystemFragment extends Fragment {
    private OnOperatingSystemFragmentInteractionListener mListener;
    private MaterialDialog mProgressDialog;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public OperatingSystemFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_operatingsystem_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new OperatingSystemRecyclerViewAdapter(mListener.getOperatingSystems(), mListener));
            recyclerView.setNestedScrollingEnabled(false);
            mListener.getFAB().attachToRecyclerView(recyclerView);
        }

        // configure toolbar
        AppBarLayout appBarLayout = mListener.getAppBarLayout();
        Util.setToolBarHeight(appBarLayout, 0, false);

        // show FAB
        mListener.getFAB().setVisibility(View.VISIBLE);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnOperatingSystemFragmentInteractionListener) {
            mListener = (OnOperatingSystemFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnOperatingSystemFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnOperatingSystemFragmentInteractionListener {
        void onOperatingSystemClicked(OperatingSystem item);
        void onOperatingSystemLongClicked(OperatingSystem item);
        DeviceInfo getDeviceInfo();
        List<OperatingSystem> getOperatingSystems();
        FloatingActionButton getFAB();
        void reloadOperatingSystems();
        AppBarLayout getAppBarLayout();
    }
}
