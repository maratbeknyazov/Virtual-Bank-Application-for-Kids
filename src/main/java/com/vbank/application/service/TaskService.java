package com.vbank.application.service;

import com.vbank.domain.model.Task;
import com.vbank.domain.repository.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service managing tasks and rewards in the Virtual Bank
 * Application for Kids.
 * <p>
 * This service handles the complete task lifecycle: creation by parents,
 * submission by children,
 * approval by parents, and automatic reward distribution. It coordinates with
 * the BankService
 * to deposit rewards into children's accounts upon task completion.
 * </p>
 *
 * <p>
 * The service supports the following workflow:
 * </p>
 * <ol>
 * <li>Parent creates a task with reward amount</li>
 * <li>Child submits the completed task</li>
 * <li>Parent approves the task with PIN verification</li>
 * <li>Reward is automatically deposited to child's current account</li>
 * <li>Task status is updated to COMPLETED</li>
 * </ol>
 *
 * @author Virtual Bank Team
 * @version 1.0
 * @since 1.0
 */
public class TaskService {
    private final Repository<Task> taskRepo;
    private final BankService bankService;

    /**
     * Constructs a new TaskService with the required dependencies.
     *
     * @param taskRepo    the repository for task operations
     * @param bankService the bank service for reward deposits
     * @throws IllegalArgumentException if either parameter is null
     */
    public TaskService(Repository<Task> taskRepo, BankService bankService) {
        if (taskRepo == null) {
            throw new IllegalArgumentException("taskRepo cannot be null");
        }
        if (bankService == null) {
            throw new IllegalArgumentException("bankService cannot be null");
        }
        this.taskRepo = taskRepo;
        this.bankService = bankService;
    }

    /**
     * Parent approves a previously submitted task. The supplied PIN is checked
     * against the parent account, the task status is changed and the reward is
     * credited to the child's current account in a single operation.
     *
     * @param taskId    the unique identifier of the task to approve
     * @param parentId  the ID of the parent approving the task
     * @param parentPin the parent's PIN for authorization
     * @throws SecurityException        if the parent id does not match the task or
     *                                  the PIN check fails
     * @throws IllegalStateException    if the task is in an inappropriate state
     * @throws IllegalArgumentException if the task is not found
     */
    public void approveTask(UUID taskId, UUID parentId, String parentPin) {
        Task t = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("task not found"));

        if (!t.getParentId().equals(parentId)) {
            throw new SecurityException("only the assigned parent may approve this task");
        }

        if (!bankService.checkPin(parentId, parentPin)) {
            throw new SecurityException("invalid PIN");
        }

        // allow approving either OPEN or SUBMITTED; child may directly ask
        // parent to approve without submitting first
        if (t.getTaskStatus() == Task.Status.OPEN || t.getTaskStatus() == Task.Status.SUBMITTED) {
            t.approve(Instant.now());
        } else {
            throw new IllegalStateException("task cannot be approved in status " + t.getTaskStatus());
        }

        // award reward and complete the task
        bankService.depositToCurrent(t.getChildId(), t.getRewardAmount());
        t.complete(Instant.now());

        taskRepo.save(t);
    }

    /**
     * Returns all tasks assigned to a particular child. Useful for rendering
     * a child's dashboard.
     */
    public java.util.List<com.vbank.domain.model.Task> getTasksForChild(UUID childId) {
        return taskRepo.findAll().stream()
                .filter(t -> t.getChildId().equals(childId))
                .toList();
    }

    /**
     * Returns all tasks created by a particular parent. This allows the parent
     * dashboard to show pending approvals.
     */
    public java.util.List<com.vbank.domain.model.Task> getTasksForParent(UUID parentId) {
        return taskRepo.findAll().stream()
                .filter(t -> t.getParentId().equals(parentId))
                .toList();
    }
}
