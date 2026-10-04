package com.trousseau.bean;

import com.trousseau.model.User;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.io.Serializable;
import java.util.Map;

@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** HTTP session attributes read by AuthFilter, which can't see CDI session beans. */
    public static final String LOGGED_IN_ATTR = "com.trousseau.loggedIn";
    public static final String USER_ID_ATTR = "com.trousseau.userId";
    public static final String CREDENTIALS_VERSION_ATTR = "com.trousseau.credentialsVersion";

    /**
     * Snapshot of the user, taken at login and replaced by {@link #refresh}. Fine for
     * reading, and for passing to queries as the owner. Never merge it back into the
     * database: its fields may be out of date (see UserService).
     */
    private User currentUser;
    private boolean loggedIn;

    public void login(User user) {
        this.loggedIn = true;
        FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put(LOGGED_IN_ATTR, true);
        refresh(user);
    }

    /**
     * Replaces the snapshot with a freshly saved user. After a password change this also
     * records the new credentials version, which is what keeps this session signed in
     * while AuthFilter signs out the others.
     */
    public void refresh(User user) {
        this.currentUser = user;
        Map<String, Object> session = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        session.put(USER_ID_ATTR, user.getId());
        session.put(CREDENTIALS_VERSION_ATTR, user.getCredentialsVersion());
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

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
