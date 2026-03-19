'use strict';

angular.module('ecommerceApp')

.service('AdminService', ['$http', function($http) {
    var BASE = 'http://localhost:8080/api';

    // ── Admin Management ──────────────────────────────────────────────────────
    this.createAdmin = function(data) {
        return $http.post(BASE + '/admin/admins', data);
    };
    this.listAdmins = function() {
        return $http.get(BASE + '/admin/admins');
    };
    this.deleteAdmin = function(id) {
        return $http.delete(BASE + '/admin/admins/' + id);
    };

    // ── Customer Management ───────────────────────────────────────────────────
    this.listCustomers = function() {
        return $http.get(BASE + '/admin/customers');
    };
    this.disableCustomer = function(id) {
        return $http.put(BASE + '/admin/customers/' + id + '/disable');
    };
    this.enableCustomer = function(id) {
        return $http.put(BASE + '/admin/customers/' + id + '/enable');
    };
    this.deleteCustomer = function(id) {
        return $http.delete(BASE + '/admin/customers/' + id);
    };

    // ── Product Management ────────────────────────────────────────────────────
    this.createProduct = function(data) {
        return $http.post(BASE + '/products', data);
    };
    this.updateProduct = function(id, data) {
        return $http.put(BASE + '/products/' + id, data);
    };
    this.deleteProduct = function(id) {
        return $http.delete(BASE + '/products/' + id);
    };

    // ── Order / Payment Overview ──────────────────────────────────────────────
    this.getAllOrders = function() {
        return $http.get(BASE + '/orders/all');
    };
    this.getAllPayments = function() {
        return $http.get(BASE + '/payments/all');
    };
}]);
