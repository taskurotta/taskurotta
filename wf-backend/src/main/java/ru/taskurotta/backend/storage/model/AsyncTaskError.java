package ru.taskurotta.backend.storage.model;

import java.util.UUID;

/**
 * User: romario
 * Date: 4/1/13
 * Time: 1:17 PM
 */
public class AsyncTaskError {

    private UUID taskId;
    private String className;
    private String message;
    private String stackTrace;

}
