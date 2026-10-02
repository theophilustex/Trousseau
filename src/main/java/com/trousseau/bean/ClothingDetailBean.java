package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.ItemStatus;
import com.trousseau.model.Tag;
import com.trousseau.model.User;
import com.trousseau.service.ClothingItemService;
import com.trousseau.service.ShareService;
import com.trousseau.service.TagService;
import com.trousseau.service.UserService;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.List;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

@Named
@ViewScoped
public class ClothingDetailBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClothingItemService clothingItemService;

    @Inject
    private TagService tagService;

    @Inject
    private ShareService shareService;

    @Inject
    private UserService userService;

    @Inject
    private SessionBean sessionBean;

    private ClothingItem item;
    private Long itemId;
    private List<Tag> availableTags;
    private Long selectedTagId;
    private String shareUsername;
    private List<User> allUsers;
    private String newTagName;
    private UploadedFile uploadedReceipt;
    private ItemStatus retireStatus = ItemStatus.ARCHIVED;
    private String retireNote;

    @PostConstruct
    public void init() {
        // Loading happens in loadItem(), called from f:viewAction
    }

    public void loadItem() {
        if (itemId != null) {
            item = clothingItemService.findById(itemId);
            User currentUser = sessionBean.getCurrentUser();
            availableTags = tagService.findByOwner(currentUser);
            allUsers = userService.findAll();
        }
    }

    public void recordWear() {
        clothingItemService.recordWear(item);
        item = clothingItemService.findById(itemId);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Wear recorded", null));
    }

    /**
     * Persists the purchase price and date entered on the detail page. This is the only
     * edit path for an item that already exists, so cost-per-wear can be filled in for a
     * wardrobe that was catalogued before the field existed.
     */
    public void savePurchaseInfo() {
        item = clothingItemService.update(item);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Purchase details saved", null));
    }

    /**
     * Takes the item out of rotation without deleting it, so its wear history and
     * final cost per wear survive.
     */
    public void retireItem() {
        clothingItemService.retire(item, retireStatus, retireNote);
        item = clothingItemService.findById(itemId);
        retireNote = null;

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        item.getName() + " marked as " + item.getStatus().getDisplayName().toLowerCase(), null));
    }

    public void reactivateItem() {
        clothingItemService.reactivate(item);
        item = clothingItemService.findById(itemId);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        item.getName() + " returned to your wardrobe", null));
    }

    public List<ItemStatus> getRetirementStatuses() {
        return clothingItemService.getRetirementStatuses();
    }

    public ItemStatus getRetireStatus() { return retireStatus; }
    public void setRetireStatus(ItemStatus retireStatus) { this.retireStatus = retireStatus; }
    public String getRetireNote() { return retireNote; }
    public void setRetireNote(String retireNote) { this.retireNote = retireNote; }

    public void markWashed() {
        clothingItemService.markWashed(item);
        item = clothingItemService.findById(itemId);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Item marked as washed", null));
    }

    public void addTag() {
        if (selectedTagId != null) {
            Tag tag = tagService.findById(selectedTagId);
            if (tag != null) {
                clothingItemService.addTagToItem(item, tag);
                item = clothingItemService.findById(itemId);
            }
        }
    }

    public void removeTag(Tag tag) {
        clothingItemService.removeTagFromItem(item, tag);
        item = clothingItemService.findById(itemId);
    }

    public void createAndAddTag() {
        if (newTagName != null && !newTagName.trim().isEmpty()) {
            User currentUser = sessionBean.getCurrentUser();
            Tag tag = tagService.findOrCreate(currentUser, newTagName);
            clothingItemService.addTagToItem(item, tag);
            item = clothingItemService.findById(itemId);
            availableTags = tagService.findByOwner(currentUser);
            newTagName = null;
        }
    }

    public String deleteItem() {
        clothingItemService.delete(item);
        return "wardrobe?faces-redirect=true";
    }

    public void shareItem() {
        if (shareUsername != null && !shareUsername.trim().isEmpty()) {
            User targetUser = userService.findByUsername(shareUsername);
            if (targetUser != null) {
                shareService.shareClothingItem(sessionBean.getCurrentUser(), targetUser, item);
                shareUsername = null;

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Item shared successfully", null));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "User not found", null));
            }
        }
    }

    public void uploadReceipt() {
        if (uploadedReceipt != null && uploadedReceipt.getContent() != null && uploadedReceipt.getContent().length > 0) {
            item = clothingItemService.saveReceipt(item,
                    uploadedReceipt.getContent(),
                    uploadedReceipt.getContentType(),
                    uploadedReceipt.getFileName());
            uploadedReceipt = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Receipt uploaded", null));
        }
    }

    public void deleteReceipt() {
        item = clothingItemService.deleteReceipt(item);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Receipt deleted", null));
    }

    public StreamedContent downloadReceipt() {
        if (item != null && item.getReceiptData() != null) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(clothingItemService.getReceiptData(item.getId())))
                    .contentType(item.getReceiptContentType())
                    .name(item.getReceiptName())
                    .build();
        }
        return new DefaultStreamedContent();
    }

    public StreamedContent downloadImage() {
        if (item != null && item.getImageData() != null) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(item.getImageData()))
                    .contentType(item.getImageContentType())
                    .name(item.getImageName())
                    .build();
        }
        return new DefaultStreamedContent();
    }

    public StreamedContent getImageStreamedContent() {
        if (item != null && item.getImageData() != null) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(item.getImageData()))
                    .contentType(item.getImageContentType())
                    .build();
        }
        return new DefaultStreamedContent();
    }

    public ClothingItem getItem() {
        return item;
    }

    public void setItem(ClothingItem item) {
        this.item = item;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public List<Tag> getAvailableTags() {
        return availableTags;
    }

    public void setAvailableTags(List<Tag> availableTags) {
        this.availableTags = availableTags;
    }

    public Long getSelectedTagId() {
        return selectedTagId;
    }

    public void setSelectedTagId(Long selectedTagId) {
        this.selectedTagId = selectedTagId;
    }

    public String getShareUsername() {
        return shareUsername;
    }

    public void setShareUsername(String shareUsername) {
        this.shareUsername = shareUsername;
    }

    public List<User> getAllUsers() {
        return allUsers;
    }

    public void setAllUsers(List<User> allUsers) {
        this.allUsers = allUsers;
    }

    public String getNewTagName() {
        return newTagName;
    }

    public void setNewTagName(String newTagName) {
        this.newTagName = newTagName;
    }

    public UploadedFile getUploadedReceipt() {
        return uploadedReceipt;
    }

    public void setUploadedReceipt(UploadedFile uploadedReceipt) {
        this.uploadedReceipt = uploadedReceipt;
    }
}
