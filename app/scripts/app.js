'use strict';

// Declare app level module which depends on views, and components
var tsApp = angular.module('tsApp', ['ngRoute','tsControllers']);

tsApp.config(['$routeProvider', function($routeProvider) {
  $routeProvider
  .when('/', {
      templateUrl: 'views/partials/vertical.html'
  })
  .when("/transitions", {
    templateUrl: 'views/transitions.html'
  })
  .when("/variables", {
      templateUrl: 'views/variables.html'
    })
  .when("/operations", {
        templateUrl: 'views/operations.html'
      })
  .otherwise({redirectTo: '/'});
}]);
