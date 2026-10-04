package com.trousseau.bean;

import com.trousseau.model.User;
import com.trousseau.service.UserService;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named
@ViewScoped
public class ProfileBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private UserService userService;

    @Inject
    private SessionBean sessionBean;

    private String displayName;
    private String email;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;

    @PostConstruct
    public void init() {
        // Load fresh rather than show the session snapshot, which may predate a change
        // made from another device.
        User currentUser = sessionBean.getCurrentUser() == null ? null
                : userService.findById(sessionBean.getCurrentUser().getId());
        if (currentUser != null) {
            displayName = currentUser.getDisplayName();
            email = currentUser.getEmail();
            latitude = currentUser.getLatitude();
            longitude = currentUser.getLongitude();
            locationName = currentUser.getLocationName();
        }
    }

    /*
     * Every save below goes through an id-based UserService method and then refreshes the
     * session's snapshot. Merging the snapshot itself would write back stale fields,
     * including a password hash from before a reset.
     */

    public void updateProfile() {
        Long userId = sessionBean.getCurrentUser().getId();
        if (userService.isEmailUsedByOther(userId, email)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "That email address is used by another account", null));
            return;
        }
        sessionBean.refresh(userService.updateProfile(userId, displayName, email));

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Profile updated successfully", null));
    }

    /**
     * Saves the coordinates the planner uses to fetch a forecast. Clearing either one
     * turns weather off, which is the right behaviour for a server with no route out.
     */
    public void updateLocation() {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Latitude must be between -90 and 90", null));
            return;
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Longitude must be between -180 and 180", null));
            return;
        }

        Long userId = sessionBean.getCurrentUser().getId();
        sessionBean.refresh(userService.updateLocation(userId, latitude, longitude, locationName));

        boolean on = latitude != null && longitude != null;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        on ? "Location saved — the planner will use the forecast"
                           : "Location cleared — the planner will ignore weather", null));
    }

    public void clearLocation() {
        latitude = null;
        longitude = null;
        locationName = null;
        updateLocation();
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    /** Changes the password, keeping this session signed in and signing out all others. */
    public void changePassword() {
        if (!newPassword.equals(confirmNewPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "New passwords do not match", null));
            return;
        }

        User updated = userService.changePassword(sessionBean.getCurrentUser().getId(), currentPassword, newPassword);
        if (updated == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Current password is incorrect", null));
            return;
        }
        sessionBean.refresh(updated);
        currentPassword = null;
        newPassword = null;
        confirmNewPassword = null;

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Password changed successfully", null));
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }
}
