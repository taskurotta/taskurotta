angular.module("console.schedule.controllers", ['console.services', 'ui.bootstrap.modal'])

    .controller("scheduleCardController", function ($scope, $log, $http, $location, $routeParams) {
        $scope.feedback = "";

        $scope.job = {
            name: "",
            cron: "",
            queueLimit: 0,
            maxErrors: 3,
            task: {
                type: "WORKER_SCHEDULED",
                method: "",
                actorId: ""
            },
            args: [],
            taskList: ''
        };

        $scope.argsVisible = false;

        $scope.types = ["WORKER_SCHEDULED", "DECIDER_START"];

        $scope.isCronValid = false;

        $scope.initialized = false;

        $scope.addArgument = function() {
            $scope.job.args.push({
                type: "string",
                value: ""
            });
        };

        $scope.removeArgument = function(idx) {
            $scope.job.args.splice(idx, 1);
        };

        var getCreateJobCommand = function() {
            return {
                name: $scope.job.name,
                cron: $scope.job.cron,
                queueLimit: $scope.job.queueLimit,
                maxErrors: $scope.job.maxErrors,
                actorId: $scope.job.task.actorId,
                method: $scope.job.task.method,
                taskType: $scope.job.task.type,
                args: $scope.job.args,
                taskList: $scope.job.taskList
            };
        };

        var getDataTypeAsArgType = function(dataType) {
            var result = dataType;
            if (!!dataType) {
                var idx = dataType.indexOf("java.lang.");
                if (idx>=0) {
                    result = dataType.substring(10).toLowerCase();
                }
            }
            return result;
        };

        var getJobArgs = function (argContainers) {
            var result = [];
            if (!!argContainers && argContainers.length>0) {
                for (var i = 0; i<argContainers.length; i++) {
                    var argContainer = argContainers[i];
                    result.push({
                        type: getDataTypeAsArgType(argContainer.dataType),
                        value: argContainer.jsonvalue
                    });
                }
            }
            return result;
        };

        $scope.update = function() {
            var jobId = parseInt($routeParams.id);
            if (jobId > 0) {//edit
                $http.get("/rest/console/schedule/card?id=" + encodeURIComponent(jobId)).then(function(success) {
                    if (success.data) {
                        $scope.job = success.data;
                        $log.info("Got job: " + angular.toJson(success.data));
                        if (!!success.data.task) {
                            $scope.job.args = getJobArgs(success.data.task.args);
                            if (!!success.data.task.options && !!success.data.task.options.taskConfigContainer) {
                                $scope.job.taskList = success.data.task.options.taskConfigContainer.taskList
                            }
                        }

                        $scope.validateCron();
                    }
                    $scope.initialized = true;

                }, function(error) {
                    $scope.feedback = error;
                    $scope.initialized = true;
                });
            } else {//create new job
                $scope.initialized = true;
            }
        };

        $scope.createScheduledJob = function() {
            if ($scope.isValidForm()) {
                $http.put("/rest/console/schedule/create?", getCreateJobCommand()).then(
                    function(value) {
                        $location.url("/schedule/list");
                    },
                    function(err) {
                        $scope.feedback = err;
                    });
            }
        };

        $scope.updateScheduledJob = function() {
            if ($scope.isValidForm()) {
                var jobId = parseInt($routeParams.id);
                if (jobId>0) {
                    $http.put("/rest/console/schedule/update?jobId="+encodeURIComponent(jobId), getCreateJobCommand()).then(
                        function(value) {
                            $location.url("/schedule/list");
                        },
                        function(err) {
                            $scope.feedback = err;
                        });
                }
            }
        };

        $scope.validateCron = function() {
            if ($scope.job.cron && $scope.job.cron.length > 0) {
                $http.get("/rest/console/schedule/validate/cron?value="+encodeURIComponent($scope.job.cron))
                    .then(function(value) {
                        if (value.data.length >0 ) {
                            $scope.isCronValid = false;
                        } else {
                            $scope.isCronValid = true;
                        }
                    }, function(errMes) {
                        $scope.feedback = errMes;
                    });
            }
        };

        $scope.isValidForm = function() {
            var exists = angular.isDefined($scope.job.name) && angular.isDefined($scope.isCronValid) && angular.isDefined($scope.job.task.method) && angular.isDefined($scope.job.task.actorId);
            var queueLimitParseable = $scope.job.queueLimit==0 || !!parseInt($scope.job.queueLimit);
            var maxErrorsParseable = $scope.job.maxErrors==0 || !!parseInt($scope.job.maxErrors);
            return exists && maxErrorsParseable && queueLimitParseable && $scope.job.name.length>0 && $scope.isCronValid && $scope.job.task.method.length>0 && $scope.job.task.actorId.length>0;
        };

        $scope.update();

    })

    .controller("scheduleListController", function ($scope, $http, $log, $modal, tskUtil) {

        $scope.scheduledTasks = [];
        $scope.feedback = {};

        $scope.initialized = false;

        $scope.total = "undefined";
        $scope.totalInitialized = false;


        $scope.getStatusClassName = function(status) {
            var result = "warning";
            if(status == -2) {
                result = "error";
            } else if(status == -1) {
                result = "info";
            } else if(status == 1) {
                result = "success";
            }

            return result;
        };

        $scope.getStatusText = function(status) {
            var result = "Undefined status";
            if(status == -2) {
                result = "Error";
            } else if(status == -1) {
                result = "Inactive";
            } else if(status == 1) {
                result = "Active";
            }

            return result;
        };


        $scope.update = function() {
            $http.get("/rest/console/schedule/list").then(function(value) {
                $scope.scheduledTasks = value.data;
                $scope.initialized = true;

                $http.get("/rest/console/schedule/node_count").then(function(value){
                    $scope.total = value.data || "undefined";
                    $scope.totalInitialized = true;
                }, function(errValue) {
                    $scope.total = "undefined";
                    $scope.totalInitialized = true;
                });

            }, function(errReason) {
                $scope.feedback = errReason;
                $scope.initialized = true;
                $scope.totalInitialized = true;
            });

            $http.get("/rest/console/timer/server_time").then(function(val){
                $scope.serverTime = val.data;
            }, function(err){
                $log.error("Cannot update server time", err);
            });
        };

        $scope.activate = function(id) {
            $http.post("/rest/console/schedule/action/activate/?id=" + id, id).then(function(value) {
                $scope.update();
            }, function(errReason) {
                $scope.feedback = errReason;
            });
        };

        $scope.deactivate = function(id) {
            $http.post("/rest/console/schedule/action/deactivate/?id="+id, id).then(function(value) {
                $scope.update();
            }, function(errReason) {
                $scope.feedback = errReason;
            });
        };

        $scope.delete = function(id) {

            var modalInstance = $modal.open({
                templateUrl: '/partials/view/modal/approve_msg.html',
                windowClass: 'approve'
            });

            modalInstance.result.then(function(okMess) {
                $http.post("/rest/console/schedule/action/delete/?id="+id, id).then(function(value) {
                    $scope.update();
                }, function(errReason) {
                    $scope.feedback = errReason;
                });
            }, function(cancelMsg) {
                //do nothing
            });

        };

        $scope.showError = function (errMessage) {
            var modalInstance = $modal.open({
                templateUrl: '/partials/view/modal/stacktrace_msg.html',
                windowClass: 'stack-trace',
                controller: function ($scope) {
                    $scope.stackTrace = errMessage;
                }
            });

            modalInstance.result.then(function(okMess) {
                //do nothing
            }, function(cancelMsg) {
                //do nothing
            });
        };

        $scope.update();

    });
