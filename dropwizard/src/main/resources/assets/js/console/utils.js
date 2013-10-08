var consoleUtilServices = angular.module("console.util.services", []);

consoleUtilServices.factory("tskUtil", function () {
    var resultService = {
        convertArrayToLine: function(array) {
            var result = "";
            if (array && array.length>0) {
                for (var i = 0; i<array.length; i++) {
                    if (result.length>0) {
                        result += ",";
                    }
                    result += array[i];
                }
            } else {
                return array;
            }
            return result;

        }
    };
});

consoleUtilServices.factory('tskDataStore', function () {
        var holder = {};

        return {
            save: function (key, data) {
                holder[key] = data;
                return true;
            },
            load: function (key) {
                return holder[key];
            },
            getHolder: function () {
                return holder;
            }
        }
    }
);

consoleUtilServices.factory('$$timeUtil', ["$timeout",
    function ($timeout) {
        var _intervals = {}, _intervalUID = 1;

        return {
            setInterval: function (operation, interval, $scope) {
                var _internalId = _intervalUID++;

                _intervals[ _internalId ] = $timeout(function intervalOperation() {
                    operation($scope || undefined);
                    _intervals[ _internalId ] = $timeout(intervalOperation, interval);
                }, interval);

                $scope.$on('$destroy', function (e) {//cancel interval on change view
                    $timeout.cancel(_intervals[_internalId]);
                });

                return _internalId;
            },

            clearInterval: function (id) {
                return $timeout.cancel(_intervals[ id ]);
            }
        }
    }
]);


