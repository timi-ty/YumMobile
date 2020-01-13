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
 * {@link OrderSummaryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OrderSummaryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OrderSummaryFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public OrderSummaryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OrderSummaryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OrderSummaryFragment newInstance(String param1, String param2) {
        OrderSummaryFragment fragment = new OrderSummaryFragment();
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
        View fragView =  inflater.inflate(R.layout.fragment_order_summary, container, false);

        RecyclerView orderSummary = fragView.findViewById(R.id.rv_orderSummary);

        orderSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        orderSummary.setAdapter(new OrderSummaryRVAdapter());

        fragView.findViewById(R.id.btn_cardPay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed(Uri.fromParts("Fragment",
                        "onClick", "OrderSummary"));
            }
        });
       fragView.findViewById(R.id.btn_deliveryPay).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               onButtonPressed(Uri.fromParts("Fragment",
                       "onClick", "OrderSummary"));
           }
       });

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

    public class OrderSummaryRVAdapter extends RecyclerView.Adapter<OrderSummaryRVAdapter.RstViewHolder>{

        @NonNull
        @Override
        public OrderSummaryRVAdapter.RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_order_summary, viewGroup, false);
            return new OrderSummaryRVAdapter.RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderSummaryRVAdapter.RstViewHolder viewHolder, int i) {
            viewHolder.bindView();
        }

        @Override
        public int getItemCount() {
            // TODO: 10/14/2019 Replace '5' with the number of restaurants around the user
            return 5;
        }

        class RstViewHolder extends RecyclerView.ViewHolder{

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
            }

            void bindView(){
                // TODO: 10/14/2019 Add onClickListener(s) for the views in this view holder
                // TODO: 10/21/2019 populate the item view with its details here
            }
        }
    }
}
