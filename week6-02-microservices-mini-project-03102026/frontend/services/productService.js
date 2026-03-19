

angular.module('ecommerceApp')
    .service('ProductService', ['$http', function($http){

        var API_URL = 'http://localhost:8080/api/products'

        this.getAllProducts = function(){
            return $http.get(API_URL)
        }

        this.getProductById = function(id){
            return $http.get(API_URL + "/" + id)
        }

        this.searchProducts = function(query){
            return $http.get(API_URL + "/search", { params: { q: query }} )
        }

    }])