'use strict';

angular.module('ecommerceApp')

.controller('ProductController', ['$scope', '$rootScope', 'ProductService', 'CartService', 'AuthService',
    function($scope, $rootScope, ProductService, CartService, AuthService) {

        $scope.products = [];
        $scope.filteredProducts = [];
        $scope.searchQuery = '';
        $scope.selectedCategory = 'All';
        $scope.categories = ['All'];
        $scope.loading = true;
        $scope.addingToCart = {};

        // Load all products
        ProductService.getAllProducts()
            .then(function(response) {
                $scope.products = response.data;
                $scope.filteredProducts = response.data;

                // Extract unique categories
                var cats = ['All'];
                response.data.forEach(function(p) {
                    if (p.category && cats.indexOf(p.category) === -1) {
                        cats.push(p.category);
                    }
                });
                $scope.categories = cats;
            })
            .catch(function() {
                $rootScope.showAlert('Failed to load products.', 'danger');
            })
            .finally(function() {
                $scope.loading = false;
            });

        $scope.search = function() {
            if (!$scope.searchQuery.trim()) {
                $scope.filterByCategory($scope.selectedCategory);
                return;
            }
            $scope.loading = true;
            ProductService.searchProducts($scope.searchQuery)
                .then(function(response) {
                    $scope.filteredProducts = response.data;
                })
                .catch(function() {
                    $rootScope.showAlert('Search failed.', 'danger');
                })
                .finally(function() {
                    $scope.loading = false;
                });
        };

        $scope.filterByCategory = function(category) {
            $scope.selectedCategory = category;
            $scope.searchQuery = '';
            if (category === 'All') {
                $scope.filteredProducts = $scope.products;
            } else {
                $scope.filteredProducts = $scope.products.filter(function(p) {
                    return p.category === category;
                });
            }
        };

        $scope.clearSearch = function() {
            $scope.searchQuery = '';
            $scope.filterByCategory($scope.selectedCategory);
        };

        $scope.addToCart = function(product) {
            if (!AuthService.isLoggedIn()) {
                $rootScope.showAlert('Please login to add items to cart.', 'warning');
                return;
            }
            $scope.addingToCart[product.id] = true;

            CartService.addToCart(product.id, 1)
                .then(function(response) {
                    $rootScope.cartCount = response.data.items ? response.data.items.length : 0;
                    $rootScope.showAlert(product.name + ' added to cart!', 'success');
                })
                .catch(function(error) {
                    var msg = (error.data && error.data.error) || 'Failed to add to cart.';
                    $rootScope.showAlert(msg, 'danger');
                })
                .finally(function() {
                    $scope.addingToCart[product.id] = false;
                });
        };
    }
]);
