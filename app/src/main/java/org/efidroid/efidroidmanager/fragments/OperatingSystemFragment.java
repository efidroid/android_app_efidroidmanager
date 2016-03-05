package org.efidroid.efidroidmanager.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import org.efidroid.efidroidmanager.R;

import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.activities.GenericProgressActivity;
import org.efidroid.efidroidmanager.activities.MainActivity;
import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.tasks.OSRemovalProgressServiceTask;

import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnOperatingSystemFragmentInteractionListener}
 * interface.
 */
public class OperatingSystemFragment extends Fragment implements OperatingSystemRecyclerViewAdapter.OnInteractionListener {
    private OnOperatingSystemFragmentInteractionListener mListener;
    private MaterialDialog mProgressDialog;

    // request codes
    private static final int REQUEST_EDIT_OS = 0;
    private static final int REQUEST_CREATE_OS = 1;
    private static final int REQUEST_DELETE_OS = 2;

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
            recyclerView.setAdapter(new OperatingSystemRecyclerViewAdapter(mListener.getOperatingSystems(), this));
            recyclerView.setNestedScrollingEnabled(false);
            mListener.getFAB().attachToRecyclerView(recyclerView);
        }

        // configure toolbar
        AppBarLayout appBarLayout = mListener.getAppBarLayout();
        Util.setToolBarHeight(appBarLayout, 0, false);

        // show FAB
        FloatingActionButton fab = mListener.getFAB();
        fab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_dialog_add, getActivity().getTheme()));
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), OperatingSystemEditActivity.class);
                intent.putExtra(OperatingSystemEditActivity.ARG_OPERATING_SYSTEM, new OperatingSystem());
                intent.putExtra(OperatingSystemEditActivity.ARG_DEVICE_INFO, mListener.getDeviceInfo());
                startActivityForResult(intent, REQUEST_CREATE_OS);
            }
        });

        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        layoutParams.gravity = Gravity.RIGHT|Gravity.BOTTOM;
        layoutParams.setMargins(layoutParams.leftMargin, 0, layoutParams.rightMargin, layoutParams.bottomMargin);
        fab.setLayoutParams(layoutParams);

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

    @Override
    public void onOperatingSystemClicked(OperatingSystem item) {
        Intent intent = new Intent(getContext(), OperatingSystemEditActivity.class);
        intent.putExtra(OperatingSystemEditActivity.ARG_OPERATING_SYSTEM, item);
        intent.putExtra(OperatingSystemEditActivity.ARG_DEVICE_INFO, mListener.getDeviceInfo());
        startActivityForResult(intent, REQUEST_EDIT_OS);
    }

    @Override
    public void onOperatingSystemLongClicked(final OperatingSystem item) {
        new MaterialDialog.Builder(getContext())
                .title("Delete")
                .content("Do you want to delete '"+item.getName()+"'?")
                .positiveText("Delete")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Bundle extras = new Bundle();
                        Intent intent = GenericProgressActivity.makeIntent(
                                getContext(),
                                OSRemovalProgressServiceTask.class,
                                extras,
                                "Deleting system\n" + item.getName(),
                                R.anim.hold, R.anim.abc_slide_out_right_full,
                                R.anim.hold, R.anim.abc_slide_out_right_full
                        );

                        extras.putParcelable(OSRemovalProgressServiceTask.ARG_OPERATING_SYSTEM, item);
                        startActivityForResult(intent, REQUEST_DELETE_OS);
                        getActivity().overridePendingTransition(R.anim.abc_slide_in_right_full, R.anim.hold);
                    }
                }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_EDIT_OS:
            case REQUEST_CREATE_OS:
                if(resultCode==OperatingSystemEditActivity.RESULT_UPDATED)
                    mListener.reloadOperatingSystems();
                break;

            case REQUEST_DELETE_OS:
                mListener.reloadOperatingSystems();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public interface OnOperatingSystemFragmentInteractionListener {
        DeviceInfo getDeviceInfo();
        List<OperatingSystem> getOperatingSystems();
        FloatingActionButton getFAB();
        void reloadOperatingSystems();
        AppBarLayout getAppBarLayout();
    }
}
