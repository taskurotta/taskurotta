angular.module("console.interrupted.services", [])
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
.service ("tskBrokenProcessesActions", ['$http', '$log', function ($http, $log) {
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

    return {
        submitRestart: function(command) {
            $log.log("Try to submit restart task command: ", command);
            return $http.post("/rest/console/process/tasks/interrupted/restart", command);
        },
        restartTask: function (itdTask) {
            var command = taskAsActionCommand(itdTask);
            return this.submitRestart(command);
        },
        restartGroup: function (itdTaskGroup) {
            var command = groupAsActionCommand(itdTaskGroup);
            return this.submitRestart(command);
        }
    };

}]);