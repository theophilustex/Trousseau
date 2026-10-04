package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.ClothingItemSummary;
import com.trousseau.model.Outfit;
import com.trousseau.model.User;
import com.trousseau.service.ClothingItemService;
import com.trousseau.service.OutfitService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class OutfitListBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private OutfitService outfitService;

    @Inject
    private ClothingItemService clothingItemService;

    @Inject
    private SessionBean sessionBean;

    private List<Outfit> outfits;
    private List<Outfit> allOutfits;
    private Outfit selectedOutfit;
    private Outfit newOutfit;
    private List<ClothingItemSummary> availableItems;
    private List<Long> selectedItemIds;
    private String searchTerm;
    private String filterSeason;
    private String filterOccasion;
    private Long filterItemId;

    @PostConstruct
    public void init() {
        newOutfit = new Outfit();
        selectedItemIds = new ArrayList<>();
        loadOutfits();
        User currentUser = sessionBean.getCurrentUser();
        availableItems = clothingItemService.findSummariesByOwner(currentUser);
    }

    public void loadOutfits() {
        User currentUser = sessionBean.getCurrentUser();
        allOutfits = outfitService.findByCreator(currentUser);
        outfits = new ArrayList<>(allOutfits);
    }

    public void applyFilters() {
        outfits = allOutfits.stream()
                .filter(o -> searchTerm == null || searchTerm.trim().isEmpty()
                        || containsIgnoreCase(o.getName(), searchTerm)
                        || containsIgnoreCase(o.getDescription(), searchTerm))
                .filter(o -> filterSeason == null || filterSeason.isEmpty()
                        || o.getSeasons().contains(filterSeason))
                .filter(o -> filterOccasion == null || filterOccasion.isEmpty()
                        || filterOccasion.equals(o.getOccasion()))
                .filter(o -> filterItemId == null
                        || o.getItems().stream().anyMatch(item -> filterItemId.equals(item.getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    public void clearFilters() {
        searchTerm = null;
        filterSeason = null;
        filterOccasion = null;
        filterItemId = null;
        outfits = new ArrayList<>(allOutfits);
    }

    private boolean containsIgnoreCase(String field, String term) {
        return field != null && field.toLowerCase().contains(term.trim().toLowerCase());
    }

    public void createOutfit() {
        User currentUser = sessionBean.getCurrentUser();
        newOutfit.setCreator(currentUser);

        List<ClothingItem> selectedItems = new ArrayList<>();
        if (selectedItemIds != null) {
            for (Long itemId : selectedItemIds) {
                ClothingItem item = clothingItemService.findById(itemId);
                if (item != null) {
                    selectedItems.add(item);
                }
            }
        }
        newOutfit.setItems(selectedItems);

        outfitService.createOutfit(newOutfit);
        newOutfit = new Outfit();
        selectedItemIds = new ArrayList<>();
        loadOutfits();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Outfit created successfully", null));
    }

    public void deleteOutfit() {
        if (selectedOutfit != null) {
            outfitService.deleteOutfit(selectedOutfit);
            selectedOutfit = null;
            loadOutfits();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Outfit deleted successfully", null));
        }
    }

    public List<Outfit> getOutfits() {
        return outfits;
    }

    public void setOutfits(List<Outfit> outfits) {
        this.outfits = outfits;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getFilterSeason() {
        return filterSeason;
    }

    public void setFilterSeason(String filterSeason) {
        this.filterSeason = filterSeason;
    }

    public String getFilterOccasion() {
        return filterOccasion;
    }

    public void setFilterOccasion(String filterOccasion) {
        this.filterOccasion = filterOccasion;
    }

    public Outfit getSelectedOutfit() {
        return selectedOutfit;
    }

    public void setSelectedOutfit(Outfit selectedOutfit) {
        this.selectedOutfit = selectedOutfit;
    }

    public Outfit getNewOutfit() {
        return newOutfit;
    }

    public void setNewOutfit(Outfit newOutfit) {
        this.newOutfit = newOutfit;
    }

    public Long getFilterItemId() {
        return filterItemId;
    }

    public void setFilterItemId(Long filterItemId) {
        this.filterItemId = filterItemId;
    }

    public List<ClothingItemSummary> getAvailableItems() {
        return availableItems;
    }

    public void setAvailableItems(List<ClothingItemSummary> availableItems) {
        this.availableItems = availableItems;
    }

    public List<Long> getSelectedItemIds() {
        return selectedItemIds;
    }

    public void setSelectedItemIds(List<Long> selectedItemIds) {
        this.selectedItemIds = selectedItemIds;
    }
}
