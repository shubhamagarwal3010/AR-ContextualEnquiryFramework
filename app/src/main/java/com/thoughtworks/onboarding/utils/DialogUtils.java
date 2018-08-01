package com.thoughtworks.onboarding.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public abstract class DialogUtils {

    public void showAlert(final Activity mactivity, String msg) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mactivity);

        alertDialog.setMessage(msg);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                onPositiveButtonClick();
            }
        });


        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                onNegativeButtonClick();
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    public abstract void onPositiveButtonClick();

    public abstract void onNegativeButtonClick();

}
