var consoleServices = angular.module("console.services", ['ngResource']);

consoleServices.factory("$$data", function($resource, $http) {

    //TODO: use benefits(if there are any) of a $resource service?
    var resultService = {
        getQueueContent: function(queueName) {
            return $http.get('/rest/console/queue/'+encodeURIComponent(queueName));
        },
        getQueueList: function() {
            return $http.get('/rest/console/queues/');
        },
        getTask: function(taskId) {
            return $http.get('/rest/console/task/' + encodeURIComponent(taskId));
        },
        getProcess: function(processId) {
            return $http.get('/rest/console/process/' + encodeURIComponent(processId));
        },
        getProcessTasks: function(processId){
            return $http.get('/rest/console/tasks/process/' + encodeURIComponent(processId));
        }
    };

    return resultService;

});

consoleServices.factory('$$timeUtil', ["$timeout",
    function ($timeout) {
        var _intervals = {}, _intervalUID = 1;

        return {
            setInterval: function(operation, interval, $scope) {
                var _internalId = _intervalUID++;

                _intervals[ _internalId ] = $timeout(function intervalOperation(){
                    operation( $scope || undefined );
                    _intervals[ _internalId ] = $timeout(intervalOperation, interval);
                }, interval);

                $scope.$on('$destroy', function(e) {//cancel interval on change view
                    this.clearInterval(_internalId);
                });

                return _internalId;
            },

            clearInterval: function(id) {
                return $timeout.cancel( _intervals[ id ] );
            }
        }
    }
]);
