package com.trousseau.converter;

import com.trousseau.model.Tag;
import com.trousseau.service.TagService;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;

@FacesConverter(value = "tagConverter", managed = true)
public class TagConverter implements Converter<Tag> {

    @Inject
    private TagService tagService;

    @Override
    public Tag getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.valueOf(value);
            return tagService.findById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Tag tag) {
        if (tag == null || tag.getId() == null) {
            return "";
        }
        return tag.getId().toString();
    }
}
