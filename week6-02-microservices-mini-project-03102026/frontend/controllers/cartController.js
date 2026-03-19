'use strict';

angular.module('ecommerceApp')

.controller('CartController', ['$scope', '$rootScope', '$location', 'CartService', 'OrderService',
    function($scope, $rootScope, $location, CartService, OrderService) {

        $scope.cart          = null;
        $scope.loading       = true;
        $scope.placingOrder  = false;
        $scope.showCheckout  = false;

        // Checkout form fields
        $scope.shippingAddress  = '';
        $scope.cardNumber       = '';
        $scope.cardExpiry       = '';
        $scope.cardCvv          = '';
        $scope.cardHolderName   = '';

        function loadCart() {
            $scope.loading = true;
            CartService.getCart()
                .then(function(response) {
                    $scope.cart = response.data;
                    $rootScope.cartCount = response.data.items ? response.data.items.length : 0;
                })
                .catch(function() {
                    $rootScope.showAlert('Failed to load cart.', 'danger');
                })
                .finally(function() { $scope.loading = false; });
        }

        loadCart();

        $scope.removeItem = function(itemId) {
            CartService.removeItem(itemId)
                .then(function(response) {
                    $scope.cart = response.data;
                    $rootScope.cartCount = response.data.items ? response.data.items.length : 0;
                    $rootScope.showAlert('Item removed from cart.', 'success');
                })
                .catch(function() { $rootScope.showAlert('Failed to remove item.', 'danger'); });
        };

        $scope.updateQuantity = function(item) {
            if (item.quantity < 1) { $scope.removeItem(item.id); return; }
            CartService.updateItem(item.id, item.quantity)
                .then(function(response) { $scope.cart = response.data; })
                .catch(function() { $rootScope.showAlert('Failed to update quantity.', 'danger'); });
        };

        $scope.proceedToCheckout = function() {
            $scope.showCheckout = true;
        };

        $scope.placeOrder = function() {
            if (!$scope.shippingAddress.trim()) {
                $rootScope.showAlert('Please enter a shipping address.', 'warning');
                return;
            }
            if (!$scope.cardNumber || !$scope.cardExpiry || !$scope.cardCvv) {
                $rootScope.showAlert('Please fill in all card details.', 'warning');
                return;
            }

            $scope.placingOrder = true;

            var orderData = {
                shippingAddress: $scope.shippingAddress,
                cardNumber:      $scope.cardNumber.replace(/\s/g, ''),
                cardExpiry:      $scope.cardExpiry,
                cardCvv:         $scope.cardCvv,
                cardHolderName:  $scope.cardHolderName
            };

            OrderService.placeOrder(orderData)
                .then(function(response) {
                    $rootScope.cartCount = 0;
                    $rootScope.showAlert(
                        'Order #' + response.data.id + ' placed! Payment ' + response.data.status + '.',
                        'success'
                    );
                    $location.path('/orders');
                })
                .catch(function(error) {
                    var msg = (error.data && error.data.error) || 'Failed to place order.';
                    $rootScope.showAlert(msg, 'danger');
                })
                .finally(function() { $scope.placingOrder = false; });
        };
    }
]);
