var consoleControllers = angular.module("console.controllers", ['console.services']);

consoleControllers.controller("rootController", function ($rootScope, $scope, $location, $log, $window) {

    $scope.isActiveTab = function (rootPath) {
        var result = "";
        if ($location.url().indexOf(rootPath) == 0) result = "active";
        return result;
    };

    $scope.encodeURI = function (value) {
        return encodeURIComponent(value);
    };

    $scope.back = function () {
        $window.history.back();
    };
});

consoleControllers.controller("queueListController", function ($scope, $$data, $$timeUtil, $log) {

    $scope.feedback = "";

    //Init paging object
    $scope.queuesPage = {
        pageSize: 5,
        pageNumber: 1,
        totalCount: 0,
        items: []
    };

    $scope.totalTasks = function () {
        var result = 0;
        for (var i = 0; i < $scope.queuesPage.items.length; i++) {
            result = result + $scope.queuesPage.items[i].count;
        }
        return result;
    };

    //Updates queues states  by polling REST resource
    $scope.update = function () {

        $$data.getQueueList($scope.queuesPage.pageNumber, $scope.queuesPage.pageSize).then(function (value) {
            $scope.queuesPage = angular.fromJson(value.data || {});
            $log.info("queueListController: successfully updated queue state");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("queueListController: queue state update failed: " + errReason);
        });

    };

    //Initialization:
    $scope.update();

});

consoleControllers.controller("queueCardController", function ($scope, $$data, $$timeUtil, $log, $routeParams) {

    $scope.feedback = "";

    $scope.queueItems = [];
    $scope.queueName = $routeParams.queueName;


    //Updates queue items by polling REST resource
    $scope.update = function () {
        $$data.getQueueContent($scope.queueName).then(function (value) {
            $scope.queueItems = angular.fromJson(value.data || {});
            $log.info("queueContentController: successfully updated queue content");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("queueContentController: queue content update failed: " + errReason);
        });

    };

    //Initialization:
    $scope.update();

});

consoleControllers.controller("processListController", function ($scope) {

});

consoleControllers.controller("profilesController", function ($scope, $$data, $log) {
    $scope.feedback = "";
    $scope.profiles = [];

    //Updates profiles by polling REST resource
    $scope.update = function () {
        $$data.getProfiles().then(function (value) {
            $scope.profiles = angular.fromJson(value.data || {});
            $log.info("profilesController: successfully updated profiles");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("profilesController: profiles update failed: " + errReason);
        });

    };

    //Initialization:
    $scope.update();

});

consoleControllers.controller("processCardController", function ($scope, $$data, $$timeUtil, $log, $routeParams) {
    $scope.process = {};
    $scope.feedback = "";
    $scope.update = function () {
        $$data.getProcess($routeParams.processId).then(function (value) {
            $scope.process = angular.fromJson(value.data || {});
            $log.info("processCardController: successfully updated process[" + $routeParams.processId + "] content");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("processCardController: process[" + $routeParams.id + "] update failed: " + errReason);
        });
    };

    $scope.update();
});

consoleControllers.controller("processSearchController", function ($scope) {

});

consoleControllers.controller("taskListController", function ($scope) {

});

consoleControllers.controller("taskCardController", function ($scope, $$data, $routeParams, $log) {
    $scope.task = {};
    $scope.feedback = "";
    $scope.update = function () {
        $$data.getTask($routeParams.id).then(function (value) {
            $scope.task = angular.fromJson(value.data || {});
            $log.info("taskController: successfully updated task[" + $routeParams.id + "] content");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("taskController: task[" + $routeParams.id + "] update failed: " + errReason);
        });
    };

    $scope.update();

});

consoleControllers.controller("taskSearchController", function ($scope, $routeParams, $$data, $log) {
    $scope.taskId = $routeParams.taskId;
    $scope.processId = $routeParams.processId;
    $scope.type = $routeParams.type;
    $scope.tasks = [];

    $scope.update = function () {
        if (angular.isDefined($routeParams.taskId)) { //searching task by ID
            $$data.getTask($routeParams.taskId).then(function (value) {
                $scope.tasks = [angular.fromJson(value.data || {})];
                $log.info("taskSearchController: successfully updated task[" + $routeParams.taskId + "] content");
            }, function (errReason) {
                $scope.feedback = errReason;
                $log.error("taskSearchController: task[" + $routeParams.taskId + "] update failed: " + errReason);
            });
        } else if (angular.isDefined($routeParams.processId)) {//searching tasks for given process
            $$data.getProcessTasks($routeParams.processId).then(function (value) {
                $scope.tasks = angular.fromJson(value.data || {});
                $log.info("taskSearchController: successfully updated process[" + $routeParams.processId + "] tasks list");
            }, function (errReason) {
                $scope.feedback = errReason;
                $log.error("taskController: process[" + $routeParams.processId + "] tasks update failed: " + errReason);
            });
        }
    };

    $scope.update();

});

consoleControllers.controller("homeController", function ($scope) {
});
consoleControllers.controller("actorsController", function ($scope, $$data, $timeout) {
});
consoleControllers.controller("aboutController", function ($scope) {
});



