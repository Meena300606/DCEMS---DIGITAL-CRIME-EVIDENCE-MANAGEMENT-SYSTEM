package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.CustodyDAO;
import com.dcems.dao.EvidenceDAO;
import com.dcems.model.CustodyEntry;
import com.dcems.model.Evidence;
import com.dcems.util.Fmt;
import com.dcems.util.Session;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Full audit trail of who handled which evidence, when and why. */
public class CustodyView extends HBox {
    private final TableView<CustodyEntry> table = UI.table();
    private final ComboBox<Evidence> filter = new ComboBox<>();
    private final ComboBox<Evidence> evBox = new ComboBox<>();
    private final ComboBox<String> action = new ComboBox<>(FXCollections.observableArrayList(
            "TRANSFERRED", "SENT_TO_LAB", "RETURNED_FROM_LAB", "SUBMITTED_TO_COURT", "ARCHIVED", "NOTE"));
    private final TextField from = UI.text("Released by");
    private final TextField to = UI.text("Received by");
    private final TextArea remarks = new TextArea();

    public CustodyView() {
        getStyleClass().add("page");
        setSpacing(20);

        Evidence all = new Evidence();
        all.id = 0; all.evidenceNo = "All evidence";
        var items = FXCollections.observableArrayList(EvidenceDAO.all());
        var withAll = FXCollections.observableArrayList(all);
        withAll.addAll(items);
        filter.setItems(withAll);
        filter.setValue(all);
        filter.setMaxWidth(320);
        filter.valueProperty().addListener((o, a, b) -> refresh());

        table.getColumns().add(UI.col("When", (CustodyEntry c) -> Fmt.dt(c.loggedAt), 145));
        table.getColumns().add(UI.col("Evidence", (CustodyEntry c) -> c.evidenceNo, 115));
        table.getColumns().add(UI.badgeCol("Action", (CustodyEntry c) -> c.action, 165));
        table.getColumns().add(UI.col("From", (CustodyEntry c) -> Fmt.nz(c.fromPerson), 105));
        table.getColumns().add(UI.col("To", (CustodyEntry c) -> Fmt.nz(c.toPerson), 105));
        table.getColumns().add(UI.flex(UI.col("Remarks", (CustodyEntry c) -> Fmt.nz(c.remarks), 160)));

        VBox left = new VBox(16, UI.row(UI.header("Chain of Custody", "An unbroken record of every hand the evidence passed through"),
                UI.spacer(), filter), table);
        HBox.setHgrow(left, Priority.ALWAYS);

        evBox.setItems(items);
        evBox.setPromptText("Select evidence");
        evBox.setMaxWidth(Double.MAX_VALUE);
        action.setValue("TRANSFERRED");
        action.setMaxWidth(Double.MAX_VALUE);
        remarks.setPrefRowCount(3);
        remarks.setPromptText("Reason / remarks");
        from.setText(Session.name());
        VBox form = UI.card(UI.section("➕  Add Custody Entry"), UI.field("EVIDENCE", evBox), UI.field("ACTION", action),
                UI.field("FROM", from), UI.field("TO", to), UI.field("REMARKS", remarks),
                UI.primary("Log Entry", this::add));
        form.setPrefWidth(330); form.setMinWidth(330); form.setMaxWidth(330);
        getChildren().addAll(left, form);
        refresh();
    }

    private void refresh() {
        Evidence e = filter.getValue();
        table.setItems(FXCollections.observableArrayList(CustodyDAO.list(e == null ? 0 : e.id)));
    }

    private void add() {
        if (evBox.getValue() == null || to.getText().isBlank()) { Toast.error("Select evidence and enter who received it"); return; }
        Evidence e = evBox.getValue();
        CustodyDAO.add(e.id, action.getValue(), from.getText().trim(), to.getText().trim(), remarks.getText().trim());
        if ("SENT_TO_LAB".equals(action.getValue())) EvidenceDAO.updateStatus(e.id, "IN_ANALYSIS");
        if ("ARCHIVED".equals(action.getValue())) EvidenceDAO.updateStatus(e.id, "ARCHIVED");
        ActivityDAO.log("CUSTODY_ENTRY", e.evidenceNo + " → " + to.getText().trim());
        Toast.success("Custody entry logged");
        to.clear(); remarks.clear(); evBox.setValue(null);
        refresh();
    }
}
