angular.module("console.broken.process.directives", ['console.broken.process.services'])

    .directive('tskBrokenProcessList', ['$http', function($http) {

        return {
            restrict: 'ECA',//Element, Class, Attribute
            terminal: true,
            scope: {
                processes: "=model"
            },
            controller: ['$scope', '$element', '$attrs', '$transclude', function ($scope, $element, $attrs, $transclude) {

            }],
            templateUrl: "/partials/widget/broken/broken_process_list.html",
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

    }]);




