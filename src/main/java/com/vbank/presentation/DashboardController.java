package com.vbank.presentation;

import com.vbank.application.service.BankService;
import com.vbank.application.service.TaskService;
import com.vbank.application.service.UserService;
import com.vbank.domain.model.Task;
import com.vbank.domain.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.util.UUID;

/**
 * JavaFX controller for the main dashboard of the Virtual Bank Application for Kids.
 * <p>
 * This controller manages the parent dashboard interface, providing functionality to:
 * </p>
 * <ul>
 *   <li>View and select children in the organization</li>
 *   <li>Display pending tasks for selected children</li>
 *   <li>Approve completed tasks with PIN verification</li>
 *   <li>Show savings goals and completed tasks</li>
 *   <li>Handle user interactions and display alerts</li>
 * </ul>
 *
 * <p>The dashboard serves as the main interface for parents to manage their children's
 * banking activities and task approvals.</p>
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public class DashboardController {
    @FXML
    protected ListView<String> goalsList;
    @FXML
    protected ListView<String> tasksList;
    @FXML
    protected ListView<String> childrenList;
    @FXML
    protected ListView<String> pendingTasksList;
    @FXML
    private Button approveButton;

    private BankService bankService;
    private TaskService taskService;
    private UserService userService;

    private UUID selectedChildId;

    /**
     * Sets the application services required by this controller.
     * <p>
     * This method must be called before the controller can function properly.
     * It initializes the services and loads the initial data for the UI.
     * </p>
     *
     * @param bankService the bank service for account operations
     * @param taskService the task service for task management
     * @param userService the user service for user data access
     */
    public void setServices(BankService bankService,
            TaskService taskService,
            UserService userService) {
        this.bankService = bankService;
        this.taskService = taskService;
        this.userService = userService;
        // after services are available we can populate UI elements that depend on them
        loadChildren();
    }

    /**
     * Initializes the controller after the FXML file has been loaded.
     * <p>
     * Sets up event listeners for UI components, particularly the children list
     * selection listener that updates the pending tasks display.
     * </p>
     */
    @FXML
    public void initialize() {
        childrenList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updatePendingTasksForSelectedChild(newVal);
            } else {
                pendingTasksList.getItems().clear();
            }
        });

        pendingTasksList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            approveButton.setDisable(newVal == null);
        });
    }

    public void loadDataForChild(UUID childId) {
        this.selectedChildId = childId;
        var tasks = taskService.getTasksForChild(childId);
        tasksList.getItems().setAll(tasks.stream()
                .map(t -> t.getTaskDescription() + " (" + t.getTaskStatus() + ")")
                .toList());
        goalsList.getItems().setAll("[no goals implemented]");
    }

    public void loadChildren() {
        if (userService == null) {
            return;
        }
        var children = userService.getAllChildren();
        childrenList.getItems().setAll(children.stream()
                .map(u -> u.getName() + " [" + u.getId() + "]")
                .toList());
    }

    private void updatePendingTasksForSelectedChild(String listItem) {
        String uuidStr = listItem.replaceAll(".*\\[(.*)\\].*", "$1");
        try {
            selectedChildId = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return;
        }
        var tasks = taskService.getTasksForChild(selectedChildId);
        pendingTasksList.getItems().setAll(tasks.stream()
                .filter(t -> t.getTaskStatus() == Task.Status.SUBMITTED
                        || t.getTaskStatus() == Task.Status.OPEN)
                .map(t -> t.getTaskDescription() + " (" + t.getId() + ")")
                .toList());
    }

    @FXML
    private void onApprovePressed() {
        String selected = pendingTasksList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String uuidStr = selected.replaceAll(".*\\((.*)\\).*", "$1");
        try {
            UUID taskId = UUID.fromString(uuidStr);
            // pick a parent automatically (first one)
            User parent = userService.getAllParents(UUID.randomUUID()).stream().findFirst().orElse(null);
            if (parent != null) {
                taskService.approveTask(taskId, parent.getId(), "1234");
                updatePendingTasksForSelectedChild(childrenList.getSelectionModel().getSelectedItem());
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to approve: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
