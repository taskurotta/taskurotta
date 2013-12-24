angular.module("console.directives", ['ngRoute'])

.directive('tskPaginator', ['$cookieStore', function ($cookieStore) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {
            genericPage: "=genericPage",
            updateAction: "&updateAction",
            store: "@"
        },
        controller: ['$scope', '$element', '$attrs', '$transclude', function ($scope, $element, $attrs, $transclude) {


            if ($scope.store && $scope.store.length>0) {
                $scope.$watch('genericPage.pageNumber', function() {
                    $cookieStore.put($scope.store, {
                        pageNumber: $scope.genericPage.pageNumber,
                        pageSize: $scope.genericPage.pageSize
                    });
                });
                $scope.$watch('genericPage.pageSize', function() {
                    $cookieStore.put($scope.store, {
                        pageNumber: $scope.genericPage.pageNumber,
                        pageSize: $scope.genericPage.pageSize
                    });
                });
            }

            $scope.updatePageSize = function () {

                //If user is on the last page and change pageSize, we can get situation when current page number > total pages count
                if ($scope.genericPage.pageNumber > $scope.totalPages()) {
                    $scope.genericPage.pageNumber = $scope.totalPages();
                }

                $scope.updateAction();
            }

            //Show previous page
            $scope.prevPage = function () {
                if ($scope.isPrevDisabled()) {
                    return;
                }
                if ($scope.genericPage.pageNumber > 1) {
                    $scope.genericPage.pageNumber--;
                }

                $scope.updateAction();
            };

            $scope.totalPages = function () {
                var reminder = $scope.genericPage.totalCount % $scope.genericPage.pageSize;
                var pagesCount = Math.floor($scope.genericPage.totalCount / $scope.genericPage.pageSize);
                if (reminder > 0) {
                    pagesCount++;
                }
                return pagesCount
            };

            $scope.getMinIndex = function () {
                var minIndex = ($scope.genericPage.pageNumber - 1) * $scope.genericPage.pageSize + 1;
                if ($scope.genericPage.totalCount <= 0) {
                    minIndex = 0;
                }
                return minIndex;
            };

            $scope.getMaxIndex = function () {
                var maxIndex = $scope.genericPage.totalCount;
                if ($scope.genericPage.pageNumber < $scope.totalPages()) {
                    maxIndex = $scope.genericPage.pageNumber * $scope.genericPage.pageSize;
                }
                return maxIndex
            };

            //Show next page
            $scope.nextPage = function () {
                if ($scope.isNextDisabled()) {
                   return;
                }
                if ($scope.genericPage.pageNumber < $scope.totalPages()) {
                    $scope.genericPage.pageNumber++;
                }

                $scope.updateAction();
            };

            $scope.firstPage = function () {
                if ($scope.isPrevDisabled()) {
                    return;
                }
                $scope.genericPage.pageNumber = 1;
                $scope.updateAction();
            };

            $scope.lastPage = function () {
                if ($scope.isNextDisabled()) {
                    return;
                }
                $scope.genericPage.pageNumber = $scope.totalPages();
                $scope.updateAction();
            };

            $scope.isNextDisabled = function() {
                return (
                    $scope.genericPage.pageNumber >= $scope.totalPages()
                );
            };

            $scope.isPrevDisabled = function() {
                return $scope.genericPage.pageNumber == 1;
            };

        }],
        templateUrl: "/partials/widget/paginator_bar.html",
        replace: true
    };
}])

