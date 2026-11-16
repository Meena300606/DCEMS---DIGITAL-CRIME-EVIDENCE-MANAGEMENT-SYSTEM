package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.CustodyDAO;
import com.dcems.dao.EvidenceDAO;
import com.dcems.model.CustodyEntry;
import com.dcems.model.Evidence;
import com.dcems.util.Fmt;
import com.dcems.util.Hash;
import com.dcems.util.Session;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Searchable evidence register with integrity verification, status changes and custody transfer. */
public class EvidenceVaultView extends VBox {
    private final TableView<Evidence> table = UI.table();
    private final TextField search = UI.text("🔍  Search number, description, case, file…");
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Label count = new Label();
    private final Button details = UI.ghost("👁  Details", this::showDetails);
    private final Button verify = UI.primary("✔  Verify Integrity", this::verify);
    private final Button statusBtn = UI.ghost("🔄  Change Status", this::changeStatus);
    private final Button transfer = UI.ghost("🔁  Transfer Custody", this::transfer);
    private final Button delete = UI.danger("🗑  Delete", this::delete);

    public EvidenceVaultView() {
        getStyleClass().add("page");
        setSpacing(16);

        search.getStyleClass().add("search-box");
        typeFilter.getItems().add("All");
        typeFilter.getItems().addAll(EvidenceCaptureView.TYPES);
        typeFilter.setValue("All");
        statusFilter.getItems().addAll("All", "COLLECTED", "IN_ANALYSIS", "VERIFIED", "ARCHIVED");
        statusFilter.setValue("All");
        search.textProperty().addListener((o, a, b) -> refresh());
        typeFilter.valueProperty().addListener((o, a, b) -> refresh());
        statusFilter.valueProperty().addListener((o, a, b) -> refresh());

        HBox filters = UI.row(UI.header("Evidence Vault", "Every captured item, protected by its SHA-256 fingerprint"),
                UI.spacer(), search, typeFilter, statusFilter);

        table.getColumns().add(UI.col("Evidence No", (Evidence e) -> e.evidenceNo, 120));
        table.getColumns().add(UI.col("Case", (Evidence e) -> e.caseNo, 125));
        table.getColumns().add(UI.col("Type", (Evidence e) -> e.type, 115));
        table.getColumns().add(UI.flex(UI.col("Description", (Evidence e) -> e.description, 240)));
        table.getColumns().add(UI.badgeCol("Status", (Evidence e) -> e.status, 125));
        table.getColumns().add(UI.col("Collected By", (Evidence e) -> Fmt.nz(e.collector), 115));
        table.getColumns().add(UI.col("Collected On", (Evidence e) -> Fmt.dt(e.collectedAt), 150));
        table.setOnMouseClicked(e -> { if (e.getClickCount() == 2) showDetails(); });
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> updateButtons());

