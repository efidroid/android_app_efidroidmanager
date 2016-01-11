package org.efidroid.efidroidmanager.fragments.operatingsystemedit;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.models.OperatingSystem;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link RecyclerView.Adapter} that can display a {@link OperatingSystem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ReplacementItemRecyclerViewAdapter extends RecyclerView.Adapter<ReplacementItemRecyclerViewAdapter.ViewHolder> implements OperatingSystem.OperatingSystemChangeListener {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private final List<Object> mValues = new ArrayList<>();
    private final ArrayList<Integer> mSelectedItems = new ArrayList<>();
    private final OnListFragmentInteractionListener mListener;
    private final OperatingSystem mOperatingSystem;
    private RecyclerView mRecyclerView = null;

    @Override
    public void onOperatingSystemChanged() {
        rebuildItems();
    }

    public static class ReplacementItem {
        public String name;
        public String value;

        public ReplacementItem(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class HeaderItem {
        public String title;

        public HeaderItem(String title) {
            this.title = title;
        }
    }

    public ReplacementItemRecyclerViewAdapter(OperatingSystem os, OnListFragmentInteractionListener listener) {
        mOperatingSystem = os;
        mListener = listener;
        rebuildItems();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
        mOperatingSystem.addChangeListener(this);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mOperatingSystem.removeChangeListener(this);
        mRecyclerView = null;
    }

    private void rebuildItems() {
        mValues.clear();

        mValues.add(new HeaderItem("Binaries"));

        String value = mOperatingSystem.getReplacementKernel();
        mValues.add(new ReplacementItem("kernel", value));

        value = mOperatingSystem.getReplacementRamdisk();
        mValues.add(new ReplacementItem("ramdisk", value));

        value = mOperatingSystem.getReplacementDT();
        mValues.add(new ReplacementItem("dt", value));

        mValues.add(new HeaderItem("Commandline"));
        mValues.addAll(mOperatingSystem.getCmdline());
    }

    @Override
    public int getItemViewType(int position) {
        if (mValues.get(position) instanceof OperatingSystem.CmdlineItem) {
            return TYPE_ITEM;
        }
        else if (mValues.get(position) instanceof ReplacementItem) {
            return TYPE_ITEM;
        }
        else if (mValues.get(position) instanceof HeaderItem) {
            return TYPE_HEADER;
        }
        else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            default:
            case TYPE_ITEM:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_replacemenitem, parent, false);
                break;
            case TYPE_HEADER:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_replacemenitem_header, parent, false);
                break;
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        if (holder.mItem instanceof OperatingSystem.CmdlineItem) {
            OperatingSystem.CmdlineItem item = (OperatingSystem.CmdlineItem)mValues.get(position);
            holder.mTitleView.setText(item.name);
            holder.mSubtitleView.setText(item.value);
            holder.mPosition = position;
        }

        else if (holder.mItem instanceof ReplacementItem) {
            ReplacementItem item = (ReplacementItem)mValues.get(position);
            holder.mTitleView.setText(item.name);
            holder.mSubtitleView.setText(item.value);
            holder.mPosition = position;
        }

        else if (holder.mItem instanceof HeaderItem) {
            HeaderItem item = (HeaderItem)mValues.get(position);
            holder.mTitleView.setText(item.title);
            holder.mType = TYPE_HEADER;
            holder.mPosition = position;
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    if(holder.mItem instanceof OperatingSystem.CmdlineItem)
                        mListener.onCmdlineItemClicked(v, (OperatingSystem.CmdlineItem)holder.mItem);
                }
            }
        });

        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (null != mListener) {
                    if (holder.mItem instanceof OperatingSystem.CmdlineItem)
                        mListener.onCmdlineItemLongClicked(v, (OperatingSystem.CmdlineItem) holder.mItem);

                    /*int pos = mValues.indexOf(item);
                    mOperatingSystem.getCmdline().remove(item);
                    mOperatingSystem.notifyChange();
                    notifyItemRemoved(pos);*/
                }
                return true;
            }
        });

        // activation status
        holder.mView.setActivated(mSelectedItems.contains(position));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void setSelected(int position, boolean selected) {
        mSelectedItems.remove(new Integer(position));
        if(selected)
            mSelectedItems.add(new Integer(position));
    }

    public boolean isSelected(int position) {
        return mSelectedItems.indexOf(new Integer(position))>=0;
    }

    public int getItemPosition(Object item) {
        return mValues.indexOf(item);
    }

    public ArrayList<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public void removeSelectedItems() {
        for(Integer position : mSelectedItems) {
            Object item = mValues.get(position);
            mOperatingSystem.getCmdline().remove(item);
        }
        mSelectedItems.clear();
        mOperatingSystem.notifyChange();
    }

    public void deselectAllItems() {
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mSubtitleView;
        public Object mItem;
        public int mType;
        public int mPosition;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = (TextView) view.findViewById(android.support.design.R.id.text);
            mSubtitleView = (TextView) view.findViewById(android.support.design.R.id.text2);
            mType = TYPE_ITEM;
            mPosition = 0;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTitleView.getText() + "'";
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onCmdlineItemClicked(View v, OperatingSystem.CmdlineItem item);
        void onCmdlineItemLongClicked(View v, OperatingSystem.CmdlineItem item);
    }
}
