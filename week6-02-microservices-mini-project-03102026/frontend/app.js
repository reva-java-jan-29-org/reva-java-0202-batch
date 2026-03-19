'use strict';

angular.module('ecommerceApp', ['ngRoute'])

.config(['$routeProvider', '$httpProvider',
    function($routeProvider, $httpProvider) {

        // ── Auth guard helpers ─────────────────────────────────────────────────
        var requireLogin = ['AuthService', '$location', function(AuthService, $location) {
            if (!AuthService.isLoggedIn()) { $location.path('/login'); }
        }];

        var requireAdmin = ['AuthService', '$location', function(AuthService, $location) {
            if (!AuthService.isLoggedIn()) { $location.path('/login'); return; }
            if (!AuthService.isAdmin())    { $location.path('/products'); }
        }];

        var requireCustomer = ['AuthService', '$location', function(AuthService, $location) {
            if (!AuthService.isLoggedIn()) { $location.path('/login'); return; }
            if (AuthService.isAdmin())     { $location.path('/admin/dashboard'); }
        }];

        // ── Routes ────────────────────────────────────────────────────────────
        $routeProvider
            .when('/', { redirectTo: '/products' })

            // Public / auth
            .when('/login',    { templateUrl: 'views/login.html',    controller: 'AuthController' })
            .when('/register', { templateUrl: 'views/register.html', controller: 'AuthController' })

            // Customer routes
            .when('/products', { templateUrl: 'views/products.html', controller: 'ProductController' })
            .when('/cart', {
                templateUrl: 'views/cart.html',
                controller: 'CartController',
                resolve: { auth: requireCustomer }
            })
            .when('/orders', {
                templateUrl: 'views/orders.html',
                controller: 'OrderController',
                resolve: { auth: requireCustomer }
            })
            .when('/payments', {
                templateUrl: 'views/payments.html',
                controller: 'PaymentController',
                resolve: { auth: requireCustomer }
            })

            // Admin routes
            .when('/admin/dashboard', {
                templateUrl: 'views/admin-dashboard.html',
                controller: 'AdminController',
                resolve: { auth: requireAdmin }
            })
            .when('/admin/products', {
                templateUrl: 'views/admin-products.html',
                controller: 'AdminController',
                resolve: { auth: requireAdmin }
            })
            .when('/admin/customers', {
                templateUrl: 'views/admin-customers.html',
                controller: 'AdminController',
                resolve: { auth: requireAdmin }
            })
            .when('/admin/orders', {
                templateUrl: 'views/admin-orders.html',
                controller: 'AdminController',
                resolve: { auth: requireAdmin }
            })
            .when('/admin/admins', {
                templateUrl: 'views/admin-management.html',
                controller: 'AdminController',
                resolve: { auth: requireAdmin }
            })

            .otherwise({ redirectTo: '/products' });

        // JWT Interceptor — attaches Bearer token to every request
        $httpProvider.interceptors.push('authInterceptor');
    }
])

.factory('authInterceptor', ['$q', '$injector', function($q, $injector) {
    return {
        request: function(config) {
            var AuthService = $injector.get('AuthService');
            var token = AuthService.getToken();
            if (token) {
                config.headers = config.headers || {};
                config.headers.Authorization = 'Bearer ' + token;
            }
            return config;
        },
        responseError: function(rejection) {
            if (rejection.status === 401) {
                var AuthService = $injector.get('AuthService');
                AuthService.logout();
                var $location = $injector.get('$location');
                $location.path('/login');
            }
            return $q.reject(rejection);
        }
    };
}])

.run(['$rootScope', 'AuthService', '$location', function($rootScope, AuthService, $location) {

    $rootScope.isLoggedIn = function() { return AuthService.isLoggedIn(); };
    $rootScope.isAdmin    = function() { return AuthService.isAdmin(); };

    $rootScope.currentUser = AuthService.getUser();

    $rootScope.logout = function() {
        AuthService.logout();
        $rootScope.currentUser = null;
        $rootScope.cartCount   = 0;
        $location.path('/login');
    };

    $rootScope.cartCount = 0;

    $rootScope.showAlert = function(message, type) {
        $rootScope.globalAlert = { message: message, type: type || 'info' };
        setTimeout(function() {
            $rootScope.$apply(function() { $rootScope.globalAlert = {}; });
        }, 4000);
    };

    $rootScope.clearAlert = function() { $rootScope.globalAlert = {}; };

    // Refresh currentUser on every route change (handles login/logout transitions)
    $rootScope.$on('$routeChangeStart', function() {
        $rootScope.currentUser = AuthService.getUser();
    });
}]);
