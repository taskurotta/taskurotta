var consoleApp = angular.module("consoleApp", ['console.services', 'console.controllers', 'console.animation', 'console.directives']);

consoleApp.config(function ($routeProvider, $locationProvider) {

    $routeProvider.when('/home', {
        templateUrl: '/partials/view/home.html',
        controller: "homeController"
    });

    //queues
    $routeProvider.when('/queues', {
        templateUrl: '/partials/view/queues.html',
        controller: "queueListController"
    });

    $routeProvider.when('/queues/queue/:queueName', {
        templateUrl: '/partials/view/queue.html',
        controller: "queueCardController"
    });

    //processes
    $routeProvider.when('/processes', {
        templateUrl: '/partials/view/processes.html',
        controller: "processListController"
    });
    $routeProvider.when('/processes/process/:processId', {
        templateUrl: '/partials/view/process.html',
        controller: "processCardController"
    });
    $routeProvider.when('/processes/search/:type/', {
        templateUrl: '/partials/view/process_search.html',
        controller: "processSearchController"
    });
    $routeProvider.when('/processes/search/:type/:id', {
        templateUrl: '/partials/view/process_search.html',
        controller: "processSearchController"
    });

    //tasks
    $routeProvider.when('/tasks', {
        templateUrl: '/partials/view/tasks.html',
        controller: "taskListController"
    });

    $routeProvider.when('/tasks/search/:type', {
        templateUrl: '/partials/view/task_search.html',
        controller: "taskSearchController"
    });

    $routeProvider.when('/tasks/search/:type/:id', {
        templateUrl: '/partials/view/task_search.html',
        controller: "taskSearchController"
    });

    $routeProvider.when('/tasks/task/:processId/:taskId', {
        templateUrl: '/partials/view/task.html',
        controller: "taskCardController"
    });

    //profiles
    $routeProvider.when('/monitoring/profiles', {
        templateUrl: '/partials/view/profiles.html',
        controller: "profilesController"
    });

    $routeProvider.when('/monitoring/hoveringQueues', {
        templateUrl: '/partials/view/hoveringQueues.html',
        controller: "hoveringQueuesController"
    });

    $routeProvider.when('/monitoring/repeatedTasks', {
        templateUrl: '/partials/view/repeatedTasks.html',
        controller: "repeatedTasksController"
    });

//unused
    $routeProvider.when('/about', {
        templateUrl: '/partials/view/about.html',
        controller: "aboutController"
    });
    $routeProvider.when('/actors', {
        templateUrl: '/partials/view/actors.html',
        controller: "actorsController"
    });

    $routeProvider.otherwise({
        redirectTo: '/home'
    });

    // configure html5 to get links working on jsfiddle
    //TODO: causes troubles on page refresh
//    $locationProvider.html5Mode(true);

});

consoleApp.run(function ($rootScope, $log) {
    $rootScope.getStartIndex = function (pageNum, pageSize) {
        return (pageNum - 1) * pageSize + 1;
    };
});