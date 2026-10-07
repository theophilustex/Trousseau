package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.ItemStatus;
import com.trousseau.model.Tag;
import com.trousseau.model.User;
import com.trousseau.service.AccessService;
import com.trousseau.service.ClothingItemService;
import com.trousseau.service.ShareService;
import com.trousseau.service.TagService;
import com.trousseau.service.UserService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
    private AccessService accessService;

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
    private String newTagName;
    private UploadedFile uploadedReceipt;
    private ItemStatus retireStatus = ItemStatus.ARCHIVED;
    /** Whether the current user owns the item; everyone else gets a read-only page. */
    private boolean owner;
    private String retireNote;

    @PostConstruct
    public void init() {
        // Loading happens in loadItem(), called from f:viewAction
    }

    /**
     * Loads the item named by the id parameter. Answers 404 when it does not exist or
     * the current user may not see it (see {@link AccessService}).
     */
    public void loadItem() {
        User currentUser = sessionBean.getCurrentUser();
        if (!accessService.canViewItem(currentUser, itemId)) {
            item = null;
            PageResponses.notFound();
            return;
        }
        item = clothingItemService.findById(itemId);
        owner = accessService.ownsItem(currentUser, itemId);
        if (owner) {
            availableTags = tagService.findByOwner(currentUser);
        }
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
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Only the owner can change this item", null));
        return true;
    }

    public void recordWear() {
        if (refuseUnlessOwner()) return;
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
        if (refuseUnlessOwner()) return;
        item = clothingItemService.update(item);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Purchase details saved", null));
    }

    /**
     * Takes the item out of rotation without deleting it, so its wear history and
     * final cost per wear survive.
     */
    public void retireItem() {
        if (refuseUnlessOwner()) return;
        clothingItemService.retire(item, retireStatus, retireNote);
        item = clothingItemService.findById(itemId);
        retireNote = null;

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        item.getName() + " marked as " + item.getStatus().getDisplayName().toLowerCase(), null));
    }

    public void reactivateItem() {
        if (refuseUnlessOwner()) return;
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
        if (refuseUnlessOwner()) return;
        clothingItemService.markWashed(item);
        item = clothingItemService.findById(itemId);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Item marked as washed", null));
    }

    public void addTag() {
        if (refuseUnlessOwner()) return;
        if (selectedTagId != null) {
            Tag tag = tagService.findById(selectedTagId);
            if (tag != null) {
                clothingItemService.addTagToItem(item, tag);
                item = clothingItemService.findById(itemId);
            }
        }
    }

    public void removeTag(Tag tag) {
        if (refuseUnlessOwner()) return;
        clothingItemService.removeTagFromItem(item, tag);
        item = clothingItemService.findById(itemId);
    }

    public void createAndAddTag() {
        if (refuseUnlessOwner()) return;
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
        if (refuseUnlessOwner()) return null;
        clothingItemService.delete(item);
        return "wardrobe?faces-redirect=true";
    }

    public void shareItem() {
        if (refuseUnlessOwner()) return;
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
        if (refuseUnlessOwner()) return;
        if (uploadedReceipt != null && uploadedReceipt.getContent() != null && uploadedReceipt.getContent().length > 0) {
            clothingItemService.saveReceipt(item,
                    uploadedReceipt.getContent(),
                    uploadedReceipt.getContentType(),
                    uploadedReceipt.getFileName());
            // Reload rather than keep the merged copy: hasReceipt is computed by the
            // database, and the copy would also carry the receipt bytes in view state.
            item = clothingItemService.findById(itemId);
            uploadedReceipt = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Receipt uploaded", null));
        }
    }

    public void deleteReceipt() {
        if (refuseUnlessOwner()) return;
        clothingItemService.deleteReceipt(item);
        item = clothingItemService.findById(itemId);
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Receipt deleted", null));
    }

    // Downloads fetch bytes by id: the item's own BLOB fields are lazy and must not be read
    // once the transaction that loaded it has ended.

    public StreamedContent downloadReceipt() {
        if (owner && item != null && item.isHasReceipt()) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(clothingItemService.getReceiptData(item.getId())))
                    .contentType(item.getReceiptContentType())
                    .name(item.getReceiptName())
                    .build();
        }
        return new DefaultStreamedContent();
    }

    public StreamedContent downloadImage() {
        if (item != null && item.isHasImage()) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(clothingItemService.getImageData(item.getId())))
                    .contentType(item.getImageContentType())
                    .name(item.getImageName())
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
