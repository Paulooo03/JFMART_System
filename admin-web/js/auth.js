"use strict";

document.addEventListener("DOMContentLoaded", () => {
    // Already signed in: skip the form. An expired token is cleared by the first API call.
    if (JF.session.token) {
        window.location.href = "dashboard.html";
        return;
    }

    const form = document.getElementById("login-form");
    const submit = document.getElementById("login-button");
    const errorBox = document.getElementById("login-error");

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;
        if (!username || !password) {
            errorBox.textContent = "Enter your username and password.";
            return;
        }

        await UI.runAction(submit, errorBox, async () => {
            const result = await JF.api.post("/auth/login", { username, password });
            if (result.role !== "ROLE_ADMIN") {
                throw new JF.ApiError("This site is for the JF MiniMart admin account only.", 403);
            }
            JF.session.set(result.token);
            window.location.href = "dashboard.html";
        });
    });
});
