angular.module('notificationsModule', ['coreApp'])
    .factory('subscriptionsRest', function($log, coreApp, $resource) {
        var restSubscriptionsUrl = coreApp.getRestUrl() + 'subscriptions/';
        var rawInterceptor = coreApp.getRawInterceptor();
        function stringTransformResponse(data, headersGetter, status) {
            return data;
        }

        return $resource(restSubscriptionsUrl, {}, {
                //list
                query: {url: restSubscriptionsUrl, params: {}, isArray: false},
                //actions
                create: {url: restSubscriptionsUrl, method: 'POST', params: {}},
                update: {url: restSubscriptionsUrl + '/:id', method: 'POST', params: {}},
                delete: {url: restSubscriptionsUrl + '/:id', method: 'DELETE', params: {}}
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

    })
;