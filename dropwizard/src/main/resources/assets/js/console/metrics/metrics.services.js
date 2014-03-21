angular.module("console.metrics.services", [])

    .factory("tskMetricsDateTime", function () {
        var resultService = {
            withLeadingZero: function(number) {
                if (number<10) {
                    return "0"+number;
                } else {
                    return number;
                }
            },
            getShortDate: function(date) {
                if (date) {
                    return this.withLeadingZero(date.getDate()) + "/" + this.withLeadingZero(date.getMonth()+1);
                } else {
                    return "";
                }
            },
            getTimeStr: function(time) {
                if (time) {
                    return this.withLeadingZero(time.getHours()) + ":" + this.withLeadingZero(time.getMinutes());
                } else {
                    return "";
                }
            }
        };

        return resultService;
    })

    .factory("tskMetricsFormatters", function (tskMetricsDateTime, $log) {
        var resultService = {
            time: function (val, axis) {
                var date = new Date(val);
                return tskMetricsDateTime.getShortDate(date) + " " + tskMetricsDateTime.getTimeStr(date);
            },
            memory: function (bytes, axis) {
                var kilobyte = 1024;
                var megabyte = kilobyte * 1024;
                var gigabyte = megabyte * 1024;
                var terabyte = gigabyte * 1024;

                if ((bytes >= 0) && (bytes < kilobyte)) {
                    return bytes + ' B';

                } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
                    return (bytes / kilobyte).toFixed(2) + ' KB';

                } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
                    return (bytes / megabyte).toFixed(2) + ' MB';

                } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
                    return (bytes / gigabyte).toFixed(2) + ' GB';

                } else if (bytes >= terabyte) {
                    return (bytes / terabyte).toFixed(2) + ' TB';

                } else {
                    return bytes + ' B';
                }
            },
            getFormattedValue: function(formatterName, value, axis) {
                var result = value;
                if (!!formatterName && !!this[formatterName]) {
                    result = this[formatterName](value, axis);
                }
                return result;
            }
        };

        return resultService;
    })

    .factory("tskMetricsData", function ($http, $q) {
        var resultService = {
            getMetricsOptions: function() {
                return $http.get('/rest/console/metrics/options/');
            },
            getDataSetUrl: function(selection, dataset) {
                var type = selection.dataMode.value;
                var scope = selection.scopeMode.value;
                var period = selection.periodMode.value;
                var metric = selection.metric.value;
                var zeroes = !selection.omitZeroes;
                var smooth = selection.smoothRate;
                if (!!dataset && !!type && !!scope && !!period && !!metric) {//url contains some defined values
                    return "/rest/console/metrics/data/?zeroes="+zeroes+"&metric=" + metric + "&period=" + period + "&scope=" + scope + "&type=" + type + "&dataset=" + encodeURIComponent(dataset) + "&smooth=" + smooth;
                }
                return "";
            },
            getMetricsData: function (selection, dataset) {
                var url = this.getDataSetUrl(selection, dataset);
                if (url.length>0) {
                    return $http.get(url);
                } else {
                    var deff = $q.defer();
                    deff.resolve({data: []});
                    return deff.promise;
                }
            }
        };

        return resultService;
    })

;