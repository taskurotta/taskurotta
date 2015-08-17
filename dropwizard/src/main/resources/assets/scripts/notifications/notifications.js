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
    .controller('subscriptionCardController', function ($log, $scope, coreApp, subscriptionsRest, coreRest, coreApp, $state, $stateParams) {

        $log.info('subscriptionCardController', $stateParams);

        $scope.doUpdate = $stateParams.id && $stateParams.id>0;

        function asCommaSeparatedString(value) {
            var result = value;
            if (angular.isArray(value)) {
                result = "";
                for (var i = 0; i<value.length ; i++) {
                    if (i > 0) {
                        result = result + ",";
                    }
                    result = result + value[i];
                }
            }
            return result;
        }

        function getCommand() {
            var result = {
                id: $scope.subscription.id,
                emails: asCommaSeparatedString($scope.subscription.emails),
                actorIds: asCommaSeparatedString($scope.subscription.actorIds)
            };

            return result;
        };

        //Updates schedules  by polling REST resource
        function loadModel() {
            $log.info('Load model', $stateParams.id);
            $scope.subscription = subscriptionsRest.get($stateParams,
                function success(value) {
                    $log.info('subscriptionsCardController: successfully updated subscription');
                }, function error(reason) {
                    coreApp.error('Subscription page update failed',reason);
                });
        }

        //Initialization:
        if($stateParams.id){
            loadModel();
        } else {
            $scope.subscription = new subscriptionsRest();
        }

        $scope.isValidForm = function () {
            return $scope.subscription.emails && $scope.subscription.actorIds;
        };

        //Actions
        $scope.save = function () {
            subscriptionsRest.create($scope.subscription,
                function success(value) {
                    $log.log('subscriptionCardController: subscription save success', value);
                    $state.go('subscriptions', {});
                }, function error(reason) {
                    coreApp.error('Subscription save error',reason);
                });
        };

        $scope.update = function () {
            var command = getCommand();
            $log.log("Try to update subscription with id["+$scope.subscription.id+"] with command", command);
            subscriptionsRest.update(command,
                function success(value) {
                    $log.log('subscriptionCardController: subscription update success', value);
                    $state.go('subscriptions', {});
                }, function error(reason) {
                    coreApp.error('Subscription update error', reason);
                });
        };

    })
;