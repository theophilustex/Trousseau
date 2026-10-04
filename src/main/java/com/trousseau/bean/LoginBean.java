package com.trousseau.bean;

import com.trousseau.model.User;
import com.trousseau.service.UserService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@RequestScoped
public class LoginBean {

    @Inject
    private UserService userService;

    @Inject
    private SessionBean sessionBean;

    private String username;
    private String password;

    public String login() {
        User user = userService.authenticate(username, password);
        if (user != null) {
            sessionBean.login(user);
            return "wardrobe?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid username or password", null));
            return null;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
