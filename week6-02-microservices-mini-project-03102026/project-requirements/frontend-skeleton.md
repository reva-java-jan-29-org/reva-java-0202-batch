# Frontend Skeleton — AngularJS 1.x
### Common Starting Point for All 5 Projects

---

## Overview

Every project must include an AngularJS 1.x single-page application that consumes the microservices backend through the API Gateway.

| Item | Value |
|---|---|
| Framework | AngularJS 1.8.3 |
| Frontend port | `http://localhost:3000` |
| API Gateway | `http://localhost:8080` |
| Auth token | JWT stored in `localStorage` |
| Routing | `ngRoute` — `$routeProvider` / `ng-view` |

---

## Common Folder Structure

```
frontend/
├── index.html               ← shell page; loads all scripts; contains <ng-view>
├── app.js                   ← module declaration + route config
├── services/
│   ├── auth.service.js      ← login, register, logout, JWT helpers
│   └── api.service.js       ← $http wrapper that injects Authorization header
├── controllers/
│   ├── auth.controller.js   ← LoginCtrl, RegisterCtrl
│   ├── admin.controller.js  ← AdminCtrl (project-specific tabs)
│   ├── customer.controller.js
│   └── supplier.controller.js
└── views/
    ├── login.html
    ├── register.html
    ├── admin/
    │   └── dashboard.html
    ├── customer/            ← rename to patient/, student/, buyer/, owner/, guest/
    │   └── dashboard.html
    └── supplier/            ← rename to doctor/, instructor/, farmer/, mechanic/, host/
        └── dashboard.html
```

> Rename `customer/` and `supplier/` folders to the domain-specific role names for each project (e.g. `patient/` for MediConnect, `student/` for EduHub).

---

## index.html

```html
<!DOCTYPE html>
<html ng-app="app">
<head>
  <meta charset="UTF-8">
  <title>Project App</title>
  <!-- AngularJS 1.8.3 via CDN -->
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.8.3/angular.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.8.3/angular-route.min.js"></script>

  <style>
    body { font-family: Arial, sans-serif; margin: 0; padding: 0; }
    nav  { background: #333; color: #fff; padding: 10px 20px; }
    nav a { color: #fff; margin-right: 15px; text-decoration: none; }
    .container { padding: 20px; }
    .error { color: red; }
    .success { color: green; }
  </style>
</head>
<body>

  <!-- Navbar — shown only when logged in -->
  <nav ng-if="isLoggedIn()">
    <span>{{currentUser.username}} ({{currentUser.role}})</span>
    <a href="#!/dashboard">Dashboard</a>
    <a href ng-click="logout()">Logout</a>
  </nav>

  <div class="container">
    <ng-view></ng-view>
  </div>

  <!-- App scripts -->
  <script src="services/auth.service.js"></script>
  <script src="services/api.service.js"></script>
  <script src="controllers/auth.controller.js"></script>
  <script src="controllers/admin.controller.js"></script>
  <script src="controllers/customer.controller.js"></script>
  <script src="controllers/supplier.controller.js"></script>
  <script src="app.js"></script>
</body>
</html>
```

---

## app.js

