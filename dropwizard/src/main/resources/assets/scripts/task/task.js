angular.module('taskModule', ['coreApp'])

    .factory('taskRest', function ($log, coreApp, $resource) {
        var restTaskUrl = coreApp.getRestUrl() + 'tasks/';
        return $resource(restTaskUrl + 'task', {}, {
                getDecision: {url: restTaskUrl + 'decision/:processId/:taskId', params: {}},
                getTree: {url: coreApp.getRestUrl() + 'tree/task/:processId/:taskId', params: {}},
                //list
                query: {url: restTaskUrl + 'search', params: {}, isArray: true},
                queryList: {url: restTaskUrl, params: {}},
                queryRepeated: {url: coreApp.getRestUrl() + 'repeatedTasks/', params: {}, isArray: true},
                //dictionaries
                dictionaryState: {url: '/scripts/task/states.json', params: {}, isArray: true}

            }
        );
    })

    .filter('taskState', function (taskRest,coreApp) {
        var states;
        taskRest.dictionaryState(function(list){
            states = coreApp.toObject(list);
        });
        return function (id,field) {
            if(!states){ return '...'; }
            return states[id] ? states[id][field] : states.unknown[field];
        };
    })

    .directive('taskTreeTable', function ($log, coreTree) {
        return {
            restrict: 'A',
            transclude: false,
            scope: {
                taskTreeTable: '=',
                processTask: '='
            },
            templateUrl: '/views/task/tree-table.html',
            replace: false,
            link: function (scope, element, attrs) {
                scope.taskTreeItems = null;
                scope.$watch('taskTreeTable', function (value) {
                    if (value && value.$resolved) {
                        scope.taskTreeItems = coreTree.getFlatArray([value], 'children');
                    }
                }, true);

            }
        };
    })

    .directive('taskForm', function ($log, coreApp) {
        return {
            restrict: 'A',//Element, Class, Attribute
            terminal: true,
            scope: {
                task: "=taskForm",
                types: "=taskTypes"
            },
            controller: function ($scope, $element, $attrs) {
                //  $scope.types = ['DECIDER_START'];

                $scope.argTypes = coreApp.getArgTypes();

                $scope.addArgument = function () {
                    if (!$scope.task.args) {
                        $scope.task.args = [];
                    }
                    $scope.task.args.push({type: $scope.argTypes[0], value: ''});
                };

                $scope.removeArgument = function (idx) {
                    if ($scope.task.args && $scope.task.args.length > 0) {
                        $scope.task.args.splice(idx, 1);
                    }
                };
            },
            templateUrl: "/views/task/form.html",
            replace: true
        };
    })

    .controller('taskListController', function ($log, $scope, taskRest, coreApp) {
        $log.info('taskListController');

        function getRest(params) {
            return params.iterationCount ? taskRest.queryRepeated:
                ((params.taskId || params.processId) ?
                    taskRest.query : taskRest.queryList );
        }

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.tasksResource = getRest(params)(params,
                function success(value) {
                    $scope.tasksModel =  coreApp.parseListModel(value); //cause array or object
                    if($scope.tasksModel){
                        $log.info('Successfully updated tasks page');
                    }else{
                        coreApp.info('Tasks not found',value);
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Tasks page update failed', reason);
                });
        }

        //Initialization:
        $scope.formParams = coreApp.copyStateParams();
        loadModel(angular.copy($scope.formParams));

        //Submit form command:
        $scope.search = function () {
            coreApp.reloadState($scope.formParams);
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });

        //Actions
        $scope.showArgs = function (task) {
            coreApp.openPropertiesModal(task.args, task.taskId);
        };

    })
    .controller('taskCardController', function ($log, $scope, taskRest, coreApp) {
        $log.info('taskCardController');
        $scope.taskParams = coreApp.copyStateParams();

        //Updates tasks  by polling REST resource
        function loadModel() {

            $log.info('Load model', $scope.taskParams);

            $scope.task = taskRest.get($scope.taskParams,
                function success(value) {
                    if(value.taskId) {
                        $log.info('taskCardController: successfully updated task page');
                    }else{
                        coreApp.warn('Task not found by id',$scope.taskParams.taskId);
                    }
                }, function error(reason) {
                    coreApp.error('Task page update failed', reason);
                });

            $scope.taskDecision = taskRest.getDecision($scope.taskParams,
                function success(value) {
                    $log.info('taskCardController: successfully updated task page');
                }, function error(reason) {
                    coreApp.error('Tasks decision update failed', reason);
                });

            $scope.taskTree = taskRest.getTree($scope.taskParams,
                function success(value) {
                    $log.info('taskCardController: successfully updated task page');
                }, function error(reason) {
                    coreApp.error('Task tree update failed', reason);
                });
        }

        //Initialization:
        loadModel();

    });
