package com.trousseau.model;

/**
 * Where a garment is in its life. Retiring an item keeps its wear history and
 * cost-per-wear intact, which deleting would throw away — the retired items are
 * often the most instructive ones.
 */
public enum ItemStatus {

    ACTIVE("Active", "In the wardrobe and available to wear"),
    ARCHIVED("Archived", "Kept, but out of rotation — stored or off-season"),
    DONATED("Donated", "Given away"),
    SOLD("Sold", "Sold on");

    private final String displayName;
    private final String description;

    ItemStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** True for everything except {@link #ACTIVE} — i.e. no longer in rotation. */
    public boolean isRetired() {
        return this != ACTIVE;
    }
}
