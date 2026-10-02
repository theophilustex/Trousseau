package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.model.User;
import com.trousseau.service.ClothingItemService;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The laundry basket: everything flagged for washing, cleared in one action instead
 * of one button press per garment.
 */
@Named
@ViewScoped
public class LaundryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClothingItemService clothingItemService;

    @Inject
    private SessionBean sessionBean;

    private List<ClothingItem> basket;
    private List<ClothingItem> selected = new ArrayList<>();

    @PostConstruct
    public void init() {
        load();
    }

    public void load() {
        User currentUser = sessionBean.getCurrentUser();
        basket = clothingItemService.findLaundryBasket(currentUser);
        selected = new ArrayList<>();
    }

    /** Washes the ticked items. */
    public void washSelected() {
        if (selected == null || selected.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Select at least one item first", null));
            return;
        }
        int count = clothingItemService.markWashedBatch(selected);
        load();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        count + (count == 1 ? " item" : " items") + " marked as washed", null));
    }

    /** Washes everything in the basket. */
    public void washAll() {
        if (basket == null || basket.isEmpty()) {
            return;
        }
        int count = clothingItemService.markWashedBatch(new ArrayList<>(basket));
        load();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Laundry done — " + count + (count == 1 ? " item" : " items") + " marked as washed", null));
    }

    /** Washes a single item, for the per-row button. */
    public void washItem(ClothingItem item) {
        clothingItemService.markWashed(item);
        load();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, item.getName() + " marked as washed", null));
    }

    public void selectAll() {
        selected = new ArrayList<>(basket);
    }

    public void clearSelection() {
        selected = new ArrayList<>();
    }

    /**
     * Named "basketEmpty", not "empty": {@code empty} is a reserved operator in EL, so
     * {@code #{laundryBean.empty}} is a parse error rather than a property read.
     */
    public boolean isBasketEmpty() {
        return basket == null || basket.isEmpty();
    }

    public int getBasketCount() {
        return basket == null ? 0 : basket.size();
    }

    public int getSelectedCount() {
        return selected == null ? 0 : selected.size();
    }

    public List<ClothingItem> getBasket() {
        return basket;
    }

    public List<ClothingItem> getSelected() {
        return selected;
    }

    public void setSelected(List<ClothingItem> selected) {
        this.selected = selected;
    }
}
