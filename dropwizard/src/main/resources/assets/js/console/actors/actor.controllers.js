angular.module("console.actor.controllers", ['console.services', 'ui.bootstrap.modal', 'ngRoute'])

    .controller("actorListController", ['$scope', '$$data', 'tskActors', '$log', '$modal', '$location', function ($scope, $$data, tskActors, $log, $modal, $location) {
        $scope.initialized = false;
        $scope.metrics = [];
        $scope.metricsData = {};
        $scope.feedback = {};

        var defaultActorsPage = {
            pageSize: 10,
            pageNumber: 1,
            totalCount: 0,
            items: []
        };
        //Init paging object
        $scope.actorsPage = defaultActorsPage;

        $scope.periods = ["day", "hour"];

        $scope.selection = {
            metric: {},
            metricPeriod: {},
            filter: ''
        };

        var setBlockedState = function(actorId, isBlocked) {
            var modalInstance = $modal.open({
                templateUrl: '/partials/view/modal/approve_msg.html',
                windowClass: 'approve'
            });

            modalInstance.result.then(function(okMess) {
                tskActors.setActorBlocked(actorId, isBlocked).then(function(success) {
                    $log.log("Actor ["+actorId+"] have been set to blocked["+isBlocked+"]");
                    $scope.update();
                }, function(error) {
                    $log.error("Error setting blocked["+isBlocked+"] for actor ["+actorId+"]: " + angular.toJson(error));
                    $scope.feedback = error;
                    $scope.update();
                });
            }, function(cancelMsg) {
                //do nothing
            });
        };

        $scope.blockActor = function(actorId) {
            setBlockedState(actorId, true);
        };

        $scope.unblockActor = function(actorId) {
            setBlockedState(actorId, false);
        };

        $scope.hasHourRateData = function(actorVO) {
            var result = false;
            if (actorVO.queueState) {
                result = (actorVO.queueState.totalOutHour && actorVO.queueState.totalOutHour>=0)
                    || (actorVO.queueState.totalInHour && actorVO.queueState.totalInHour>=0);
            }
            return result;
        };

        $scope.hasDayRateData = function(actorVO) {
            var result = false;
            if (actorVO.queueState) {
                result = (actorVO.queueState.totalOutDay && actorVO.queueState.totalOutDay>=0)
                    || (actorVO.queueState.totalInDay && actorVO.queueState.totalInDay>=0);
            }
            return result;
        };

        //Updates queues states  by polling REST resource
        $scope.update = function () {

            tskActors.listActors($scope.actorsPage.pageNumber, $scope.actorsPage.pageSize, $scope.selection.filter).then(function (value) {
                $scope.actorsPage = value.data || defaultActorsPage;
                $log.info("actorListController: successfully updated actors list: ", $scope.actorsPage);

                tskActors.listMetrics().then(function (success) {
                    $scope.metrics = success.data;
                    for (var i = 0; i<$scope.metrics.length; i++) {//period "hour" by default for all metrics (if other value has not been specified)
                        if (!$scope.selection.metricPeriod[$scope.metrics[i]]) {
                            $scope.selection.metricPeriod[$scope.metrics[i]] = "hour";
                        }
                    }

                    $scope.updateMetricsData();
                    $scope.initialized = true;
                }, function (fail) {
                    $scope.feedback = fail.data;
                    $scope.initialized = true;
                });

            }, function (errReason) {
                $scope.feedback = errReason;
                $scope.initialized = true;
                $log.error("actorListController: actor list update failed: " + angular.toJson($scope.feedback));
            });

        };

        $scope.getSelectedMetricNames = function() {
            var result = [];
            for (var key in $scope.selection.metric) {
                if ($scope.selection.metric[key]) {
                    result.push(key);
                }
            }
            return result;
        };

        $scope.updateMetricsData = function() {
            var actorIds = [];
            for (var i = 0; i< $scope.actorsPage.items.length; i++) {
                actorIds.push($scope.actorsPage.items[i].actorId);
            }
            var metricsArr = $scope.getSelectedMetricNames();
            if (metricsArr.length > 0) {
                tskActors.getMetricsData(actorIds, metricsArr).then(function (success) {
                    $scope.metricsData = success.data;
                }, function (error) {
                    $scope.feedback = error.data;
                });
            }
        };

        $scope.getMetricsRows = function(columns) {
            var result = [];//array of row start indexes
            for (var i = 0; i<$scope.metrics.length; i = i+columns) {
                result.push(i);
            }
            return result;
        };

        $scope.getActorStatData = function(metric, actor, collection) {
            var result = {};
            for (var i = 0; i < collection.length; i++) {
                var item = collection[i];
                if (item.metricName==metric && item.datasetName==actor ) {
                    result = item;
                    break;
                }
            }
            //$log.log("getActorStatData with args: metric["+metric+"], actor["+actor+"], collection["+angular.toJson(collection)+"] result is ["+angular.toJson(result)+"]");
            return result;
        };

        //Initialization:
        $scope.update();
        $scope.$watch("getSelectedMetricNames().length", function(newVal, oldVal) {
            $scope.updateMetricsData();
        });

    }])
;