package com.examprep.model;

public class QuestionOption {

    private final String letter;
    private final String text;

    public QuestionOption(String letter, String text) {
        this.letter = letter;
        this.text = text;
    }

    public String getLetter() {
        return letter;
    }

    public String getText() {
        return text;
    }
}
