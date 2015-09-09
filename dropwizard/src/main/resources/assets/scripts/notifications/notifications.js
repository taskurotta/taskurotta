angular.module('notificationsModule', ['coreApp'])
    .factory('subscriptionsRest', function($log, coreApp, $resource) {
        var restSubscriptionsUrl = coreApp.getRestUrl() + 'subscriptions/';
        var rawInterceptor = coreApp.getRawInterceptor();
        function stringTransformResponse(data, headersGetter, status) {
            return data;
        }

        return $resource(restSubscriptionsUrl, {}, {
                get: {url: restSubscriptionsUrl + ':id', method: 'GET', params: {}},
                //list
                query: {url: restSubscriptionsUrl, params: {}, isArray: false},
                //actions
                create: {url: restSubscriptionsUrl, method: 'PUT', params: {}},
                update: {url: restSubscriptionsUrl + ':id', method: 'POST', params: {}},
                delete: {url: restSubscriptionsUrl + ':id', method: 'DELETE', params: {}}
            }
        );
    })
    .factory('triggersRest', function($log, coreApp, $resource) {
        var restTriggersUrl = coreApp.getRestUrl() + 'triggers/';
        var rawInterceptor = coreApp.getRawInterceptor();
        function stringTransformResponse(data, headersGetter, status) {
            return data;
        }

        return $resource(restTriggersUrl, {}, {
                query: {url: restTriggersUrl, params: {}, isArray: true}
            }
        );
    })
    .controller('subscriptionsListController', function($log, $scope, coreApp, subscriptionsRest, coreRest, coreApp) {
        $log.info('subscriptionsListController');

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.subscriptionsResource = subscriptionsRest.query(params,
                function success(value) {
                    $scope.subscriptionsModel =  coreApp.parseListModel(value);//cause array or object
                    if($scope.subscriptionsModel){
                        $log.info('Successfully updated subscriptions page');
                    }else{
                        coreApp.info('Subscriptions not found');
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Subscriptions page update failed',reason);
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

        $scope.delete = function (id) {
            coreApp.openConfirmModal('Current subscription will be deleted',
                function confirmed() {
                    subscriptionsRest.delete({id: id},
                        function success(value) {
                            $log.log('subscriptionsListController: subscriptions removed', value);
                            loadModel($scope.resourceParams);
                        }, function error(reason) {
                            coreApp.error('Subscription removal failed',reason);
                        });
                });
        };

        $scope.showError = function (message) {
            coreApp.openStacktraceModal(message, 'Error');
        };

    })
    .controller('subscriptionCardController', function ($log, $scope, coreApp, subscriptionsRest, coreRest, coreApp, $state, $stateParams, triggersRest) {

        $log.info('subscriptionCardController', $stateParams);

        $scope.doUpdate = $stateParams.id && $stateParams.id>0;

        function asCommaSeparatedString(value) {
            var result = value;
            if (angular.isArray(value)) {
                result = "";
                for (var i = 0; i<value.length ; i++) {
                    if (result.length > 0) {
                        result = result + ",";
                    }
                    if (!!value[i].value && value[i].value.length>0) {
                        result = result + value[i].value;
                    }
                }
            }
            return result;
        }

        function asValuesArray(col) {
            var result = [];
            if (!!col && col.length>0) {
                for (var i = 0; i<col.length; i++) {
                    result.push({value: col[i]});
                }
            }
            return result;
        }

        function getCommand() {

            var selectedTriggers = [];
            if (!!$scope.events && $scope.events.length>0) {
                for (var i = 0 ; i<$scope.events.length ; i++) {
                    if ($scope.events[i].checked) {
                        selectedTriggers.push($scope.events[i].id);
                    }
                }
            }

            var result = {
                id: $scope.subscription.id,
                emails: asCommaSeparatedString($scope.subscription.emails),
                actorIds: asCommaSeparatedString($scope.subscription.actorIds),
                triggersKeys: selectedTriggers
            };

            return result;
        };

        //Updates schedules  by polling REST resource
        function loadModel() {
            $log.info('Load model', $stateParams.id);
            subscriptionsRest.get($stateParams,
                function success(value) {
                    $scope.subscription = {
                        id: value.id,
                        emails: asValuesArray(value.emails),
                        actorIds: asValuesArray(value.actorIds),
                        triggerKeys: value.triggersKeys
                    };

                    if (!!value.triggersKeys && value.triggersKeys.length>0) {

                        for (var i = 0; i < $scope.events.length; i++) {
                            var event = $scope.events[i];
                            $log.info("Parse event ", event);
                            if (value.triggersKeys.indexOf(event.id) != -1) {
                                event.checked = true;
                            }
                        }
                    }

                    $log.info('subscriptionsCardController: successfully updated subscription by data: ', value);
                }, function error(reason) {
                    coreApp.error('Subscription page update failed',reason);
                });

        }

        function hasValue(col) {
            if (!!col && col.length>0) {
                for (var j = 0; j<col.length ;j++) {
                    if (!!col[j].value && col[j].value.trim().length>0) {
                        return true;
                    }
                }
            }
            return false;
        }

        //Initialization:
        triggersRest.query({}, function success(value) {
            $scope.events = value;
            if ($stateParams.id) {
                loadModel();
            } else {
                $scope.subscription = new subscriptionsRest();
                $scope.subscription["emails"] = [{value: ""}];
                $scope.subscription["actorIds"] = [{value: ""}];
                for (var i = 0; i < $scope.events.length; i++) {
                    $scope.events[i].checked = true;
                }
            }

        }, function error(reason) {
            coreApp.error('Events list update failed', reason);
        });

        $scope.isValidForm = function () {
            var hasSelectedTrigger = false;
            if (!!$scope.events && $scope.events.length>0) {
                for (var i = 0 ; i<$scope.events.length ; i++) {
                    if ($scope.events[i].checked) {
                        hasSelectedTrigger = true;
                        break;
                    }
                }
            }

            return !!$scope.subscription && hasSelectedTrigger && hasValue($scope.subscription.emails) && hasValue($scope.subscription.actorIds);
        };

        //Actions
        $scope.save = function () {
            if ($scope.isValidForm()) {
                var command = getCommand();
                $log.log("Try to save subscription with command", command);
                subscriptionsRest.create(command,
                    function success(value) {
                        $log.log('subscriptionCardController: subscription save success', value);
                        $state.go('subscriptions', {});
                    }, function error(reason) {
                        coreApp.error('Subscription save error',reason);
                    });
            }
        };

        $scope.update = function () {
            if ($scope.isValidForm()) {
                var command = getCommand();
                $log.log("Try to update subscription with id["+$scope.subscription.id+"] with command", command);
                subscriptionsRest.update(command,
                    function success(value) {
                        $log.log('subscriptionCardController: subscription update success', value);
                        $state.go('subscriptions', {});
                    }, function error(reason) {
                        coreApp.error('Subscription update error', reason);
                    });
            }
        };

        $scope.addEmail = function() {
            if (!$scope.subscription.emails) {
                $scope.subscription.emails = [];
            }
            $scope.subscription.emails.push({value: ""});
        };

        $scope.removeEmail = function(idx) {
            if ($scope.subscription.emails && $scope.subscription.emails.length > 0) {
                $scope.subscription.emails.splice(idx, 1);
            }
        };

        $scope.addActorId = function() {
            if (!$scope.subscription.actorIds) {
                $scope.subscription.actorIds = [];
            }
            $scope.subscription.actorIds.push({value:""});
        };

        $scope.removeActorId = function(idx) {
            if ($scope.subscription.actorIds && $scope.subscription.actorIds.length > 0) {
                $scope.subscription.actorIds.splice(idx, 1);
            }
        };

    })
    .filter('triggerTypeName', function () {
        return function (type) {
            if (type == "voidQueues") {
                return "Queues are not polled for too long";
            } else if (type == "failedTasks") {
                return "Actor task failed with critical error"
            } else {
                return type;
            }
        };
    })
;