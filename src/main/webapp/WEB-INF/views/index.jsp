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
    <script>
        // Show a loading indicator on submit
        function showLoading() {
            document.getElementById('loadingIndicator').style.display = 'block';
        }

        // Validate the password and confirmation match before form submission
        function validatePasswords() {
            const passwordFields = document.querySelectorAll('input[type="password"]');
            // If at least two password fields exist, check they match
            if (passwordFields.length >= 2) {
                const pwd = passwordFields[0].value;
                const confirmPwd = passwordFields[1].value;

                // Optional logic to skip if left '********' in edit mode (meaning no change)
                if ((pwd !== '********' && pwd !== '') || confirmPwd !== '') {
                    if (pwd !== confirmPwd) {
                        document.getElementById('loadingIndicator').style.display = 'none';
                        alert("Passwords do not match!");
                        return false; // Prevent form submission
                    }
                }
            }
            return true;
        }

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
            const method = form.getAttribute('method').toUpperCase();
            const formData = new FormData(form);

            // Extract CSRF token if present
            const csrfTokenInput = form.querySelector('input[name="_csrf"]');
            let csrfToken = '';
            if (csrfTokenInput) {
                csrfToken = csrfTokenInput.value;
            }

            const contentArea = document.getElementById('contentArea');

            // Show loading indicator
            contentArea.innerHTML = '<p class="loading">Submitting...</p>';

            // Convert FormData to URL-encoded string
            const formBody = new URLSearchParams();
            for (const pair of formData.entries()) {
                formBody.append(pair[0], pair[1]);
            }

            fetch(url, {
                method: method,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-CSRF-TOKEN': csrfToken // Include CSRF token in headers if needed
                },
                body: formBody.toString(),
                credentials: 'same-origin' // Include cookies for session handling
            })
                .then(response => response.json())
                .then(data => {
                    if (data.status === 'success') {
                        // Fetch and load the updated list of records
                        loadContentFromUrl(data.redirectUrl);
                        if (form.getAttribute('action').includes('/tableSetup/save')) {
                            reloadSidebar();
                        }
                    } else if (data.status === 'error') {
                        // Display error message(s)
                        contentArea.innerHTML = '<div class="alert alert-danger">'+data.message+'</div>';
                    }
                })
                .catch(error => {
                    console.error('Error submitting form:', error);
                    contentArea.innerHTML = '<p class="error-message">Error submitting form. Please try again later.</p>';
                });
        }

        // Function to load content from a given URL into contentArea
        function loadContentFromUrl(url) {
            const contentArea = document.getElementById('contentArea');

            // Show loading indicator
            contentArea.innerHTML = '<p class="loading">Loading...</p>';

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
                    // Inject the fetched HTML into the content area
                    contentArea.innerHTML = html;

                    // Optionally, update the page title
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const newTitle = doc.querySelector('h2') ? doc.querySelector('h2').innerText : 'Home';
                    document.title = newTitle;
                })
                .catch(error => {
                    console.error('Error loading content:', error);
                    contentArea.innerHTML = '<p class="error-message">Error loading content. Please try again later.</p>';
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
                const link = e.target.closest('a');

                if (!link) return;

                // If logout link is clicked, allow normal navigation (no AJAX)
                if (link.href.includes('/logout')) {
                    return; // Let browser handle the logout link normally
                }

                // AJAX-based navigation for sidebar links
                if (link.closest('.sidebar')) {
                    const parentLi = link.parentElement;
                    const submenu = parentLi.querySelector('ul');

                    if (submenu) {
                        toggleSubmenu(e, link);
                    } else {
                        loadContent(e, link);
                    }
                }
            });

            document.body.addEventListener('submit', function(e) {
                if (e.target.tagName === 'FORM' && !e.defaultPrevented) {
                    submitForm(e, e.target);
                }
            });
        };
    </script>
    <script>
        var csrfToken = "${_csrf.token}";
        function deleteRecord(tableName, id) {
            if (!confirm('Are you sure you want to delete this record?')) {
                return;
            }

            fetch("${pageContext.request.contextPath}/table/" + tableName + "/delete/" + id, {
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': csrfToken
                }
            })
                .then(response => {
                    if (!response.ok) throw new Error('Network response was not ok');
                    return response.json();
                })
                .then(data => {
                    if (data.status === 'success') {
                        loadContentFromUrl(data.redirectUrl);
                    } else {
                        alert(data.message);
                    }
                })
                .catch(error => console.error('Delete error:', error));
        }
    </script>
    <script>
        function reloadSidebar() {
            const sidebarContainer = document.getElementById('sidebarContainer');

            fetch('${pageContext.request.contextPath}/ajax/sidebar', {
                method: 'GET',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                },
                credentials: 'same-origin'
            })
                .then(response => {
                    if (!response.ok) throw new Error('Failed to reload sidebar');
                    return response.text();
                })
                .then(html => {
                    sidebarContainer.innerHTML = html;
                    console.log("Sidebar reloaded successfully");
                })
                .catch(error => {
                    console.error('Error reloading sidebar:', error);
                    sidebarContainer.innerHTML = '<div class="alert alert-danger">Failed to reload sidebar.</div>';
                });
        }
    </script>
</head>
<body>
<!-- Include Sidebar -->
<div id="sidebarContainer">
    <jsp:include page="/WEB-INF/views/includes/sidebar.jsp" />
</div>

<!-- Main Content Area -->
<div class="main-content">
    <div id="contentArea">
        <!-- Dynamic content will be loaded here -->
        <p class="loading">Loading...</p>
    </div>
</div>
</body>
</html>
