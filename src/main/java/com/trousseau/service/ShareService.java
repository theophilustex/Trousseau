package com.trousseau.service;

import com.trousseau.dao.ShareDao;
import com.trousseau.model.ClothingItem;
import com.trousseau.model.Outfit;
import com.trousseau.model.Share;
import com.trousseau.model.User;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

@Stateless
public class ShareService {

    @Inject
    private ShareDao shareDao;

    public Share shareClothingItem(User owner, User sharedWith, ClothingItem item) {
        if (shareDao.exists(owner, sharedWith, item, null)) {
            throw new IllegalArgumentException("This clothing item is already shared with this user.");
        }
        Share share = new Share();
        share.setOwner(owner);
        share.setSharedWith(sharedWith);
        share.setClothingItem(item);
        return shareDao.save(share);
    }

    public Share shareOutfit(User owner, User sharedWith, Outfit outfit) {
        if (shareDao.exists(owner, sharedWith, null, outfit)) {
            throw new IllegalArgumentException("This outfit is already shared with this user.");
        }
        Share share = new Share();
        share.setOwner(owner);
        share.setSharedWith(sharedWith);
        share.setOutfit(outfit);
        return shareDao.save(share);
    }

    public List<Share> findSharedByUser(User owner) {
        return shareDao.findByOwner(owner);
    }

    public List<Share> findSharedWithUser(User user) {
        return shareDao.findSharedWithUser(user);
    }

    public void unshare(Share share) {
        shareDao.delete(share);
    }
}
