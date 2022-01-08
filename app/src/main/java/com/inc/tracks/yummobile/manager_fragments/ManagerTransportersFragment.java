package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.UserPrefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class ManagerTransportersFragment extends Fragment{

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private TransportersRVAdapter transportersRVAdapter;

    public ManagerTransportersFragment() {
        // Required empty public constructor
    }


    public static ManagerTransportersFragment newInstance() {
        ManagerTransportersFragment fragment = new ManagerTransportersFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_transporters, container, false);
        myLayout = fragView.findViewById(R.id.layout_transManager);
        RecyclerView rvTransporters = fragView.findViewById(R.id.rv_allTransporters);

        rvTransporters.setLayoutManager(new LinearLayoutManager(getContext()));
        if(transportersRVAdapter == null){
            transportersRVAdapter = new TransportersRVAdapter();
        }
        transportersRVAdapter.getFilter().filter("");
        rvTransporters.setAdapter(transportersRVAdapter);

        mListener.onFragmentInteraction(R.layout.fragment_manager_transporters);

        return fragView;
    }

    private void onButtonPressed(int buttonId, UserPrefs transporter) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId);
            mListener.onFragmentInteraction(buttonId, transporter);
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
        void onFragmentInteraction(int interactionId);
        void onFragmentInteraction(int interactionId, UserPrefs transporter);
    }

    public void onSearchQuery(String newText){
        if(isVisible()){
            transportersRVAdapter.getFilter().filter(newText);
        }
    }

    public class TransportersRVAdapter extends
            RecyclerView.Adapter<TransportersRVAdapter.TransViewHolder> implements Filterable {

        private final String TAG = "FireStore";
        private HashMap<UserPrefs, String> transporters = new HashMap<>();
        private ArrayList<UserPrefs> transportersList = new ArrayList<>();
        List<UserPrefs> transportersListFiltered = new ArrayList<>();
        FirebaseFirestore fireDB;

        TransportersRVAdapter() {
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
                                UserPrefs transporter;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New Transporter: " + dc.getDocument().getData());

                                        transporter = dc.getDocument().toObject(UserPrefs.class);

                                        transporter.setId(dc.getDocument().getId());

                                        transportersList.add(transporter);

                                        notifyItemInserted(transportersList.size() - 1);
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified Transporter: " + dc.getDocument().getData());

                                        transporter = dc.getDocument().toObject(UserPrefs.class);

                                        for(UserPrefs item : transportersList){
                                            if(item.getId().equals(transporter.getId())){
                                                position = transportersList.indexOf(item);
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed Transporter: " + dc.getDocument().getData());

                                        transporter = dc.getDocument().toObject(UserPrefs.class);

                                        for(UserPrefs item : transportersList){
                                            if(item.getId().equals(transporter.getId())){
                                                position = transportersList.indexOf(item);
                                                Log.d(TAG, "Removed Transporter Notified!: " + item.getId()
                                                        + " => " + transporter.getId() + " => " + transportersList.indexOf(item));
                                            }
                                        }

                                        if(position >= 0){
                                            transportersList.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("transporters").addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public TransViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View transView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_transporter, viewGroup, false);
            return new TransViewHolder(transView);
        }

        @Override
        public void onBindViewHolder(@NonNull TransViewHolder viewHolder, int position) {
            UserPrefs transporter = transportersListFiltered.get(position);
            viewHolder.bindView(transporter);
        }

        @Override
        public int getItemCount() {
            return transportersListFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();

                    if (charString.isEmpty()) {
                        transportersListFiltered = transportersList;
                    } else {
                        List<UserPrefs> filteredList = new ArrayList<>();
                        for (UserPrefs item : transportersList) {
                            if (item.getUserName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(item);
                            }
                        }

                        transportersListFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = transportersListFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    transportersListFiltered = (ArrayList<UserPrefs>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }

        class TransViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            TextView tvName;
            TextView tvPhone;
            ImageView imgLogo;

            UserPrefs transporter;

            TransViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_transporterName);
                tvPhone = itemView.findViewById(R.id.tv_transporterPhone);
                imgLogo = itemView.findViewById(R.id.img_transporterAvatar);

                ImageButton btnDelete = itemView.findViewById(R.id.btn_deleteTransporter);

                itemView.setOnClickListener(this);
                btnDelete.setOnClickListener(this);
            }

            void bindView(final UserPrefs transporter){
                this.transporter = transporter;

                if(transporter.getUserName() == null){
                    fireDB.collection("users")
                            .document(transporter.getId())
                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            UserPrefs userEntry = documentSnapshot.toObject(UserPrefs.class);
                            assert  userEntry != null;
                            transporter.setUserName(userEntry.getUserName());
                            transporter.setUserPhone(userEntry.getUserPhone());

                            notifyItemChanged(getAdapterPosition());
                        }
                    });
                }
                else{
                    tvName.setText(transporter.getUserName());
                    tvPhone.setText(transporter.getUserPhone());
                    tvPhone.setOnClickListener(this);
                }

            }

            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_deleteTransporter:
                        deleteTransporter();
                        break;
                    case R.id.item_manageTransporters:
                        onButtonPressed(v.getId(), transporter);
                        break;
                    case R.id.tv_transporterPhone:
                        callTransporter();
                }
            }

            private void callTransporter(){
                if(transporter != null){
                    Uri phone = Uri.parse("tel:" + transporter.getUserPhone());

                    Intent dialerIntent = new Intent(Intent.ACTION_DIAL, phone);

                    if (dialerIntent.resolveActivity(requireActivity()
                            .getPackageManager()) != null) {
                        startActivity(dialerIntent);
                    }
                }
            }

            private void deleteTransporter(){
                Snackbar.make(myLayout,
                        "Revoke " + tvName.getText() + " Authorization To Transport?", Snackbar.LENGTH_LONG)
                        .setAction("Revoke", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                fireDB.collection("transporters")
                                        .document(transporter.getId())
                                        .delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Snackbar.make(myLayout,
                                                        "Transporter Removed Successfully.", Snackbar.LENGTH_LONG).show();
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
