angular.module("console.broken.process.services", [])
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
.service ("tskBrokenProcessesActions", ['$http', function ($http) {
    return {
        restartProcesses: function(restartCommand) {
            return $http.post("/rest/console/process/broken/restart", restartCommand);
        }
//        restartProcessCollection: function(groupCommand) {
//            return $http.post("/rest/console/process/broken/group_restart", groupCommand);
//        }
    };

}]);