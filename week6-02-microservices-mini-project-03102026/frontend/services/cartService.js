'use strict';

angular.module('ecommerceApp')

.service('CartService', ['$http', function($http) {
    var API_URL = 'http://localhost:8080/api/cart';

    this.getCart = function() {
        return $http.get(API_URL);
    };

    this.addToCart = function(productId, quantity) {
        return $http.post(API_URL + '/add', {
            productId: productId,
            quantity: quantity || 1
        });
    };

    this.removeItem = function(itemId) {
        return $http.delete(API_URL + '/items/' + itemId);
    };

    this.updateItem = function(itemId, quantity) {
        return $http.put(API_URL + '/items/' + itemId, null, {
            params: { quantity: quantity }
        });
    };

    this.clearCart = function() {
        return $http.delete(API_URL + '/clear');
    };
}]);
