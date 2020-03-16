package com.inc.tracks.yummobile.components;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class MessageDialog extends DialogFragment {

    private AlertDialog.Builder builder;

    public MessageDialog(DialogInterface.OnClickListener positiveClick,
                         DialogInterface.OnClickListener negativeClick,
                         String message, Activity activity) {

        builder = new AlertDialog
                .Builder(Objects.requireNonNull(activity));

        builder.setMessage(message);

        builder.setPositiveButton("Ok", positiveClick);

        builder.setNegativeButton("Cancel", negativeClick);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return builder.create();
    }
}
