angular.module("queue.controllers", ['console.services', 'ngRoute'])

    .controller("queueListController", function ($scope, $$data, $$timeUtil, $log, $cookieStore) {
        var initPagingObject = function() {
            var result = {
                pageSize: 5,
                pageNumber: 1,
                totalCount: 0,
                items: []
            };
            var pagingState = $cookieStore.get("queue.list.pagination");
            if (pagingState) {
                result.pageSize = pagingState.pageSize;
                result.pageNumber = pagingState.pageNumber;
            }
            return result;
        };

        //Init paging object
        $scope.queuesPage = initPagingObject();

        $scope.feedback = "";
        $scope.initialized = false;

        $scope.realSizes = {};
        $scope.balancePeriods = ["Last day", "Last hour"];


        $scope.selection = $cookieStore.get('queue.controller.selection');
        if(!$scope.selection) {
            $scope.selection = {
                balancePeriod: "Last hour",
                initialRefreshRate: 0,
                filter: "",
                pageSize: 5,
                pageNumber: 1
            };
        }

        $scope.$watch('selection | json', function() {
            $cookieStore.put('queue.controller.selection', $scope.selection);
        });

        $scope.hasCurrentSizeValue = function(idx) {
            if ($scope.realSizes[idx]) {
                return true;
            } else {
                return false;
            }
        };

        $scope.showRealSize = function (queueName, idx) {

            $$data.getQueueRealSize(queueName).then(function(value) {
                $scope.realSizes[idx] = value.data;
            }, function(err) {
                $scope.realSizes[idx] = "n/a";
            });

        };

        $scope.totalTasks = function () {
            var result = 0;
            if($scope.queuesPage.items) {
                for (var i = 0; i < $scope.queuesPage.items.length; i++) {
                    result = result + $scope.queuesPage.items[i].count;
                }
            }
            return result;
        };

        //Updates queues states  by polling REST resource
        $scope.update = function () {

            $$data.getQueueList($scope.queuesPage.pageNumber, $scope.queuesPage.pageSize, $scope.selection.filter).then(function (value) {
                $scope.queuesPage = value.data || initPagingObject();
                $log.info("queueListController: successfully updated queues state: " + angular.toJson($scope.queuesPage));
                $scope.initialized = true;
            }, function (errReason) {
                $scope.feedback = angular.toJson(errReason);
                $log.error("queueListController: queue state update failed: " + $scope.feedback);
                $scope.initialized = true;
            });

        };

        $scope.refresh = function() {
            $scope.initialized = false;
            $scope.update();
        };

        //Initialization:
        $scope.refresh();

    });