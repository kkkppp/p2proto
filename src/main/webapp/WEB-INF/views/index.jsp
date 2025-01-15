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
    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">

    <!-- jQuery and Bootstrap JS -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>

    <!-- Link to External CSS -->
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/css/styles.css">
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
<!-- Include Sidebar -->
<jsp:include page="/WEB-INF/views/includes/sidebar.jsp" />

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