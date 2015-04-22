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


                if ($scope.store && $scope.store.length > 0) {
                    $scope.$watch('genericPage.pageNumber', function () {
                        $cookieStore.put($scope.store, {
                            pageNumber: $scope.genericPage.pageNumber,
                            pageSize: $scope.genericPage.pageSize
                        });
                    });
                    $scope.$watch('genericPage.pageSize', function () {
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

                $scope.isNextDisabled = function () {
                    return (
                        $scope.genericPage.pageNumber >= $scope.totalPages()
                        );
                };

                $scope.isPrevDisabled = function () {
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
            controller: ['$scope', '$element', '$attrs', '$transclude', 'tskTimeUtil', function ($scope, $element, $attrs, $transclude, tskTimeUtil) {

                $scope.refreshRates = [0, 1, 3, 5, 10];

                //Auto refresh feature. triggers auto refreshing on refresh rate changes
                var currentRefreshIntervalId = -1;
                $scope.$watch('refreshRate', function (value) {
                    if (currentRefreshIntervalId > 0) {
                        tskTimeUtil.clearInterval(currentRefreshIntervalId);
                    }
                    if ($scope.refreshRate > 0) {
                        currentRefreshIntervalId = tskTimeUtil.setInterval($scope.updateAction, $scope.refreshRate * 1000, $scope);//Start autoUpdate
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

    .directive('tskTaskForm', ['$http', function ($http) {
        return {
            restrict: 'ECA',//Element, Class, Attribute
            terminal: true,
            scope: {
                model: "=",
                types: "="
            },
            controller: ['$scope', '$element', '$attrs', function ($scope, $element, $attrs) {

                $scope.addArgument = function() {
                    if (!$scope.model.args) {
                        $scope.model.args = [];
                    }
                    $scope.model.args.push({
                        type: "string",
                        value: ""
                    });
                };

                $scope.removeArgument = function(idx) {
                    if (!!$scope.model.args && $scope.model.args.length>0) {
                        $scope.model.args.splice(idx, 1);
                    }
                };

            }],
            templateUrl: "/partials/widget/containers/task_command_form.html",
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

    .directive('tskErrMessage', ['$http', function ($http) {
        return {
            restrict: 'ECA',//Element, Class, Attribute
            terminal: true,
            scope: {
                model: "="
            },
            controller: ['$scope', '$element', '$attrs', '$transclude', '$window', '$log', '$http', function ($scope, $element, $attrs, $transclude, $window, $log, $http) {
                $scope.detail = false;

                $scope.getCollapseIconClassName = function (visible) {
                    if (visible) {
                        return "icon-chevron-up";
                    } else {
                        return "icon-chevron-down";
                    }
                };

                $scope.getCollapseIconText = function (visible) {
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
    }])

    .directive('tskArgument', ['$log', function ($log) {
        return {
            require: 'ngModel',
            restrict: 'EA',//Element, Attribute
            scope: {//'=' enables ability to $watch
                model: "=ngModel"
            },
            link: function ($scope, $element, $attrs, ngModelCtrl) {
                $scope.types = ["string", "boolean", "integer", "double", "long", "null"];
            },
            templateUrl: '/partials/widget/schedule/tsk_argument.html'
        };
    }])

    .directive('tskTime', ['$log', function ($log) {
        return {
            require: 'ngModel',
            restrict: 'EA',//Element, Attribute
            scope: {//'=' enables ability to $watch
                model: "=ngModel"
            },
            link: function ($scope, $element, $attrs, ngModelCtrl) {
                var tp = $element.timepicker({
                    minuteStep: 1,
                    showMeridian: false
                });

                tp.on('changeTime.timepicker', function(e) {
                    ngModelCtrl.$setViewValue({
                        hour: e.time.hour,
                        minute: e.time.minute
                    });
                });

            },
            templateUrl: '/partials/widget/tsk_time.html'
        };
    }])
;



