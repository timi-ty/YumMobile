package com.inc.tracks.yummobile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;


public class ManagerRestaurantsFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    public ManagerRestaurantsFragment() {
        // Required empty public constructor
    }


    public static ManagerRestaurantsFragment newInstance(String param1, String param2) {
        ManagerRestaurantsFragment fragment = new ManagerRestaurantsFragment();
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
        View fragView = inflater.inflate(R.layout.fragment_manager_restaurants, container, false);
        myLayout = fragView.findViewById(R.id.layout_restManager);
        RecyclerView rvRestaurants = fragView.findViewById(R.id.rv_allRestaurants);
        FloatingActionButton fab_addRestaurant = fragView.findViewById(R.id.fab_addRestaurant);

        rvRestaurants.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRestaurants.setAdapter(new RestaurantsRVAdapter());

        fab_addRestaurant.setOnClickListener(this);

        mListener.onFragmentInteraction(R.layout.fragment_manager_restaurants);

        return fragView;
    }

    public void onButtonPressed(int buttonId, RestaurantItem restaurantItem) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId);
            mListener.onFragmentInteraction(buttonId, restaurantItem);
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab_addRestaurant){
            onButtonPressed(v.getId(), new RestaurantItem());
        }
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int interactionId);
        void onFragmentInteraction(int interactionId, RestaurantItem restaurantItem);
    }

    public class RestaurantsRVAdapter extends RecyclerView.Adapter<RestaurantsRVAdapter.RstViewHolder>{

        private final String TAG = "FireStore";
        private ArrayList<RestaurantItem> restaurantItems = new ArrayList<>();
        FirebaseFirestore fireDB;

        public RestaurantsRVAdapter() {
            fireDB = FirebaseFirestore.getInstance();

            EventListener<QuerySnapshot> dataChangedListener =
                    new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot snapshots,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w(TAG, "listen:error", e);
                                return;
                            }

                            if (snapshots == null) {
                                Log.w(TAG, "snapshot not found:error");
                                return;
                            }

                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                                RestaurantItem restaurantItem;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        restaurantItem.setId(dc.getDocument().getId());

                                        restaurantItems.add(restaurantItem);

                                        notifyItemInserted(restaurantItems.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        for(RestaurantItem item : restaurantItems){
                                            if(item.getId().equals(restaurantItem.getId())){
                                                position = restaurantItems.indexOf(item);
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed Restaurant: " + dc.getDocument().getData());

                                        restaurantItem = dc.getDocument().toObject(RestaurantItem.class);

                                        for(RestaurantItem item : restaurantItems){
                                            if(item.getId().equals(restaurantItem.getId())){
                                                position = restaurantItems.indexOf(item);
                                                Log.d(TAG, "Removed Restaurant Notified!: " + item.getId()
                                                        + " => " + restaurantItem.getId() + " => " + restaurantItems.indexOf(item));
                                            }
                                        }

                                        if(position >= 0){
                                            restaurantItems.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("restaurants").addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public RstViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View restaurantView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_restaurant, viewGroup, false);
            return new RstViewHolder(restaurantView);
        }

        @Override
        public void onBindViewHolder(@NonNull RstViewHolder viewHolder, int position) {
            RestaurantItem restaurantItem = restaurantItems.get(position);
            viewHolder.bindView(restaurantItem);
        }

        @Override
        public int getItemCount() {
            return restaurantItems.size();
        }

        class RstViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvName;
            TextView tvDesc;
            TextView tvAddress;
            ImageView imgLogo;

            RstViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_restaurantName);
                tvDesc = itemView.findViewById(R.id.tv_restaurantDesc);
                tvAddress = itemView.findViewById(R.id.tv_restaurantAddress);
                imgLogo = itemView.findViewById(R.id.img_restaurantLogo);

                ImageButton btnDelete = itemView.findViewById(R.id.btn_deleteResataurant);

                itemView.setOnClickListener(this);
                btnDelete.setOnClickListener(this);
            }

            void bindView(RestaurantItem restaurantItem){
                tvName.setText(restaurantItem.getName());
                tvDesc.setText(restaurantItem.getDescription());
                tvAddress.setText(restaurantItem.getAddress());

                refreshThumbnail(restaurantItem);
            }

            private void refreshThumbnail(RestaurantItem restaurantItem) {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(restaurantItem.getImgRef());

                    GlideApp.with(imgLogo.getContext())
                            .load(imageRef)
                            .into(imgLogo);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_deleteResataurant:
                        deleteRestaurant();
                        break;
                    case R.id.item_manageRestaurant:
                        onButtonPressed(v.getId(), restaurantItems.get(getAdapterPosition()));
                }
            }

            private void deleteRestaurant(){
                Snackbar.make(myLayout,
                        "Delete " + tvName.getText() + " Permanently?", Snackbar.LENGTH_LONG)
                        .setAction("Delete", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int position = getAdapterPosition();
                                String docId = restaurantItems.get(position).getId();
                                fireDB.collection("restaurants")
                                        .document(docId)
                                        .delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Snackbar.make(myLayout,
                                                        "Restaurant Deleted Successfully.", Snackbar.LENGTH_LONG).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(myLayout,
                                                        "Failed. Could Not Delete.", Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        })
                        .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
                        .show();
            }
        }
    }
}
