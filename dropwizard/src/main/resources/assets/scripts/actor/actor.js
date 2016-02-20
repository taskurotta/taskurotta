angular.module('actorModule', ['coreApp'])

    .factory('actorRest', function ($log, coreApp, $resource) {
        var restActorUrl = coreApp.getRestUrl() + 'actor/';

        return $resource(restActorUrl, {}, {
            //list
            query: {url: restActorUrl + 'list/', params: {}},
            loadMetrics: {url: restActorUrl + 'metrics/compare', method: 'POST', params: {}},
            info: {url: restActorUrl + 'info', method: 'GET', isArray: true},
            //actions
            unblock: {url: restActorUrl + 'unblock/', method: 'POST'},
            block: {url: restActorUrl + 'block/', method: 'POST'},
            //dictionaries
            dictionaryMetrics: {url: restActorUrl + 'metrics/compare', params: {}, isArray: true, cache:true}
        });
    })

    .controller('actorListController', function ($log, $scope, $modal, actorRest, coreApp) {
        $log.info('actorListController');

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.actorsResource = actorRest.query(params,
                function success(value) {
                    $scope.actorsModel = coreApp.parseListModel(value); //cause array or object
                    if ($scope.actorsModel) {
                        $log.info('Successfully updated actors data');
                        params.metrics = coreApp.clearObject(params.metrics);
                        if (_.size(params.metrics) > 0) {
                            loadMetrics(params.metrics, $scope.actorsModel.items);
                        }
                    } else {
                        coreApp.info('Actors not found');
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Actors page update failed', reason);
                });
        }

        function loadMetrics(metrics, actors) {
            $log.info('Load metric data');
            $scope.metricsResource = actorRest.loadMetrics({
                    metrics: coreApp.getKeys(metrics),
                    actorIds: _.map(actors, function (item) { return item.id; })
                },
                function success(value) {
                    $log.info('Successfully updated metrics data');
                    $scope.metricsModel = value;
                }, function error(reason) {
                    coreApp.error('Metrics for actors update failed', reason);
                    $scope.metricsModel = null;
                });
        }

        //Initialization:
        $scope.formParams = coreApp.copyStateParams();
        $scope.formParams.metrics = coreApp.parseObjectParam($scope.formParams.metrics);

        $scope.metrics = actorRest.dictionaryMetrics({},
            function success(value) {
                $log.log('Loaded metrics dictionary', value);
                loadModel(angular.copy($scope.formParams));
            }, function error(reason) {
                coreApp.error('Metrics dictionary update failed', reason);
            });


        $scope.changeStateParams = function () {
            var params = coreApp.getStateParams();
            params.metrics = coreApp.stringifyObjectParam($scope.resourceParams.metrics);
            $log.debug('change $stateParams', params);
        };

        //Update command:
        $scope.search = function () {
            coreApp.reloadState(angular.extend({},$scope.formParams,{
                pageNum: undefined,
                refreshRate: undefined,
                metrics: coreApp.stringifyObjectParam($scope.formParams.metrics)
            }));
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });

        //Actions
        $scope.unblock = function (actor) {
            coreApp.openConfirmModal('Actor will be set to unblock.',
                function confirmed() {
                    actorRest.unblock(actor.id, function success(value) {
                        $log.log('Actor [' + actor.id + '] have been set to unblocked',value);
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error setting unblocked for actor [' + actor.id + ']', reason);
                    });
                });
        };

        $scope.block = function (actor) {
            coreApp.openConfirmModal('Actor will be set to block.',
                function confirmed() {
                    actorRest.block(actor.id, function success(value) {
                        $log.log('Actor [' + actor.id + '] have been set to blocked',value);
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error setting blocked for actor [' + actor.id + ']', reason);
                    });
                });
        };

        $scope.showInfo = function (actorId) {
            $modal.open({
                size: 'actor',
                backdrop: false,
                templateUrl: 'views/actor/modal.html',
                controller: 'ActorModalCtrl',
                resolve: {
                    actorId: function () {
                        return actorId;
                    }
                }
            });
        }

    })

    .controller('ActorModalCtrl', ['$scope', '$modalInstance', 'actorRest', 'actorId',
        function ($scope, $uibModalInstance, actorRest, actorId) {

            $scope.actorId = actorId;
            actorRest.info(
                {actorId: actorId},
                function success(value) {
                    $scope.info = value;
                },
                function error(reason) {
                    console.error(reason);
                }
            );

            $scope.ok = function (form) {
                $uibModalInstance.close();
            };

        }])

;
