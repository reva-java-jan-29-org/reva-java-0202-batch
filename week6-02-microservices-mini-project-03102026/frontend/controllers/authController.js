'use strict';

angular.module('ecommerceApp')

.controller('AuthController', ['$scope', '$rootScope', '$location', 'AuthService',
    function($scope, $rootScope, $location, AuthService) {

        // Redirect if already logged in
        if (AuthService.isLoggedIn()) {
            $location.path('/products');
            return;
        }

        $scope.loginData = {};
        $scope.registerData = {};
        $scope.loading = false;
        $scope.error = null;

        $scope.login = function() {
            if ($scope.loginForm.$invalid) return;
            $scope.loading = true;
            $scope.error = null;

            AuthService.login({
                username: $scope.loginData.username,
                password: $scope.loginData.password
            })
                .then(function() {
                    $rootScope.currentUser = AuthService.getUser();
                    $rootScope.showAlert('Welcome back, ' + $rootScope.currentUser.username + '!', 'success');
                    $location.path('/products');
                })
                .catch(function(error) {
                    $scope.error = (error.data && error.data.error) || 'Login failed. Check your credentials.';
                })
                .finally(function() {
                    $scope.loading = false;
                });
        };

        $scope.register = function() {
            if ($scope.registerForm.$invalid) return;
            if ($scope.registerData.password !== $scope.registerData.confirmPassword) {
                $scope.error = 'Passwords do not match';
                return;
            }
            $scope.loading = true;
            $scope.error = null;

            AuthService.register({
                username: $scope.registerData.username,
                password: $scope.registerData.password,
                firstName: $scope.registerData.firstName,
                lastName: $scope.registerData.lastName,
                mobileNumber: $scope.registerData.mobileNumber
            })
            .then(function() {
                $rootScope.currentUser = AuthService.getUser();
                $rootScope.showAlert('Welcome, ' + $rootScope.currentUser.username + '! Account created.', 'success');
                $location.path('/products');
            })
            .catch(function(error) {
                $scope.error = (error.data && error.data.error) || 'Registration failed. Please try again.';
            })
            .finally(function() {
                $scope.loading = false;
            });
        };
    }
]);
