angular.module('interruptedModule', ['taskModule', 'coreApp'])

    .factory('interruptedRest', function ($log, coreApp, $resource) {
        var restTaskUrl = coreApp.getRestUrl() + 'process/tasks/interrupted/';

        return $resource(restTaskUrl + 'task', {}, {
                getStacktrace: {url: restTaskUrl + 'stacktrace', params: {}},
                //list
                queryGroup: {url: restTaskUrl + 'group', params: {}, isArray: true},
                query: {url: restTaskUrl + 'list', params: {}, isArray: true},
                //actions
                restart: {url: restTaskUrl + 'restart/task', method:'POST', params: {}},
                restartGroup: {url: restTaskUrl + 'restart/group', method:'POST', params: {
                    errorClassName:'@exception'
                }},
                abortGroup: {url: restTaskUrl + 'abort/group', method:'POST', params: {
                    errorClassName:'@exception'
                }},

                //dictionaries
                dictionaryGroup: {url: '/scripts/interrupted/groups.json', params: {}, isArray: true, cache: true}

            }
        );
    })

    .controller('interruptedController', function ($log, $scope, interruptedRest, coreApp, $state,$stateParams) {
        $log.info('interruptedController');


        function getRest() {
            return $state.is('interrupted')? interruptedRest.queryGroup : interruptedRest.query;
        }

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);

            $scope.interruptedResource = getRest()(params,
                function success(value) {
                    $scope.interruptedModel = coreApp.parseListModel(value);//cause array or object
                    if($scope.interruptedModel){
                        $log.info('Successfully updated interrupted tasks page');
                    }else{
                        coreApp.info('Not found any interrupted tasks');
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

            function getRestDateFormat(date, withTime) {
                return (angular.isDate(date) ? moment(date) : moment(date, moment.ISO_8601))
                    .format('DD.MM.YYYY' + (withTime ? ' HH:mm' : ''));
            }

            loadModel(angular.extend({},$scope.formParams,{
                dateFrom: getRestDateFormat($scope.formParams.dateFrom, $scope.formParams.withTime, true),
                dateTo: getRestDateFormat($scope.formParams.dateTo, $scope.formParams.withTime, true),
                withTime: undefined
            }));

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

            coreApp.reloadState(angular.extend({},$scope.formParams,{
               // pageNum: undefined,
                refreshRate: undefined,
                dateFrom: getIsoDateFormat($scope.formParams.dateFrom, $scope.formParams.withTime),
                dateTo: getIsoDateFormat($scope.formParams.dateTo, $scope.formParams.withTime)
            }));
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
                    var params = $scope.joinFilterParam(angular.copy($scope.resourceParams),group.name);
                    $log.info('restartGroup', params);
                    interruptedRest.restartGroup(params, function success() {
                        $log.log('Tasks of group ' + group.name + ' have been restarted');
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error task group '+group.name +' restarting', reason);
                    });
                });
        };

        $scope.abortGroup = function (group) {
            coreApp.openConfirmModal('Tasks of group '+group.name +' will be aborted.',
                function confirmed() {
                    var params = $scope.joinFilterParam(angular.copy($scope.resourceParams),group.name);
                    $log.info('abortGroup', params);
                    interruptedRest.abortGroup(params, function success() {
                        $log.log('Tasks of group ' + group.name + ' have been aborted');
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error task group '+group.name +' aborting', reason);
                    });
                });
        };

        $scope.restart = function (task) {
            coreApp.openConfirmModal('Task [' + task.taskId + '] will be restarted.',
                function confirmed() {
                    interruptedRest.restart({
                        taskId: task.taskId,
                        processId: task.processId
                    }, function success() {
                        $log.log('Task [' + task.taskId + '] have been restarted');
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error task [' + task.taskId + '] restarting', reason);
                    });
                });
        };

    });
