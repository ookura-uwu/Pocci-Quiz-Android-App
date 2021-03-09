package com.example.iqt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordFragment extends Fragment {

    View view;

    TextInputEditText email;
    TextInputLayout emailLayout;
    Button forgotPasswordButton, backToLogin;
    ProgressDialog dialog;

    FirebaseAuth firebaseAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        dialog = new ProgressDialog(getContext());

        email = view.findViewById(R.id.forgot_password_email_edit_text);
        emailLayout = view.findViewById(R.id.forgot_password_email_input_layout);
        forgotPasswordButton = view.findViewById(R.id.forgot_password_button);
        backToLogin = view.findViewById(R.id.back_to_login_button);

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getConnectivityStatus()) {
                    if (validateEmail(email.getText().toString())) {
                        dialog.setMessage("Sending Reset Password Email...");
                        dialog.show();
                        dialog.setCanceledOnTouchOutside(false);

                        firebaseAuth.sendPasswordResetEmail(email.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Reset Password Email has been sent successfully!", Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                    email.setText("");
                                }
                            }
                        });
                    }
                }
            }
        });

        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
                startActivity(new Intent(getActivity(), SignInActivity.class));
            }
        });

        return view;
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            emailLayout.setErrorEnabled(true);
            emailLayout.setError("Email Address is Required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setErrorEnabled(true);
            emailLayout.setError("Email Address is invalid");
            return false;
        } else {
            emailLayout.setErrorEnabled(false);
            return true;
        }
    }

    // Check for internet connection
    private boolean getConnectivityStatus() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}