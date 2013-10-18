var consoleAnimation = angular.module('console.animation', []);
//consoleAnimation.animation('fadein-enter', function() {
//    return {
//        setup : function(element) {
//            //prepare the element for animation
//            element.css({ 'opacity': 0 });
//            var memo = "..."; //this value is passed to the start function
//            return memo;
//        },
//        start : function(element, done, memo) {
//            //start the animation
//            element.animate({
//                'opacity' : 1
//            }, function() {
//                //call when the animation is complete
//                done()
//            });
//        }
//    }
//});