```javascript
angular.module('app', ['ngRoute'])

.config(function($routeProvider) {
  $routeProvider
    .when('/login',    { templateUrl: 'views/login.html',    controller: 'LoginCtrl' })
    .when('/register', { templateUrl: 'views/register.html', controller: 'RegisterCtrl' })

    // Admin routes
    .when('/admin',    { templateUrl: 'views/admin/dashboard.html', controller: 'AdminCtrl',
                         resolve: { auth: requireRole('ADMIN') } })

    // Customer routes  — adapt paths per project
    .when('/dashboard', { templateUrl: 'views/customer/dashboard.html', controller: 'CustomerCtrl',
                          resolve: { auth: requireRole('CUSTOMER') } })

    // Supplier routes
    .when('/supplier/dashboard', { templateUrl: 'views/supplier/dashboard.html', controller: 'SupplierCtrl',
                                   resolve: { auth: requireRole('SUPPLIER') } })

    .otherwise({ redirectTo: '/login' });
})

// Route guard helper
.factory('requireRole', function($q, $location, AuthService) {
  return function(role) {
    return function() {
      var deferred = $q.defer();
      if (!AuthService.isLoggedIn()) {
        $location.path('/login');
        deferred.reject('not authenticated');
      } else if (role && AuthService.getRole() !== role) {
        $location.path('/login');
        deferred.reject('wrong role');
      } else {
        deferred.resolve();
      }
      return deferred.promise;
    };
  };
})

// Expose auth helpers on $rootScope for the navbar
.run(function($rootScope, $location, AuthService) {
  $rootScope.isLoggedIn  = AuthService.isLoggedIn.bind(AuthService);
  $rootScope.currentUser = AuthService.getCurrentUser();
  $rootScope.logout = function() {
    AuthService.logout();
    $location.path('/login');
  };

  // Keep currentUser reactive after login
  $rootScope.$on('auth:login', function() {
    $rootScope.currentUser = AuthService.getCurrentUser();
  });
});
```

---

## services/auth.service.js

```javascript
angular.module('app')
.factory('AuthService', function($http, $rootScope) {

  var API = 'http://localhost:8080';

  function saveToken(token) {
    localStorage.setItem('jwt', token);
  }

  function getToken() {
    return localStorage.getItem('jwt');
  }

  function decodePayload(token) {
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
      return null;
    }
  }

  return {
    login: function(credentials) {
      return $http.post(API + '/api/auth/login', credentials)
        .then(function(res) {
          saveToken(res.data.token);
          $rootScope.$emit('auth:login');
          return res.data;
        });
    },

    register: function(payload) {
      return $http.post(API + '/api/auth/register', payload)
        .then(function(res) {
          saveToken(res.data.token);
          $rootScope.$emit('auth:login');
          return res.data;
        });
    },

    logout: function() {
      localStorage.removeItem('jwt');
    },

    isLoggedIn: function() {
      return !!getToken();
    },

    getToken: getToken,

    getRole: function() {
      var payload = decodePayload(getToken());
      return payload ? payload.role : null;
    },

    getCurrentUser: function() {
      var payload = decodePayload(getToken());
      return payload || {};
    }
  };
});
```

---

## services/api.service.js

```javascript
angular.module('app')
.factory('ApiService', function($http, AuthService) {

  var BASE = 'http://localhost:8080';

  function headers() {
    return { Authorization: 'Bearer ' + AuthService.getToken() };
  }

  return {
    get: function(path) {
      return $http.get(BASE + path, { headers: headers() });
    },
    post: function(path, body) {
      return $http.post(BASE + path, body, { headers: headers() });
    },
    put: function(path, body) {
      return $http.put(BASE + path, body, { headers: headers() });
    },
    delete: function(path) {
      return $http.delete(BASE + path, { headers: headers() });
    },
    // Public (no auth header)
    publicGet: function(path) {
      return $http.get(BASE + path);
    }
  };
});
```

---

## controllers/auth.controller.js

```javascript
angular.module('app')

.controller('LoginCtrl', function($scope, $location, AuthService) {
  $scope.credentials = {};
  $scope.error = null;

  $scope.login = function() {
    $scope.error = null;
    AuthService.login($scope.credentials)
      .then(function(data) {
        var role = AuthService.getRole();
        if      (role === 'ADMIN')    $location.path('/admin');
        else if (role === 'SUPPLIER') $location.path('/supplier/dashboard');
        else                          $location.path('/dashboard');
      })
      .catch(function(err) {
        $scope.error = (err.data && err.data.message) || 'Login failed';
      });
  };
})

.controller('RegisterCtrl', function($scope, $location, AuthService) {
  $scope.form = { role: 'CUSTOMER' };   // default role
  $scope.error = null;

  $scope.register = function() {
    $scope.error = null;
    AuthService.register($scope.form)
      .then(function(data) {
        var role = AuthService.getRole();
        if (role === 'SUPPLIER') $location.path('/supplier/dashboard');
        else                     $location.path('/dashboard');
      })
      .catch(function(err) {
        $scope.error = (err.data && err.data.message) || 'Registration failed';
      });
  };
});
```

