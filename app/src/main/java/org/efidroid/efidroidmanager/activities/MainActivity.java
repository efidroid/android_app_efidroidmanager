package org.efidroid.efidroidmanager.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.stericson.rootshell.RootShell;
import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.DataHelper;
import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.fragments.EmptyFragment;
import org.efidroid.efidroidmanager.fragments.OperatingSystemFragment;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.MountInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.MountEntry;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OperatingSystemFragment.OnListFragmentInteractionListener,
        DataHelper.DeviceInfoLoadCallback {

    private NavigationView mNavigationView;
    private FloatingActionButton mFab;
    private ProgressBar mFragmentProgress;
    private DrawerLayout mDrawer;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private MaterialDialog mProgressDialog = null;
    private boolean hasBusybox = false;
    private boolean hasRoot = false;
    private DeviceInfo mDeviceInfo = null;
    private AsyncTask<?, ?, ?> mFragmentLoadingTask = null;
    private ArrayList<OperatingSystem> mOperatingSystems;
    private boolean mTouchDisabled = false;
    private int mActiveMenuItemIndex = 0;
    private MenuItem mPreviousMenuItem;

    private static final String ARG_DEVICE_INFO = "deviceinfo";
    private static final String ARG_OPERATING_SYSTEMS = "operating_systems";
    private static final String ARG_ACTIVEMENU_INDEX = "activemenu_index";
    private static final String ARG_HAS_BUSYBOX = "has_busybox";
    private static final String ARG_HAS_ROOT = "has_root";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get views
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mFragmentProgress = (ProgressBar)findViewById(R.id.progressBar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        // actionbar
        setSupportActionBar(toolbar);

        // drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.setDrawerListener(toggle);
        toggle.syncState();

        // FAB
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, OperatingSystemEditActivity.class);
                intent.putExtra(OperatingSystemEditActivity.ARG_OPERATING_SYSTEM, new OperatingSystem());
                intent.putExtra(OperatingSystemEditActivity.ARG_DEVICE_INFO, mDeviceInfo);
                startActivity(intent);
            }
        });

        // navigation view
        mNavigationView.setNavigationItemSelectedListener(this);

        // SRL
        mSwipeRefreshLayout.setEnabled(false);

        // load data the first time
        if (savedInstanceState == null) {
            // show progress dialog
            mProgressDialog = new MaterialDialog.Builder(this)
                    .title("Loading device info")
                    .content("Please wait")
                    .cancelable(false)
                    .progress(true, 0)
                    .show();

            // load device info
            DataHelper.loadDeviceInfo(this, this);
        }

        else {
            mDeviceInfo = savedInstanceState.getParcelable(ARG_DEVICE_INFO);
            mOperatingSystems = savedInstanceState.getParcelableArrayList(ARG_OPERATING_SYSTEMS);
            mActiveMenuItemIndex = savedInstanceState.getInt(ARG_ACTIVEMENU_INDEX);
            hasBusybox = savedInstanceState.getBoolean(ARG_HAS_BUSYBOX);
            hasRoot = savedInstanceState.getBoolean(ARG_HAS_ROOT);
            onLoadUiData();
        }
    }

    public void onDeviceInfoLoadError(Exception e) {
        new MaterialDialog.Builder(this)
                .title("Title")
                .content("Can't load device info. Please check your connection.\n\n"+e.getLocalizedMessage())
                .positiveText("Try again")
                .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                DataHelper.loadDeviceInfo(MainActivity.this, MainActivity.this);
            }
        }).show();
    }

    private void onLoadUiData() {
        if (!hasBusybox && !RootTools.isBusyboxAvailable()) {
            new MaterialDialog.Builder(this)
                    .title("Title")
                    .content("You need BusyBox to use this app.")
                    .positiveText("Install")
                    .neutralText("Try again")
                    .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            RootTools.offerBusyBox(MainActivity.this);
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            onLoadUiData();
                        }
                    }).show();
            return;
        }
        hasBusybox = true;

        if (!hasRoot && !RootToolsEx.isAccessGiven(0, 0)) {
            new MaterialDialog.Builder(this)
                    .title("Title")
                    .content("You need Root access to use this app.")
                    .positiveText("Try again")
                    .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            onLoadUiData();
                        }
                    }).show();
            return;
        }

        // show first tab
        onNavigationItemSelected(mNavigationView.getMenu().getItem(mActiveMenuItemIndex));
    }

    @Override
    public void onDeviceInfoLoaded(DeviceInfo deviceInfo) {
        mDeviceInfo = deviceInfo;
        mProgressDialog.dismiss();
        onLoadUiData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // we got paused by offering the busybox, so resume loading now
        if(!hasBusybox) {
            onLoadUiData();
        }

    }
    @Override
    protected void onPause() {
        super.onPause();

        // cancel current task and disable loading animations
        if(mFragmentLoadingTask!=null && !mFragmentLoadingTask.isCancelled()) {
            mFragmentLoadingTask.cancel(true);
            mSwipeRefreshLayout.setRefreshing(false);
            mFragmentProgress.setVisibility(View.GONE);
            mTouchDisabled = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ARG_DEVICE_INFO, mDeviceInfo);
        outState.putParcelableArrayList(ARG_OPERATING_SYSTEMS, mOperatingSystems);
        outState.putInt(ARG_ACTIVEMENU_INDEX, mActiveMenuItemIndex);
        outState.putBoolean(ARG_HAS_BUSYBOX, hasBusybox);
        outState.putBoolean(ARG_HAS_ROOT, hasRoot);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private AsyncTask<Void, Void, Exception> makeOperatingSystemsTask() {
        final ArrayList<OperatingSystem> list = new ArrayList<>();

        return new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    // load mount info
                    MountInfo mountInfo = RootToolsEx.getMountInfo();
                    if (isCancelled()) return null;

                    // load block devices
                    List<String> blockDevices = RootToolsEx.getBlockDevices();

                    for (String blkDevice : blockDevices) {
                        if (isCancelled()) return null;

                        try {
                            // get mountpoint
                            int[] majmin = RootToolsEx.getDeviceNode(blkDevice);
                            MountEntry mountEntry = mountInfo.getByMajorMinor(majmin[0], majmin[1]);
                            if (mountEntry == null)
                                continue;
                            String mountPoint = mountEntry.getMountPoint();
                            String multibootDir;

                            // find multiboot directory
                            if (RootToolsEx.fileExists(mountPoint + "/media/0/multiboot"))
                                multibootDir = mountPoint + "/media/0/multiboot";
                            else if (RootToolsEx.fileExists(mountPoint + "/media/multiboot"))
                                multibootDir = mountPoint + "/media/multiboot";
                            else if (RootToolsEx.fileExists(mountPoint + "/multiboot"))
                                multibootDir = mountPoint + "/multiboot";
                            else
                                continue;

                            // get multiboot.ini's
                            List<String> directories = RootToolsEx.getMultibootSystems(multibootDir);
                            for (String directory : directories) {
                                String path = directory + "/multiboot.ini";
                                list.add(new OperatingSystem(path));
                            }
                        }
                        catch (Exception e) {
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
                mFragmentProgress.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);

                if (e != null) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("Title")
                            .content("Can't load operating systems. " + e.getLocalizedMessage())
                            .positiveText("ok").show();
                    return;
                }

                mOperatingSystems = list;

                // show fragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.flContent, new OperatingSystemFragment()).commit();

                mTouchDisabled = false;
            }
        };
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mTouchDisabled || super.dispatchTouchEvent(ev);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;

        // cancel any running loading task
        if(mFragmentLoadingTask!=null && !mFragmentLoadingTask.isCancelled())
            mFragmentLoadingTask.cancel(true);

        // hide progressbar
        mFragmentProgress.setVisibility(View.GONE);

        // disable SwipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener(null);
        mSwipeRefreshLayout.setEnabled(false);

        // hide FAB
        mFab.setVisibility(View.GONE);

        if (id == R.id.nav_operating_systems) {
            mActiveMenuItemIndex = 0;

            // start loading
            if(mOperatingSystems==null) {
                // show progressbar
                mFragmentProgress.setVisibility(View.VISIBLE);

                // run task
                mFragmentLoadingTask = makeOperatingSystemsTask().execute();
            }
            else {
                fragment = new OperatingSystemFragment();
            }

            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mTouchDisabled = true;

                    // run task
                    mFragmentLoadingTask = makeOperatingSystemsTask().execute();
                }
            });
            mSwipeRefreshLayout.setEnabled(true);

            // show fab
            mFab.setVisibility(View.VISIBLE);
        } else if (id == R.id.nav_recovery_tools) {
            mActiveMenuItemIndex = 1;

            // show fab
            mFab.setVisibility(View.VISIBLE);
        } else if (id == R.id.nav_uefi_apps) {
            mActiveMenuItemIndex = 2;
        } else if (id == R.id.nav_plugins) {
            mActiveMenuItemIndex = 3;
        } else if (id == R.id.nav_install) {
            mActiveMenuItemIndex = 4;
        } else if (id == R.id.nav_share) {
            mActiveMenuItemIndex = 5;
        } else if (id == R.id.nav_troubleshoot) {
            mActiveMenuItemIndex = 6;
        } else if (id == R.id.nav_bug_report) {
            mActiveMenuItemIndex = 7;
        } else if (id == R.id.nav_about) {
            mActiveMenuItemIndex = 8;
        }

        // use empty fragment
        if(fragment == null) {
            fragment = new EmptyFragment();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        // Highlight the selected item and update the title
        item.setChecked(true);
        if (mPreviousMenuItem != null) {
            mPreviousMenuItem.setChecked(false);
        }
        mPreviousMenuItem = item;
        setTitle(item.getTitle());

        // close drawer
        mDrawer.closeDrawer(GravityCompat.START);

        return true;
    }

    public void onListFragmentInteraction(OperatingSystem item) {
        Intent intent = new Intent(this, OperatingSystemEditActivity.class);
        intent.putExtra(OperatingSystemEditActivity.ARG_OPERATING_SYSTEM, item);
        intent.putExtra(OperatingSystemEditActivity.ARG_DEVICE_INFO, mDeviceInfo);
        startActivity(intent);
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    public List<OperatingSystem> getOperatingSystems() {
        return mOperatingSystems;
    }

    public FloatingActionButton getFAB() {
        return mFab;
    }
}
