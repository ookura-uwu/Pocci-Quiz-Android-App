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
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static androidx.core.content.ContextCompat.getSystemService;

public class SignUpFragment extends Fragment {

    AutoCompleteTextView positionDropdownText;
    TextInputEditText fullName, email, phone, password, passwordConfirm;
    TextInputLayout fullNameLayout, emailLayout, phoneLayout, passwordLayout, positionLayout;
    ScrollView signUpScrollView;
    Button signUpButton;

    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;

    ProgressDialog dialog;

    View view;

    String phoneNo;
    String position;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        init();

        loadPositions();

        // OnClickListener for SignUp Button
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // check for internet connection before validation
                if (getConnectivityStatus()) {
                    // show progress dialog
                    dialog.setMessage("Signing Up...");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();

                    // validate
                    if (validate() == 0) {
                        phoneNo = phone.getText().toString();

                        firebaseAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // send email verification to user
                                    sendEmailVerification();

                                    // proceed with user registration
                                    Toast.makeText(getActivity(), "User Account is created", Toast.LENGTH_LONG).show();

                                    if (phoneNo.isEmpty()) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Please verify your email before logging in", Toast.LENGTH_SHORT).show();
                                        signUpScrollView.fullScroll(ScrollView.FOCUS_UP);
                                        firebaseAuth.signOut();
                                    } else {
                                        dialog.dismiss();
                                        // send the user to verify phone
                                        String phoneNumber = phone.getText().toString().substring(1);
                                        Intent intent = new Intent(getActivity(), VerifyPhoneActivity.class);
                                        Bundle extras = new Bundle();
                                        extras.putString("PhoneNo", phoneNumber);
                                        extras.putString("Reference", "Register");
                                        intent.putExtras(extras);
                                        startActivity(intent);
                                    }

                                } else {
                                    dialog.dismiss();
                                    Toast.makeText(getActivity(), "Registration Error", Toast.LENGTH_LONG).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getActivity(), "Error! " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        dialog.dismiss();
                    }
                }
            }
        });

        return view;
    }

    // Initialize all components
    private void init() {
        signUpScrollView = view.findViewById(R.id.sign_up_scrollview);

        fullName = view.findViewById(R.id.name_edit_text);
        fullNameLayout = view.findViewById(R.id.name_input_layout);

        positionLayout = view.findViewById(R.id.position_input_layout);
        positionDropdownText = view.findViewById(R.id.position_dropdown_menu_text);

        email = view.findViewById(R.id.email_edit_text);
        emailLayout = view.findViewById(R.id.email_input_layout);

        phone = view.findViewById(R.id.phone_edit_text);
        phoneLayout = view.findViewById(R.id.phone_input_layout);

        password = view.findViewById(R.id.password_edit_text);
        passwordLayout = view.findViewById(R.id.password_input_layout);

        positionLayout = view.findViewById(R.id.position_input_layout);

        signUpButton = view.findViewById(R.id.sign_up_button);

        dialog = new ProgressDialog(getContext());
    }

    // Validate user input
    private int validate() {
        int countError = 0;

        String name = fullName.getText().toString();
        String user_email = email.getText().toString();
        String user_password = password.getText().toString();
        String user_phone = phone.getText().toString();
        String user_position = positionDropdownText.getText().toString();

        if (name.isEmpty()) {
            fullNameLayout.setErrorEnabled(true);
            fullNameLayout.setError("Full Name is required");
            countError += 1;
        } else {
            fullNameLayout.setErrorEnabled(false);
        }

        if (user_position.isEmpty()) {
            positionLayout.setErrorEnabled(true);
            positionLayout.setError("Position is required");
            countError += 1;
        } else {
            positionLayout.setErrorEnabled(false);
        }

        if (user_email.isEmpty()) {
            emailLayout.setErrorEnabled(true);
            emailLayout.setError("Email Address is required");
            countError += 1;
        } else {
            emailLayout.setErrorEnabled(false);
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(user_email).matches()) {
            emailLayout.setErrorEnabled(true);
            emailLayout.setError("Email Address is invalid");
            countError += 1;
        } else {
            emailLayout.setErrorEnabled(false);
        }

        if (!user_phone.isEmpty() && user_phone.length() < 11) {
            phoneLayout.setErrorEnabled(true);
            phoneLayout.setError("Phone Number is invalid");
            countError += 1;
        } else {
            phoneLayout.setErrorEnabled(false);
        }

        if (user_password.isEmpty()) {
            passwordLayout.setErrorEnabled(true);
            passwordLayout.setError("Password is required");
            countError += 1;
        } else {
            passwordLayout.setErrorEnabled(false);
        }

        return countError;
    }

    private void clearFields() {
        fullName.setText("");
        positionDropdownText.setText("");
        email.setText("");
        phone.setText("");
        password.setText("");

        fullName.clearFocus();
        positionDropdownText.clearFocus();
        email.clearFocus();
        phone.clearFocus();
        password.clearFocus();
    }

    // Send email verification to user's email address
    private void sendEmailVerification() {
        final FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        assert firebaseUser != null;
        firebaseUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    String id = firebaseUser.getUid();

                    position = positionDropdownText.getText().toString();

                    saveUser(id, fullName.getText().toString(), email.getText().toString(), phone.getText().toString(), position);

                    getActivity().finish();
                    startActivity(new Intent(getContext(), SignInActivity.class));

                    Toast.makeText(getContext(), "Successfully Registered, Verification Email has been sent to your email address", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "An error has occurred, Verification Email cannot be sent", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Save User information to Firebase
    private void saveUser(String id, String name, String email, String phone, String position) {
        reference = FirebaseDatabase.getInstance().getReference("Users");

        Map<String, String> data = new HashMap<>();
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
        String currentDate = df.format(c);

        data.put("ID", id);
        data.put("Name", name);
        data.put("PhoneNumber", phone);
        data.put("EmailAddress", email);
        data.put("Position", position);
        data.put("Joined", currentDate);

        reference.child(id).setValue(data);
    }

    // Check for internet connection
    private boolean getConnectivityStatus() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Load all positions
    private void loadPositions() {
        getPosition(new FirebaseCallback() {
            @Override
            public void onGetPositionsCallback(List<String> positions) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        getContext(),
                        R.layout.position_dropdown_menu_layout,
                        positions
                );

                positionDropdownText.setAdapter(adapter);
            }
        });
    }

    // Callback for loading positions
    private void getPosition(final FirebaseCallback firebaseCallback) {
        final List<String> positions = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference();
        reference.child("Positions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    positions.add(ds.child("Position").getValue().toString());
                }

                firebaseCallback.onGetPositionsCallback(positions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Callback interface for firebase OnDataChanged() method
    private interface FirebaseCallback {
        void onGetPositionsCallback(List<String> positions);
    }
}