'use strict';

angular.module('ecommerceApp')

.service('AuthService', ['$http', function($http) {
    var API_URL = 'http://localhost:8080/api/auth';

    function saveSession(data) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            userId:    data.userId,
            username:  data.username,
            firstName: data.firstName,
            role:      data.role        // "ROLE_ADMIN" | "ROLE_CUSTOMER"
        }));
    }

    this.register = function(userData) {
        return $http.post(API_URL + '/register', userData)
            .then(function(response) {
                saveSession(response.data);
                return response;
            });
    };

    this.login = function(credentials) {
        return $http.post(API_URL + '/login', credentials)
            .then(function(response) {
                saveSession(response.data);
                return response;
            });
    };

    this.logout = function() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    };

    this.isLoggedIn = function() {
        return !!localStorage.getItem('token');
    };

    this.isAdmin = function() {
        var user = this.getUser();
        return user && user.role === 'ROLE_ADMIN';
    };

    this.getToken = function() {
        return localStorage.getItem('token');
    };

    this.getUser = function() {
        var user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    };
}]);
