'use strict';

angular.module('ecommerceApp')

.controller('PaymentController', ['$scope', '$rootScope', 'PaymentService',
    function($scope, $rootScope, PaymentService) {

        $scope.payments = [];
        $scope.loading  = true;

        PaymentService.getMyPayments()
            .then(function(response) {
                $scope.payments = response.data;
            })
            .catch(function() {
                $rootScope.showAlert('Failed to load payment history.', 'danger');
            })
            .finally(function() { $scope.loading = false; });
    }
]);
