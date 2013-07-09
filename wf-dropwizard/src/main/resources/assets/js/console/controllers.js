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

    //Init paging object
    $scope.queueTasksPage = {
        pageSize: 5,
        pageNumber: 1,
        totalCount: 0,
        items: []
    };

    $scope.queueName = $routeParams.queueName;

    //Updates queue items by polling REST resource
    $scope.update = function () {
        $$data.getQueueContent($scope.queueName, $scope.queueTasksPage.pageNumber, $scope.queueTasksPage.pageSize).then(function (value) {
            $scope.queueTasksPage = angular.fromJson(value.data || {});
            $log.info("queueContentController: successfully updated queue content");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("queueContentController: queue content update failed: " + errReason);
        });

    };

    //Initialization:
    $scope.update();

});

consoleControllers.controller("processListController", function ($scope, $$data, $$timeUtil, $log) {
    //Init paging object
    $scope.processesPage = {
        pageSize: 5,
        pageNumber: 1,
        totalCount: 0,
        items: []
    };

    //Updates queues states  by polling REST resource
    $scope.update = function () {

        $$data.getProcessesList($scope.processesPage.pageNumber, $scope.processesPage.pageSize).then(function (value) {
            $scope.processesPage = angular.fromJson(value.data || {});
            $log.info("processListController: successfully updated processes list");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("processListController: process list update failed: " + errReason);
        });

    };

    //Initialization:
    $scope.update();
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
    $scope.taskTree = {};
    $scope.id = $routeParams.processId;
    $scope.feedback = "";

    $scope.update = function () {
        $$data.getProcess($routeParams.processId).then(function (value) {
            $scope.process = angular.fromJson(value.data || {});
            $log.info("processCardController: successfully updated process[" + $routeParams.processId + "] content");

            $$data.getProcessTree($routeParams.processId, $scope.process.startTaskUuid).then(function (value) {
                $scope.taskTree = angular.fromJson(value.data || {});
                $log.info("processCardController: successfully updated process[" + $routeParams.processId + "]/["+$scope.process.startTaskUuid+"] tree");
            }, function (errReason) {
                $scope.feedback = errReason;
                $log.error("processCardController: process[" + $routeParams.processId + "] tree update failed: " + errReason);
            });

        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("processCardController: process[" + $routeParams.id + "] update failed: " + errReason);
        });


    };

    $scope.update();
});

consoleControllers.controller("processSearchController", function ($scope, $$data, $$timeUtil, $log, $routeParams) {
    $scope.id = $routeParams.id || '';
    $scope.type = $routeParams.type;
    $scope.processes = [];

    $scope.update = function () {
        if ($scope.type == 'custom_id') { //searching process by customID
            $$data.findProcess($scope.type, $scope.id).then(function (value) {
                $scope.processes = angular.fromJson(value.data || {});
                $log.info("processSearchController: successfully found processes with customId started with[" + $scope.id + "]");
            }, function (errReason) {
                $scope.feedback = errReason;
                $log.error("processSearchController: search for processes with customId started wirh [" + $scope.id + "] failed: " + errReason);
            });
        } else if ($scope.type == 'process_id') {//searching process by ID
            $$data.findProcess($scope.type, $scope.id).then(function (value) {
                $scope.processes = angular.fromJson(value.data || {});
                $log.info("processSearchController: successfully found processes with Id started with[" + $scope.id + "]");
            }, function (errReason) {
                $scope.feedback = errReason;
                $log.error("processSearchController: search for processes with Id started wirh [" + $scope.id + "] failed: " + errReason);
            });
        }
    };

    $scope.update();

});

consoleControllers.controller("taskListController", function ($scope, $$data, $log) {

    $scope.feedback = "";

    //Init paging object
    $scope.tasksPage = {
        pageSize: 5,
        pageNumber: 1,
        totalCount: 0,
        items: []
    };

    //Updates queues states  by polling REST resource
    $scope.update = function () {

        $$data.listTasks($scope.tasksPage.pageNumber, $scope.tasksPage.pageSize).then(function (value) {
            $scope.tasksPage = angular.fromJson(value.data || {});
            $log.info("taskListController: successfully updated tasks page");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("queueListController: tasks page update failed: " + errReason);
        });

    };

    //Initialization:
    $scope.update();

});

consoleControllers.controller("taskCardController", function ($scope, $$data, $routeParams, $log) {
    $scope.task = {};
    $scope.taskTree = {};
    $scope.id = $routeParams.taskId;

    $scope.feedback = "";
    $scope.update = function () {
        $$data.getTask($routeParams.taskId, $routeParams.processId).then(function (value) {
            $scope.task = angular.fromJson(value.data || {});
            $log.info("taskController: successfully updated task[" + $routeParams.id + "] content");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("taskController: task[" + $routeParams.id + "] update failed: " + errReason);
        });
        $$data.getTaskTree($routeParams.taskId, $routeParams.processId).then(function (value) {
            $scope.taskTree = angular.fromJson(value.data || {});
            $log.info("taskController: successfully updated task tree[" + $routeParams.id + "] content");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("taskController: task[" + $routeParams.id + "] tree update failed: " + errReason);
        });
    };

    $scope.update();

});

consoleControllers.controller("taskSearchController", function ($scope, $routeParams, $$data, $log) {
    $scope.taskId = $routeParams.id || '';
    $scope.processId = $routeParams.id || '';
    $scope.type = $routeParams.type;
    $scope.tasks = [];

    $scope.update = function () {
        if ($scope.type == 'task_id') { //searching task by ID
            $$data.getTask($scope.taskId, $scope.processId).then(function (value) {
                $scope.tasks = [angular.fromJson(value.data || {})];
                $log.info("taskSearchController: successfully updated task[" + $routeParams.taskId + "] content");
            }, function (errReason) {
                $scope.feedback = errReason;
                $log.error("taskSearchController: task[" + $routeParams.taskId + "] update failed: " + errReason);
            });
        } else if ($scope.type == 'process_id') {//searching tasks for given process
            $$data.getProcessTasks($scope.processId).then(function (value) {
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

consoleControllers.controller("hoveringQueuesController", function ($scope, $$data, $$timeUtil, $log) {

    $scope.feedback = "";

    //Init paging object
    $scope.queues = [];

    $scope.periodSize = 2;

    $scope.totalTasks = function () {
        var result = 0;
        for (var i = 0; i < $scope.queues.length; i++) {
            result = result + $scope.queues[i].count;
        }
        return result;
    };

    //Updates queues states  by polling REST resource
    $scope.update = function () {

        $$data.getHoveringQueues($scope.periodSize).then(function (value) {
            $scope.queues = angular.fromJson(value.data || {});
            $log.info("queueListController: successfully updated queue state");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("queueListController: queue state update failed: " + errReason);
        });

    };

    //Initialization:
    $scope.update();

});

consoleControllers.controller("repeatedTasksController", function ($scope, $routeParams, $$data, $log) {
    $scope.iterationCount = 5;
    $scope.tasks = [];

    $scope.update = function () {
        $$data.getRepeatedTasks($scope.iterationCount).then(function (value) {
            $scope.tasks = angular.fromJson(value.data || {});
            $log.info("repeatedTasksController: tasks loaded successfully");
        }, function (errReason) {
            $scope.feedback = errReason;
            $log.error("repeatedTasksController: load repeated tasks failed: " + errReason);
        });
    };

    $scope.update();

});
consoleControllers.controller("homeController", function ($scope) {
});
consoleControllers.controller("actorsController", function ($scope, $$data, $timeout) {
});
consoleControllers.controller("aboutController", function ($scope) {
});



