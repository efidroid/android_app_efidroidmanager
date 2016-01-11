package org.efidroid.efidroidmanager.fragments.operatingsystemedit;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;
import org.efidroid.efidroidmanager.models.OperatingSystem;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GeneralFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GeneralFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GeneralFragment extends Fragment {
    private OperatingSystem mOperatingSystem = null;
    private OperatingSystemEditActivity mListener;

    public GeneralFragment() {
        // Required empty public constructor
    }

    public static GeneralFragment newInstance(OperatingSystem os) {
        GeneralFragment fragment = new GeneralFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_general, container, false);

        mOperatingSystem = mListener.getOperatingSystem();

        EditText name = (EditText)view.findViewById(R.id.name);
        EditText description = (EditText)view.findViewById(R.id.description);

        name.setText(mOperatingSystem.getName());
        description.setText(mOperatingSystem.getDescription());

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OperatingSystemEditActivity) {
            mListener = (OperatingSystemEditActivity) context;
        } else {
            //throw new RuntimeException(context.toString()
              //      + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mOperatingSystem = null;
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
