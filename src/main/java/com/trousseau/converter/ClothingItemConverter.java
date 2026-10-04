package com.trousseau.converter;

import com.trousseau.model.ClothingItem;
import com.trousseau.service.ClothingItemService;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

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
