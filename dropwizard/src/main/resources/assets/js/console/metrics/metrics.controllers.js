angular.module("console.metrics.controllers", ['console.metrics.services', 'console.metrics.directives', 'console.util.services'])
    .controller("metricsController", function ($scope, tskMetricsData, tskTimeUtil, $log) {
        $scope.dataHolder = [];
        $scope.smoothRates = ["", 3, 7, 20, 30];
        $scope.metricsOptions = {};

        $scope.plotProps = {
            updatePeriod: 0,
            options: {
                zoom: {interactive: false},
                pan: {interactive: false}
//                xaxis: {ticks: 10},
//                yaxis: {ticks: 10}
            }
        };

        $scope.collapse = {
            filter: false,
            plot: false,
            table: true
        };

        //selected objects
        $scope.selection = {
            showDatasets: false,
            smoothRate: "",
            omitZeroes: false,
            datasets: {},
            metric: {},
            scopeMode: {},
            dataMode: {},
            periodMode: {}
        };

        $scope.getAvailableTypes = function(types) {
            if ($scope.selection.metric.value && types) {
                return types[$scope.selection.metric.value];
            } else {
                return [];
            }
        };

        var unCheckAllDatasets = function() {
            for (var key in $scope.selection.datasets) {
                $scope.selection.datasets[key] = false;
            }
        };

        var getActiveDataset = function() {
            var datasets = $scope.dataHolder;
            if (datasets && datasets.length>0) {
                for(var i = 0; i<datasets.length; i++) {
                    if (datasets[i].data && datasets[i].data.length>0) {
                        return datasets[i];
                    }
                }
            }
            return null;
        };

        var selectFirstAvailableOptions = function(){

            if ($scope.metricsOptions.scopes && $scope.metricsOptions.scopes[$scope.selection.metric.value].length>0) {
                $scope.selection.scopeMode = $scope.metricsOptions.scopes[$scope.selection.metric.value][0];
            }

            if ($scope.metricsOptions.periods && $scope.metricsOptions.periods[$scope.selection.metric.value].length>0) {
                $scope.selection.periodMode = $scope.metricsOptions.periods[$scope.selection.metric.value][0];
            }

            if ($scope.metricsOptions.dataTypes && $scope.metricsOptions.dataTypes[$scope.selection.metric.value].length>0) {
                $scope.selection.dataMode = $scope.metricsOptions.dataTypes[$scope.selection.metric.value][0];
            }
        };

        //Uncheck previously selected datasets
        $scope.$watch('selection.metric', function() {
            unCheckAllDatasets();
        });

        $scope.getYLabel = function() {
            var activeDs = getActiveDataset();
            if (activeDs) {
                return activeDs.yLabel;
            } else {
                return "";
            }
        };

        $scope.getXLabel = function() {
            var activeDs = getActiveDataset();
            if (activeDs) {
                return activeDs.xLabel;
            } else {
                return "";
            }
        };

        $scope.getTableData = function () {
            if($scope.collapse.table) {
                return [];
            } else {
                return $scope.dataHolder;
            }
        };

        $scope.getSelectedDataSets = function() {
            var result = "";
            for(var ds in $scope.selection.datasets) {
                if($scope.selection.datasets[ds]) {
                    if(result.length > 0) {
                        result = result + ",";
                    }
                    result = result + ds;
                }
            }
            return result;
        };

        $scope.getSelectedDataSetsCount = function() {
            var result = 0;
            for(var ds in $scope.selection.datasets) {
                if($scope.selection.datasets[ds]) {
                    result++;
                }
            }
            return result;
        };

        $scope.getDatasetList = function() {
            var result = [];
            if(angular.isDefined($scope.selection.metric.value)
                && angular.isDefined($scope.metricsOptions.dataSets)) {
                var allDatasetsForMetric = $scope.metricsOptions.dataSets[$scope.selection.metric.value];
                if (allDatasetsForMetric) {//TODO: use filter?
                    for(var i = 0; i<allDatasetsForMetric.length; i++) {
                        if($scope.selection.showDatasets || (allDatasetsForMetric[i].general == !$scope.selection.showDatasets)) {
                            result.push(allDatasetsForMetric[i]);
                        }
                    }
                }
            }

            if (result.length == 0) {
                $scope.selection.datasets = {};
            }

            return result;
        };

        tskMetricsData.getMetricsOptions().then(function(value) {
            $scope.metricsOptions = angular.fromJson(value.data || {});
            $log.info("metricsController: metricsOptions found are: " + angular.toJson(value.data));

            //Select first available values by default
            if ($scope.metricsOptions.metrics && $scope.metricsOptions.metrics.length>0) {
                $scope.selection.metric = $scope.metricsOptions.metrics[0];
            }

            selectFirstAvailableOptions();

        });

        $scope.update = function() {
            var dataset = $scope.getSelectedDataSets();
            tskMetricsData.getMetricsData($scope.selection, dataset).then(function(success) {
                $scope.dataHolder = success.data || [];
            }, function (error) {
                $scope.dataHolder = [];
                $log.log("Error updating metrics data: " + angular.toJson(error));
            });
        };

        $scope.clear = function() {
            $scope.dataHolder = [];
            unCheckAllDatasets();
        };

        var refreshTriggerId = -1;
        $scope.$watch('plotProps.updatePeriod', function (newVal, oldVal) {
            if (newVal > 0) {
                refreshTriggerId = tskTimeUtil.setInterval($scope.update, newVal * 1000, $scope);//Start autoUpdate
            } else {
                tskTimeUtil.clearInterval(refreshTriggerId);
            }
        });

        $scope.$watch(function() {
            var result = 0;
            for (var ds in $scope.selection.datasets) {
                if($scope.selection.datasets[ds]) {
                    result++;
                }
            }
            return result;

        }, function (newVal, oldVal) {
            if (newVal > 0) {
                $scope.update();
            } else {
                $scope.clear();
            }
        });

        $scope.$watch("selection.metric.value", function(newVal, oldVal){
            selectFirstAvailableOptions();
        });

        $scope.update();

    })

;
