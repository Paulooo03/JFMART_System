document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form");
    const passwordInput = document.getElementById("password");
    const errorDiv = document.getElementById("login-error");

    if (localStorage.getItem("jwt_token")) {
        window.location.href = "dashboard.html";
        return;
    }

    loginForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const password = passwordInput.value.trim();

        if (!password) {
            errorDiv.textContent = "Please enter a password.";
            return;
        }

        // Accepts ANY password for UI/UX demo purposes
        localStorage.setItem("jwt_token", "demo_mock_token");
        window.location.href = "dashboard.html";
    });
});