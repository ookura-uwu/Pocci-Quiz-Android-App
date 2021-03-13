package com.example.iqt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class SettingsFragment extends Fragment {

    TextInputEditText phoneNumber, newPassword, confirmPassword;
    TextInputLayout phoneNumberLayout, newPasswordLayout, confirmPasswordLayout;
    Button addPhoneNumberButton, updateButton, logoutButton;
    int countError = 0;

    View view;

    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference ref;

    String userId;

    ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_settings, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        ref = firebaseDatabase.getReference();
        user = firebaseAuth.getCurrentUser();
        userId = user.getUid();

        progressDialog = new ProgressDialog(getContext());

        init();

        addPhoneNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!phoneNumber.getText().toString().isEmpty()) {
                    // send the user to verify phone
                    String phone = phoneNumber.getText().toString().substring(1);
                    Intent intent = new Intent(getActivity(), VerifyPhoneActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("PhoneNo", phone);
                    extras.putString("UidAddPhone", user.getUid());
                    extras.putString("Reference", "Settings");
                    intent.putExtras(extras);
                    getActivity().finish();
                    startActivity(intent);

                }
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (getConnectivityStatus()) {
                    if (validate() == 0) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                        builder.setTitle("Update Password");
                        builder.setMessage("Update Password?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.setMessage("Updating Password...");
                                progressDialog.setCanceledOnTouchOutside(false);
                                progressDialog.show();
                                updatePassword(newPassword.getText().toString(), confirmPassword.getText().toString());
                            }
                        });

                        builder.setNegativeButton(android.R.string.cancel, null);
                        builder.show();
                    }
                }
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                getActivity().finish();
                startActivity(new Intent(getActivity(), SignInActivity.class));
            }
        });

        return view;
    }

    private void init() {
        phoneNumber = view.findViewById(R.id.settings_phone_number_edit_text);
        phoneNumberLayout = view.findViewById(R.id.settings_phone_number_input_layout);

        newPassword = view.findViewById(R.id.settings_password_edit_text);
        newPasswordLayout = view.findViewById(R.id.settings_password_input_layout);

        confirmPassword = view.findViewById(R.id.settings_confirm_password_edit_text);
        confirmPasswordLayout = view.findViewById(R.id.settings_confirm_password_input_layout);

        addPhoneNumberButton = view.findViewById(R.id.settings_add_phone_number_button);
        updateButton = view.findViewById(R.id.settings_save_button);
        logoutButton = view.findViewById(R.id.home_logout_button);

        SharedPreferences prefs = this.getActivity().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String userPhone = String.format("PhoneNumber&Uid=%s", userId);

        if (!prefs.getString(userPhone, "").isEmpty()) {
            phoneNumber.setText(prefs.getString(userPhone, ""));
            phoneNumberLayout.setEnabled(false);
            addPhoneNumberButton.setVisibility(View.INVISIBLE);
        }
    }

    private int validate() {
        if (newPassword.getText().toString().isEmpty()) {
            newPasswordLayout.setErrorEnabled(true);
            newPasswordLayout.setError("Required Field");
            countError += 1;
        }

        if (confirmPassword.getText().toString().isEmpty()) {
            confirmPasswordLayout.setErrorEnabled(true);
            confirmPasswordLayout.setError("Required Field");
            countError += 1;
        }

        return countError;
    }

    private void updatePassword(String password, String confirmPassword) {
        if (password.equals(confirmPassword)) {
            user.updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password has been updated", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "Password does not match", Toast.LENGTH_LONG).show();
        }
    }

    private boolean getConnectivityStatus() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}