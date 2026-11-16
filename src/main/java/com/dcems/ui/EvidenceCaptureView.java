package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.CaseDAO;
import com.dcems.dao.CustodyDAO;
import com.dcems.dao.EvidenceDAO;
import com.dcems.model.CaseFile;
import com.dcems.model.Evidence;
import com.dcems.util.Fmt;
import com.dcems.util.Hash;
import com.dcems.util.Session;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Evidence capture: attach a file by drag & drop (or browse), compute its SHA-256 fingerprint in the
 * background, store a protected copy in /evidence_store and write the first chain-of-custody entry.
 */
public class EvidenceCaptureView extends ScrollPane {
    public static final String[] TYPES = {"Mobile Phone", "Computer/Laptop", "Storage Device", "Email", "Screenshot",
            "Image/Photo", "Video", "Document", "Chat/Messages", "Network Logs", "Other"};

    private final ComboBox<CaseFile> caseBox = new ComboBox<>(FXCollections.observableArrayList(CaseDAO.all()));
    private final ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(TYPES));
    private final ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("COLLECTED", "IN_ANALYSIS", "VERIFIED", "ARCHIVED"));
    private final TextArea desc = new TextArea();
    private final TextField source = UI.text("e.g. iPhone 13, Dell laptop, USB drive");
    private final TextField location = UI.text("Where was it found / seized?");
    private final Label evNo = new Label();
    private final Label collector = new Label(Session.name());
    private final Label when = new Label();

    private final VBox dropzone = new VBox(8);
    private final Label fileName = new Label("No file attached");
    private final Label fileSize = new Label("—");
    private final Label hashLabel = new Label("Attach a file to generate its SHA-256 fingerprint");
    private final ImageView preview = new ImageView();
    private final StackPane previewBox = new StackPane();

    private File selected;
    private String hash;
    private Task<String> hashTask;
    private final Consumer<String> nav;

    public EvidenceCaptureView(Consumer<String> nav) {
        this.nav = nav;
        VBox page = new VBox(18);
        page.getStyleClass().add("page");
        page.getChildren().add(UI.header("Evidence Capture", "Log new digital or physical evidence and lock it with a cryptographic fingerprint"));

        // ---------- left card: details ----------
        caseBox.setPromptText("Select the case this evidence belongs to");
        type.setPromptText("Select type");
        status.setValue("COLLECTED");
        type.setMaxWidth(Double.MAX_VALUE);
        status.setMaxWidth(Double.MAX_VALUE);
        caseBox.setMaxWidth(Double.MAX_VALUE);
        desc.setPromptText("Describe the evidence: what it is, condition, how it was acquired…");
        desc.setPrefRowCount(4);
        desc.setWrapText(true);

        evNo.getStyleClass().add("section-title");
        evNo.setStyle("-fx-text-fill:#0284c7;");
        refreshMeta();
        VBox meta = new VBox(4, kv("Evidence No", evNo), kv("Collected by", collector), kv("Timestamp", when));
        meta.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:12;-fx-padding:12 14;");

        HBox t2 = new HBox(12, UI.field("EVIDENCE TYPE", type), UI.field("INITIAL STATUS", status));
        t2.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        HBox t3 = new HBox(12, UI.field("SOURCE DEVICE", source), UI.field("LOCATION FOUND", location));
        t3.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        VBox form = UI.card(UI.section("📋  Evidence Details"), meta,
                UI.field("CASE", caseBox), t2, UI.field("DESCRIPTION", desc), t3,
                UI.row(UI.primary("🔒  Capture & Seal Evidence", this::save), UI.ghost("Reset", this::reset)));
        HBox.setHgrow(form, Priority.ALWAYS);

        // ---------- right card: file ----------
        Label icon = new Label("📂");
        icon.getStyleClass().add("dropzone-icon");
        Label t = new Label("Drag & drop evidence file here");
        t.getStyleClass().add("dropzone-text");
        Label or = new Label("or click to browse");
        or.getStyleClass().add("page-sub");
        dropzone.getChildren().addAll(icon, t, or);
        dropzone.getStyleClass().add("dropzone");
        dropzone.setAlignment(Pos.CENTER);
        dropzone.setOnMouseClicked(e -> browse());
        dropzone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) { e.acceptTransferModes(TransferMode.COPY); dropzone.getStyleClass().add("dropzone-active"); }
            e.consume();
        });
        dropzone.setOnDragExited((DragEvent e) -> dropzone.getStyleClass().remove("dropzone-active"));
        dropzone.setOnDragDropped(e -> {
            var files = e.getDragboard().getFiles();
            if (files != null && !files.isEmpty()) loadFile(files.get(0));
            e.setDropCompleted(true);
            e.consume();
        });

        preview.setFitWidth(280);
        preview.setFitHeight(160);
        preview.setPreserveRatio(true);
        Label noPrev = UI.placeholder("File preview");
        previewBox.getChildren().addAll(noPrev, preview);
        previewBox.getStyleClass().add("preview-box");

        hashLabel.getStyleClass().add("hash-text");
        hashLabel.setWrapText(true);
        VBox hashBox = new VBox(4, new Label("SHA-256 FINGERPRINT") {{ setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;-fx-font-weight:bold;"); }}, hashLabel);
        hashBox.getStyleClass().add("hash-box");

        fileName.setWrapText(true);
        fileName.setStyle("-fx-font-weight:bold;");
        VBox filePanel = UI.card(UI.section("📎  Evidence File"), dropzone, previewBox,
                kv("File", fileName), kv("Size", fileSize), hashBox,
                UI.ghost("Remove file", this::removeFile));
        filePanel.setPrefWidth(340);
        filePanel.setMinWidth(340);
        filePanel.setMaxWidth(340);

        HBox body = new HBox(18, form, filePanel);
        page.getChildren().add(body);
        setContent(page);
        setFitToWidth(true);
    }

    private HBox kv(String k, Label v) {
        Label key = new Label(k);
        key.getStyleClass().add("kv-key");
        key.setMinWidth(90);
        v.getStyleClass().add("kv-val");
        return UI.row(key, v);
    }

    private void refreshMeta() {
        evNo.setText(EvidenceDAO.nextEvidenceNo());
        when.setText(Fmt.dt(LocalDateTime.now()));
    }

    private void browse() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select evidence file");
        File f = fc.showOpenDialog(getScene().getWindow());
        if (f != null) loadFile(f);
    }

    private void loadFile(File f) {
        selected = f;
        hash = null;
        fileName.setText(f.getName());
        fileSize.setText(Fmt.size(f.length()));
        hashLabel.setText("Calculating…");
        showPreview(f);
        if (hashTask != null) hashTask.cancel();
        hashTask = new Task<>() { @Override protected String call() throws Exception { return Hash.sha256(f.toPath()); } };
        hashTask.setOnSucceeded(e -> { hash = hashTask.getValue(); hashLabel.setText(hash); Toast.info("Fingerprint generated"); });
        hashTask.setOnFailed(e -> hashLabel.setText("Could not read file"));
        Thread th = new Thread(hashTask, "hash-worker");
        th.setDaemon(true);
        th.start();
    }

    private void showPreview(File f) {
        String n = f.getName().toLowerCase();
        boolean img = n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".bmp");
        preview.setImage(img ? new Image(f.toURI().toString(), 280, 160, true, true, true) : null);
    }

    private void removeFile() {
        selected = null; hash = null;
        if (hashTask != null) hashTask.cancel();
        fileName.setText("No file attached");
        fileSize.setText("—");
        hashLabel.setText("Attach a file to generate its SHA-256 fingerprint");
        preview.setImage(null);
    }

    private void reset() {
        removeFile();
        caseBox.setValue(null); type.setValue(null); status.setValue("COLLECTED");
        desc.clear(); source.clear(); location.clear();
        refreshMeta();
    }

    private void save() {
        if (caseBox.getValue() == null || type.getValue() == null || desc.getText().isBlank()) {
            Toast.error("Case, type and description are required");
            return;
        }
        if (selected != null && hash == null) { Toast.error("Fingerprint still being calculated – please wait"); return; }

        Evidence e = new Evidence();
        e.evidenceNo = EvidenceDAO.nextEvidenceNo();
        e.caseId = caseBox.getValue().id;
        e.type = type.getValue();
        e.description = desc.getText().trim();
        e.sourceDevice = source.getText().trim();
        e.location = location.getText().trim();
        e.status = status.getValue();
        e.collectedBy = Session.user.id;
        e.collectedAt = LocalDateTime.now().withNano(0);

        try {
            if (selected != null) {
                Path dir = Paths.get("evidence_store");
                Files.createDirectories(dir);
                Path dest = dir.resolve(e.evidenceNo + "_" + selected.getName());
                Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                e.fileName = selected.getName();
                e.filePath = dest.toString();
                e.sha256 = hash;
                e.fileSize = selected.length();
            }
        } catch (Exception ex) {
            UI.error("Could not store the evidence file", ex.getMessage());
            return;
        }

        int id = EvidenceDAO.insert(e);
        CustodyDAO.add(id, "COLLECTED", "Scene / Source", Session.name(), "Initial evidence capture" + (e.sha256 != null ? " – SHA-256 sealed" : ""));
        ActivityDAO.log("EVIDENCE_CAPTURED", e.evidenceNo + " added to " + caseBox.getValue().caseNo);
        Toast.success(e.evidenceNo + " captured & sealed");
        reset();
        Platform.runLater(() -> nav.accept("vault"));
    }
}
