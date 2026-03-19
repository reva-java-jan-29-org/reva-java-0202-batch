'use strict';

angular.module('ecommerceApp', ['ngRoute'])
    .config(['$routeProvider', '$httpProvider', '$locationProvider',
        function($routeProvider, $httpProvider, $locationProvider) {

            $routeProvider
                // Default redirect
                .when('/', { redirectTo: '/products' })

                // Simple route
                .when('/products', {
                    templateUrl: 'views/products.html',
                    controller: 'ProductController'
                })

                // Route with resolve (guard)
                .when('/cart', {
                    templateUrl: 'views/cart.html',
                    controller: 'CartController',
                    resolve: {
                        auth: ['AuthService', '$location', function(AuthService, $location) {
                            if (!AuthService.isLoggedIn()) {
                                $location.path('/login');
                            }
                        }]
                    }
                })

                // Fallback
                .otherwise({ redirectTo: '/products' });
        }
])

  