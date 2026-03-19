# ShopEasy Frontend — AngularJS Step-by-Step Implementation Guide

> **How to use this guide:**
> Each module explains the *concept first*, then gives you *exact numbered steps* to write the code yourself.
> The goal is learning by doing — every line you type is explained so you understand *why*, not just *what*.
> Cross-reference the **ANGULARJS_STUDY_GUIDE.md** for deeper theory on any concept.

---

## Table of Contents

| Module | What You Build | Key Concepts Learned |
|--------|---------------|----------------------|
| [Module 0](#module-0-understand-the-project) | Understand the project | Project map, API contracts |
| [Module 1](#module-1-bootstrap--skeleton-app) | Skeleton app runs in browser | `ng-app`, module, `ng-view`, routing |
| [Module 2](#module-2-authservice--jwt-token-management) | AuthService | Services, localStorage, JWT |
| [Module 3](#module-3-wire-jwt--authinterceptor--run-block) | authInterceptor + run() | Interceptors, `$rootScope`, `$q` |
| [Module 4](#module-4-full-indexhtml--navbar--global-alert) | Full index.html | `$rootScope` in templates, script load order |
| [Module 5](#module-5-auth-routes--authcontroller--login--register-views) | Login + Register pages | Controllers, form validation, `$location` |
| [Module 6](#module-6-productservice--productcontroller--products-view) | Products page | `ng-repeat`, filters, per-product state |
| [Module 7](#module-7-cartservice--cartcontroller--cart-view) | Cart + Checkout | Nested state, PUT with params, checkout flow |
| [Module 8](#module-8-orderservice--ordercontroller--orders-view) | Orders page | `date` filter, `ng-class`, detail panel |
| [Module 9](#module-9-final-wiring--complete-app) | Wire everything together | Script load order, route defaults |
| [Module 10](#module-10-debugging--common-mistakes) | Debug the app | DevTools, `$scope`, common pitfalls |

---

## Module 0: Understand the Project

### What Exists Right Now

```
frontend/
├── index.html              ← minimal skeleton (no navbar, no Bootstrap CSS)
├── app.js                  ← module + /products + /cart routes ONLY (incomplete)
├── css/
│   └── style.css           ← ✅ complete (already written)
├── services/
│   └── productService.js   ← partial (missing getByCategory)
├── controllers/
│   ├── authController.js   ← EMPTY (1 line)
│   └── productController.js ← partial (fetches but doesn't populate $scope)
└── views/
    └── products.html       ← just a heading ("This is Products View...")
```

### What You Will Build (all missing files)

```
frontend/
├── index.html              ← REWRITE with navbar, alert, Bootstrap CSS, all scripts
├── app.js                  ← ADD authInterceptor + run() block + /login /register /orders routes
├── services/
│   ├── authService.js      ← CREATE (login, register, token management)
│   ├── cartService.js      ← CREATE (cart CRUD)
│   └── orderService.js     ← CREATE (place order, fetch orders)
├── controllers/
│   ├── authController.js   ← WRITE (login + register logic)
│   ├── cartController.js   ← CREATE (cart display, quantity, checkout)
│   └── orderController.js  ← CREATE (order list, detail panel)
└── views/
    ├── login.html          ← CREATE
    ├── register.html       ← CREATE
    ├── cart.html           ← CREATE
    └── orders.html         ← CREATE
```

### Backend API Contract (what each service expects)

**User Service — no JWT needed**
```
POST /api/auth/register    body: { username, password, firstName, lastName, mobileNumber }
POST /api/auth/login       body: { username, password }
                           both return: { token, username, role }
```

**Product Service — GET requests are public, no JWT needed**
```
GET /api/products                   → list all products
GET /api/products/{id}              → one product
GET /api/products/search?q={query}  → search by name
GET /api/products/category/{name}   → filter by category
```

**Cart & Order Service — JWT required**
```
GET    /api/cart                            → get my cart
POST   /api/cart/add                        body: { productId, quantity }
PUT    /api/cart/items/{itemId}?quantity=N  → update item quantity
DELETE /api/cart/items/{itemId}             → remove item
DELETE /api/cart/clear                      → empty cart

POST   /api/orders          body: { shippingAddress }  → place order
GET    /api/orders                                     → my order history
GET    /api/orders/{orderId}                           → one order detail
```

**How JWT works in this system:**
```
You → POST /api/auth/login with username/password
    ← Server returns { token, username, role }
    → Save token in localStorage
    → Every subsequent request includes: Authorization: Bearer <token>
    → API Gateway validates the token
    → Gateway extracts userId from token, adds X-User-Id header
    → Backend services use X-User-Id (you never send it manually)
```

---

## Module 1: Bootstrap — Skeleton App

### Concept: How AngularJS Starts Up

When your browser loads `index.html`, AngularJS scans the DOM for `ng-app`. Once found, it:
1. Loads the named module (`ecommerceApp`)
2. Runs `.config()` — sets up routes and providers
3. Runs `.run()` — initializes global state
4. Reads the current URL hash (e.g., `#!/products`) and loads the matching view into `ng-view`

```
Browser loads index.html
      ↓
Finds ng-app="ecommerceApp"
      ↓
Loads the module (app.js), runs .config()  ← sets up routes
      ↓
Runs .run()                                ← sets up $rootScope
      ↓
URL is #!/products → loads views/products.html into <div ng-view>
      ↓
ProductController runs → $scope.products populated → DOM updates
```

**The `ng-view` directive** is the "router outlet" — a placeholder where Angular swaps different view templates as the user navigates.

### Step 1.1 — Update `index.html` (Minimal Working Skeleton)

Open `frontend/index.html` and rewrite it completely:

```html
<!DOCTYPE html>
<html lang="en" ng-app="ecommerceApp">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ShopEasy</title>
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <!-- Your custom CSS -->
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

    <!-- Navigation Bar (placeholder for now) -->
    <nav class="navbar navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="#!/products">
                <i class="bi bi-shop"></i> ShopEasy
            </a>
            <a href="#!/products" class="text-white">Products</a>
        </div>
    </nav>

    <!-- Page Content — ng-view swaps templates here -->
    <main class="container mt-4">
        <div ng-view></div>
    </main>

    <!-- 1. AngularJS framework (load FIRST) -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.8.3/angular.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/angular-route/1.8.3/angular-route.min.js"></script>
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <!-- 2. App module (load SECOND — must be defined before services/controllers) -->
    <script src="app.js"></script>

    <!-- 3. Services (load BEFORE controllers that use them) -->
    <script src="services/productService.js"></script>

    <!-- 4. Controllers (load LAST) -->
    <script src="controllers/productController.js"></script>

</body>
</html>
```

> **Why this load order?** AngularJS registers components (`service`, `controller`) on the module. The module must exist first (`app.js`), then services (controllers depend on services), then controllers.

### Step 1.2 — Verify `views/products.html` Shows Something

Open `frontend/views/products.html` and write:

```html
<div class="text-center py-5">
    <h2>Products Page</h2>
    <p class="text-muted">Loading products from the backend...</p>
</div>
```

### Step 1.3 — Open the App in a Browser

Open `index.html` in Chrome using a local server (or Live Server in VS Code). You should see:
- The dark navbar with "ShopEasy"
- The products placeholder text in the main content area
- URL should change to `#!/products`

**If you see a blank page:** Open DevTools (F12) → Console. A common error at this stage is `Module 'ngRoute' is not available` — it means `angular-route.min.js` didn't load. Check the script tags.

---

## Module 2: AuthService — JWT Token Management

### Concept: Services in AngularJS

A **service** is a **singleton** — one instance created once, shared across the whole app. Perfect for:
- Wrapping API calls (AuthService, ProductService, etc.)
- Storing state that must survive across controller changes (token, user info)

```javascript
// Syntax: .service(name, ['dep1', 'dep2', function(dep1, dep2) { ... }])
// Methods attached to 'this' become the public API of the service
angular.module('ecommerceApp')
    .service('AuthService', ['$http', function($http) {
        this.login = function(credentials) { ... };
        this.logout = function() { ... };
    }]);
```

**Why localStorage?**
JWT tokens must persist across page refreshes. `$scope` and `$rootScope` are cleared on refresh. `localStorage` survives until explicitly cleared.

```
Login succeeds → token saved in localStorage
Page refresh   → app reads token from localStorage (still logged in)
Logout         → token removed from localStorage
Token expires  → API returns 401 → authInterceptor clears token and redirects to /login
```

### Step 2.1 — Create `services/authService.js`

Create the file `frontend/services/authService.js`:

```javascript
'use strict';

angular.module('ecommerceApp')
    .service('AuthService', ['$http', function($http) {

        var API_URL = 'http://localhost:8080/api/auth';

        // ── Private helper: saves token + user to localStorage ──────────
        function saveSession(data) {
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify({
                username: data.username,
                role: data.role
            }));
        }

        // ── Public Methods ───────────────────────────────────────────────

        // POST /api/auth/register
        // request body: { username, password, firstName, lastName, mobileNumber }
        this.register = function(userData) {
            return $http.post(API_URL + '/register', userData)
                .then(function(response) {
                    saveSession(response.data);   // auto-login after register
                    return response;
                });
        };

        // POST /api/auth/login
        // request body: { username, password }
        this.login = function(credentials) {
            return $http.post(API_URL + '/login', credentials)
                .then(function(response) {
                    saveSession(response.data);   // save token on success
                    return response;
                });
        };

        // Remove all auth data from localStorage
        this.logout = function() {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        };

        // Returns true if a token exists in localStorage
        this.isLoggedIn = function() {
            return !!localStorage.getItem('token');
            // !! converts any value to boolean:
            // null → false, "eyJ..." → true
        };

        // Returns the raw JWT string (used by authInterceptor)
        this.getToken = function() {
            return localStorage.getItem('token');
        };

        // Returns the user object { username, role } or null
        this.getUser = function() {
            var userStr = localStorage.getItem('user');
            return userStr ? JSON.parse(userStr) : null;
        };

    }]);
```

### Step 2.2 — Add the Script Tag to `index.html`

In `index.html`, add the authService script tag right after `app.js` (services load before controllers):

```html
<script src="app.js"></script>

<!-- Services -->
<script src="services/authService.js"></script>    <!-- ← ADD THIS LINE -->
<script src="services/productService.js"></script>

<!-- Controllers -->
<script src="controllers/productController.js"></script>
```

### Step 2.3 — Verify in Browser Console

After saving, open the browser console and type:
```javascript
// Get the AngularJS injector
var injector = angular.element(document.body).injector();
var auth = injector.get('AuthService');

auth.isLoggedIn();       // → false (no token yet)
auth.getUser();          // → null
```

If you get the AuthService back without errors, the service is registered correctly.

---

## Module 3: Wire JWT — authInterceptor + run() Block

### Concept: HTTP Interceptors

An **interceptor** is middleware that runs on *every* `$http` request and response. You use it to:
- Add the JWT `Authorization` header to every outgoing request (so you don't repeat it in every service)
- Handle 401 responses globally (expired token → auto logout)

```
Your code calls CartService.getCart()
      ↓
$http.get('/api/cart')
      ↓
authInterceptor.request() runs  ← ADDS "Authorization: Bearer eyJ..."
      ↓
API Gateway validates token → routes to cart service
      ↓
authInterceptor.responseError() runs if response is 401 ← AUTO LOGOUT
      ↓
.then() in CartController runs
```

**The circular dependency problem:** `authInterceptor` needs `AuthService` to get the token. `AuthService` uses `$http` to make API calls. `$http` needs `authInterceptor`. This creates a circle. The fix is **lazy injection** — get `AuthService` at call time using `$injector.get()` instead of injecting it upfront.

### Concept: $rootScope and the run() Block

`$rootScope` is the **parent of all scopes** — any property you set on it is available in every template in the app. Use it for truly global state: auth status, cart count, notifications.

`.run()` executes **once** after all configuration is done. It's where you initialize `$rootScope`.

### Step 3.1 — Add authInterceptor + run() Block to `app.js`

Open `frontend/app.js`. Currently it only has `.config(...)`. You need to:
1. Register `authInterceptor` with `$httpProvider` inside `.config()`
2. Add a `.factory('authInterceptor', ...)` definition
3. Add a `.run(...)` block at the end

Rewrite `app.js` completely:

```javascript
'use strict';

angular.module('ecommerceApp', ['ngRoute'])

    // ════════════════════════════════════════════════════════════════════
    //  CONFIG BLOCK — runs before the app starts
    //  Can only inject Providers ($routeProvider, $httpProvider)
    // ════════════════════════════════════════════════════════════════════
    .config(['$routeProvider', '$httpProvider',
        function($routeProvider, $httpProvider) {

            // ── Routes ──────────────────────────────────────────────────
            $routeProvider

                // Default: redirect / to /products
                .when('/', { redirectTo: '/products' })

                // Public routes (no auth guard)
                .when('/login', {
                    templateUrl: 'views/login.html',
                    controller:  'AuthController'
                })
                .when('/register', {
                    templateUrl: 'views/register.html',
                    controller:  'AuthController'
                })

                // Public: product listing
                .when('/products', {
                    templateUrl: 'views/products.html',
                    controller:  'ProductController'
                })

                // Protected: require login (resolve guard)
                .when('/cart', {
                    templateUrl: 'views/cart.html',
                    controller:  'CartController',
                    resolve: {
                        // 'auth' is just a name for this resolve function
                        // It runs BEFORE CartController initializes
                        auth: ['AuthService', '$location',
                            function(AuthService, $location) {
                                if (!AuthService.isLoggedIn()) {
                                    $location.path('/login');
                                }
                            }
                        ]
                    }
                })
                .when('/orders', {
                    templateUrl: 'views/orders.html',
                    controller:  'OrderController',
                    resolve: {
                        auth: ['AuthService', '$location',
                            function(AuthService, $location) {
                                if (!AuthService.isLoggedIn()) {
                                    $location.path('/login');
                                }
                            }
                        ]
                    }
                })

                // Fallback for any unknown URL
                .otherwise({ redirectTo: '/products' });

            // ── Register the JWT interceptor ─────────────────────────────
            // This tells $http to run authInterceptor on every request/response
            $httpProvider.interceptors.push('authInterceptor');
        }
    ])

    // ════════════════════════════════════════════════════════════════════
    //  AUTH INTERCEPTOR FACTORY
    //  Runs on every $http call — adds JWT and handles 401
    // ════════════════════════════════════════════════════════════════════
    .factory('authInterceptor', ['$q', '$injector',
        function($q, $injector) {
            return {

                // ── request: called before every outgoing $http call ─────
                request: function(config) {
                    // Use $injector.get() to avoid circular dependency:
                    // authInterceptor → AuthService → $http → authInterceptor
                    var AuthService = $injector.get('AuthService');
                    var token = AuthService.getToken();

                    if (token) {
                        // Ensure headers object exists, then add the JWT
                        config.headers = config.headers || {};
                        config.headers['Authorization'] = 'Bearer ' + token;
                    }

                    // MUST return config — or the request hangs forever!
                    return config;
                },

                // ── responseError: called when server returns an error ───
                responseError: function(rejection) {
                    if (rejection.status === 401) {
                        // Token expired or invalid — force logout and redirect
                        $injector.get('AuthService').logout();
                        $injector.get('$location').path('/login');
                    }

                    // MUST return $q.reject() to propagate the error to .catch()
                    return $q.reject(rejection);
                }
            };
        }
    ])

    // ════════════════════════════════════════════════════════════════════
    //  RUN BLOCK — runs after config, injector is fully set up
    //  Can inject Services (AuthService, $location, etc.)
    //  Sets up $rootScope — the global state available in every template
    // ════════════════════════════════════════════════════════════════════
    .run(['$rootScope', 'AuthService', '$location',
        function($rootScope, AuthService, $location) {

            // ── Auth state ───────────────────────────────────────────────
            // isLoggedIn() is called from navbar templates: ng-show="isLoggedIn()"
            $rootScope.isLoggedIn = function() {
                return AuthService.isLoggedIn();
            };

            // currentUser = { username, role } — shown in navbar
            $rootScope.currentUser = AuthService.getUser();

            // ── Cart count (badge on navbar cart icon) ───────────────────
            $rootScope.cartCount = 0;

            // ── Global logout ────────────────────────────────────────────
            // Called by the navbar Logout button: ng-click="logout()"
            $rootScope.logout = function() {
                AuthService.logout();
                $rootScope.currentUser = null;
                $rootScope.cartCount = 0;
                $location.path('/login');
            };

            // ── Global alert notification system ─────────────────────────
            // Any controller can call: $rootScope.showAlert('message', 'success')
            // The navbar displays it using: ng-show="globalAlert.message"
            $rootScope.showAlert = function(message, type) {
                $rootScope.globalAlert = { message: message, type: type || 'info' };

                // Auto-hide after 4 seconds
                // setTimeout is NOT AngularJS-aware, so we must use $apply
                // to tell AngularJS: "something changed, re-render the view"
                setTimeout(function() {
                    $rootScope.$apply(function() {
                        $rootScope.globalAlert = {};
                    });
                }, 4000);
            };

            // ── Refresh user state on every navigation ───────────────────
            // $routeChangeStart fires before each route loads
            // We re-read localStorage so the navbar stays in sync
            $rootScope.$on('$routeChangeStart', function() {
                $rootScope.currentUser = AuthService.getUser();
            });
        }
    ]);
```

**Why `.config()` vs `.run()`?**
- `.config()` runs before the injector is created → can only use providers (things with `Provider` suffix, like `$routeProvider`)
- `.run()` runs after → can use fully constructed services (`AuthService`, `$location`, etc.)

### Step 3.2 — Verify the run() Block

Reload the app, open Console, type:
```javascript
angular.element(document.body).scope().$root.isLoggedIn()   // → false
angular.element(document.body).scope().$root.cartCount      // → 0
```

---

## Module 4: Full `index.html` — Navbar + Global Alert

### Concept: $rootScope in Templates

Because `$rootScope` is the parent of every scope, its properties are accessible in *any* template — including `index.html` which lives outside `ng-view`. This is how the navbar can show `cartCount`, `currentUser`, and call `logout()` from `app.js`.

```html
<!-- $rootScope.currentUser — accessible directly (no prefix) -->
<span>{{currentUser.username}}</span>

<!-- $rootScope.isLoggedIn() — function accessible directly -->
<li ng-show="isLoggedIn()">My Cart</li>
<li ng-show="!isLoggedIn()">Login</li>

<!-- $rootScope.logout() — called by button -->
<button ng-click="logout()">Logout</button>
```

### Step 4.1 — Rewrite `index.html` with Full Navbar

Replace the entire content of `frontend/index.html`:

```html
<!DOCTYPE html>
<html lang="en" ng-app="ecommerceApp">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ShopEasy — E-Commerce Store</title>

    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <!-- Custom styles -->
    <link rel="stylesheet" href="css/style.css">
</head>

<body>

    <!-- ═══════════════════════════════════════════════════════════
         NAVBAR — always visible, uses $rootScope properties
         ng-show/ng-hide toggle links based on auth state
    ════════════════════════════════════════════════════════════════ -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm">
        <div class="container">

            <!-- Brand -->
            <a class="navbar-brand fw-bold" href="#!/products">
                <i class="bi bi-shop-window me-2"></i>ShopEasy
            </a>

            <!-- Hamburger toggle (mobile) -->
            <button class="navbar-toggler" type="button"
                    data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>

            <!-- Nav Links -->
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <!-- Always visible -->
                    <li class="nav-item">
                        <a class="nav-link" href="#!/products">
                            <i class="bi bi-grid me-1"></i>Products
                        </a>
                    </li>
                    <!-- Only shown when logged in -->
                    <li class="nav-item" ng-show="isLoggedIn()">
                        <a class="nav-link" href="#!/orders">
                            <i class="bi bi-receipt me-1"></i>My Orders
                        </a>
                    </li>
                </ul>

                <!-- Right side: Cart + User info + Auth buttons -->
                <ul class="navbar-nav ms-auto align-items-center">

                    <!-- Cart link with item count badge — logged in only -->
                    <li class="nav-item me-2" ng-show="isLoggedIn()">
                        <a class="nav-link position-relative" href="#!/cart">
                            <i class="bi bi-cart3 fs-5"></i>
                            <!-- Badge only shown when cartCount > 0 -->
                            <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
                                  ng-show="cartCount > 0">
                                {{cartCount}}
                            </span>
                        </a>
                    </li>

                    <!-- Logged in: show username + logout -->
                    <li class="nav-item dropdown" ng-show="isLoggedIn()">
                        <a class="nav-link dropdown-toggle text-white" href="#"
                           data-bs-toggle="dropdown">
                            <i class="bi bi-person-circle me-1"></i>
                            {{currentUser.username}}
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li>
                                <span class="dropdown-item-text text-muted small">
                                    Role: {{currentUser.role}}
                                </span>
                            </li>
                            <li><hr class="dropdown-divider"></li>
                            <li>
                                <!-- ng-click calls $rootScope.logout() -->
                                <a class="dropdown-item text-danger" href="#"
                                   ng-click="logout()">
                                    <i class="bi bi-box-arrow-right me-2"></i>Logout
                                </a>
                            </li>
                        </ul>
                    </li>

                    <!-- Not logged in: show Login + Register links -->
                    <li class="nav-item" ng-show="!isLoggedIn()">
                        <a class="nav-link" href="#!/login">
                            <i class="bi bi-box-arrow-in-right me-1"></i>Login
                        </a>
                    </li>
                    <li class="nav-item" ng-show="!isLoggedIn()">
                        <a class="nav-link" href="#!/register">
                            <i class="bi bi-person-plus me-1"></i>Register
                        </a>
                    </li>

                </ul>
            </div>
        </div>
    </nav>

    <!-- ═══════════════════════════════════════════════════════════
         GLOBAL ALERT BANNER
         $rootScope.showAlert('msg', 'success') triggers this
         ng-show hides it when globalAlert.message is empty
    ════════════════════════════════════════════════════════════════ -->
    <div class="container mt-2" ng-show="globalAlert && globalAlert.message">
        <div class="alert d-flex align-items-center justify-content-between"
             ng-class="'alert-' + globalAlert.type">
            <span>
                <i class="bi bi-info-circle me-2"></i>
                {{globalAlert.message}}
            </span>
            <!-- Dismiss button clears the alert -->
            <button class="btn-close" ng-click="globalAlert = {}"></button>
        </div>
    </div>

    <!-- ═══════════════════════════════════════════════════════════
         MAIN CONTENT — ng-view swaps view templates here
    ════════════════════════════════════════════════════════════════ -->
    <main class="container mt-4">
        <div ng-view></div>
    </main>

    <!-- ═══════════════════════════════════════════════════════════
         FOOTER
    ════════════════════════════════════════════════════════════════ -->
    <footer class="footer bg-dark text-white text-center py-3 mt-5">
        <small>&copy; 2026 ShopEasy. Built with AngularJS + Spring Boot Microservices.</small>
    </footer>

    <!-- ═══════════════════════════════════════════════════════════
         SCRIPTS — ORDER MATTERS!
         1. Framework libs (angular, angular-route, bootstrap)
         2. app.js  ← MUST be first (defines the module)
         3. Services ← BEFORE controllers (controllers depend on services)
         4. Controllers ← last
    ════════════════════════════════════════════════════════════════ -->
    <!-- Framework -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.8.3/angular.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/angular-route/1.8.3/angular-route.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <!-- App module (defines ecommerceApp) -->
    <script src="app.js"></script>

    <!-- Services (singletons, shared state + API calls) -->
    <script src="services/authService.js"></script>
    <script src="services/productService.js"></script>
    <script src="services/cartService.js"></script>
    <script src="services/orderService.js"></script>

    <!-- Controllers (per-view logic) -->
    <script src="controllers/authController.js"></script>
    <script src="controllers/productController.js"></script>
    <script src="controllers/cartController.js"></script>
    <script src="controllers/orderController.js"></script>

</body>
</html>
```

### Verify

Reload. You should see:
- Full dark navbar with "ShopEasy"
- "Products" link and "Login"/"Register" links (logged-out state)
- No console errors

---

## Module 5: Auth Routes + AuthController + Login & Register Views

### Concept: Controllers

A **controller** connects a view template to business logic. It receives `$scope` — the glue between controller code and the HTML template.

```javascript
.controller('AuthController', ['$scope', 'AuthService', function($scope, AuthService) {
    //                                    ↑ injected service   ↑ controller function

    $scope.loginData = {};      // ← $scope property → template accesses it as {{loginData}}
    $scope.login = function() { // ← $scope function → template calls it via ng-submit="login()"
        AuthService.login($scope.loginData)...
    };
}]);
```

**Form validation:** AngularJS automatically tracks form state using `FormController`. If you name a form `name="loginForm"`, you get:
- `loginForm.$valid` — true if all required fields are filled and pass validation
- `loginForm.$invalid` — true if any field fails
- `loginForm.$pristine` — true if user hasn't touched anything yet

### Step 5.1 — Routes are Already Added

The routes for `/login`, `/register`, and `/orders` were added in Module 3 when you rewrote `app.js`. Nothing to do here — move on.

### Step 5.2 — Write `controllers/authController.js`

Create (or overwrite) `frontend/controllers/authController.js`:

```javascript
'use strict';

angular.module('ecommerceApp')
    .controller('AuthController', ['$scope', '$rootScope', '$location', 'AuthService',
        function($scope, $rootScope, $location, AuthService) {

            // ── Redirect if already logged in ────────────────────────────
            // No point showing login form to someone already authenticated
            if (AuthService.isLoggedIn()) {
                $location.path('/products');
                return;
            }

            // ── Shared state for both login and register forms ────────────
            $scope.loading = false;   // controls spinner on submit button
            $scope.error   = null;    // shows error message from server

            // ── Login form data ───────────────────────────────────────────
            // ng-model="loginData.username" binds to this object
            $scope.loginData = {
                username: '',
                password: ''
            };

            // ── Register form data ────────────────────────────────────────
            $scope.registerData = {
                username: '',
                password: '',
                confirmPassword: '',
                firstName: '',
                lastName: '',
                mobileNumber: ''
            };

            // ═══════════════════════════════════════════════════════════════
            //  LOGIN — called by login.html: ng-submit="login()"
            // ═══════════════════════════════════════════════════════════════
            $scope.login = function() {
                // Guard: stop if form has invalid fields
                if ($scope.loginForm.$invalid) return;

                $scope.loading = true;
                $scope.error = null;

                AuthService.login($scope.loginData)
                    .then(function() {
                        // Update navbar immediately
                        $rootScope.currentUser = AuthService.getUser();
                        $rootScope.showAlert('Welcome back, ' + $rootScope.currentUser.username + '!', 'success');
                        $location.path('/products');
                    })
                    .catch(function(error) {
                        // Show the server's error message, or a fallback
                        $scope.error = (error.data && error.data.error)
                            || (error.data && error.data.message)
                            || 'Invalid username or password.';
                    })
                    .finally(function() {
                        $scope.loading = false;   // always hide spinner
                    });
            };

            // ═══════════════════════════════════════════════════════════════
            //  REGISTER — called by register.html: ng-submit="register()"
            // ═══════════════════════════════════════════════════════════════
            $scope.register = function() {
                if ($scope.registerForm.$invalid) return;

                // Custom validation: AngularJS can't compare two fields automatically
                if ($scope.registerData.password !== $scope.registerData.confirmPassword) {
                    $scope.error = 'Passwords do not match.';
                    return;
                }

                $scope.loading = true;
                $scope.error = null;

                // Build the payload matching the backend RegisterRequest record:
                // { username, password, firstName, lastName, mobileNumber }
                var payload = {
                    username:     $scope.registerData.username,
                    password:     $scope.registerData.password,
                    firstName:    $scope.registerData.firstName,
                    lastName:     $scope.registerData.lastName,
                    mobileNumber: $scope.registerData.mobileNumber
                };

                AuthService.register(payload)
                    .then(function() {
                        $rootScope.currentUser = AuthService.getUser();
                        $rootScope.showAlert('Account created! Welcome, ' + $rootScope.currentUser.username + '!', 'success');
                        $location.path('/products');
                    })
                    .catch(function(error) {
                        $scope.error = (error.data && error.data.error)
                            || (error.data && error.data.message)
                            || 'Registration failed. Please try again.';
                    })
                    .finally(function() {
                        $scope.loading = false;
                    });
            };

        }
    ]);
```

### Step 5.3 — Create `views/login.html`

Create `frontend/views/login.html`:

```html
<div class="row justify-content-center">
    <div class="col-md-5 col-lg-4">
        <div class="card shadow-sm">
            <div class="card-header bg-dark text-white text-center py-3">
                <h4 class="mb-0"><i class="bi bi-box-arrow-in-right me-2"></i>Login</h4>
            </div>
            <div class="card-body p-4">

                <!-- Error message from controller ($scope.error) -->
                <div class="alert alert-danger d-flex align-items-center" ng-show="error">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    {{error}}
                </div>

                <!--
                    name="loginForm"  → registers as $scope.loginForm (for $valid/$invalid)
                    ng-submit="login()" → calls $scope.login() when form is submitted
                    novalidate → disables browser's native HTML5 validation (let Angular handle it)
                -->
                <form name="loginForm" ng-submit="login()" novalidate>

                    <!-- Username field -->
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Username</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-person"></i></span>
                            <!--
                                ng-model="loginData.username"
                                    → two-way binding: whatever user types goes into $scope.loginData.username
                                required → AngularJS marks loginForm.$invalid if empty
                            -->
                            <input type="text"
                                   class="form-control"
                                   ng-model="loginData.username"
                                   placeholder="Enter your username"
                                   required>
                        </div>
                    </div>

                    <!-- Password field -->
                    <div class="mb-4">
                        <label class="form-label fw-semibold">Password</label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock"></i></span>
                            <input type="password"
                                   class="form-control"
                                   ng-model="loginData.password"
                                   placeholder="Enter your password"
                                   required>
                        </div>
                    </div>

                    <!-- Submit button -->
                    <button type="submit"
                            class="btn btn-dark w-100"
                            ng-disabled="loading || loginForm.$invalid">
                        <!--
                            ng-disabled: button is greyed out while loading OR form is invalid
                            ng-show: toggle between normal text and spinner text
                        -->
                        <span ng-show="!loading">
                            <i class="bi bi-box-arrow-in-right me-2"></i>Login
                        </span>
                        <span ng-show="loading">
                            <span class="spinner-border spinner-border-sm me-2"></span>Signing in...
                        </span>
                    </button>

                </form>

                <!-- Link to register page -->
                <hr>
                <p class="text-center text-muted mb-0">
                    Don't have an account?
                    <a href="#!/register" class="text-dark fw-semibold">Register here</a>
                </p>
            </div>
        </div>
    </div>
</div>
```

### Step 5.4 — Create `views/register.html`

Create `frontend/views/register.html`:

```html
<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card shadow-sm">
            <div class="card-header bg-dark text-white text-center py-3">
                <h4 class="mb-0"><i class="bi bi-person-plus me-2"></i>Create Account</h4>
            </div>
            <div class="card-body p-4">

                <!-- Error message -->
                <div class="alert alert-danger d-flex align-items-center" ng-show="error">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    {{error}}
                </div>

                <form name="registerForm" ng-submit="register()" novalidate>

                    <!-- Row: First Name + Last Name -->
                    <div class="row mb-3">
                        <div class="col-6">
                            <label class="form-label fw-semibold">First Name</label>
                            <input type="text"
                                   class="form-control"
                                   ng-model="registerData.firstName"
                                   placeholder="John">
                        </div>
                        <div class="col-6">
                            <label class="form-label fw-semibold">Last Name</label>
                            <input type="text"
                                   class="form-control"
                                   ng-model="registerData.lastName"
                                   placeholder="Doe">
                        </div>
                    </div>

                    <!-- Username -->
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Username <span class="text-danger">*</span></label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-person"></i></span>
                            <input type="text"
                                   class="form-control"
                                   ng-model="registerData.username"
                                   placeholder="Choose a username (min 3 chars)"
                                   required
                                   minlength="3">
                        </div>
                        <!-- Inline validation error — only shows after user touches the field -->
                        <small class="text-danger"
                               ng-show="registerForm.username.$dirty && registerForm.username.$error.minlength">
                            Username must be at least 3 characters.
                        </small>
                    </div>

                    <!-- Mobile Number -->
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Mobile Number <span class="text-danger">*</span></label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-phone"></i></span>
                            <input type="tel"
                                   class="form-control"
                                   ng-model="registerData.mobileNumber"
                                   placeholder="Your mobile number"
                                   required>
                        </div>
                    </div>

                    <!-- Password -->
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Password <span class="text-danger">*</span></label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock"></i></span>
                            <input type="password"
                                   class="form-control"
                                   ng-model="registerData.password"
                                   placeholder="At least 6 characters"
                                   required
                                   minlength="6">
                        </div>
                    </div>

                    <!-- Confirm Password -->
                    <div class="mb-4">
                        <label class="form-label fw-semibold">Confirm Password <span class="text-danger">*</span></label>
                        <div class="input-group">
                            <span class="input-group-text"><i class="bi bi-lock-fill"></i></span>
                            <input type="password"
                                   class="form-control"
                                   ng-model="registerData.confirmPassword"
                                   placeholder="Re-enter password"
                                   required>
                        </div>
                    </div>

                    <button type="submit"
                            class="btn btn-dark w-100"
                            ng-disabled="loading || registerForm.$invalid">
                        <span ng-show="!loading">
                            <i class="bi bi-person-check me-2"></i>Create Account
                        </span>
                        <span ng-show="loading">
                            <span class="spinner-border spinner-border-sm me-2"></span>Creating account...
                        </span>
                    </button>

                </form>

                <hr>
                <p class="text-center text-muted mb-0">
                    Already have an account?
                    <a href="#!/login" class="text-dark fw-semibold">Login here</a>
                </p>
            </div>
        </div>
    </div>
</div>
```

### Verify Module 5

1. Make sure your backend is running (user-service, api-gateway, eureka-server)
2. Navigate to `#!/register` → fill in the form → submit
3. You should be redirected to `#!/products` with a green success alert
4. The navbar should show your username and a logout option
5. Navigate to `#!/login` → try wrong password → should see error message
6. Try correct credentials → redirected to products

---

## Module 6: ProductService + ProductController + Products View

### Concept: ng-repeat and Template Expressions

`ng-repeat` is the AngularJS loop directive. For each item in an array, it creates a **new child scope** and renders the inner HTML.

```html
<div ng-repeat="product in filteredProducts">
    <!-- Each iteration gets its own 'product' variable -->
    <h5>{{product.name}}</h5>
    <p>${{product.price | number:2}}</p>
    <!-- | number:2  is a filter — formats 19.9 → 19.90 -->
</div>
```

**The per-product loading flag trick:**
When 10 products are shown, you need each "Add to Cart" button to have *its own* loading state — clicking product 3 should only spinner product 3's button.

```javascript
// Use an object (hash map) keyed by product id
$scope.addingToCart = {};    // empty object = no product is loading

// When adding product with id=5:
$scope.addingToCart[5] = true;    // only product 5 is in loading state

// In template:
// ng-disabled="addingToCart[product.id]"  ← only product 5's button is disabled
// ng-show="addingToCart[product.id]"      ← only product 5's button shows spinner
```

### Step 6.1 — Update `services/productService.js`

Rewrite `frontend/services/productService.js` to add the `getByCategory` method:

```javascript
'use strict';

angular.module('ecommerceApp')
    .service('ProductService', ['$http', function($http) {

        var API_URL = 'http://localhost:8080/api/products';

        // GET /api/products → all products
        this.getAllProducts = function() {
            return $http.get(API_URL);
        };

        // GET /api/products/{id} → single product
        this.getProductById = function(id) {
            return $http.get(API_URL + '/' + id);
        };

        // GET /api/products/search?q={query} → search by name
        this.searchProducts = function(query) {
            return $http.get(API_URL + '/search', { params: { q: query } });
        };

        // GET /api/products/category/{category} → filter by category
        this.getByCategory = function(category) {
            return $http.get(API_URL + '/category/' + category);
        };

    }]);
```

### Step 6.2 — Rewrite `controllers/productController.js`

```javascript
'use strict';

angular.module('ecommerceApp')
    .controller('ProductController', ['$scope', '$rootScope', 'ProductService', 'CartService',
        function($scope, $rootScope, ProductService, CartService) {

            // ── Initial state ─────────────────────────────────────────────
            $scope.products         = [];     // full list from API
            $scope.filteredProducts = [];     // list shown in template (may be filtered)
            $scope.categories       = ['All']; // dropdown options
            $scope.selectedCategory = 'All';
            $scope.searchQuery      = '';
            $scope.loading          = true;
            $scope.addingToCart     = {};     // { productId: true/false } map

            // ═════════════════════════════════════════════════════════════
            //  LOAD ALL PRODUCTS (runs immediately when controller loads)
            // ═════════════════════════════════════════════════════════════
            ProductService.getAllProducts()
                .then(function(response) {
                    $scope.products         = response.data;
                    $scope.filteredProducts = response.data;

                    // Extract unique categories for the filter dropdown
                    var cats = ['All'];
                    response.data.forEach(function(product) {
                        if (product.category && cats.indexOf(product.category) === -1) {
                            cats.push(product.category);
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

            // ═════════════════════════════════════════════════════════════
            //  SEARCH — calls the backend search API
            //  Called by: ng-click="search()" on the search button
            //  Also called by: ng-keyup when Enter is pressed
            // ═════════════════════════════════════════════════════════════
            $scope.search = function() {
                if (!$scope.searchQuery || $scope.searchQuery.trim() === '') {
                    // Empty query → reload all products
                    $scope.filteredProducts = $scope.products;
                    return;
                }

                $scope.loading = true;
                ProductService.searchProducts($scope.searchQuery.trim())
                    .then(function(response) {
                        $scope.filteredProducts = response.data;
                        $scope.selectedCategory = 'All';  // reset category filter
                    })
                    .catch(function() {
                        $rootScope.showAlert('Search failed.', 'danger');
                    })
                    .finally(function() {
                        $scope.loading = false;
                    });
            };

            // ═════════════════════════════════════════════════════════════
            //  CLEAR SEARCH — resets to full product list
            // ═════════════════════════════════════════════════════════════
            $scope.clearSearch = function() {
                $scope.searchQuery = '';
                $scope.filteredProducts = $scope.products;
                $scope.selectedCategory = 'All';
            };

            // ═════════════════════════════════════════════════════════════
            //  FILTER BY CATEGORY — client-side filtering (no API call)
            //  Called by: ng-change="filterByCategory(selectedCategory)"
            // ═════════════════════════════════════════════════════════════
            $scope.filterByCategory = function(category) {
                $scope.searchQuery = '';  // clear search when filtering
                if (category === 'All') {
                    $scope.filteredProducts = $scope.products;
                } else {
                    // Array.filter() creates a new array with matching items
                    $scope.filteredProducts = $scope.products.filter(function(p) {
                        return p.category === category;
                    });
                }
            };

            // ═════════════════════════════════════════════════════════════
            //  ADD TO CART
            //  Called by: ng-click="addToCart(product)" on each product card
            // ═════════════════════════════════════════════════════════════
            $scope.addToCart = function(product) {
                if (!$rootScope.isLoggedIn()) {
                    $rootScope.showAlert('Please login to add items to cart.', 'warning');
                    return;
                }

                // Set loading state for THIS product only
                $scope.addingToCart[product.id] = true;

                CartService.addToCart(product.id, 1)
                    .then(function(response) {
                        // Update cart count badge in navbar
                        $rootScope.cartCount = response.data.items
                            ? response.data.items.length
                            : 0;
                        $rootScope.showAlert(product.name + ' added to cart!', 'success');
                    })
                    .catch(function(error) {
                        var msg = (error.data && error.data.error) || 'Could not add to cart.';
                        $rootScope.showAlert(msg, 'danger');
                    })
                    .finally(function() {
                        // Clear loading state for THIS product only
                        $scope.addingToCart[product.id] = false;
                    });
            };

        }
    ]);
```

### Step 6.3 — Rewrite `views/products.html`

```html
<!-- ══════════════════════════════════════════════════════════════
     SEARCH + FILTER BAR
══════════════════════════════════════════════════════════════════ -->
<div class="row mb-4">
    <div class="col-md-6">
        <div class="input-group">
            <span class="input-group-text"><i class="bi bi-search"></i></span>
            <!--
                ng-model="searchQuery" → two-way binding to $scope.searchQuery
                ng-keyup → search() fires when user presses Enter (keyCode 13)
            -->
            <input type="text"
                   class="form-control"
                   ng-model="searchQuery"
                   placeholder="Search products..."
                   ng-keyup="$event.keyCode === 13 && search()">
            <button class="btn btn-dark" ng-click="search()">Search</button>
            <!-- Only show Clear button when there's an active search -->
            <button class="btn btn-outline-secondary" ng-show="searchQuery" ng-click="clearSearch()">
                Clear
            </button>
        </div>
    </div>

    <div class="col-md-4 mt-2 mt-md-0">
        <!--
            ng-model="selectedCategory" → bound to the selected dropdown value
            ng-change="filterByCategory(selectedCategory)" → fires on change
        -->
        <select class="form-select"
                ng-model="selectedCategory"
                ng-change="filterByCategory(selectedCategory)">
            <!--
                ng-repeat on <option> creates one option per category
                ng-value sets the value used by ng-model
            -->
            <option ng-repeat="cat in categories" ng-value="cat">{{cat}}</option>
        </select>
    </div>

    <div class="col-md-2 mt-2 mt-md-0 d-flex align-items-center">
        <small class="text-muted">
            <!-- Ternary expression in template: condition ? a : b -->
            {{filteredProducts.length}} product{{filteredProducts.length !== 1 ? 's' : ''}} found
        </small>
    </div>
</div>

<!-- ══════════════════════════════════════════════════════════════
     STATE 1: LOADING SPINNER
     ng-show: element stays in DOM but is hidden (display:none)
══════════════════════════════════════════════════════════════════ -->
<div class="text-center py-5" ng-show="loading">
    <div class="spinner-border text-dark" role="status">
        <span class="visually-hidden">Loading...</span>
    </div>
    <p class="text-muted mt-3">Loading products...</p>
</div>

<!-- ══════════════════════════════════════════════════════════════
     STATE 2: EMPTY / NO RESULTS
══════════════════════════════════════════════════════════════════ -->
<div class="text-center py-5" ng-show="!loading && filteredProducts.length === 0">
    <i class="bi bi-search fs-1 text-muted"></i>
    <h5 class="mt-3 text-muted">No products found</h5>
    <button class="btn btn-outline-dark mt-2" ng-click="clearSearch()">Show All Products</button>
</div>

<!-- ══════════════════════════════════════════════════════════════
     STATE 3: PRODUCT GRID
     ng-show: only visible when not loading AND there are results
══════════════════════════════════════════════════════════════════ -->
<div class="row row-cols-1 row-cols-md-3 row-cols-lg-4 g-4"
     ng-show="!loading && filteredProducts.length > 0">

    <!--
        ng-repeat="product in filteredProducts"
        For each product, creates a child scope with 'product' variable
        All properties of $scope are also available (addingToCart, addToCart, etc.)
    -->
    <div class="col" ng-repeat="product in filteredProducts">
        <div class="card h-100 product-card shadow-sm">

            <!-- Product image -->
            <div class="product-img-placeholder">
                <!-- ng-src: use instead of src for dynamic URLs -->
                <!-- ng-show: only render img tag when imageUrl exists -->
                <img ng-show="product.imageUrl"
                     ng-src="{{product.imageUrl}}"
                     alt="{{product.name}}">
                <!-- Placeholder icon when no image -->
                <i ng-hide="product.imageUrl" class="bi bi-box-seam fs-1 text-muted"></i>
            </div>

            <div class="card-body d-flex flex-column">
                <!-- Category badge -->
                <span class="badge bg-secondary mb-2" ng-show="product.category">
                    {{product.category}}
                </span>

                <!-- Product name -->
                <h6 class="card-title fw-semibold">{{product.name}}</h6>

                <!-- Description (truncated) -->
                <p class="card-text text-muted small flex-grow-1">
                    <!--
                        Ternary + substring for truncation:
                        If description is longer than 80 chars, show first 80 + "..."
                    -->
                    {{product.description && product.description.length > 80
                        ? product.description.substring(0, 80) + '...'
                        : product.description}}
                </p>

                <!-- Price -->
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="fw-bold fs-5 text-dark">
                        <!-- | number:2  formats: 19.9 → 19.90 -->
                        ${{product.price | number:2}}
                    </span>
                    <!-- ng-class: applies class dynamically based on expression -->
                    <span class="badge"
                          ng-class="product.stock > 0 ? 'bg-success' : 'bg-danger'">
                        {{product.stock > 0 ? 'In Stock (' + product.stock + ')' : 'Out of Stock'}}
                    </span>
                </div>

                <!-- Add to Cart button -->
                <button class="btn btn-dark w-100 mt-auto"
                        ng-click="addToCart(product)"
                        ng-disabled="product.stock === 0 || addingToCart[product.id]">
                    <!--
                        addingToCart[product.id] → looks up this product's loading state
                        product.id is used as the key in the object:
                        $scope.addingToCart = { 5: true, 12: false }
                        means product 5 is loading, product 12 is not
                    -->
                    <span ng-show="!addingToCart[product.id]">
                        <i class="bi bi-cart-plus me-2"></i>Add to Cart
                    </span>
                    <span ng-show="addingToCart[product.id]">
                        <span class="spinner-border spinner-border-sm me-2"></span>Adding...
                    </span>
                </button>

            </div>
        </div>
    </div>
</div>
```

### Verify Module 6

1. Navigate to `#!/products`
2. Products grid should load with cards
3. Type in search box, press Enter → filtered results
4. Change category dropdown → client-side filter
5. Click "Add to Cart" when logged in → spinner on that button only, then success alert, cart count badge appears
6. Click "Add to Cart" when NOT logged in → warning alert

---

## Module 7: CartService + CartController + Cart View

### Concept: PUT with Query Parameters

The cart update endpoint is: `PUT /api/cart/items/{itemId}?quantity=N`

The quantity is a **query parameter**, not a request body. In `$http`:
```javascript
$http.put(URL + '/items/' + itemId, null, { params: { quantity: quantity } })
//                                  ↑        ↑
//                                  no body  query params config
// Sends: PUT /api/cart/items/5?quantity=3
```

### Concept: Checkout Flow State Machine

The cart view has two modes controlled by `$scope.showCheckout`:

```
showCheckout = false  →  show cart table + "Proceed to Checkout" button
showCheckout = true   →  show shipping address form + "Place Order" button
```

### Step 7.1 — Create `services/cartService.js`

Create `frontend/services/cartService.js`:

```javascript
'use strict';

angular.module('ecommerceApp')
    .service('CartService', ['$http', function($http) {

        var API_URL = 'http://localhost:8080/api/cart';

        // GET /api/cart → returns CartDto { id, items: [...], totalPrice }
        // JWT is added automatically by authInterceptor
        // API Gateway extracts userId from JWT and adds X-User-Id header
        this.getCart = function() {
            return $http.get(API_URL);
        };

        // POST /api/cart/add
        // body: { productId: Long, quantity: Integer }
        this.addToCart = function(productId, quantity) {
            return $http.post(API_URL + '/add', {
                productId: productId,
                quantity:  quantity
            });
        };

        // PUT /api/cart/items/{itemId}?quantity=N
        // quantity is a query param (null body)
        this.updateItem = function(itemId, quantity) {
            return $http.put(API_URL + '/items/' + itemId, null, {
                params: { quantity: quantity }
            });
        };

        // DELETE /api/cart/items/{itemId}
        this.removeItem = function(itemId) {
            return $http.delete(API_URL + '/items/' + itemId);
        };

        // DELETE /api/cart/clear
        this.clearCart = function() {
            return $http.delete(API_URL + '/clear');
        };

    }]);
```

### Step 7.2 — Create `controllers/cartController.js`

Create `frontend/controllers/cartController.js`:

```javascript
'use strict';

angular.module('ecommerceApp')
    .controller('CartController', ['$scope', '$rootScope', '$location', 'CartService', 'OrderService',
        function($scope, $rootScope, $location, CartService, OrderService) {

            // ── State ─────────────────────────────────────────────────────
            $scope.cart            = null;     // CartDto from API
            $scope.loading         = true;
            $scope.showCheckout    = false;    // toggles checkout form
            $scope.shippingAddress = '';
            $scope.placingOrder    = false;

            // ═════════════════════════════════════════════════════════════
            //  LOAD CART
            // ═════════════════════════════════════════════════════════════
            function loadCart() {
                $scope.loading = true;
                CartService.getCart()
                    .then(function(response) {
                        $scope.cart = response.data;
                        // Keep navbar badge in sync
                        $rootScope.cartCount = $scope.cart.items
                            ? $scope.cart.items.length
                            : 0;
                    })
                    .catch(function() {
                        $rootScope.showAlert('Could not load cart.', 'danger');
                    })
                    .finally(function() {
                        $scope.loading = false;
                    });
            }

            // Load immediately when controller initializes
            loadCart();

            // ═════════════════════════════════════════════════════════════
            //  UPDATE QUANTITY
            //  Called by: ng-click on + / - buttons and ng-change on input
            // ═════════════════════════════════════════════════════════════
            $scope.updateQuantity = function(item) {
                // Prevent quantity going below 1
                if (item.quantity < 1) {
                    item.quantity = 1;
                    return;
                }

                CartService.updateItem(item.id, item.quantity)
                    .then(function(response) {
                        $scope.cart = response.data;
                        $rootScope.cartCount = $scope.cart.items.length;
                    })
                    .catch(function() {
                        $rootScope.showAlert('Could not update quantity.', 'danger');
                        loadCart();  // re-load to reset to actual server state
                    });
            };

            // ═════════════════════════════════════════════════════════════
            //  REMOVE ITEM
            // ═════════════════════════════════════════════════════════════
            $scope.removeItem = function(item) {
                CartService.removeItem(item.id)
                    .then(function(response) {
                        $scope.cart = response.data;
                        $rootScope.cartCount = $scope.cart.items.length;
                        $rootScope.showAlert(item.productName + ' removed from cart.', 'info');
                    })
                    .catch(function() {
                        $rootScope.showAlert('Could not remove item.', 'danger');
                    });
            };

            // ═════════════════════════════════════════════════════════════
            //  CHECKOUT TOGGLE
            // ═════════════════════════════════════════════════════════════
            $scope.proceedToCheckout = function() {
                $scope.showCheckout = true;  // show shipping address form
            };

            $scope.cancelCheckout = function() {
                $scope.showCheckout    = false;
                $scope.shippingAddress = '';
            };

            // ═════════════════════════════════════════════════════════════
            //  PLACE ORDER
            //  POST /api/orders with { shippingAddress }
            // ═════════════════════════════════════════════════════════════
            $scope.placeOrder = function() {
                if (!$scope.shippingAddress || $scope.shippingAddress.trim() === '') {
                    $rootScope.showAlert('Please enter a shipping address.', 'warning');
                    return;
                }

                $scope.placingOrder = true;

                OrderService.placeOrder($scope.shippingAddress.trim())
                    .then(function() {
                        // Order placed → cart is now empty
                        $rootScope.cartCount = 0;
                        $rootScope.showAlert('Order placed successfully!', 'success');
                        $location.path('/orders');    // navigate to order history
                    })
                    .catch(function(error) {
                        var msg = (error.data && error.data.error) || 'Could not place order.';
                        $rootScope.showAlert(msg, 'danger');
                    })
                    .finally(function() {
                        $scope.placingOrder = false;
                    });
            };

        }
    ]);
```

### Step 7.3 — Create `views/cart.html`

Create `frontend/views/cart.html`:

```html
<h3 class="mb-4"><i class="bi bi-cart3 me-2"></i>My Cart</h3>

<!-- ══════════════════════════════════════════════════════════════
     STATE 1: LOADING
══════════════════════════════════════════════════════════════════ -->
<div class="text-center py-5" ng-show="loading">
    <div class="spinner-border text-dark"></div>
    <p class="text-muted mt-3">Loading cart...</p>
</div>

<!-- ══════════════════════════════════════════════════════════════
     STATE 2: EMPTY CART
══════════════════════════════════════════════════════════════════ -->
<div class="text-center py-5"
     ng-show="!loading && (!cart || !cart.items || cart.items.length === 0)">
    <i class="bi bi-cart-x fs-1 text-muted"></i>
    <h5 class="mt-3 text-muted">Your cart is empty</h5>
    <a href="#!/products" class="btn btn-dark mt-2">
        <i class="bi bi-grid me-2"></i>Browse Products
    </a>
</div>

<!-- ══════════════════════════════════════════════════════════════
     STATE 3: CART WITH ITEMS
══════════════════════════════════════════════════════════════════ -->
<div ng-show="!loading && cart && cart.items && cart.items.length > 0">

    <div class="row">

        <!-- ── Left: Cart Items Table ─────────────────────────────── -->
        <div class="col-lg-8">
            <div class="card shadow-sm">
                <div class="card-header bg-dark text-white">
                    <strong>Cart Items ({{cart.items.length}})</strong>
                </div>
                <div class="card-body p-0">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>#</th>
                                <th>Product</th>
                                <th>Price</th>
                                <th>Quantity</th>
                                <th>Subtotal</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <!--
                                ng-repeat with $index: gives 0-based index
                                $index + 1 gives 1-based row number
                            -->
                            <tr ng-repeat="item in cart.items">
                                <td class="text-muted">{{$index + 1}}</td>
                                <td class="fw-semibold">{{item.productName}}</td>
                                <td>${{item.price | number:2}}</td>
                                <td style="width: 160px">
                                    <div class="input-group input-group-sm">
                                        <!--
                                            Two expressions in one ng-click (separated by ;):
                                            1. Decrement item.quantity
                                            2. Call updateQuantity(item) to sync with server
                                        -->
                                        <button class="btn btn-outline-secondary"
                                                ng-click="item.quantity = item.quantity - 1; updateQuantity(item)"
                                                ng-disabled="item.quantity <= 1">-</button>
                                        <!--
                                            ng-model="item.quantity" → two-way binding
                                            Typing a number directly in the input also updates item.quantity
                                        -->
                                        <input type="number"
                                               class="form-control text-center"
                                               ng-model="item.quantity"
                                               ng-change="updateQuantity(item)"
                                               min="1">
                                        <button class="btn btn-outline-secondary"
                                                ng-click="item.quantity = item.quantity + 1; updateQuantity(item)">+</button>
                                    </div>
                                </td>
                                <td class="fw-semibold">
                                    ${{(item.price * item.quantity) | number:2}}
                                </td>
                                <td>
                                    <button class="btn btn-sm btn-outline-danger"
                                            ng-click="removeItem(item)"
                                            title="Remove item">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- ── Right: Order Summary ───────────────────────────────── -->
        <div class="col-lg-4 mt-3 mt-lg-0">
            <div class="card shadow-sm">
                <div class="card-header bg-dark text-white">
                    <strong>Order Summary</strong>
                </div>
                <div class="card-body">
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Items</span>
                        <span>{{cart.items.length}}</span>
                    </div>
                    <hr>
                    <div class="d-flex justify-content-between mb-3">
                        <strong>Total</strong>
                        <strong class="fs-5">${{cart.totalPrice | number:2}}</strong>
                    </div>

                    <!-- CHECKOUT SECTION — toggled by showCheckout flag -->

                    <!-- Button visible when NOT in checkout mode -->
                    <button class="btn btn-dark w-100"
                            ng-show="!showCheckout"
                            ng-click="proceedToCheckout()">
                        <i class="bi bi-credit-card me-2"></i>Proceed to Checkout
                    </button>

                    <!-- Shipping address form — visible when in checkout mode -->
                    <div ng-show="showCheckout">
                        <label class="form-label fw-semibold">Shipping Address</label>
                        <!--
                            ng-model="shippingAddress" → binds textarea to $scope.shippingAddress
                        -->
                        <textarea class="form-control mb-3"
                                  rows="3"
                                  ng-model="shippingAddress"
                                  placeholder="Enter your full delivery address..."></textarea>

                        <button class="btn btn-success w-100 mb-2"
                                ng-click="placeOrder()"
                                ng-disabled="placingOrder || !shippingAddress">
                            <span ng-show="!placingOrder">
                                <i class="bi bi-check-circle me-2"></i>Place Order
                            </span>
                            <span ng-show="placingOrder">
                                <span class="spinner-border spinner-border-sm me-2"></span>
                                Placing Order...
                            </span>
                        </button>

                        <button class="btn btn-outline-secondary w-100"
                                ng-click="cancelCheckout()">
                            Cancel
                        </button>
                    </div>
                </div>
            </div>

            <!-- Continue shopping link -->
            <div class="mt-3 text-center">
                <a href="#!/products" class="text-muted small">
                    <i class="bi bi-arrow-left me-1"></i>Continue Shopping
                </a>
            </div>
        </div>

    </div>
</div>
```

### Verify Module 7

1. Add items from products page
2. Navigate to `#!/cart` — items should appear
3. Change quantity using +/- buttons — total updates
4. Click "Proceed to Checkout" → address form appears
5. Enter address → "Place Order" → redirected to `#!/orders` with success alert
6. Cart count in navbar resets to 0

---

## Module 8: OrderService + OrderController + Orders View

### Concept: date Filter + ng-class with a Function

**date filter:** Formats an ISO date string from the backend:
```html
<!-- 2026-03-19T10:30:00 → "Mar 19, 2026" -->
{{order.createdAt | date:'mediumDate'}}

<!-- → "Mar 19, 2026, 10:30:00 AM" -->
{{order.createdAt | date:'medium'}}
```

**ng-class with a function:** When the class logic is complex, define it in the controller:
```javascript
// Controller
$scope.getStatusClass = function(status) {
    var map = { 'PENDING': 'warning', 'CONFIRMED': 'info', 'DELIVERED': 'success' };
    return 'badge bg-' + (map[status] || 'secondary');
};
```
```html
<!-- Template — ng-class evaluates the function and sets the returned class -->
<span ng-class="getStatusClass(order.status)">{{order.status}}</span>
```

### Concept: Detail Panel Toggle

Instead of routing to a new page, clicking an order opens a **detail panel overlay**:
```javascript
$scope.selectedOrder = null;  // no panel shown

$scope.viewOrder = function(order) {
    $scope.selectedOrder = order;  // sets panel content + shows it
};

$scope.closeOrder = function() {
    $scope.selectedOrder = null;   // hides panel
};
```
```html
<!-- Panel only visible when selectedOrder is not null -->
<div class="order-detail-panel" ng-show="selectedOrder">
    <h5>Order #{{selectedOrder.id}}</h5>
```

### Step 8.1 — Create `services/orderService.js`

Create `frontend/services/orderService.js`:

```javascript
'use strict';

angular.module('ecommerceApp')
    .service('OrderService', ['$http', function($http) {

        var API_URL = 'http://localhost:8080/api/orders';

        // POST /api/orders
        // body: { shippingAddress: "..." }
        // Returns: OrderDto with 201 Created
        this.placeOrder = function(shippingAddress) {
            return $http.post(API_URL, { shippingAddress: shippingAddress });
        };

        // GET /api/orders → list of OrderDto for the current user
        this.getMyOrders = function() {
            return $http.get(API_URL);
        };

        // GET /api/orders/{orderId} → single order detail
        this.getOrderById = function(orderId) {
            return $http.get(API_URL + '/' + orderId);
        };

    }]);
```

### Step 8.2 — Create `controllers/orderController.js`

Create `frontend/controllers/orderController.js`:

```javascript
'use strict';

angular.module('ecommerceApp')
    .controller('OrderController', ['$scope', '$rootScope', 'OrderService',
        function($scope, $rootScope, OrderService) {

            // ── State ─────────────────────────────────────────────────────
            $scope.orders        = [];
            $scope.loading       = true;
            $scope.selectedOrder = null;    // the order shown in the detail panel
            $scope.searchText    = '';      // for the order search filter

            // ═════════════════════════════════════════════════════════════
            //  LOAD ORDERS
            // ═════════════════════════════════════════════════════════════
            OrderService.getMyOrders()
                .then(function(response) {
                    $scope.orders = response.data;
                })
                .catch(function() {
                    $rootScope.showAlert('Could not load orders.', 'danger');
                })
                .finally(function() {
                    $scope.loading = false;
                });

            // ═════════════════════════════════════════════════════════════
            //  STATUS BADGE — maps status string to Bootstrap color class
            //  Used in template: ng-class="getStatusClass(order.status)"
            // ═════════════════════════════════════════════════════════════
            $scope.getStatusClass = function(status) {
                var colorMap = {
                    'PENDING':   'bg-warning text-dark',
                    'CONFIRMED': 'bg-info text-dark',
                    'SHIPPED':   'bg-primary',
                    'DELIVERED': 'bg-success',
                    'CANCELLED': 'bg-danger'
                };
                return 'badge ' + (colorMap[status] || 'bg-secondary');
            };

            // ═════════════════════════════════════════════════════════════
            //  DETAIL PANEL — show/hide order details overlay
            // ═════════════════════════════════════════════════════════════
            $scope.viewOrder = function(order) {
                $scope.selectedOrder = order;
            };

            $scope.closeOrder = function() {
                $scope.selectedOrder = null;
            };

            // ═════════════════════════════════════════════════════════════
            //  CALCULATE ORDER TOTAL (if not provided by backend)
            // ═════════════════════════════════════════════════════════════
            $scope.getOrderTotal = function(order) {
                if (order.totalAmount) return order.totalAmount;
                if (!order.items) return 0;
                return order.items.reduce(function(sum, item) {
                    return sum + (item.price * item.quantity);
                }, 0);
            };

        }
    ]);
```

### Step 8.3 — Create `views/orders.html`

Create `frontend/views/orders.html`:

```html
<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-receipt me-2"></i>My Orders</h3>
    <!-- Search orders in real-time using AngularJS's built-in filter -->
    <div class="input-group" style="max-width: 300px">
        <span class="input-group-text"><i class="bi bi-search"></i></span>
        <!--
            ng-model="searchText" → bound to $scope.searchText
            Used below as: orders | filter:searchText
        -->
        <input type="text"
               class="form-control"
               ng-model="searchText"
               placeholder="Search orders...">
    </div>
</div>

<!-- ══════════════════════════════════════════════════════════════
     LOADING SPINNER
══════════════════════════════════════════════════════════════════ -->
<div class="text-center py-5" ng-show="loading">
    <div class="spinner-border text-dark"></div>
    <p class="text-muted mt-3">Loading orders...</p>
</div>

<!-- ══════════════════════════════════════════════════════════════
     NO ORDERS
══════════════════════════════════════════════════════════════════ -->
<div class="text-center py-5" ng-show="!loading && orders.length === 0">
    <i class="bi bi-receipt fs-1 text-muted"></i>
    <h5 class="mt-3 text-muted">No orders yet</h5>
    <a href="#!/products" class="btn btn-dark mt-2">Start Shopping</a>
</div>

<!-- ══════════════════════════════════════════════════════════════
     ORDERS TABLE
══════════════════════════════════════════════════════════════════ -->
<div ng-show="!loading && orders.length > 0">
    <div class="card shadow-sm">
        <div class="card-body p-0">
            <table class="table table-hover mb-0">
                <thead class="table-dark">
                    <tr>
                        <th>Order ID</th>
                        <th>Date</th>
                        <th>Items</th>
                        <th>Total</th>
                        <th>Status</th>
                        <th>Shipping Address</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <!--
                        | filter:searchText  → AngularJS built-in filter
                        Searches ALL properties of each order for the searchText string
                        Updates in real-time as user types (two-way binding via ng-model)

                        | orderBy:'-id'  → sort descending by id (newest first)
                        The '-' prefix means descending order
                    -->
                    <tr ng-repeat="order in orders | filter:searchText | orderBy:'-id'">
                        <td class="fw-semibold">#{{order.id}}</td>
                        <td>
                            <!--
                                | date:'mediumDate'  → formats ISO string to "Mar 19, 2026"
                            -->
                            {{order.createdAt | date:'mediumDate'}}
                        </td>
                        <td>
                            <span class="badge bg-secondary">
                                {{order.items ? order.items.length : 0}} items
                            </span>
                        </td>
                        <td class="fw-semibold">${{getOrderTotal(order) | number:2}}</td>
                        <td>
                            <!--
                                ng-class="getStatusClass(order.status)"
                                → calls the controller function, returns a CSS class string
                                → e.g., "badge bg-success" for DELIVERED
                            -->
                            <span ng-class="getStatusClass(order.status)">
                                {{order.status}}
                            </span>
                        </td>
                        <td class="text-muted small">
                            {{order.shippingAddress}}
                        </td>
                        <td>
                            <button class="btn btn-sm btn-outline-dark"
                                    ng-click="viewOrder(order)">
                                <i class="bi bi-eye me-1"></i>Details
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- ══════════════════════════════════════════════════════════════
     ORDER DETAIL PANEL + BACKDROP
     Fixed position overlay — shows when selectedOrder is not null
══════════════════════════════════════════════════════════════════ -->

<!-- Semi-transparent backdrop — click to close -->
<div class="modal-backdrop-light"
     ng-show="selectedOrder"
     ng-click="closeOrder()">
</div>

<!-- The panel itself (styled in css/style.css as .order-detail-panel) -->
<div class="order-detail-panel card shadow-lg" ng-show="selectedOrder">
    <div class="card-header bg-dark text-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0">
            <i class="bi bi-receipt me-2"></i>
            Order #{{selectedOrder.id}} Details
        </h5>
        <!-- Close button -->
        <button class="btn-close btn-close-white" ng-click="closeOrder()"></button>
    </div>
    <div class="card-body">

        <!-- Order meta info -->
        <div class="row mb-3">
            <div class="col-6">
                <small class="text-muted d-block">Order Date</small>
                <strong>{{selectedOrder.createdAt | date:'medium'}}</strong>
            </div>
            <div class="col-6">
                <small class="text-muted d-block">Status</small>
                <span ng-class="getStatusClass(selectedOrder.status)">
                    {{selectedOrder.status}}
                </span>
            </div>
        </div>

        <div class="mb-3">
            <small class="text-muted d-block">Shipping Address</small>
            <strong>{{selectedOrder.shippingAddress}}</strong>
        </div>

        <!-- Items table -->
        <h6 class="fw-semibold mb-2">Items</h6>
        <table class="table table-sm">
            <thead class="table-light">
                <tr>
                    <th>Product</th>
                    <th>Qty</th>
                    <th>Price</th>
                    <th>Subtotal</th>
                </tr>
            </thead>
            <tbody>
                <tr ng-repeat="item in selectedOrder.items">
                    <td>{{item.productName}}</td>
                    <td>{{item.quantity}}</td>
                    <td>${{item.price | number:2}}</td>
                    <td>${{(item.price * item.quantity) | number:2}}</td>
                </tr>
            </tbody>
            <tfoot>
                <tr class="table-dark">
                    <td colspan="3" class="text-end fw-bold">Total</td>
                    <td class="fw-bold">${{getOrderTotal(selectedOrder) | number:2}}</td>
                </tr>
            </tfoot>
        </table>
    </div>
</div>
```

### Verify Module 8

1. After placing an order, you're redirected to `#!/orders`
2. The order appears in the table with its status badge
3. Type in the search box → orders filter in real-time
4. Click "Details" → detail panel slides in over the page
5. Click the backdrop or × to close

---

## Module 9: Final Wiring — Complete App

At this point all the files exist. This module ensures everything is wired together correctly.

### Step 9.1 — Verify Script Load Order in `index.html`

Open `index.html` and confirm the scripts section at the bottom looks exactly like this:

```html
<!-- Framework (Angular must be first) -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.8.3/angular.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/angular-route/1.8.3/angular-route.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

<!-- App module (MUST be before services and controllers) -->
<script src="app.js"></script>

<!-- Services (ALL services before ALL controllers) -->
<script src="services/authService.js"></script>
<script src="services/productService.js"></script>
<script src="services/cartService.js"></script>
<script src="services/orderService.js"></script>

<!-- Controllers (last — they depend on services) -->
<script src="controllers/authController.js"></script>
<script src="controllers/productController.js"></script>
<script src="controllers/cartController.js"></script>
<script src="controllers/orderController.js"></script>
```

### Step 9.2 — Verify app.js Has All Routes

Open `app.js` and confirm `$routeProvider` has these routes:

```javascript
.when('/', { redirectTo: '/products' })
.when('/login',    { templateUrl: 'views/login.html',    controller: 'AuthController' })
.when('/register', { templateUrl: 'views/register.html', controller: 'AuthController' })
.when('/products', { templateUrl: 'views/products.html', controller: 'ProductController' })
.when('/cart',     { templateUrl: 'views/cart.html',     controller: 'CartController',  resolve: { auth: ... } })
.when('/orders',   { templateUrl: 'views/orders.html',   controller: 'OrderController', resolve: { auth: ... } })
.otherwise({ redirectTo: '/products' })
```

### Step 9.3 — Verify All Files Exist

Your `frontend/` folder should now look like:

```
frontend/
├── index.html                   ← full navbar + global alert + all scripts
├── app.js                       ← module + all routes + authInterceptor + run()
├── css/
│   └── style.css                ← already complete (do not modify)
├── services/
│   ├── authService.js           ← login, register, token management
│   ├── productService.js        ← product API calls
│   ├── cartService.js           ← cart CRUD
│   └── orderService.js          ← place order + order history
├── controllers/
│   ├── authController.js        ← login + register logic
│   ├── productController.js     ← product list, search, filter, add to cart
│   ├── cartController.js        ← cart display, quantity update, checkout
│   └── orderController.js       ← order history, status badge, detail panel
└── views/
    ├── login.html               ← login form
    ├── register.html            ← registration form
    ├── products.html            ← product grid
    ├── cart.html                ← cart table + checkout
    └── orders.html              ← order history + detail panel
```

### Step 9.4 — Full End-to-End Test

Run through this sequence to verify the complete app:

**Test 1: Registration**
1. Open `index.html` → navbar shows Login / Register
2. Click Register → fill in form → submit
3. ✅ Redirected to /products, navbar shows username, green alert

**Test 2: Products**
4. Products grid loads with Bootstrap cards
5. ✅ Search works (type + Enter)
6. ✅ Category dropdown filters products
7. ✅ Add to Cart (logged in) → badge count increases, green alert

**Test 3: Cart**
8. Click Cart icon in navbar → `#!/cart` loads
9. ✅ Item appears in table
10. ✅ Click + → quantity increases, subtotal updates
11. ✅ Click trash icon → item removed
12. ✅ "Proceed to Checkout" → address form appears
13. ✅ Enter address → "Place Order" → redirected to orders

**Test 4: Orders**
14. ✅ Order appears in table with PENDING status badge
15. ✅ Type in search box → filters in real-time
16. ✅ Click "Details" → detail panel appears
17. ✅ Click backdrop → panel closes

**Test 5: Logout**
18. Click username dropdown → Logout
19. ✅ Redirected to /login, navbar shows Login / Register
20. ✅ Navigating to /cart or /orders redirects to /login (auth guard)

**Test 6: Token Persistence**
21. Login → close browser tab → reopen the app
22. ✅ Still logged in (token in localStorage)

---

## Module 10: Debugging & Common Mistakes

### How to Read AngularJS Errors

Open DevTools Console (F12). Common errors and their fixes:

**`Module 'ecommerceApp' is not available`**
→ `angular.min.js` loaded but `app.js` failed to load or has a syntax error.
→ Check: script tag path is correct; no typos in `app.js`

**`Unknown provider: AuthServiceProvider`**
→ A controller declares `AuthService` but the service file wasn't loaded.
→ Fix: add `<script src="services/authService.js">` to index.html BEFORE the controller script

**`Cannot read properties of undefined (reading 'isLoggedIn')`**
→ Controller loaded before the service it depends on.
→ Fix: services before controllers in index.html script load order

**`$http is not defined`** or circular dependency errors
→ If injecting `AuthService` directly into `authInterceptor`, you'll get a circular dependency.
→ Fix: use `$injector.get('AuthService')` inside the function body (as shown in app.js)

**Request hangs / never resolves**
→ You forgot `return config;` in the interceptor's `request` function.
→ The interceptor receives the config but if you don't return it, $http has no config to proceed with.

**Cart/Order shows 401 Unauthorized**
→ Token is missing or expired.
→ Check localStorage in DevTools → Application → Local Storage: `token` key should exist.
→ Verify `authInterceptor` is registered: `$httpProvider.interceptors.push('authInterceptor')`

**ng-repeat shows `[object Object]` instead of values**
→ You're binding to the object itself instead of its property: `{{product}}` vs `{{product.name}}`

**Alert doesn't disappear after 4 seconds**
→ `setTimeout` used without `$apply`. AngularJS doesn't detect changes made outside its digest cycle.
→ Fix: wrap the change in `$rootScope.$apply(function() { ... })`

### Inspect $scope in Browser Console

```javascript
// Click any DOM element in Elements tab, then in Console:
var scope = angular.element($0).scope();
scope.products        // → see all products in current scope
scope.loading         // → see loading state
scope.$parent         // → parent scope

// Access $rootScope:
var root = angular.element(document.body).scope().$root;
root.cartCount        // → current cart badge count
root.currentUser      // → { username: "...", role: "..." }
root.isLoggedIn()     // → true/false
```

### Inspect $http calls in Network Tab

1. Open DevTools → Network tab
2. Filter by "Fetch/XHR"
3. Every `$http` call appears here
4. Click a request → Headers → verify `Authorization: Bearer eyJ...` is present
5. Click Response → see the actual JSON returned

### Key Rules to Remember

| Rule | Why |
|------|-----|
| Always `return config` in interceptor | $http needs the config to proceed |
| Always `return $q.reject(rejection)` in responseError | Propagates error to `.catch()` |
| Use `$injector.get()` in authInterceptor | Breaks circular dependency |
| Services before controllers in `<script>` tags | Controllers depend on services |
| `angular.module('name')` with no 2nd arg = GET | With 2nd arg = CREATE (destroys existing!) |
| Use `ng-src` for dynamic image URLs | `src="{{url}}"` fires before Angular processes it |
| Wrap `setTimeout` changes in `$apply` | `setTimeout` is outside Angular's digest cycle |
| Use nested objects for `ng-model` (`loginData.username`) | Prevents scope inheritance bugs |

---

## Quick Reference: What Each File Does

| File | Purpose | Key Concepts |
|------|---------|--------------|
| `index.html` | Shell page loaded once | `ng-app`, `ng-view`, `$rootScope` in navbar |
| `app.js` | Module + routes + interceptor + run() | `.config()`, `.factory()`, `.run()`, `$routeProvider` |
| `authService.js` | JWT token lifecycle | localStorage, `.service()`, promise chaining |
| `productService.js` | Product API wrapper | `$http.get()`, query params |
| `cartService.js` | Cart CRUD | `$http.put()` with `params:`, `$http.delete()` |
| `orderService.js` | Order placement + history | `$http.post()`, `$http.get()` |
| `authController.js` | Login + register form logic | form `$valid`, `$location.path()`, `.catch()` |
| `productController.js` | Product list + search + cart | `ng-repeat` state, per-product loading object |
| `cartController.js` | Cart display + checkout | state machine, `loadCart()` reload pattern |
| `orderController.js` | Order history + detail panel | `ng-class` function, filter, `selectedOrder` toggle |
| `login.html` | Login form | `ng-submit`, `ng-model`, `ng-disabled` |
| `register.html` | Registration form | `$dirty`, `$error.minlength`, custom validation |
| `products.html` | Product grid | `ng-repeat`, `| number:2`, `addingToCart[id]` |
| `cart.html` | Cart table + checkout | two-expr `ng-click`, `ng-model` quantity |
| `orders.html` | Order list + detail panel | `| filter:`, `| orderBy:`, `ng-class`, overlay panel |

---

*Implementation guide for ShopEasy — AngularJS 1.8.3 + Spring Boot Microservices*
*Backend port: 8080 (API Gateway) · Eureka: 8761 · User Service: 8081 · Product Service: 8082 · Order Service: 8083*
