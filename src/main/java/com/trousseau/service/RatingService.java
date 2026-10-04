package com.trousseau.service;

import com.trousseau.dao.RatingDao;
import com.trousseau.model.Outfit;
import com.trousseau.model.Rating;
import com.trousseau.model.RatingType;
import com.trousseau.model.User;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

@Stateless
public class RatingService {

    @Inject
    private RatingDao ratingDao;

    public Rating addOrUpdateRating(Outfit outfit, User rater, RatingType type, int score) {
        List<Rating> existing = ratingDao.findByOutfitAndRater(outfit, rater);
        for (Rating rating : existing) {
            if (rating.getRatingType() == type) {
                rating.setScore(score);
                return ratingDao.update(rating);
            }
        }

        Rating rating = new Rating();
        rating.setOutfit(outfit);
        rating.setRater(rater);
        rating.setRatingType(type);
        rating.setScore(score);
        return ratingDao.save(rating);
    }

    public List<Rating> findByOutfit(Outfit outfit) {
        return ratingDao.findByOutfit(outfit);
    }

    public List<Rating> findByOutfitAndType(Outfit outfit, RatingType type) {
        return ratingDao.findByOutfitAndType(outfit, type);
    }

    public Double getAverageRating(Outfit outfit, RatingType type) {
        return ratingDao.getAverageByOutfitAndType(outfit, type);
    }

    public List<Rating> findByOutfitAndRater(Outfit outfit, User rater) {
        return ratingDao.findByOutfitAndRater(outfit, rater);
    }

    public void deleteRating(Rating rating) {
        ratingDao.delete(rating);
    }
}
