'use strict';

angular.module('ecommerceApp')

.controller('AdminController', ['$scope', '$rootScope', '$location', '$route',
    'AdminService', 'ProductService',
    function($scope, $rootScope, $location, $route, AdminService, ProductService) {

        // ── Dashboard ─────────────────────────────────────────────────────────
        $scope.loadDashboard = function() {
            AdminService.listCustomers().then(function(r) { $scope.customerCount = r.data.length; });
            AdminService.getAllOrders().then(function(r)  { $scope.orderCount    = r.data.length; });
            ProductService.getAllProducts().then(function(r) { $scope.productCount = r.data.length; });
            AdminService.getAllPayments().then(function(r) { $scope.paymentCount  = r.data.length; });
        };

        // ── Products ──────────────────────────────────────────────────────────
        $scope.products   = [];
        $scope.productForm = {};
        $scope.editMode   = false;

        $scope.loadProducts = function() {
            ProductService.getAllProducts().then(function(r) { $scope.products = r.data; });
        };

        $scope.editProduct = function(p) {
            $scope.productForm = angular.copy(p);
            $scope.editMode    = true;
        };

        $scope.newProduct = function() {
            $scope.productForm = {};
            $scope.editMode    = false;
        };

        $scope.saveProduct = function() {
            var promise = $scope.editMode
                ? AdminService.updateProduct($scope.productForm.id, $scope.productForm)
                : AdminService.createProduct($scope.productForm);

            promise.then(function() {
                $rootScope.showAlert('Product saved successfully.', 'success');
                $scope.productForm = {};
                $scope.editMode    = false;
                $scope.loadProducts();
            }).catch(function(err) {
                var msg = (err.data && err.data.error) || 'Failed to save product.';
                $rootScope.showAlert(msg, 'danger');
            });
        };

        $scope.deleteProduct = function(id) {
            if (!confirm('Delete this product?')) return;
            AdminService.deleteProduct(id)
                .then(function() {
                    $rootScope.showAlert('Product deleted.', 'success');
                    $scope.loadProducts();
                })
                .catch(function() { $rootScope.showAlert('Failed to delete product.', 'danger'); });
        };

        // ── Customers ─────────────────────────────────────────────────────────
        $scope.customers = [];

        $scope.loadCustomers = function() {
            AdminService.listCustomers().then(function(r) { $scope.customers = r.data; });
        };

        $scope.disableCustomer = function(id) {
            AdminService.disableCustomer(id)
                .then(function() {
                    $rootScope.showAlert('Customer account disabled.', 'warning');
                    $scope.loadCustomers();
                })
                .catch(function(err) {
                    var msg = (err.data && err.data.error) || 'Failed to disable customer.';
                    $rootScope.showAlert(msg, 'danger');
                });
        };

        $scope.enableCustomer = function(id) {
            AdminService.enableCustomer(id)
                .then(function() {
                    $rootScope.showAlert('Customer account re-enabled.', 'success');
                    $scope.loadCustomers();
                })
                .catch(function() { $rootScope.showAlert('Failed to enable customer.', 'danger'); });
        };

        $scope.deleteCustomer = function(id) {
            if (!confirm('Permanently delete this customer?')) return;
            AdminService.deleteCustomer(id)
                .then(function() {
                    $rootScope.showAlert('Customer deleted.', 'success');
                    $scope.loadCustomers();
                })
                .catch(function() { $rootScope.showAlert('Failed to delete customer.', 'danger'); });
        };

        // ── Orders (All) ──────────────────────────────────────────────────────
        $scope.allOrders     = [];
        $scope.selectedOrder = null;

        $scope.loadAllOrders = function() {
            AdminService.getAllOrders().then(function(r) { $scope.allOrders = r.data; });
        };

        $scope.viewOrder = function(order) {
            $scope.selectedOrder = order;
        };

        $scope.closeOrder = function() {
            $scope.selectedOrder = null;
        };

        // ── Admin Management ──────────────────────────────────────────────────
        $scope.admins      = [];
        $scope.adminForm   = {};

        $scope.loadAdmins = function() {
            AdminService.listAdmins().then(function(r) { $scope.admins = r.data; });
        };

        $scope.createAdmin = function() {
            AdminService.createAdmin($scope.adminForm)
                .then(function() {
                    $rootScope.showAlert('Admin account created.', 'success');
                    $scope.adminForm = {};
                    $scope.loadAdmins();
                })
                .catch(function(err) {
                    var msg = (err.data && err.data.error) || 'Failed to create admin.';
                    $rootScope.showAlert(msg, 'danger');
                });
        };

        $scope.deleteAdmin = function(id) {
            if (!confirm('Delete this admin account?')) return;
            AdminService.deleteAdmin(id)
                .then(function() {
                    $rootScope.showAlert('Admin deleted.', 'success');
                    $scope.loadAdmins();
                })
                .catch(function() { $rootScope.showAlert('Failed to delete admin.', 'danger'); });
        };

        // ── Initialise based on current path ─────────────────────────────────
        var path = $location.path();
        if (path === '/admin/dashboard')  $scope.loadDashboard();
        if (path === '/admin/products')   $scope.loadProducts();
        if (path === '/admin/customers')  $scope.loadCustomers();
        if (path === '/admin/orders')     $scope.loadAllOrders();
        if (path === '/admin/admins')     $scope.loadAdmins();
    }
]);