        count.getStyleClass().add("page-sub");
        HBox actions = UI.row(details, verify, statusBtn, transfer, UI.spacer(), count, delete);
        getChildren().addAll(filters, table, actions);
        updateButtons();
        refresh();
    }

    private void updateButtons() {
        boolean none = table.getSelectionModel().getSelectedItem() == null;
        details.setDisable(none); verify.setDisable(none); statusBtn.setDisable(none);
        transfer.setDisable(none); delete.setDisable(none || !Session.isAdmin() && !isOwner());
    }

    private boolean isOwner() {
        Evidence e = table.getSelectionModel().getSelectedItem();
        return e != null && e.collectedBy == Session.user.id;
    }

    private void refresh() {
        List<Evidence> list = EvidenceDAO.search(search.getText(), typeFilter.getValue(), statusFilter.getValue());
        table.setItems(FXCollections.observableArrayList(list));
        count.setText(list.size() + " item(s)");
    }

    private Evidence sel() { return table.getSelectionModel().getSelectedItem(); }

    // ---------- details dialog ----------
    private void showDetails() {
        Evidence e = sel();
        if (e == null) return;
        GridPane g = new GridPane();
        g.setHgap(16); g.setVgap(8);
        String[][] rows = {
                {"Evidence No", e.evidenceNo}, {"Case", e.caseNo}, {"Type", e.type}, {"Status", e.status.replace('_', ' ')},
                {"Description", e.description}, {"Source device", Fmt.nz(e.sourceDevice)}, {"Location found", Fmt.nz(e.location)},
                {"Collected by", Fmt.nz(e.collector)}, {"Collected on", Fmt.dt(e.collectedAt)},
                {"File", e.fileName == null ? "No file attached (physical item / note)" : e.fileName + "  (" + Fmt.size(e.fileSize) + ")"}};
        for (int i = 0; i < rows.length; i++) {
            Label k = new Label(rows[i][0]);
            k.getStyleClass().add("kv-key");
            Label v = new Label(rows[i][1]);
            v.setWrapText(true);
            v.setMaxWidth(420);
            v.getStyleClass().add("kv-val");
            g.add(k, 0, i); g.add(v, 1, i);
        }
        VBox box = new VBox(14, g);
        if (e.sha256 != null) {
            Label h = new Label(e.sha256);
            h.getStyleClass().add("hash-text");
            h.setWrapText(true);
            VBox hb = new VBox(3, new Label("SHA-256") {{ setStyle("-fx-text-fill:#94a3b8;-fx-font-size:10px;-fx-font-weight:bold;"); }}, h);
            hb.getStyleClass().add("hash-box");
            box.getChildren().add(hb);
        }
        if (e.filePath != null && isImage(e.fileName) && new File(e.filePath).exists()) {
            ImageView iv = new ImageView(new Image(new File(e.filePath).toURI().toString(), 420, 200, true, true, true));
            box.getChildren().add(iv);
        }
        TableView<CustodyEntry> log = UI.table();
        log.getColumns().add(UI.col("When", (CustodyEntry c) -> Fmt.dt(c.loggedAt), 130));
        log.getColumns().add(UI.col("Action", (CustodyEntry c) -> c.action.replace('_', ' '), 110));
        log.getColumns().add(UI.col("From → To", (CustodyEntry c) -> Fmt.nz(c.fromPerson) + " → " + Fmt.nz(c.toPerson), 190));
        log.setItems(FXCollections.observableArrayList(CustodyDAO.list(e.id)));
        log.setPrefHeight(150);
        log.setMinHeight(150);
        box.getChildren().addAll(UI.section("Chain of custody"), log);
        box.setPrefWidth(560);

        Dialog<Void> d = new Dialog<>();
        d.setTitle("Evidence details");
        d.setHeaderText("🗄  " + e.evidenceNo);
        d.getDialogPane().setContent(box);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        UI.style(d);
        d.showAndWait();
    }

    private static boolean isImage(String n) {
        if (n == null) return false;
        n = n.toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".bmp");
    }

    // ---------- integrity check ----------
    private void verify() {
        Evidence e = sel();
        if (e == null) return;
        if (e.filePath == null || e.sha256 == null) {
            UI.info("Nothing to verify", e.evidenceNo + " has no digital file attached, so there is no fingerprint to check.");
            return;
        }
        Path p = Paths.get(e.filePath);
        if (!Files.exists(p)) {
            CustodyDAO.add(e.id, "INTEGRITY_CHECK", Session.name(), "-", "FAILED – stored file is missing");
            Toast.error("Stored file is MISSING");
            return;
        }
        try {
            String now = Hash.sha256(p);
            boolean ok = now.equalsIgnoreCase(e.sha256);
            CustodyDAO.add(e.id, "INTEGRITY_CHECK", Session.name(), "-", ok ? "PASSED – hash matches" : "FAILED – hash mismatch (possible tampering)");
            ActivityDAO.log("INTEGRITY_CHECK", e.evidenceNo + (ok ? " passed" : " FAILED"));
            if (ok) {
                EvidenceDAO.updateStatus(e.id, "VERIFIED");
                Toast.success("Integrity verified – file is untouched");
                refresh();
            } else {
                UI.error("TAMPERING DETECTED", "The stored file's SHA-256 does not match the fingerprint recorded at capture time.\n\nRecorded: "
                        + e.sha256 + "\nCurrent:  " + now);
            }
        } catch (Exception ex) { UI.error("Verification failed", ex.getMessage()); }
    }

    // ---------- status ----------
    private void changeStatus() {
        Evidence e = sel();
        if (e == null) return;
        ChoiceDialog<String> d = new ChoiceDialog<>(e.status, "COLLECTED", "IN_ANALYSIS", "VERIFIED", "ARCHIVED");
        d.setTitle("Change status");
        d.setHeaderText("Update status of " + e.evidenceNo);
        d.setContentText("New status:");
        UI.style(d);
        d.showAndWait().ifPresent(s -> {
            EvidenceDAO.updateStatus(e.id, s);
            CustodyDAO.add(e.id, "STATUS_CHANGED", e.status, s, "Status updated");
            ActivityDAO.log("STATUS_CHANGED", e.evidenceNo + " → " + s);
            Toast.success("Status updated");
            refresh();
        });
    }

    // ---------- custody transfer ----------
    private void transfer() {
        Evidence e = sel();
        if (e == null) return;
        ComboBox<String> action = new ComboBox<>(FXCollections.observableArrayList(
                "TRANSFERRED", "SENT_TO_LAB", "RETURNED_FROM_LAB", "SUBMITTED_TO_COURT", "ARCHIVED"));
        action.setValue("TRANSFERRED");
        action.setMaxWidth(Double.MAX_VALUE);
        TextField to = UI.text("Person / department receiving the evidence");
        TextArea remarks = new TextArea();
        remarks.setPrefRowCount(3);
        remarks.setPromptText("Reason / remarks");
        VBox box = new VBox(12, UI.field("ACTION", action), UI.field("TRANSFER TO", to), UI.field("REMARKS", remarks));
        box.setPrefWidth(380);
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Transfer custody");
        d.setHeaderText("🔁  " + e.evidenceNo);
        d.getDialogPane().setContent(box);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        UI.style(d);
        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            if (to.getText().isBlank()) { Toast.error("Receiver is required"); return; }
            CustodyDAO.add(e.id, action.getValue(), Session.name(), to.getText().trim(), remarks.getText().trim());
            if (action.getValue().equals("SENT_TO_LAB")) EvidenceDAO.updateStatus(e.id, "IN_ANALYSIS");
            if (action.getValue().equals("ARCHIVED")) EvidenceDAO.updateStatus(e.id, "ARCHIVED");
            ActivityDAO.log("CUSTODY_TRANSFER", e.evidenceNo + " → " + to.getText().trim());
            Toast.success("Custody transfer recorded");
            refresh();
        });
    }

    // ---------- delete ----------
    private void delete() {
        Evidence e = sel();
        if (e == null) return;
        if (!UI.confirm("Permanently delete " + e.evidenceNo + " and its custody history?")) return;
        EvidenceDAO.delete(e.id);
        if (e.filePath != null) try { Files.deleteIfExists(Paths.get(e.filePath)); } catch (Exception ignored) {}
        ActivityDAO.log("EVIDENCE_DELETED", e.evidenceNo + " deleted");
        Toast.info("Evidence deleted");
        refresh();
    }
}
