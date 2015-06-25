'use strict';
angular.module('frontendApp').controller('ScheduleCtrl', function($scope, $resource, $location, $http, Host) {
    $scope.generateMode = 'new';
    $scope.startDate = null;
    var User = $resource(Host.backend + '/user/:id');
    $scope.users = User.query();
    $scope.open = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.opened = true;
    };
    $scope.disabled = function(date, mode) {
        return (mode === 'day' && date.getDay() !== 1);
    };
    $scope.generate = function() {
        $http.post(Host.backend + '/schedule', {
            'startDate': $scope.startDate
        }).success(function(data, status, headers, config) {
          $scope.response = data;
        });
    }
});