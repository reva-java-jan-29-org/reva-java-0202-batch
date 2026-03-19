'use strict';

angular.module('ecommerceApp')

.controller('OrderController', ['$scope', '$rootScope', 'OrderService',
    function($scope, $rootScope, OrderService) {

        $scope.orders = [];
        $scope.loading = true;
        $scope.selectedOrder = null;

        OrderService.getMyOrders()
            .then(function(response) {
                $scope.orders = response.data;
            })
            .catch(function() {
                $rootScope.showAlert('Failed to load orders.', 'danger');
            })
            .finally(function() {
                $scope.loading = false;
            });

        $scope.viewOrder = function(order) {
            $scope.selectedOrder = order;
        };

        $scope.closeOrder = function() {
            $scope.selectedOrder = null;
        };

        $scope.getStatusClass = function(status) {
            var classes = {
                'PENDING': 'warning',
                'CONFIRMED': 'info',
                'SHIPPED': 'primary',
                'DELIVERED': 'success',
                'CANCELLED': 'danger'
            };
            return 'badge bg-' + (classes[status] || 'secondary');
        };
    }
]);
