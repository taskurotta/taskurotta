describe('wait...', function() {

    it('a while', function() {
        setTimeout(function() {
            console.log('hello world!');
        }, 5000);
    });

    afterAll(function(done) {
        process.nextTick(done);
    });
});