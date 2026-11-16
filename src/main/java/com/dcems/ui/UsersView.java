package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.UserDAO;
import com.dcems.db.Database;
import com.dcems.model.User;
import com.dcems.util.Session;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Admin-only: manage investigator accounts. */
public class UsersView extends HBox {
    private final TableView<User> table = UI.table();
    private final TextField username = UI.text("Login username");
    private final TextField fullName = UI.text("Full name");
    private final PasswordField password = new PasswordField();
    private final ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "INVESTIGATOR", "ANALYST"));

    public UsersView() {
        getStyleClass().add("page");
        setSpacing(20);

        table.getColumns().add(UI.col("Name", (User u) -> u.fullName, 180));
        table.getColumns().add(UI.col("Username", (User u) -> u.username, 130));
        table.getColumns().add(UI.badgeCol("Role", (User u) -> u.role, 120));
        Button del = UI.danger("🗑  Delete selected", this::delete);
        VBox left = new VBox(16, UI.header("User Management", "Control who can access the evidence system"), table, UI.row(del));
        HBox.setHgrow(left, Priority.ALWAYS);

        password.setPromptText("Password (min 6 characters)");
        role.setValue("INVESTIGATOR");
        role.setMaxWidth(Double.MAX_VALUE);
        VBox form = UI.card(UI.section("➕  New User"), UI.field("FULL NAME", fullName), UI.field("USERNAME", username),
                UI.field("PASSWORD", password), UI.field("ROLE", role), UI.primary("Create User", this::add));
        form.setPrefWidth(320); form.setMinWidth(320); form.setMaxWidth(320);
        getChildren().addAll(left, form);
        refresh();
    }

    private void refresh() { table.setItems(FXCollections.observableArrayList(UserDAO.all())); }

    private void add() {
        if (fullName.getText().isBlank() || username.getText().isBlank() || password.getText().length() < 6) {
            Toast.error("Fill all fields (password min 6 chars)");
            return;
        }
        try {
            UserDAO.add(username.getText().trim(), fullName.getText().trim(), password.getText(), role.getValue());
        } catch (Database.DbException e) { Toast.error("Username already exists"); return; }
        ActivityDAO.log("USER_CREATED", username.getText().trim() + " (" + role.getValue() + ")");
        Toast.success("User created");
        fullName.clear(); username.clear(); password.clear();
        refresh();
    }

    private void delete() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { Toast.error("Select a user first"); return; }
        if (u.id == Session.user.id) { Toast.error("You cannot delete your own account"); return; }
        if (!UI.confirm("Delete user " + u.username + "?")) return;
        UserDAO.delete(u.id);
        ActivityDAO.log("USER_DELETED", u.username);
        Toast.info("User deleted");
        refresh();
    }
}
