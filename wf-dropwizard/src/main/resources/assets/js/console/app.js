var consoleApp = angular.module("consoleApp", ['console.services', 'console.controllers', 'console.animation']);

consoleApp.config(function($routeProvider, $locationProvider) {

    $routeProvider.when('/home', {
        templateUrl: '/partials/view/home.html',
        controller: "homeController"
    });

    $routeProvider.when('/actors', {
        templateUrl: '/partials/view/actors.html',
        controller: "actorsController"
    });

    $routeProvider.when('/queues', {
        templateUrl: '/partials/view/queues.html',
        controller: "queueListController"
    });

    $routeProvider.when('/queues/queue/:queueName', {
        templateUrl: '/partials/view/queue.html',
        controller: "queueContentController"
    });

    $routeProvider.when('/processes', {
        templateUrl: '/partials/view/processes.html',
        controller: "processesController"
    });

    $routeProvider.when('/tasks', {
        templateUrl: '/partials/view/tasks.html',
        controller: "taskListController"
    });

    $routeProvider.when('/about', {
        templateUrl: '/partials/view/about.html',
        controller: "aboutController"
    });

    $routeProvider.when('/tasks/task/:id', {
        templateUrl: '/partials/view/task.html',
        controller: "taskController"
    });

    $routeProvider.otherwise({
        redirectTo: '/home'
    });

    // configure html5 to get links working on jsfiddle
    //TODO: causes troubles on page refresh
//    $locationProvider.html5Mode(true);

});

