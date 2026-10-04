package com.trousseau.bean;

import com.trousseau.model.Share;
import com.trousseau.model.User;
import com.trousseau.service.ShareService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class SharedItemsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ShareService shareService;

    @Inject
    private SessionBean sessionBean;

    private List<Share> sharedByMe;
    private List<Share> sharedWithMe;

    @PostConstruct
    public void init() {
        loadShares();
    }

    public void loadShares() {
        User currentUser = sessionBean.getCurrentUser();
        sharedByMe = shareService.findSharedByUser(currentUser);
        sharedWithMe = shareService.findSharedWithUser(currentUser);
    }

    public void unshare(Share share) {
        shareService.unshare(share);
        loadShares();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Share removed", null));
    }

    public List<Share> getSharedByMe() {
        return sharedByMe;
    }

    public void setSharedByMe(List<Share> sharedByMe) {
        this.sharedByMe = sharedByMe;
    }

    public List<Share> getSharedWithMe() {
        return sharedWithMe;
    }

    public void setSharedWithMe(List<Share> sharedWithMe) {
        this.sharedWithMe = sharedWithMe;
    }
}
