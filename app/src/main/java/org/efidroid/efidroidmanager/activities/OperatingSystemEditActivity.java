package org.efidroid.efidroidmanager.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.fragments.operatingsystemedit.GeneralFragment;
import org.efidroid.efidroidmanager.fragments.operatingsystemedit.PartitionItemFragment;
import org.efidroid.efidroidmanager.fragments.operatingsystemedit.ReplacementItemFragment;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.MountInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.FABListener;
import org.efidroid.efidroidmanager.types.FSTabEntry;
import org.efidroid.efidroidmanager.types.MountEntry;
import org.efidroid.efidroidmanager.types.OSEditFragmentInteractionListener;
import org.efidroid.efidroidmanager.tasks.OSUpdateProgressServiceTask;
import org.efidroid.efidroidmanager.view.CustomViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OperatingSystemEditActivity extends AppCompatActivity implements OSEditFragmentInteractionListener {
    // result codes
    public static final int RESULT_UPDATED = 0;
    public static final int RESULT_ABORTED = 1;

    // argument names
    public static final String ARG_OPERATING_SYSTEM = "operatingsystem";
    public static final String ARG_DEVICE_INFO = "deviceinfo";
    private static final String ARG_MULTIBOOT_DIRECTORIES = "multiboot_directories";
    private static final String ARG_MULTIBOOT_PARTITION_INFO = "multiboot_partition_info";

    // argument values
    private OperatingSystem mOperatingSystem;
    private DeviceInfo mDeviceInfo = null;
    private ArrayList<MultibootDir> mMultibootDirectories = null;
    private ArrayList<OSEditFragmentInteractionListener.MultibootPartitionInfo> mMultibootPartitionInfo = null;

    // listeners
    private ArrayList<CommitListener> mCommitListeners = new ArrayList<>();

    // UI
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private FloatingActionButton mFab;
    private TabLayout mTabLayout;
    private CustomViewPager mViewPager;
    private MaterialDialog mProgressDialog;

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private SparseArray<Fragment> registeredFragments = new SparseArray<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return GeneralFragment.newInstance();
                case 1:
                    return PartitionItemFragment.newInstance();
                case 2:
                    return ReplacementItemFragment.newInstance();
            }
            return null;

        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_title_general);
                case 1:
                    return getString(R.string.tab_title_partitions);
                case 2:
                    return getString(R.string.tab_title_replacements);
                case 3:
                    return getString(R.string.tab_title_plugins);
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        public Fragment getRegisteredFragment(int position) {
            return this.registeredFragments.get(position);
        }
    }

    private AsyncTask<Void, Void, Exception> makeOperatingSystemsTask() {
        final ArrayList<MultibootDir> list = new ArrayList<>();
        final ArrayList<OSEditFragmentInteractionListener.MultibootPartitionInfo> partitionSizes = new ArrayList<>();

        return new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    // load mount info
                    MountInfo mountInfo = RootToolsEx.getMountInfo();
                    if (isCancelled()) return null;

                    // load block devices
                    List<String> blockDevices = RootToolsEx.getBlockDevices();

                    // get sizes of multiboot fstab devices
                    for (FSTabEntry entry : mDeviceInfo.getFSTab().getFSTabEntries()) {
                        long size = RootToolsEx.getDeviceSize(entry.getBlkDevice());
                        partitionSizes.add(new OSEditFragmentInteractionListener.MultibootPartitionInfo(entry.getMountPoint().substring(1), size));
                    }

                    for (String blkDevice : blockDevices) {
                        if (isCancelled()) return null;

                        try {
                            // get device node id's
                            int[] majmin = RootToolsEx.getDeviceNode(blkDevice);

                            // ignore loop device mounts
                            if(majmin[0]==7)
                                continue;

                            // get mountpoint
                            MountEntry mountEntry = mountInfo.getByMajorMinor(majmin[0], majmin[1]);
                            if (mountEntry == null)
                                continue;
                            if(mountEntry.getMountOptionsList().contains("ro"))
                                continue;
                            String mountPoint = mountEntry.getMountPoint();
                            String multibootDir;

                            // find multiboot directory
                            if (RootToolsEx.isDirectory(mountPoint + "/media/0"))
                                multibootDir = mountPoint + "/media/0/multiboot";
                            else if (RootToolsEx.isDirectory(mountPoint + "/media") && RootToolsEx.isFile(mountPoint + "/.layout_version"))
                                multibootDir = mountPoint + "/media/multiboot";
                            else
                                multibootDir = mountPoint + "/multiboot";

                            // add directory
                            list.add(new MultibootDir(multibootDir, mountEntry));
                        } catch (Exception e) {
                            continue;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return e;
                }
                return null;
            }

            protected void onPostExecute(Exception e) {
                // hide progressbar
                mProgressDialog.dismiss();

                if (e != null) {
                    new MaterialDialog.Builder(OperatingSystemEditActivity.this)
                            .title(R.string.error)
                            .content(getString(R.string.cant_load_system_info) + e.getLocalizedMessage())
                            .positiveText(R.string.ok)
                            .cancelable(false)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    finish();
                                }
                            }).show();
                } else if (isCancelled()) {
                        new MaterialDialog.Builder(OperatingSystemEditActivity.this)
                                .title(R.string.error)
                                .content(R.string.cancelled_loading_system_info)
                                .positiveText(R.string.ok)
                                .cancelable(false)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        finish();
                                    }
                                }).show();
                } else {
                    mMultibootDirectories = list;
                    mMultibootPartitionInfo = partitionSizes;
                    init();
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RootToolsEx.init(this);

        if (savedInstanceState != null) {
            mOperatingSystem = savedInstanceState.getParcelable(ARG_OPERATING_SYSTEM);
            mDeviceInfo = savedInstanceState.getParcelable(ARG_DEVICE_INFO);
            mMultibootDirectories = savedInstanceState.getParcelableArrayList(ARG_MULTIBOOT_DIRECTORIES);
            mMultibootPartitionInfo = savedInstanceState.getParcelableArrayList(ARG_MULTIBOOT_PARTITION_INFO);

            super.onCreate(savedInstanceState);

            init();
        } else {
            mOperatingSystem = getIntent().getParcelableExtra(ARG_OPERATING_SYSTEM);
            mDeviceInfo = getIntent().getParcelableExtra(ARG_DEVICE_INFO);

            super.onCreate(savedInstanceState);

            // show progress dialog
            mProgressDialog = new MaterialDialog.Builder(this)
                    .title(R.string.loading_device_info)
                    .content(R.string.please_wait)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            // start loading
            makeOperatingSystemsTask().execute();
        }
    }

    private void init() {
        // layout
        setContentView(R.layout.activity_operating_system_edit);

        // title
        if(mOperatingSystem.isCreationMode())
            setTitle(R.string.create_operating_system);
        else
            setTitle(mOperatingSystem.getName());

        // toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // pager adapter for tabs
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                boolean show = (mSectionsPagerAdapter.getRegisteredFragment(position) instanceof FABListener);
                setFabVisible(show);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // tab layout
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        // FAB
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = mViewPager.getCurrentItem();
                Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(position);
                if ((fragment instanceof FABListener)) {
                    ((FABListener) fragment).onFABClicked();
                }
            }
        });

        boolean showFab = (mSectionsPagerAdapter.getRegisteredFragment(0) instanceof FABListener);
        mFab.setVisibility(showFab ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ARG_OPERATING_SYSTEM, mOperatingSystem);
        outState.putParcelable(ARG_DEVICE_INFO, mDeviceInfo);
        outState.putParcelableArrayList(ARG_MULTIBOOT_DIRECTORIES, mMultibootDirectories);
        outState.putParcelableArrayList(ARG_MULTIBOOT_PARTITION_INFO, mMultibootPartitionInfo);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_operatingsystemedit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                boolean error = false;
                for(CommitListener l : mCommitListeners) {
                    if(!l.onCommit())
                        error = true;
                }

                if(!error) {
                    int textid = mOperatingSystem.isCreationMode()?R.string.creating_system:R.string.updating_system;
                    Bundle extras = new Bundle();
                    Intent intent = GenericProgressActivity.makeIntent(
                            this,
                            OSUpdateProgressServiceTask.class,
                            extras,
                            getString(textid, mOperatingSystem.getName()),
                            R.anim.hold, R.anim.abc_slide_out_bottom_full,
                            R.anim.abc_slide_in_left_full, R.anim.abc_slide_out_right_full
                    );

                    extras.putParcelable(OSUpdateProgressServiceTask.ARG_OPERATING_SYSTEM, mOperatingSystem);
                    extras.putParcelable(OSUpdateProgressServiceTask.ARG_DEVICE_INFO, mDeviceInfo);
                    startActivityForResult(intent, 0);
                    overridePendingTransition(R.anim.abc_slide_in_right_full, R.anim.abc_slide_out_left_full);
                }
                return true;
            case android.R.id.home:
                setResult(RESULT_ABORTED);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(RESULT_UPDATED);
        if(requestCode==0 && resultCode==GenericProgressActivity.RESULT_CODE_OK) {
            finish();
        }
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    @Override
    public OperatingSystem getOperatingSystem() {
        return mOperatingSystem;
    }

    @Override
    public ArrayList<MultibootDir> getMultibootDirectories() {
        return mMultibootDirectories;
    }

    @Override
    public ArrayList<MultibootPartitionInfo> getMultibootPartitionInfo() {
        return mMultibootPartitionInfo;
    }

    @Override
    public void onPartitionItemClicked(final OperatingSystem.Partition item) {
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.edit_partition)
                .customView(R.layout.dialog_edit_partition, true)
                .positiveText(R.string.save)
                .negativeText(R.string.md_cancel_label)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // get views
                        EditText nameEditText = (EditText) dialog.getCustomView().findViewById(R.id.name);
                        EditText sizeEditText = (EditText) dialog.getCustomView().findViewById(R.id.size);
                        AppCompatSpinner unitSpinner = (AppCompatSpinner) dialog.getCustomView().findViewById(R.id.spinner_unit);

                        // filename
                        item.setFileName(nameEditText.getText().toString());

                        // size
                        long size = Long.valueOf(sizeEditText.getText().toString());
                        switch (unitSpinner.getSelectedItemPosition()) {
                            // B
                            case 0:
                                break;
                            // KB
                            case 1:
                                size *= 1024;
                                break;
                            // MB
                            case 2:
                                size *= 1024 * 1024;
                                break;
                            // GB
                            case 3:
                                size *= 1024 * 1024 * 1024;
                                break;
                        }
                        item.setSize(size);
                        mOperatingSystem.notifyChange();
                    }
                })
                .build();

        // get views
        EditText nameEditText = (EditText) dialog.getCustomView().findViewById(R.id.name);
        EditText sizeEditText = (EditText) dialog.getCustomView().findViewById(R.id.size);
        AppCompatSpinner unitSpinner = (AppCompatSpinner) dialog.getCustomView().findViewById(R.id.spinner_unit);

        // name
        nameEditText.setText(item.getFileName());

        // size
        if (item.getType() == OperatingSystem.Partition.TYPE_BIND) {
            sizeEditText.setVisibility(View.GONE);
            unitSpinner.setVisibility(View.GONE);
        }
        sizeEditText.setText(String.valueOf(item.getSize()));

        // unit
        String[] arrayUnits = getResources().getStringArray(R.array.units);
        ArrayList<String> listUnits = new ArrayList<>(Arrays.asList(arrayUnits));
        unitSpinner.setAdapter(new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, listUnits));

        // show
        dialog.show();
    }

    @Override
    public void addOnCommitListener(CommitListener listener) {
        mCommitListeners.add(listener);
    }

    @Override
    public void removeOnCommitListener(CommitListener listener) {
        mCommitListeners.remove(listener);
    }

    @Override
    public CustomViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    public TabLayout getTabLayout() {
        return mTabLayout;
    }

    @Override
    public void setFabVisible(final boolean visible) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        if (visible && mFab.getVisibility() == View.GONE) {
            mFab.setVisibility(View.VISIBLE);
            mFab.setAlpha(0f);
        }
        mFab.animate().setDuration(shortAnimTime).alpha(
                visible ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFab.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_ABORTED);
        super.onBackPressed();
    }
}
