package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.CaseDAO;
import com.dcems.dao.SuspectDAO;
import com.dcems.model.CaseFile;
import com.dcems.model.Suspect;
import com.dcems.util.Fmt;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Register suspects and link them to cases. */
public class SuspectsView extends HBox {
    private final TableView<Suspect> table = UI.table();
    private final TextField search = UI.text("🔍  Search name, alias, case…");
    private final TextField name = UI.text("Full name");
    private final TextField alias = UI.text("Alias / username");
    private final TextField age = UI.text("Age");
    private final TextField phone = UI.text("Phone");
    private final TextField address = UI.text("Address / last known location");
    private final ComboBox<CaseFile> caseBox = new ComboBox<>(FXCollections.observableArrayList(CaseDAO.all()));
    private final TextArea notes = new TextArea();
    private final Label formTitle = UI.section("➕  New Suspect");
    private final Button save = UI.primary("Save Suspect", this::save);
    private final Button delete = UI.danger("Delete", this::delete);
    private Suspect editing;

    public SuspectsView() {
        getStyleClass().add("page");
        setSpacing(20);

        search.getStyleClass().add("search-box");
        search.textProperty().addListener((o, a, b) -> refresh());
        HBox top = UI.row(UI.header("Suspect Registry", "Persons of interest linked to your cases"), UI.spacer(), search);

        table.getColumns().add(UI.flex(UI.col("Name", (Suspect s) -> s.fullName, 150)));
        table.getColumns().add(UI.col("Alias", (Suspect s) -> Fmt.nz(s.alias), 110));
        table.getColumns().add(UI.col("Age", (Suspect s) -> s.age == 0 ? "" : String.valueOf(s.age), 55));
        table.getColumns().add(UI.col("Phone", (Suspect s) -> Fmt.nz(s.phone), 125));
        table.getColumns().add(UI.col("Linked Case", (Suspect s) -> Fmt.nz(s.caseNo), 125));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> { if (s != null) load(s); });
        VBox left = new VBox(16, top, table);
        HBox.setHgrow(left, Priority.ALWAYS);

        notes.setPrefRowCount(4);
        notes.setWrapText(true);
        notes.setPromptText("Behaviour, known associates, digital footprint…");
        caseBox.setPromptText("Link to a case");
        caseBox.setMaxWidth(Double.MAX_VALUE);
        HBox ap = new HBox(10, UI.field("AGE", age), UI.field("PHONE", phone));
        ap.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        VBox form = new VBox(12, formTitle, UI.field("FULL NAME", name), UI.field("ALIAS", alias), ap,
                UI.field("ADDRESS", address), UI.field("LINKED CASE", caseBox), UI.field("NOTES", notes),
                UI.row(save, UI.ghost("Clear", this::clear), delete));
        form.setPadding(new Insets(4));
        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        VBox card = UI.card(sp);
        card.setPrefWidth(340); card.setMinWidth(340); card.setMaxWidth(340);
        VBox.setVgrow(sp, Priority.ALWAYS);
        getChildren().addAll(left, card);
        clear();
        refresh();
    }

    private void refresh() { table.setItems(FXCollections.observableArrayList(SuspectDAO.search(search.getText()))); }

    private void load(Suspect s) {
        editing = s;
        formTitle.setText("✏  Edit Suspect");
        name.setText(s.fullName); alias.setText(Fmt.nz(s.alias)); age.setText(s.age == 0 ? "" : String.valueOf(s.age));
        phone.setText(Fmt.nz(s.phone)); address.setText(Fmt.nz(s.address)); notes.setText(Fmt.nz(s.notes));
        caseBox.setValue(null);
        for (CaseFile c : caseBox.getItems()) if (c.id == s.caseId) caseBox.setValue(c);
        save.setText("Update Suspect");
        delete.setDisable(false);
    }

    private void clear() {
        editing = null;
        table.getSelectionModel().clearSelection();
        formTitle.setText("➕  New Suspect");
        name.clear(); alias.clear(); age.clear(); phone.clear(); address.clear(); notes.clear(); caseBox.setValue(null);
        save.setText("Save Suspect");
        delete.setDisable(true);
    }

    private void save() {
        if (name.getText().isBlank()) { Toast.error("Name is required"); return; }
        int a = 0;
        if (!age.getText().isBlank()) {
            try { a = Integer.parseInt(age.getText().trim()); if (a < 0 || a > 120) throw new NumberFormatException(); }
            catch (NumberFormatException e) { Toast.error("Enter a valid age"); return; }
        }
        Suspect s = editing == null ? new Suspect() : editing;
        s.fullName = name.getText().trim(); s.alias = alias.getText().trim(); s.age = a; s.phone = phone.getText().trim();
        s.address = address.getText().trim(); s.notes = notes.getText();
        s.caseId = caseBox.getValue() == null ? 0 : caseBox.getValue().id;
        if (editing == null) { SuspectDAO.insert(s); ActivityDAO.log("SUSPECT_ADDED", s.fullName); Toast.success("Suspect added"); }
        else { SuspectDAO.update(s); ActivityDAO.log("SUSPECT_UPDATED", s.fullName); Toast.success("Suspect updated"); }
        clear();
        refresh();
    }

    private void delete() {
        if (editing == null || !UI.confirm("Delete suspect " + editing.fullName + "?")) return;
        SuspectDAO.delete(editing.id);
        ActivityDAO.log("SUSPECT_DELETED", editing.fullName);
        Toast.info("Suspect deleted");
        clear();
        refresh();
    }
}
