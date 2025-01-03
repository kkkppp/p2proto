<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>
        <c:choose>
            <c:when test="${not empty pageTitle}">${pageTitle}</c:when>
            <c:otherwise>Home</c:otherwise>
        </c:choose>
    </title>
    <style>
        /* Reset default browser styles */
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        /* Keycloak Color Scheme */
        :root {
            --keycloak-primary: #007bff; /* Blue */
            --keycloak-secondary: #6c757d; /* Gray */
            --keycloak-background: #f8f9fa; /* Light Gray */
            --keycloak-text: #ffffff; /* White */
            --keycloak-hover: #0056b3; /* Darker Blue */
            --keycloak-content-bg: #ffffff; /* White for content area */
            --keycloak-content-text: #212529; /* Dark text */
            --keycloak-active-link: #0056b3; /* Active link color */
            --keycloak-error: #dc3545; /* Red for errors */
        }

        body {
            font-family: Arial, sans-serif;
            display: flex;
            height: 100vh;
            background-color: var(--keycloak-background);
            color: var(--keycloak-content-text);
            margin: 0;
        }

        /* Left Sidebar */
        .sidebar {
            width: 250px;
            background-color: var(--keycloak-primary);
            display: flex;
            flex-direction: column;
            padding-top: 20px;
            position: fixed;
            height: 100%;
            overflow: auto;
        }

        .sidebar h1 {
            text-align: center;
            margin-bottom: 30px;
            font-size: 1.5em;
            color: var(--keycloak-text);
        }

        .sidebar ul {
            list-style-type: none;
        }

        .sidebar ul li {
            position: relative;
        }

        /* Top-level menu items */
        .sidebar ul li > a {
            display: block;
            padding: 15px 20px;
            color: var(--keycloak-text);
            text-decoration: none;
            transition: background-color 0.3s;
            cursor: pointer;
        }

        .sidebar ul li > a:hover, .sidebar ul li > a.active-link {
            background-color: var(--keycloak-hover);
        }

        /* Submenu container */
        .sidebar ul li ul {
            list-style-type: none;
            display: none; /* Hidden by default */
            background-color: #0056b3;
        }

        /* Submenu items */
        .sidebar ul li ul li a {
            padding-left: 40px; /* Indent submenu items */
            background-color: #0056b3;
            color: var(--keycloak-text);
            text-decoration: none;
            transition: background-color 0.3s;
            cursor: pointer;
        }

        .sidebar ul li ul li a:hover, .sidebar ul li ul li a.active-link {
            background-color: var(--keycloak-hover);
        }

        /* Show submenu when parent has 'open' class */
        .sidebar ul li.open > ul {
            display: block;
        }

        /* Submenu toggle indicator */
        .sidebar ul li > a.has-submenu::after {
            content: "\25B6"; /* Right-pointing arrow */
            float: right;
            transition: transform 0.3s;
        }

        .sidebar ul li.open > a.has-submenu::after {
            transform: rotate(90deg); /* Down-pointing arrow */
        }

        /* Main Content Area */
        .main-content {
            margin-left: 250px; /* Same as sidebar width */
            padding: 20px;
            width: calc(100% - 250px);
            height: 100vh;
            overflow-y: auto;
            background-color: var(--keycloak-content-bg);
            color: var(--keycloak-content-text);
        }

        .welcome {
            margin-bottom: 20px;
            font-size: 1.8em;
            color: var(--keycloak-content-text);
        }

        /* Loading Indicator */
        .loading {
            font-size: 1.2em;
            color: var(--keycloak-primary);
        }

        /* Error Message */
        .error-message {
            font-size: 1.2em;
            color: var(--keycloak-error);
            font-weight: bold;
        }

        /* Responsive Design */
        @media (max-width: 768px) {
            .sidebar {
                width: 200px;
            }

            .main-content {
                margin-left: 200px;
                width: calc(100% - 200px);
            }
        }

        @media (max-width: 480px) {
            .sidebar {
                width: 100%;
                height: auto;
                position: relative;
            }

            .main-content {
                margin-left: 0;
                width: 100%;
            }

            .sidebar ul li {
                display: block;
            }

            .sidebar ul li > a {
                padding: 10px;
            }

            .sidebar ul li ul li a {
                padding-left: 30px;
            }
        }
    </style>
    <script>
        // Object to cache loaded content
        const contentCache = {};

        // Function to toggle submenu visibility
        function toggleSubmenu(event, link) {
            const parentLi = link.parentElement;
            const submenu = parentLi.querySelector('ul');

            if (submenu) {
                event.preventDefault(); // Prevent default link behavior
                parentLi.classList.toggle('open');
            }
        }

        // Function to load content via AJAX
        function loadContent(event, link) {
            event.preventDefault(); // Prevent default link behavior

            const url = link.getAttribute('data-url');
            const contentArea = document.getElementById('contentArea');

            // Remove active class from all links that have data-url
            const allLinks = document.querySelectorAll('.sidebar ul li a[data-url]');
            allLinks.forEach(l => l.classList.remove('active-link'));

            // Add active class to the clicked link
            link.classList.add('active-link');

            // If content is cached, use it
            if (contentCache[url] && url !== '#') { // Avoid loading '#' as content
                contentArea.innerHTML = contentCache[url];
                return;
            }

            // Show loading indicator
            contentArea.innerHTML = '<p class="loading">Loading...</p>';

            // Fetch the content
            fetch(url, {
                method: 'GET',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest' // To identify AJAX requests server-side
                },
                credentials: 'same-origin' // Include cookies for session handling
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Network response was not ok (${response.status})`);
                    }
                    return response.text();
                })
                .then(html => {
                    // Cache the content if URL is valid
                    if (url !== '#') {
                        contentCache[url] = html;
                    }
                    // Inject the fetched HTML into the content area
                    contentArea.innerHTML = html;
                })
                .catch(error => {
                    console.error('Error fetching content:', error);
                    contentArea.innerHTML = '<p class="error-message">Error loading content. Please try again later.</p>';
                });
        }

        // Function to handle form submissions via AJAX
        function submitForm(event, form) {
            event.preventDefault(); // Prevent default form submission

            const url = form.getAttribute('action');
            const method = form.getAttribute('method');
            const formData = new FormData(form);

            const contentArea = document.getElementById('contentArea');

            // Show loading indicator
            contentArea.innerHTML = '<p class="loading">Submitting...</p>';

            // Convert FormData to URL-encoded string
            const formBody = new URLSearchParams();
            for (const pair of formData.entries()) {
                formBody.append(pair[0], pair[1]);
            }

            fetch(url, {
                method: method.toUpperCase(),
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: formBody.toString(),
                credentials: 'same-origin' // Include cookies for session handling
            })
                .then(response => {
                    if (response.redirected) {
                        // Handle redirection if needed
                        window.location.href = response.url;
                        return;
                    }
                    if (!response.ok) {
                        throw new Error(`Network response was not ok (${response.status})`);
                    }
                    return response.text();
                })
                .then(html => {
                    // Inject the fetched HTML into the content area
                    contentArea.innerHTML = html;

                    // Optionally, update the page title
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const newTitle = doc.querySelector('h2') ? doc.querySelector('h2').innerText : 'Home';
                    document.title = newTitle;
                })
                .catch(error => {
                    console.error('Error submitting form:', error);
                    contentArea.innerHTML = '<p class="error-message">Error submitting form. Please try again later.</p>';
                });
        }

        // Initialize the first content load on page load
        window.onload = function() {
            const firstLink = document.querySelector('.sidebar ul li a[data-url]');
            if (firstLink && firstLink.getAttribute('data-url') !== '#') {
                firstLink.classList.add('active-link');
                loadContent(new Event('click'), firstLink);
            }

            // Attach event listeners to sidebar links and forms
            document.body.addEventListener('click', function(e) {
                if (e.target.tagName === 'A' && e.target.closest('.sidebar')) {
                    const link = e.target;
                    const parentLi = link.parentElement;
                    const submenu = parentLi.querySelector('ul');

                    if (submenu) {
                        // If the link has a submenu, toggle it
                        toggleSubmenu(e, link);
                    } else {
                        // Otherwise, load the content via AJAX
                        loadContent(e, link);
                    }
                }
            });

            document.body.addEventListener('submit', function(e) {
                if (e.target.tagName === 'FORM') {
                    submitForm(e, e.target);
                }
            });
        };
    </script>
</head>
<body>
<!-- Left Sidebar -->
<div class="sidebar">
    <h1>Platform 2</h1>
    <ul>
        <c:forEach var="menuItem" items="${menu}">
            <li class="${menuItem.title == activeMenu ? 'open' : ''}">
                <!-- Parent Menu Item -->
                <a href="#"
                   class="${not empty menuItem.children ? 'has-submenu' : ''}">
                        ${menuItem.title}
                </a>
                <c:if test="${not empty menuItem.children}">
                    <ul>
                        <c:forEach var="subMenu" items="${menuItem.children}">
                            <li>
                                <!-- Submenu Item -->
                                <a href="${pageContext.request.contextPath}${subMenu.url}"
                                   data-url="${pageContext.request.contextPath}${subMenu.url}">
                                        ${subMenu.title}
                                </a>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </li>
        </c:forEach>
    </ul>
</div>

<!-- Main Content Area -->
<div class="main-content">
    <h1 class="welcome">Welcome, ${username}!</h1>
    <div id="contentArea">
        <!-- Dynamic content will be loaded here -->
        <p class="loading">Loading...</p>
    </div>
</div>
</body>
</html>
