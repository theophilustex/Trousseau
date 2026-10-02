package com.trousseau.bean;

import com.trousseau.model.CategoryWearStat;
import com.trousseau.model.ItemWearStat;
import com.trousseau.model.MonthlyWearStat;
import com.trousseau.model.OutfitWearStat;
import com.trousseau.model.User;
import com.trousseau.service.InsightsService;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Backs the insights page. Everything here is loaded once in {@link #init()}; the page
 * is read-only, so there is no reason to re-query per render.
 */
@Named
@ViewScoped
public class InsightsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private InsightsService insightsService;

    @Inject
    private SessionBean sessionBean;

    private long itemCount;
    private long outfitCount;
    private long totalItemWears;
    private long loggedOutfitWears;
    private BigDecimal wardrobeValue;
    private int pricedItemCount;

    private List<ItemWearStat> mostWornItems;
    private List<ItemWearStat> neverWornItems;
    private List<ItemWearStat> dormantItems;
    private List<ItemWearStat> bestValueItems;
    private List<ItemWearStat> worstValueItems;
    private List<CategoryWearStat> wearsByCategory;
    private List<OutfitWearStat> mostWornOutfits;
    private List<MonthlyWearStat> recentActivity;

    private long maxCategoryWears;
    private long maxMonthlyWears;

    @PostConstruct
    public void init() {
        User user = sessionBean.getCurrentUser();
        if (user == null) {
            return;
        }

        itemCount = insightsService.getItemCount(user);
        outfitCount = insightsService.getOutfitCount(user);
        totalItemWears = insightsService.getTotalItemWears(user);
        loggedOutfitWears = insightsService.getLoggedOutfitWears(user);
        wardrobeValue = insightsService.getWardrobeValue(user);
        pricedItemCount = insightsService.getPricedItemCount(user);

        mostWornItems = insightsService.getMostWornItems(user);
        neverWornItems = insightsService.getNeverWornItems(user);
        dormantItems = insightsService.getDormantItems(user);
        bestValueItems = insightsService.getBestValueItems(user);
        worstValueItems = insightsService.getWorstValueItems(user);
        wearsByCategory = insightsService.getWearsByCategory(user);
        mostWornOutfits = insightsService.getMostWornOutfits(user);
        recentActivity = insightsService.getRecentActivity(user);

        maxCategoryWears = maxOf(wearsByCategory);
        maxMonthlyWears = maxMonthly(recentActivity);
    }

    private long maxOf(List<CategoryWearStat> stats) {
        long max = 0;
        for (CategoryWearStat stat : stats) {
            max = Math.max(max, stat.getTotalWears());
        }
        return max;
    }

    private long maxMonthly(List<MonthlyWearStat> stats) {
        long max = 0;
        for (MonthlyWearStat stat : stats) {
            max = Math.max(max, stat.getWearCount());
        }
        return max;
    }

    /** Bar width as a percentage of the busiest category, for the category chart. */
    public int categoryBarWidth(CategoryWearStat stat) {
        if (maxCategoryWears <= 0) return 0;
        return (int) Math.round(stat.getTotalWears() * 100.0 / maxCategoryWears);
    }

    /** Bar height as a percentage of the busiest month, for the activity chart. */
    public int activityBarHeight(MonthlyWearStat stat) {
        if (maxMonthlyWears <= 0) return 0;
        // Floor at 2% so a month with some activity is still visible as a sliver.
        int pct = (int) Math.round(stat.getWearCount() * 100.0 / maxMonthlyWears);
        return stat.getWearCount() > 0 ? Math.max(pct, 2) : 0;
    }

    /** True when the wardrobe has no priced items, so the page can prompt instead of showing empty panels. */
    public boolean isCostTrackingUnused() {
        return pricedItemCount == 0;
    }

    public boolean isEmptyWardrobe() {
        return itemCount == 0;
    }

    public int getDormantAfterDays() {
        return InsightsService.DORMANT_AFTER_DAYS;
    }

    public long getItemCount() { return itemCount; }
    public long getOutfitCount() { return outfitCount; }
    public long getTotalItemWears() { return totalItemWears; }
    public long getLoggedOutfitWears() { return loggedOutfitWears; }
    public BigDecimal getWardrobeValue() { return wardrobeValue; }
    public int getPricedItemCount() { return pricedItemCount; }

    public List<ItemWearStat> getMostWornItems() { return mostWornItems; }
    public List<ItemWearStat> getNeverWornItems() { return neverWornItems; }
    public List<ItemWearStat> getDormantItems() { return dormantItems; }
    public List<ItemWearStat> getBestValueItems() { return bestValueItems; }
    public List<ItemWearStat> getWorstValueItems() { return worstValueItems; }
    public List<CategoryWearStat> getWearsByCategory() { return wearsByCategory; }
    public List<OutfitWearStat> getMostWornOutfits() { return mostWornOutfits; }
    public List<MonthlyWearStat> getRecentActivity() { return recentActivity; }
}
