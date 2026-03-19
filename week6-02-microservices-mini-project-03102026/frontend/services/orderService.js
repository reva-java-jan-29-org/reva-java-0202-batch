'use strict';

angular.module('ecommerceApp')

.service('OrderService', ['$http', function($http) {
    var API_URL = 'http://localhost:8080/api/orders';

    // orderData = { shippingAddress, cardNumber, cardExpiry, cardCvv, cardHolderName }
    this.placeOrder = function(orderData) {
        return $http.post(API_URL, orderData);
    };

    this.getMyOrders = function() {
        return $http.get(API_URL);
    };

    this.getOrderById = function(orderId) {
        return $http.get(API_URL + '/' + orderId);
    };
}]);