.directive('tskAutorefresh', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {
            updateAction: "&",
            refreshRate: "="
        },
        controller: ['$scope', '$element', '$attrs', '$transclude', '$$timeUtil', function ($scope, $element, $attrs, $transclude, $$timeUtil) {

            $scope.refreshRates = [0, 1, 3, 5, 10];

            //Auto refresh feature. triggers auto refreshing on refresh rate changes
            var currentRefreshIntervalId = -1;
            $scope.$watch('refreshRate', function (value) {
                if (currentRefreshIntervalId > 0) {
                    $$timeUtil.clearInterval(currentRefreshIntervalId);
                }
                if ($scope.refreshRate > 0) {
                    currentRefreshIntervalId = $$timeUtil.setInterval($scope.updateAction, $scope.refreshRate * 1000, $scope);//Start autoUpdate
                }
            }, true);

        }],
        templateUrl: "/partials/widget/auto_refresher.html",
        replace: true
    };
}])
// <ol>
//     <li ng-repeat="child in data.children" tree>
//       <div>{{child.name}}</div>
//       <ol><branch></ol>
// </ol>
//https://gist.github.com/furf/4331090
.directive('tskTree', ['$compile', function ($compile) {
    'use strict';
    return {
        restrict: 'A',
        compile: function (tElement, tAttrs) {

            var branch = tElement.find('branch'),
                repeatExpr,
                childExpr,
                childrenExpr;

            if (!branch.length) {
                throw new Error('tree directive must contain a branch node.');
            }

            repeatExpr = (branch.attr('branch') || tAttrs.ngRepeat).match(/^(.*) in (?:.*\.)?(.*)$/);
            childExpr = repeatExpr[1];
            childrenExpr = repeatExpr[2];
            tElement.attr('ng-repeat', childExpr + ' in ' + childExpr + '.' + childrenExpr);

            return function link(scope, element) {

                scope.$depth = scope.$depth || 0;
                scope.$watch(childExpr, function (child) {

                    var childScope = scope.$new();

                    childScope[childrenExpr] = child[childrenExpr];
                    childScope.$depth = scope.$depth + 1;

                    element.find('branch').replaceWith($compile(tElement.clone())(childScope));
                });
            };
        }
    };
}])


.directive('tskCreateProcessForm', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {},
        controller: ['$scope', '$element', '$attrs', '$transclude', '$$timeUtil', '$window', '$log', '$http', function ($scope, $element, $attrs, $transclude, $$timeUtil, $window, $log, $http) {

            $scope.process = {
                actorId: "",
                method: "",
                type: "DECIDER_START"
            };

            $scope.argsJson = "[]";

            $scope.create = function () {
                $scope.process.args = angular.fromJson($scope.argsJson);
                $log.log("create process: " + angular.toJson($scope.process));
                $http.post("/rest/tasks/start?generateId=true", $scope.process);
            };

        }],
        templateUrl: "/partials/widget/start_process.html",
        replace: true
    };
}])

.directive('tskTaskForm', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {},
        controller: ['$scope', '$element', '$attrs', '$transclude', '$window', '$log', '$http', function ($scope, $element, $attrs, $transclude, $window, $log, $http) {
            $scope.taskTypes = ["DECIDER_START", "WORKER", "DECIDER_ASYNCHRONIOUS"];

            $scope.processUUID = "process-uuid";
            $scope.taskUUID = "task-uuid";

            $scope.actorMethod = "actor-method";
            $scope.actorId = "actor-id";
            $scope.taskType = "DECIDER_START";
            $scope.taskStartTime = new Date();

            $scope.args = [];
            $scope.options = {};


        }],
        templateUrl: "/partials/widget/task_container_form.html",
        replace: true
    };
}])

.directive('tskArgForm', ['$http', '$compile', function ($http, $compile) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {
            arg: "="
        },
        template: "<li></li>",
        link: function (scope, element, attrs) {
            if (angular.isArray(scope.arg.compositeValue)) {
                element.append("<tsk-arg-list-form args='arg.compositeValue'></tsk-arg-list-form>");
                $compile(element.contents())(scope)
            }
        },
        replace: true
    };
}])

.directive('tskArgListForm', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {
            args: "="
        },
        template: "<ul><tsk-arg-form ng-repeat='arg in args' arg='arg'></tsk-arg-form></ul>",
        replace: true
    };
}])

