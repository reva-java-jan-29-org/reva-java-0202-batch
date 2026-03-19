# AngularJS 1.x — Complete Study Notes
> Week 7 | Topics: Orientation, Foundation, Services, Routing, Jenkins, Code Quality

---

## Table of Contents

1. [AngularJS Orientation](#1-angularjs-orientation)
   - Introduction & What is AngularJS
   - SPAs (Single Page Applications)
   - MVC Architecture
   - Expressions
   - Controllers & Scopes
   - Modules
2. [AngularJS Foundation](#2-angularjs-foundation)
   - Data Binding
   - Directives
   - Filters
   - Event Handling
3. [AngularJS Services](#3-angularjs-services)
   - Services, Factories, Providers
   - Dependency Injection
   - HTTP (`$http`)
4. [AngularJS Routing](#4-angularjs-routing)
   - `ngRoute` Setup
   - Routing & Navigation
5. [Jenkins](#5-jenkins)
   - Jenkins Overview
   - Jenkins Job / Project
   - SonarCloud & SonarLint
6. [Code Quality Analysis](#6-code-quality-analysis)
   - SonarQube vs SonarCloud
   - Code Smells, Bugs, and Vulnerabilities
7. [Interview Questions & Answers](#7-interview-questions--answers)

---

## 1. AngularJS Orientation

### What is AngularJS?

AngularJS (version 1.x) is an open-source JavaScript **MVC framework** developed and maintained by Google. It extends plain HTML with new attributes (`ng-*` directives) and binds data to HTML using expressions.

- **Current version covered:** AngularJS 1.x (NOT Angular 2+)
- **Key idea:** Declarative templates + two-way data binding + dependency injection

```html
<!-- Minimal AngularJS app -->
<!DOCTYPE html>
<html ng-app="myApp">
<head>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.8.3/angular.min.js"></script>
</head>
<body ng-controller="MainCtrl">
  <h1>Hello, {{ name }}!</h1>
</body>
<script>
  angular.module('myApp', [])
    .controller('MainCtrl', function($scope) {
      $scope.name = 'AngularJS';
    });
</script>
</html>
```

---

### Single Page Applications (SPAs)

| Traditional Web App | SPA |
|---|---|
| Full page reload on every navigation | Page never fully reloads |
| Server renders HTML | Client-side rendering |
| Slow navigation | Fast, seamless navigation |
| Example: plain PHP site | Example: Gmail, Google Maps |

**How SPAs work:**
1. Browser loads ONE HTML page (`index.html`) at start
2. JavaScript dynamically updates the DOM as user navigates
3. Routes map URLs to views (partial HTML templates)
4. Only data (JSON) is fetched from the server — not full HTML pages

```
User clicks "Products" link
    → URL changes to /#/products
    → AngularJS router intercepts
    → Loads products.html partial into <ng-view>
    → Calls ProductController
    → Fetches data from REST API
    → Updates DOM (no page reload!)
```

---

### MVC Architecture in AngularJS

```
┌─────────────┐      updates       ┌─────────────┐
│   VIEW      │ ◄────────────────  │  CONTROLLER │
│  (HTML/     │                    │  (JS Logic) │
│  Templates) │ ──── user events ► │             │
└─────────────┘                    └──────┬──────┘
                                          │ reads/writes
                                   ┌──────▼──────┐
                                   │   MODEL     │
                                   │  ($scope /  │
                                   │   data)     │
                                   └─────────────┘
```

| Role | AngularJS Component | Responsibility |
|---|---|---|
| **Model** | `$scope` properties | Holds application data |
| **View** | HTML templates | Displays data to user |
| **Controller** | `ng-controller` | Business logic, connects M & V |

---

### Expressions

AngularJS expressions are written inside `{{ }}` and are evaluated against the current `$scope`.

```html
<!-- Basic expressions -->
<p>{{ 5 + 3 }}</p>               <!-- Output: 8 -->
<p>{{ "Hello" + " World" }}</p>  <!-- Output: Hello World -->
<p>{{ user.name }}</p>           <!-- Output: scope variable -->
<p>{{ items.length }}</p>        <!-- Output: array length -->

<!-- Expressions in directives -->
<div ng-class="{ active: isActive }">...</div>
<input ng-model="price" />
<p>Total: {{ price * quantity | currency }}</p>
```

**Key differences — AngularJS Expressions vs JavaScript:**

| Feature | JS Expression | AngularJS Expression |
|---|---|---|
| `undefined` | Throws error | Returns blank |
| `null` | Throws error | Returns blank |
| Loops | Supported | Not supported |
| Function declarations | Supported | Not supported |
| Filters | Not supported | `{{ val \| filter }}` |

---

### Controllers & Scopes

**Controller** — the glue between Model and View. It sets up `$scope` with data and functions.

```javascript
// Defining a controller
angular.module('myApp', [])
  .controller('ProductController', function($scope, $http) {

    // Model data on $scope
    $scope.products = [];
    $scope.newProduct = { name: '', price: 0 };

    // Function on $scope (called from View)
    $scope.addProduct = function() {
      $scope.products.push(angular.copy($scope.newProduct));
      $scope.newProduct = { name: '', price: 0 }; // reset
    };

    // Load data from API
    $scope.loadProducts = function() {
      $http.get('/api/products').then(function(response) {
        $scope.products = response.data;
      });
    };

    $scope.loadProducts(); // call on init
  });
```

```html
<!-- View using the controller -->
<div ng-controller="ProductController">
  <input ng-model="newProduct.name" placeholder="Product Name" />
  <input ng-model="newProduct.price" type="number" placeholder="Price" />
  <button ng-click="addProduct()">Add</button>

  <ul>
    <li ng-repeat="p in products">{{ p.name }} — {{ p.price | currency }}</li>
  </ul>
</div>
```

#### $scope Hierarchy

```
$rootScope  (global, available everywhere)
  └── $scope (ProductController)
        └── $scope (nested controller / directive)
```

- `$rootScope` — top-level scope, shared across the whole app
- `$scope` — local to the controller
- Child scopes **prototypically inherit** from parent scopes
- Use `$emit` to send events UP the scope tree
- Use `$broadcast` to send events DOWN the scope tree

```javascript
// Parent controller
$scope.$broadcast('dataLoaded', { count: 5 });

// Child controller
$scope.$on('dataLoaded', function(event, data) {
  console.log('Items count:', data.count);
});
```

---

### Modules

A **module** is a container for the different parts of an app: controllers, services, filters, directives, etc.

```javascript
// Define a module (name + dependencies array)
var app = angular.module('ecommerceApp', ['ngRoute', 'ngResource']);

// Register a controller on the module
app.controller('HomeController', function($scope) {
  $scope.title = 'Welcome';
});

// Register a service on the module
app.service('CartService', function() {
  this.items = [];
  this.addItem = function(item) { this.items.push(item); };
});
```

```html
<!-- Bootstrap the app -->
<html ng-app="ecommerceApp">
```

**Module dependency injection example:**

```javascript
// auth module
angular.module('authModule', [])
  .service('AuthService', function() { ... });

// app module depends on authModule
angular.module('ecommerceApp', ['ngRoute', 'authModule'])
  .controller('AppCtrl', function(AuthService) { ... });
```

---

## 2. AngularJS Foundation

### Data Binding

AngularJS provides **Two-Way Data Binding** — the model and view stay in sync automatically.

```
Model ($scope) ←────────────────────────→ View (HTML)
  changes in JS    automatic sync         changes in input
```

#### One-Way Binding (Model → View)

```html
<p>{{ user.name }}</p>
<p ng-bind="user.name"></p>   <!-- equivalent, avoids flicker -->
```

#### Two-Way Binding (Model ↔ View)

```html
<input ng-model="user.name" />
<p>Hello, {{ user.name }}</p>
<!-- As you type in the input, the paragraph updates instantly -->
```

#### One-Time Binding (performance optimization)

```html
<p>{{ ::user.name }}</p>   <!-- binds once, then detaches watcher -->
```

#### Practical example — Live Search

```html
<div ng-controller="SearchCtrl">
  <input ng-model="query" placeholder="Search..." />
  <ul>
    <li ng-repeat="item in items | filter:query">{{ item }}</li>
  </ul>
</div>
```
```javascript
app.controller('SearchCtrl', function($scope) {
  $scope.query = '';
  $scope.items = ['Apple', 'Mango', 'Banana', 'Orange', 'Papaya'];
});
```

---

### Directives

Directives are markers on DOM elements that tell AngularJS to attach specific behavior to that element.

#### Built-in Directives

| Directive | Purpose | Example |
|---|---|---|
| `ng-app` | Bootstrap the app | `<html ng-app="myApp">` |
| `ng-controller` | Attach controller | `<div ng-controller="Ctrl">` |
| `ng-model` | Two-way bind | `<input ng-model="name">` |
| `ng-bind` | One-way bind (text) | `<span ng-bind="name">` |
| `ng-repeat` | Loop over array/object | `<li ng-repeat="x in list">` |
| `ng-if` | Conditionally add/remove DOM | `<div ng-if="isLoggedIn">` |
| `ng-show` / `ng-hide` | Toggle visibility | `<div ng-show="isVisible">` |
| `ng-click` | Click event handler | `<button ng-click="save()">` |
| `ng-class` | Dynamic CSS classes | `<div ng-class="{active: tab==1}">` |
| `ng-style` | Dynamic inline styles | `<div ng-style="{color: textColor}">` |
| `ng-src` | Safe image src | `<img ng-src="{{ imgUrl }}">` |
| `ng-href` | Safe anchor href | `<a ng-href="{{ link }}">` |
| `ng-disabled` | Disable form element | `<button ng-disabled="!form.$valid">` |
| `ng-submit` | Form submit handler | `<form ng-submit="submit()">` |
| `ng-init` | Initialize scope | `<div ng-init="count=0">` |

#### ng-repeat in detail

```html
<!-- Basic repeat -->
<li ng-repeat="product in products">{{ product.name }}</li>

<!-- With index -->
<li ng-repeat="product in products track by $index">
  {{ $index + 1 }}. {{ product.name }}
</li>

<!-- With track by (better performance for large lists) -->
<li ng-repeat="product in products track by product.id">

<!-- Special variables in ng-repeat -->
<!-- $index, $first, $last, $even, $odd -->
<tr ng-repeat="row in data" ng-class="{ 'table-striped': $odd }">

<!-- Iterating objects -->
<p ng-repeat="(key, value) in userObj">{{ key }}: {{ value }}</p>
```

#### Custom Directives

```javascript
app.directive('productCard', function() {
  return {
    restrict: 'E',          // E=Element, A=Attribute, C=Class, M=Comment
    scope: {
      product: '=',         // two-way binding
      onDelete: '&'         // expression binding (function)
    },
    template: `
      <div class="card">
        <h3>{{ product.name }}</h3>
        <p>Price: {{ product.price | currency }}</p>
        <button ng-click="onDelete({ id: product.id })">Delete</button>
      </div>
    `,
    link: function(scope, element, attrs) {
      // Direct DOM manipulation here
      element.on('mouseenter', function() {
        element.addClass('hovered');
      });
    }
  };
});
```

```html
<!-- Using custom directive -->
<product-card product="selectedProduct" on-delete="removeProduct(id)">
</product-card>
```

**Scope binding types in directives:**

| Symbol | Meaning | Use When |
|---|---|---|
| `=` | Two-way binding | Sharing an object |
| `@` | One-way string binding | Passing a string value |
| `&` | Expression / function binding | Passing a callback |

---

### Filters

Filters format values for display in the view. Applied with the `|` pipe character.

#### Built-in Filters

```html
<!-- currency -->
{{ 1500.5 | currency }}               <!-- $1,500.50 -->
{{ 1500.5 | currency:"₹" }}          <!-- ₹1,500.50 -->

<!-- number -->
{{ 3.14159 | number:2 }}             <!-- 3.14 -->

<!-- date -->
{{ today | date:"dd/MM/yyyy" }}      <!-- 19/03/2026 -->
{{ today | date:"mediumDate" }}      <!-- Mar 19, 2026 -->

<!-- uppercase / lowercase -->
{{ "hello" | uppercase }}            <!-- HELLO -->

<!-- limitTo -->
{{ [1,2,3,4,5] | limitTo:3 }}       <!-- [1,2,3] -->
{{ "Hello World" | limitTo:5 }}      <!-- Hello -->

<!-- filter (search/filter arrays) -->
<li ng-repeat="p in products | filter:searchText">

<!-- orderBy -->
<li ng-repeat="p in products | orderBy:'price'">         <!-- asc -->
<li ng-repeat="p in products | orderBy:'-price'">        <!-- desc -->
<li ng-repeat="p in products | orderBy:['category','name']"> <!-- multi -->

<!-- json (debugging) -->
<pre>{{ myObj | json }}</pre>
```

#### Custom Filters

```javascript
// Filter to truncate long text
app.filter('truncate', function() {
  return function(text, limit) {
    if (!text) return '';
    limit = limit || 100;
    return text.length > limit ? text.substring(0, limit) + '...' : text;
  };
});
```

```html
<p>{{ product.description | truncate:50 }}</p>
```

#### Chaining Filters

```html
<!-- Apply multiple filters in sequence -->
<li ng-repeat="p in products | filter:query | orderBy:'price' | limitTo:5">
```

---

### Event Handling

```html
<!-- Mouse events -->
<button ng-click="save()">Save</button>
<div ng-dblclick="edit()">Double-click to edit</div>
<div ng-mouseenter="highlight()" ng-mouseleave="unhighlight()">Hover me</div>

<!-- Keyboard events -->
<input ng-keyup="onKeyUp($event)" ng-model="query" />
<input ng-keydown="onKeyDown($event)" />

<!-- Form events -->
<input ng-change="onValueChange()" ng-model="field" />
<input ng-focus="onFocus()" ng-blur="onBlur()" />
```

```javascript
app.controller('EventCtrl', function($scope) {

  $scope.onKeyUp = function(event) {
    if (event.keyCode === 13) {       // Enter key
      $scope.search($scope.query);
    }
  };

  $scope.highlight = function() {
    $scope.isHighlighted = true;
  };

  $scope.unhighlight = function() {
    $scope.isHighlighted = false;
  };
});
```

#### $event object

```html
<!-- Access the native event object -->
<button ng-click="handleClick($event)">Click</button>
```

```javascript
$scope.handleClick = function(event) {
  event.preventDefault();
  event.stopPropagation();
  console.log('Clicked at:', event.clientX, event.clientY);
};
```

---

## 3. AngularJS Services

### Services, Factories, and Providers

All three are used to create **singleton** shared objects. The difference is in how they're defined.

#### Service

```javascript
// 'this' is the service object
app.service('UserService', function($http) {
  var self = this;
  self.currentUser = null;

  self.login = function(credentials) {
    return $http.post('/api/login', credentials).then(function(res) {
      self.currentUser = res.data;
      return res.data;
    });
  };

  self.logout = function() {
    self.currentUser = null;
    return $http.post('/api/logout');
  };

  self.isLoggedIn = function() {
    return self.currentUser !== null;
  };
});
```

#### Factory

```javascript
// Returns an object (more flexible than service)
app.factory('CartFactory', function($http) {
  var cart = { items: [], total: 0 };

  function recalculate() {
    cart.total = cart.items.reduce(function(sum, item) {
      return sum + (item.price * item.qty);
    }, 0);
  }

  return {
    getItems: function() { return cart.items; },
    getTotal: function() { return cart.total; },
    addItem: function(product) {
      var existing = cart.items.find(function(i) { return i.id === product.id; });
      if (existing) {
        existing.qty++;
      } else {
        cart.items.push({ id: product.id, name: product.name, price: product.price, qty: 1 });
      }
      recalculate();
    },
    removeItem: function(productId) {
      cart.items = cart.items.filter(function(i) { return i.id !== productId; });
      recalculate();
    },
    clear: function() {
      cart.items = [];
      cart.total = 0;
    }
  };
});
```

#### Service vs Factory vs Provider

| | Service | Factory | Provider |
|---|---|---|---|
| Returns | Instance of function (`this`) | Any value (object, function) | Configurable before use |
| Use case | Simple shared state | Complex construction logic | Needs config phase setup |
| `app.config()` configurable? | No | No | Yes |

---

### Dependency Injection (DI)

AngularJS has a built-in **DI system** — you declare what you need, Angular provides it.

```javascript
// Implicit annotation (breaks on minification!)
app.controller('BadCtrl', function($scope, $http) { ... });

// Array annotation (minification-safe)
app.controller('GoodCtrl', ['$scope', '$http', function($scope, $http) {
  // use $scope and $http here
}]);

// $inject annotation (also minification-safe)
function MyController($scope, $http) { ... }
MyController.$inject = ['$scope', '$http'];
app.controller('MyController', MyController);
```

**Built-in injectable services:**

| Service | Purpose |
|---|---|
| `$scope` | Controller's data model |
| `$rootScope` | Global scope |
| `$http` | AJAX/HTTP requests |
| `$location` | Browser URL access |
| `$routeParams` | URL route parameters |
| `$timeout` | Wrapper for `setTimeout` |
| `$interval` | Wrapper for `setInterval` |
| `$window` | Wrapper for `window` object |
| `$log` | Logging service |
| `$filter` | Apply filters programmatically |
| `$q` | Promise service |

---

### HTTP Service (`$http`)

`$http` is the AngularJS wrapper around `XMLHttpRequest` (AJAX). It returns **promises**.

```javascript
app.controller('ProductCtrl', ['$scope', '$http', function($scope, $http) {

  var API = 'http://localhost:8080/api/products';

  // GET all
  $scope.loadProducts = function() {
    $http.get(API)
      .then(function(response) {
        $scope.products = response.data;      // success
      })
      .catch(function(error) {
        $scope.errorMessage = 'Failed to load products: ' + error.status;
      });
  };

  // GET by ID
  $scope.getProduct = function(id) {
    $http.get(API + '/' + id)
      .then(function(response) {
        $scope.selectedProduct = response.data;
      });
  };

  // POST (create)
  $scope.createProduct = function(product) {
    $http.post(API, product)
      .then(function(response) {
        $scope.products.push(response.data);
        $scope.newProduct = {};               // reset form
      });
  };

  // PUT (update)
  $scope.updateProduct = function(product) {
    $http.put(API + '/' + product.id, product)
      .then(function(response) {
        var idx = $scope.products.findIndex(function(p) { return p.id === product.id; });
        $scope.products[idx] = response.data;
      });
  };

  // DELETE
  $scope.deleteProduct = function(id) {
    $http.delete(API + '/' + id)
      .then(function() {
        $scope.products = $scope.products.filter(function(p) { return p.id !== id; });
      });
  };

  // Request with headers (e.g., auth token)
  $scope.getSecureData = function() {
    $http({
      method: 'GET',
      url: API + '/secure',
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token'),
        'Content-Type': 'application/json'
      }
    }).then(function(response) {
      $scope.secureData = response.data;
    });
  };

  $scope.loadProducts(); // init

}]);
```

#### $http Response Object

```
response = {
  data:    {},       // response body (parsed JSON)
  status:  200,      // HTTP status code
  headers: fn,       // function to get response headers
  config:  {},       // original request config
  statusText: 'OK'   // HTTP status text
}
```

#### Using $q for Promises

```javascript
app.service('DataService', function($q, $http) {
  this.fetchData = function() {
    var deferred = $q.defer();

    $http.get('/api/data')
      .then(function(res) {
        deferred.resolve(res.data);
      })
      .catch(function(err) {
        deferred.reject('Error: ' + err.status);
      });

    return deferred.promise;
  };
});

// In controller
DataService.fetchData()
  .then(function(data) { $scope.data = data; })
  .catch(function(msg) { $scope.error = msg; });
```

---

## 4. AngularJS Routing

### Setup

Install `angular-route` and add `ngRoute` as a dependency:

```html
<script src="angular.min.js"></script>
<script src="angular-route.min.js"></script>
```

```javascript
var app = angular.module('myApp', ['ngRoute']);
```

### Configuring Routes

```javascript
app.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {

  $routeProvider
    .when('/', {
      templateUrl: 'views/home.html',
      controller: 'HomeController'
    })
    .when('/products', {
      templateUrl: 'views/products.html',
      controller: 'ProductController'
    })
    .when('/products/:id', {
      templateUrl: 'views/product-detail.html',
      controller: 'ProductDetailController'
    })
    .when('/login', {
      templateUrl: 'views/login.html',
      controller: 'LoginController'
    })
    .otherwise({
      redirectTo: '/'
    });

  // Use HTML5 history API (removes # from URL)
  // $locationProvider.html5Mode(true);
}]);
```

### ng-view — Router Outlet

```html
<!-- index.html — the single page -->
<html ng-app="myApp">
<body>
  <nav>
    <a href="#!/">Home</a>
    <a href="#!/products">Products</a>
    <a href="#!/login">Login</a>
  </nav>

  <!-- Route templates render here -->
  <div ng-view></div>

</body>
</html>
```

### Reading Route Parameters

```javascript
app.controller('ProductDetailController', ['$scope', '$routeParams', '$http',
  function($scope, $routeParams, $http) {

    // URL: #!/products/42  →  $routeParams.id = "42"
    var productId = $routeParams.id;

    $http.get('/api/products/' + productId)
      .then(function(res) {
        $scope.product = res.data;
      });
  }
]);
```

### Programmatic Navigation

```javascript
app.controller('LoginController', ['$scope', '$location', 'AuthService',
  function($scope, $location, AuthService) {

    $scope.login = function(credentials) {
      AuthService.login(credentials).then(function(user) {
        $location.path('/products');   // navigate programmatically
      }).catch(function() {
        $scope.error = 'Invalid credentials';
      });
    };
  }
]);
```

### Route Guards (resolve)

```javascript
$routeProvider.when('/dashboard', {
  templateUrl: 'views/dashboard.html',
  controller: 'DashboardController',
  resolve: {
    // Must resolve before controller is created
    auth: ['AuthService', '$location', function(AuthService, $location) {
      if (!AuthService.isLoggedIn()) {
        $location.path('/login');
      }
    }],
    // Pre-load data before view renders
    products: ['ProductService', function(ProductService) {
      return ProductService.getAll();  // returns a promise
    }]
  }
});

// In controller, resolved values are injected by name
app.controller('DashboardController', ['$scope', 'products',
  function($scope, products) {
    $scope.products = products;   // already resolved!
  }
]);
```

### Complete SPA File Structure

```
project/
├── index.html              ← ng-app + ng-view
├── app.js                  ← module + routes
├── views/
│   ├── home.html
│   ├── products.html
│   └── product-detail.html
├── controllers/
│   ├── HomeController.js
│   ├── ProductController.js
│   └── ProductDetailController.js
└── services/
    └── ProductService.js
```

---

## 5. Jenkins

### What is Jenkins?

Jenkins is an **open-source automation server** used for **Continuous Integration (CI)** and **Continuous Delivery (CD)**. It automates building, testing, and deploying software.

```
Developer pushes code
      ↓
Jenkins detects change (via webhook or poll)
      ↓
Build (mvn clean install / npm run build)
      ↓
Unit Tests run automatically
      ↓
Code Quality Scan (SonarQube)
      ↓
Package artifact (JAR / Docker image)
      ↓
Deploy to staging/production
      ↓
Notify team (email / Slack)
```

### Key Jenkins Concepts

| Term | Definition |
|---|---|
| **Job / Project** | A runnable task configured in Jenkins |
| **Build** | A single execution of a job |
| **Pipeline** | A series of automated steps defined as code |
| **Node / Agent** | Machine where build runs (master or slave) |
| **Workspace** | Local directory on agent where build happens |
| **Artifact** | Output of a build (JAR, WAR, ZIP) |
| **Plugin** | Extends Jenkins functionality |
| **Webhook** | Git triggers Jenkins on push automatically |

### Jenkins Job Types

| Type | Use Case |
|---|---|
| **Freestyle Project** | Simple, GUI-configured builds |
| **Pipeline** | Complex builds defined in `Jenkinsfile` |
| **Multibranch Pipeline** | Auto-creates pipelines per git branch |
| **Folder** | Organizes multiple jobs |

### Jenkinsfile (Declarative Pipeline)

```groovy
pipeline {
    agent any                    // run on any available agent

    tools {
        maven 'Maven-3.9'        // use configured Maven installation
        jdk 'JDK-17'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')   // from Jenkins credentials
        APP_NAME    = 'my-spring-app'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/org/repo.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'   // publish test results
                }
            }
        }

        stage('Code Quality') {
            steps {
                sh '''
                    mvn sonar:sonar \
                      -Dsonar.projectKey=${APP_NAME} \
                      -Dsonar.host.url=http://localhost:9000 \
                      -Dsonar.login=${SONAR_TOKEN}
                '''
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            when {
                branch 'main'    // only deploy from main branch
            }
            steps {
                sh 'docker build -t ${APP_NAME}:latest .'
                sh 'docker-compose up -d'
            }
        }
    }

    post {
        success { echo 'Build succeeded!' }
        failure { mail to: 'team@company.com', subject: 'Build Failed', body: 'Check Jenkins.' }
        always  { cleanWs() }  // clean workspace after build
    }
}
```

### Jenkins Setup Checklist

```
1. Install Jenkins (WAR / Docker)
2. Install recommended plugins
3. Configure JDK & Maven in Global Tools
4. Add credentials (Git token, SonarQube token)
5. Create Job → Link to Git repo
6. Set build trigger (GitHub webhook / Poll SCM)
7. Add build steps (mvn commands)
8. Add post-build actions (publish test results, notify)
```

### SonarLint

SonarLint is an **IDE plugin** (IntelliJ, VS Code) that provides real-time code quality feedback **as you type** — before code is committed.

- Install: IntelliJ → Plugins → SonarLint
- Highlights issues: bugs, code smells, vulnerabilities
- Can connect to SonarQube/SonarCloud for shared rule sets
- Works offline (standalone) or connected mode

---

## 6. Code Quality Analysis

### SonarQube vs SonarCloud

| Feature | SonarQube | SonarCloud |
|---|---|---|
| **Hosting** | Self-hosted (on-premise) | Cloud (SaaS by Sonar) |
| **Cost** | Free Community / Paid Enterprise | Free for open-source / Paid private |
| **Setup** | Install server, configure | Sign in with GitHub, instant setup |
| **Maintenance** | You manage server, updates | Managed by Sonar |
| **Integration** | Jenkins, GitHub Actions, etc. | GitHub, GitLab, Azure DevOps |
| **Best for** | Private/enterprise, full control | Open-source, quick cloud CI setup |

### Code Smells

Code smells are **maintainability issues** — the code works but is poorly written.

| Category | Example | Fix |
|---|---|---|
| **Long Method** | Method with 200 lines | Extract smaller methods |
| **Duplicate Code** | Same logic copy-pasted | Extract to utility method |
| **Magic Numbers** | `if (status == 3)` | Use named constant: `ACTIVE = 3` |
| **Dead Code** | Unused methods/variables | Delete them |
| **God Class** | One class does everything | Apply Single Responsibility |
| **Long Parameter List** | `createOrder(name, qty, price, discount, tax, shipping)` | Use a DTO/object |
| **Switch Statements** | Large switch on type | Use polymorphism |

```java
// Code Smell: Magic Numbers
if (user.getRole() == 1) { ... }          // BAD

// Clean Code
private static final int ADMIN_ROLE = 1;
if (user.getRole() == ADMIN_ROLE) { ... } // GOOD

// Code Smell: Long Method — extract to smaller methods
// Code Smell: Duplicate Code — extract to shared service
```

### Bugs

Bugs are **reliability issues** — code that will likely break at runtime.

| Bug Type | Example |
|---|---|
| **Null Pointer** | Accessing object without null check |
| **Resource Leak** | Opening file/DB connection, never closing |
| **Incorrect Condition** | `if (a = b)` instead of `if (a == b)` |
| **Array Index Out of Bounds** | Accessing `arr[arr.length]` |
| **Integer Overflow** | `int result = Integer.MAX_VALUE + 1` |
| **Thread Safety** | Modifying shared state without synchronization |

```java
// Bug: Null dereference
String name = user.getName();
System.out.println(name.toUpperCase()); // NPE if name is null

// Fix
if (name != null) {
    System.out.println(name.toUpperCase());
}
// Or use Optional
Optional.ofNullable(user.getName())
        .map(String::toUpperCase)
        .ifPresent(System.out::println);
```

### Vulnerabilities

Vulnerabilities are **security issues** — code that can be exploited.

| Vulnerability | Description | Example |
|---|---|---|
| **SQL Injection** | User input in raw SQL | `"SELECT * WHERE name='" + input + "'"` |
| **XSS** | Injecting JS into HTML | Displaying unsanitized user input |
| **Hardcoded Credentials** | Password in source code | `password = "admin123"` |
| **Sensitive Data Exposure** | Logging passwords/tokens | `log.info("Password: " + pass)` |
| **Insecure Deserialization** | Deserializing untrusted data | Java ObjectInputStream risks |
| **Broken Authentication** | Weak session management | No CSRF token on forms |

```java
// VULNERABILITY: SQL Injection
String sql = "SELECT * FROM users WHERE name='" + userInput + "'";

// FIX: Parameterized query
String sql = "SELECT * FROM users WHERE name = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, userInput);

// VULNERABILITY: Hardcoded credentials
String password = "Root123";   // Sonar will flag this!

// FIX: Use environment variable or config server
String password = System.getenv("DB_PASSWORD");
```

### SonarQube Quality Gate

A **Quality Gate** is a pass/fail threshold that must be met before code can be merged/deployed.

```
Quality Gate: "Sonar Way" (default)
  ✓ Coverage on New Code >= 80%
  ✓ Duplicated Lines on New Code <= 3%
  ✓ Maintainability Rating = A
  ✓ Reliability Rating = A
  ✓ Security Rating = A
  ✓ 0 New Bugs
  ✓ 0 New Vulnerabilities

Status: PASSED ✓ / FAILED ✗
```

### OWASP Top 10 (Security Reference)

1. Broken Access Control
2. Cryptographic Failures
3. **Injection** (SQL, NoSQL, Command injection)
4. Insecure Design
5. Security Misconfiguration
6. Vulnerable & Outdated Components
7. Identification & Authentication Failures
8. Software & Data Integrity Failures
9. Security Logging & Monitoring Failures
10. **Server-Side Request Forgery (SSRF)**

---

## 7. Interview Questions & Answers

### AngularJS Basics

**Q1. What is AngularJS and how is it different from Angular (2+)?**

> AngularJS (1.x) is a JavaScript MVC framework using `$scope`, controllers, and HTML directives. Angular (2+) is a complete rewrite in TypeScript using components, decorators, and a completely different architecture. They are not compatible with each other.

---

**Q2. What is two-way data binding? How does it work in AngularJS?**

> Two-way binding means the model (`$scope`) and view (HTML) stay in sync automatically. When the user types in an `ng-model` input, `$scope` updates. When `$scope` changes in JS, the view re-renders. AngularJS achieves this via **dirty checking** in the `$digest` cycle — it compares the current and previous values of all watched expressions.

---

**Q3. What is the `$digest` cycle?**

> The `$digest` cycle is AngularJS's change detection mechanism. It iterates over all `$watch` expressions, comparing current vs. previous values. If any value changed, the corresponding listener is triggered and the cycle repeats (up to 10 times) until no more changes are detected ("stable"). `$apply()` triggers a `$digest` from outside Angular code (e.g., setTimeout callbacks).

---

**Q4. What is the difference between `ng-if` and `ng-show`?**

> `ng-if` **removes** the element from the DOM entirely when false (and recreates it when true — creates/destroys child scope). `ng-show` keeps the element in the DOM but toggles `display:none`. Use `ng-if` when the element is rarely shown (saves memory). Use `ng-show` for toggled elements that need to preserve state.

---

**Q5. What is the difference between `$scope` and `$rootScope`?**

> `$scope` is local to a controller. `$rootScope` is the global scope accessible throughout the entire app. Variables on `$rootScope` are available everywhere. However, overusing `$rootScope` is an anti-pattern — use services to share state instead.

---

**Q6. What is the difference between a Service, Factory, and Provider?**

> All three create singletons. **Service** — AngularJS calls `new` on your function, so you attach to `this`. **Factory** — you return an object explicitly, giving more flexibility (conditional creation, closures). **Provider** — most flexible, configurable during `app.config()` phase before the app runs. Rule of thumb: use Factory for most cases, Service for simple cases, Provider when config-phase setup is needed.

---

**Q7. What is Dependency Injection? Why does AngularJS use it?**

> DI is a design pattern where objects declare their dependencies instead of creating them. AngularJS's injector reads the parameter names (or `$inject` annotations) and provides the right instances. Benefits: loose coupling, easier testing (swap real service with mock), and centralized object creation.

---

**Q8. Why does AngularJS DI break with minification?**

> Minifiers rename `function($scope, $http)` to `function(a, b)` — AngularJS can no longer match names. Solution: use array annotation `['$scope', '$http', function($scope, $http) {}]` or `$inject` property, since strings are not renamed.

---

**Q9. What is `ng-repeat` and what is `track by`?**

> `ng-repeat` renders a DOM element for each item in an array/object. Without `track by`, AngularJS uses `$$hashKey` to track identity, re-rendering all elements even when only one changes. `track by product.id` tells AngularJS to use the `id` field — only changed items re-render, improving performance significantly for large lists.

---

**Q10. What is a custom directive? Explain `restrict`, `scope`, `link`.**

> A custom directive extends HTML with new behavior. `restrict: 'E'` means used as an element tag. `scope: { product: '=' }` creates an **isolated scope** with two-way binding on `product`. The `link` function runs after the element is compiled and linked to the DOM — ideal for direct DOM manipulation, event listeners, and integrating third-party libraries.

---

**Q11. Explain the AngularJS application lifecycle.**

```
1. Browser loads index.html
2. angular.js script loads
3. AngularJS looks for ng-app directive
4. Module loads and runs .config() blocks
5. .run() blocks execute
6. $compile processes the DOM (replaces directives/bindings)
7. $digest cycle starts — watches initialized
8. View rendered to user
```

---

**Q12. How do you share data between controllers?**

> Best practice: use a **Service** or **Factory** (singleton). All controllers that inject the same service share the same instance. Alternative: `$rootScope` (not recommended for complex apps). Anti-pattern: using `$scope.$parent` to access parent scope.

---

**Q13. What is a filter? Write a custom filter.**

> A filter transforms data for display. Built-in: `currency`, `date`, `uppercase`, `orderBy`, `filter`. Custom filters are functions that return a transform function.
> ```javascript
> app.filter('rupee', function() {
>   return function(amount) {
>     return '₹' + parseFloat(amount).toFixed(2);
>   };
> });
> // Usage: {{ price | rupee }}
> ```

---

**Q14. How does routing work in AngularJS?**

> Add `ngRoute` dependency, configure routes in `app.config()` using `$routeProvider`, place `<div ng-view>` in `index.html` as the outlet. Each route maps a URL pattern to a template and controller. Route parameters accessed via `$routeParams`. Navigation with `$location.path('/route')`.

---

### Jenkins & CI/CD

**Q15. What is Continuous Integration (CI)?**

> CI is the practice of frequently merging developer code into a shared repository (multiple times a day). Each merge triggers an automated build and test suite. Goal: catch integration bugs early, ensure the main branch is always buildable.

**Q16. What is the difference between CI and CD?**

> **CI** = Continuous Integration (automated build + test on every commit). **CD** = Continuous Delivery (automated deployment to staging, manual trigger to production) OR Continuous Deployment (fully automated all the way to production). Jenkins covers both.

**Q17. What is a Jenkinsfile?**

> A `Jenkinsfile` is a text file stored in the repository that defines the Jenkins pipeline as code. Benefits: version-controlled alongside the app, peer-reviewed like any code, consistent pipeline across branches.

**Q18. What is the difference between Freestyle and Pipeline jobs in Jenkins?**

> Freestyle jobs are configured via the Jenkins GUI — simple but limited, not version-controlled. Pipeline jobs are defined in a `Jenkinsfile` using Groovy DSL — more powerful, supports complex logic, parallel stages, and version control. Pipeline is the modern recommended approach.

---

### Code Quality

**Q19. What is a Code Smell?**

> A code smell is a characteristic of source code that indicates a deeper problem — the code works but is hard to maintain. Examples: duplicate code, long methods, magic numbers, dead code. Sonar detects them and assigns a **Maintainability Rating** (A-E).

**Q20. What is the difference between a Bug and a Vulnerability in SonarQube?**

> **Bug** = a reliability issue — code that will likely produce incorrect behavior at runtime (null dereference, resource leak). **Vulnerability** = a security issue — code that can be exploited by an attacker (SQL injection, hardcoded passwords). Sonar assigns a **Security Rating** based on vulnerabilities.

**Q21. What is a Quality Gate?**

> A Quality Gate is a configurable pass/fail policy on code metrics. If new code doesn't meet thresholds (e.g., coverage < 80%, new bugs > 0), the gate fails and the CI pipeline can be blocked from deploying. Enforces team code quality standards automatically.

**Q22. What is SonarLint vs SonarQube?**

> SonarLint is an IDE plugin giving real-time feedback as you write code (shift-left). SonarQube is a CI server-side tool that scans the full codebase after commit. SonarLint can connect to SonarQube/SonarCloud to share the same rule set, ensuring consistency.

**Q23. What is SQL Injection and how do you prevent it?**

> SQL Injection is when attacker input is interpreted as SQL commands. E.g., input `' OR 1=1 --` bypasses authentication. Prevention: always use **parameterized queries / PreparedStatement**, never concatenate user input into SQL strings. ORMs like JPA/Hibernate use parameterized queries by default.

**Q24. What is XSS and how is it prevented?**

> XSS (Cross-Site Scripting) is when malicious JavaScript is injected into a webpage and executed in other users' browsers. Prevention: sanitize/escape all user-generated content before rendering, use Content Security Policy (CSP) headers, avoid `innerHTML` with user data. AngularJS auto-escapes `{{ }}` expressions to prevent XSS.

---

*End of AngularJS 1.x Study Notes — Week 7*
