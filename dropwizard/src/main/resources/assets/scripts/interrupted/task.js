angular.module('interruptedModule', ['taskModule', 'coreApp'])

    .factory('interruptedRest', function ($log, coreApp, $resource) {
        var restTaskUrl = coreApp.getRestUrl() + 'process/tasks/interrupted/';

        return $resource(restTaskUrl + 'task', {}, {
                getStacktrace: {url: restTaskUrl + 'stacktrace', params: {}},
                //list
                query: {url: restTaskUrl + ':query', params: {}, isArray: true},
                //actions
                restart: {url: restTaskUrl + 'restart', params: {}},
                //dictionaries
                dictionaryGroup: {url: '/scripts/interrupted/groups.json', params: {}, isArray: true, cache: true}

            }
        );
    })

    .controller('interruptedController', function ($log, $scope, interruptedRest, coreApp, $state,$stateParams) {
        $log.info('interruptedController');

        function getRestDateFormat(date, withTime) {
            return (angular.isDate(date) ? moment(date) : moment(date, moment.ISO_8601))
                .format('DD.MM.YYYY' + (withTime ? ' HH:mm' : ''));
        }

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);

            $scope.interruptedResource = interruptedRest.query(params,
                function success(value) {
                    $scope.interruptedModel = coreApp.parseListModel(value);//cause array or object
                    if($scope.interruptedModel){
                        $log.info('Successfully updated interrupted tasks page');
                    }else{
                        coreApp.warn('Not found any interrupted tasks',value);
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Interrupted tasks page update failed',reason);
                });
        }

        //Initialization:
        $scope.$stateParams = $stateParams;
        $scope.formParams = coreApp.copyStateParams();

        interruptedRest.dictionaryGroup({}, function success(groups) {
            $scope.groups = coreApp.toObject(groups);
            $scope.groups.starter.selected = $stateParams.starterId || $stateParams.group === 'starter';
            $scope.groups.actor.selected = $stateParams.actorId || $stateParams.group === 'actor';
            $scope.groups.exception.selected = $stateParams.exception || $stateParams.group === 'exception';

            //mark selected filter or group
            var params = angular.copy($scope.formParams);
            params.query = $state.is('interrupted')? 'group':'list';
            params.dateFrom = getRestDateFormat(params.dateFrom, params.withTime, true);
            params.dateTo = getRestDateFormat(params.dateTo, params.withTime, true);
            delete params.withTime;

            loadModel(params);

            $scope.joinFilterParam = function (params, value) {
                var param = $scope.groups[$scope.resourceParams.group].param;
                params[param] = value;
                return params;
            };

        });

        //Submit form command:
        $scope.search = function () {
            function getIsoDateFormat(date, withTime) {
                return (angular.isDate(date) ? moment(date) : moment(date, moment.ISO_8601))
                    .format('YYYY-MM-DD' + (withTime ? 'THH:mm' : ''));
            }
            var params = angular.copy($scope.formParams);
            params.dateFrom = getIsoDateFormat(params.dateFrom, params.withTime);
            params.dateTo = getIsoDateFormat(params.dateTo, params.withTime);
            coreApp.reloadState(params);
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });

        //Actions
        $scope.showStackTrace = function (task) {
            interruptedRest.getStacktrace({taskId:task.taskId,processId:task.processId},
                function success(value) {
                    coreApp.openStacktraceModal(value.msg, 'StackTrace');
                }, function error(reason) {
                    coreApp.error('Interrupted tasks StackTrace not found',reason);
                });
        };

        $scope.showMessage = function (task) {
            coreApp.openStacktraceModal(task.errorMessage, 'Message');
        };

        $scope.restartGroup = function (group) {
            coreApp.openConfirmModal('Tasks of group '+group.name +' will be restarted.',
                function confirmed() {
                    interruptedRest.restart({
                        restartIds: _.map(group.tasks, function (item) {
                            return {
                                taskId: item.taskId,
                                processId: item.processId
                            };
                        })
                    }, function success() {
                        $log.log('Tasks of group ' + group.name + ' have been restarted');
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error task group '+group.name +' restarting', reason);
                    });
                });
        };

        $scope.restart = function (task) {
            coreApp.openConfirmModal('Task [' + task.taskId + '] will be restarted.',
                function confirmed() {
                    interruptedRest.restart({
                        restartIds: [{
                            taskId: task.taskId,
                            processId: task.processId
                        }]
                    }, function success() {
                        $log.log('Task [' + task.taskId + '] have been restarted');
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error task [' + task.taskId + '] restarting', reason);
                    });
                });
        };

    });
