package com.trousseau.service;

import com.trousseau.dao.OutfitDao;
import com.trousseau.model.ClothingItem;
import com.trousseau.model.Outfit;
import com.trousseau.model.User;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

@Stateless
public class OutfitService {

    @Inject
    private OutfitDao outfitDao;

    public Outfit createOutfit(Outfit outfit) {
        return outfitDao.save(outfit);
    }

    public Outfit findById(Long id) {
        Outfit outfit = outfitDao.findById(id);
        if (outfit != null) {
            outfit.getItems().size();
            outfit.getRatings().size();
            outfit.getComments().size();
            outfit.getSeasons().size();
        }
        return outfit;
    }

    public List<Outfit> findByCreator(User creator) {
        List<Outfit> outfits = outfitDao.findByCreator(creator);
        for (Outfit o : outfits) {
            o.getRatings().size();
            o.getComments().size();
            o.getSeasons().size();
        }
        return outfits;
    }

    public List<Outfit> findSharedWith(User user) {
        return outfitDao.findSharedWith(user);
    }

    public Outfit updateOutfit(Outfit outfit) {
        return outfitDao.update(outfit);
    }

    public void deleteOutfit(Outfit outfit) {
        outfitDao.delete(outfit);
    }

    public Outfit addItemToOutfit(Outfit outfit, ClothingItem item) {
        outfit.getItems().add(item);
        return outfitDao.update(outfit);
    }

    public Outfit removeItemFromOutfit(Outfit outfit, ClothingItem item) {
        outfit.getItems().remove(item);
        return outfitDao.update(outfit);
    }
}
