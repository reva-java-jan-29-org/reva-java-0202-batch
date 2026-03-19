'use strict';

angular.module('ecommerceApp')

.service('PaymentService', ['$http', function($http) {
    var BASE = 'http://localhost:8080/api/payments';

    /** Get payment details for a specific order. */
    this.getPaymentByOrder = function(orderId) {
        return $http.get(BASE + '/order/' + orderId);
    };

    /** Get the current user's full payment history. */
    this.getMyPayments = function() {
        return $http.get(BASE + '/my');
    };
}]);
