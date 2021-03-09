package com.example.iqt;

import java.util.Dictionary;

public class IQTestResult {

    private String userId;
    private String score;
    private Dictionary<String, String> questionKeyAnswer;

    public IQTestResult() {}

    public IQTestResult(String userId, String score, Dictionary<String, String> questionKeyAnswer) {
        this.userId = userId;
        this.score = score;
        this.questionKeyAnswer = questionKeyAnswer;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public Dictionary<String, String> getQuestionKeyAnswer() {
        return questionKeyAnswer;
    }

    public void setQuestionKeyAnswer(Dictionary<String, String> questionKeyAnswer) {
        this.questionKeyAnswer = questionKeyAnswer;
    }
}
