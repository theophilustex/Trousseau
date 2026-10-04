package com.trousseau.service;

import com.trousseau.dao.ClothingItemDao;
import com.trousseau.dao.OutfitDao;
import com.trousseau.model.User;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * The one place that decides who may see or change an item or outfit.
 *
 * <p>List pages are safe by construction (they only query the current user's rows),
 * but anything that loads a single object by an id from the request, such as the
 * detail pages and the image endpoint, must ask here first. Ids are sequential, so
 * without this check any id can be guessed.</p>
 *
 * <ul>
 *   <li><b>View</b>: the owner, anyone it has been shared with, and, for items, anyone
 *       an outfit containing the item has been shared with.</li>
 *   <li><b>Change</b>: the owner only. Recipients of a shared outfit may still rate and
 *       comment on it, which is the point of sharing.</li>
 * </ul>
 *
 * <p>Every method returns false for a null user or id, so an anonymous request is
 * always refused.</p>
 */
@Stateless
public class AccessService {

    @Inject
    private ClothingItemDao clothingItemDao;

    @Inject
    private OutfitDao outfitDao;

    public boolean canViewItem(User user, Long itemId) {
        return user != null && itemId != null && clothingItemDao.isVisibleTo(itemId, user);
    }

    public boolean ownsItem(User user, Long itemId) {
        return user != null && itemId != null && clothingItemDao.isOwnedBy(itemId, user);
    }

    public boolean canViewOutfit(User user, Long outfitId) {
        return user != null && outfitId != null && outfitDao.isVisibleTo(outfitId, user);
    }

    public boolean ownsOutfit(User user, Long outfitId) {
        return user != null && outfitId != null && outfitDao.isCreatedBy(outfitId, user);
    }
}
