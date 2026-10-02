package com.trousseau.service;

import com.trousseau.dao.ClothingItemDao;
import com.trousseau.dao.OutfitDao;
import com.trousseau.dao.OutfitWearLogDao;
import com.trousseau.model.CategoryWearStat;
import com.trousseau.model.ItemWearStat;
import com.trousseau.model.MonthlyWearStat;
import com.trousseau.model.OutfitWearStat;
import com.trousseau.model.User;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only rollups over a user's wardrobe and wear history for the insights page.
 *
 * <p>Nothing here writes; everything is derived from data the app already records.
 * Queries project into the lightweight stat classes so a report never loads image
 * or receipt BLOBs.</p>
 */
@Stateless
public class InsightsService {

    /** An item unworn for this many days is reported as dormant. */
    public static final int DORMANT_AFTER_DAYS = 90;

    private static final int LIST_LIMIT = 10;
    private static final int ACTIVITY_MONTHS = 6;

    @Inject
    private ClothingItemDao clothingItemDao;

    @Inject
    private OutfitDao outfitDao;

    @Inject
    private OutfitWearLogDao outfitWearLogDao;

    // --- headline numbers ----------------------------------------------------

    public long getItemCount(User user) {
        return clothingItemDao.countByOwner(user);
    }

    public long getOutfitCount(User user) {
        return outfitDao.countByCreator(user);
    }

    /** Lifetime wears summed across every item. */
    public long getTotalItemWears(User user) {
        return clothingItemDao.sumWearCount(user);
    }

    /** Number of dated outfit-wear entries, which is not the same as item wears. */
    public long getLoggedOutfitWears(User user) {
        return outfitWearLogDao.countByUser(user);
    }

    /** Total recorded spend, or null when nothing has a price yet. */
    public BigDecimal getWardrobeValue(User user) {
        return clothingItemDao.sumPurchasePrice(user);
    }

    // --- lists ---------------------------------------------------------------

    public List<ItemWearStat> getMostWornItems(User user) {
        return clothingItemDao.findMostWorn(user, LIST_LIMIT);
    }

    public List<ItemWearStat> getNeverWornItems(User user) {
        return clothingItemDao.findNeverWorn(user, LIST_LIMIT);
    }

    /** Worn at least once, but not in the last {@value #DORMANT_AFTER_DAYS} days. */
    public List<ItemWearStat> getDormantItems(User user) {
        LocalDate cutoff = LocalDate.now().minusDays(DORMANT_AFTER_DAYS);
        return clothingItemDao.findNotWornSince(user, cutoff, LIST_LIMIT);
    }

    public List<CategoryWearStat> getWearsByCategory(User user) {
        return clothingItemDao.countWearsByCategory(user);
    }

    public List<OutfitWearStat> getMostWornOutfits(User user) {
        return outfitWearLogDao.findMostWornOutfits(user, LIST_LIMIT);
    }

    // --- cost per wear -------------------------------------------------------

    /**
     * Priced items ranked by cost per wear, cheapest first. Ranked in Java rather than
     * SQL because the metric divides by {@code max(wearCount, 1)} and that guard is not
     * expressible portably across H2 and PostgreSQL.
     */
    public List<ItemWearStat> getBestValueItems(User user) {
        return rankByCostPerWear(user, true);
    }

    /** Priced items ranked by cost per wear, most expensive first. */
    public List<ItemWearStat> getWorstValueItems(User user) {
        return rankByCostPerWear(user, false);
    }

    private List<ItemWearStat> rankByCostPerWear(User user, boolean ascending) {
        List<ItemWearStat> priced = new ArrayList<>(clothingItemDao.findPricedItems(user));
        Comparator<ItemWearStat> byCost = Comparator.comparing(ItemWearStat::getCostPerWear);
        priced.sort(ascending ? byCost : byCost.reversed());
        // Copy rather than returning subList(): that view is not serializable, and these
        // lists are held by a @ViewScoped bean the container may passivate.
        return priced.size() > LIST_LIMIT ? new ArrayList<>(priced.subList(0, LIST_LIMIT)) : priced;
    }

    /** How many items carry a price, so the page can prompt when the answer is zero. */
    public int getPricedItemCount(User user) {
        return clothingItemDao.findPricedItems(user).size();
    }

    // --- activity ------------------------------------------------------------

    /**
     * Outfit wears bucketed by month for the last {@value #ACTIVITY_MONTHS} months,
     * oldest first. Months with no activity are present with a count of zero so the
     * chart keeps an even x-axis.
     */
    public List<MonthlyWearStat> getRecentActivity(User user) {
        YearMonth thisMonth = YearMonth.now();
        YearMonth firstMonth = thisMonth.minusMonths(ACTIVITY_MONTHS - 1L);

        LocalDate start = firstMonth.atDay(1);
        LocalDate end = thisMonth.atEndOfMonth();

        Map<YearMonth, Long> counts = new HashMap<>();
        for (LocalDate worn : outfitWearLogDao.findWearDates(user, start, end)) {
            counts.merge(YearMonth.from(worn), 1L, Long::sum);
        }

        List<MonthlyWearStat> activity = new ArrayList<>();
        for (int i = 0; i < ACTIVITY_MONTHS; i++) {
            YearMonth month = firstMonth.plusMonths(i);
            activity.add(new MonthlyWearStat(month, counts.getOrDefault(month, 0L)));
        }
        return activity;
    }
}
