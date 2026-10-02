package com.trousseau.model;

import lombok.Getter;

import java.io.Serializable;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * One bar of the wear-activity chart: a calendar month and how many outfit wears
 * were logged in it.
 */
@Getter
public class MonthlyWearStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final YearMonth month;
    private final long wearCount;

    public MonthlyWearStat(YearMonth month, long wearCount) {
        this.month = month;
        this.wearCount = wearCount;
    }

    /** e.g. "Aug" — the year is implied by the chart's range. */
    public String getLabel() {
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public String getFullLabel() {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }
}
