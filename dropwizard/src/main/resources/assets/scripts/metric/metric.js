angular.module('metricModule', ['coreApp'])

    .factory('metricRest', function ($log, coreApp, $resource) {
        var restMetricUrl = coreApp.getRestUrl() + 'metrics/';

        return $resource(restMetricUrl + 'data/', {}, {
                //dictionaries
                dictionaryOptions: {url: restMetricUrl + 'options/', params: {}, cache: true}
                //dictionaryOptions: {url: '/scripts/metric/options.json', params: {}, cache: true}
            }
        );
    })


    .controller('metricListController', function ($log, $scope, metricRest, smoothRates, coreApp, $state,$stateParams) {
        $log.info('metricListController');

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.metricsResource =  metricRest.query(params,
                function success(value) {
                    $scope.metricsModel = value; //cause array or object
                    if ($scope.metricsModel) {
                        $log.info('Successfully updated metrics data');
                    } else {
                        coreApp.info('Metrics data not found');
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Metrics data update failed', reason);
                });
        }

        //Initialization:
        $scope.smoothRates = smoothRates;

        $scope.formParams = coreApp.copyStateParams();
        $scope.$stateParams = coreApp.getStateParams();
        $scope.formParams.dataset = $scope.formParams.dataset ?
            coreApp.clearObject(JSON.parse($scope.formParams.dataset)) : {};


        $scope.options = metricRest.dictionaryOptions({},
            function success(options) {
                $log.log('Loaded metric options dictionary', options);

                $scope.isValidForm = function() {
                    var params = $scope.formParams;
                    return params.metric &&
                        angular.isObject(params.dataset) &&
                        _.find(options.scopes[params.metric],
                            function(scope){ return scope.value === params.scope; }) &&
                        _.find(options.dataTypes[params.metric],
                            function(type){ return type.value === params.type; }) &&
                        _.find(options.periods[params.metric],
                            function(period){ return period.value === params.period; }) &&
                        _.find(params.dataset,
                            function(dataset){ return dataset === true; });
                };

                if(options.metrics.length){
                    if($scope.isValidForm()){
                        loadModel(angular.extend({},$scope.formParams, {
                            dataset: coreApp.getKeys($scope.formParams.dataset).join(',')
                        }));
                    }
                }else{
                    coreApp.error('No available metrics to show', options);
                }

            }, function error(reason) {
                coreApp.error('Metrics options dictionary update failed', reason);
            });



        //Update command:
        $scope.search = function () {
            coreApp.reloadState(angular.extend({},$scope.formParams,{
                refreshRate: undefined,
                dataset: JSON.stringify(coreApp.clearObject($scope.formParams.dataset))
            }));
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });


    })

    .directive('metricsPlot', function ( $log, metricsFormatters) {
        function getOptions(params, xFormatter, yFormatter){
            var xFormatterFunc = xFormatter ? metricsFormatters[xFormatter] : undefined;
            var yFormatterFunc = yFormatter ? metricsFormatters[yFormatter] : undefined;
            return {
                zoom: {interactive: (params && params.zoom) || false},
                pan: {interactive: (params && params.pan) || false},
                xaxis: xFormatterFunc ? { tickFormatter: xFormatterFunc } : undefined,
                yaxis: yFormatterFunc ? { tickFormatter: yFormatterFunc } : undefined,
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
        }
        var style = {
            position: "absolute",
            display: "none",
            border: "1px solid #fdd",
            padding: "2px",
            backgroundColor: "#FFA801",
            opacity: 0.80,
            fontSize: "12px"
        };

        return {
            restrict: 'CA',//Class, Attribute
            terminal: true,
            scope: {
                model: "=metricsPlot",
                width: "@",
                height: "@",
                params: "="
            },
            controller: function ($scope, $element, $attrs) {

                var jPlot = $($element);

                //Setting css width and height if explicitly set
                if($scope.width) { jPlot.css("width", $scope.width); }
                if($scope.height) { jPlot.css("height", $scope.height); }

                $("<div id='metrics-tooltip'></div>").css(style).appendTo("body");

                var plotElem = $.plot(jPlot, [], getOptions());

                jPlot.bind("plothover", function (event, pos, item) {

                    if ($scope.model.length>0) {
                        var posX = metricsFormatters.getFormattedValue($scope.model[0].xFormatter, pos.x, false);
                        var posY = metricsFormatters.getFormattedValue($scope.model[0].yFormatter, pos.y, false);

                        $("#metrics-hoverdata .current").text("(" +
                                posX + ", " + posY + ")");

                        //$log.info('$scope.mousePosition',$scope.mousePosition);
                        if (item) {
                            var xVal = metricsFormatters.getFormattedValue($scope.model[0].xFormatter, item.datapoint[0], false);
                            var yVal = metricsFormatters.getFormattedValue($scope.model[0].yFormatter, item.datapoint[1], false);
                            $("#metrics-tooltip").html("[" + xVal + ", " + yVal + "]")
                                .css({top: item.pageY+5, left: item.pageX+5})
                                .fadeIn(200);
                        } else {
                            $("#metrics-tooltip").hide();
                        }
                    }

                });

                var updatePlotData = function(model, params) {
                    if(model && model.length>0){
                        var options = getOptions(params, model[0].xFormatter, model[0].yFormatter);
                        //$log.info("Plot options:",options);
                        plotElem = $.plot(jPlot, model, options );
                    }else{
                        $log.info('Clear datasets');
                        plotElem = $.plot(jPlot, [], getOptions());
                    }
                };

                $scope.$watch('model', function (newData) {
                   // $log.info('$watch(model)',newData);
                    if(newData.$resolved) {
                        $log.info("Updated plot datasets:");
                        updatePlotData(newData,$scope.params);
                    }
                },false);

                $scope.$watch('params', function (newParams, oldParams) {
                    if(newParams!==oldParams) {
                        $log.info("Updated plot params:", newParams);
                        updatePlotData($scope.model,newParams);
                   }
                }, true);

            },
            replace: true
        };
    })

    .factory("metricsFormatters", function ( $log) {
        var resultService = {
            time: function (val, axis) {
                return moment(new Date(val)).format('DD/MM HH:mm');
            },
            memory: function (bytes, axis) {
                if (!bytes || isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
                    return '-';
                }
                var units = [' B', ' KB', ' MB', ' GB', ' TB', ' PB'],
                    number = Math.floor(Math.log(bytes) / Math.log(1024));
                return (bytes / Math.pow(1024, Math.floor(number))).toFixed(1) + ' ' + units[number];
            },
            getFormattedValue: function(formatterName, value, axis) {
                var result = value;
                if (!!formatterName && !!this[formatterName]) {
                    result = this[formatterName](value, axis);
                }
                return result;
            }
        };

        return resultService;
    })
;
