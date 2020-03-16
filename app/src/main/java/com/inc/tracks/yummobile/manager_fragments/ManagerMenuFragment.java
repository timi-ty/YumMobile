package com.inc.tracks.yummobile.manager_fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.inc.tracks.yummobile.components.MenuItem;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.RestaurantItem;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.utils.GlideApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;


public class ManagerMenuFragment extends Fragment implements View.OnClickListener{
    private static final String ARG_RST_ITEM = "rstItem";

    private final static int RC_GET_IMAGE = 9011;


    private RestaurantItem restaurantItem;

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private RecyclerView rvMenu;

    private MenuRVAdapter menuRVAdapter;


    private ImageView tempImageView;

    private String tempItemId;

    private HashMap<String, Uri> uriPool = new HashMap<>();

    public ManagerMenuFragment() {
        // Required empty public constructor
    }


    public static ManagerMenuFragment newInstance(RestaurantItem restaurantItem) {
        ManagerMenuFragment fragment = new ManagerMenuFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RST_ITEM, restaurantItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            restaurantItem = (RestaurantItem) getArguments().getSerializable(ARG_RST_ITEM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_menu, container, false);
        myLayout = fragView.findViewById(R.id.layout_menuManager);

        TextView tvMenuTitle = fragView.findViewById(R.id.tv_menuTitle);
        rvMenu = fragView.findViewById(R.id.rv_entireMenu);
        FloatingActionButton fab_addMenuItem = fragView.findViewById(R.id.fab_addMenuItem);

        rvMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        if(menuRVAdapter == null){
            menuRVAdapter = new MenuRVAdapter();
            menuRVAdapter.getFilter().filter("");
        }
        if(rvMenu.getAdapter() == null){
            rvMenu.setAdapter(menuRVAdapter);
        }

        String menuTitle = restaurantItem.getName() + " Menu";
        tvMenuTitle.setText(menuTitle);

        fab_addMenuItem.setOnClickListener(this);

        mListener.onFragmentInteraction(R.layout.fragment_manager_menu);

        return fragView;
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab_addMenuItem){
            addNewMenuItem();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == RC_GET_IMAGE && resultCode == RESULT_OK && data != null){

            uriPool.put(tempItemId, data.getData());

            Log.d("SparseArray",  "key =  " + tempItemId);

            GlideApp.with(Objects.requireNonNull(getActivity()))
                    .load(data.getData()).into(tempImageView);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int interactionId);
    }

    public void onSearchQuery(String newText){
        if(isVisible()){
            menuRVAdapter.getFilter().filter(newText);
        }
    }

    private void addNewMenuItem(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef = db.collection("restaurants")
                                        .document(restaurantItem.getId())
                                        .collection("menuItems")
                                        .document();

        MenuItem menuItem = new MenuItem();
        menuItem.setId(docRef.getId());

        menuRVAdapter.menuItems.add(menuItem);

        int position = menuRVAdapter.menuItems.size() - 1;

        menuRVAdapter.notifyItemInserted(position);

        rvMenu.scrollToPosition(position);

        final Snackbar notify =
                Snackbar.make(myLayout, "Edit And Save New Menu Item.", Snackbar.LENGTH_LONG)
                .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark));

        notify.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify.dismiss();
            }
        });
        notify.show();
    }

    public class MenuRVAdapter extends RecyclerView.Adapter<MenuRVAdapter.MenuItemViewHolder>
            implements Filterable {

        private final String TAG = "FireStore";
        private ArrayList<MenuItem> menuItems = new ArrayList<>();
        List<MenuItem> menuItemsFiltered = new ArrayList<>();
        FirebaseFirestore fireDB;

        MenuRVAdapter() {
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
                                MenuItem menuItem;
                                int position = -1;
                                switch (dc.getType()) {
                                    case ADDED:
                                        Log.d(TAG, "New menuItem: " + dc.getDocument().getData());

                                        menuItem = dc.getDocument().toObject(MenuItem.class);

                                        boolean itemExists = false;
                                        for(int i = menuItems.size()-1; i >=0; i--){
                                            if(menuItems.get(i).getId() != null){
                                                itemExists = menuItems.get(i).getId()
                                                        .equals(menuItem.getId());
                                                if(itemExists) break;
                                            }
                                        }
                                        if(!itemExists){
                                            menuItems.add(menuItem);

                                            notifyItemInserted(menuItems.size() - 1);
                                        }
                                        break;
                                    case MODIFIED:
                                        Log.d(TAG, "Modified menuItem: " + dc.getDocument().getData());

                                        menuItem = dc.getDocument().toObject(MenuItem.class);

                                        for(MenuItem item : menuItems){
                                            if(item.getId() != null){
                                                if(item.getId().equals(menuItem.getId())){
                                                    position = menuItems.indexOf(item);
                                                    break;
                                                }
                                            }
                                        }
                                        if(position >= 0){
                                            notifyItemChanged(position);
                                        }
                                        break;
                                    case REMOVED:
                                        Log.d(TAG, "Removed menuItem: " + dc.getDocument().getData());

                                        menuItem = dc.getDocument().toObject(MenuItem.class);

                                        for(MenuItem item : menuItems){
                                            if(item.getId() != null){
                                                if(item.getId().equals(menuItem.getId())){
                                                    position = menuItems.indexOf(item);
                                                    break;
                                                }
                                            }
                                        }

                                        if(position >= 0){
                                            menuItems.remove(position);
                                            notifyItemRemoved(position);
                                        }
                                        break;
                                }
                            }

                        }
                    };

            fireDB.collection("restaurants")
                    .document(restaurantItem.getId())
                    .collection("menuItems")
                    .addSnapshotListener(dataChangedListener);
        }

        @NonNull
        @Override
        public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View menuItemView = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_manage_menu, viewGroup, false);
            return new MenuItemViewHolder(menuItemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MenuItemViewHolder viewHolder, int position) {
            MenuItem menuItem = menuItemsFiltered.get(position);
            viewHolder.bindView(menuItem);
        }

        @Override
        public int getItemCount() {
            return menuItemsFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();

                    if (charString.isEmpty()) {
                        menuItemsFiltered = menuItems;
                    } else {
                        List<MenuItem> filteredList = new ArrayList<>();
                        for (MenuItem item : menuItems) {
                            if (item.getName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(item);
                            }
                        }

                        menuItemsFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = menuItemsFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    menuItemsFiltered = (ArrayList<MenuItem>) filterResults.values;

                    notifyDataSetChanged();
                }
            };
        }

        class MenuItemViewHolder extends RecyclerView.ViewHolder implements
                View.OnClickListener {
            MenuItem menuItem;

            TextView txtName;
            TextView txtDesc;
            TextView txtPrice;
            ImageView imgLogo;
            Button btnSave;
            ProgressBar pbSaving;

            MenuItemViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txt_menuItemName);
                txtDesc = itemView.findViewById(R.id.txt_menuItemDesc);
                txtPrice = itemView.findViewById(R.id.txt_menuItemPrice);
                imgLogo = itemView.findViewById(R.id.img_menuItemLogo);

                ImageButton btnDelete = itemView.findViewById(R.id.btn_deleteMenuItem);
                btnSave = itemView.findViewById(R.id.btn_saveMenuItem);

                pbSaving = itemView.findViewById(R.id.pb_saveMenuItem);

                btnDelete.setOnClickListener(this);
                btnSave.setOnClickListener(this);
                imgLogo.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_deleteMenuItem:
                        deleteMenuItem();
                        break;
                    case R.id.btn_saveMenuItem:
                        if(verifyUserInput()){
                            saveMenuItem();
                        }
                        break;
                    case R.id.img_menuItemLogo:
                        pickRestaurantImage();
                        break;
                }
            }

            void bindView(final MenuItem menuItem){
                this.menuItem = menuItem;

                TextWatcher textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        determineSaveState();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                };

                txtName.addTextChangedListener(textWatcher);
                txtDesc.addTextChangedListener(textWatcher);
                txtPrice.addTextChangedListener(textWatcher);

                txtName.setText(menuItem.getName());
                txtDesc.setText(menuItem.getDescription());
                txtPrice.setText(String.valueOf(menuItem.getPrice()));

                refreshThumbnail();
            }

            private void determineSaveState(){
                boolean check1 = txtName.getText().toString().equals(menuItem.getName());
                boolean check2 = txtDesc.getText().toString().equals(menuItem.getDescription());
                boolean check3 = txtPrice.getText().toString().equals(Integer.toString(menuItem.getPrice()));


                if(!(check1 && check2 && check3)){
                    btnSave.setVisibility(View.VISIBLE);
                }
                else {
                    btnSave.setVisibility(View.INVISIBLE);
                }
            }

            private void refreshThumbnail() {
                try {
                    StorageReference imageRef = UserAuth.firebaseStorage
                            .getReferenceFromUrl(menuItem.getImgRef());

                    GlideApp.with(imgLogo.getContext())
                            .load(imageRef)
                            .into(imgLogo);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

            private void pickRestaurantImage(){
                if(UserAuth.isAdmin){
                    Intent upImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    upImageIntent.setType("image/jpeg");
                    upImageIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                    startActivityForResult(Intent.createChooser(upImageIntent,
                            "Choose Restaurant Image"), RC_GET_IMAGE);

                    tempImageView = imgLogo;
                    tempItemId = menuItem.getId();

                    btnSave.setVisibility(View.VISIBLE);
                }
                else{
                    Snackbar.make(myLayout.findViewById(R.id.layout_restEditor),
                            "Admin Verification Failed.", Snackbar.LENGTH_LONG).show();
                }
            }

            private void saveMenuItem(){
                setSavingUi(true);

                Uri imgUri = uriPool.get(menuItem.getId());

                menuItem.setName(txtName.getText().toString());
                menuItem.setDescription(txtDesc.getText().toString());
                menuItem.setPrice(Integer.parseInt(txtPrice.getText().toString()));

                if(imgUri != null && imgUri.getLastPathSegment() != null){
                    final StorageReference ref = UserAuth.firebaseMenuStorageRef
                            .child(restaurantItem.getId())
                            .child(imgUri.getLastPathSegment());
                    ref.putFile(imgUri)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                                    menuItem.setImgRef(ref.toString()
                                            .replaceAll("%3A", ":"));
                                    uploadToFirestore();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar.make(myLayout,
                                            "Upload Failed. Restaurant Not Saved.",
                                            Snackbar.LENGTH_LONG).show();

                                    setSavingUi(false);
                                }
                            });
                }
                else {
                    uploadToFirestore();
                    if(menuItem.getImgRef() == null){
                        Snackbar.make(myLayout,
                                "Saving Menu Item Without Image.",
                                Snackbar.LENGTH_LONG).show();
                    }
                }
            }

            private void uploadToFirestore(){
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                DocumentReference docRef;

                if(menuItem.getId() == null){
                    docRef = db.collection("restaurants")
                            .document(restaurantItem.getId())
                            .collection("menuItems")
                            .document();
                }
                else{
                    docRef = db.collection("restaurants")
                            .document(restaurantItem.getId())
                            .collection("menuItems")
                            .document(menuItem.getId());
                }

                menuItem.setId(docRef.getId());

                docRef.set(menuItem, SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                setSavingUi(false);
                                Snackbar.make(myLayout,
                                        "Menu Item Saved Successfully.", Snackbar.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                setSavingUi(false);
                                Snackbar.make(myLayout,
                                        "Upload Failed. Menu Item Not Saved.",
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });
            }

            private void deleteMenuItem(){
                Snackbar.make(myLayout,
                        "Delete " + txtName.getText() + " Permanently?", Snackbar.LENGTH_LONG)
                        .setAction("Delete", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String docId = menuItem.getId();
                                if(docId == null){
                                    menuItems.remove(menuItem);

                                    notifyItemRemoved(getAdapterPosition());
                                }
                                else{
                                    fireDB.collection("restaurants")
                                            .document(restaurantItem.getId())
                                            .collection("menuItems")
                                            .document(docId)
                                            .delete()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    Snackbar.make(myLayout,
                                                            "Menu Item Deleted Successfully.", Snackbar.LENGTH_LONG).show();
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
                            }
                        })
                        .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
                        .show();
            }

            private boolean verifyUserInput(){
                boolean isVerified = true;

                String name = txtName.getText().toString();
                String desc = txtDesc.getText().toString();
                String price = txtPrice.getText().toString();

                if(name.equals("")){
                    txtName.setHint("Give a name");
                    txtName.setHintTextColor(getResources().getColor(R.color.colorError));
                    isVerified = false;
                }

                if(desc.equals("")){
                    txtDesc.setHint("Write a description");
                    txtDesc.setHintTextColor(getResources().getColor(R.color.colorError));
                    isVerified = false;
                }

                if(price.equals("")){
                    txtPrice.setHint("Set a price");
                    txtPrice.setHintTextColor(getResources().getColor(R.color.colorError));
                    isVerified = false;
                }

                try{
                    Integer.parseInt(price);
                }
                catch (NumberFormatException e){
                    isVerified = false;
                    e.printStackTrace();
                }

                return isVerified;
            }

            private void setSavingUi(boolean loading){
                txtName.setEnabled(!loading);
                txtDesc.setEnabled(!loading);
                txtPrice.setEnabled(!loading);

                btnSave.setVisibility(!loading ? View.VISIBLE : View.INVISIBLE);
                pbSaving.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);

                if(!loading) determineSaveState();
            }
        }
    }
}
