var consoleServices = angular.module("console.services", ['ngResource']);

consoleServices.factory("$$data", function ($resource, $http) {

    var resultService = {
        getQueueContent: function (queueName, pageNumber, pageSize) {
            return $http.get('/rest/console/queue/' + encodeURIComponent(queueName) + '?pageNum=' + pageNumber + '&pageSize=' + pageSize);
        },
        getQueueList: function (pageNumber, pageSize) {
            return $http.get('/rest/console/queues/?pageNum=' + pageNumber + '&pageSize=' + pageSize);
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
        blockActor: function(actorId) {
            return $http.post('/rest/console/actor/block/', actorId);
        },
        unblockActor: function(actorId) {
            return $http.post('/rest/console/actor/unblock/', actorId);
        },
        listActors: function() {
            return $http.get('/rest/console/actor/list/');
        },
        getMetricsOptions: function() {
            return $http.get('/rest/console/metrics/options/');
        }

    };

    return resultService;

});

consoleServices.factory('$$timeUtil', ["$timeout",
    function ($timeout) {
        var _intervals = {}, _intervalUID = 1;

        return {
            setInterval: function (operation, interval, $scope) {
                var _internalId = _intervalUID++;

                _intervals[ _internalId ] = $timeout(function intervalOperation() {
                    operation($scope || undefined);
                    _intervals[ _internalId ] = $timeout(intervalOperation, interval);
                }, interval);

                $scope.$on('$destroy', function (e) {//cancel interval on change view
                    $timeout.cancel(_intervals[_internalId]);
                });

                return _internalId;
            },

            clearInterval: function (id) {
                return $timeout.cancel(_intervals[ id ]);
            }
        }
    }
]);
