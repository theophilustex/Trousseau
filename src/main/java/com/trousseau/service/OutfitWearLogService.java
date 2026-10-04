package com.trousseau.service;

import com.trousseau.dao.OutfitWearLogDao;
import com.trousseau.model.ClothingItem;
import com.trousseau.model.Outfit;
import com.trousseau.model.OutfitWearLog;
import com.trousseau.model.User;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Stateless
public class OutfitWearLogService {

    @Inject
    private OutfitWearLogDao outfitWearLogDao;

    @Inject
    private ClothingItemService clothingItemService;

    public OutfitWearLog recordWear(Outfit outfit, User user, LocalDate date) {
        // Record wear on every clothing item in the outfit
        for (ClothingItem item : outfit.getItems()) {
            clothingItemService.recordWear(item);
        }

        OutfitWearLog log = new OutfitWearLog();
        log.setOutfit(outfit);
        log.setUser(user);
        log.setWornDate(date);
        return outfitWearLogDao.save(log);
    }

    public List<OutfitWearLog> findRecent(Outfit outfit, int limit) {
        return outfitWearLogDao.findByOutfitRecent(outfit, limit);
    }

    /** Count how many times the outfit was worn in the given calendar month/year. */
    public long getMonthlyWearCount(Outfit outfit, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return outfitWearLogDao.countByOutfitAndDateRange(outfit, start, end);
    }

    /** Count how many times the outfit was worn during the given season name. */
    public long getSeasonalWearCount(Outfit outfit, String season) {
        LocalDate[] range = seasonDateRange(season, LocalDate.now());
        return outfitWearLogDao.countByOutfitAndDateRange(outfit, range[0], range[1]);
    }

    /** All wear log entries for a user within a date range (for email scheduler). */
    public List<OutfitWearLog> findByUserAndDateRange(User user, LocalDate start, LocalDate end) {
        return outfitWearLogDao.findByUserAndDateRange(user, start, end);
    }

    /** Returns the season name for a given date. */
    public static String getSeasonForDate(LocalDate date) {
        int m = date.getMonthValue();
        if (m >= 3 && m <= 5)  return "Spring";
        if (m >= 6 && m <= 8)  return "Summer";
        if (m >= 9 && m <= 11) return "Fall";
        return "Winter";
    }

    /**
     * Returns {start, end} LocalDate for the given season name, anchored to the
     * year that contains most of that season relative to the reference date.
     */
    private LocalDate[] seasonDateRange(String season, LocalDate reference) {
        int year = reference.getYear();
        switch (season) {
            case "Spring": return new LocalDate[]{ LocalDate.of(year, Month.MARCH, 1),    LocalDate.of(year, Month.MAY, 31) };
            case "Summer": return new LocalDate[]{ LocalDate.of(year, Month.JUNE, 1),     LocalDate.of(year, Month.AUGUST, 31) };
            case "Fall":   return new LocalDate[]{ LocalDate.of(year, Month.SEPTEMBER, 1), LocalDate.of(year, Month.NOVEMBER, 30) };
            default: // Winter spans year boundary
                if (reference.getMonthValue() == 12) {
                    // Dec of this year through Feb of next year
                    return new LocalDate[]{ LocalDate.of(year, Month.DECEMBER, 1), LocalDate.of(year + 1, Month.FEBRUARY, 28) };
                } else {
                    // Jan/Feb of this year — winter started in previous Dec
                    return new LocalDate[]{ LocalDate.of(year - 1, Month.DECEMBER, 1), LocalDate.of(year, Month.FEBRUARY, 28) };
                }
        }
    }
}
