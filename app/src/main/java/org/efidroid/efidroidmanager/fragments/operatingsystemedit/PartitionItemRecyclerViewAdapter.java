package org.efidroid.efidroidmanager.fragments.operatingsystemedit;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.fragments.operatingsystemedit.PartitionItemFragment.OnListFragmentInteractionListener;
import org.efidroid.efidroidmanager.models.OperatingSystem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link OperatingSystem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class PartitionItemRecyclerViewAdapter extends RecyclerView.Adapter<PartitionItemRecyclerViewAdapter.ViewHolder> {

    private final List<OperatingSystem.Partition> mValues;
    private final OnListFragmentInteractionListener mListener;
    private final OperatingSystem mOperatingSystem;

    public PartitionItemRecyclerViewAdapter(OperatingSystem os, OnListFragmentInteractionListener listener) {
        mOperatingSystem = os;
        mListener = listener;
        mValues = mOperatingSystem.getPartitions();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_partitionitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).name);
        holder.mContentView.setText(mValues.get(position).path);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public OperatingSystem.Partition mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(android.support.design.R.id.text);
            mContentView = (TextView) view.findViewById(android.support.design.R.id.text2);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
