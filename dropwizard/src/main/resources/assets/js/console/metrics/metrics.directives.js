angular.module("console.metrics.directives", ['console.metrics.services'])
    .directive('tskMetricsPlot', ['tskMetricsFormatters', function (tskMetricsFormatters) {
        return {
            restrict: 'CA',//Class, Attribute
            terminal: true,
            scope: {//'=' enables ability to $watch
                datasets: "=",
                width: "@",
                height: "@",
                addOptions: "="
            },
            controller: ['$scope', '$element', '$attrs', '$transclude', '$window', '$log', '$http', 'tskTimeUtil', function ($scope, $element, $attrs, $transclude, $window, $log, $http, tskTimeUtil) {

                var jPlot = $($element);

                //Setting css width and height if explicitly set
                if($scope.width) { jPlot.css("width", $scope.width); }
                if($scope.height) { jPlot.css("height", $scope.height); }

                $("<div id='metrics-tooltip'></div>").css({
                    position: "absolute",
                    display: "none",
                    border: "1px solid #fdd",
                    padding: "2px",
                    backgroundColor: "#FFA801",
                    opacity: 0.80,
                    fontSize: "12px"
                }).appendTo("body");

                var defaultOptions = {
                    legend: {show: true},
                    grid: {
                        hoverable: true
//                        clickable: true
                    },
                    series: {
                        lines: {show: true},
                        points: {show: true}
                    }
                };

                var plotElem = $.plot(jPlot, [], defaultOptions);

                jPlot.bind("plothover", function (event, pos, item) {

                    if ($scope.datasets.length>0) {
                        var posX = tskMetricsFormatters.getFormattedValue($scope.datasets[0].xFormatter, pos.x, false);
                        var posY = tskMetricsFormatters.getFormattedValue($scope.datasets[0].yFormatter, pos.y, false);
                        if (angular.isNumber(posX)) {
                            posX = posX.toFixed(2);
                        }
                        if (angular.isNumber(posY)) {
                            posY = posY.toFixed(2);
                        }
                        $("#metrics-hoverdata .current").text("(" + posX + ", " + posY + ")");

                        if (item) {
                            var xVal = tskMetricsFormatters.getFormattedValue($scope.datasets[0].xFormatter, item.datapoint[0], false);
                            var yVal = tskMetricsFormatters.getFormattedValue($scope.datasets[0].yFormatter, item.datapoint[1], false);
                            $("#metrics-tooltip").html("[" + xVal + ", " + yVal + "]")
                                .css({top: item.pageY+5, left: item.pageX+5})
                                .fadeIn(200);
                        } else {
                            $("#metrics-tooltip").hide();
                        }
                    }

                });

                var updatePlotData = function(newData) {
                    $log.info("Update plot data. New datasets count: " + newData.length);

                    if (!!newData && newData.length>0) {
                        var newOptions = $.extend({}, defaultOptions);

                        if (!!newData[0].yFormatter && !!tskMetricsFormatters[newData[0].yFormatter]) {
                            newOptions = $.extend(newOptions, {
                                yaxis: {
                                    tickFormatter: tskMetricsFormatters[newData[0].yFormatter]
                                }
                            });
                        }

                        if (!!newData[0].xFormatter && !!tskMetricsFormatters[newData[0].xFormatter]) {
                            newOptions = $.extend(newOptions, {
                                xaxis: {
                                    tickFormatter: tskMetricsFormatters[newData[0].xFormatter]
                                }
                            });
                        }
                        newOptions = $.extend(newOptions, $scope.addOptions);

                        plotElem = $.plot(jPlot, newData, newOptions);
                    } else {
                        plotElem = $.plot(jPlot, [], defaultOptions);
                    }
                };

                $scope.$watch('datasets', function (newVal, oldVal) {
                    updatePlotData(newVal);
                });

                $scope.$watch('addOptions', function (newVal, oldVal) {
                    updatePlotData(plotElem.getData());
                }, true);

            }],
            replace: true
        };
    }])
;