angular.module("console.interrupted.controllers", ['console.interrupted.directives', 'console.util.services'])

.controller("interruptedTasksListController", ['$scope', '$log', '$http', 'tskBpTextProvider', 'tskBrokenProcessesActions', function($scope, $log, $http, tskBpTextProvider, tskBrokenProcessesActions) {

    $scope.brokenGroups = [];
    $scope.brokenTasks = [];
    $scope.foundBrokenProcesses = [];

    $scope.searchInitialized = true;

    $scope.initialized = false;
    $scope.groupCommand = {
        group: 'starter',
        starterId: '',
        actorId: '',
        exception: '',
        dateFrom: '',
        dateTo: ''
    };

    $scope.searchCommand = {
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
        minDate: null,
        time: {
            hour: 0, minute: 0
        }
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

    var setDatesToCommand = function(command) {
        var result = "";
        if ($scope.period.dateFrom && $scope.period.dateTo) {
            var fromDateStr = getDateAsString($scope.period.dateFrom);
            var toDateStr = getDateAsString($scope.period.dateTo);
            if ($scope.period.withTime) {
                fromDateStr = fromDateStr + " " + getTimeAsString($scope.period.timeFrom);
                toDateStr = toDateStr + " " + getTimeAsString($scope.period.timeTo);
            }
            command.dateFrom = fromDateStr;
            command.dateTo = toDateStr;
        }
        return result;
    };


    var getCommandAsParamLine = function() {
        var result = "";
        setDatesToCommand($scope.groupCommand);
        for (var key in $scope.groupCommand) {
            if (result.length>0) {
                result = result + "&";
            }
            result = result + key + "=" + encodeURIComponent($scope.groupCommand[key]);
        }
        $log.log("group command line is " + result);
        return result;
    };

    var getSearchCommandAsParamLine = function() {
        var result = "";
        setDatesToCommand($scope.searchCommand);
        for (var key in $scope.searchCommand) {
            if (result.length>0) {
                result = result + "&";
            }
            result = result + key + "=" + encodeURIComponent($scope.searchCommand[key]);
        }
        $log.log("Search command line is " + result);
        return result;
    };


    var setFilterFieldCondition = function(command, condition){
        if ('starter' == command.group) {
            command.starterId = condition;
        } else if ('actor' == command.group) {
            command.actorId = condition;
        } else if ('exception' == command.group) {
            command.exception = condition;
        }
    };

    var setCommandConditions = function(condition) {
        setFilterFieldCondition($scope.groupCommand, condition);
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
        $http.get('/rest/console/process/tasks/interrupted/group?' + getCommandAsParamLine()).then(function(success) {
            $scope.brokenGroups = success.data;
            $scope.initialized = true;
        }, function(error) {
            $scope.feedback = error;
            $scope.initialized = true;
        });
    };

    var updateProcessesList = function() {
        $scope.initialized = false;
        $http.get('/rest/console/process/tasks/interrupted/list?' + getCommandAsParamLine()).then(function(success) {
            $scope.brokenTasks = success.data;
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
        setCommandConditions(groupName);
        $scope.groupCommand.group = groupType;
        $scope.update();
    };

    $scope.applyGroup = function(name) {

        setCommandConditions(name);
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
        setCommandConditions(groupName);
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

    $scope.isSearchFormCorrect = function() {
        return ($scope.searchCommand.starterId && $scope.searchCommand.starterId.length > 0)
            || ($scope.searchCommand.actorId && $scope.searchCommand.actorId.length > 0)
            || ($scope.searchCommand.exception && $scope.searchCommand.exception.length > 0);
    };

    $scope.findProcesses = function() {
        if ($scope.isSearchFormCorrect()) {
            $scope.searchInitialized = false;
            $http.get('/rest/console/process/tasks/interrupted/list?' + getSearchCommandAsParamLine()).then(function(success) {
                $scope.foundBrokenProcesses = success.data;
                $scope.searchInitialized = true;
            }, function(error) {
                $scope.feedback = error.data;
                $scope.searchInitialized = true;
            });
        }
    };

    $scope.restartGroup = function (bpg, index) {
        tskBrokenProcessesActions.restartGroup(bpg).then(function(okResp) {
            $scope.brokenGroups.splice(index, 1);
        }, function(errResp){
            $scope.feedback = errResp;
        });
    };

    $scope.update();

}]);