package com.dcems.ui;

import com.dcems.dao.*;
import com.dcems.model.*;
import com.dcems.util.Csv;
import com.dcems.util.Fmt;
import com.dcems.util.Session;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** CSV exports, per-case summary table and a printable text case report. */
public class ReportsView extends ScrollPane {
    private final ComboBox<CaseFile> caseBox = new ComboBox<>(FXCollections.observableArrayList(CaseDAO.all()));
    private final TextArea preview = new TextArea();

    public ReportsView() {
        VBox page = new VBox(18);
        page.getStyleClass().add("page");
        page.getChildren().add(UI.header("Reports & Export", "Share findings with prosecutors, courts and your team"));

        // ----- export buttons -----
        HBox exports = UI.row(
                UI.primary("⬇  Evidence CSV", () -> exportCsv("evidence.csv", new String[]{"Evidence No", "Case", "Type", "Description", "Source", "Location", "File", "SHA-256", "Status", "Collected By", "Collected On"},
                        EvidenceDAO.all().stream().map(e -> new String[]{e.evidenceNo, e.caseNo, e.type, e.description, e.sourceDevice, e.location, e.fileName, e.sha256, e.status, e.collector, Fmt.dt(e.collectedAt)}).toList())),
                UI.primary("⬇  Cases CSV", () -> exportCsv("cases.csv", new String[]{"Case No", "Title", "Category", "Priority", "Status", "Assigned To", "Opened", "Evidence Items"},
                        CaseDAO.all().stream().map(c -> new String[]{c.caseNo, c.title, c.category, c.priority, c.status, c.assignee, Fmt.d(c.openedOn), String.valueOf(c.evidenceCount)}).toList())),
                UI.primary("⬇  Suspects CSV", () -> exportCsv("suspects.csv", new String[]{"Name", "Alias", "Age", "Phone", "Address", "Case", "Notes"},
                        SuspectDAO.all().stream().map(s -> new String[]{s.fullName, s.alias, String.valueOf(s.age), s.phone, s.address, s.caseNo, s.notes}).toList())),
                UI.primary("⬇  Custody Log CSV", () -> exportCsv("custody_log.csv", new String[]{"When", "Evidence", "Action", "From", "To", "Remarks", "Logged By"},
                        CustodyDAO.list(0).stream().map(c -> new String[]{Fmt.dt(c.loggedAt), c.evidenceNo, c.action, c.fromPerson, c.toPerson, c.remarks, c.loggedBy}).toList())));
        VBox exportCard = UI.card(UI.section("📤  Quick Exports"), exports);

        // ----- case report -----
        caseBox.setPromptText("Choose a case to generate its report");
        caseBox.setPrefWidth(420);
        caseBox.valueProperty().addListener((o, a, c) -> preview.setText(c == null ? "" : buildReport(c)));
        preview.setEditable(false);
        preview.setPrefRowCount(14);
        preview.setStyle("-fx-font-family:'Consolas','Menlo',monospace;-fx-font-size:12px;");
        VBox reportCard = UI.card(UI.section("📄  Case Report"),
                UI.row(caseBox, UI.ghost("💾  Save as TXT", this::saveReport)), preview);

        // ----- summary table -----
        TableView<String[]> table = UI.table();
        String[] heads = {"Case No", "Title", "Status", "Evidence", "Verified", "Suspects"};
        double[] widths = {110, 300, 110, 80, 80, 80};
        for (int i = 0; i < heads.length; i++) {
            final int idx = i;
            table.getColumns().add(i == 2 ? UI.badgeCol(heads[i], (String[] r) -> r[idx], widths[i])
                    : i == 1 ? UI.flex(UI.col(heads[i], (String[] r) -> r[idx], widths[i])) : UI.col(heads[i], (String[] r) -> r[idx], widths[i]));
        }
        table.setItems(FXCollections.observableArrayList(StatsDAO.caseSummary()));
        table.setPrefHeight(300);
        table.setMinHeight(300);
        VBox summary = UI.card(UI.section("📊  Case Summary (evidence & suspects per case)"), table);

        page.getChildren().addAll(exportCard, reportCard, summary);
        setContent(page);
        setFitToWidth(true);
    }

    private void exportCsv(String defaultName, String[] headers, List<String[]> rows) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(defaultName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;
        try {
            Csv.write(f, headers, new ArrayList<>(rows));
            ActivityDAO.log("EXPORT", defaultName + " exported");
            Toast.success("Exported " + rows.size() + " rows");
        } catch (Exception e) { UI.error("Export failed", e.getMessage()); }
    }

    private String buildReport(CaseFile c) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(64);
        sb.append(line).append("\n DIGITAL CRIME EVIDENCE MANAGEMENT SYSTEM – CASE REPORT\n").append(line).append("\n");
        sb.append("Generated : ").append(Fmt.dt(LocalDateTime.now())).append("  by ").append(Session.name()).append("\n\n");
        sb.append("Case No   : ").append(c.caseNo).append("\nTitle     : ").append(c.title)
          .append("\nCategory  : ").append(c.category).append("\nPriority  : ").append(c.priority)
          .append("\nStatus    : ").append(c.status).append("\nAssigned  : ").append(Fmt.nz(c.assignee))
          .append("\nOpened on : ").append(Fmt.d(c.openedOn)).append("\n\nDescription:\n").append(Fmt.nz(c.description)).append("\n");

        List<Suspect> sus = SuspectDAO.byCase(c.id);
        sb.append("\n--- SUSPECTS (").append(sus.size()).append(") ---\n");
        for (Suspect s : sus) sb.append(" • ").append(s.fullName).append(s.alias == null || s.alias.isBlank() ? "" : " (" + s.alias + ")")
                .append(" – ").append(Fmt.nz(s.address)).append("\n");

        List<Evidence> ev = EvidenceDAO.byCase(c.id);
        sb.append("\n--- EVIDENCE (").append(ev.size()).append(") ---\n");
        for (Evidence e : ev) {
            sb.append("\n[").append(e.evidenceNo).append("] ").append(e.type).append("  –  ").append(e.status).append("\n")
              .append("   ").append(e.description).append("\n")
              .append("   Collected ").append(Fmt.dt(e.collectedAt)).append(" by ").append(Fmt.nz(e.collector)).append("\n");
            if (e.sha256 != null) sb.append("   File: ").append(e.fileName).append("\n   SHA-256: ").append(e.sha256).append("\n");
            List<CustodyEntry> log = CustodyDAO.list(e.id);
            sb.append("   Custody events: ").append(log.size()).append("\n");
        }
        sb.append("\n").append(line).append("\n End of report\n");
        return sb.toString();
    }

    private void saveReport() {
        CaseFile c = caseBox.getValue();
        if (c == null) { Toast.error("Choose a case first"); return; }
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(c.caseNo + "_report.txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;
        try {
            Files.write(f.toPath(), preview.getText().getBytes(StandardCharsets.UTF_8));
            ActivityDAO.log("REPORT", "Report saved for " + c.caseNo);
            Toast.success("Report saved");
        } catch (Exception e) { UI.error("Could not save report", e.getMessage()); }
    }
}
