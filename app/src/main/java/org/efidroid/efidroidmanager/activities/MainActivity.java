package org.efidroid.efidroidmanager.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;

import org.efidroid.efidroidmanager.AppConstants;
import org.efidroid.efidroidmanager.DataHelper;
import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.fragments.EmptyFragment;
import org.efidroid.efidroidmanager.fragments.InstallFragment;
import org.efidroid.efidroidmanager.fragments.OperatingSystemFragment;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.MountInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.InstallationStatus;
import org.efidroid.efidroidmanager.types.MountEntry;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OperatingSystemFragment.OnOperatingSystemFragmentInteractionListener,
        DataHelper.DeviceInfoLoadCallback, InstallFragment.OnInstallFragmentInteractionListener {

    // args
    private static final String ARG_DEVICE_INFO = "deviceinfo";
    private static final String ARG_ACTIVEMENU_ID = "activemenu_id";
    private static final String ARG_HAS_ROOT = "has_root";
    private static final String ARG_OPERATING_SYSTEMS = "operating_systems";
    private static final String ARG_INSTALL_STATUS = "install_status";
    // status
    private boolean hasRoot = false;
    private boolean mTouchDisabled = false;
    private int mActiveMenuItemId = 0;
    private MenuItem mPreviousMenuItem;
    private AsyncTask<?, ?, ?> mFragmentLoadingTask = null;
    // data
    private DeviceInfo mDeviceInfo = null;
    // UI
    private NavigationView mNavigationView;
    private FloatingActionButton mFab;
    private ProgressBar mFragmentProgress;
    private DrawerLayout mDrawer;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private MaterialDialog mProgressDialog = null;
    private Toolbar mToolbar;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private AppBarLayout mAppBarLayout;
    private FrameLayout mToolbarFrameLayout;
    // operating systems
    private ArrayList<OperatingSystem> mOperatingSystems;
    // installation
    private InstallationStatus mInstallStatus = null;

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

                            // find multiboot directories
                            for (String multibootPath : OperatingSystem.MULTIBOOT_PATHS) {
                                String multibootDir = mountPoint + multibootPath;

                                if (!RootToolsEx.isDirectory(multibootDir))
                                    continue;

                                // get multiboot.ini's
                                List<String> directories = RootToolsEx.getMultibootSystems(multibootDir);
                                for (String directory : directories) {
                                    String path = directory + "/multiboot.ini";

                                    try {
                                        list.add(new OperatingSystem(path));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
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
                mFragmentProgress.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);

                if (e != null) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.error)
                            .content(getString(R.string.cant_load_operating_systems) + e.getLocalizedMessage())
                            .positiveText(R.string.ok).show();
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

    private AsyncTask<Void, Void, Exception> makeInstallationStatusTask(final InstallFragment.InstallStatusLoadCallback callback) {
        final InstallationStatus installStatus = new InstallationStatus();

        return new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    installStatus.doLoad(MainActivity.this, mDeviceInfo);
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
                            .title(R.string.error)
                            .content(getString(R.string.cant_load_install_info) + e.getLocalizedMessage())
                            .positiveText(R.string.ok).show();
                    return;
                }

                mInstallStatus = installStatus;

                if (callback == null) {
                    // show fragment
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    fragmentManager.beginTransaction().replace(R.id.flContent, new InstallFragment()).commit();

                    mTouchDisabled = false;
                } else {
                    callback.onStatusLoaded();
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();

                if (callback != null) {
                    callback.onStatusLoadError();
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RootToolsEx.init(this);
        setContentView(R.layout.activity_main);

        // get views
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFragmentProgress = (ProgressBar) findViewById(R.id.progressBar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mToolbarFrameLayout = (FrameLayout) findViewById(R.id.toolbar_frame_layout);

        // set initial toolbar height
        int initialHeight = Util.getStatusBarHeight(this) + Util.getToolBarHeight(this);
        ViewGroup.LayoutParams layoutParams = mAppBarLayout.getLayoutParams();
        layoutParams.height = initialHeight;
        mAppBarLayout.setLayoutParams(layoutParams);
        mAppBarLayout.setExpanded(false, false);

        // actionbar
        setSupportActionBar(mToolbar);

        // drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        // navigation view
        mNavigationView.setNavigationItemSelectedListener(this);

        // SRL
        mSwipeRefreshLayout.setEnabled(false);

        if (savedInstanceState != null) {
            mDeviceInfo = savedInstanceState.getParcelable(ARG_DEVICE_INFO);
            mActiveMenuItemId = savedInstanceState.getInt(ARG_ACTIVEMENU_ID);
            hasRoot = savedInstanceState.getBoolean(ARG_HAS_ROOT);

            // operating systems
            mOperatingSystems = savedInstanceState.getParcelableArrayList(ARG_OPERATING_SYSTEMS);

            // install status
            mInstallStatus = savedInstanceState.getParcelable(ARG_INSTALL_STATUS);
        }

        // load data the first time
        if (mDeviceInfo == null) {
            // show progress dialog
            mProgressDialog = new MaterialDialog.Builder(this)
                    .title(R.string.loading_device_info)
                    .content(R.string.please_wait)
                    .cancelable(false)
                    .progress(true, 0)
                    .show();

            // load device info
            DataHelper.loadDeviceInfo(this, this);
        } else {
            onLoadUiData();
        }
    }

    public void onDeviceInfoLoadError(Exception e) {
        new MaterialDialog.Builder(this)
                .title(R.string.error)
                .content(getString(R.string.cant_load_device_info_check_connection) + e.getLocalizedMessage())
                .positiveText(R.string.try_again).neutralText(R.string.override_ota_server)
                .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                DataHelper.loadDeviceInfo(MainActivity.this, MainActivity.this);
            }
        }).onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                showOverrideOTAServerDialog();
            }
        }).show();
    }

    public void showOverrideOTAServerDialog() {
        new MaterialDialog.Builder(MainActivity.this).title(R.string.override_ota_server)
                .input(null, AppConstants.getUrlServerConfig(MainActivity.this), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        AppConstants.setUrlServerConfig(MainActivity.this, input.toString());
                        DataHelper.loadDeviceInfo(MainActivity.this, MainActivity.this);
                    }
                }).show();
    }

    private void onLoadUiData() {
        if (!hasRoot && !RootToolsEx.isAccessGiven(0, 0)) {
            new MaterialDialog.Builder(this)
                    .title(R.string.error)
                    .content(R.string.you_need_root)
                    .positiveText(R.string.try_again)
                    .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    onLoadUiData();
                }
            }).show();
            return;
        }

        // set default tab
        if (mActiveMenuItemId == 0)
            mActiveMenuItemId = R.id.nav_operating_systems;

        // restore old tab
        onNavigationItemSelected(mNavigationView.getMenu().findItem(mActiveMenuItemId));
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

        // operating systems got deleted, reload them
        if (mActiveMenuItemId == R.id.nav_operating_systems && mOperatingSystems == null)
            reloadOperatingSystems();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // cancel current task and disable loading animations
        if (mFragmentLoadingTask != null && !mFragmentLoadingTask.isCancelled()) {
            mFragmentLoadingTask.cancel(true);
            mSwipeRefreshLayout.setRefreshing(false);
            mFragmentProgress.setVisibility(View.GONE);
            mTouchDisabled = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ARG_DEVICE_INFO, mDeviceInfo);
        outState.putInt(ARG_ACTIVEMENU_ID, mActiveMenuItemId);
        outState.putBoolean(ARG_HAS_ROOT, hasRoot);

        // operating systems
        outState.putParcelableArrayList(ARG_OPERATING_SYSTEMS, mOperatingSystems);

        // install status
        outState.putParcelable(ARG_INSTALL_STATUS, mInstallStatus);

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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mTouchDisabled || super.dispatchTouchEvent(ev);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, mDeviceInfo.getDeviceName()));
            sendIntent.setType("text/plain");
            startActivity(sendIntent);

            // close drawer
            mDrawer.closeDrawer(GravityCompat.START);

            return true;
        } else if (id == R.id.nav_get_help) {
            String url = "https://plus.google.com/communities/114053643671219382368";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);

            // close drawer
            mDrawer.closeDrawer(GravityCompat.START);

            return true;
        } else if (id == R.id.nav_bug_report) {
            String url = "https://github.com/efidroid/projectmanagement/issues";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);

            // close drawer
            mDrawer.closeDrawer(GravityCompat.START);

            return true;
        } else if (id == R.id.nav_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);

            // close drawer
            mDrawer.closeDrawer(GravityCompat.START);

            return true;
        }

        // cancel any running loading task
        if (mFragmentLoadingTask != null && !mFragmentLoadingTask.isCancelled())
            mFragmentLoadingTask.cancel(true);

        // hide progressbar
        mFragmentProgress.setVisibility(View.GONE);

        // disable SwipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener(null);
        mSwipeRefreshLayout.setEnabled(false);

        // reset FAB
        mFab.setVisibility(View.GONE);
        mFab.setOnClickListener(null);

        // reset appbar settings
        mCollapsingToolbarLayout.setTitleEnabled(false);
        mCollapsingToolbarLayout.setContentScrimColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, getTheme()));
        mCollapsingToolbarLayout.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, getTheme()));
        mCollapsingToolbarLayout.setStatusBarScrimColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDark, getTheme()));
        mCollapsingToolbarLayout.setScrimVisibleHeightTrigger(Integer.MAX_VALUE);
        mToolbarFrameLayout.removeAllViews();

        if (id == R.id.nav_operating_systems) {
            // start loading
            if (mOperatingSystems == null) {
                // show progressbar
                mFragmentProgress.setVisibility(View.VISIBLE);

                // run task
                mFragmentLoadingTask = makeOperatingSystemsTask().execute();
            } else {
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
        } else if (id == R.id.nav_recovery_tools) {
        } else if (id == R.id.nav_uefi_apps) {
        } else if (id == R.id.nav_plugins) {
        } else if (id == R.id.nav_themes) {
        } else if (id == R.id.nav_install) {
            // start loading
            if (mInstallStatus == null) {
                // show progressbar
                mFragmentProgress.setVisibility(View.VISIBLE);

                // run task
                mFragmentLoadingTask = makeInstallationStatusTask(null).execute();
            } else {
                fragment = new InstallFragment();
            }
        } else if (id == R.id.nav_override_ota) {
            mDrawer.closeDrawer(GravityCompat.START);
            showOverrideOTAServerDialog();
            return true;
        }

        mActiveMenuItemId = item.getItemId();

        // use empty fragment
        if (fragment == null) {
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

    @Override
    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    @Override
    public FloatingActionButton getFAB() {
        return mFab;
    }

    @Override
    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public CollapsingToolbarLayout getCollapsingToolbarLayout() {
        return mCollapsingToolbarLayout;
    }

    @Override
    public AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }

    @Override
    public FrameLayout getToolbarFrameLayout() {
        return mToolbarFrameLayout;
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return mDrawer;
    }

    @Override
    public List<OperatingSystem> getOperatingSystems() {
        return mOperatingSystems;
    }

    @Override
    public void reloadOperatingSystems() {
        mOperatingSystems = null;
        if (mActiveMenuItemId > 0)
            onNavigationItemSelected(mNavigationView.getMenu().findItem(mActiveMenuItemId));
    }

    @Override
    public InstallationStatus getInstallStatus() {
        return mInstallStatus;
    }

    @Override
    public void reloadInstallStatus(InstallFragment.InstallStatusLoadCallback callback) {
        mInstallStatus = null;

        if (callback == null) {
            if (mActiveMenuItemId > 0)
                onNavigationItemSelected(mNavigationView.getMenu().findItem(mActiveMenuItemId));
        } else {
            // run task
            mFragmentLoadingTask = makeInstallationStatusTask(callback).execute();
        }
    }
}
