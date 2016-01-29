angular.module('documentationModule', ['coreApp'])

    .controller('documentationController', function ($log, $scope, $http, $location) {
        $log.info('documentationController');

        $scope.documentationUrl = $location.absUrl();
        $http.get('/documentation/menu.json').success(function (response) {
            $scope.menuItems = response;
        });

        $scope.scrollToAnchor = function (anchorId) {
            var anchors = document.getElementsByTagName("a");
            for (var i = 0; i < anchors.length; i ++) {
                if (anchors[i].name === anchorId) {
                    window.scrollTo(0, anchors[i].offsetTop - 40);
                    break;
                }
            }
        }
    })

    .directive('menuBuilder', function() {
        return {
            templateUrl: '/scripts/documentation/menu.html'
        };
    });