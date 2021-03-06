
/* Config Module */
angular.module('DrillRig.navigation', [ ])

	/**
	 * module route configuration
	 */
  	.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {

  		$routeProvider.when('/configuration', {
			redirectTo : '/configuration_forward'
  		});
  		$routeProvider.when('/configuration_forward', {
  			templateUrl: 'gui/partials/forward.html', 
  			controller: 'ConfigForwardCtrl'
  		});
  		$routeProvider.when('/configuration_connection', {
  			templateUrl: 'gui/partials/connection.html', 
  			controller: 'ConfigConnectionCtrl'
  		});

		$routeProvider.when('/monitor', {
			templateUrl : 'gui/partials/monitor.html',
			controller : 'MonitoringCtrl'
		});
		//$routeProvider.when('/logout');
		
		$routeProvider.otherwise({redirectTo: '/monitor'});

		$locationProvider.html5Mode(true);
  		
  	}])
  	
  	.controller('NavigationCtrl', [ '$scope', '$location', function($scope, $location) {
  		$scope.$location = $location;
  		
  		$scope.naviCSS = function($location, path, active, inactive) {
  			return $location.path().indexOf(path)==0 ? active : inactive;
  		}
  	}]);