.directive('tskPlot', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {//'=' enables ability to $watch
            datasetUrl: "=",
            width: "@",
            height: "@",
            updatePeriod: "=",
            holder: "="
        },
        controller: ['$scope', '$element', '$attrs', '$transclude', '$window', '$log', '$http', '$$timeUtil', function ($scope, $element, $attrs, $transclude, $window, $log, $http, $$timeUtil) {

            var dateTimeUtils = {
                withLeadingZero: function(number) {
                    if (number<10) {
                        return "0"+number;
                    } else {
                        return number;
                    }
                },
                getShortDate: function(date) {
                    if (date) {
                        return this.withLeadingZero(date.getDate()) + "/" + this.withLeadingZero(date.getMonth()+1);
                    } else {
                        return "";
                    }
                },
                getTimeStr: function(time) {
                    if (time) {
                        return this.withLeadingZero(time.getHours()) + ":" + this.withLeadingZero(time.getMinutes());
                    } else {
                        return "";
                    }
                }
            };

            var formatters = {
                time: function(val, axis) {
                    var date = new Date(val);
                    return dateTimeUtils.getShortDate(date) + " " + dateTimeUtils.getTimeStr(date);
                },
                memory: function (bytes, axis) {
                    var kilobyte = 1024;
                    var megabyte = kilobyte * 1024;
                    var gigabyte = megabyte * 1024;
                    var terabyte = gigabyte * 1024;

                    if ((bytes >= 0) && (bytes < kilobyte)) {
                        return bytes + ' B';

                    } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
                        return (bytes / kilobyte).toFixed(2) + ' KB';

                    } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
                        return (bytes / megabyte).toFixed(2) + ' MB';

                    } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
                        return (bytes / gigabyte).toFixed(2) + ' GB';

                    } else if (bytes >= terabyte) {
                        return (bytes / terabyte).toFixed(2) + ' TB';

                    } else {
                        return bytes + ' B';
                    }
                }
            };

            var jPlot = $($element);
            //Setting css width and height if explicitly set
            if($scope.width) { jPlot.css("width", $scope.width); }
            if($scope.height) { jPlot.css("height", $scope.height); }

            var defaultOptions = {
                legend: {show: true},
                series: {
                    lines: {show: true},
                    points: {show: true}
                },
                yaxis: {
                    ticks: 5
                },
                zoom: {interactive: true},
                pan: {interactive: true}
            };

            var plotElem = $.plot(jPlot, [], defaultOptions);

            var refreshTriggerId = -1;
            var updatePlotData = function(newData) {
                $log.info("Update plot data. New datasets count: " + newData.length);
                if($scope.holder) {
                    $scope.holder = newData;
                }
                var newOptions = $.extend({}, defaultOptions);

                if (newData.length>0) {
                    if (!!newData[0].yFormatter && !!formatters[newData[0].yFormatter]) {
                        newOptions = $.extend(newOptions, {
                            yaxis: {
                                tickFormatter: formatters[newData[0].yFormatter],
                                ticks: newData[0].yTicks
                            }
                        });
                    }

                    if (!!newData[0].xFormatter && !!formatters[newData[0].xFormatter]) {
                        newOptions = $.extend(newOptions, {
                            xaxis: {
                                tickFormatter: formatters[newData[0].xFormatter],
                                ticks: newData[0].xTicks
                            }
                        });
                    }
                }

                plotElem = $.plot(jPlot, newData, newOptions);
            };

            $scope.update = function() {
                $http.get($scope.datasetUrl).then(function(value) {
                    updatePlotData(value.data);
                });
            };

            $scope.$watch('updatePeriod', function(newVal, oldVal) {
                if(newVal > 0){
                    refreshTriggerId = $$timeUtil.setInterval($scope.update, newVal * 1000, $scope);//Start autoUpdate
                } else {
                    $$timeUtil.clearInterval(refreshTriggerId);
                }
            });

            $scope.$watch('datasetUrl', function(newVal, oldVal) {
                if(newVal) {
                    $scope.update();
                } else {
                    updatePlotData([]);//reset plot data
                }
            });

        }],
        replace: true
    };
}])

.directive('tskErrMessage', ['$http', function ($http) {
    return {
        restrict: 'ECA',//Element, Class, Attribute
        terminal: true,
        scope: {
            model: "="
        },
        controller: ['$scope', '$element', '$attrs', '$transclude', '$window', '$log', '$http', function ($scope, $element, $attrs, $transclude, $window, $log, $http) {
            $scope.detail = false;

            $scope.getCollapseIconClassName = function(visible) {
                if (visible) {
                    return "icon-chevron-up";
                } else {
                    return "icon-chevron-down";
                }
            };

            $scope.getCollapseIconText = function(visible) {
                if (visible) {
                    return "Hide details";
                } else {
                    return "Show details";
                }
            };

        }],
        templateUrl: "/partials/widget/error_message.html",
        replace: true
    };
}]);



