package com.vbank.application.service;

import com.vbank.domain.model.Task;
import com.vbank.domain.repository.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service managing tasks and rewards.
 * <p>
 * Designed around the workflow described in the requirements: a parent creates
 * a
 * task and assigns it to a child, the child submits the task when finished, and
 * the parent approves it. Upon approval the reward is automatically deposited
 * into the child's current account via {@link BankService} and the task is
 * transitioned to the completed state.
 */
public class TaskService {
    private final Repository<Task> taskRepo;
    private final BankService bankService;

    public TaskService(Repository<Task> taskRepo, BankService bankService) {
        this.taskRepo = taskRepo;
        this.bankService = bankService;
    }

    /**
     * Parent approves a previously submitted task. The supplied PIN is checked
     * against the parent account, the task status is changed and the reward is
     * credited to the child's current account in a single operation.
     *
     * @throws SecurityException     if the parent id does not match the task or the
     *                               PIN check fails
     * @throws IllegalStateException if the task is in an inappropriate state
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
}
