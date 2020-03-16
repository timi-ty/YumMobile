package com.inc.tracks.yummobile.user_activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.inc.tracks.yummobile.R;
import com.inc.tracks.yummobile.components.UserAuth;
import com.inc.tracks.yummobile.components.UserPrefs;


public class RegistrationActivity extends AppCompatActivity implements View.OnClickListener{
    UserPrefs theUser;
    EditText etvName;
    EditText etvPhone;
    Button btnDone;
    TextView tvSwitchAccount;
    TextView txtUserGreeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        theUser = new UserPrefs();
        etvName = findViewById(R.id.txt_nameField);
        etvPhone = findViewById(R.id.txt_phoneField);
        btnDone = findViewById(R.id.btn_Done);
        tvSwitchAccount = findViewById(R.id.tv_switchAccount);
        txtUserGreeting = findViewById(R.id.txt_userGreeting);

        btnDone.setOnClickListener(this);
        tvSwitchAccount.setOnClickListener(this);

        greetUser();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_Done:
                if (verifyUserInput()) {
                    saveUserRegistration();
                }
                break;
            case R.id.tv_switchAccount:
                FirebaseAuth.getInstance().signOut();

                Intent mainActivityIntent = new Intent(RegistrationActivity.this,
                        Launcher.class);
                startActivity(mainActivityIntent);
                RegistrationActivity.this.finish();
        }
    }

    private boolean verifyUserInput(){
        boolean isVerified = true;

        String name = etvName.getText().toString();
        String id = etvPhone.getText().toString();

        if(name.equals("") || name.contains(" ")){
            etvName.setHint("Invalid username");
            etvName.setHintTextColor(getResources().getColor(R.color.colorError));
            isVerified = false;
        }

        if(id.equals("") || id.contains(" ")){
            etvPhone.setHint("Invalid phone number");
            etvPhone.setHintTextColor(getResources().getColor(R.color.colorError));
            isVerified = false;
        }

        return isVerified;
    }

    private void saveUserRegistration(){
        updateUI(true);

        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setUserName(etvName.getText().toString());
        userPrefs.setUserPhone(etvPhone.getText().toString());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference userDoc = db.collection("users")
                .document(UserAuth.currentUser.getUid());
        userDoc.set(userPrefs, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                finalizeRegistration();
            }
        })
        .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                updateUI(false);
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(findViewById(R.id.layout_registration), "Sorry, Couldn't Register.", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void finalizeRegistration(){
        Intent mainActivityIntent = new Intent(RegistrationActivity.this,
                MainActivity.class);
        startActivity(mainActivityIntent);
        RegistrationActivity.this.finish();
    }

    private void updateUI(boolean loading){
        btnDone.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }

    private void greetUser(){
        String userGreeting = "Hello, " + UserAuth.currentUser.getDisplayName();
        if(UserAuth.isAdmin){
            userGreeting += "\n Administrator Account";
        }
        txtUserGreeting.setText(userGreeting);
    }
}
