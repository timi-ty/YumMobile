package com.inc.tracks.yummobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;


public class Launcher extends AppCompatActivity implements
        View.OnClickListener, UserAuth.authChangeListener{

    private final String TAG = "LauncherActivity";

    private final int RC_SIGN_IN = 9001;


    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    private SignInButton btn_signIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        btn_signIn = findViewById(R.id.sign_in_button);

        btn_signIn.setOnClickListener(this);

        mGoogleSignInClient = getGoogleSignInClient();

        mAuth = UserAuth.init(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser != null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        UserAuth.attachAuthListener();
    }

    @Override
    protected void onPause() {
        super.onPause();

        UserAuth.detachAuthListener();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.sign_in_button){
            signIn();
        }
    }

    @Override
    public void onUserConnected(boolean isUser, boolean isAdmin) {
        Log.d(TAG, "User Connected");
        finalizeLaunch(isUser);
    }

    @Override
    public void onUserDisconnected() {
        updateUI(false);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, authenticate with firebase.
            assert account != null;
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Snackbar.make(findViewById(R.id.layout_launcher), "Sign In Failed.", Snackbar.LENGTH_SHORT).show();
            updateUI(false);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user != null);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.layout_launcher), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(false);
                        }
                    }
                });
    }

    private void signIn() {
        mGoogleSignInClient.signOut();//to make auth ui prompt which account to use

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void updateUI(boolean foundUser){
        if(foundUser){
            btn_signIn.setVisibility(View.INVISIBLE);
        }
        else{
            btn_signIn.setVisibility(View.VISIBLE);
        }
    }

    private void finalizeLaunch(boolean foundUser){
        Intent nextActivityIntent;
        if(foundUser){
            nextActivityIntent = new Intent(Launcher.this,
                        MainActivity.class);
        }
        else {
            nextActivityIntent = new Intent(Launcher.this,
                    RegistrationActivity.class);
        }
        startActivity(nextActivityIntent);
        Launcher.this.finish();
    }

    private GoogleSignInClient getGoogleSignInClient(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        return GoogleSignIn.getClient(this, gso);
    }
}
