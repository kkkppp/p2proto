<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Home</title>
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
            margin: 10px 0;
        }

        .sidebar ul li a {
            display: block;
            padding: 15px 20px;
            color: var(--keycloak-text);
            text-decoration: none;
            transition: background-color 0.3s;
        }

        .sidebar ul li a:hover {
            background-color: var(--keycloak-hover);
        }

        .sidebar ul li a.active-link {
            background-color: var(--keycloak-active-link);
            color: var(--keycloak-text);
        }

        /* Main Content Area */
        .main-content {
            margin-left: 250px; /* Same as sidebar width */
            padding: 20px;
            width: calc(100% - 250px);
            height: 100vh;
            overflow-y: auto;
            background-color: var(--keycloak-content-bg);
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
                display: inline;
            }

            .sidebar ul li a {
                padding: 10px;
            }
        }
    </style>
</head>
<body>
<!-- Left Sidebar -->
<div class="sidebar">
    <h1>Keycloak</h1>
    <ul>
        <li>
            <a href="${pageContext.request.contextPath}/users" data-url="${pageContext.request.contextPath}/users" onclick="loadContent(event, this)">Manage Users</a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/groups" data-url="${pageContext.request.contextPath}/groups" onclick="loadContent(event, this)">Manage Groups</a>
        </li>
        <li>
            <a href="${pageContext.request.contextPath}/roles" data-url="${pageContext.request.contextPath}/roles" onclick="loadContent(event, this)">Manage Roles</a>
        </li>
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

<!-- JavaScript for AJAX Content Loading and Active Link Highlighting -->
<script>
    // Object to cache loaded content
    const contentCache = {};

    // Function to load content via AJAX
    function loadContent(event, link) {
        event.preventDefault(); // Prevent default link behavior

        const url = link.getAttribute('data-url');
        const contentArea = document.getElementById('contentArea');

        // Remove active class from all links
        const links = document.querySelectorAll('.sidebar ul li a');
        links.forEach(l => l.classList.remove('active-link'));

        // Add active class to the clicked link
        link.classList.add('active-link');

        // If content is cached, use it
        if (contentCache[url]) {
            contentArea.innerHTML = contentCache[url];
            return;
        }

        // Show loading indicator
        contentArea.innerHTML = '<p class="loading">Loading...</p>';

        // Fetch the content
        fetch(url, {
            method: 'GET',
            headers: {
                'X-Requested-With': 'XMLHttpRequest' // Optional: to identify AJAX requests server-side
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
                // Cache the content
                contentCache[url] = html;
                // Inject the fetched HTML into the content area
                contentArea.innerHTML = html;
            })
            .catch(error => {
                console.error('Error fetching content:', error);
                contentArea.innerHTML = '<p class="error-message">Error loading content. Please try again later.</p>';
            });
    }

    // Set the first link as active and load its content on page load
    window.onload = function() {
        const firstLink = document.querySelector('.sidebar ul li a');
        if (firstLink) {
            firstLink.classList.add('active-link');
            loadContent(new Event('click'), firstLink);
        }
    };
</script>
</body>
</html>
