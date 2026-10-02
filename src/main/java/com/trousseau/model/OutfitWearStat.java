package com.trousseau.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * Projection counting how many times an outfit has been logged as worn.
 * Avoids loading the outfit's items (and therefore their BLOBs) for a leaderboard.
 */
@Getter
public class OutfitWearStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String name;
    private final String occasion;
    private final long wearCount;

    public OutfitWearStat(Long id, String name, String occasion, Long wearCount) {
        this.id = id;
        this.name = name;
        this.occasion = occasion;
        this.wearCount = wearCount == null ? 0L : wearCount;
    }
}
