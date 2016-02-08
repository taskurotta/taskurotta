angular.module('documentationModule', ['coreApp'])

    .controller('documentationController', function ($scope, $rootScope, $http, $location, coreApp) {

        $scope.documentationUrl = $location.absUrl();
        $http.get('/documentation/menu.json').success(function (response) {
            $scope.menuItems = response;
        });

        $scope.scrollToAnchor = function (anchorId) {
            var anchors = document.getElementsByTagName("a");
            for (var i = 0; i < anchors.length; i++) {
                if (anchors[i].name === anchorId) {
                    window.scrollTo(0, anchors[i].offsetTop - 40);
                    break;
                }
            }

        };

        $scope.formParams = coreApp.copyStateParams();

        $rootScope.$on("$includeContentLoaded", function(event, templateName){
            if ($scope.formParams.anchor) {
                $scope.scrollToAnchor($scope.formParams.anchor);
            }
        });

    })

    .directive('menuBuilder', function () {
        return {
            templateUrl: '/views/documentation/menu.html'
        };
    });