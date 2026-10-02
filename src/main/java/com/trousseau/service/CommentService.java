package com.trousseau.service;

import com.trousseau.dao.CommentDao;
import com.trousseau.model.Comment;
import com.trousseau.model.Outfit;
import com.trousseau.model.User;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

@Stateless
public class CommentService {

    @Inject
    private CommentDao commentDao;

    public Comment addComment(Outfit outfit, User author, String text) {
        Comment comment = new Comment();
        comment.setOutfit(outfit);
        comment.setAuthor(author);
        comment.setText(text);
        return commentDao.save(comment);
    }

    public List<Comment> findByOutfit(Outfit outfit) {
        return commentDao.findByOutfit(outfit);
    }

    public Comment updateComment(Comment comment) {
        return commentDao.update(comment);
    }

    public void deleteComment(Comment comment) {
        commentDao.delete(comment);
    }
}
