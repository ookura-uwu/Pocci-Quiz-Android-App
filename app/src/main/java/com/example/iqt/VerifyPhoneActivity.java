package com.example.iqt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.VerifiedInputEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VerifyPhoneActivity extends AppCompatActivity {
    TextInputEditText verificationCode;
    TextView timer;
    Button verifyPhoneButton, resendOTPButton;
    Boolean otpValid = true;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference ref;
    String verificationCodeBySystem;
    String phoneNo, activityReference, userId;
    ProgressBar progressBar;
    ProgressDialog dialog;
    PhoneAuthProvider.ForceResendingToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_phone);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        ref = firebaseDatabase.getReference();

        dialog = new ProgressDialog(this);

        verifyPhoneButton = findViewById(R.id.verify_phone_button);
        verificationCode = findViewById(R.id.verification_edit_text);
        resendOTPButton = findViewById(R.id.resendOTP_button);
        resendOTPButton.setVisibility(View.INVISIBLE);
        timer = findViewById(R.id.timer_text_view);
        timer.setVisibility(View.INVISIBLE);
        progressBar = findViewById(R.id.progress_bar_verify);
        progressBar.setVisibility(View.INVISIBLE);

        Bundle extras = getIntent().getExtras();

        phoneNo = extras.getString("PhoneNo");
        activityReference = extras.getString("Reference");
        if (activityReference.equals("Settings")) {
            userId = extras.getString("UidAddPhone");
        }

        if (activityReference.equals("Login")) {
            resendOTP(phoneNo);
        } else {
            sendVerificationCodeToUser(phoneNo);
        }

        verifyPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String code = verificationCode.getText().toString().trim();

                if (code.isEmpty() || code.length() < 6) {
                    Toast.makeText(VerifyPhoneActivity.this, "Invalid Verification Code", Toast.LENGTH_LONG).show();
                } else {
                    dialog.setMessage("Verifying Phone Number...");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    verifyCode(code);
                }
            }
        });

        resendOTPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!phoneNo.isEmpty()) {
                    resendOTP(phoneNo);
                }
            }
        });
    }

    private void sendVerificationCodeToUser(String phoneNo) {

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber("+63" + phoneNo)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendOTP(String phoneNo) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber("+63" + phoneNo)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .setForceResendingToken(token)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            verificationCodeBySystem = s;
            token = forceResendingToken;
            resendOTPButton.setVisibility(View.INVISIBLE);

            timer.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            new CountDownTimer(60000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timer.setText("00:" + millisUntilFinished / 1000);
                }

                @Override
                public void onFinish() {
                    resendOTPButton.setVisibility(View.VISIBLE);
                    timer.setVisibility(View.INVISIBLE);
                }
            }.start();
        }

        @Override
        public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
            super.onCodeAutoRetrievalTimeOut(s);
            resendOTPButton.setVisibility(View.VISIBLE);
        }

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

            String code = phoneAuthCredential.getSmsCode();
            if (code != null) {
                verifyCode(code);
            }
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            Toast.makeText(VerifyPhoneActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    private void verifyCode(String verificationCodeByUser) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCodeBySystem, verificationCodeByUser);
        FirebaseAuth.getInstance().getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);

        if (activityReference.equals("Register") || activityReference.equals("Settings")) {
            linkPhoneToUserCredentials(credential);
        } else if (activityReference.equals("Login")) {
            signInTheUserByCredentials(credential);
        }
    }

    private void linkPhoneToUserCredentials(PhoneAuthCredential credential) {
        firebaseAuth.getCurrentUser().linkWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                if (activityReference.equals("Settings")) {
                    ref.child("Users").child(userId).child("PhoneNumber").setValue(phoneNo);

                    String userPhone = String.format("PhoneNumber&Uid=%s", userId);
                    SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(userPhone, phoneNo);
                    editor.apply();

                    Toast.makeText(VerifyPhoneActivity.this, "Phone Number has been linked to your account", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(VerifyPhoneActivity.this, "Account created and linked", Toast.LENGTH_SHORT).show();
                }

                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                finish();
                startActivity(intent);
            }
        });
    }

    private void signInTheUserByCredentials(final PhoneAuthCredential credential) {

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {
                    if (dialog.isShowing())
                        dialog.dismiss();

                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                    finish();
                    startActivity(intent);
                } else {
                    Toast.makeText(VerifyPhoneActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}