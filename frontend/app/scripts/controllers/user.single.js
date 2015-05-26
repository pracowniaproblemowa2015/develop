'use strict';

angular.module('frontendApp').controller('UserSingleCtrl', function($scope, $resource, $stateParams,  Host) {
  var User = $resource(Host.backend + '/user/:id');

  $scope.user = User.get({id:$stateParams.id});

});