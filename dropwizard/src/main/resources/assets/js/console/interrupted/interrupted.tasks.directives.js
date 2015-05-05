angular.module("console.interrupted.directives", ['console.interrupted.services', 'console.util.services', 'ui.bootstrap.modal'])

    .directive('tskBrokenProcessList', ['tskUtil', '$log', '$modal', 'tskBrokenProcessesActions',  function(tskUtil, $log, $modal, tskBrokenProcessesActions) {

        return {
            restrict: 'ECA',//Element, Class, Attribute
            terminal: true,
            scope: {
                tasks: "=model"
            },
            controller: ['$scope', '$element', '$attrs', '$transclude', function ($scope, $element, $attrs, $transclude) {

                $scope.enableNewLine = function (word) {
                    return tskUtil.injectNewLineDelimiter(word, 25, '.');
                };

                $scope.showStackTrace = function (stackTrace) {
                    //$log.log("Showing stacktrace : " + stackTrace);
                    var modalInstance = $modal.open({
                        templateUrl: '/partials/view/modal/stacktrace_msg.html',
                        windowClass: 'stack-trace',
                        controller: function ($scope) {
                            $scope.stackTrace = stackTrace;
                        }
                    });

                    modalInstance.result.then(function(okMess) {
                        //do nothing
                    }, function(cancelMsg) {
                        //do nothing
                    });
                };

                $scope.submitRestart = function (itdTask, $index) {
                    $log.log("Submitting restart for taskId["+itdTask.taskId+"], processId["+itdTask.processId+"]index ["+$index+"]");
                    tskBrokenProcessesActions.restartTask(itdTask).then(function(okResp) {
                        $scope.tasks.splice($index, 1);

                    }, function(errResp){
                        $scope.feedback = errResp;
                    });
                };

            }],
            templateUrl: "/partials/widget/broken/interrupted_tasks_list.html",
            replace: true
        };

    }])

    .directive('tskBrokenFilterView', ['$http', function($http) {

        return {
            restrict: 'ECA',//Element, Class, Attribute
            terminal: true,
            scope: {
                filter: "=",
                updateAction: "&"
            },
            controller: ['$scope', '$element', '$attrs', '$transclude', 'tskBpTextProvider', '$log',  function ($scope, $element, $attrs, $transclude, tskBpTextProvider, $log) {

                $scope.getGroupLabel = function(name) {
                    return tskBpTextProvider.getGroupLabel(name);
                };

                $scope.getGroupShortLabel = function(name) {
                    return tskBpTextProvider.getGroupShortLabel(name);
                };

                $scope.remove = function(idx, type) {
                    $scope.filter.splice(idx, 1);
                    $scope.updateAction();
                };


            }],
            templateUrl: "/partials/widget/broken/applied_filter_view.html",
            replace: true
        };

    }])

    .directive('tskPeriodSelect', ['$log', function($log) {

        return {
            restrict: 'ECA',//Element, Class, Attribute
            terminal: true,
            scope: {
                period: "=model"
            },
            controller: ['$scope', '$element', '$attrs', '$transclude', '$log', '$timeout',  function ($scope, $element, $attrs, $transclude, $log, $timeout) {
                $scope.fromOpened = false;
                $scope.toOpened = false;
                $scope.maxPossibleDate = $scope.period.maxDate;

                $scope.toggleOpenFrom = function() {
                    $timeout(function() {
                        $scope.fromOpened = !$scope.fromOpened;
                    });
                };

                $scope.toggleOpenTo = function() {
                    $timeout(function() {
                        $scope.toOpened = !$scope.toOpened;
                    });
                };

            }],
            templateUrl: "/partials/widget/broken/period_filter.html",
            replace: true
        };

    }]);




