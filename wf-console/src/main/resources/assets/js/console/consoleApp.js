var consoleApp = angular.module("console_app", []);

consoleApp.controller("bodyController", function($rootScope, $scope, $location, $log) {
    var currentUrl = "";

    $scope.isActiveTab = function(tabName){
        var result = "";
        if(tabName == currentUrl) result =  "active";
        return result;
    };

    $rootScope.$watch(function(){return $location.url()}, function(value) {
        currentUrl = value;
    }, true);

});

consoleApp.controller("homeController", function($scope) {
});

consoleApp.controller("actorsController", function($scope) {
});

consoleApp.controller("queuesController", function($scope) {
});

consoleApp.controller("processesController", function($scope) {
});

consoleApp.config(function($routeProvider, $locationProvider) {

    $routeProvider.when('/console/home', {
        templateUrl: '/console/partials/view/home.html',
        controller: "homeController"
    });

    $routeProvider.when('/console/actors', {
        templateUrl: '/console/partials/view/actors.html',
        controller: "actorsController"
    });

    $routeProvider.when('/console/queues', {
        templateUrl: '/console/partials/view/queues.html',
        controller: "queuesController"
    });

    $routeProvider.when('/console/processes', {
        templateUrl: '/console/partials/view/processes.html',
        controller: "processesController"
    });

    $routeProvider.otherwise({
        redirectTo: '/console/home'
    });

    // configure html5 to get links working on jsfiddle
    //TODO: doesn't work on page refresh
//    $locationProvider.html5Mode(true);

});

