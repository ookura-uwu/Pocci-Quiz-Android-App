package com.example.iqt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class QuestionnairesActivity extends AppCompatActivity {

    QuestionnaireDBHelper dbHelper;

    FirebaseAuth firebaseAuth;

    String userId;

    CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;
    private long mEndTime;
    long defaultExamTime = 60 * 60000;
    boolean timeOut = false;

    Button next;
    RadioButton option1;
    RadioButton option2;
    RadioButton option3;
    RadioButton option4;
    RadioGroup optionGroup;
    TextView textQuestionnaire;
    TextView textQuestionnaireCount;
    TextView textScore;
    TextView textTimer;
    LinearLayout answersLinearLayout;

    List<QuestionnaireModel> questionnaireModelList;
    IQTestResult iqTestResult;
    int count;
    int countTotal;
    QuestionnaireModel currentQuestion;

    int score = 0;

    FirebaseDatabase database;
    DatabaseReference reference;

    Map<String, String> questionKey = new HashMap<>();
    Map<String, String> questionAnswer = new HashMap<>();
    Map<String, QuestionsList> questionsAnswer = new HashMap<>();
    String currentQuestionKey;

    ProgressDialog dialog;

    String name;
    String position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaires);

        firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getUid();
        
        dbHelper = new QuestionnaireDBHelper(this, false);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference();


        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        Bundle extras = getIntent().getExtras();
        name = extras.getString("Name");
        position = extras.getString("Position");

        init();

        // Load question
        showNextQuestion();

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (next.getText().toString().equals("Next")) {
                    if (optionGroup.getCheckedRadioButtonId() == -1) {
                        Toast.makeText(QuestionnairesActivity.this, "Please select an answer", Toast.LENGTH_LONG).show();
                    } else {
                        RadioButton optionSelected = findViewById(optionGroup.getCheckedRadioButtonId());
                        int answerNr = optionGroup.indexOfChild(optionSelected) + 1;

                        questionAnswer.put(currentQuestionKey, String.valueOf(answerNr));

                        String answer = currentQuestion.getAnswer();
                        int intAnswer;

                        switch (answer) {
                            case "A":
                                intAnswer = 1;
                                break;
                            case "B":
                                intAnswer = 2;
                                break;
                            case "C":
                                intAnswer = 3;
                                break;
                            case "D":
                                intAnswer = 4;
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + answer);
                        }

                        if (answerNr == intAnswer) {
                            score++;
                        }

                        // Save current question to database
                        dbHelper.addUserExamAnswer(userId, currentQuestionKey, String.valueOf(answerNr));

                        showNextQuestion();
                    }
                } else if (next.getText().toString().equals("Finish")) {

                    dialog.setMessage("Saving result to database...");
                    dialog.show();
                    dialog.setCanceledOnTouchOutside(false);

                    saveExam();

                    Toast.makeText(QuestionnairesActivity.this, "Your Test Result has been saved to the database successfully!", Toast.LENGTH_LONG).show();

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(QuestionnairesActivity.this);
                    builder.setTitle("Exam Complete");

                    if (score >= 30) {
                        builder.setMessage("You have Passed the Exam!\nYour Exam Score: " + score + "\nPlease wait for the company to contact you.");
                    } else {
                        builder.setMessage("You have Failed the Exam!\nYour Exam Score: " + score + "\nPlease wait for another month to retake the exam");
                    }

                    builder.setCancelable(false);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(QuestionnairesActivity.this, HomeActivity.class));
                            finish();
                        }
                    });

                    builder.show();
                }
            }
        });

    }

    private void saveExam() {
        Map<String, Object> data = new HashMap<>();
        Map<String, String> examDates = new HashMap<>();

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("MM_dd_yyyy", Locale.getDefault());
        String currentDate = df.format(c);

        data.put("(Name)", name);
        data.put("(Userkey)", userId);
        examDates.put("ExamDate", currentDate.replace("_", "-"));
        data.put(currentDate, examDates);
        reference.child("TestResults").child(userId).setValue(data);

        data.clear();

        data.put("Position", position);
        data.put("Evaluated", "false");
        data.put("Score", String.valueOf(score));

        for (String key : questionKey.keySet()) {
            questionsAnswer.put(key, new QuestionsList(key, questionAnswer.get(key)));
        }

        data.put("Results", questionsAnswer);

        reference.child("TestResults").child(userId).child(currentDate).updateChildren(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                }
            }
        });
    }

    // Initialize all components
    private void init() {
        userId = firebaseAuth.getUid();

        dialog = new ProgressDialog(this);

        textTimer = findViewById(R.id.exam_timer_text_view);
        textQuestionnaire = findViewById(R.id.questionnaire_text_view);
        textQuestionnaireCount = findViewById(R.id.total_question_counter_text_view);
        optionGroup = findViewById(R.id.rdOptionGroup);
        option1 = findViewById(R.id.rdAnswer1);
        option2 = findViewById(R.id.rdAnswer2);
        option3 = findViewById(R.id.rdAnswer3);
        option4 = findViewById(R.id.rdAnswer4);
        next = findViewById(R.id.next_finish_button);
        answersLinearLayout = findViewById(R.id.answers_linear_layout);

        Bundle extras = getIntent().getExtras();
        String position = extras.getString("Position");

        questionnaireModelList = dbHelper.getAllQuestions(position);
        countTotal = questionnaireModelList.size();

        reference.child("UserExamState").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(userId)) {
                    setTime(mTimeLeftInMillis);
                } else {
                    setTime(defaultExamTime);
                }
                startTimer();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Just like the method's name says
    private void showNextQuestion() {
        optionGroup.clearCheck();

        if (count < countTotal) {
            currentQuestion = questionnaireModelList.get(count);

            currentQuestionKey = currentQuestion.getKey();
            questionKey.put(currentQuestionKey, null);

            textQuestionnaire.setText(currentQuestion.getQuestion().replace("__b", "\n"));
            option1.setText("A - " + currentQuestion.getOption1());
            option2.setText("B - " + currentQuestion.getOption2());
            option3.setText("C - " + currentQuestion.getOption3());

            option4.setEnabled(true);

            if (currentQuestion.getOption4().equals("None")) {
                option4.setText("");
                option4.setEnabled(false);
                option4.setVisibility(View.INVISIBLE);
            } else {
                option4.setText("D - " + currentQuestion.getOption4());
                option4.setVisibility(View.VISIBLE);
            }

            count++;
            textQuestionnaireCount.setText(getString(R.string.question_count, count, countTotal));
        } else {
            answersLinearLayout.setVisibility(View.INVISIBLE);
            textQuestionnaire.setText("");
            option1.setText("");
            option2.setText("");
            option3.setText("");
            option4.setText("");

            option1.setEnabled(false);
            option2.setEnabled(false);
            option3.setEnabled(false);
            option4.setEnabled(false);

            next.setText("Finish");
        }
    }

    static class QuestionsList {
        public String key;
        public String answer;

        public QuestionsList(String key, String answer) {
            this.key = key;
            this.answer = answer;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(QuestionnairesActivity.this);
        builder.setTitle("Cancel IQ Test?");
        builder.setMessage("Your progress will be saved.\nProceed?");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                pauseTimer();

                startActivity(new Intent(QuestionnairesActivity.this, HomeActivity.class));
                finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    // <!----- Timer ------------> //

    private void setTime(long milliseconds) {
        mStartTimeInMillis = milliseconds;
        resetTimer();
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateWatchInterface();
    }

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateWatchInterface();
    }

    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }
            @Override
            public void onFinish() {
                mTimerRunning = false;
                timeOut = true;
                updateWatchInterface();
            }
        }.start();
        mTimerRunning = true;
        updateWatchInterface();
    }

    private void updateCountDownText() {
        int hours = (int) (mTimeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        String timeLeftFormatted;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%02d:%02d", minutes, seconds);
        }
        textTimer.setText(timeLeftFormatted);
    }

    private void updateWatchInterface() {
        if (!mTimerRunning) {
            if (mTimeLeftInMillis < mStartTimeInMillis) {

                if (timeOut) {
                    dialog.setTitle("Time Out");
                    dialog.setMessage("Saving result to database...");
                    dialog.show();
                    dialog.setCanceledOnTouchOutside(false);

                    saveExam();

                    Toast.makeText(QuestionnairesActivity.this, "Your Test Result has been saved to the database successfully!", Toast.LENGTH_LONG).show();

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(QuestionnairesActivity.this);
                    builder.setTitle("Exam Complete");

                    if (score >= 30) {
                        builder.setMessage("You have Passed the Exam!\nYour Exam Score: " + score + "\nPlease wait for the company to contact you.");
                    } else {
                        builder.setMessage("You have Failed the Exam!\nYour Exam Score: " + score + "\nPlease wait for another month to retake the exam");
                    }

                    builder.setCancelable(false);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(QuestionnairesActivity.this, HomeActivity.class));
                            finish();
                        }
                    });

                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("startTimeInMillis", mStartTimeInMillis);
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);
        editor.apply();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        mStartTimeInMillis = prefs.getLong("startTimeInMillis", 600000);
        mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        mTimerRunning = prefs.getBoolean("timerRunning", false);
        updateCountDownText();
        updateWatchInterface();
        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();
            if (mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
                updateWatchInterface();
            } else {
                startTimer();
            }
        }
    }

}