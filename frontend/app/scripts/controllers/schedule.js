'use strict';
angular.module('frontendApp').controller('ScheduleCtrl', function($scope, $resource, $location, $http, Host) {
    $scope.generateMode = 'new';
    $scope.startDate = null;
    $scope.schedule = null;
    $scope.lastWeekend = {};
    var User = $resource(Host.backend + '/user/:id');
    $scope.users = {};
    $scope.userss = [];
    User.query({}, function(users) {
        angular.forEach(users, function(user) {
            $scope.users[user.id] = user;
            user.name = user.firstName + " " + user.lastName;
            $scope.userss.push(user);
        });
    });
    $scope.open = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.opened = true;
    };
    $scope.disabled = function(date, mode) {
        return (mode === 'day' && date.getDay() !== 1);
    };
    $scope.generate = function() {
        var lastWeek = null;
        if ($scope.generateMode == 'withPrevious') { 
            lastWeek = {};
            
            angular.forEach($scope.lastWeekend, function(day) {
              var shifts = {'EARLY':[], 'DAY':[], 'LATE':[], 'NIGHT':[]};
              angular.forEach(day.shifts, function(nurses, name) {
                angular.forEach(nurses, function(nurse){
                  shifts[name].push({"id":nurse.id});
                });
                
              });
              lastWeek[day.date.toISOString()] = shifts;
            });
        }
        $http.post(Host.backend + '/schedule', {
            'startDate': $scope.startDate,
            'lastWeek': lastWeek
        }).success(function(data, status, headers, config) {
            $scope.schedule = [];
            angular.forEach(data, function(item, date) {
                $scope.schedule.push({
                    'date': new Date(date),
                    'shifts': item
                });
            });
            angular.forEach($scope.schedule, function(day) {
                angular.forEach(day, function(shift) {
                    angular.forEach(shift, function(nurses) {
                        angular.forEach(nurses, function(nurse) {
                            nurse.name = $scope.users[nurse.id].firstName + " " + $scope.users[nurse.id].lastName;
                            nurse.user = $scope.users[nurse.id];
                        });
                    });
                });
            });
        });
    };
    $scope.$watch('startDate', function(date) {
        if (date === null) {
            return;
        }
        var start = new Date(date);
        $scope.lastWeekend = [{
            date: new Date(date.getFullYear(), date.getMonth(), date.getDate() - 7),
            shifts: {
                'EARLY': [],
                'DAY': [],
                'LATE': [],
                'NIGHT': []
            }
        }, {
            date: new Date(date.getFullYear(), date.getMonth(), date.getDate() - 6),
            shifts: {
                'EARLY': [],
                'DAY': [],
                'LATE': [],
                'NIGHT': []
            }
        }, {
            date: new Date(date.getFullYear(), date.getMonth(), date.getDate() - 5),
            shifts: {
                'EARLY': [],
                'DAY': [],
                'LATE': [],
                'NIGHT': []
            }
        }, {
            date: new Date(date.getFullYear(), date.getMonth(), date.getDate() - 4),
            shifts: {
                'EARLY': [],
                'DAY': [],
                'LATE': [],
                'NIGHT': []
            }
        }, {
            date: new Date(date.getFullYear(), date.getMonth(), date.getDate() - 3),
            shifts: {
                'EARLY': [],
                'DAY': [],
                'LATE': [],
                'NIGHT': []
            }
        }, {
            date: new Date(date.getFullYear(), date.getMonth(), date.getDate() - 2),
            shifts: {
                'EARLY': [],
                'DAY': [],
                'LATE': [],
                'NIGHT': []
            }
        }, {
            date: new Date(date.getFullYear(), date.getMonth(), date.getDate() - 1),
            shifts: {
                'EARLY': [],
                'DAY': [],
                'LATE': [],
                'NIGHT': []
            }
        }];
    });
});