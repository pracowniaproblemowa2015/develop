'use strict';

angular.module('frontendApp').controller('UserSingleCtrl', function($scope, $resource, $stateParams, $location, Host) {
  var User = $resource(Host.backend + '/user/:id');

  $scope.roles = [
    {name:'Nurse', val:'ROLE_USER'},
    {name:'Admin', val:'ROLE_ADMIN'}
  ];

  $scope.user = User.get({id:$stateParams.id});

  $scope.save = function(){
  	$scope.user.$save({id:$scope.user.id}, function(){
  		$location.path("/user");  		
  	}); 
  };

  $scope.cancel = function(){
  	$location.path("/user");
  };

});