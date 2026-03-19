
angular.module('ecommerceApp')
    .controller('ProductController', ['$scope', '$rootScope', 'ProductService', function($scope, $rootScope, ProductService){

        //code / logic for this controller
        //store the data
        $scope.products = [];


        ProductService.getAllProducts()
            .then(function(response){
                console.log('fetched all products from the backend')
                console.log('response = ', response)
            })
            .catch(function(error){
                console.log('Something went wrong while fetching all products from the backend')
                console.log('error = ' , error)
            })

    }  ])

