package com.trousseau.bean;

import com.trousseau.model.User;
import com.trousseau.service.DataExportImportService;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Named
@ViewScoped
public class DataManagementBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject private DataExportImportService exportImportService;
    @Inject private SessionBean sessionBean;

    private UploadedFile importFile;
    private String importResult;

    public StreamedContent exportData() {
        User user = sessionBean.getCurrentUser();
        try {
            String json = exportImportService.exportUserData(user);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            String filename = "trousseau-export-" + user.getUsername() + "-" + LocalDate.now() + ".json";
            return DefaultStreamedContent.builder()
                    .name(filename)
                    .contentType("application/json")
                    .stream(() -> new ByteArrayInputStream(bytes))
                    .build();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Export failed: " + e.getMessage(), null));
            return null;
        }
    }

    public void importData() {
        if (importFile == null || importFile.getSize() == 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Please select a JSON export file.", null));
            return;
        }

        User user = sessionBean.getCurrentUser();
        try {
            String json = new String(importFile.getContent(), StandardCharsets.UTF_8);
            importResult = exportImportService.importUserData(user, json);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, importResult, null));
        } catch (Exception e) {
            importResult = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Import failed: " + e.getMessage(), null));
        }
        importFile = null;
    }

    public UploadedFile getImportFile() { return importFile; }
    public void setImportFile(UploadedFile importFile) { this.importFile = importFile; }
    public String getImportResult() { return importResult; }
}
