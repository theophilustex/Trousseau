package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.Tag;
import com.trousseau.model.User;
import com.trousseau.service.ClothingItemService;
import com.trousseau.service.TagService;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.primefaces.model.file.UploadedFile;

@Named
@ViewScoped
public class WardrobeBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClothingItemService clothingItemService;

    @Inject
    private TagService tagService;

    @Inject
    private SessionBean sessionBean;

    private List<ClothingItem> clothingItems;
    private List<ClothingItem> allItems;
    private ClothingItem selectedItem;
    private ClothingItem newItem;
    private UploadedFile uploadedFile;
    private String selectedCategory;
    private Tag selectedTag;
    private List<Tag> availableTags;
    private List<String> categories;
    private List<ClothingItem> washAlerts;
    private String newTagName;
    private String searchTerm;
    private List<ClothingItem> retiredItems;
    private boolean showingRetired;

    @PostConstruct
    public void init() {
        newItem = new ClothingItem();
        loadItems();
        User currentUser = sessionBean.getCurrentUser();
        availableTags = tagService.findByOwner(currentUser);
        categories = clothingItemService.getCategories();
        washAlerts = clothingItemService.findNeedingWash(currentUser);
    }

    public void loadItems() {
        User currentUser = sessionBean.getCurrentUser();
        allItems = clothingItemService.findByOwner(currentUser);
        clothingItems = new ArrayList<>(allItems);
    }

    public void filterByCategory() {
        if (selectedCategory == null || selectedCategory.isEmpty()) {
            clothingItems = new ArrayList<>(allItems);
        } else {
            clothingItems = allItems.stream()
                    .filter(item -> selectedCategory.equals(item.getCategory()))
                    .collect(Collectors.toList());
        }
    }

    public void filterByTag() {
        if (selectedTag == null) {
            clothingItems = new ArrayList<>(allItems);
        } else {
            clothingItems = clothingItemService.findByTag(selectedTag);
        }
    }

    public void clearFilters() {
        selectedCategory = null;
        selectedTag = null;
        searchTerm = null;
        clothingItems = new ArrayList<>(allItems);
    }

    public void search() {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            clothingItems = new ArrayList<>(allItems);
        } else {
            String lowerSearch = searchTerm.toLowerCase();
            clothingItems = allItems.stream()
                    .filter(item -> item.getName() != null
                            && item.getName().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }
    }

    public void saveNewItem() {
        User currentUser = sessionBean.getCurrentUser();
        newItem.setOwner(currentUser);

        if (uploadedFile != null && uploadedFile.getContent() != null && uploadedFile.getContent().length > 0) {
            clothingItemService.attachImage(newItem, uploadedFile.getContent(),
                    uploadedFile.getContentType(), uploadedFile.getFileName());
        }

        clothingItemService.save(newItem);
        newItem = new ClothingItem();
        uploadedFile = null;
        loadItems();
        washAlerts = clothingItemService.findNeedingWash(currentUser);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Item saved successfully", null));
    }

    public void updateItem() {
        clothingItemService.update(selectedItem);
        loadItems();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Item updated successfully", null));
    }

    public void deleteItem() {
        clothingItemService.delete(selectedItem);
        selectedItem = null;
        loadItems();
        washAlerts = clothingItemService.findNeedingWash(sessionBean.getCurrentUser());

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Item deleted successfully", null));
    }

    public void recordWear() {
        clothingItemService.recordWear(selectedItem);
        loadItems();
        washAlerts = clothingItemService.findNeedingWash(sessionBean.getCurrentUser());

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Wear recorded", null));
    }

    public void recordWearItem(ClothingItem item) {
        clothingItemService.recordWear(item);
        loadItems();
        washAlerts = clothingItemService.findNeedingWash(sessionBean.getCurrentUser());

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Wear recorded for " + item.getName(), null));
    }

    public void markWashed() {
        clothingItemService.markWashed(selectedItem);
        loadItems();
        washAlerts = clothingItemService.findNeedingWash(sessionBean.getCurrentUser());

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Item marked as washed", null));
    }

    public void markWashedItem(ClothingItem item) {
        clothingItemService.markWashed(item);
        loadItems();
        washAlerts = clothingItemService.findNeedingWash(sessionBean.getCurrentUser());

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, item.getName() + " marked as washed", null));
    }

    public void createTag() {
        if (newTagName != null && !newTagName.trim().isEmpty()) {
            tagService.createTag(sessionBean.getCurrentUser(), newTagName);
            availableTags = tagService.findByOwner(sessionBean.getCurrentUser());
            newTagName = null;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Tag created", null));
        }
    }

    public void addTagToItem() {
        clothingItemService.addTagToItem(selectedItem, selectedTag);
        loadItems();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Tag added to item", null));
    }

    public void removeTagFromItem(Tag tag) {
        clothingItemService.removeTagFromItem(selectedItem, tag);
        loadItems();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Tag removed from item", null));
    }

    public int getWashAlertCount() {
        return washAlerts != null ? washAlerts.size() : 0;
    }

    // --- retired items -------------------------------------------------------

    /** Loads items taken out of rotation, shown in a separate collapsed section. */
    public void loadRetired() {
        retiredItems = clothingItemService.findRetired(sessionBean.getCurrentUser());
        showingRetired = true;
    }

    public void hideRetired() {
        showingRetired = false;
    }

    public void reactivate(ClothingItem item) {
        clothingItemService.reactivate(item);
        loadItems();
        loadRetired();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        item.getName() + " is back in your wardrobe", null));
    }

    public List<ClothingItem> getRetiredItems() {
        return retiredItems;
    }

    public boolean isShowingRetired() {
        return showingRetired;
    }

    public int getRetiredCount() {
        return retiredItems != null ? retiredItems.size() : 0;
    }

    public List<ClothingItem> getClothingItems() {
        return clothingItems;
    }

    public void setClothingItems(List<ClothingItem> clothingItems) {
        this.clothingItems = clothingItems;
    }

    public List<ClothingItem> getAllItems() {
        return allItems;
    }

    public void setAllItems(List<ClothingItem> allItems) {
        this.allItems = allItems;
    }

    public ClothingItem getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(ClothingItem selectedItem) {
        this.selectedItem = selectedItem;
    }

    public ClothingItem getNewItem() {
        return newItem;
    }

    public void setNewItem(ClothingItem newItem) {
        this.newItem = newItem;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public Tag getSelectedTag() {
        return selectedTag;
    }

    public void setSelectedTag(Tag selectedTag) {
        this.selectedTag = selectedTag;
    }

    public List<Tag> getAvailableTags() {
        return availableTags;
    }

    public void setAvailableTags(List<Tag> availableTags) {
        this.availableTags = availableTags;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<ClothingItem> getWashAlerts() {
        return washAlerts;
    }

    public void setWashAlerts(List<ClothingItem> washAlerts) {
        this.washAlerts = washAlerts;
    }

    public String getNewTagName() {
        return newTagName;
    }

    public void setNewTagName(String newTagName) {
        this.newTagName = newTagName;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}
