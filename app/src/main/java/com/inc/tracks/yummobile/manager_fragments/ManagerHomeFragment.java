package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.inc.tracks.yummobile.R;


public class ManagerHomeFragment extends Fragment implements View.OnClickListener{

    private OnFragmentInteractionListener mListener;

    public ManagerHomeFragment() {
        // Required empty public constructor
    }


    public static ManagerHomeFragment newInstance() {
        ManagerHomeFragment fragment = new ManagerHomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_home,
                container, false);

        fragView.findViewById(R.id.btn_manageRestaurants).setOnClickListener(this);

        fragView.findViewById(R.id.btn_manageOrders).setOnClickListener(this);

        fragView.findViewById(R.id.btn_manageTransporters).setOnClickListener(this);

        mListener.onFragmentInteraction(R.layout.fragment_manager_home);

        return fragView;
    }

    private void onButtonPressed(int buttonId) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId);
    }

    @Override
    public void onClick(View v) {
        onButtonPressed(v.getId());
    }
}
