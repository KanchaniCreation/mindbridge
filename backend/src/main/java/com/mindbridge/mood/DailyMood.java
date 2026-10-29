package com.mindbridge.mood;

import java.time.LocalDate;

public record DailyMood(LocalDate date, double averageMood, int entries) {
}
