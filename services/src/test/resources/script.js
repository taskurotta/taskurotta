function filter(interruptedTasks) {
    var trackedActorId = "ru.taskurotta.another.test.ZipWorker2#1.0#task4";
    var trackedErrorMessage = "testError";
    var trackedErrorClassName = "ru.taskurotta.util.NotificationUtilsTest";

    var ArrayList = Java.type('java.util.ArrayList');
    var result = new ArrayList();

    for (i in interruptedTasks) {
        var task = interruptedTasks[i];

        var actorId = task.getActorId();
        var errorMessage = task.getErrorMessage();
        var errorClassName = task.getErrorClassName();

        if (actorId == trackedActorId && errorMessage == trackedErrorMessage && errorClassName == trackedErrorClassName) {
            result.add(actorId);
        }
    }

    return result;
}