package com.example.iqt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    TextView welcome;

    ViewPager viewPager;
    TabLayout tabLayout;

    HomeFragment homeFragment;
    SettingsFragment settingsFragment;

    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference;

    String userId;
    private static String TAG = "HomeActivityTAG";

    boolean doubleBackToExitPressedOnce = false;

    QuestionnaireDBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference();

        welcome = findViewById(R.id.username_text_view);

        checkAppStatus();

        // Initialize components
        init();
        // load user
        getCurrentUser();

        dbHelper = new QuestionnaireDBHelper(this, true);
        dbHelper.getQuestionsFromFirebase();
    }

    private void init() {
        viewPager = findViewById(R.id.home_view_pager);
        tabLayout = findViewById(R.id.home_tab_layout);

        homeFragment = new HomeFragment();
        settingsFragment = new SettingsFragment();

        tabLayout.setupWithViewPager(viewPager);

        HomeActivity.ViewPagerAdapter viewPagerAdapter = new HomeActivity.ViewPagerAdapter(getSupportFragmentManager(), 0);
        viewPagerAdapter.addFragment(homeFragment, "Home");
        viewPagerAdapter.addFragment(settingsFragment, "Settings");
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout.getTabAt(0).setIcon(R.drawable.home_48px);
        tabLayout.getTabAt(1).setIcon(R.drawable.settings_48px);
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> fragments = new ArrayList<>();
        private List<String> fragmentTitle = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        public void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            fragmentTitle.add(title);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitle.get(position);
        }
    }

    private void getCurrentUser() {
        user = firebaseAuth.getCurrentUser();
        userId = firebaseAuth.getUid();

        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();


        final String userName = String.format("FullName&Uid=%s", userId);
        final String userPosition = String.format("Position&Uid=%s", userId);
        final String userPhone = String.format("PhoneNumber&Uid=%s", userId);
        final String userEmail = String.format("Email&Uid=%s", userId);

        if (prefs.contains(userName)) {
            welcome.setText(getString(R.string.welcome_message, prefs.getString(userName, "")));
        } else {
            firebaseDatabase.getReference().child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    welcome.setText(getString(R.string.welcome_message, snapshot.child("Name").getValue()));
                    editor.putString(userName, snapshot.child("Name").getValue().toString());
                    editor.putString(userPosition, snapshot.child("Position").getValue().toString());
                    editor.putString(userPhone, snapshot.child("PhoneNumber").getValue().toString());
                    editor.putString(userEmail, snapshot.child("EmailAddress").getValue().toString());
                    editor.apply();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.d(TAG, error.getMessage());
                }
            });
        }
    }

    private void checkAppStatus() {
        reference.child("AppTest").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("ExpirationEnabled").getValue().toString().equals("true")) {
                    int month = Integer.parseInt(snapshot.child("Month").getValue().toString());
                    int day = Integer.parseInt(snapshot.child("Day").getValue().toString());
                    int year = Integer.parseInt(snapshot.child("Year").getValue().toString());

                    GregorianCalendar expDate = new GregorianCalendar(year, month, day);
                    GregorianCalendar now = new GregorianCalendar();

                    boolean isExpired = now.after(expDate);

                    if (isExpired) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(HomeActivity.this);
                        builder.setTitle("App Status");
                        builder.setMessage("App has expired");
                        builder.setCancelable(false);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                                System.exit(0);
                            }
                        });
                        builder.show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press the 'BACK' button again to exit", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}