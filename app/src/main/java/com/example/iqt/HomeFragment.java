package com.example.iqt;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

    TextInputLayout nameLayout, positionLayout, emailLayout, phoneLayout;
    TextInputEditText name, position, email, phone;
    Button startButton;
    View view;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference ref;
    String userId;

    List<String> examDates = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_home, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        ref = firebaseDatabase.getReference();
        userId = firebaseAuth.getUid();

        init();

        getInfo();

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                final List<String> examDates = new ArrayList<>();

                getMonths(new FirebaseCallback() {
                    @Override
                    public void onCallback(List<String> examDates) {

                        SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

                        final String userPosition = String.format("Position&Uid=%s", userId);
                        final String position = prefs.getString(userPosition, "");
                        final String userName = String.format("FullName&Uid=%s", userId);
                        final String name = prefs.getString(userName, "");

                        Intent intent = new Intent(getActivity(), QuestionnairesActivity.class);
                        Bundle extras = new Bundle();
                        extras.putString("Name", name);
                        extras.putString("Position", position);
                        intent.putExtras(extras);

                        if (examDates.get(0).equals("N/A")) {

                            extras.putString("Name", name);
                            extras.putString("Position", position);
                            intent.putExtras(extras);

                            getActivity().finish();
                            startActivity(intent);
                        } else {
                            try {
                                Collections.sort(examDates, new Comparator<String>() {
                                    @Override
                                    public int compare(String object1, String object2) {
                                        return object2.compareTo(object1);
                                    }
                                });

                                SimpleDateFormat sdfn = new SimpleDateFormat("MM/dd/yyyy");
                                Date parse = sdfn.parse(examDates.get(0));
                                Calendar c = Calendar.getInstance();
                                c.setTime(parse);

                                LocalDate currentDate = LocalDate.now();
                                LocalDate examDate = LocalDate.of(c.get(Calendar.YEAR), getMonthInt(c.get(Calendar.MONTH)), c.get(Calendar.DAY_OF_MONTH));

                                Period period = Period.between(examDate, currentDate);
                                if (period.getMonths() <= -1) {

                                    extras.putString("Name", name);
                                    extras.putString("Position", position);
                                    intent.putExtras(extras);

                                    getActivity().finish();
                                    startActivity(intent);
                                } else {
                                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
                                    builder.setTitle("Exam Disabled");
                                    builder.setMessage("Your previous exam hasn't been more than 1 month, please come back later");
                                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Close dialog
                                        }
                                    });
                                    builder.show();
                                }
                            } catch (ParseException e) {
                                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
                                builder.setTitle("Error");
                                builder.setMessage("Parse Exception Error: " + e.getMessage() + "\nPlease contact developer");
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Close dialog
                                    }
                                });
                                builder.show();
                            }
                        }
                    }
                });
            }
        });

        return view;
    }

    private int getMonthInt(int month) {
        switch (month) {
            case 0: return 1;
            case 1: return 2;
            case 2: return 3; 
            case 3: return 4; 
            case 4: return 5; 
            case 5: return 6; 
            case 6: return 7; 
            case 7: return 8; 
            case 8: return 9; 
            case 9: return 10; 
            case 10: return 11; 
            case 11: return 12;
            default: return 0;
        }
    }

    private void getMonths(final FirebaseCallback firebaseCallback) {
        examDates.clear();
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot doc : snapshot.getChildren()) {
                    if (doc.getKey().contains("_")) {
                        String date = doc.getKey().replaceAll("_", "/");

                        examDates.add(date);
                    }
                }

                if (examDates.isEmpty())
                    examDates.add("N/A");

                firebaseCallback.onCallback(examDates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        ref.child("TestResults").child(userId).child("ExamsList").addListenerForSingleValueEvent(valueEventListener);
    }

    private interface FirebaseCallback {
        void onCallback(List<String> examDates);
    }

    private void init() {
        name = view.findViewById(R.id.home_full_name_edit_text);
        nameLayout = view.findViewById(R.id.home_full_name_input_layout);

        position = view.findViewById(R.id.home_position_edit_text);
        positionLayout = view.findViewById(R.id.home_position_input_layout);

        email = view.findViewById(R.id.home_email_edit_text);
        emailLayout = view.findViewById(R.id.home_email_input_layout);

        phone = view.findViewById(R.id.home_phone_edit_text);
        phoneLayout = view.findViewById(R.id.home_phone_input_layout);

        startButton = view.findViewById(R.id.start_test_button);
    }

    private void getInfo() {
        SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        String userName = String.format("FullName&Uid=%s", userId);
        String userPosition = String.format("Position&Uid=%s", userId);
        String userEmail = String.format("Email&Uid=%s", userId);
        String userPhone = String.format("PhoneNumber&Uid=%s", userId);

        String name = prefs.getString(userName, "");
        String position = prefs.getString(userPosition, "");
        String email = prefs.getString(userEmail, "");
        String phone = prefs.getString(userPhone, "");

        this.name.setText(name);
        this.position.setText(position);
        this.email.setText(email);
        this.phone.setText(phone);

        this.name.setEnabled(false);
        this.position.setEnabled(false);
        this.email.setEnabled(false);
        this.phone.setEnabled(false);
    }
}