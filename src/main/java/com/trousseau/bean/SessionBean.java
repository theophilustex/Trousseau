package com.trousseau.bean;

import com.trousseau.model.User;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private User currentUser;
    private boolean loggedIn;

    public void login(User user) {
        this.currentUser = user;
        this.loggedIn = true;
        FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put("com.trousseau.loggedIn", true);
    }

    public String logout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.invalidateSession();
        return "login?faces-redirect=true";
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
