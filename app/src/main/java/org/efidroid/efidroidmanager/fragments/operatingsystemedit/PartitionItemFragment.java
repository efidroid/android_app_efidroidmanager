package org.efidroid.efidroidmanager.fragments.operatingsystemedit;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;
import org.efidroid.efidroidmanager.models.FSTab;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.FSTabEntry;
import org.efidroid.efidroidmanager.types.OSEditFragmentInteractionListener;

import java.util.ArrayList;

public class PartitionItemFragment extends Fragment implements OperatingSystem.OperatingSystemChangeListener {
    // data,status
    private OperatingSystem mOperatingSystem = null;
    private String mPreviousOSType = null;
    private OperatingSystemEditActivity.MultibootDir mPreviousLocation = null;

    // listener
    private OSEditFragmentInteractionListener mListener = null;

    // scheme id's
    private static final int SCHEMEID_ANDROID_LOOPSYS_BINDOTHER = 0;
    private static final int SCHEMEID_ANDROID_BINDALL = 1;
    private static final int SCHEMEID_ANDROID_LOOPALL = 2;

    // schemes
    private final SparseArray<PartitionScheme> SCHEMES = new SparseArray<>();

    // UI
    private AppCompatSpinner mSpinnerPartitionScheme = null;
    private ArrayList<PartitionScheme> mSpinnerSchemes = new ArrayList<>();

    private static class PartitionScheme {
        private final Context mContext;
        private final int mNameId;
        private final Callback mCallback;

        public interface Callback {
            void onSetDefaults();
        }
        public PartitionScheme(Context context, int nameId, Callback cb) {
            mContext = context;
            mNameId = nameId;
            mCallback = cb;
        }

        @Override
        public String toString() {
            return mContext.getString(mNameId);
        }
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PartitionItemFragment() {
    }

    public static PartitionItemFragment newInstance() {
        return new PartitionItemFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_partitionitem_list, container, false);

        //  get views
        mSpinnerPartitionScheme = (AppCompatSpinner) view.findViewById(R.id.spinner_partition_scheme);
        View schemeContainer = view.findViewById(R.id.scheme_container);

        if(mOperatingSystem.isCreationMode()) {
            // partition scheme
            mSpinnerPartitionScheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    PartitionScheme scheme = mSpinnerSchemes.get(position);
                    scheme.mCallback.onSetDefaults();
                    mOperatingSystem.notifyChange();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            onOperatingSystemChanged();
        } else {
            schemeContainer.setVisibility(View.GONE);
        }

