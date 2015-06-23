angular.module('scheduleModule', ['taskModule', 'coreApp'])

    .factory('scheduleRest', function ($log, coreApp, $resource) {
        var restScheduleUrl = coreApp.getRestUrl() + 'schedule/';
        var rawInterceptor = coreApp.getRawInterceptor();
        function stringTransformResponse(data, headersGetter, status) {
            return data;
        }

        return $resource(restScheduleUrl + 'card', {}, {
                getNodeCount: {url: restScheduleUrl + 'node_count', interceptor:rawInterceptor },
                //list
                query: {url: restScheduleUrl + 'list', params: {}, isArray: false},
                //actions
                create: {url: restScheduleUrl + 'create', method: 'PUT', params: {}},
                update: {url: restScheduleUrl + 'update', method: 'PUT', params: {}},

                validate: {url: restScheduleUrl + 'validate/cron',
                    transformResponse: stringTransformResponse, interceptor: rawInterceptor},
                activate: {url: restScheduleUrl + 'action/activate', method: 'POST', params: {}},
                deactivate: {url: restScheduleUrl + 'action/deactivate', method: 'POST', params: {}},
                delete: {url: restScheduleUrl + 'action/delete', method: 'POST', params: {}},
                //dictionaries
                dictionaryState: {url: '/scripts/schedule/states.json', params: {}, isArray: true, cache: true}
            }
        );
    })

    .filter('scheduleState', function (scheduleRest,coreApp) {
        var states;
        scheduleRest.dictionaryState(function(list){
            states = coreApp.toObject(list);
        });
        return function (id,field) {
            if(!states){ return '...'; }
            return states[id] ? states[id][field] : states.unknown[field];
        };
    })

    .controller('scheduleListController', function ($log, $scope, scheduleRest,coreRest, coreApp) {
        $log.info('scheduleListController');

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.schedulesResource = scheduleRest.query(params,
                function success(value) {
                    $scope.schedulesModel =  coreApp.parseListModel(value);//cause array or object
                    if($scope.schedulesModel){
                        $log.info('Successfully updated scheduled tasks page');
                    }else{
                        coreApp.info('Scheduled tasks not found');
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Schedules page update failed',reason);
                });

            scheduleRest.getNodeCount({} ,
                function success(value) {
                    $log.info('Successfully updated NodeCount',value);
                    $scope.total = value;
                }, function error(reason) {
                    coreApp.error('Cannot get node count',reason);
                });

            coreRest.getTime({} ,
                function success(value) {
                    $log.info('Successfully updated serverTime',value);
                    $scope.serverTime = value;
                }, function error(reason) {
                    coreApp.error('Cannot update server time',reason);
                });
        }

        //Initialization:
        $scope.formParams = coreApp.copyStateParams();
        loadModel(angular.copy($scope.formParams));

        //Submit form command:
        $scope.search = function () {
            //$scope.formParams.pageNum = undefined;
            $scope.formParams.refreshRate = undefined;
            coreApp.reloadState($scope.formParams);
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });

        //Actions
        $scope.activate = function (id) {
            $log.log("Activating job with id["+id+"]");
            scheduleRest.activate(id,
                function success(value) {
                    $log.log('scheduleListController: schedule activated', value);
                    loadModel($scope.resourceParams);
                }, function error(reason) {
                    coreApp.error('Schedule activate failed',reason);
                });
        };

        $scope.deactivate = function (id) {
            $log.log("Deactivating job with id["+id+"]");
            scheduleRest.deactivate(id,
                function success(value) {
                    $log.log('scheduleListController: schedule deactivated', value);
                    loadModel($scope.resourceParams);
                }, function error(reason) {
                    coreApp.error('Schedule deactivate failed',reason);
                });
        };

        $scope.delete = function (id) {
            $log.log("Deleting job with id["+id+"]");
            coreApp.openConfirmModal('Current schedule, will be deleted',
                function confirmed() {
                    scheduleRest.delete(id,
                        function success(value) {
                            $log.log('scheduleListController: schedule removed', value);
                            loadModel($scope.resourceParams);
                        }, function error(reason) {
                            coreApp.error('Schedule removal failed',reason);
                        });
                });
        };

        $scope.showError = function (message) {
            coreApp.openStacktraceModal(message, 'Error');
        };

    })

    .controller('scheduleCardController', function ($log, $scope, scheduleRest, coreApp, $state, $stateParams) {

        $log.info('scheduleCardController', $stateParams);
        $scope.types = ['WORKER_SCHEDULED', 'DECIDER_START'];

        //Updates schedules  by polling REST resource
        function loadModel() {
            $log.info('Load model', $stateParams.id);
            $scope.command = scheduleRest.get($stateParams,
                    function success(value) {
                        $log.info('scheduleCardController: successfully updated schedule page');
                        $scope.changeCron();
                    }, function error(reason) {
                        coreApp.error('Schedule page update failed',reason);
                    });
        }

        //Initialization:
        if($stateParams.id){
            loadModel();
        }else{
            $scope.command = new scheduleRest();
            // defaults
            $scope.command["queueLimit"] = 0;
            $scope.command["maxErrors"] = 0;
            $scope.command["type"] = "DECIDER_START";
            $scope.command["id"] = -1;
            // /defaults
        }

        $scope.isValidForm = function () {
            return $scope.command.name && $scope.command.isCronValid &&
                $scope.command.method && $scope.command.actorId &&
                $scope.command.queueLimit >= 0 && $scope.command.maxErrors >= 0;
        };

        //Actions
        $scope.save = function () {
            if ($scope.isValidForm()) {
                var saveRest = $scope.command.id>0 ? scheduleRest.update : scheduleRest.create;
                $log.log("try to save job, command: ", $scope.command);
                saveRest($scope.command,
                    function success(value) {
                        $log.log('scheduleCardController: schedule save success', value);
                        $state.go('schedules', {});
                    }, function error(reason) {
                        coreApp.error('Schedule save error',reason);
                    });
            }
        };

        $scope.changeCron = function () {
            if ($scope.command.cron) {
                scheduleRest.validate({value: $scope.command.cron},
                    function success(value) {
                        if(value.length === 0) {
                            $log.info('scheduleCardController: successfully cron validate', value);
                            $scope.command.isCronValid = true;
                        } else {
                            coreApp.warn('Schedule cron validate failed',value);
                            $scope.command.isCronValid = false;
                        }
                    }, function error(reason) {
                        coreApp.error('Schedule cron validate failed',reason);
                    });
            } else {
                delete $scope.command.isCronValid;
            }
        };

    });
