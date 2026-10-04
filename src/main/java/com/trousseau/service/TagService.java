package com.trousseau.service;

import com.trousseau.dao.TagDao;
import com.trousseau.model.Tag;
import com.trousseau.model.User;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

@Stateless
public class TagService {

    @Inject
    private TagDao tagDao;

    public Tag createTag(User owner, String name) {
        Tag existing = tagDao.findByOwnerAndName(owner, name);
        if (existing != null) {
            throw new IllegalArgumentException("Tag '" + name + "' already exists for this user.");
        }
        Tag tag = new Tag();
        tag.setOwner(owner);
        tag.setName(name);
        return tagDao.save(tag);
    }

    public Tag findById(Long id) {
        return tagDao.findById(id);
    }

    public List<Tag> findByOwner(User owner) {
        return tagDao.findByOwner(owner);
    }

    public Tag findOrCreate(User owner, String name) {
        Tag existing = tagDao.findByOwnerAndName(owner, name);
        if (existing != null) {
            return existing;
        }
        Tag tag = new Tag();
        tag.setOwner(owner);
        tag.setName(name);
        return tagDao.save(tag);
    }

    public Tag updateTag(Tag tag) {
        return tagDao.update(tag);
    }

    public void deleteTag(Tag tag) {
        tagDao.delete(tag);
    }
}