---

## views/login.html

```html
<h2>Login</h2>
<p class="error" ng-if="error">{{error}}</p>

<form ng-submit="login()">
  <div>
    <label>Username</label>
    <input type="text" ng-model="credentials.username" required>
  </div>
  <div>
    <label>Password</label>
    <input type="password" ng-model="credentials.password" required>
  </div>
  <button type="submit">Login</button>
</form>

<p>Don't have an account? <a href="#!/register">Register</a></p>
```

---

## views/register.html

```html
<h2>Register</h2>
<p class="error" ng-if="error">{{error}}</p>

<form ng-submit="register()">
  <!-- Common fields -->
  <select ng-model="form.role">
    <option value="CUSTOMER">Customer</option>  <!-- rename per project -->
    <option value="SUPPLIER">Supplier</option>  <!-- rename per project -->
  </select>
  <input type="text"     ng-model="form.username"    placeholder="Username"   required>
  <input type="password" ng-model="form.password"    placeholder="Password"   required>
  <input type="text"     ng-model="form.firstName"   placeholder="First Name" required>
  <input type="text"     ng-model="form.lastName"    placeholder="Last Name"  required>
  <input type="text"     ng-model="form.mobileNumber" placeholder="Mobile Number">

  <!-- Customer-specific fields (ng-if="form.role === 'CUSTOMER'") -->
  <!-- Add project-specific fields here -->

  <!-- Supplier-specific fields (ng-if="form.role === 'SUPPLIER'") -->
  <!-- Add project-specific fields here -->

  <button type="submit">Register</button>
</form>
```

---

## Patterns to Follow in All Controllers

```javascript
// Fetch a list and display
$scope.items = [];
$scope.loading = true;
ApiService.get('/api/items').then(function(res) {
  $scope.items = res.data;
}).catch(function(err) {
  $scope.error = err.data && err.data.message || 'Failed to load';
}).finally(function() {
  $scope.loading = false;
});

// Submit a form
$scope.submit = function() {
  ApiService.post('/api/items', $scope.form)
    .then(function(res) {
      $scope.success = 'Created successfully!';
      $scope.form = {};   // reset
      // refresh list...
    })
    .catch(function(err) {
      $scope.error = err.data && err.data.message || 'Error occurred';
    });
};
```

---

## Docker Integration (Optional but Recommended)

Add a simple nginx container in `docker-compose.yml` to serve the frontend alongside the backend services:

```yaml
frontend:
  image: nginx:alpine
  ports:
    - "3000:80"
  volumes:
    - ./frontend:/usr/share/nginx/html:ro
  depends_on:
    - api-gateway
```

No build step required — AngularJS 1.x is loaded via CDN in `index.html`, and the files are served statically.

---

## AngularJS Quick Reference

| Directive / Service | Purpose |
|---|---|
| `ng-app="app"` | Bootstrap the Angular application |
| `ng-view` | Renders the current route's template |
| `ng-controller` | Attach a controller to a DOM element |
| `ng-model` | Two-way binding to scope variable |
| `ng-repeat="item in items"` | Loop over array |
| `ng-if="condition"` | Conditionally show/hide element |
| `ng-click="fn()"` | Bind click event |
| `ng-submit="fn()"` | Bind form submit event |
| `ng-class="{'active': flag}"` | Conditional CSS classes |
| `$http` | Make HTTP requests |
| `$location.path('/route')` | Navigate programmatically |
| `$routeProvider` | Define client-side routes |
| `$q` | Promises (deferred pattern) |
| `$rootScope.$emit / $on` | Cross-controller events |
