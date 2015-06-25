'use strict';
angular.module('frontendApp').controller('NavCtrl', function($scope, Auth) {
    $scope.user = Auth.user;


    $scope.logout = function(){
    	console.log('lol');
    	Auth.logout();
    }
});