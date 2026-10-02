package com.trousseau.model;

public enum RatingType {
    PERSONAL("Personal"),
    SPOUSE_PARTNER("Spouse/Partner"),
    FRIENDS_OTHER("Friends/Other");

    private final String displayName;

    RatingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
