package com.inc.tracks.yummobile;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ActiveOrdersFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ActiveOrdersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ActiveOrdersFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public ActiveOrdersFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ActiveOrdersFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ActiveOrdersFragment newInstance(String param1, String param2) {
        ActiveOrdersFragment fragment = new ActiveOrdersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragView = inflater.inflate(R.layout.fragment_active_orders, container, false);
        RecyclerView activeOrders = fragView.findViewById(R.id.rv_activeOrders);

        activeOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        activeOrders.setAdapter(new ActiveOrdersRVAdapter());

        return fragView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public class ActiveOrdersRVAdapter extends RecyclerView.Adapter<ActiveOrdersRVAdapter.MenuItemViewHolder>{

        @NonNull
        @Override
        public ActiveOrdersRVAdapter.MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_active_order, viewGroup, false);
            return new ActiveOrdersRVAdapter.MenuItemViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull ActiveOrdersRVAdapter.MenuItemViewHolder viewHolder, int i) {
            viewHolder.bindView();
        }

        @Override
        public int getItemCount() {
            // TODO: 10/14/2019 Replace '15' with the number food items the restaurant sells
            return 5;
        }

        class MenuItemViewHolder extends RecyclerView.ViewHolder{

            MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);
            }

            void bindView(){
                // TODO: 10/14/2019 Add onClickListener(s) for the views in this view holder
            }
        }
    }
}
