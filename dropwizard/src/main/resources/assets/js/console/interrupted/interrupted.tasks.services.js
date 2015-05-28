angular.module("console.interrupted.services", ['ui.bootstrap.modal'])
.service("tskBpTextProvider", ['$http', function($http){
    return {
        getGroupLabel: function(name) {
            var result = name;
            if ('starter' == name) {
                result = "Process start task type";
            } else if ('exception' == name) {
                result = 'Fail cause exception class';
            } else if ('actor' == name) {
                result = 'Failing actor ID';
            }
            return result;
        },
        getGroupShortLabel: function(name) {
            var result = name;
            if ('starter' == name) {
                result = 'P';
            } else if ('exception' == name) {
                result = 'E';
            } else if ('actor' == name) {
                result = 'A';
            }
            return result;
        }
    };
}])
.service ("tskBrokenProcessesActions", ['$http', '$log', '$modal', function ($http, $log, $modal) {
    var asTaskIdentifier = function (obj) {
        var result = {};
        if (!!obj) {
            result["taskId"] = obj.taskId;
            result["processId"] = obj.processId;
        }
        return result;
    };

    var taskAsActionCommand = function(itdTask) {
        return {
            "restartIds": [ asTaskIdentifier(itdTask)]
        };
    };

    var groupAsActionCommand = function(group) {
        var identifiersArray = [];
        if (!!group && !!group.tasks && group.tasks.length>0) {
            for(var i = 0; i<group.tasks.length ;i++) {
                identifiersArray[i] = asTaskIdentifier(group.tasks[i]);
            }
        }
        return {"restartIds": identifiersArray};
    };

    var createMessageUrl = function(type, processId, taskId) {
        return '/rest/console/process/tasks/interrupted/' + type + '?processId=' + processId + '&taskId=' + taskId;
    };

    return {
        submitRestart: function(command) {
            $log.log("Try to submit restart task command: ", command);
            return $http.post("/rest/console/process/tasks/interrupted/restart", command);
        },
        restartTask: function (itdTask) {
            $log.log("Try to submit restart task:", itdTask);
            return $http.post("/rest/console/process/tasks/interrupted/restart/task", itdTask);
        },
        restartGroup: function (command) {
            $log.log("Try to submit restart group with command: ", command);
            return $http.post("/rest/console/process/tasks/interrupted/restart/group", command);
        },
        abortGroup: function(command) {
            $log.log("Try to abort processes group with command: ", command);
            return $http.post("/rest/console/process/tasks/interrupted/abort/group", command);
        },
        showModalMessage: function(type, processId, taskId) {
            var url = createMessageUrl(type, processId, taskId);
            $http.get(url).then(function (success) {
                $log.log("Result is", success);
                var modalInstance = $modal.open({
                    templateUrl: '/partials/view/modal/modal_msg.html',
                    windowClass: 'stack-trace',
                    controller: function ($scope) {
                        $scope.status = success.data;
                    }
                });

                modalInstance.result.then(function(okMess) {
                    //do nothing
                }, function(cancelMsg) {
                    //do nothing
                });

            }, function(error) {
                $log.error("Cannot show message type[" + type + "]for processId[" + processId + "], taskId[" + taskId + "]");
            });
        }
    };

}]);