angular.module("console.util.services", [])

.factory("tskUtil", function ($log) {
    var resultService = {
        skippingIndexOf: function(word, skip, char) {
            var result = -1;
            if (word && word.length>skip) {
                var subResult = word.substr(skip).indexOf(char);
                if (subResult != -1) {
                    result = subResult + skip;
                }
            }
            //$log.log("skipping index for [" + word + "] is [" + result + "]");
            return result;
        },
        injectNewLineDelimiter: function (word, skip, delimiter) {
            var result = word;
            var skippingIndex = this.skippingIndexOf(word, skip, delimiter);
            if (skippingIndex != -1) {
                //$log.log("skippingIndex["+skippingIndex+"] for["+word+"], sub1["+word.substr(0, skippingIndex+1)+"], sub 2["+word.substr(skippingIndex+1)+"]");
                result = word.substr(0, skippingIndex+1) + " " + word.substr(skippingIndex+1);
            }
            return result;
        },
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

        },
        hasValueInArray: function(val, arr) {
            var result = false;
            for (var i = 0; i<arr.length; i++) {
                if (val == arr[i]) {
                    result = true;
                    break;
                }
            }
            return result;
        }
    };
    return resultService;
})

.factory('tskDataStore', function () {
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
)

.factory('tskTimeUtil', ["$timeout",
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


