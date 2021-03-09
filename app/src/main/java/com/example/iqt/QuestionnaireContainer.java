package com.example.iqt;

import android.provider.BaseColumns;

public final class QuestionnaireContainer {

    private QuestionnaireContainer() {}

    public static class QuestionsTable implements BaseColumns {
        public static final String TABLE_NAME = "Questionnaires";
        public static final String COLUMN_QUESTION = "question";
        public static final String COLUMN_KEY = "_key";
        public static final String COLUMN_POSITION = "position";
        public static final String COLUMN_OPTION1 = "option1";
        public static final String COLUMN_OPTION2 = "option2";
        public static final String COLUMN_OPTION3 = "option3";
        public static final String COLUMN_OPTION4 = "option4";
        public static final String COLUMN_ANSWER_NR = "answer_nr";
        public static final String COLUMN_EXPLANATION = "explanation";
    }
}
