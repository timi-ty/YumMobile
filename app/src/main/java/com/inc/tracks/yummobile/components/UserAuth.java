package com.inc.tracks.yummobile.components;

import android.app.Activity;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class UserAuth {
    private static final String TAG = "UserAuth";

    private static FirebaseAuth firebaseAuth;
    private static FirebaseAuth.AuthStateListener firebaseAuthListener;

    public static FirebaseUser currentUser;

    private static authChangeListener mListener;


    public static FirebaseStorage firebaseStorage;
    public static StorageReference firebaseRstStorageRef;
    public static StorageReference firebaseMenuStorageRef;

    public static UserPrefs userPrefs;

    private static Boolean isUser = null;

    public static Boolean isAdmin = null;

    public static Boolean isTransporter = null;


    public static Location mCurrentLocation;

    private UserAuth(){}

    public static FirebaseAuth init(final Activity initActivity){
        if (initActivity instanceof authChangeListener) {
            mListener = (authChangeListener) initActivity;
        } else {
            throw new RuntimeException(initActivity.toString()
                    + " must implement authChangeListener");
        }

        firebaseAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();

                if(currentUser == null){
                    mListener.onUserDisconnected();
                }
                else{
                    lookUpUser(currentUser);
                    lookUpAdmin(currentUser);
                    lookUpTransporter(currentUser);
                }
            }
        };

        connectStorage();

        return firebaseAuth;
    }

    private static void lookUpUser(final FirebaseUser user){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            DocumentSnapshot userDocument = task.getResult();
                            assert userDocument != null;
                            isUser = userDocument.exists();
                            userPrefs = userDocument.toObject(UserPrefs.class);
                            concludeLookUp();
                        } else {
                            mListener.onUserDisconnected();
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private static void lookUpAdmin(final FirebaseUser user){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("admins")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            DocumentSnapshot adminDocument = task.getResult();
                            assert adminDocument != null;
                            isAdmin = adminDocument.exists();
                            concludeLookUp();
                        } else {
                            mListener.onUserDisconnected();
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private static void lookUpTransporter(final FirebaseUser user){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("transporters")
                .document(user.getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            DocumentSnapshot transporterDocument = task.getResult();
                            assert transporterDocument != null;
                            isTransporter = transporterDocument.exists();
                            concludeLookUp();
                        } else {
                            mListener.onUserDisconnected();
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private static void concludeLookUp(){
        if(isUser != null && isAdmin != null && isTransporter != null){
            mListener.onUserConnected(isUser);
        }
    }

    public static void attachAuthListener(){
        isUser = isAdmin = isTransporter = null;
        firebaseAuth.addAuthStateListener(firebaseAuthListener);
    }

    public static void detachAuthListener(){
        firebaseAuth.removeAuthStateListener(firebaseAuthListener);
    }

    private static void connectStorage(){
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseRstStorageRef = firebaseStorage.getReference().child("restaurants_pictures");
        firebaseMenuStorageRef = firebaseStorage.getReference().child("food_pictures");
    }

    public interface authChangeListener {
        void onUserConnected(boolean isUser);
        void onUserDisconnected();
    }
}
