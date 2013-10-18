var consoleActorControllers = angular.module("console.actor.controllers", ['console.services', 'ui.bootstrap.modal', 'ngRoute']);

consoleActorControllers.controller("actorListController", ['$scope', '$$data', 'tskActors', '$log', '$modal', '$location', '$cookieStore', function ($scope, $$data, tskActors, $log, $modal, $location, $cookieStore) {
    $scope.feedback = "";
    $scope.initialized = false;

    //Init paging object
    $scope.actorsPage = {
        pageSize: 10,
        pageNumber: 1,
        totalCount: 0,
        items: []
    };

    $scope.selection = {
        actorIds: {}
    };

    $scope.getSelectedActorIds = function() {
        var result = [];
        for (var key in $scope.selection.actorIds) {
            if ($scope.selection.actorIds[key]) {
                result.push(key);
            }
        }
        return result;
    };

    $scope.navigateToCompareView = function() {
        $cookieStore.put('actors.compare.actorIds', $scope.getSelectedActorIds());
        $location.url('/actors/compare/');
    };

    $scope.isSeveralActorsSelected = function(){
        return $scope.getSelectedActorIds().length > 1;
    };

    $scope.isActorSelected = function(actorId) {
        var selected = $scope.getSelectedActorIds();
        for (var i = 0; i<selected.length; i++) {
            if (selected[i] == actorId) {
                return true;
            }
        }
        return false;
    };

    $scope.isSelectionLinkActive = function(actorId) {
        return $scope.isSeveralActorsSelected() && $scope.isActorSelected(actorId);
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

    $scope.totalTasks = function () {
        var result = 0;
        if($scope.actorsPage.items) {
            for (var i = 0; i < $scope.actorsPage.items.length; i++) {
                result = result + $scope.actorsPage.items[i].count;
            }
        }
        return result;
    };

    //Updates queues states  by polling REST resource
    $scope.update = function () {

        tskActors.listActors($scope.actorsPage.pageNumber, $scope.actorsPage.pageSize).then(function (value) {
            $scope.actorsPage = value.data || {};
            $scope.initialized = true;
            $log.info("actorListController: successfully updated actors list: " + angular.toJson($scope.actorsPage));
            $cookieStore.put('actors.compare.actorIds', $scope.getSelectedActorIds());
        }, function (errReason) {
            $scope.feedback = errReason;
            $scope.initialized = true;
            $log.error("actorListController: actor list update failed: " + angular.toJson($scope.feedback));
        });

    };

    $scope.initSelection = function() {
        var selected = $cookieStore.get('actors.compare.actorIds');
        if (selected && selected.length>0) {
            for (var i = 0; i<selected.length; i++) {
                $scope.selection.actorIds[selected[i]] = true;
            }
        }
    };

    //Initialization:
    $scope.initSelection();
    $scope.update();

}]);


consoleActorControllers.controller("actorCompareController", function ($scope, $location, $log, $cookieStore, tskActors) {

    $scope.actorIds = $cookieStore.get('actors.compare.actorIds');

    $scope.metrics = [];
    $scope.metricsData = {};
    $scope.feedback = {};
    $scope.selection = {
        metric: {
            "enqueue": true,
            "successfulPoll": true
        }
    };


    $scope.getSelectedMetricNames = function() {
        var result = [];
        for(var key in $scope.selection.metric) {
            if ($scope.selection.metric[key]) {
                result.push(key);
            }
        }
        return result;
    }


    $scope.updateMetricsData = function() {
        //var selectedMetrics = $scope.getSelectedMetricNames();
        if ($scope.actorIds.length>0 && $scope.metrics.length>0) {
            tskActors.getMetricsData($scope.actorIds, $scope.metrics).then(function (success) {
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

    $scope.update = function () {
        tskActors.listMetrics().then(function (success) {
            $scope.metrics = success.data;
            $scope.updateMetricsData();
        }, function (fail) {
            $scope.feedback = fail.data;
        });
    };


    $scope.getActorStatData = function(metric, actor, collection) {
        for (var i = 0; i < collection.length; i++) {
            var item = collection[i];
            if (item.metricName==metric && item.datasetName==actor ) {
                return item;
            }
        }
        return {};
    };

    $scope.update();

});



