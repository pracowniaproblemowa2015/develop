'use strict';

angular.module('frontendApp', ['ngCookies', 'ngResource', 'ngSanitize', 'ui.router', 'ui.bootstrap'])
  .config(function($stateProvider, $urlRouterProvider, $httpProvider) {

    $urlRouterProvider.otherwise("/");

    $stateProvider
      .state('main', {
        abstract: true,
        views: {
          '': {
            templateUrl: "views/main.html"
          },
          'nav-top@main': {
            controller : "NavCtrl",
            templateUrl: "views/main.nav.top.html"
          }
        }
      })

    .state('main.dashboard', {
      url: "/",
      views: {
        'content@main': {
          templateUrl: "views/dashboard.html",
          controller: "MainCtrl"
        }
      }
    })

    .state('main.user', {

    })

    .state('main.user.list', {
      url: "/user",
      views: {
        'content@main': {
          templateUrl: "views/user.list.html",
          controller: "UserCtrl"
        }
      }
    })

    .state('main.user.single', {
      url: "/user/:id",
      views: {
        'content@main': {
          templateUrl: "views/user.single.html",
          controller: "UserSingleCtrl"
        }
      }
    })

    .state('main.schedule', {
      url: "/schedule",
      views: {
        'content@main': {
          templateUrl: "views/schedule.html",
          controller: "ScheduleCtrl"
        }
      }
    })

    

    .state('login', {
      url: "/login",
      templateUrl: "views/login.html",
      controller: 'LoginCtrl'
    });



    $httpProvider.interceptors.push(['$q', '$location', '$injector',
      function($q, $location, $injector) {
        return {
          'request': function(config) {
            if(config.url.indexOf("8080") == -1){
              return config || $q.when(config);
            }
            var authService = $injector.get('Auth');
            if (authService.token) {
              config.headers.Authorization = 'Bearer ' + authService.token;
            }
            return config || $q.when(config);
          },

          'responseError': function(response) {
            //response.status === 0 -> connection problem(probably backend unavilable)
            if (response.status === 401 || response.status === 403 || response.status === 0) {
              // Probably the ticket has expired - need to login again.
              console.log('Redirecting to the login page because of status: ' + response.status);
              $location.path('/login');
            } else {
              console.log('Response contains error: ' + JSON.stringify(response));
              return $q.reject(response);
            }
          }
        };
      }
    ]);



  }).run(function ($rootScope, $state, $stateParams, Auth) {
    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;
    $rootScope.currentUser = Auth.user;
});