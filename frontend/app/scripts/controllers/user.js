'use strict';

angular.module('frontendApp').controller('UserCtrl', function($scope, $resource, $location, Host) {
  var User = $resource(Host.backend + '/user/:id');

  $scope.roles = [
    {name:'Nurse', val:'ROLE_USER'},
    {name:'Admin', val:'ROLE_ADMIN'}
  ];

  $scope.users = User.query();

  $scope.addUser = function(user) {
    User.save({}, user, function(){
      $scope.users = User.query();
    });
  };

  $scope.goTo = function(id) {
    $location.path('/user/' + id);
  };

  

});