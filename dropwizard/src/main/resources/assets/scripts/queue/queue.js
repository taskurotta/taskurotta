angular.module('queueModule', ['coreApp'])

    .factory('queueRest', function ($log, coreApp, $resource) {
        var restQueueUrl = coreApp.getRestUrl() + 'queues/';
        var rawInterceptor = coreApp.getRawInterceptor();

        return $resource(coreApp.getRestUrl() + 'queue/:queueName', {}, {
                getSize: {url: restQueueUrl + ':name/size', interceptor: rawInterceptor},
                getStorageSize: {url: restQueueUrl + ':name/storage/size', interceptor: rawInterceptor},
                //list
                query: {url: restQueueUrl, params: {}},
                //queryHovering: {url: coreApp.getRestUrl() + 'hoveringQueues/', params: {}, isArray: true},
                //actions
                clear: {url: restQueueUrl + 'clear', method: 'POST', interceptor: rawInterceptor},
                remove: {url: restQueueUrl + 'remove', method: 'POST', interceptor: rawInterceptor}

            }
        );
    })

    .factory('actorRest', function ($log, coreApp, $resource) {
        var restActorUrl = coreApp.getRestUrl() + 'actor/';

        return $resource(restActorUrl, {}, {
            info: {url: restActorUrl + 'info/', method: 'GET'},
            //actions
            unblock: {url: restActorUrl + 'unblock/', method: 'POST'},
            block: {url: restActorUrl + 'block/', method: 'POST'}
        });
    })

    .factory('actorStateConf', function () {
        return {
            ACTIVE: {css: "actor-active", blocked: false},
            INACTIVE: {css: "actor-inactive", blocked: false},
            BLOCKED: {css: "actor-blocked", blocked: true}
        }
    })


    .controller('queueListController', function ($log, $scope, queueRest, coreApp, actorStateConf, actorRest, util) {
        $log.info('queueListController');

        $scope.actorStateConf = actorStateConf;
        $scope.util = util;

        //function getRest(params) {
        //    return params.periodSize ? queueRest.queryHovering : queueRest.query;
        //}

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.queuesResource = queueRest.query(params,
                function success(value) {
                    $scope.queuesModel = coreApp.parseListModel(value);//cause array or object
                    if ($scope.queuesModel) {
                        $log.info('Successfully updated queues page');
                        $scope.queuesModel.$totalTasks = _.reduce($scope.queuesModel.items,
                            function (sum, item) {
                                return sum + item.count;
                            }, 0);
                    } else {
                        coreApp.info('Queues not found');
                    }
                    coreApp.refreshRate(params, loadModel);
                }, function error(reason) {
                    coreApp.error('Processes model update failed', reason);
                });
        }

        //Initialization:
        $scope.formParams = coreApp.copyStateParams();
        $scope.$stateParams = coreApp.getStateParams();

        loadModel(angular.copy($scope.formParams));

        //Submit form command:
        $scope.search = function () {
            $scope.formParams.pageNum = undefined;
            $scope.formParams.refreshRate = undefined;
            coreApp.reloadState($scope.formParams);
        };

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });

        //Actions
        $scope.showRealSize = function (queue) {
            queueRest.getSize({name: queue.name},
                function success(value) {
                    $log.info('Queue storage' + queue.name + ' realSize:', value);
                    queue.realSize = value;
                }, function error(reason) {
                    queue.realSize = 'n/a';
                });
        };

        $scope.showStorageRealSize = function (queue) {
            queueRest.getStorageSize({name: queue.name},
                function success(value) {
                    $log.info('Queue storage' + queue.name + ' realSize:', value);
                    queue.realSize = value;
                }, function error(reason) {
                    queue.realSize = 'n/a';
                });
        };

        $scope.clear = function (queue) {
            coreApp.openConfirmModal('All current elements of the queue would be completely lost.',
                function confirmed() {
                    queueRest.clear(queue.name, function (value) {
                        $log.log('Queue cleared', value);
                        loadModel($scope.resourceParams);
                    }, function (reason) {
                        coreApp.error('Queue draining failed', reason);
                    });
                });

        };

        $scope.remove = function (queue) {
            coreApp.openConfirmModal('Current queue, all of its content and actor preference data would be lost',
                function confirmed() {
                    queueRest.remove(queue.name, function (value) {
                        $log.log('Queue removed', value);
                        loadModel($scope.resourceParams);
                    }, function (reason) {
                        coreApp.error('Queue removal failed', reason);
                    });
                });

        };

        $scope.unblock = function (queue) {
            coreApp.openConfirmModal('Actor will be set to unblock.',
                function confirmed() {
                    actorRest.unblock(queue.name, function success(value) {
                        $log.log('Actor [' + queue.name + '] have been set to unblocked', value);
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error setting unblocked for actor [' + queue.name + ']', reason);
                    });
                });
        };

        $scope.block = function (queue) {
            coreApp.openConfirmModal('Actor will be set to block.',
                function confirmed() {
                    actorRest.block(queue.name, function success(value) {
                        $log.log('Actor [' + queue.name + '] have been set to blocked', value);
                        loadModel($scope.resourceParams);
                    }, function error(reason) {
                        coreApp.error('Error setting blocked for actor [' + queue.name + ']', reason);
                    });
                });
        };


    })

    .controller('actorController', function ($log, $scope, actorRest, coreApp, actorStateConf, util) {
        $log.info('actorController');

        $scope.util = util;

        $scope.actorStateConf = actorStateConf;

        function loadModel(params) {
            $log.info('Load model', $scope.resourceParams = params);
            $scope.actorResource = actorRest.info(params,
                function success(value) {
                    $log.info('Successfully updated actor data');
                    $scope.info = value;
                    coreApp.refreshRate(params, loadModel);
                },
                function error(reason) {
                    coreApp.error('Actors page update failed', reason);
                }
            );
        }

        //Initialization:
        $scope.formParams = coreApp.copyStateParams();
        $scope.formParams.metrics = coreApp.parseObjectParam($scope.formParams.metrics);
        loadModel(angular.copy($scope.formParams));

        //Finalization:
        $scope.$on('$destroy', function () {
            coreApp.stopRefreshRate();
        });

        $scope.quotes = function (str) {
            return '{"' + str + '":true}';
        };

        var mean = "mean";
        var count = "count";
        $scope.metricsInfo = [
            {text: "New tasks", name: "enqueue", type: count},
            {text: "Done tasks", name: "release", type: count},
            {text: "Mean time", name: "executionTime", type: mean},
            {text: "With exceptions", name: "errorDecision", type: count},
            {text: "Mean time on exception", name: "errorDecision", type: mean},
            {text: "Poll", name: "poll", type: count},
            {text: "Poll with tasks", name: "successfulPoll", type: count},
            {text: "Start process", name: "startProcess", type: count}
            //{text: "Mean queue size", name: "queueSize", type: count}

        ]

    });
