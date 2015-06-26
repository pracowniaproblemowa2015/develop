'use strict';
angular.module('frontendApp').factory('Host', function() {
    if (window.location.host === 'localhost:9000') {
        /* developer settings - run from localhost on ports: frontend - 9000, backend - 8080 */
        return {
            backend: 'http://localhost:8080/scheduler/api'
        };
    } else {
        /* production settings - all on 8080 */
        return {
            backend: 'http://' + window.location.host.replace('9000', '8080') + '/zmianoustalacz/api'
        };
    }
});