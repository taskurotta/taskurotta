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
    $scope.refreshRate = 0;

    //Init paging object
    $scope.queuesPage = {};
    $scope.queuesPage.items = [];
    $scope.queuesPage.pageNumber = 1;
    $scope.queuesPage.pageSize = 5;

    $scope.totalTasks = function () {
        var result = 0;
        for (var i = 0; i < $scope.queuesPage.items.length; i++) {
            result = result + $scope.queuesPage.items[i].count;
        }
        return result;
    };

    //Updates queues states  by polling REST resource
    $scope.update = function () {

        //If user is on the last page and change pageSize, we can get situation when current page number > total pages count
        if ($scope.queuesPage.pageNumber > $scope.totalPages()) {
            $scope.queuesPage.pageNumber = $scope.totalPages();
        }

        $$data.getQueueList($scope.queuesPage.pageNumber, $scope.queuesPage.pageSize).then(function (value) {
            $scope.queuesPage = angular.fromJson(value.data || {});
            $log.info("queueListController: successfully updated queue state");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("queueListController: queue state update failed: " + errReason);
        });

    };

    //Show previous page
    $scope.prevPage = function () {
        if ($scope.queuesPage.pageNumber > 1) {
            $scope.queuesPage.pageNumber--;
        }
        $scope.update();
    };

    $scope.totalPages = function () {
        var reminder = $scope.queuesPage.totalCount % $scope.queuesPage.pageSize;
        var pagesCount = Math.floor($scope.queuesPage.totalCount / $scope.queuesPage.pageSize);
        if (reminder > 0) {
            pagesCount++;
        }
        return pagesCount
    };

    $scope.getMinIndex = function () {
        var minIndex = ($scope.queuesPage.pageNumber - 1) * $scope.queuesPage.pageSize + 1;
        if ($scope.queuesPage.totalCount <= 0) {
            minIndex = 0;
        }
        return minIndex;
    };

    $scope.getMaxIndex = function () {
        var maxIndex = $scope.queuesPage.totalCount;
        if ($scope.queuesPage.pageNumber < $scope.totalPages()) {
            maxIndex = $scope.queuesPage.pageNumber * $scope.queuesPage.pageSize;
        }
        return maxIndex
    };

    //Show next page
    $scope.nextPage = function () {
        if ($scope.queuesPage.pageNumber < $scope.totalPages()) {
            $scope.queuesPage.pageNumber++;
        }
        $scope.update();
    };


    //Auto refresh feature. re triggers auto refreshing on refresh rate changes
    var currentRefreshIntervalId = -1;
    $scope.$watch(function () {
        return $scope.refreshRate;
    }, function (value) {
        if (currentRefreshIntervalId > 0) {
            $$timeUtil.clearInterval(currentRefreshIntervalId);
        }
        if ($scope.refreshRate > 0) {
            currentRefreshIntervalId = $$timeUtil.setInterval($scope.update, $scope.refreshRate * 1000, $scope);//Start autoUpdate
        }
    }, true);


    //Initialization:
    $scope.update();

});

consoleControllers.controller("queueCardController", function ($scope, $$data, $$timeUtil, $log, $routeParams) {

    $scope.feedback = "";
    $scope.refreshRate = 0;

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

    //Auto refresh feature. re triggers auto refreshing on refresh rate changes
    var currentRefreshIntervalId = -1;
    $scope.$watch(function () {
        return $scope.refreshRate;
    }, function (value) {
        if (currentRefreshIntervalId > 0) {
            $$timeUtil.clearInterval(currentRefreshIntervalId);
        }
        if ($scope.refreshRate > 0) {
            currentRefreshIntervalId = $$timeUtil.setInterval($scope.update, $scope.refreshRate * 1000, $scope);//Start autoUpdate
        }
    }, true);


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



