angular.module("console.metrics.controllers", [])
    .controller("metricsController", function ($scope, $$data, $log, $location, $filter) {
        $scope.dataHolder = [];
        $scope.smoothRates = ["", 3, 7, 20, 30];
        $scope.actorIds = [];
        $scope.metricsOptions = {};

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

        //Uncheck previously selected datasets
        $scope.$watch('selection.metric', function() {
            for (var key in $scope.selection.datasets) {
                $scope.selection.datasets[key] = false;
            }
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


        $scope.getDatasetList = function() {
            var result = [];
            if(angular.isDefined($scope.selection.metric.value)
                && angular.isDefined($scope.metricsOptions.dataSets)) {
                var allDatasetsForMetric = $scope.metricsOptions.dataSets[$scope.selection.metric.value];
                if(allDatasetsForMetric) {//TODO: use filter?
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

        $scope.getDataSetUrl = function() {
            var dataset = $scope.getSelectedDataSets();
            var type = $scope.selection.dataMode.value;
            var scope = $scope.selection.scopeMode.value;
            var period = $scope.selection.periodMode.value;
            var metric = $scope.selection.metric.value;
            var action = "data";
            var zeroes = !$scope.selection.omitZeroes;
            var smooth = $scope.selection.smoothRate;

            if($scope.selection.actorSpecific) {
                action = "actorData";
            }

            if (!!dataset && !!type && !!scope && !!period && !!metric) {//url contains some defined values
                return "/rest/console/metrics/"+action+"/?zeroes="+zeroes+"&metric=" + metric + "&period=" + period + "&scope=" + scope + "&type=" + type + "&dataset=" + encodeURIComponent(dataset) + "&smooth=" + smooth;
            }
            return "";
        };

        $$data.getMetricsOptions().then(function(value) {
            $scope.metricsOptions = angular.fromJson(value.data || {});
            $log.info("metricsController: metricsOptions found are: " + angular.toJson(value.data));

            //Select first available values by default
            if($scope.metricsOptions.metrics && $scope.metricsOptions.metrics.length>0) {
                $scope.selection.metric = $scope.metricsOptions.metrics[0];
            }
            if($scope.metricsOptions.scopes && $scope.metricsOptions.scopes[$scope.selection.metric.value].length>0) {
                $scope.selection.scopeMode = $scope.metricsOptions.scopes[$scope.selection.metric.value][0];
            }
            if($scope.metricsOptions.periods && $scope.metricsOptions.periods[$scope.selection.metric.value].length>0) {
                $scope.selection.periodMode = $scope.metricsOptions.periods[$scope.selection.metric.value][0];
            }
            if($scope.metricsOptions.dataTypes && $scope.metricsOptions.dataTypes[$scope.selection.metric.value].length>0) {
                $scope.selection.dataMode = $scope.metricsOptions.dataTypes[$scope.selection.metric.value][0];
            }

        });


    });
