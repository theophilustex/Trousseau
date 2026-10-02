package com.trousseau.converter;

import com.trousseau.model.ClothingItem;
import com.trousseau.service.ClothingItemService;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

@FacesConverter(value = "clothingItemConverter", managed = true)
public class ClothingItemConverter implements Converter<ClothingItem> {

    @Inject
    private ClothingItemService clothingItemService;

    @Override
    public ClothingItem getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.valueOf(value);
            return clothingItemService.findById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, ClothingItem item) {
        if (item == null || item.getId() == null) {
            return "";
        }
        return item.getId().toString();
    }
}
