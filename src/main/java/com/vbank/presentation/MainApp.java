package com.vbank.presentation;

import com.vbank.application.service.BankService;
import com.vbank.application.service.TaskService;
import com.vbank.application.service.UserService;
import com.vbank.domain.model.BankAccount;
import com.vbank.domain.model.Task;
import com.vbank.domain.model.User;
import com.vbank.infrastructure.persistence.JsonRepository;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entry point for the JavaFX application. Responsible for bootstrapping the
 * repositories with a small sample dataset and wiring the UI controllers to the
 * application services.
 * <p>
 * This JavaFX application serves as the main entry point, responsible for:
 * </p>
 * <ul>
 * <li>Initializing repositories and application services</li>
 * <li>Creating sample data for demonstration purposes</li>
 * <li>Loading and displaying the JavaFX dashboard interface</li>
 * <li>Wiring together the presentation, application, and infrastructure
 * layers</li>
 * </ul>
 *
 * <p>
 * The application uses a layered architecture with clear separation of
 * concerns:
 * presentation (JavaFX), application services, domain models, and
 * infrastructure (JSON persistence).
 * </p>
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public class MainApp extends Application {

    /**
     * Starts the JavaFX application.
     * <p>
     * This method initializes all components of the application:
     * repositories, services, sample data, and the UI. It sets up the
     * dependency injection and displays the main dashboard window.
     * </p>
     *
     * @param primaryStage the primary stage for the JavaFX application
     * @throws Exception if application initialization fails
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // set up repositories and services
        JsonRepository<User> userRepo = new JsonRepository<>(User.class);
        JsonRepository<BankAccount> accountRepo = new JsonRepository<>(BankAccount.class);
        JsonRepository<Task> taskRepo = new JsonRepository<>(Task.class);

        BankService bankService = new BankService(accountRepo, userRepo);
        TaskService taskService = new TaskService(taskRepo, bankService);
        UserService userService = new UserService(userRepo);

        // create sample parent/child/task if none exist
        if (userRepo.findAll().isEmpty()) {
            UUID orgId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            String parentPin = "1234";
            String hash = BCrypt.hashpw(parentPin, BCrypt.gensalt());
            User parent = new User(parentId,
                    orgId,
                    "parent",
                    "Parent User",
                    User.Role.PARENT,
                    hash,
                    null,
                    false,
                    Instant.now(),
                    Instant.now());
            userRepo.save(parent);

            UUID childId = UUID.randomUUID();
            User child = new User(childId,
                    orgId,
                    "child",
                    "Child User",
                    User.Role.CHILD,
                    BCrypt.hashpw("0000", BCrypt.gensalt()),
                    10,
                    false,
                    Instant.now(),
                    Instant.now());
            userRepo.save(child);

            BankAccount current = new BankAccount(UUID.randomUUID(), orgId, childId,
                    BankAccount.AccountType.CURRENT, 0, "USD", "C-001", true,
                    Instant.now(), Instant.now());
            accountRepo.save(current);

            // add a pending task
            Task task = new Task(UUID.randomUUID(),
                    orgId,
                    parentId,
                    childId,
                    "Do homework",
                    100,
                    Task.Status.SUBMITTED,
                    "school",
                    LocalDate.now().plusDays(2),
                    Instant.now(),
                    null,
                    Instant.now(),
                    Instant.now());
            taskRepo.save(task);
        }

        // load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
        Parent root = loader.load();

        DashboardController controller = loader.getController();
        controller.setServices(bankService, taskService, userService);
        controller.loadChildren();
        // for demo, show first child in child tab if available
        userService.getAllChildren().stream().findFirst().ifPresent(user -> controller.loadDataForChild(user.getId()));

        primaryStage.setTitle("Virtual Bank Dashboard");
        primaryStage.setScene(new Scene(root, 600, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
