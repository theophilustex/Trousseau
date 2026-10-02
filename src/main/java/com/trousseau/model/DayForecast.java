package com.trousseau.model;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * One day of forecast, reduced to the three things that actually change what you
 * would wear: how warm it is, whether it will rain, and the resulting season band.
 */
@Getter
public class DayForecast implements Serializable {

    private static final long serialVersionUID = 1L;

    private final LocalDate date;
    private final double maxTempC;
    private final double minTempC;
    private final double precipitationMm;

    public DayForecast(LocalDate date, double maxTempC, double minTempC, double precipitationMm) {
        this.date = date;
        this.maxTempC = maxTempC;
        this.minTempC = minTempC;
        this.precipitationMm = precipitationMm;
    }

    /** More than a trace of rain expected. */
    public boolean isWet() {
        return precipitationMm >= 1.0;
    }

    public boolean isCold() {
        return maxTempC < 10;
    }

    public boolean isHot() {
        return maxTempC >= 24;
    }

    /** Rounded max temperature for display. */
    public long getDisplayTemp() {
        return Math.round(maxTempC);
    }

    /** Short human summary for the planner card. */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDisplayTemp()).append("°C");
        if (isWet()) {
            sb.append(" · rain");
        }
        return sb.toString();
    }
}