        // Set the adapter
        Context context = view.getContext();
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new PartitionItemRecyclerViewAdapter(mOperatingSystem, mListener));
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OSEditFragmentInteractionListener) {
            mListener = (OSEditFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must be OSEditFragmentInteractionListener");
        }

        SCHEMES.put(SCHEMEID_ANDROID_LOOPSYS_BINDOTHER, new PartitionScheme(getContext(), R.string.scheme_android_loopsystem_bindother, new PartitionScheme.Callback() {
            @Override
            public void onSetDefaults() {
                ArrayList<OperatingSystem.Partition> list = new ArrayList<>();
                FSTab fsTab = mListener.getDeviceInfo().getFSTab();
                for(FSTabEntry entry : fsTab.getFSTabEntries()) {
                    if(!entry.isMultiboot())
                        continue;

                    String name = entry.getMountPoint().substring(1);
                    boolean is_bind = entry.getFsType().equals("auto");

                    int type;
                    if(name.equals("system"))
                        type = OperatingSystem.Partition.TYPE_LOOP;
                    else if(is_bind)
                        type = OperatingSystem.Partition.TYPE_BIND;
                    else
                        type = OperatingSystem.Partition.TYPE_LOOP;

                    long size = -1;
                    OperatingSystemEditActivity.MultibootPartitionInfo info = Util.getPartitionInfoByName(mListener.getMultibootPartitionInfo(), name);
                    if(info!=null && type!=OperatingSystem.Partition.TYPE_BIND)
                        size = info.size;

                    list.add(new OperatingSystem.Partition(name, name, type, size));
                }
                mOperatingSystem.setPartitions(list);
            }
        }));

        SCHEMES.put(SCHEMEID_ANDROID_BINDALL, new PartitionScheme(getContext(), R.string.scheme_android_bindall, new PartitionScheme.Callback() {
            @Override
            public void onSetDefaults() {
                ArrayList<OperatingSystem.Partition> list = new ArrayList<>();
                FSTab fsTab = mListener.getDeviceInfo().getFSTab();
                for(FSTabEntry entry : fsTab.getFSTabEntries()) {
                    if(!entry.isMultiboot())
                        continue;

                    String name = entry.getMountPoint().substring(1);
                    boolean is_bind = entry.getFsType().equals("auto");

                    int type;
                    if(is_bind)
                        type = OperatingSystem.Partition.TYPE_BIND;
                    else
                        type = OperatingSystem.Partition.TYPE_LOOP;

                    long size = -1;
                    OperatingSystemEditActivity.MultibootPartitionInfo info = Util.getPartitionInfoByName(mListener.getMultibootPartitionInfo(), name);
                    if(info!=null && type!=OperatingSystem.Partition.TYPE_BIND)
                        size = info.size;

                    list.add(new OperatingSystem.Partition(name, name, type, size));
                }
                mOperatingSystem.setPartitions(list);
            }
        }));

        SCHEMES.put(SCHEMEID_ANDROID_LOOPALL, new PartitionScheme(getContext(), R.string.scheme_android_loopall, new PartitionScheme.Callback() {
            @Override
            public void onSetDefaults() {
                ArrayList<OperatingSystem.Partition> list = new ArrayList<>();
                FSTab fsTab = mListener.getDeviceInfo().getFSTab();
                for(FSTabEntry entry : fsTab.getFSTabEntries()) {
                    if(!entry.isMultiboot())
                        continue;

                    String name = entry.getMountPoint().substring(1);

                    long size = -1;
                    OperatingSystemEditActivity.MultibootPartitionInfo info = Util.getPartitionInfoByName(mListener.getMultibootPartitionInfo(), name);
                    if(info!=null)
                        size = info.size;

                    list.add(new OperatingSystem.Partition(name, name, OperatingSystem.Partition.TYPE_LOOP, size));
                }
                mOperatingSystem.setPartitions(list);
            }
        }));

        mOperatingSystem = mListener.getOperatingSystem();
        mOperatingSystem.addChangeListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mOperatingSystem.removeChangeListener(this);
        mOperatingSystem = null;
    }

    @Override
    public void onOperatingSystemChanged() {
        if(mSpinnerPartitionScheme==null || !mOperatingSystem.isCreationMode())
            return;

        OperatingSystemEditActivity.MultibootDir location = mOperatingSystem.getLocation();
        String osType = mOperatingSystem.getOperatingSystemType();

        // check if sth. has changed
        if(mPreviousOSType!=null && mPreviousLocation!=null) {
            if(mPreviousOSType.equals(osType) && mPreviousLocation==location)
                return;
        }

        // spinner schemes
        mSpinnerSchemes.clear();
        if(location!=null) {
            boolean bindSupported = OperatingSystem.isBindAllowed(mOperatingSystem.getLocation().mountEntry.getFsType());

            if (bindSupported)
                mSpinnerSchemes.add(SCHEMES.get(SCHEMEID_ANDROID_LOOPSYS_BINDOTHER));
            if (bindSupported)
                mSpinnerSchemes.add(SCHEMES.get(SCHEMEID_ANDROID_BINDALL));
            mSpinnerSchemes.add(SCHEMES.get(SCHEMEID_ANDROID_LOOPALL));
        }
        mSpinnerPartitionScheme.setAdapter(new ArrayAdapter<>(getContext(), R.layout.support_simple_spinner_dropdown_item, mSpinnerSchemes));

        // update previous data
        mPreviousOSType = osType;
        mPreviousLocation = location;
    }
}
