package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.ClothingItemSummary;
import com.trousseau.model.Comment;
import com.trousseau.model.Outfit;
import com.trousseau.model.OutfitWearLog;
import com.trousseau.model.Rating;
import com.trousseau.model.RatingType;
import com.trousseau.model.User;
import com.trousseau.service.AccessService;
import com.trousseau.service.ClothingItemService;
import com.trousseau.service.CommentService;
import com.trousseau.service.OutfitService;
import com.trousseau.service.OutfitWearLogService;
import com.trousseau.service.RatingService;
import com.trousseau.service.ShareService;
import com.trousseau.service.UserService;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Named
@ViewScoped
public class OutfitDetailBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private OutfitService outfitService;
    @Inject private AccessService accessService;
    @Inject private RatingService ratingService;
    @Inject private CommentService commentService;
    @Inject private ShareService shareService;
    @Inject private ClothingItemService clothingItemService;
    @Inject private UserService userService;
    @Inject private OutfitWearLogService outfitWearLogService;
    @Inject private SessionBean sessionBean;

    private Outfit outfit;
    private Long outfitId;
    private List<Rating> ratings;
    private List<Comment> comments;
    private String newCommentText;
    private int personalRating;
    private int spouseRating;
    private int friendsRating;
    private Double personalAvg;
    private Double spouseAvg;
    private Double friendsAvg;
    private Double overallAvg;
    private String shareUsername;
    private List<ClothingItemSummary> availableItems;
    private Long selectedItemId;
    /** Creator of the outfit. Others who can see it may rate and comment, nothing more. */
    private boolean owner;

    // Wear tracking
    private List<OutfitWearLog> recentWearLogs;
    private long monthlyWearCount;
    private long seasonalWearCount;
    private String currentSeason;
    private LocalDate wearDate;

    /**
     * Loads the outfit named by the id parameter. Answers 404 when it does not exist or
     * the current user may not see it (see {@link AccessService}).
     */
    public void loadOutfit() {
        User viewer = sessionBean.getCurrentUser();
        if (!accessService.canViewOutfit(viewer, outfitId)) {
            outfit = null;
            PageResponses.notFound();
            return;
        }
        owner = accessService.ownsOutfit(viewer, outfitId);
        outfit = outfitService.findById(outfitId);
        comments = commentService.findByOutfit(outfit);
        if (owner) {
            availableItems = clothingItemService.findSummariesByOwner(viewer);
        }
        loadRatings();
        loadExistingUserRatings();
        loadWearStats();
    }

    public void loadRatings() {
        ratings = ratingService.findByOutfit(outfit);
        personalAvg = ratingService.getAverageRating(outfit, RatingType.PERSONAL);
        spouseAvg = ratingService.getAverageRating(outfit, RatingType.SPOUSE_PARTNER);
        friendsAvg = ratingService.getAverageRating(outfit, RatingType.FRIENDS_OTHER);
        double total = 0.0;
        int count = 0;
        if (personalAvg != null) { total += personalAvg; count++; }
        if (spouseAvg != null) { total += spouseAvg; count++; }
        if (friendsAvg != null) { total += friendsAvg; count++; }
        overallAvg = count > 0 ? total / count : null;
    }

    public void loadExistingUserRatings() {
        User currentUser = sessionBean.getCurrentUser();
        List<Rating> userRatings = ratingService.findByOutfitAndRater(outfit, currentUser);
        personalRating = 0;
        spouseRating = 0;
        friendsRating = 0;
        for (Rating r : userRatings) {
            switch (r.getRatingType()) {
                case PERSONAL: personalRating = r.getScore(); break;
                case SPOUSE_PARTNER: spouseRating = r.getScore(); break;
                case FRIENDS_OTHER: friendsRating = r.getScore(); break;
            }
        }
    }

    private void loadWearStats() {
        recentWearLogs = outfitWearLogService.findRecent(outfit, 10);
        LocalDate today = LocalDate.now();
        monthlyWearCount = outfitWearLogService.getMonthlyWearCount(outfit, today.getYear(), today.getMonthValue());
        currentSeason = OutfitWearLogService.getSeasonForDate(today);
        seasonalWearCount = outfitWearLogService.getSeasonalWearCount(outfit, currentSeason);
        if (wearDate == null) {
            wearDate = today;
        }
    }

    public void recordWear() {
        if (refuseUnlessOwner()) return;
        User currentUser = sessionBean.getCurrentUser();
        LocalDate date = wearDate != null ? wearDate : LocalDate.now();
        outfitWearLogService.recordWear(outfit, currentUser, date);
        loadWearStats();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Wear recorded for " + date, null));
    }

    public void rateOutfit() {
        User currentUser = sessionBean.getCurrentUser();
        if (personalRating > 0) {
            ratingService.addOrUpdateRating(outfit, currentUser, RatingType.PERSONAL, personalRating);
        }
        if (spouseRating > 0) {
            ratingService.addOrUpdateRating(outfit, currentUser, RatingType.SPOUSE_PARTNER, spouseRating);
        }
        if (friendsRating > 0) {
            ratingService.addOrUpdateRating(outfit, currentUser, RatingType.FRIENDS_OTHER, friendsRating);
        }
        loadRatings();
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Ratings submitted", null));
    }

    public void addComment() {
        if (newCommentText != null && !newCommentText.trim().isEmpty()) {
            User currentUser = sessionBean.getCurrentUser();
            commentService.addComment(outfit, currentUser, newCommentText);
            newCommentText = null;
            comments = commentService.findByOutfit(outfit);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Comment added", null));
        }
    }

    public void deleteComment(Comment comment) {
        User currentUser = sessionBean.getCurrentUser();
        if (comment.getAuthor() != null && comment.getAuthor().equals(currentUser)) {
            commentService.deleteComment(comment);
            comments = commentService.findByOutfit(outfit);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Comment deleted", null));
        }
    }

    public void shareOutfit() {
        if (refuseUnlessOwner()) return;
        if (shareUsername != null && !shareUsername.trim().isEmpty()) {
            User targetUser = userService.findByUsername(shareUsername);
            if (targetUser != null) {
                shareService.shareOutfit(sessionBean.getCurrentUser(), targetUser, outfit);
                shareUsername = null;
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Outfit shared successfully", null));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not found", null));
            }
        }
    }

    public void addItemToOutfit() {
        if (refuseUnlessOwner()) return;
        if (selectedItemId != null) {
            ClothingItem item = clothingItemService.findById(selectedItemId);
            if (item != null) {
                outfitService.addItemToOutfit(outfit, item);
                outfit = outfitService.findById(outfitId);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Item added to outfit", null));
            }
        }
    }

    public void removeItemFromOutfit(ClothingItem item) {
        if (refuseUnlessOwner()) return;
        outfitService.removeItemFromOutfit(outfit, item);
        outfit = outfitService.findById(outfitId);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Item removed from outfit", null));
    }

    public String deleteOutfit() {
        if (refuseUnlessOwner()) return null;
        outfitService.deleteOutfit(outfit);
        return "outfits?faces-redirect=true";
    }

    public boolean isOwner() {
        return owner;
    }

    /**
     * Owner-only controls are not rendered for anyone else, and JSF will not invoke an
     * unrendered component, but each action checks again rather than rely on that.
     */
    private boolean refuseUnlessOwner() {
        if (owner) {
            return false;
        }
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Only the owner can change this outfit", null));
        return true;
    }

    // --- Getters/Setters ---

    public Outfit getOutfit() { return outfit; }
    public void setOutfit(Outfit outfit) { this.outfit = outfit; }
    public Long getOutfitId() { return outfitId; }
    public void setOutfitId(Long outfitId) { this.outfitId = outfitId; }
    public List<Rating> getRatings() { return ratings; }
    public void setRatings(List<Rating> ratings) { this.ratings = ratings; }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    public String getNewCommentText() { return newCommentText; }
    public void setNewCommentText(String newCommentText) { this.newCommentText = newCommentText; }
    public int getPersonalRating() { return personalRating; }
    public void setPersonalRating(int personalRating) { this.personalRating = personalRating; }
    public int getSpouseRating() { return spouseRating; }
    public void setSpouseRating(int spouseRating) { this.spouseRating = spouseRating; }
    public int getFriendsRating() { return friendsRating; }
    public void setFriendsRating(int friendsRating) { this.friendsRating = friendsRating; }
    public Double getPersonalAvg() { return personalAvg; }
    public void setPersonalAvg(Double personalAvg) { this.personalAvg = personalAvg; }
    public Double getSpouseAvg() { return spouseAvg; }
    public void setSpouseAvg(Double spouseAvg) { this.spouseAvg = spouseAvg; }
    public Double getFriendsAvg() { return friendsAvg; }
    public void setFriendsAvg(Double friendsAvg) { this.friendsAvg = friendsAvg; }
    public Double getOverallAvg() { return overallAvg; }
    public void setOverallAvg(Double overallAvg) { this.overallAvg = overallAvg; }
    public String getShareUsername() { return shareUsername; }
    public void setShareUsername(String shareUsername) { this.shareUsername = shareUsername; }
    public List<ClothingItemSummary> getAvailableItems() { return availableItems; }
    public void setAvailableItems(List<ClothingItemSummary> availableItems) { this.availableItems = availableItems; }
    public Long getSelectedItemId() { return selectedItemId; }
    public void setSelectedItemId(Long selectedItemId) { this.selectedItemId = selectedItemId; }
    public List<OutfitWearLog> getRecentWearLogs() { return recentWearLogs; }
    public long getMonthlyWearCount() { return monthlyWearCount; }
    public long getSeasonalWearCount() { return seasonalWearCount; }
    public String getCurrentSeason() { return currentSeason; }
    public LocalDate getWearDate() { return wearDate; }
    public void setWearDate(LocalDate wearDate) { this.wearDate = wearDate; }
}
