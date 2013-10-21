var consoleBrokenProcessesControllers = angular.module("console.broken.process.controllers", ['console.broken.process.directives', 'console.broken.process.services']);

consoleBrokenProcessesControllers.controller("brokenProcessListController", ['$scope', '$log', '$http', 'tskBpTextProvider', function($scope, $log, $http, tskBpTextProvider) {

    $scope.brokenGroups = [];
    $scope.brokenProcesses = [];

    $scope.initialized = false;
    $scope.groupCommand = {
        group: 'starter',
        starterId: '',
        actorId: '',
        exception: '',
        dateFrom: '',
        dateTo: ''
    };

    var yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);

    $scope.period = {
        dateFrom: yesterday,
        dateTo: new Date(),
        timeFrom: new Date(),
        timeTo: new Date(),
        withTime: false,
        maxDate: new Date(),
        minDate: null
    };

    $scope.feedback = {};
    $scope.viewType = 'group';//or 'list'

    $scope.filter = [];

    $scope.syncFilter = function() {
        resetCommandFilter();
        setCommandFilter();
        if ($scope.filter.length < 3) {
            $scope.viewType = 'group';
        }
        $scope.update();
    };

    var setFilterCondition = function(type, condition) {
        for (var i = 0; i<$scope.filter.length; i++) {
            if (type == $scope.filter[i][0]) {//has this type already
                $scope.filter[i][1] = condition;
                return;
            }
        }
        $scope.filter.push([type, condition]);//new one
    };

    var resetCommandFilter = function() {
        $scope.groupCommand.starterId = '';
        $scope.groupCommand.actorId = '';
        $scope.groupCommand.exception = '';
    };

    var setCommandFilter = function() {
        for (var i = 0 ; i < $scope.filter.length; i++) {
            if ('starter' == $scope.filter[i][0]) {
                $scope.groupCommand.starterId = $scope.filter[i][1];

            } else if ('actor' == $scope.filter[i][0]) {
                $scope.groupCommand.actorId = $scope.filter[i][1];

            } else if ('exception' == $scope.filter[i][0]) {
                $scope.groupCommand.exception = $scope.filter[i][1];

            }
        }
    };

    var withLeadingZero = function(number) {
        if (number<10) {
            return "0"+number;
        } else {
            return number;
        }
    };

    var getDateAsString = function (dateObj) {
        if (dateObj) {
            return withLeadingZero(dateObj.getDate()) + "." + withLeadingZero(dateObj.getMonth()+1)+"." + dateObj.getFullYear();
        }
    };

    var getTimeAsString = function (dateObj) {
        if (dateObj) {
            return withLeadingZero(dateObj.getHours()) + ":" + withLeadingZero(dateObj.getMinutes());
        }
    };

    var setDatesToCommand = function() {
        var result = "";
        if ($scope.period.dateFrom && $scope.period.dateTo) {
            var fromDateStr = getDateAsString($scope.period.dateFrom);
            var toDateStr = getDateAsString($scope.period.dateTo);
            if ($scope.period.withTime) {
                fromDateStr = fromDateStr + " " + getTimeAsString($scope.period.timeFrom);
                toDateStr = toDateStr + " " + getTimeAsString($scope.period.timeTo);
            }
            $scope.groupCommand.dateFrom = fromDateStr;
            $scope.groupCommand.dateTo = toDateStr;
        }
        return result;
    };

    var getCommandAsParamLine = function() {
        var result = "";
        setDatesToCommand();
        for (var key in $scope.groupCommand) {
            if (result.length>0) {
                result = result + "&";
            }
            result = result + key + "=" + encodeURIComponent($scope.groupCommand[key]);
        }
        $log.log("Command line is " + result);
        return result;
    };

    var setCommandFilterCondition = function(condition) {
        if ('starter' == $scope.groupCommand.group) {
            $scope.groupCommand.starterId = condition;
        } else if ('actor' == $scope.groupCommand.group) {
            $scope.groupCommand.actorId = condition;
        } else if ('exception' == $scope.groupCommand.group) {
            $scope.groupCommand.exception = condition;
        }
        setFilterCondition($scope.groupCommand.group, condition);
    };

    var getFirstAvailableGroupMode = function() {
        if ($scope.groupCommand.starterId.length==0) {
            return 'starter';
        } else if ($scope.groupCommand.actorId.length==0) {
            return 'actor';
        } else if($scope.groupCommand.exception.length==0) {
            return 'exception';
        } else {
            return null;
        }
    };

    var updateGroupsList = function() {
        $scope.initialized = false;
        $http.get('/rest/console/process/broken/group?' + getCommandAsParamLine()).then(function(success) {
            $scope.brokenGroups = success.data;
            $scope.initialized = true;
        }, function(error) {
            $scope.feedback = error;
            $scope.initialized = true;
        });
    };

    var updateProcessesList = function() {
        $scope.initialized = false;
        $http.get('/rest/console/process/broken/list?' + getCommandAsParamLine()).then(function(success) {
            $scope.brokenProcesses = success.data;
            $scope.initialized = true;
        }, function(error) {
            $scope.feedback = error.data;
            $scope.initialized = true;
        });
    };

    $scope.regroup = function (groupType) {
        $scope.groupCommand.group = groupType;
        $scope.update();
    };

    $scope.applyWithRegroup = function(groupName, groupType) {
        setCommandFilterCondition(groupName);
        $scope.groupCommand.group = groupType;
        $scope.update();
    };

    $scope.applyGroup = function(name) {

        setCommandFilterCondition(name);
        var nextGroupingName = getFirstAvailableGroupMode();
        if (nextGroupingName) {
            $scope.groupCommand.group = nextGroupingName;
            $scope.update();
        } else {
            $scope.showProcessListView(name);
        }

    };

    $scope.showProcessListView = function(groupName) {
        $scope.viewType = 'list';
        setCommandFilterCondition(groupName);
        $scope.update();
    };

    $scope.getGroupLabel = function(name) {
        return tskBpTextProvider.getGroupLabel(name);
    };

    $scope.hasStartersColumn = function() {
        var isFiltered = $scope.groupCommand.starterId && $scope.groupCommand.starterId.length>0;
        return $scope.groupCommand.group != 'starter' && !isFiltered;
    };

    $scope.hasExceptionsColumn = function() {
        var isFiltered = $scope.groupCommand.exception && $scope.groupCommand.exception.length>0;
        return $scope.groupCommand.group!='exception' && !isFiltered;
    };

    $scope.hasActorsColumn = function() {
        var isFiltered = $scope.groupCommand.actorId && $scope.groupCommand.actorId.length>0;
        return $scope.groupCommand.group!='actor' && !isFiltered;
    };

    $scope.update = function() {
        if ($scope.viewType == 'list') {
            updateProcessesList();
        } else {
            updateGroupsList();
        }
    };

    $scope.update();

}]);