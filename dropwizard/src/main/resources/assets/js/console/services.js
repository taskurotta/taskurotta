angular.module("console.services", ['ngResource', 'ngCookies', 'console.util.services'])

.factory("tskActors", function ($resource, $http) {
    var resultService = {
        setActorBlocked: function(actorId, isBlocked) {
            var url = '/rest/console/actor/unblock/';
            if (isBlocked) {
                url = '/rest/console/actor/block/';
            }
            return $http.post(url, actorId);
        },
        listActors: function(pageNumber, pageSize) {
            return $http.get('/rest/console/actor/list/?pageNum=' + pageNumber + '&pageSize=' + pageSize);
        },
        listMetrics: function() {
            return $http.get('/rest/console/actor/metrics/compare');
        },
        getMetricsData: function(actorIds, metricNames) {
            var command = {};
            command["actorIds"] = actorIds;
            command["metrics"] = metricNames;
            return $http.post('/rest/console/actor/metrics/compare', command);
        }
    };

    return resultService;
})

.factory("$$data", function ($resource, $http) {

    var resultService = {
        getQueueContent: function (queueName, pageNumber, pageSize) {
            return $http.get('/rest/console/queue/' + encodeURIComponent(queueName) + '?pageNum=' + pageNumber + '&pageSize=' + pageSize);
        },
        getQueueList: function (pageNumber, pageSize, filter) {
            return $http.get('/rest/console/queues/?pageNum=' + pageNumber + '&pageSize=' + pageSize + '&filter=' + encodeURIComponent(filter));
        },
        getTask: function (taskId, processId) {
            return $http.get('/rest/console/task?processId=' + encodeURIComponent(processId) + '&taskId=' + encodeURIComponent(taskId));
        },
        getTaskTree: function (taskId, processId) {
            return $http.get('/rest/console/tree/task/' + encodeURIComponent(processId) + '/' + encodeURIComponent(taskId));
        },
        listTasks: function (pageNumber, pageSize) {
            return $http.get('/rest/console/tasks/?pageNum=' + pageNumber + '&pageSize=' + pageSize);
        },
        getProcess: function (processId) {
            return $http.get('/rest/console/process/' + encodeURIComponent(processId));
        },
        findTasks: function (processId, taskId) {
            return $http.get('/rest/console/task/search?processId=' + encodeURIComponent(processId) + "&taskId=" + encodeURIComponent(taskId));
        },
        findProcess: function (processId, customId) {
            return $http.get('/rest/console/process/search?processId=' + encodeURIComponent(processId) + "&customId=" + encodeURIComponent(customId));
        },
        getProcessTree: function (processId, startTaskId) {
            return $http.get('/rest/console/tree/process/' + encodeURIComponent(processId) + '/' + encodeURIComponent(startTaskId));
        },
        getProcessesList: function (pageNumber, pageSize) {
            return $http.get('/rest/console/processes/?pageNum=' + pageNumber + '&pageSize=' + pageSize);
        },
        getProcessTasks: function (processId) {
            return $http.get('/rest/console/tasks/process/' + encodeURIComponent(processId));
        },
        getHoveringQueues: function (periodSize) {
            return $http.get('/rest/console/hoveringQueues/?periodSize=' + periodSize);
        },
        getRepeatedTasks: function (iterationCount) {
            return $http.get('/rest/console/repeatedTasks/?iterationCount=' + iterationCount);
        },
        getTaskDecision: function(taskId, processId) {
            return $http.get('/rest/console/task/decision/' + encodeURIComponent(processId) + '/' + encodeURIComponent(taskId));
        },
        getMetricsOptions: function() {
            return $http.get('/rest/console/metrics/options/');
        },
        getQueueRealSize: function(queueName) {
            return $http.get('/rest/console/queues/' + encodeURIComponent(queueName) + "/size");
        }

    };

    return resultService;

});
