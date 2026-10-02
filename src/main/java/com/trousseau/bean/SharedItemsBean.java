package com.trousseau.bean;

import com.trousseau.model.Share;
import com.trousseau.model.User;
import com.trousseau.service.ShareService;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
