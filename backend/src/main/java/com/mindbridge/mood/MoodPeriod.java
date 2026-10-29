package com.mindbridge.mood;

public enum MoodPeriod {
    WEEK(7),
    MONTH(30);

    private final int days;

    MoodPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
