package com.trousseau.model;

import lombok.Getter;

import java.io.Serializable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lightweight projection of a ClothingItem for the insights page — display fields and
 * wear statistics, no BLOBs. Loaded through JPQL constructor expressions so a report
 * over the whole wardrobe never pulls image or receipt data into memory.
 */
@Getter
public class ItemWearStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String name;
    private final String category;
    private final int wearCount;
    private final LocalDate lastWornDate;
    private final BigDecimal purchasePrice;

    /**
     * Constructor parameters use wrapper types because that is what JPQL hands a
     * constructor expression; the primitive counters are normalised here.
     */
    public ItemWearStat(Long id, String name, String category, Integer wearCount,
                        LocalDate lastWornDate, BigDecimal purchasePrice) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.wearCount = wearCount == null ? 0 : wearCount;
        this.lastWornDate = lastWornDate;
        this.purchasePrice = purchasePrice;
    }

    /** Same rule as {@link ClothingItem#getCostPerWear()}: unworn items cost their full price. */
    public BigDecimal getCostPerWear() {
        if (purchasePrice == null) {
            return null;
        }
        return purchasePrice.divide(BigDecimal.valueOf(Math.max(wearCount, 1)), 2, RoundingMode.HALF_UP);
    }

    /** Days since this item was last worn, or {@code null} if it never has been. */
    public Long getDaysSinceWorn() {
        if (lastWornDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(lastWornDate, LocalDate.now());
    }

    public String getCategoryLabel() {
        return (category == null || category.isEmpty()) ? "Uncategorised" : category;
    }
}
