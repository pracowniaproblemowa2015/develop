'use strict';

angular.module('frontendApp')
  .factory('Host', function () {
    return {
      backend: "http://localhost:8080/scheduler/api"
    };
  });
