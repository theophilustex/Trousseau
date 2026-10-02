package com.trousseau.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * Per-category rollup for the insights page: how many items are in the category and
 * how many wears they account for between them.
 */
@Getter
public class CategoryWearStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String category;
    private final long itemCount;
    private final long totalWears;

    public CategoryWearStat(String category, Long itemCount, Long totalWears) {
        this.category = category;
        this.itemCount = itemCount == null ? 0L : itemCount;
        this.totalWears = totalWears == null ? 0L : totalWears;
    }

    public String getCategoryLabel() {
        return (category == null || category.isEmpty()) ? "Uncategorised" : category;
    }

    /** Mean wears per item in this category, to one decimal. */
    public double getWearsPerItem() {
        return itemCount == 0 ? 0d : Math.round((double) totalWears / itemCount * 10d) / 10d;
    }
}
