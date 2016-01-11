package org.efidroid.efidroidmanager.fragments.operatingsystemedit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.activities.MainActivity;
import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.FABListener;
import org.efidroid.efidroidmanager.types.RootFileChooserDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class ReplacementItemFragment extends Fragment implements FABListener, ReplacementItemRecyclerViewAdapter.OnListFragmentInteractionListener, ActionMode.Callback, RootFileChooserDialog.FileCallback {
    private OperatingSystem mOperatingSystem = null;
    private OperatingSystemEditActivity mActivity;
    private RecyclerView mRecyclerView = null;
    private ActionMode mActionMode = null;
    private ReplacementItemRecyclerViewAdapter mAdapter = null;
    private SparseArray<Fragment> registeredFragments = new SparseArray();
    private int mChooserItemPosition = -1;

    private static final String ARG_ACTIONMODE_ENABLED = "actionmode_enabled";
    private static final String ARG_SELECTED_ITEMS = "actionmode_selected_items";
    private static final String ARG_CHOOSER_ITEM_POSITION = "choose_item_position";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ReplacementItemFragment() {
    }

    @SuppressWarnings("unused")
    public static ReplacementItemFragment newInstance(OperatingSystem os) {
        ReplacementItemFragment fragment = new ReplacementItemFragment();
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ARG_ACTIONMODE_ENABLED, mActionMode!=null);
        outState.putIntegerArrayList(ARG_SELECTED_ITEMS, mAdapter.getSelectedItems());
        outState.putInt(ARG_CHOOSER_ITEM_POSITION, mChooserItemPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_replacemenitem_list, container, false);

        mOperatingSystem = mActivity.getOperatingSystem();

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            mAdapter = new ReplacementItemRecyclerViewAdapter(mOperatingSystem, this);
            recyclerView.setAdapter(mAdapter);
            mRecyclerView = recyclerView;
        }

        if(savedInstanceState!=null) {
            boolean actionModeRunning = savedInstanceState.getBoolean(ARG_ACTIONMODE_ENABLED);
            if(actionModeRunning) {
                mActionMode = mActivity.startSupportActionMode(this);
            }

            ArrayList<Integer> selectedItems = savedInstanceState.getIntegerArrayList(ARG_SELECTED_ITEMS);
            for(Integer position : selectedItems) {
                mAdapter.setSelected(position, true);
            }

            mChooserItemPosition = savedInstanceState.getInt(ARG_CHOOSER_ITEM_POSITION);
        }

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OperatingSystemEditActivity) {
            mActivity = (OperatingSystemEditActivity) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
        mOperatingSystem = null;
    }

    private class CmdlineItemValidator implements TextWatcher {
        private final EditText mEditText;
        private final CmdlineItemButtonValidator mValidator;
        public boolean isValid = true;

        public CmdlineItemValidator(CmdlineItemButtonValidator validator, EditText editText) {
            mValidator = validator;
            mEditText = editText;

            mEditText.addTextChangedListener(this);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            boolean valid = true;

            if(s.toString().contains(" "))
                valid = false;
            else if(s.toString().contains("\t"))
                valid = false;
            else if(s.toString().contains("="))
                valid = false;


            isValid = valid;
            mEditText.setError(isValid?null:"Invalid characters");
            mValidator.onValidationChange();
        }
    }

    private class CmdlineItemButtonValidator {
        private final List<CmdlineItemValidator> mValidators = new ArrayList<>();
        private final MaterialDialog mDialog;

        public CmdlineItemButtonValidator(MaterialDialog dialog, EditText... editTexts) {
            // create and add editText validators
            for(EditText editText : editTexts) {
                mValidators.add(new CmdlineItemValidator(this, editText));
            }

            mDialog = dialog;
        }

        public void onValidationChange() {
            boolean allValid = true;

            // check if all editTexts are valid
            for(CmdlineItemValidator validator : mValidators) {
                if(!validator.isValid) {
                    allValid = false;
                    break;
                }
            }

            // set positive button status
            mDialog.getActionButton(DialogAction.POSITIVE).setEnabled(allValid);
        }
    }

    @Override
    public void onFABClicked() {
        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("New cmdline override")
                .customView(R.layout.dialog_new_cmdline, true)
                .positiveText("save")
                .negativeText("cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        EditText nameEditText = (EditText)dialog.getCustomView().findViewById(R.id.name);
                        EditText valueEditText = (EditText)dialog.getCustomView().findViewById(R.id.value);
                        OperatingSystem.CmdlineItem item = new OperatingSystem.CmdlineItem(nameEditText.getText().toString(), valueEditText.getText().toString());

                        int pos = mRecyclerView.getAdapter().getItemCount();

                        mOperatingSystem.getCmdline().add(item);
                        mOperatingSystem.notifyChange();

                        mRecyclerView.getAdapter().notifyItemInserted(pos);
                        mRecyclerView.scrollToPosition(pos);
                    }
                })
                .build();

        // add validator
        EditText nameEditText = (EditText)dialog.getCustomView().findViewById(R.id.name);
        EditText valueEditText = (EditText)dialog.getCustomView().findViewById(R.id.value);
        new CmdlineItemButtonValidator(dialog, nameEditText, valueEditText);

        dialog.show();
    }

    @Override
    public void onCmdlineItemClicked(View v, OperatingSystem.CmdlineItem item) {
        final OperatingSystem.CmdlineItem cmdlineItem = item;

        final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("Edit cmdline override")
                .customView(R.layout.dialog_new_cmdline, true)
                .positiveText("save")
                .negativeText("cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog dialog, DialogAction which) {
                        EditText nameEditText = (EditText)dialog.getCustomView().findViewById(R.id.name);
                        EditText valueEditText = (EditText)dialog.getCustomView().findViewById(R.id.value);

                        int pos = mRecyclerView.getAdapter().getItemCount();

                        cmdlineItem.name = nameEditText.getText().toString();
                        cmdlineItem.value = valueEditText.getText().toString();
                        mOperatingSystem.notifyChange();

                        mRecyclerView.getAdapter().notifyItemChanged(pos);
                    }
                })
                .build();

        // add validator
        EditText nameEditText = (EditText)dialog.getCustomView().findViewById(R.id.name);
        EditText valueEditText = (EditText)dialog.getCustomView().findViewById(R.id.value);
        new CmdlineItemButtonValidator(dialog, nameEditText, valueEditText);

        // set values
        nameEditText.setText(item.name);
        valueEditText.setText(item.value);

        dialog.show();
    }

    @Override
    public void onCmdlineItemLongClicked(View v, OperatingSystem.CmdlineItem item) {
        // start action mode
        if (mActionMode==null) {
            mActionMode = mActivity.startSupportActionMode(this);
        }

        // toggle item selection
        int position = mAdapter.getItemPosition(item);
        if(position>=0) {
            mAdapter.setSelected(position, !mAdapter.isSelected(position));
            v.setActivated(!v.isActivated());
        }

        // stop action mode when last item got deselected
        if(mAdapter.getSelectedItems().size()==0) {
            mActionMode.finish();
        }
    }

    @Override
    public void onFileSelection(@NonNull File file) {
        Log.e("TAG", file.getAbsolutePath());

        ReplacementItemRecyclerViewAdapter.ReplacementItem item = (ReplacementItemRecyclerViewAdapter.ReplacementItem)mAdapter.getItem(mChooserItemPosition);
        item.setValue(file.getName());
        mAdapter.notifyItemChanged(mChooserItemPosition);

        mChooserItemPosition = -1;
    }

    @Override
    public void onReplacementItemClicked(View v, ReplacementItemRecyclerViewAdapter.ReplacementItem item) {
        mChooserItemPosition = mAdapter.getItemPosition(item);

        RootFileChooserDialog d = new RootFileChooserDialog.Builder(mActivity)
                .initialPath("/")  // changes initial path, defaults to external storage directory
                .build();
        d.setTargetFragment(this, 0);
        d.show(mActivity);
    }

    @Override
    public void onReplacementItemLongClicked(View v, ReplacementItemRecyclerViewAdapter.ReplacementItem item) {

    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.actionmode_operatingsystemedit, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // set statusBar color
        if (Build.VERSION.SDK_INT >= 21) {
            mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, R.color.colorActionmodeDark));
        }

        // disable paging
        mActivity.getViewPager().setPagingEnabled(false);

        // set tabLayout background
        TransitionDrawable transitionDrawable = (TransitionDrawable)mActivity.getTabLayout().getBackground();
        transitionDrawable.startTransition(300);

        // hide inactive tabs
        TabLayout tabLayout = mActivity.getTabLayout();
        if(tabLayout.getChildAt(0) instanceof ViewGroup) {
            ViewGroup tabLayoutChild0 = (ViewGroup)tabLayout.getChildAt(0);
            if(tabLayoutChild0.getChildCount()==tabLayout.getTabCount()) {
                for(int i=0; i<tabLayoutChild0.getChildCount(); i++) {
                    if(i==tabLayout.getSelectedTabPosition())
                        continue;

                    View child = tabLayoutChild0.getChildAt(i);
                    child.animate().setDuration(300).alpha(0);
                }
            }
        }

        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_delete:
                mAdapter.removeSelectedItems();
                mActionMode.finish();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // show all tabs
        TabLayout tabLayout = mActivity.getTabLayout();
        if(tabLayout.getChildAt(0) instanceof ViewGroup) {
            ViewGroup tabLayoutChild0 = (ViewGroup)tabLayout.getChildAt(0);
            if(tabLayoutChild0.getChildCount()==tabLayout.getTabCount()) {
                for(int i=0; i<tabLayoutChild0.getChildCount(); i++) {
                    if(i==tabLayout.getSelectedTabPosition())
                        continue;

                    View child = tabLayoutChild0.getChildAt(i);
                    child.animate().setDuration(300).alpha(1);
                }
            }
        }

        // restore tabLayout background
        TransitionDrawable transitionDrawable = (TransitionDrawable)mActivity.getTabLayout().getBackground();
        transitionDrawable.reverseTransition(300);

        // enable paging
        mActivity.getViewPager().setPagingEnabled(true);

        // restore statusBar color
        if (Build.VERSION.SDK_INT >= 21) {
            mActivity.getWindow().setStatusBarColor(ContextCompat.getColor(mActivity, R.color.colorPrimaryDark));
        }

        mAdapter.deselectAllItems();
        mActionMode = null;
    }
}
