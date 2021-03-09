package com.example.iqt;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;
import android.service.autofill.Dataset;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.iqt.QuestionnaireContainer.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QuestionnaireDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "IQTest.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TAG = "QuestionDBHelperTAG";

    private SQLiteDatabase db;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference firebaseRootRef;
    private List<QuestionnaireModel> questionnaireModelList = new ArrayList<>();
    private Context contextActivity;

    private ProgressDialog dialog;

    private int i = 0;

    public QuestionnaireDBHelper(@Nullable Context context, boolean deleteTable) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        contextActivity = context;

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseRootRef = firebaseDatabase.getReference().child("Questionnaires");

        final String SQL_CREATE_QUESTIONS_TABLE = "CREATE TABLE " +
                QuestionsTable.TABLE_NAME + " ( " +
                QuestionsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                QuestionsTable.COLUMN_KEY + " TEXT, " +
                QuestionsTable.COLUMN_POSITION + " TEXT, " +
                QuestionsTable.COLUMN_QUESTION + " TEXT, " +
                QuestionsTable.COLUMN_OPTION1 + " TEXT, " +
                QuestionsTable.COLUMN_OPTION2 + " TEXT, " +
                QuestionsTable.COLUMN_OPTION3 + " TEXT, " +
                QuestionsTable.COLUMN_OPTION4 + " TEXT, " +
                QuestionsTable.COLUMN_ANSWER_NR + " INTEGER, " +
                QuestionsTable.COLUMN_EXPLANATION + " TEXT " +
                ")";

        String SQL_CREATE_USEREXAMSTATE_TABLE = "CREATE TABLE " +
                "UserExamState (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_key TEXT, " +
                "exam_state TEXT, " +
                "remaining_time TEXT)";

        String SQL_CREATE_USEREXAMANSWER_TABLE = "CREATE TABLE " +
                "UserExamAnswers (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_key TEXT, " +
                "question_key TEXT, " +
                "question_answer TEXT)";

        this.db = getReadableDatabase();

        if (deleteTable) {
            db.execSQL("DROP TABLE IF EXISTS " + QuestionsTable.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS UserExamState");
        }

        String query = "SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='" + QuestionsTable.TABLE_NAME + "'";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.getCount() < 1) {
            db.execSQL(SQL_CREATE_QUESTIONS_TABLE);
        }

        query = "SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='UserExamState'";
        cursor = db.rawQuery(query, null);

        if (cursor.getCount() < 1) {
            db.execSQL(SQL_CREATE_USEREXAMSTATE_TABLE);
        }

        query = "SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name='UserExamAnswers'";
        cursor = db.rawQuery(query, null);

        if (cursor.getCount() < 1) {
            db.execSQL(SQL_CREATE_USEREXAMANSWER_TABLE);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + QuestionsTable.TABLE_NAME);
        onCreate(db);
    }

    public void getQuestionsFromFirebase() {
        this.db = getReadableDatabase();

        dialog = new ProgressDialog(contextActivity);

        dialog.setMessage("Updating...");
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);

        getResult(new getDataResultCallBack() {
            @Override
            public void onCallBack(boolean done) {
                if (done)
                    dialog.dismiss();
            }
        });
    }

    private interface getDataResultCallBack {
        void onCallBack(boolean done);
    }

    private void getResult(final getDataResultCallBack getData) {
        readData(new FirebaseCallback() {
            @Override
            public void onCallback(List<QuestionnaireModel> list) {
                for (QuestionnaireModel model : list) {
                    Cursor cursor = db.rawQuery("SELECT * FROM " + QuestionsTable.TABLE_NAME + " WHERE " + QuestionsTable.COLUMN_KEY + " = '" + model.getKey() + "'", null);
                    if (cursor.getCount() < 1) {
                        addQuestion(model);
                    }

                    Log.d(TAG, "ENTRY " + i);
                    i++;
                }

                countChildren(new FirebaseCallbackCountChildren() {
                    @Override
                    public void onCallback(int count) {
                        if (i == count) {
                            getData.onCallBack(true);
                        }
                    }
                });

            }
        });
    }

    // Add new question data
    private void addQuestion(QuestionnaireModel questionnaireModel) {
        ContentValues cv = new ContentValues();
        cv.put(QuestionsTable.COLUMN_KEY, questionnaireModel.getKey());
        cv.put(QuestionsTable.COLUMN_POSITION, questionnaireModel.getPosition());
        cv.put(QuestionsTable.COLUMN_QUESTION, questionnaireModel.getQuestion());
        cv.put(QuestionsTable.COLUMN_OPTION1, questionnaireModel.getOption1());
        cv.put(QuestionsTable.COLUMN_OPTION2, questionnaireModel.getOption2());
        cv.put(QuestionsTable.COLUMN_OPTION3, questionnaireModel.getOption3());
        cv.put(QuestionsTable.COLUMN_OPTION4, questionnaireModel.getOption4());
        cv.put(QuestionsTable.COLUMN_ANSWER_NR, questionnaireModel.getAnswer());
        cv.put(QuestionsTable.COLUMN_EXPLANATION, questionnaireModel.getExplanation());

        db.insert(QuestionsTable.TABLE_NAME, null, cv);
    }

    // Get all questions from SQLite
    public ArrayList<QuestionnaireModel> getAllQuestions(String position) {
        ArrayList<QuestionnaireModel> questionnaireModelList = new ArrayList<>();
        db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + QuestionsTable.TABLE_NAME + " WHERE " + QuestionsTable.COLUMN_POSITION + " ='" + position + "' LIMIT 50", null);

        if (c.moveToFirst()) {
            do {
                QuestionnaireModel model = new QuestionnaireModel();
                model.setKey(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_KEY)));
                model.setPosition(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_POSITION)));
                model.setQuestion(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_QUESTION)));
                model.setOption1(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_OPTION1)));
                model.setOption2(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_OPTION2)));
                model.setOption3(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_OPTION3)));
                model.setOption4(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_OPTION4)));
                model.setExplanation(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_EXPLANATION)));
                model.setAnswer(c.getString(c.getColumnIndex(QuestionsTable.COLUMN_ANSWER_NR)));

                questionnaireModelList.add(model);
            } while (c.moveToNext());
        }

        c.close();
        return questionnaireModelList;
    }

    // Read data from Firebase and pass the data to callback function
    private void readData(final FirebaseCallback firebaseCallback) {
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseRootRef = firebaseDatabase.getReference().child("Questionnaires");

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    QuestionnaireModel model = new QuestionnaireModel(
                            ds.child("Question").getValue().toString(),
                            ds.getKey(),
                            ds.child("Explanation").getValue().toString(),
                            ds.child("Option1").getValue().toString(),
                            ds.child("Option2").getValue().toString(),
                            ds.child("Option3").getValue().toString(),
                            ds.child("Option4").getValue().toString(),
                            ds.child("CorrectAnswer").getValue().toString(),
                            ds.child("Position").getValue().toString()
                    );

                    questionnaireModelList.add(model);
                }
                firebaseCallback.onCallback(questionnaireModelList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        firebaseRootRef.addListenerForSingleValueEvent(valueEventListener);
    }

    public void addUserExamState(String uid, String state, Long remainingTime) {
        db.execSQL("DELETE FROM UserExamState WHERE user_key = '" + uid + "'");

        ContentValues cv = new ContentValues();
        cv.put("user_key", uid);
        cv.put("exam_state", state);
        cv.put("remaining_time", remainingTime);

        db.insert("UserExamState", null, cv);

        saveExamStateToFirebase(uid);
    }

    public void saveExamStateToFirebase(String id) {
        Cursor cursor = db.rawQuery("SELECT * FROM UserExamState WHERE user_key = '" + id + "'", null);

        Map<String, String> data = new ArrayMap<>();

        data.put("(UserKey)", id);

        if (cursor.moveToFirst()) {
            do {
                data.put("ExamState", cursor.getString(cursor.getColumnIndex("exam_state")));
                data.put("RemainingTime", cursor.getString(cursor.getColumnIndex("remaining_time")));
            } while (cursor.moveToNext());

            DatabaseReference ref;
            ref = firebaseDatabase.getReference();
            ref.child("UserExamState").child(id).setValue(data);
        }
    }

    public void saveDataToFirebase(String id) {
        Cursor cursor = db.rawQuery("SELECT * FROM UserExamAnswers WHERE user_key = '" + id + "' ORDER BY _id DESC", null);

        Map<String, UserExamAnswersList> examAnswersList = new ArrayMap<>();
        Map<String, Object> data = new HashMap<>();

        data.put("(UserKey)", id);

        if (cursor.moveToFirst()) {
            do {
                UserExamAnswersList model = new UserExamAnswersList();
                String qKey = cursor.getString(cursor.getColumnIndex("question_key"));
                String qAnswer = cursor.getString(cursor.getColumnIndex("question_answer"));

                model.setQuestionKey(qKey);
                model.setQuestionAnswer(qAnswer);

                examAnswersList.put(qKey, model);
            } while (cursor.moveToNext());

            DatabaseReference ref;
            ref = firebaseDatabase.getReference();

            data.put("CurrentQuestionsAnswered", examAnswersList);

            ref.child("UserExamAnswers").child(id).setValue(data);
        }
    }

    public void addUserExamAnswer(String uid, String question_key, String answer) {
        Cursor c = db.rawQuery("SELECT * FROM UserExamAnswers WHERE question_key = '" + question_key + "'", null);

        if (c.getCount() < 1) {
            ContentValues cv = new ContentValues();
            cv.put("user_key", uid);
            cv.put("question_key", question_key);
            cv.put("question_answer", answer);

            db.insert("UserExamAnswers", null, cv);
        }
    }

    public void getUserAnswersFromFirebase(final String id) {
        DatabaseReference ref;
        ref = firebaseDatabase.getReference();
        ref.child("UserExamAnswers").child(id).child("CurrentQuestionsAnswered").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    addUserExamAnswer(id, ds.getKey(), ds.child("questionAnswer").getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public Map<String, String> getAllUserAnsweredQuestions(String id) {
        getUserAnswersFromFirebase(id);

        HashMap<String, String> data = new HashMap<>();

        Cursor c = db.rawQuery("SELECT * FROM UserExamAnswers WHERE user_key = '" + id + "' ORDER BY _id ASC", null);
        if (c.getCount() > 0) {
            if (c.moveToFirst()) {
                do {
                    data.put(c.getString(c.getColumnIndex("question_key")), c.getString(c.getColumnIndex("question_answer")));
                } while (c.moveToNext());
            }
        }

        return data;
    }

    private void countChildren(final FirebaseCallbackCountChildren firebaseCallbackCountChildren) {
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseRootRef = firebaseDatabase.getReference().child("Questionnaires");

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    count++;
                }

                firebaseCallbackCountChildren.onCallback(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        firebaseRootRef.addListenerForSingleValueEvent(valueEventListener);
    }

    private interface FirebaseCallback {
        void onCallback(List<QuestionnaireModel> list);
    }

    private interface FirebaseCallbackCountChildren {
        void onCallback(int count);
    }

    static class UserExamAnswersList {
        String QuestionKey;
        String QuestionAnswer;

        public UserExamAnswersList() {}

        public UserExamAnswersList(String questionKey, String questionAnswer) {
            QuestionKey = questionKey;
            QuestionAnswer = questionAnswer;
        }

        public String getQuestionKey() {
            return QuestionKey;
        }

        public void setQuestionKey(String questionKey) {
            QuestionKey = questionKey;
        }

        public String getQuestionAnswer() {
            return QuestionAnswer;
        }

        public void setQuestionAnswer(String questionAnswer) {
            QuestionAnswer = questionAnswer;
        }
    }
}