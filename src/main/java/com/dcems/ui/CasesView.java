package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.CaseDAO;
import com.dcems.dao.UserDAO;
import com.dcems.model.CaseFile;
import com.dcems.model.User;
import com.dcems.util.Fmt;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

/** Create, search, edit and delete investigation cases. */
public class CasesView extends HBox {
    public static final String[] CATEGORIES = {"Cyber Fraud", "Identity Theft", "Data Breach", "Ransomware", "Hacking",
            "Financial Crime", "Online Harassment", "Child Safety", "Intellectual Property", "Other"};

    private final TableView<CaseFile> table = UI.table();
    private final TextField search = UI.text("🔍  Search cases…");
    private final TextField caseNo = new TextField();
    private final TextField title = UI.text("Short case title");
    private final ComboBox<String> category = new ComboBox<>(FXCollections.observableArrayList(CATEGORIES));
    private final ComboBox<String> priority = new ComboBox<>(FXCollections.observableArrayList("LOW", "MEDIUM", "HIGH", "CRITICAL"));
    private final ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("OPEN", "INVESTIGATING", "CLOSED"));
    private final ComboBox<User> assignee = new ComboBox<>(FXCollections.observableArrayList(UserDAO.all()));
    private final DatePicker opened = new DatePicker(LocalDate.now());
    private final TextArea desc = new TextArea();
    private final Label formTitle = UI.section("➕  New Case");
    private final Button save = UI.primary("Save Case", this::save);
    private final Button delete = UI.danger("Delete", this::delete);
    private CaseFile editing;

    public CasesView() {
        getStyleClass().add("page");
        setSpacing(20);

        // ----- left: list -----
        search.getStyleClass().add("search-box");
        search.textProperty().addListener((o, a, b) -> refresh());
        HBox top = UI.row(UI.header("Case Management", "Open, track and close investigations"), UI.spacer(), search);

        table.getColumns().add(UI.col("Case No", (CaseFile c) -> c.caseNo, 125));
        table.getColumns().add(UI.flex(UI.col("Title", (CaseFile c) -> c.title, 200)));
        table.getColumns().add(UI.badgeCol("Priority", (CaseFile c) -> c.priority, 95));
        table.getColumns().add(UI.badgeCol("Status", (CaseFile c) -> c.status, 125));
        table.getColumns().add(UI.col("Evidence", (CaseFile c) -> String.valueOf(c.evidenceCount), 75));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> { if (c != null) load(c); });

        VBox left = new VBox(16, top, table);
        HBox.setHgrow(left, Priority.ALWAYS);

        // ----- right: form -----
        caseNo.setEditable(false);
        desc.setPromptText("What happened? Key details of the case…");
        desc.setPrefRowCount(4);
        desc.setWrapText(true);
        category.setPromptText("Select category");
        category.setMaxWidth(Double.MAX_VALUE);
        priority.setMaxWidth(Double.MAX_VALUE);
        status.setMaxWidth(Double.MAX_VALUE);
        assignee.setMaxWidth(Double.MAX_VALUE);
        assignee.setPromptText("Select officer");
        opened.setMaxWidth(Double.MAX_VALUE);

        HBox pr = new HBox(10, UI.field("PRIORITY", priority), UI.field("STATUS", status));
        pr.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        HBox buttons = UI.row(save, UI.ghost("Clear", this::clear), delete);
        VBox form = new VBox(12, formTitle,
                UI.field("CASE NUMBER", caseNo), UI.field("TITLE", title), UI.field("CATEGORY", category), pr,
                UI.field("ASSIGNED TO", assignee), UI.field("DATE OPENED", opened), UI.field("DESCRIPTION", desc), buttons);
        form.setPadding(new Insets(4));
        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        VBox card = UI.card(sp);
        card.setPrefWidth(360);
        card.setMinWidth(360);
        card.setMaxWidth(360);
        VBox.setVgrow(sp, Priority.ALWAYS);

        getChildren().addAll(left, card);
        clear();
        refresh();
    }

    private void refresh() { table.setItems(FXCollections.observableArrayList(CaseDAO.search(search.getText()))); }

    private void load(CaseFile c) {
        editing = c;
        formTitle.setText("✏  Edit Case");
        caseNo.setText(c.caseNo); title.setText(c.title); category.setValue(c.category);
        priority.setValue(c.priority); status.setValue(c.status); opened.setValue(c.openedOn);
        desc.setText(Fmt.nz(c.description));
        assignee.getSelectionModel().clearSelection();
        for (User u : assignee.getItems()) if (u.id == c.assignedTo) assignee.setValue(u);
        save.setText("Update Case");
        delete.setDisable(false);
    }

    private void clear() {
        editing = null;
        table.getSelectionModel().clearSelection();
        formTitle.setText("➕  New Case");
        caseNo.setText(CaseDAO.nextCaseNo());
        title.clear(); desc.clear(); category.setValue(null);
        priority.setValue("MEDIUM"); status.setValue("OPEN"); assignee.setValue(null); opened.setValue(LocalDate.now());
        save.setText("Save Case");
        delete.setDisable(true);
    }

    private void save() {
        if (title.getText().isBlank() || category.getValue() == null) {
            Toast.error("Title and category are required");
            return;
        }
        CaseFile c = editing == null ? new CaseFile() : editing;
        c.caseNo = caseNo.getText(); c.title = title.getText().trim(); c.description = desc.getText();
        c.category = category.getValue(); c.priority = priority.getValue(); c.status = status.getValue();
        c.assignedTo = assignee.getValue() == null ? 0 : assignee.getValue().id;
        c.openedOn = opened.getValue() == null ? LocalDate.now() : opened.getValue();
        if (editing == null) { CaseDAO.insert(c); ActivityDAO.log("CASE_CREATED", c.caseNo + " – " + c.title); Toast.success("Case created"); }
        else { CaseDAO.update(c); ActivityDAO.log("CASE_UPDATED", c.caseNo + " updated"); Toast.success("Case updated"); }
        clear();
        refresh();
    }

    private void delete() {
        if (editing == null) return;
        if (!UI.confirm("Delete " + editing.caseNo + "?\nAll evidence and custody records of this case will be deleted too.")) return;
        CaseDAO.delete(editing.id);
        ActivityDAO.log("CASE_DELETED", editing.caseNo + " deleted");
        Toast.info("Case deleted");
        clear();
        refresh();
    }
}
