angular.module('documentationModule', ['coreApp'])

    .controller('documentationController', function ($log, $scope, $http, $location) {
        $log.info('documentationController');

        $scope.documentationUrl = $location.absUrl();
        $http.get('/documentation/menu.json').success(function (response) {
            $scope.menuItems = response;
        });
    })

    .directive('menuBuilder', function() {
        return {
            templateUrl: '/scripts/documentation/menu.html'
        };
    });