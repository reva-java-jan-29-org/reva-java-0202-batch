# Week 6 — Frontend Interview Questions & Study Notes

> 3 questions per topic, formatted as Q&A study notes for learning and revision.

---

## TABLE OF CONTENTS

- [HTML Orientation](#html-orientation)
- [HTML Foundation](#html-foundation)
- [HTML Forms](#html-forms)
- [CSS Foundation](#css-foundation)
- [CSS3 Features](#css3-features)
- [CSS Depth](#css-depth)
- [JavaScript Introduction](#javascript-introduction)
- [JavaScript Advanced](#javascript-advanced)
- [JavaScript DOM](#javascript-dom)
- [JavaScript HTTP](#javascript-http)
- [JavaScript Testing](#javascript-testing)

---

# HTML ORIENTATION

## 1. Introduction to HTTP

**Q1. What is HTTP and how does it work?**
> HTTP (HyperText Transfer Protocol) is the foundation of data communication on the web. It is a stateless, request-response protocol where a client (browser) sends a request to a server, and the server returns a response. Each HTTP transaction is independent — the server doesn't remember previous requests unless sessions/cookies are used.
>
> **Flow:** Browser → DNS Lookup → TCP Connection → HTTP Request → Server processes → HTTP Response → Browser renders.

**Q2. What does "stateless" mean in the context of HTTP?**
> Stateless means each HTTP request is completely independent. The server does not retain any information about previous requests from the same client. To maintain state (e.g., login sessions), mechanisms like cookies, sessions, or tokens (JWT) are used on top of HTTP.

**Q3. What is the difference between HTTP and HTTPS?**
> HTTP transmits data in plain text, making it vulnerable to interception. HTTPS (HTTP Secure) wraps HTTP inside TLS/SSL encryption, ensuring data confidentiality and integrity. HTTPS also authenticates the server using digital certificates, protecting against man-in-the-middle attacks. All modern websites should use HTTPS.

---

## 2. HTTP Methods

**Q1. What are the main HTTP methods and when do you use each?**
> - **GET** — Retrieve data. No body. Safe and idempotent.
> - **POST** — Send data to create a resource. Has a request body.
> - **PUT** — Replace an entire resource. Idempotent.
> - **PATCH** — Partially update a resource.
> - **DELETE** — Remove a resource.
> - **HEAD** — Like GET but returns only headers, no body.
> - **OPTIONS** — Returns allowed methods for a resource (used in CORS preflight).

**Q2. What is the difference between PUT and PATCH?**
> **PUT** replaces the entire resource — if you omit fields, they get removed or reset. **PATCH** applies a partial update — only the fields you send are changed. Use PATCH when you want to update one or two fields without resending the whole object.

**Q3. What does "idempotent" mean? Which HTTP methods are idempotent?**
> An operation is idempotent if calling it multiple times produces the same result as calling it once. GET, PUT, DELETE, and HEAD are idempotent. POST is NOT idempotent — calling POST twice may create two resources.

---

## 3. HTTP Status Codes

**Q1. What are HTTP status code categories?**
> - **1xx** — Informational (e.g., 100 Continue)
> - **2xx** — Success (200 OK, 201 Created, 204 No Content)
> - **3xx** — Redirection (301 Moved Permanently, 302 Found, 304 Not Modified)
> - **4xx** — Client Errors (400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found)
> - **5xx** — Server Errors (500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable)

**Q2. What is the difference between 401 and 403?**
> **401 Unauthorized** means the client is not authenticated — it needs to log in. **403 Forbidden** means the client IS authenticated but doesn't have permission to access the resource. Think: 401 = "Who are you?", 403 = "I know who you are, but you can't come in."

**Q3. When would you use 201 vs 200?**
> **200 OK** is returned for a successful GET, PUT, or PATCH request. **201 Created** is returned specifically when a POST request successfully creates a new resource. The response for 201 often includes a `Location` header pointing to the new resource's URL.

---

# HTML FOUNDATION

## 4. Overview of HTML

**Q1. What is HTML and what role does it play in a web page?**
> HTML (HyperText Markup Language) is the standard language for creating web pages. It defines the **structure and content** of a page using a series of elements represented by tags. HTML provides the skeleton; CSS handles styling and JavaScript handles behavior.

**Q2. What is the difference between HTML elements and HTML tags?**
> A **tag** is the actual markup syntax (e.g., `<p>`, `</p>`). An **element** is the complete unit — the opening tag + content + closing tag (e.g., `<p>Hello</p>`). Some elements are self-closing/void and have no closing tag (e.g., `<img>`, `<br>`, `<input>`).

**Q3. What does the `<!DOCTYPE html>` declaration do?**
> It tells the browser which version of HTML the document uses. `<!DOCTYPE html>` triggers **standards mode** (HTML5), ensuring consistent rendering across browsers. Without it, browsers may fall into **quirks mode**, where they apply legacy, inconsistent rendering rules.

---

## 5. HTML Tags

**Q1. What is the difference between block-level and inline tags?**
> **Block-level tags** (e.g., `<div>`, `<p>`, `<h1>`) start on a new line and take full width. **Inline tags** (e.g., `<span>`, `<a>`, `<strong>`) flow within text and only take as much width as needed. Block elements can contain inline elements, but inline elements should not contain block elements.

**Q2. What are semantic HTML tags? Give examples.**
> Semantic tags clearly describe their meaning and purpose. Examples: `<header>`, `<nav>`, `<main>`, `<article>`, `<section>`, `<aside>`, `<footer>`. They improve accessibility, SEO, and code readability compared to generic `<div>` wrappers.

**Q3. What is the purpose of the `<head>` tag?**
> The `<head>` element contains metadata about the document — information not directly visible to the user. It includes: `<title>` (page title shown in browser tab), `<meta>` (charset, viewport, description), `<link>` (stylesheets), and `<script>` (JavaScript). It does NOT render visible content.

---

## 6. HTML Document Structure and DOM

**Q1. What is the basic structure of an HTML document?**
> ```html
> <!DOCTYPE html>
> <html lang="en">
>   <head>
>     <meta charset="UTF-8">
>     <title>Page Title</title>
>   </head>
>   <body>
>     <!-- visible content -->
>   </body>
> </html>
> ```
> The `<html>` element is the root. `<head>` holds metadata. `<body>` contains all visible page content.

**Q2. What is the DOM?**
> The DOM (Document Object Model) is a tree-like programming interface that represents the HTML document as nodes and objects. When the browser parses HTML, it builds the DOM tree. JavaScript can read and manipulate the DOM to dynamically change page content, structure, and styling.

**Q3. What is the difference between the HTML source and the DOM?**
> The HTML source is the raw text file you write. The DOM is the live, in-memory representation created by the browser after parsing that source. JavaScript and browser extensions can modify the DOM after page load, so the DOM may differ from the original HTML source.

---

## 7. Elements and Attributes

**Q1. What is an HTML attribute? Give examples.**
> Attributes provide additional information about an element. They are written inside the opening tag as `name="value"` pairs. Examples: `<a href="https://example.com">`, `<img src="photo.jpg" alt="A photo">`, `<input type="text" placeholder="Enter name">`.

**Q2. What is the difference between `id` and `class` attributes?**
> `id` is unique — only one element per page should have a given id. It's used for specific targeting with `#id` in CSS or `getElementById` in JS. `class` can be shared across multiple elements and allows grouping for styling (`.class`) or behavior.

**Q3. What are global attributes in HTML?**
> Global attributes can be applied to any HTML element. Common ones include: `id`, `class`, `style`, `title` (tooltip text), `hidden` (hides element), `data-*` (custom data attributes), `tabindex` (keyboard navigation order), and `lang` (language of the element's content).

---

## 8. Inline and Block Elements

**Q1. List 5 block elements and 5 inline elements.**
> **Block:** `<div>`, `<p>`, `<h1>`–`<h6>`, `<ul>`, `<section>`
> **Inline:** `<span>`, `<a>`, `<img>`, `<strong>`, `<em>`
>
> Block elements stack vertically; inline elements flow horizontally within text.

**Q2. Can you place a block element inside an inline element?**
> Generally no. Placing a `<div>` inside a `<span>` is invalid HTML. However, in HTML5, `<a>` tags can wrap block-level elements (making a clickable block), which is an exception to the older rule.

**Q3. How does `display: inline-block` differ from `inline` and `block`?**
> `inline` — flows with text, cannot set width/height.
> `block` — starts on new line, full width, can set width/height.
> `inline-block` — flows with text like inline BUT respects width, height, padding, and margin like block. Useful for buttons and nav items.

---

## 9. Common Tags

**Q1. What is the difference between `<strong>` and `<b>`, and between `<em>` and `<i>`?**
> `<strong>` conveys semantic importance (screen readers emphasize it); `<b>` is purely visual bold with no semantic meaning. Similarly, `<em>` conveys stressed emphasis semantically; `<i>` is just italic styling (used for foreign words, technical terms). Prefer semantic tags for accessibility.

**Q2. When would you use `<div>` vs `<span>`?**
> Use `<div>` as a block-level container for grouping larger sections of content or layout. Use `<span>` as an inline container for styling or scripting a specific piece of text without breaking flow. Neither has any default visual meaning.

**Q3. What is the purpose of the `<meta>` tag? Give 3 important uses.**
> `<meta>` provides metadata to browsers and search engines:
> 1. `<meta charset="UTF-8">` — sets character encoding
> 2. `<meta name="viewport" content="width=device-width, initial-scale=1">` — enables responsive design on mobile
> 3. `<meta name="description" content="...">` — provides SEO page description shown in search results

---

# HTML FORMS

## 10. Form Elements and Attributes

**Q1. What is the purpose of the `<form>` element and its key attributes?**
> `<form>` wraps all form controls and defines how data is submitted. Key attributes:
> - `action` — URL where form data is sent
> - `method` — HTTP method (`get` or `post`)
> - `enctype` — encoding type (use `multipart/form-data` for file uploads)
> - `novalidate` — disables browser validation

**Q2. What is the difference between GET and POST form submission?**
> **GET** appends data to the URL as query parameters (visible, bookmarkable, limited length). Use for search forms. **POST** sends data in the request body (hidden from URL, no size limit). Use for sensitive data like passwords or large payloads.

**Q3. What is the role of the `<label>` element in a form?**
> `<label>` associates descriptive text with a form control, improving usability and accessibility. Clicking the label focuses/activates the linked input. Link via `for` attribute matching the input's `id`: `<label for="email">Email</label> <input id="email">`. Screen readers use labels to describe inputs.

---

## 11. Input Elements and Input Types

**Q1. What are the different input types in HTML5?**
> HTML5 introduced many semantic input types: `text`, `password`, `email`, `number`, `tel`, `url`, `date`, `time`, `datetime-local`, `month`, `week`, `range`, `color`, `checkbox`, `radio`, `file`, `hidden`, `submit`, `button`, `reset`, `search`. Each provides appropriate UI and built-in validation.

**Q2. What is the difference between `type="button"`, `type="submit"`, and `type="reset"`?**
> - `type="submit"` — submits the form to the server (default button behavior inside a form)
> - `type="reset"` — resets all form fields to their default values
> - `type="button"` — does nothing by default; used with JavaScript event listeners for custom behavior

**Q3. What attributes control input validation?**
> - `required` — field must be filled
> - `minlength` / `maxlength` — text length constraints
> - `min` / `max` — numeric range constraints
> - `pattern` — regex pattern the value must match
> - `type="email"` / `type="url"` — format validation built into the type

---

## 12. Select and Multi Select

**Q1. How do you create a dropdown select in HTML?**
> ```html
> <select name="city" id="city">
>   <option value="">-- Choose a city --</option>
>   <option value="mumbai">Mumbai</option>
>   <option value="delhi">Delhi</option>
> </select>
> ```
> The `<select>` element wraps `<option>` elements. The `value` attribute is what gets submitted; the text between tags is what the user sees.

**Q2. How do you allow multiple selections in a `<select>`?**
> Add the `multiple` attribute: `<select multiple name="skills">`. Users can Ctrl+click (or Cmd+click) to select multiple options. The submitted data will contain multiple values for the same field name. In HTML, use `name="skills[]"` convention for server-side array parsing.

**Q3. What is `<optgroup>` and when would you use it?**
> `<optgroup label="Group Name">` groups related options within a `<select>`, making long dropdowns easier to navigate. The group label is displayed but not selectable. Example: grouping countries by continent in a country picker.

---

## 13. Submitting Forms

**Q1. What happens when a form is submitted?**
> The browser collects all form control values (only enabled, named controls are included). For GET, it appends them as URL query parameters. For POST, it sends them in the request body. The browser then navigates to the `action` URL with the response. With JavaScript, you can intercept submission using `event.preventDefault()` and handle it via Fetch API.

**Q2. How do you prevent a form from reloading the page on submit?**
> Use JavaScript to intercept the submit event:
> ```js
> document.querySelector('form').addEventListener('submit', function(e) {
>   e.preventDefault();
>   // handle form data with fetch/AJAX
> });
> ```
> `e.preventDefault()` stops the default browser submit behavior.

**Q3. What is `FormData` and how is it used?**
> `FormData` is a built-in JavaScript object that captures form field values (including files) for use with Fetch or XMLHttpRequest:
> ```js
> const form = document.querySelector('form');
> const data = new FormData(form);
> fetch('/submit', { method: 'POST', body: data });
> ```
> It automatically sets the correct `Content-Type` header for multipart/form-data.

---

## 14. HTML5 Validation

**Q1. What is HTML5 built-in form validation?**
> HTML5 provides client-side validation without JavaScript using attributes like `required`, `type`, `min`, `max`, `pattern`, and `minlength`. The browser shows native error messages and prevents form submission if validation fails. It's a first line of defense but must always be paired with server-side validation.

**Q2. How do you customize HTML5 validation error messages?**
> Use the `setCustomValidity()` method and the `invalid` event in JavaScript:
> ```js
> input.addEventListener('input', () => {
>   if (input.value.length < 3) {
>     input.setCustomValidity('Must be at least 3 characters');
>   } else {
>     input.setCustomValidity('');
>   }
> });
> ```
> An empty string clears the custom error.

**Q3. What is the `novalidate` attribute and when would you use it?**
> Adding `novalidate` to a `<form>` disables all browser built-in validation. Use it when you want to implement fully custom JavaScript validation with your own UI/error messages, or when you're handling validation entirely on the server side.

---

# CSS FOUNDATION

## 15. Overview of CSS

**Q1. What is CSS and how does it relate to HTML?**
> CSS (Cascading Style Sheets) controls the visual presentation of HTML elements — layout, colors, fonts, spacing, and animations. HTML provides structure; CSS provides style. CSS works by selecting HTML elements and applying declarations (property: value pairs) to them.

**Q2. What is a CSS rule and what are its parts?**
> A CSS rule has two parts:
> - **Selector** — targets the HTML element(s)
> - **Declaration block** — contains one or more property-value pairs
> ```css
> p {              /* selector */
>   color: blue;   /* declaration */
>   font-size: 16px;
> }
> ```

**Q3. What does the "Cascading" in CSS mean?**
> Cascading refers to the process by which the browser determines which CSS rule to apply when multiple rules target the same element. It considers three factors in order: **Specificity** (how targeted the selector is), **Source order** (later rules win when specificity is equal), and **Importance** (`!important` overrides all). This cascade resolves conflicts between rules.

---

## 16. Inline, Internal, and External Stylesheets

**Q1. What are the three ways to add CSS to a page?**
> 1. **Inline** — `style` attribute directly on an element: `<p style="color:red">`. Highest specificity, hardest to maintain.
> 2. **Internal** — `<style>` block inside `<head>`. Scoped to that page only.
> 3. **External** — separate `.css` file linked via `<link rel="stylesheet" href="style.css">`. Best practice: reusable across pages, cacheable by browsers.

**Q2. What are the pros and cons of inline styles?**
> **Pros:** Highest specificity (overrides most other styles), quick for one-off fixes, no extra files.
> **Cons:** Mixes content with presentation, not reusable, hard to maintain at scale, can't use pseudo-classes or media queries inline.

**Q3. Why is external CSS preferred over internal CSS?**
> External CSS files are cached by the browser after the first load, speeding up subsequent page visits. External files can be shared across multiple pages (write once, apply everywhere). They separate concerns (HTML structure vs. CSS presentation), making codebases easier to maintain and scale.

---

## 17. CSS Properties

**Q1. What categories of CSS properties exist?**
> - **Text/Font:** `color`, `font-size`, `font-family`, `font-weight`, `text-align`, `line-height`
> - **Box Model:** `margin`, `padding`, `border`, `width`, `height`
> - **Layout:** `display`, `position`, `flex`, `grid`, `float`
> - **Background:** `background-color`, `background-image`, `background-size`
> - **Visual:** `opacity`, `box-shadow`, `border-radius`, `overflow`
> - **Animation:** `transition`, `animation`, `transform`

**Q2. What is the difference between `color` and `background-color`?**
> `color` sets the foreground text color of an element and its children. `background-color` sets the background fill of the element's box. Both accept color values: named colors (`red`), hex (`#ff0000`), rgb (`rgb(255,0,0)`), hsl, or `transparent`.

**Q3. What CSS units are available and how do they differ?**
> - **Absolute:** `px` (pixels), `pt` (points), `cm`
> - **Relative to font:** `em` (relative to parent font-size), `rem` (relative to root font-size)
> - **Relative to viewport:** `vw` (viewport width %), `vh` (viewport height %)
> - **Percentage:** relative to parent element's size
>
> Prefer `rem` for font sizes and `%`/`vw`/`vh` for layout to support responsive design.

---

## 18. Element Selectors

**Q1. What is a CSS element selector?**
> An element (type) selector targets all instances of a specific HTML tag. Example: `p { color: gray; }` styles every `<p>` on the page. It has the lowest specificity among selector types (excluding the universal selector `*`).

**Q2. What is the universal selector and when should you use it?**
> `*` selects every element on the page. It's commonly used in CSS resets:
> ```css
> * {
>   margin: 0;
>   padding: 0;
>   box-sizing: border-box;
> }
> ```
> Use sparingly for resets — avoid using it for general styling as it applies to everything.

**Q3. How do you select multiple elements with one rule?**
> Use a comma-separated list of selectors:
> ```css
> h1, h2, h3 {
>   font-family: Arial, sans-serif;
> }
> ```
> This groups selectors so the same declarations apply to all of them, reducing code repetition.

---

## 19. Class and ID Selectors

**Q1. How do class and ID selectors differ in syntax and usage?**
> - **Class selector** uses `.` prefix: `.card { ... }` — targets elements with `class="card"`. Multiple elements can share a class.
> - **ID selector** uses `#` prefix: `#header { ... }` — targets the single element with `id="header"`. IDs must be unique per page.
>
> ID selectors have higher specificity than class selectors.

**Q2. When should you prefer classes over IDs for styling?**
> Almost always prefer classes for CSS. IDs have very high specificity, making them hard to override. Classes are reusable and composable — you can apply multiple classes to one element (`class="card featured"`). Reserve IDs for JavaScript targeting or fragment links (`<a href="#section1">`).

**Q3. What is a compound selector? Give an example.**
> A compound selector combines multiple selectors without a space to target an element matching ALL conditions:
> ```css
> p.intro { color: blue; }      /* <p class="intro"> only */
> a.btn.primary { ... }         /* element with BOTH classes */
> ```
> Without a space, it means "AND" (same element). With a space it becomes a descendant combinator ("inside of").

---

## 20. CSS Box Model

**Q1. What is the CSS box model?**
> Every HTML element is represented as a rectangular box with four layers (from inside out):
> 1. **Content** — the actual text/image
> 2. **Padding** — space between content and border (inside)
> 3. **Border** — the edge line around padding
> 4. **Margin** — space outside the border (between elements)
>
> Total element width = content + padding-left + padding-right + border-left + border-right + margin-left + margin-right (in default box-sizing).

**Q2. What is the difference between `box-sizing: content-box` and `box-sizing: border-box`?**
> - `content-box` (default): `width` sets only the content area. Padding and border are added ON TOP of the specified width.
> - `border-box`: `width` includes content + padding + border. Makes sizing much more predictable.
>
> Best practice: apply `* { box-sizing: border-box; }` globally.

**Q3. What is margin collapse?**
> When two vertically adjacent elements both have margins, the margins don't add — they collapse into a single margin equal to the larger of the two. Example: if `<p>` below has `margin-top: 20px` and the `<h2>` above has `margin-bottom: 30px`, the space between them is 30px (not 50px). Margin collapse does NOT happen horizontally or with flex/grid children.

---

# CSS3 FEATURES

## 21. CSS Variables (Custom Properties)

**Q1. What are CSS custom properties (variables) and how do you define them?**
> CSS custom properties store reusable values. Define them on `:root` for global scope using `--` prefix:
> ```css
> :root {
>   --primary-color: #3498db;
>   --font-size-base: 16px;
> }
> p { color: var(--primary-color); }
> ```
> They cascade and can be overridden at any scope level, unlike preprocessor variables.

**Q2. How are CSS variables different from Sass/LESS variables?**
> CSS variables are **live** — they are part of the browser's rendering engine and can be changed at runtime via JavaScript (`element.style.setProperty('--color', 'red')`). They also respond to the cascade and inheritance. Sass/LESS variables are compiled away at build time — they don't exist in the browser.

**Q3. How can you use CSS variables to implement theming?**
> Define theme-specific variables and swap them using a class or media query:
> ```css
> :root { --bg: white; --text: black; }
> [data-theme="dark"] { --bg: #1a1a1a; --text: white; }
> body { background: var(--bg); color: var(--text); }
> ```
> Toggle `data-theme="dark"` on `<body>` with JavaScript to switch themes without changing any other CSS.

---

## 22. CSS Grid

**Q1. What is CSS Grid and when should you use it?**
> CSS Grid is a two-dimensional layout system that controls both rows AND columns simultaneously. Use Grid when you need complex layouts with alignment in both axes — like page layouts, dashboards, card grids. Use Flexbox for one-dimensional (row OR column) alignment.

**Q2. Explain `grid-template-columns` with an example.**
> It defines the column structure of the grid:
> ```css
> .container {
>   display: grid;
>   grid-template-columns: 200px 1fr 1fr;
>   /* 1st col: 200px fixed, cols 2&3: equal share of remaining space */
>   gap: 16px;
> }
> ```
> `fr` = fractional unit. `repeat(3, 1fr)` creates 3 equal columns.

**Q3. What is `grid-area` and how does it work with `grid-template-areas`?**
> `grid-template-areas` lets you name regions of your grid visually:
> ```css
> .container {
>   grid-template-areas:
>     "header header"
>     "sidebar main"
>     "footer footer";
> }
> header { grid-area: header; }
> aside  { grid-area: sidebar; }
> main   { grid-area: main; }
> ```
> This creates a clear, readable layout map directly in CSS.

---

## 23. Flexbox

**Q1. What is Flexbox and what problems does it solve?**
> Flexbox (Flexible Box Layout) is a one-dimensional layout model for distributing space along a row or column. It solves classic CSS problems like: vertically centering an element, equal-height columns, distributing items with space between them, and reordering elements visually without changing HTML.

**Q2. What is the difference between `justify-content` and `align-items`?**
> - `justify-content` — aligns flex items along the **main axis** (horizontal by default): `flex-start`, `flex-end`, `center`, `space-between`, `space-around`, `space-evenly`
> - `align-items` — aligns flex items along the **cross axis** (vertical by default): `flex-start`, `flex-end`, `center`, `stretch`, `baseline`
>
> Tip: main axis is set by `flex-direction` (row or column).

**Q3. What is `flex-grow`, `flex-shrink`, and `flex-basis`?**
> These three properties control how flex items size themselves:
> - `flex-grow` — how much the item grows relative to others (0 = no grow)
> - `flex-shrink` — how much it shrinks when space is tight (0 = no shrink)
> - `flex-basis` — the initial size before growing/shrinking (like width)
>
> Shorthand: `flex: 1` equals `flex-grow: 1; flex-shrink: 1; flex-basis: 0%`. Use `flex: 1` to make items share space equally.

---

# CSS DEPTH

## 24. Sibling Selectors

**Q1. What is the adjacent sibling combinator (`+`) in CSS?**
> The `+` combinator selects an element that is **immediately** after a specific sibling:
> ```css
> h2 + p { color: blue; }
> ```
> This styles only the first `<p>` that directly follows an `<h2>`. They must share the same parent.

**Q2. What is the general sibling combinator (`~`)?**
> The `~` combinator selects **all** siblings after a given element (not just the immediate one):
> ```css
> h2 ~ p { color: gray; }
> ```
> This styles every `<p>` that comes after an `<h2>` within the same parent — not just the one immediately after.

**Q3. What is the difference between a child combinator (`>`) and a descendant combinator (` `)?**
> - **Descendant (` `):** `div p` selects any `<p>` anywhere inside a `<div>`, at any nesting depth.
> - **Child (`>`):** `div > p` selects only `<p>` elements that are **direct children** of `<div>`, not nested deeper.

---

## 25. Advanced Selectors

**Q1. What are attribute selectors? Give examples.**
> Attribute selectors target elements based on their attributes:
> ```css
> [type="text"]       /* exact match */
> [href^="https"]     /* starts with */
> [src$=".jpg"]       /* ends with */
> [class*="btn"]      /* contains */
> [data-active]       /* attribute exists */
> ```
> They're powerful for styling form inputs by type or links by protocol.

**Q2. What are pseudo-classes? Give 5 examples.**
> Pseudo-classes select elements based on their state or position:
> - `:hover` — mouse over element
> - `:focus` — element has keyboard/input focus
> - `:nth-child(n)` — element at position n
> - `:first-child` / `:last-child` — first/last child of parent
> - `:checked` — checked checkbox or radio
> - `:not(selector)` — elements NOT matching selector
> - `:disabled` — disabled form control

**Q3. What are pseudo-elements? How do they differ from pseudo-classes?**
> Pseudo-elements create virtual elements for styling parts of an element's content:
> - `::before` — inserts content before element's content
> - `::after` — inserts content after element's content
> - `::first-line` — styles the first line of text
> - `::placeholder` — styles input placeholder text
>
> Pseudo-classes (`:`) target existing element states. Pseudo-elements (`::`) create new virtual elements. The double colon `::` distinguishes them (though single `:` still works for legacy reasons).

---

## 26. Cascading Nature of CSS

**Q1. What is the CSS cascade and how does it resolve conflicts?**
> When multiple CSS rules apply to the same element, the cascade determines which wins using this priority order:
> 1. `!important` declarations
> 2. Inline styles (`style=""`)
> 3. ID selectors
> 4. Class, attribute, pseudo-class selectors
> 5. Element/tag selectors
> 6. Source order (last rule wins among equal-specificity rules)

**Q2. What does CSS inheritance mean?**
> Some CSS properties are automatically inherited by child elements from their parents. Text-related properties like `color`, `font-family`, `font-size`, and `line-height` inherit by default. Layout properties like `margin`, `padding`, `border`, and `width` do NOT inherit. You can explicitly control this with `inherit`, `initial`, or `unset` values.

**Q3. What does `!important` do and why should it be avoided?**
> `!important` overrides all other specificity rules, forcing a declaration to apply. It breaks the natural cascade, making stylesheets hard to debug and override. It's a code smell — if you need it, it usually indicates a specificity or architecture problem. Avoid it except for utility classes or in absolutely unavoidable situations.

---

## 27. Specificity

**Q1. How is CSS specificity calculated?**
> Specificity is calculated as a score in the format **(A, B, C)**:
> - **A** = number of ID selectors (`#id`)
> - **B** = number of class, attribute, pseudo-class selectors
> - **C** = number of element/tag selectors
>
> Example: `#nav .menu li:hover` → (1, 2, 1)
> Higher A beats any number of B; higher B beats any number of C.

**Q2. Which has higher specificity: `#id` or `.class.class.class.class`?**
> `#id` wins. A single ID (1,0,0) outweighs any number of classes (0,n,0). This is why overusing IDs for styling creates specificity wars and maintenance headaches.

**Q3. How do you override a high-specificity rule without using `!important`?**
> Options:
> 1. Match or increase specificity of the overriding selector
> 2. Move the overriding rule later in the file (source order)
> 3. Refactor the original to use lower specificity (e.g., replace `#id` with a class)
> 4. In modern CSS (2023+), use `@layer` to manage specificity through cascade layers

---

## 28. Responsive Web Design

**Q1. What is responsive web design?**
> Responsive web design (RWD) is an approach where a website's layout and content adapt to fit different screen sizes and devices. Key tools: fluid grids (using `%` or `fr`), flexible images (`max-width: 100%`), and CSS media queries to apply different styles at different breakpoints.

**Q2. What are media queries? Write an example.**
> Media queries apply CSS rules only when certain conditions (like screen width) are met:
> ```css
> /* Mobile first — base styles for small screens */
> .container { flex-direction: column; }
>
> /* Override for screens 768px and wider */
> @media (min-width: 768px) {
>   .container { flex-direction: row; }
> }
> ```
> Common breakpoints: 480px (mobile), 768px (tablet), 1024px (desktop).

**Q3. What is the viewport meta tag and why is it essential for responsive design?**
> Without it, mobile browsers zoom out to show the desktop version:
> ```html
> <meta name="viewport" content="width=device-width, initial-scale=1">
> ```
> `width=device-width` sets the viewport width to the device's screen width. `initial-scale=1` prevents default zooming. Without this tag, media queries won't work correctly on mobile devices.

---

# JAVASCRIPT INTRODUCTION

## 29. JavaScript Introduction

**Q1. What is JavaScript and what can it do in the browser?**
> JavaScript is a lightweight, interpreted, dynamically typed programming language. In the browser it can: manipulate the DOM, handle user events, validate forms, make HTTP requests (Fetch/AJAX), animate elements, and interact with browser APIs (localStorage, geolocation, etc.). It's the only language that runs natively in browsers.

**Q2. What is the difference between JavaScript runtime in the browser vs Node.js?**
> The browser provides Web APIs (DOM, Fetch, setTimeout, localStorage). Node.js provides server-side APIs (file system, HTTP server, OS). Both environments use the V8 JavaScript engine (Chrome/Node) but expose different global objects: browsers have `window`; Node has `global` and `process`.

**Q3. What is `"use strict"` and what does it enable? (brief intro)**
> `"use strict"` enables strict mode, which catches common coding mistakes and prevents certain unsafe features. It must be placed at the top of a file or function. It's a prerequisite for some ES6+ features and is automatically enabled in ES6 modules and classes.

---

## 30. Datatypes

**Q1. What are the primitive data types in JavaScript?**
> JavaScript has 7 primitive types:
> 1. `string` — `"hello"`
> 2. `number` — `42`, `3.14`, `NaN`, `Infinity`
> 3. `boolean` — `true` / `false`
> 4. `undefined` — variable declared but not assigned
> 5. `null` — intentional absence of value
> 6. `symbol` — unique identifier
> 7. `bigint` — integers beyond `Number.MAX_SAFE_INTEGER`
>
> Everything else (arrays, objects, functions) is an **object**.

**Q2. What is the difference between `undefined` and `null`?**
> `undefined` — JavaScript's default for an uninitialized variable or missing parameter. The engine sets it automatically.
> `null` — an intentional, programmer-assigned empty value meaning "no object here."
> ```js
> typeof undefined // "undefined"
> typeof null      // "object"  ← historical bug in JS
> null == undefined  // true (loose)
> null === undefined // false (strict)
> ```

**Q3. What is `NaN` and how do you check for it?**
> `NaN` (Not a Number) is a special numeric value representing an invalid math operation (e.g., `"abc" * 2`, `0/0`). The odd thing: `NaN !== NaN` (NaN is not equal to itself). To check, use `Number.isNaN(value)` (strict — only true for actual NaN) or `isNaN(value)` (converts to number first, less reliable).

---

## 31. Type Coercion

**Q1. What is type coercion in JavaScript?**
> Type coercion is JavaScript's automatic conversion of values from one type to another during operations. **Implicit coercion** happens automatically: `"5" + 3 → "53"` (number coerced to string). **Explicit coercion** is done intentionally: `Number("5")`, `String(42)`, `Boolean(0)`.

**Q2. What is the difference between `==` and `===`?**
> `==` (loose equality) performs type coercion before comparing: `0 == "0"` is `true`, `null == undefined` is `true`.
> `===` (strict equality) compares value AND type without coercion: `0 === "0"` is `false`.
> Always prefer `===` to avoid unexpected coercion bugs.

**Q3. What values are falsy in JavaScript?**
> Exactly 6 falsy values: `false`, `0`, `""` (empty string), `null`, `undefined`, `NaN`.
> Everything else is truthy — including `[]`, `{}`, `"0"`, and `"false"`.
> Understanding falsy values is essential for writing correct conditional logic.

---

## 32. Arrays

**Q1. What are the key array methods in JavaScript and what do they do?**
> - `push` / `pop` — add/remove from end
> - `shift` / `unshift` — remove/add from beginning
> - `splice(start, deleteCount, ...items)` — remove/insert at any position
> - `slice(start, end)` — returns a copy of a portion (non-mutating)
> - `indexOf` / `includes` — find elements
> - `join` — convert to string
> - `concat` — merge arrays
> - `reverse` / `sort` — reorder (mutating)
> - `flat` / `flatMap` — flatten nested arrays

**Q2. What is the difference between `map`, `filter`, and `reduce`?**
> - `map(fn)` — transforms each element, returns new array of same length
> - `filter(fn)` — keeps only elements where fn returns true, returns new array
> - `reduce(fn, initial)` — accumulates all elements into a single value
> ```js
> [1,2,3].map(x => x * 2)        // [2, 4, 6]
> [1,2,3].filter(x => x > 1)     // [2, 3]
> [1,2,3].reduce((acc,x) => acc+x, 0) // 6
> ```

**Q3. What is the difference between `for...of` and `forEach`?**
> `forEach(fn)` is an array method that executes a callback for each element. You cannot use `break`, `continue`, or `return` from it (return only exits the callback, not the loop).
> `for...of` is a loop statement that works on any iterable. You CAN use `break` and `continue`. It's generally more flexible.

---

## 33. Functions

**Q1. What are the different ways to define a function in JavaScript?**
> 1. **Function declaration:** `function greet() { }` — hoisted
> 2. **Function expression:** `const greet = function() { }` — not hoisted
> 3. **Arrow function:** `const greet = () => { }` — no own `this`
> 4. **Method shorthand:** `{ greet() { } }` — in object literals
> 5. **Immediately Invoked Function Expression (IIFE):** `(function() { })()` — runs immediately

**Q2. What is a higher-order function?**
> A higher-order function is one that either takes a function as an argument, or returns a function. Examples: `map`, `filter`, `reduce`, `setTimeout`. Writing HOFs allows for powerful patterns like callbacks, function composition, and currying.

**Q3. What is a closure?**
> A closure is a function that retains access to its outer (enclosing) scope even after the outer function has returned:
> ```js
> function counter() {
>   let count = 0;
>   return function() { return ++count; };
> }
> const inc = counter();
> inc(); // 1
> inc(); // 2
> ```
> The inner function "closes over" `count`. Closures enable data encapsulation and stateful functions.

---

## 34. Variable Scopes

**Q1. What are the three types of scope in JavaScript?**
> 1. **Global scope** — variables declared outside any function or block. Accessible everywhere.
> 2. **Function scope** — variables declared with `var` inside a function. Accessible only within that function.
> 3. **Block scope** — variables declared with `let` or `const` inside `{}`. Accessible only within that block.

**Q2. What is lexical scope?**
> Lexical scope means that the scope of a variable is determined by where it is **written** in the source code (at author time), not where the function is called from (at runtime). Inner functions have access to variables of all their outer functions. This is the basis of closures.

**Q3. What is the scope chain?**
> When JavaScript looks up a variable, it first checks the current scope. If not found, it goes up to the enclosing scope, then up again, until it reaches the global scope. This chain of scope lookups is the scope chain. If the variable isn't found in the global scope, a `ReferenceError` is thrown.

---

## 35. Let and Const Keywords

**Q1. What are the differences between `var`, `let`, and `const`?**
> | Feature | `var` | `let` | `const` |
> |---|---|---|---|
> | Scope | Function | Block | Block |
> | Hoisting | Yes (undefined) | Yes (TDZ) | Yes (TDZ) |
> | Re-declare | Yes | No | No |
> | Re-assign | Yes | Yes | No |
>
> Use `const` by default; `let` when reassignment is needed; avoid `var`.

**Q2. What is the Temporal Dead Zone (TDZ)?**
> `let` and `const` are hoisted but not initialized — accessing them before their declaration in the code throws a `ReferenceError`. This "dead zone" between the start of the block and the declaration is the TDZ. It exists to catch bugs from using variables before they're set up.

**Q3. Does `const` make an object immutable?**
> No. `const` prevents **reassignment** of the variable binding, but the object's contents (properties) can still be changed:
> ```js
> const obj = { name: "Alice" };
> obj.name = "Bob";  // ✅ allowed
> obj = {};          // ❌ TypeError
> ```
> To make an object truly immutable, use `Object.freeze(obj)`.

---

## 36. Strict Mode

**Q1. What is strict mode and how do you enable it?**
> Strict mode is a way to opt into a more restricted version of JavaScript. It catches common mistakes, disables some dangerous features, and makes code easier to optimize.
> Enable at file level: `"use strict";` at the very top.
> Enable at function level: `"use strict";` at the top of a function.
> ES6 modules and classes use strict mode automatically.

**Q2. What does strict mode prevent?**
> - Using undeclared variables (`x = 5` without `var/let/const` throws)
> - Deleting variables or functions
> - Duplicate parameter names in functions
> - Writing to read-only properties
> - Using reserved future keywords (`let`, `static`, `implements`) as variable names
> - `this` in functions defaults to `undefined` (not global object)

**Q3. Why is strict mode important for modern JavaScript development?**
> It catches silent errors that would otherwise fail quietly — like accidentally creating global variables by misspelling a variable name. It also makes refactoring safer and is required for some ES6+ features. Linters and build tools often enforce it, and all ES6 modules run in strict mode by default.

---

## 37. Arrow Functions

**Q1. What is the syntax of an arrow function?**
> ```js
> // Traditional
> const add = function(a, b) { return a + b; };
>
> // Arrow - with body
> const add = (a, b) => { return a + b; };
>
> // Arrow - implicit return (single expression)
> const add = (a, b) => a + b;
>
> // Single parameter - parentheses optional
> const double = x => x * 2;
>
> // No parameters
> const greet = () => "Hello";
> ```

**Q2. How does `this` behave differently in arrow functions vs regular functions?**
> Regular functions have their own `this` — determined by how they're called (the calling context). Arrow functions do NOT have their own `this` — they inherit `this` from the surrounding lexical scope at the time they are defined. This makes arrow functions ideal for callbacks and methods that need to access the parent's `this`.

**Q3. When should you NOT use arrow functions?**
> Avoid arrow functions when you need their own `this`:
> - Object methods (use regular function/method shorthand so `this` refers to the object)
> - Constructor functions (arrow functions can't be used with `new`)
> - `addEventListener` callbacks where `this` should be the element
> - `prototype` methods that rely on `this`

---

## 38. Template Literals

**Q1. What are template literals and how do you use them?**
> Template literals (backtick strings) support multi-line strings and embedded expressions:
> ```js
> const name = "World";
> const greeting = `Hello, ${name}!`;
> // "Hello, World!"
>
> const multi = `Line 1
> Line 2`;  // actual newline
> ```
> Use backticks (`` ` ``) instead of quotes, and `${expression}` for interpolation.

**Q2. What can you put inside `${}` in a template literal?**
> Any valid JavaScript expression: variables, arithmetic, function calls, ternary operators:
> ```js
> `${2 + 2}`           // "4"
> `${user.name.toUpperCase()}`
> `${isLoggedIn ? "Welcome" : "Please log in"}`
> `${items.map(i => `<li>${i}</li>`).join('')}`
> ```

**Q3. What are tagged template literals?**
> A tag is a function placed before a template literal that processes it:
> ```js
> function highlight(strings, ...values) {
>   return strings.reduce((acc, str, i) =>
>     acc + str + (values[i] ? `<b>${values[i]}</b>` : ''), '');
> }
> const name = "Alice";
> highlight`Hello ${name}!`;  // "Hello <b>Alice</b>!"
> ```
> Libraries like `styled-components` (CSS-in-JS) and `graphql` use tagged templates.

---

## 39. Naming Conventions

**Q1. What are JavaScript naming conventions for variables, functions, classes, and constants?**
> - **Variables & functions:** `camelCase` — `firstName`, `getUserData()`
> - **Classes & constructors:** `PascalCase` — `UserProfile`, `ShoppingCart`
> - **Constants (compile-time):** `UPPER_SNAKE_CASE` — `MAX_RETRIES`, `API_BASE_URL`
> - **Private (convention):** `_prefixed` — `_internalState` (not enforced by JS)
> - **Files:** `kebab-case.js` or `camelCase.js` (team-dependent)

**Q2. Why do naming conventions matter?**
> Consistent naming makes code readable, reduces cognitive load, and helps teams communicate about code. Good names serve as documentation — `calculateTotalPrice()` is self-explanatory; `fn3()` is not. Conventions also help linters and other developers instantly identify the type and purpose of a value.

**Q3. What makes a good variable or function name?**
> - **Descriptive:** `userAge` > `a`, `sendEmailNotification` > `send`
> - **Intent-revealing:** reveals WHY, not just WHAT
> - **Appropriately scoped:** short names OK for tight loops (`i`, `x`), longer names for broader scope
> - **Avoid abbreviations** unless universally known (`id`, `url`, `html`)
> - **Boolean variables** should read as yes/no questions: `isLoggedIn`, `hasPermission`, `canEdit`

---

## 40. JavaScript Assignment Operators

**Q1. What are the assignment operators in JavaScript?**
> - `=` — basic assignment
> - `+=` — add and assign: `x += 5` → `x = x + 5`
> - `-=`, `*=`, `/=`, `%=` — subtract/multiply/divide/modulo and assign
> - `**=` — exponentiation assign: `x **= 2` → `x = x ** 2`
> - `&&=` — assign only if left is truthy: `x &&= y` → `if (x) x = y`
> - `||=` — assign only if left is falsy: `x ||= defaultValue`
> - `??=` — assign only if left is null/undefined (nullish coalescing assign)

**Q2. What is the nullish coalescing assignment operator `??=`?**
> `x ??= y` assigns `y` to `x` only if `x` is `null` or `undefined`. Unlike `||=`, it does NOT trigger on falsy values like `0` or `""`:
> ```js
> let count = 0;
> count ||= 10;  // count = 10 (treats 0 as falsy — probably a bug)
> count ??= 10;  // count = 0  (0 is not null/undefined — correct!)
> ```

**Q3. What is destructuring assignment?**
> Destructuring extracts values from arrays or objects into variables:
> ```js
> // Array destructuring
> const [a, b, ...rest] = [1, 2, 3, 4];
>
> // Object destructuring
> const { name, age = 25 } = user;  // age defaults to 25
>
> // Rename while destructuring
> const { name: userName } = user;
>
> // In function parameters
> function greet({ name, age }) { ... }
> ```

---

## 41. Arithmetic Operators

**Q1. What are the arithmetic operators in JavaScript?**
> - `+` — addition (also string concatenation)
> - `-` — subtraction
> - `*` — multiplication
> - `/` — division (always returns float)
> - `%` — modulus (remainder)
> - `**` — exponentiation (`2 ** 10 = 1024`)
> - `++` — increment (prefix `++x` vs postfix `x++`)
> - `--` — decrement

**Q2. What is the difference between prefix and postfix increment?**
> - **Postfix** (`x++`) — returns the current value THEN increments: `let x=5; y=x++; // y=5, x=6`
> - **Prefix** (`++x`) — increments FIRST THEN returns: `let x=5; y=++x; // y=6, x=6`
>
> In a standalone statement `x++;` or `++x;` the result is the same. The difference matters when used inside expressions.

**Q3. What does the `%` (modulo) operator do and what are its uses?**
> `%` returns the remainder after division: `7 % 3 = 1`, `10 % 2 = 0`.
> Common uses:
> - Check if a number is even/odd: `n % 2 === 0`
> - Wrap numbers in a range: `index % array.length` for circular loops
> - Check divisibility
> - Create alternating patterns (e.g., zebra stripe rows)

---

## 42. Comparison and Logical Operators

**Q1. What are the comparison operators in JavaScript?**
> - `==` — loose equality (coerces types)
> - `===` — strict equality (no coercion) ← always prefer this
> - `!=` — loose inequality
> - `!==` — strict inequality
> - `>`, `<`, `>=`, `<=` — greater/less than
> - When comparing strings, JavaScript compares Unicode code points: `"Z" < "a"` is true.

**Q2. What are the logical operators and how do they work?**
> - `&&` (AND) — returns first falsy value, or last value if all truthy: `true && "hi"` → `"hi"`
> - `||` (OR) — returns first truthy value, or last value if all falsy: `null || "default"` → `"default"`
> - `!` (NOT) — inverts a boolean: `!true` → `false`, `!!value` converts to boolean
> - `??` (Nullish coalescing) — returns right side only if left is `null`/`undefined`

**Q3. What is short-circuit evaluation and how is it used practically?**
> `&&` and `||` stop evaluating as soon as the result is determined:
> ```js
> // Conditional rendering pattern
> isLoggedIn && renderDashboard();
>
> // Default values
> const name = user.name || "Guest";
>
> // Safe property access (before optional chaining)
> const city = user && user.address && user.address.city;
>
> // Modern: use optional chaining
> const city = user?.address?.city;
> ```

---

## 43. Control Flow

**Q1. What control flow statements does JavaScript provide?**
> - **Conditionals:** `if/else if/else`, `switch`
> - **Loops:** `for`, `while`, `do...while`, `for...of`, `for...in`
> - **Loop control:** `break` (exit loop), `continue` (skip iteration)
> - **Exception handling:** `try/catch/finally`, `throw`
> - **Short circuits / ternary:** `condition ? a : b`

**Q2. When should you use `switch` over `if/else`?**
> Use `switch` when checking a single variable against multiple specific values:
> ```js
> switch(day) {
>   case "Mon": ...; break;
>   case "Tue": ...; break;
>   default: ...;
> }
> ```
> Without `break`, execution falls through to the next case. Use `if/else` for ranges, complex conditions, or boolean checks.

**Q3. What is the difference between `for...in` and `for...of`?**
> - `for...in` iterates over an object's **enumerable property keys** (including inherited ones). Use for plain objects: `for (let key in obj)`.
> - `for...of` iterates over **values** of any iterable (arrays, strings, Maps, Sets). Cannot be used on plain objects directly.
> - Never use `for...in` on arrays — it may iterate inherited prototype properties.

---

## 44. Object Literals

**Q1. How do you create an object literal in JavaScript?**
> ```js
> const person = {
>   name: "Alice",          // property
>   age: 30,
>   greet() {               // method shorthand (ES6)
>     return `Hi, I'm ${this.name}`;
>   }
> };
> ```
> Access with dot notation (`person.name`) or bracket notation (`person["name"]`).

**Q2. What are ES6 shorthand property names and computed properties?**
> ```js
> const name = "Alice";
> const age = 30;
>
> // Shorthand: variable name becomes property name
> const person = { name, age };  // same as { name: name, age: age }
>
> // Computed property names
> const key = "color";
> const obj = { [key]: "blue" };  // { color: "blue" }
> ```

**Q3. How do you copy or merge objects?**
> ```js
> // Spread operator (shallow copy)
> const copy = { ...original };
> const merged = { ...obj1, ...obj2 };
>
> // Object.assign (shallow copy)
> const copy = Object.assign({}, original);
>
> // Deep copy (native, modern)
> const deep = structuredClone(original);
>
> // Deep copy (older alternative)
> const deep = JSON.parse(JSON.stringify(original));
> // ⚠️ Loses functions, Dates, undefined values
> ```

---

# JAVASCRIPT ADVANCED

## 45. OOP (Object-Oriented Programming)

**Q1. What are the four pillars of OOP?**
> 1. **Encapsulation** — bundling data and methods that operate on it together; hiding internal state
> 2. **Abstraction** — exposing only what's necessary, hiding implementation details
> 3. **Inheritance** — a class inheriting properties and methods from a parent class
> 4. **Polymorphism** — different classes can be treated through the same interface; methods can behave differently based on the object

**Q2. How does JavaScript implement OOP?**
> JavaScript uses **prototype-based** OOP. Every object has an internal `[[Prototype]]` link to another object (its prototype). Properties/methods are looked up through the prototype chain. ES6 classes are syntactic sugar over this prototype mechanism — they don't introduce a new OOP model.

**Q3. What is the prototype chain?**
> When you access a property on an object, JavaScript first checks the object itself. If not found, it checks the object's prototype, then the prototype's prototype, and so on — up to `Object.prototype` (which returns `null` as its prototype, ending the chain). This chain of prototype lookups is how JavaScript implements inheritance.

---

## 46. The `this` Keyword

**Q1. What does `this` refer to in different contexts?**
> - **Global scope (non-strict):** `window` (browser) / `global` (Node)
> - **Regular function (non-strict):** the calling object (or global if called without object)
> - **Regular function (strict):** `undefined`
> - **Method:** the object that owns the method
> - **Arrow function:** lexically inherited — the `this` of the enclosing scope
> - **Constructor (`new`):** the newly created instance
> - **`call`/`apply`/`bind`:** explicitly set by you

**Q2. What is the difference between `call`, `apply`, and `bind`?**
> All three explicitly set `this`:
> - `fn.call(thisArg, arg1, arg2)` — calls immediately, args passed individually
> - `fn.apply(thisArg, [arg1, arg2])` — calls immediately, args passed as array
> - `fn.bind(thisArg, arg1)` — returns a new function with `this` permanently bound, doesn't call immediately

**Q3. Why does `this` lose context in callbacks, and how do you fix it?**
> ```js
> const obj = {
>   name: "Alice",
>   greet: function() {
>     setTimeout(function() {
>       console.log(this.name); // ❌ undefined — `this` is window/undefined
>     }, 1000);
>   }
> };
> ```
> Fix options:
> 1. Use arrow function in callback (inherits `this`)
> 2. Store `const self = this` before callback
> 3. Use `.bind(this)` on the callback

---

## 47. Classes

**Q1. What is the syntax of an ES6 class?**
> ```js
> class Animal {
>   constructor(name) {
>     this.name = name;
>   }
>   speak() {
>     return `${this.name} makes a sound.`;
>   }
>   static create(name) {     // static: called on class, not instance
>     return new Animal(name);
>   }
> }
> const dog = new Animal("Rex");
> ```

**Q2. How does class inheritance work with `extends` and `super`?**
> ```js
> class Dog extends Animal {
>   constructor(name, breed) {
>     super(name);        // must call super before using `this`
>     this.breed = breed;
>   }
>   speak() {
>     return `${this.name} barks!`;  // overrides parent method
>   }
> }
> ```
> `extends` sets up the prototype chain. `super()` calls the parent constructor. `super.method()` calls a parent method.

**Q3. What are getters and setters in classes?**
> ```js
> class Circle {
>   constructor(radius) { this._radius = radius; }
>
>   get radius() { return this._radius; }
>   set radius(value) {
>     if (value < 0) throw new Error("Radius cannot be negative");
>     this._radius = value;
>   }
>   get area() { return Math.PI * this._radius ** 2; }
> }
> const c = new Circle(5);
> c.radius = 10;      // calls setter
> c.area;             // calls getter, no parentheses needed
> ```

---

## 48. Hoisting

**Q1. What is hoisting in JavaScript?**
> Hoisting is JavaScript's behavior of moving declarations to the top of their scope during compilation before code executes. `var` declarations are hoisted and initialized to `undefined`. Function declarations are fully hoisted (name AND body). `let`/`const` are hoisted but stay in the Temporal Dead Zone until declaration.

**Q2. What is the difference in hoisting between function declarations and function expressions?**
> ```js
> greet(); // ✅ Works — function declaration is fully hoisted
> function greet() { return "Hello"; }
>
> greet2(); // ❌ TypeError — variable is hoisted as undefined, not the function
> var greet2 = function() { return "Hello"; };
>
> greet3(); // ❌ ReferenceError — let/const in TDZ
> const greet3 = () => "Hello";
> ```

**Q3. How does hoisting affect `var` in loops? Why is this a problem?**
> ```js
> for (var i = 0; i < 3; i++) {
>   setTimeout(() => console.log(i), 100);
> }
> // Prints: 3, 3, 3  (not 0, 1, 2)
> ```
> `var` is function-scoped — all iterations share the same `i`. By the time callbacks run, the loop is done and `i = 3`. Fix: use `let` (block-scoped, creates new `i` per iteration).

---

## 49. Errors

**Q1. What are the built-in error types in JavaScript?**
> - `Error` — generic base error
> - `TypeError` — wrong type: calling non-function, accessing property of null
> - `ReferenceError` — accessing undeclared variable
> - `SyntaxError` — invalid code syntax
> - `RangeError` — number out of valid range
> - `URIError` — malformed URI
> - `EvalError` — eval-related errors

**Q2. How do you handle errors with try/catch/finally?**
> ```js
> try {
>   const data = JSON.parse(invalidJson);
> } catch (error) {
>   console.error(error.name, error.message);
>   // Handle gracefully
> } finally {
>   // Always runs — cleanup here (close connections, stop loaders)
> }
> ```
> `finally` runs regardless of success or failure.

**Q3. How do you create and throw custom errors?**
> ```js
> class ValidationError extends Error {
>   constructor(field, message) {
>     super(message);
>     this.name = "ValidationError";
>     this.field = field;
>   }
> }
>
> function validateAge(age) {
>   if (age < 0) throw new ValidationError("age", "Age cannot be negative");
> }
>
> try {
>   validateAge(-1);
> } catch (e) {
>   if (e instanceof ValidationError) {
>     console.log(`Field: ${e.field}, Error: ${e.message}`);
>   }
> }
> ```

---

## 50. Default Parameters

**Q1. What are default parameters in JavaScript?**
> Default parameters allow function parameters to have a fallback value when not provided or when `undefined` is passed:
> ```js
> function greet(name = "Guest", greeting = "Hello") {
>   return `${greeting}, ${name}!`;
> }
> greet();              // "Hello, Guest!"
> greet("Alice");       // "Hello, Alice!"
> greet("Bob", "Hi");   // "Hi, Bob!"
> ```

**Q2. Can default parameters reference earlier parameters?**
> Yes! Default values are evaluated at call time, and later parameters can reference earlier ones:
> ```js
> function createBox(width = 10, height = width, depth = width) {
>   return { width, height, depth };
> }
> createBox(5);  // { width: 5, height: 5, depth: 5 }
> ```

**Q3. How were default parameters handled before ES6?**
> The old pattern used `||`:
> ```js
> function greet(name) {
>   name = name || "Guest";  // ⚠️ Problem: 0 or "" also triggers default
> }
> ```
> ES6 default parameters only trigger on `undefined` (not `null`, `0`, or `""`), making them more precise and readable.

---

## 51. Spread and Rest Operators

**Q1. What is the spread operator (`...`) and how is it used?**
> Spread expands an iterable (array, string, object) into individual elements:
> ```js
> // Arrays
> const merged = [...arr1, ...arr2];
> Math.max(...[1, 2, 3]);     // same as Math.max(1, 2, 3)
>
> // Objects
> const updated = { ...original, name: "Alice" };
>
> // Function call
> const args = [1, 2];
> fn(...args);
> ```

**Q2. What is the rest parameter (`...`) and how does it differ from spread?**
> Rest collects multiple arguments into a single array parameter — it must be the **last** parameter:
> ```js
> function sum(...numbers) {
>   return numbers.reduce((acc, n) => acc + n, 0);
> }
> sum(1, 2, 3, 4);  // 10
>
> function first(a, b, ...rest) {
>   console.log(a, b, rest);  // 1, 2, [3, 4, 5]
> }
> first(1, 2, 3, 4, 5);
> ```
> Same `...` syntax but: **spread** expands, **rest** collects.

**Q3. How do you use spread for immutable array/object updates?**
> ```js
> // Add to array (immutable)
> const newArr = [...arr, newItem];
>
> // Remove from array (immutable)
> const filtered = arr.filter(item => item.id !== idToRemove);
>
> // Update object property (immutable)
> const updated = { ...user, age: 31 };
>
> // Nested object update
> const updated = {
>   ...user,
>   address: { ...user.address, city: "Mumbai" }
> };
> ```
> This pattern is essential in React/Redux state management.

---

# JAVASCRIPT DOM

## 52. DOM Structure

**Q1. What is the DOM tree structure?**
> The DOM is a tree of nodes. At the top is the `document` node. Its child is `<html>`, which has two children: `<head>` and `<body>`. Every HTML element becomes an **Element node**. Text inside elements becomes **Text nodes**. HTML comments become **Comment nodes**. Attributes are **Attribute nodes** accessible via elements.

**Q2. What is the difference between `document` and `window`?**
> `window` is the global object in the browser — it represents the browser window. It contains everything including `document`, `console`, `setTimeout`, `localStorage`, `navigator`, `location`.
> `document` is a property of `window` — it represents the HTML document loaded in the window. DOM manipulation happens through `document`.

**Q3. What are the different types of nodes in the DOM?**
> - `Node.ELEMENT_NODE (1)` — HTML elements (`<div>`, `<p>`)
> - `Node.TEXT_NODE (3)` — text content inside elements
> - `Node.COMMENT_NODE (8)` — HTML comments
> - `Node.DOCUMENT_NODE (9)` — the `document` itself
>
> Check with `node.nodeType`. For most manipulation, you work with Element nodes.

---

## 53. Selecting Elements from the DOM

**Q1. What are the different ways to select DOM elements?**
> - `document.getElementById("id")` — single element by ID (fastest)
> - `document.querySelector("selector")` — first match by CSS selector
> - `document.querySelectorAll("selector")` — all matches (NodeList)
> - `document.getElementsByClassName("class")` — live HTMLCollection by class
> - `document.getElementsByTagName("tag")` — live HTMLCollection by tag
>
> Prefer `querySelector`/`querySelectorAll` for flexibility.

**Q2. What is the difference between a NodeList and an HTMLCollection?**
> Both are array-like but NOT arrays (no `map`, `filter`).
> - **HTMLCollection** (from `getElementsBy*`) is **live** — automatically updates when DOM changes.
> - **NodeList** (from `querySelectorAll`) is **static** — a snapshot at query time.
>
> Convert to array with `Array.from(nodeList)` or `[...nodeList]` to use array methods.

**Q3. What is the difference between `querySelector` and `getElementById`?**
> `getElementById("myId")` only accepts an ID (without `#`) and returns the matching element or `null`. It's marginally faster.
> `querySelector("#myId")` accepts any CSS selector (class, attribute, pseudo-class, etc.) and is far more flexible. Use `querySelector` for most cases.

---

## 54. DOM Manipulation

**Q1. How do you create and add elements to the DOM?**
> ```js
> // Create
> const div = document.createElement("div");
> div.textContent = "Hello";
> div.className = "card";
>
> // Add to DOM
> document.body.appendChild(div);     // append as last child
> parent.insertBefore(div, sibling);  // insert before sibling
> parent.prepend(div);                // modern: first child
> parent.append(div);                 // modern: last child
> sibling.before(div);                // modern: before element
> ```

**Q2. What is the difference between `innerHTML`, `textContent`, and `innerText`?**
> - `innerHTML` — gets/sets HTML markup (parses tags). ⚠️ XSS risk if set from user input!
> - `textContent` — gets/sets plain text (ignores HTML tags, includes hidden elements). Fast and safe.
> - `innerText` — gets/sets visible text only (respects CSS, triggers reflow). Slower but returns what user sees.
>
> Use `textContent` for safe text manipulation. Use `innerHTML` only with trusted/sanitized content.

**Q3. How do you modify CSS classes on an element?**
> ```js
> element.classList.add("active");
> element.classList.remove("active");
> element.classList.toggle("active");         // add if absent, remove if present
> element.classList.contains("active");       // boolean check
> element.classList.replace("old", "new");
>
> // Set inline style
> element.style.color = "blue";
> element.style.fontSize = "18px";  // camelCase in JS
> ```

---

## 55. Traversing the DOM

**Q1. How do you navigate the DOM tree (parent, children, siblings)?**
> ```js
> element.parentElement           // parent element
> element.children                // live HTMLCollection of child elements
> element.firstElementChild       // first child element
> element.lastElementChild        // last child element
> element.nextElementSibling      // next sibling element
> element.previousElementSibling  // previous sibling element
>
> // Node versions (include text/comment nodes)
> element.parentNode
> element.childNodes
> element.firstChild
> element.nextSibling
> ```

**Q2. What is the difference between `children` and `childNodes`?**
> `children` returns only **Element nodes** (ignores text and comment nodes). `childNodes` returns ALL node types including text nodes (whitespace counts!). Almost always use `children` for element traversal.

**Q3. How do you find the closest ancestor matching a selector?**
> Use `element.closest("selector")` — it traverses UP the DOM tree from the element and returns the first matching ancestor (including itself), or `null` if none found:
> ```js
> button.addEventListener("click", function(e) {
>   const card = e.target.closest(".card");
>   if (card) card.classList.add("selected");
> });
> ```
> `closest` is the upward equivalent of `querySelector`.

---

## 56. Events and Listeners

**Q1. How do you attach event listeners in JavaScript?**
> ```js
> // Preferred: addEventListener
> element.addEventListener("click", function(event) {
>   console.log("Clicked!", event.target);
> });
>
> // Remove listener (must use named function reference)
> element.removeEventListener("click", handler);
>
> // Avoid: inline HTML handlers — mixes JS with HTML
> // <button onclick="doSomething()"> — avoid
>
> // Avoid: direct property assignment (only one handler)
> // element.onclick = fn; — only allows one listener
> ```

**Q2. What is the `Event` object and what properties does it have?**
> The event object is automatically passed to the handler:
> - `event.type` — type of event ("click", "keydown")
> - `event.target` — element that triggered the event
> - `event.currentTarget` — element the listener is attached to
> - `event.preventDefault()` — prevent default browser action
> - `event.stopPropagation()` — stop event from bubbling up
> - `event.key`, `event.code` — for keyboard events
> - `event.clientX`, `event.clientY` — mouse position

**Q3. What is event delegation and why is it useful?**
> Event delegation attaches a single listener to a parent element instead of individual listeners on many children. It works because events bubble up:
> ```js
> document.querySelector("ul").addEventListener("click", function(e) {
>   if (e.target.tagName === "LI") {
>     console.log("Clicked:", e.target.textContent);
>   }
> });
> ```
> Benefits: fewer listeners (better performance), works for dynamically added elements, simpler code.

---

## 57. Bubbling and Capturing

**Q1. What is event bubbling?**
> When an event fires on an element, it first runs handlers on that element, then bubbles UP through ancestors to the document root. Example: clicking a `<button>` inside a `<div>` inside `<body>` — the click event triggers on button, then div, then body, then document.
> Most events bubble. Exceptions: `focus`, `blur`, `load`, `scroll` do NOT bubble.

**Q2. What is event capturing and how does it differ from bubbling?**
> Event capturing (trickling) is the opposite direction — events propagate DOWN from the document root to the target element BEFORE bubbling back up. By default, `addEventListener` registers listeners in the **bubble phase**. To listen in the capture phase: `element.addEventListener("click", handler, true)` or `{ capture: true }`.

**Q3. What is `event.stopPropagation()` and when would you use it?**
> `stopPropagation()` stops the event from continuing to bubble up (or capture down). Use it when an inner element should handle an event independently of its parent:
> ```js
> modal.addEventListener("click", () => closeModal());
> modalContent.addEventListener("click", (e) => {
>   e.stopPropagation(); // prevent click from reaching modal overlay
> });
> ```
> `stopImmediatePropagation()` also prevents other listeners on the SAME element from running.

---

# JAVASCRIPT HTTP

## 58. JSON

**Q1. What is JSON and what are its rules?**
> JSON (JavaScript Object Notation) is a text-based data format for storing and transporting data. Rules:
> - Keys must be **double-quoted strings**
> - Values can be: string, number, boolean, null, array, or object
> - No functions, `undefined`, or comments allowed
> - No trailing commas
>
> ```json
> { "name": "Alice", "age": 30, "active": true, "scores": [95, 87] }
> ```

**Q2. How do you convert between JSON and JavaScript objects?**
> ```js
> // Object → JSON string (serialization)
> const json = JSON.stringify({ name: "Alice", age: 30 });
> // '{"name":"Alice","age":30}'
>
> // JSON string → JavaScript object (parsing/deserialization)
> const obj = JSON.parse('{"name":"Alice"}');
>
> // Pretty print
> JSON.stringify(obj, null, 2);
>
> // Replacer: exclude certain properties
> JSON.stringify(obj, ["name"]);  // include only "name"
> ```

**Q3. What happens when `JSON.parse` or `JSON.stringify` encounters unsupported values?**
> `JSON.stringify`: `undefined`, functions, and Symbols are **omitted** from objects or converted to `null` in arrays. `Date` objects become strings. `JSON.parse` on invalid JSON throws `SyntaxError` — always wrap in try/catch.
> ```js
> try {
>   const data = JSON.parse(response);
> } catch (e) {
>   console.error("Invalid JSON:", e.message);
> }
> ```

---

## 59. Promises

**Q1. What is a Promise and what states can it be in?**
> A Promise is an object representing the eventual completion or failure of an async operation. States:
> - **Pending** — initial state, operation not done yet
> - **Fulfilled** — operation succeeded, has a value
> - **Rejected** — operation failed, has a reason (error)
>
> A promise can only transition from pending → fulfilled OR pending → rejected. It cannot go back.

**Q2. How do you create and consume a Promise?**
> ```js
> // Create
> const promise = new Promise((resolve, reject) => {
>   setTimeout(() => resolve("Done!"), 1000);
>   // or: reject(new Error("Failed"));
> });
>
> // Consume
> promise
>   .then(value => console.log(value))   // on success
>   .catch(error => console.error(error)) // on failure
>   .finally(() => console.log("Always runs"));
> ```

**Q3. What are `Promise.all`, `Promise.allSettled`, `Promise.race`, and `Promise.any`?**
> - `Promise.all([p1, p2])` — resolves when ALL resolve; rejects if ANY rejects
> - `Promise.allSettled([p1, p2])` — waits for ALL to settle (resolve or reject); never rejects
> - `Promise.race([p1, p2])` — resolves/rejects as soon as the FIRST one settles
> - `Promise.any([p1, p2])` — resolves with the FIRST successful one; rejects only if ALL reject

---

## 60. Fetch API

**Q1. How do you make a GET request with the Fetch API?**
> ```js
> fetch("https://api.example.com/users")
>   .then(response => {
>     if (!response.ok) throw new Error(`HTTP ${response.status}`);
>     return response.json();  // parse body as JSON
>   })
>   .then(data => console.log(data))
>   .catch(error => console.error("Request failed:", error));
> ```
> Note: `fetch` only rejects on network failure, NOT on HTTP error status codes (4xx, 5xx). Always check `response.ok`.

**Q2. How do you make a POST request with Fetch?**
> ```js
> fetch("/api/users", {
>   method: "POST",
>   headers: { "Content-Type": "application/json" },
>   body: JSON.stringify({ name: "Alice", age: 30 })
> })
>   .then(res => res.json())
>   .then(data => console.log(data));
> ```
> Set `Content-Type` header to tell the server what format you're sending.

**Q3. What is the difference between `response.json()`, `response.text()`, and `response.blob()`?**
> These are methods on the Response object to read the body:
> - `response.json()` — parses body as JSON, returns JS object
> - `response.text()` — returns body as a string
> - `response.blob()` — returns body as a Blob (binary data, for images/files)
> - `response.formData()` — returns body as FormData
>
> Each returns a Promise. The body can only be read once.

---

## 61. Async/Await

**Q1. What is async/await and how does it relate to Promises?**
> `async/await` is syntactic sugar over Promises, making async code look and behave more like synchronous code. An `async` function always returns a Promise. `await` pauses execution of the async function until the Promise resolves, then returns the resolved value.
> ```js
> async function fetchUser(id) {
>   const response = await fetch(`/api/users/${id}`);
>   const user = await response.json();
>   return user;
> }
> ```

**Q2. How do you handle errors with async/await?**
> Use try/catch:
> ```js
> async function getData() {
>   try {
>     const res = await fetch("/api/data");
>     if (!res.ok) throw new Error(`HTTP ${res.status}`);
>     const data = await res.json();
>     return data;
>   } catch (error) {
>     console.error("Error:", error.message);
>   }
> }
> ```
> Without try/catch, rejected promises become unhandled rejections. You can also `.catch()` on the returned promise.

**Q3. How do you run multiple async operations in parallel with async/await?**
> ```js
> // Sequential (slower — waits for each one)
> const user = await fetchUser(1);
> const posts = await fetchPosts(1);
>
> // Parallel (faster — all start simultaneously)
> const [user, posts] = await Promise.all([
>   fetchUser(1),
>   fetchPosts(1)
> ]);
> ```
> Use `Promise.all` with `await` when operations are independent. Only use sequential `await` when each operation depends on the previous result.

---

# JAVASCRIPT TESTING

## 62. AAA Pattern

**Q1. What is the AAA (Arrange-Act-Assert) pattern?**
> AAA is a standard structure for writing tests:
> - **Arrange** — set up test data, objects, and conditions
> - **Act** — call the function or perform the action being tested
> - **Assert** — verify the result matches expectations
>
> ```js
> test("adds two numbers", () => {
>   // Arrange
>   const a = 2, b = 3;
>   // Act
>   const result = add(a, b);
>   // Assert
>   expect(result).toBe(5);
> });
> ```

**Q2. Why is the AAA pattern important?**
> It makes tests readable, maintainable, and self-documenting. A developer reading a test immediately knows: what's being set up, what action is being tested, and what the expected outcome is. It enforces single-responsibility in tests (test one thing) and separates concerns clearly.

**Q3. What are the different types of testing?**
> - **Unit testing** — tests individual functions/components in isolation
> - **Integration testing** — tests how multiple units work together
> - **End-to-end (E2E) testing** — tests full user flows through the application
> - **Snapshot testing** — captures output and alerts when it changes
> - **Performance testing** — measures speed and resource usage
> Jest primarily handles unit and integration testing.

---

## 63. Jest Expect

**Q1. What are the most commonly used Jest matchers?**
> ```js
> expect(value).toBe(5);              // strict equality (===)
> expect(value).toEqual({a: 1});      // deep equality (objects/arrays)
> expect(value).toBeTruthy();
> expect(value).toBeFalsy();
> expect(value).toBeNull();
> expect(value).toBeUndefined();
> expect(value).toContain("item");    // array/string contains
> expect(value).toHaveLength(3);
> expect(fn).toThrow("Error message");
> expect(value).toBeGreaterThan(5);
> ```

**Q2. What is the difference between `toBe` and `toEqual`?**
> `toBe` uses `Object.is` (similar to `===`) — it checks reference equality. Two different objects with the same content will NOT pass `toBe`.
> `toEqual` recursively checks that objects have the same structure and values — it compares content, not reference.
> ```js
> expect({a: 1}).toBe({a: 1});    // ❌ fails — different objects
> expect({a: 1}).toEqual({a: 1}); // ✅ passes — same content
> ```

**Q3. How do you test that a function throws an error in Jest?**
> Wrap the call in a function when using `toThrow`:
> ```js
> expect(() => divide(10, 0)).toThrow();
> expect(() => divide(10, 0)).toThrow("Cannot divide by zero");
> expect(() => divide(10, 0)).toThrow(RangeError);
>
> // For async functions
> await expect(asyncFn()).rejects.toThrow("Error message");
> ```
> You must pass a function to `expect`, not the direct call — otherwise the error throws before Jest can catch it.

---

## 64. Mock Functions

**Q1. What is a mock function in Jest and why do you use them?**
> A mock function replaces a real function with a controlled fake version that:
> - Tracks how many times it was called
> - Records what arguments it received
> - Returns a specified value
>
> Use mocks to isolate the code under test from external dependencies (APIs, databases, timers).
> ```js
> const mockFn = jest.fn();
> mockFn("hello");
> expect(mockFn).toHaveBeenCalledWith("hello");
> expect(mockFn).toHaveBeenCalledTimes(1);
> ```

**Q2. How do you make a mock function return a specific value?**
> ```js
> const mockFn = jest.fn();
>
> mockFn.mockReturnValue(42);          // always returns 42
> mockFn.mockReturnValueOnce(42);      // returns 42 once, then undefined
>
> mockFn.mockResolvedValue({ id: 1 }); // returns resolved promise
> mockFn.mockRejectedValue(new Error("fail")); // returns rejected promise
>
> mockFn.mockImplementation((x) => x * 2); // custom implementation
> ```

**Q3. How do you mock an entire module in Jest?**
> ```js
> // At top of test file
> jest.mock("./api");           // auto-mocks all exports
>
> // Manual mock
> jest.mock("./api", () => ({
>   fetchUser: jest.fn().mockResolvedValue({ name: "Alice" })
> }));
>
> // In test
> const { fetchUser } = require("./api");
> expect(fetchUser).toHaveBeenCalled();
>
> // Restore mocks between tests
> afterEach(() => jest.clearAllMocks());
> ```

---

## 65. Code Coverage

**Q1. What is code coverage and what metrics does it measure?**
> Code coverage measures what percentage of your code is executed during tests:
> - **Statement coverage** — % of statements executed
> - **Branch coverage** — % of branches (if/else paths) taken
> - **Function coverage** — % of functions called
> - **Line coverage** — % of lines executed
>
> Run with: `jest --coverage`. Results appear in terminal and `coverage/` folder as HTML report.

**Q2. What does 100% code coverage mean? Is it sufficient?**
> 100% coverage means every line/branch was executed by at least one test. But it does NOT mean your code is bug-free or that tests are meaningful. You can reach 100% coverage with poor assertions. Coverage is a tool to find untested code — high coverage is necessary but not sufficient for quality software.

**Q3. How do you configure coverage thresholds in Jest?**
> In `jest.config.js` or `package.json`:
> ```json
> {
>   "jest": {
>     "coverageThreshold": {
>       "global": {
>         "statements": 80,
>         "branches": 75,
>         "functions": 80,
>         "lines": 80
>       }
>     }
>   }
> }
> ```
> Jest will fail the test run if coverage falls below these thresholds, enforcing quality gates in CI/CD.

---

## 66. Jest Introduction

**Q1. What is Jest and what are its key features?**
> Jest is a JavaScript testing framework developed by Meta (Facebook). Key features:
> - Zero configuration for most projects
> - Built-in test runner, assertion library, and mocking
> - Snapshot testing
> - Code coverage reporting
> - Watch mode for re-running on file changes
> - Works with Node.js, React, Angular, Vue, and more

**Q2. How do you structure a Jest test file?**
> ```js
> // user.test.js
> describe("User module", () => {      // group related tests
>   beforeAll(() => { /* runs once before all tests in block */ });
>   afterAll(() => { /* runs once after all tests */ });
>   beforeEach(() => { /* runs before each test */ });
>   afterEach(() => { /* runs after each test */ });
>
>   test("should create user", () => {  // individual test
>     expect(createUser("Alice")).toHaveProperty("name", "Alice");
>   });
>
>   it("should validate email", () => { // 'it' is alias for 'test'
>     expect(isValidEmail("bad")).toBe(false);
>   });
> });
> ```

**Q3. How do you run Jest tests and common CLI options?**
> ```bash
> npx jest                     # run all tests
> npx jest user.test.js        # run specific file
> npx jest --watch             # watch mode
> npx jest --coverage          # with coverage report
> npx jest -t "should create"  # run tests matching name
> npx jest --verbose           # show individual test names
> ```
> Add to `package.json`: `"scripts": { "test": "jest", "test:watch": "jest --watch" }`

---

*End of Week 6 Interview Questions & Study Notes*
*Topics covered: 66 | Questions: 198*
