var consoleControllers = angular.module("console.controllers", ['console.services']);

consoleControllers.controller("bodyController", function($rootScope, $scope, $location, $log) {

    $scope.isActiveTab = function(rootPath) {
        var result = "";
        if($location.url().indexOf(rootPath) == 0) result =  "active";
        return result;
    };

});


consoleControllers.controller("homeController", function($scope) {
});

consoleControllers.controller("actorsController", function($scope, $$data, $timeout) {

});

consoleControllers.controller("queueListController", function($scope, $$data, $$timeUtil, $log) {

    $scope.feedback = "";
    $scope.refreshRate = 0;

    $scope.queues = [];
    $scope.totalTasks = function(){
        var result = 0;
        for(var i = 0; i<$scope.queues.length; i++) {
            result = result + $scope.queues[i].count;
        }
        return result;
    };

    //Updates queues states  by polling REST resource
    $scope.update = function(){
        $$data.getQueueList().then(function(value) {
            $scope.queues =  angular.fromJson(value.data);
            $log.info("queueListController: successfully updated queue state");
        }, function(errReason) {
            $scope.feedback = errReason;
            $log.error("queueListController: queue state update failed: " + errReason);
        });

    };

    //Auto refresh feature. re triggers auto refreshing on refresh rate changes
    var currentRefreshIntervalId = -1;
    $scope.$watch(function(){return $scope.refreshRate;}, function(value) {
        if(currentRefreshIntervalId > 0) {
            $$timeUtil.clearInterval(currentRefreshIntervalId);
        }
        if($scope.refreshRate > 0) {
            currentRefreshIntervalId = $$timeUtil.setInterval($scope.update, $scope.refreshRate*1000, $scope);//Start autoUpdate
        }
    }, true);


    //Initialization:
    $scope.update();

});

consoleControllers.controller("queueContentController", function($scope, $$data, $$timeUtil, $log, $routeParams) {

    $scope.feedback = "";
    $scope.refreshRate = 0;

    $scope.queueItems = [];
    $scope.queueName = $routeParams.queueName;



    //Updates queue items by polling REST resource
    $scope.update = function(){
        $$data.getQueueContent($scope.queueName).then(function(value) {
            $scope.queueItems =  angular.fromJson(value.data);
            $log.info("queueContentController: successfully updated queue content");
        }, function(errReason) {
            $scope.feedback = errReason;
            $log.error("queueContentController: queue content update failed: " + errReason);
        });

    };

    //Auto refresh feature. re triggers auto refreshing on refresh rate changes
    var currentRefreshIntervalId = -1;
    $scope.$watch(function(){return $scope.refreshRate;}, function(value) {
        if(currentRefreshIntervalId > 0) {
            $$timeUtil.clearInterval(currentRefreshIntervalId);
        }
        if($scope.refreshRate > 0) {
            currentRefreshIntervalId = $$timeUtil.setInterval($scope.update, $scope.refreshRate*1000, $scope);//Start autoUpdate
        }
    }, true);


    //Initialization:
    $scope.update();

});

consoleControllers.controller("processesController", function($scope) {

});

consoleControllers.controller("taskListController", function($scope) {

});

consoleControllers.controller("taskController", function($scope) {

});

consoleControllers.controller("aboutController", function($scope) {

});
