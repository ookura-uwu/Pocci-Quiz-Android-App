package com.example.iqt;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.ref.Reference;

import static android.view.ViewGroup.*;

public class LoginFragment extends Fragment {

    TextInputEditText email, password, phone;
    TextInputLayout emailLayout, passwordLayout, phoneLayout;

    Button loginWithEmailButton, loginWithPhoneButton, loginUsingPhoneButton, forgotPasswordButton;
    LinearLayout loginWithEmailLayout;
    LinearLayout loginWithPhoneLayout;
    LayoutParams loginEmailParams;
    LayoutParams loginPhoneParams;
    ProgressBar progressBarEmailLogin;
    ProgressBar progressBarPhoneLogin;

    View view;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_login, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        init();

        if (user != null) {
            getActivity().finish();
            startActivity(new Intent(getActivity(), HomeActivity.class));
        }

        // Login using email and password
        loginWithEmailButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (email.getText().toString().isEmpty()) {
                    emailLayout.setErrorEnabled(true);
                    emailLayout.setError("Email Address is required!");
                } else {
                    if (getConnectivityStatus()) {
                        email.setEnabled(false);
                        password.setEnabled(false);

                        progressBarEmailLogin.setVisibility(View.VISIBLE);

                        firebaseAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    if (checkEmailVerification()) {
                                        getActivity().finish();
                                        startActivity(new Intent(getActivity(), HomeActivity.class));
                                    } else {
                                        Toast.makeText(getContext(), "Login Failed. Please verify your email address", Toast.LENGTH_LONG).show();
                                        firebaseAuth.signOut();

                                        email.setEnabled(true);
                                        password.setEnabled(true);
                                        progressBarEmailLogin.setVisibility(View.INVISIBLE);
                                    }
                                } else {
                                    Toast.makeText(getContext(), "Incorrect Email Address/Password", Toast.LENGTH_LONG).show();
                                    email.setEnabled(true);
                                    password.setEnabled(true);
                                    progressBarEmailLogin.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            }
        });

        // Login using phone number
        loginWithPhoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phone.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Please enter your phone number", Toast.LENGTH_SHORT).show();
                }

                if (phone.getText().toString().length() < 11) {
                    Toast.makeText(getContext(), "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                }

                if (phone.getText().toString().length() == 11) {
                    Intent intent = new Intent(getContext(), VerifyPhoneActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("PhoneNo", phone.getText().toString().substring(1));
                    extras.putString("Reference", "Login");
                    intent.putExtras(extras);
                    getActivity().finish();
                    startActivity(intent);
                }
            }
        });

        loginUsingPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginUsingPhoneButton.getText().toString().equals("Sign In Using Phone Number"))
                {
                    loginWithPhoneLayout.setVisibility(VISIBLE);
                    loginEmailParams.height = 1;
                    loginWithEmailLayout.setLayoutParams(loginEmailParams);

                    loginUsingPhoneButton.setText("Sign In Using Email");
                    loginUsingPhoneButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.important_mail_24px, 0);
                } else if (loginUsingPhoneButton.getText().toString().equals("Sign In Using Email")) {
                    loginWithPhoneLayout.setVisibility(INVISIBLE);
                    loginEmailParams.height = 350;
                    loginWithEmailLayout.setLayoutParams(loginEmailParams);

                    loginUsingPhoneButton.setText("Sign In Using Phone Number");
                    loginUsingPhoneButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.phone_24px, 0);
                }
            }
        });

        forgotPasswordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
                startActivity(new Intent(getActivity(), ForgotPasswordActivity.class));
            }
        });

        return view;
    }

    private void init() {
        email = view.findViewById(R.id.login_email_edit_text);
        emailLayout = view.findViewById(R.id.login_email_input_layout);
        password = view.findViewById(R.id.login_password_edit_text);
        passwordLayout = view.findViewById(R.id.login_password_input_layout);
        phone = view.findViewById(R.id.phone_edit_text);
        phoneLayout = view.findViewById(R.id.phone_input_layout);

        loginWithEmailLayout = view.findViewById(R.id.login_with_email_layout);
        loginWithPhoneLayout = view.findViewById(R.id.login_with_phone_layout);
        loginEmailParams = loginWithEmailLayout.getLayoutParams();
        loginPhoneParams = loginWithPhoneLayout.getLayoutParams();

        progressBarEmailLogin = view.findViewById(R.id.progress_bar_login_with_email);
        progressBarPhoneLogin = view.findViewById(R.id.progress_bar_login_with_phone);

        progressBarEmailLogin.setVisibility(View.INVISIBLE);
        progressBarPhoneLogin.setVisibility(View.INVISIBLE);

        loginWithPhoneLayout.setVisibility(INVISIBLE);

        loginWithEmailButton = view.findViewById(R.id.login_with_email_button);
        loginWithPhoneButton = view.findViewById(R.id.login_with_phone_button);
        loginUsingPhoneButton = view.findViewById(R.id.login_using_phone_button);
        forgotPasswordButton = view.findViewById(R.id.forgot_password_button);
    }

    private boolean checkEmailVerification() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        assert firebaseUser != null;

        return firebaseUser.isEmailVerified();
    }

    private boolean getConnectivityStatus() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

}