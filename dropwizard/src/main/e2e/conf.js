var HtmlScreenshotReporter = require('protractor-jasmine2-screenshot-reporter');

var reportDir = "e2e";
process.argv.forEach(function (val, index, array) {
    if (val.indexOf("--report-dir=") == 0) {
        reportDir = val.substr("--report-dir=".length);
    }
});

var reporter = new HtmlScreenshotReporter({
    dest: reportDir,
    filename: 'e2e-report.html',
    reportOnlyFailedSpecs: true,
    captureOnlyFailedSpecs: true
});


exports.config = {
    specs: [
        'interrupted-tsk-list.js',
        'should-be-last-spec.js'],

    // Setup the report before any tests start
    beforeLaunch: function () {
        return new Promise(function (resolve) {
            reporter.beforeLaunch(resolve);
        });
    },

    // Assign the test reporter to each running instance
    onPrepare: function () {
        jasmine.getEnv().addReporter(reporter);
    },

    // Close the report after all tests finish
    afterLaunch: function (exitCode) {
        return new Promise(function (resolve) {
            reporter.afterLaunch(resolve.bind(this, exitCode));
        });
    }
};


