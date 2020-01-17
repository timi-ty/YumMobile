package com.inc.tracks.yummobile;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

import static android.app.Activity.RESULT_OK;


public class ManagerRestaurantsEditorFragment extends Fragment implements
        View.OnClickListener{

    private final static int RC_GET_IMAGE = 9011;


    private static final String ARG_REST_ITEM = "paramRestItem";
    private static final String ARG_REST_IMG = "argRestImg";

    private RestaurantItem currentRestItem;
    private Uri restImgUri;

    private OnFragmentInteractionListener mListener;

    private ConstraintLayout myLayout;

    private EditText txtRestaurantName;
    private EditText txtRestaurantDesc;
    private EditText txtRestaurantAddress;

    private ImageView imgRestaurantLogo;
    private TextView tvRestaurantName;
    private TextView tvRestaurantDesc;
    private TextView tvRestaurantAddress;

    private Button btnUpImage;
    private Button btnSaveRestaurant;
    private Button btnManageMenu;
    private ImageButton btnGeoTagRestaurant;

    private ProgressBar pbUploading;

    public ManagerRestaurantsEditorFragment() {
        // Required empty public constructor
    }


    public static ManagerRestaurantsEditorFragment newInstance(RestaurantItem restaurantItem) {
        ManagerRestaurantsEditorFragment fragment = new ManagerRestaurantsEditorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_REST_ITEM, restaurantItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentRestItem = (RestaurantItem) getArguments().getSerializable(ARG_REST_ITEM);
            restImgUri = getArguments().getParcelable(ARG_REST_IMG);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragView = inflater.inflate(R.layout.fragment_manager_restaurant_editor,
                container, false);

        myLayout = fragView.findViewById(R.id.layout_restEditor);
        mListener.onFragmentInteraction(R.layout.fragment_manager_restaurant_editor);

        btnUpImage = fragView.findViewById(R.id.btn_upRestaurantImage);
        btnSaveRestaurant = fragView.findViewById(R.id.btn_saveRestaurant);
        btnManageMenu = fragView.findViewById(R.id.btn_manageMenu);
        btnGeoTagRestaurant = fragView.findViewById(R.id.btn_geoTagRestaurant);

        txtRestaurantName = fragView.findViewById(R.id.txt_restaurantName);
        txtRestaurantDesc = fragView.findViewById(R.id.txt_restaurantDesc);
        txtRestaurantAddress = fragView.findViewById(R.id.txt_restaurantAddress);

        imgRestaurantLogo = fragView.findViewById(R.id.img_restaurantLogo);
        tvRestaurantName = fragView.findViewById(R.id.tv_restaurantName);
        tvRestaurantDesc = fragView.findViewById(R.id.tv_restaurantDesc);
        tvRestaurantAddress = fragView.findViewById(R.id.tv_restaurantAddress);

        pbUploading = fragView.findViewById(R.id.pb_restEditor);

        btnUpImage.setOnClickListener(this);
        btnSaveRestaurant.setOnClickListener(this);
        btnManageMenu.setOnClickListener(this);
        btnGeoTagRestaurant.setOnClickListener(this);

        synchronizeTextFields(txtRestaurantName, tvRestaurantName);
        synchronizeTextFields(txtRestaurantDesc, tvRestaurantDesc);
        synchronizeTextFields(txtRestaurantAddress, tvRestaurantAddress);

        txtRestaurantName.setText(currentRestItem.getName());
        txtRestaurantDesc.setText(currentRestItem.getDescription());
        txtRestaurantAddress.setText(currentRestItem.getAddress());

        updateEditorUi();

        refreshThumbnail();

        return fragView;
    }

    private void onButtonPressed(int buttonId) {
        if (mListener != null) {
            mListener.onFragmentInteraction(buttonId, currentRestItem);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_upRestaurantImage:
                pickRestaurantImage();
                break;
            case R.id.btn_saveRestaurant:
                if(verifyUserInput()){
                    saveRestaurantItem();
                }
                break;
            case R.id.btn_manageMenu:
                onButtonPressed(v.getId());
            case R.id.btn_geoTagRestaurant:
                if(currentRestItem.getLocation() == null){
                    geoTagRestaurant();
                }
                else {
                    removeGeoTag();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == RC_GET_IMAGE && resultCode == RESULT_OK && data != null){

            if (getArguments() != null) {
                getArguments().putParcelable(ARG_REST_IMG, data.getData());
                restImgUri = getArguments().getParcelable(ARG_REST_IMG);
            }

            GlideApp.with(Objects.requireNonNull(getActivity()))
                    .load(restImgUri).into(imgRestaurantLogo);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(int buttonId);
        void onFragmentInteraction(int buttonId, RestaurantItem restaurantItem);
        void onFragmentInteraction(int buttonId, MessageDialog messageDialog);
    }

    /* Worker methods below */

    private void geoTagRestaurant(){
        onStartGeoTagAttempt();
        if(UserAuth.mCurrentLocation != null){
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    btnGeoTagRestaurant.setImageDrawable(getResources().getDrawable(R.drawable.ic_minus));

                    GeoPoint geoPoint = new GeoPoint(UserAuth.mCurrentLocation.getLatitude(),
                            UserAuth.mCurrentLocation.getLongitude());
                    currentRestItem.setLocation(geoPoint);

                    Snackbar.make(myLayout, "Successfully Geo Tagged This Restaurant.",
                            Snackbar.LENGTH_SHORT).show();

                    onEndGeoTagAttempt();

                    updateEditorUi(true);
                }
            }, 3000);
        }
        else{
            Snackbar.make(myLayout, "Failed To Geo Tag. " +
                            "Ensure That You Granted Access To All Location Requests",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onStartGeoTagAttempt(){
        setLoadingUi(true);
        btnGeoTagRestaurant.setVisibility(View.INVISIBLE);
    }

    private void onEndGeoTagAttempt(){
        setLoadingUi(false);
        btnGeoTagRestaurant.setVisibility(View.VISIBLE);
    }

    private void removeGeoTag(){
        currentRestItem.setLocation(null);

        Snackbar.make(myLayout, "Geo Tag Removed.",
                Snackbar.LENGTH_SHORT).show();

        updateEditorUi(true);
    }

    private void synchronizeTextFields(EditText editableField, final TextView displayField){
        editableField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()>=1) displayField.setText(s);
                else{
                    switch (displayField.getId()){
                        case R.id.tv_restaurantName:
                            if(currentRestItem.getName() != null){
                                displayField.setText(currentRestItem.getName());
                            }
                            else{
                                displayField.setText(R.string.sample_restaurant_name);
                            }
                            break;
                        case R.id.tv_restaurantDesc:
                            if(currentRestItem.getDescription() != null){
                                displayField.setText(currentRestItem.getDescription());
                            }
                            else{
                                displayField.setText(R.string.sample_description);
                            }
                            break;
                        case R.id.tv_restaurantAddress:
                            if(currentRestItem.getAddress() != null){
                                displayField.setText(currentRestItem.getAddress());
                            }
                            else{
                                displayField.setText(R.string.sample_address);
                            }
                            break;
                    }
                }
                updateEditorUi();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void pickRestaurantImage(){
        if(UserAuth.isAdmin){
            Intent upImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
            upImageIntent.setType("image/jpeg");
            upImageIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(Intent.createChooser(upImageIntent,
                    "Choose Restaurant Image"), RC_GET_IMAGE);
        }
        else{
            Snackbar.make(myLayout.findViewById(R.id.layout_restEditor),
                    "Admin Verification Failed.", Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveRestaurantItem(){
        setLoadingUi(true);

        currentRestItem.setName(tvRestaurantName.getText().toString());
        currentRestItem.setDescription(tvRestaurantDesc.getText().toString());
        currentRestItem.setAddress(tvRestaurantAddress.getText().toString());

        if(restImgUri != null && restImgUri.getLastPathSegment() != null){
            final StorageReference ref = UserAuth.firebaseRstStorageRef
                    .child(restImgUri.getLastPathSegment());
            ref.putFile(restImgUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                            currentRestItem.setImgRef(ref.toString()
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
                            setLoadingUi(false);
                        }
                    });
        }
        else{
            uploadToFirestore();
        }
    }

    private void uploadToFirestore(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef;

        if(currentRestItem.getId() == null){
            docRef = db.collection("restaurants").document();
        }
        else{
            docRef = db.collection("restaurants").document(currentRestItem.getId());
        }

        currentRestItem.setId(docRef.getId());

        docRef.set(currentRestItem, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        setLoadingUi(false);
                        resetToDefault();
                        Snackbar.make(myLayout,
                                "Restaurant Saved Successfully.", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoadingUi(false);
                        Snackbar.make(myLayout,
                                "Upload Failed. Restaurant Not Saved.",
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private boolean verifyUserInput(){
        boolean isVerified = true;

        String name = txtRestaurantName.getText().toString();
        String desc = txtRestaurantDesc.getText().toString();
        String addr = txtRestaurantAddress.getText().toString();

        if(name.equals("")){
            txtRestaurantName.setHint("Give a name");
            txtRestaurantName.setHintTextColor(getResources().getColor(R.color.colorError));
            isVerified = false;
        }

        if(desc.equals("")){
            txtRestaurantDesc.setHint("Write a description");
            txtRestaurantDesc.setHintTextColor(getResources().getColor(R.color.colorError));
            isVerified = false;
        }

        if(addr.equals("")){
            txtRestaurantAddress.setHint("Provide an address");
            txtRestaurantAddress.setHintTextColor(getResources().getColor(R.color.colorError));
            isVerified = false;
        }

        if(restImgUri == null && currentRestItem.getImgRef() == null){
            Snackbar.make(myLayout.findViewById(R.id.layout_restEditor),
                    "Insert Restaurant Image.", Snackbar.LENGTH_LONG)
            .setAction("Choose", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickRestaurantImage();
                }
            })
                    .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark))
                    .show();
            isVerified = false;
        }

        return isVerified;
    }

    private void resetToDefault(){
        txtRestaurantName.setText("");
        txtRestaurantDesc.setText("");
        txtRestaurantAddress.setText("");

        txtRestaurantName.setHintTextColor(getResources().getColor(R.color.colorDark));
        txtRestaurantDesc.setHintTextColor(getResources().getColor(R.color.colorDark));
        txtRestaurantAddress.setHintTextColor(getResources().getColor(R.color.colorDark));

        txtRestaurantName.requestFocus();

        tvRestaurantName.setText(R.string.sample_restaurant_name);
        tvRestaurantDesc.setText(R.string.sample_description);
        tvRestaurantAddress.setText(R.string.sample_address);

        imgRestaurantLogo.setImageResource(R.drawable.restaurant);

        if (getArguments() != null) {
            getArguments().putSerializable(ARG_REST_ITEM, new RestaurantItem());
            getArguments().putSerializable(ARG_REST_IMG, null);

            currentRestItem = (RestaurantItem) getArguments().getSerializable(ARG_REST_ITEM);
            restImgUri = getArguments().getParcelable(ARG_REST_IMG);
        }

        if(currentRestItem.getLocation() == null){
            btnGeoTagRestaurant.setImageDrawable(getResources().getDrawable(R.drawable.ic_pin));
        }
        else{
            btnGeoTagRestaurant.setImageDrawable(getResources().getDrawable(R.drawable.ic_minus));
        }
    }

    private void updateEditorUi(){
        boolean _check_1 = txtRestaurantName.getText().toString().equals(currentRestItem.getName());
        boolean _check_2 = txtRestaurantDesc.getText().toString().equals(currentRestItem.getDescription());
        boolean _check_3 = txtRestaurantAddress.getText().toString().equals(currentRestItem.getAddress());
        boolean _check_4 = restImgUri == null;
        boolean _check_5 = currentRestItem.getId() != null;

        if(!(_check_1 && _check_2 && _check_3 && _check_4)){
            btnSaveRestaurant.setVisibility(View.VISIBLE);
            btnManageMenu.setVisibility(View.INVISIBLE);
        }
        else if(_check_5){
            btnSaveRestaurant.setVisibility(View.INVISIBLE);
            btnManageMenu.setVisibility(View.VISIBLE);
        }

        if(currentRestItem.getLocation() == null){
            btnGeoTagRestaurant.setImageDrawable(getResources().getDrawable(R.drawable.ic_pin));
        }
        else{
            btnGeoTagRestaurant.setImageDrawable(getResources().getDrawable(R.drawable.ic_minus));
        }
    }

    private void updateEditorUi(boolean canSave){
        if(canSave){
            btnSaveRestaurant.setVisibility(View.VISIBLE);
            btnManageMenu.setVisibility(View.INVISIBLE);
        }
        else{
            btnSaveRestaurant.setVisibility(View.INVISIBLE);
            btnManageMenu.setVisibility(View.VISIBLE);
        }

        if(currentRestItem.getLocation() == null){
            btnGeoTagRestaurant.setImageDrawable(getResources().getDrawable(R.drawable.ic_pin));
        }
        else{
            btnGeoTagRestaurant.setImageDrawable(getResources().getDrawable(R.drawable.ic_minus));
        }
    }

    private void refreshThumbnail() {
        if(currentRestItem.getImgRef() != null){
            try {
                StorageReference imageRef = UserAuth.firebaseStorage
                        .getReferenceFromUrl(currentRestItem.getImgRef());

                GlideApp.with(imgRestaurantLogo.getContext())
                        .load(imageRef)
                        .into(imgRestaurantLogo);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        else{
            imgRestaurantLogo.setImageURI(restImgUri);
        }
    }

    private void setLoadingUi(boolean loading){
        txtRestaurantName.setEnabled(!loading);
        txtRestaurantDesc.setEnabled(!loading);
        txtRestaurantAddress.setEnabled(!loading);
        btnUpImage.setEnabled(!loading);
        btnSaveRestaurant.setEnabled(!loading);
        btnGeoTagRestaurant.setEnabled(!loading);

        pbUploading.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
    }
}
