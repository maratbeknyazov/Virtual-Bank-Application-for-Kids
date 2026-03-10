package com.vbank.presentation;

import com.vbank.application.service.TaskService;
import com.vbank.application.service.UserService;
import com.vbank.domain.model.Task;
import com.vbank.domain.model.User;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DashboardControllerTest {
    // needed to initialize JavaFX toolkit
    @BeforeAll
    static void initToolkit() {
        new JFXPanel();
    }

    private DashboardController controller;
    private TaskService taskService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        controller = new DashboardController();
        taskService = Mockito.mock(TaskService.class);
        userService = Mockito.mock(UserService.class);
        // Initialize the ListView fields since FXML injection won't happen in unit
        // tests
        controller.childrenList = new ListView<>();
        controller.tasksList = new ListView<>();
        controller.goalsList = new ListView<>();
        controller.pendingTasksList = new ListView<>();
        controller.setServices(null, taskService, userService);
    }

    @Test
    void loadChildren_populatesList() {
        User child1 = new User(UUID.randomUUID(), UUID.randomUUID(), "c1", "Child One", User.Role.CHILD, "h", null,
                false, null, null);
        User child2 = new User(UUID.randomUUID(), UUID.randomUUID(), "c2", "Child Two", User.Role.CHILD, "h", null,
                false, null, null);
        when(userService.getAllChildren()).thenReturn(List.of(child1, child2));

        controller.loadChildren();
        ListView<String> list = controller.childrenList;
        assertEquals(2, list.getItems().size());
        assertTrue(list.getItems().get(0).contains("Child One"));
    }

    @Test
    void loadDataForChild_showsTasks() {
        UUID childId = UUID.randomUUID();
        Task t = new Task(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), childId,
                "do", 10, Task.Status.OPEN, "", null, null, null, null, null);
        when(taskService.getTasksForChild(childId)).thenReturn(List.of(t));

        controller.loadDataForChild(childId);
        assertEquals(1, controller.tasksList.getItems().size());
        assertTrue(controller.tasksList.getItems().get(0).contains("do"));
    }
}
