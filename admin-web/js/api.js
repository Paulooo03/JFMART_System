"use strict";

/**
 * Talks to the JF MiniMart server. The pages are served by the same server, so every call goes to a
 * relative /api address and no cross-origin settings are needed.
 * The login token lives in sessionStorage: it disappears when the tab is closed.
 */
(function (global) {
    const TOKEN_KEY = "jfmart_admin_token";

    const session = {
        get token() {
            try {
                return sessionStorage.getItem(TOKEN_KEY);
            } catch (e) {
                return null;
            }
        },
        set(token) {
            sessionStorage.setItem(TOKEN_KEY, token);
        },
        clear() {
            try {
                sessionStorage.removeItem(TOKEN_KEY);
            } catch (e) {
                /* storage unavailable: nothing to clear */
            }
        },
    };

    class ApiError extends Error {
        constructor(message, status) {
            super(message);
            this.status = status;
        }
    }

    function goToLogin() {
        window.location.href = "index.html";
    }

    /** Signed out, or the token expired: go back to the login page. */
    function endSession() {
        session.clear();
        goToLogin();
        return new ApiError("Your session has expired. Please sign in again.", 401);
    }

    async function request(path, options = {}) {
        const headers = { Accept: "application/json" };
        const hasBody = options.body !== undefined;
        if (hasBody) headers["Content-Type"] = "application/json";
        const token = session.token;
        if (token) headers.Authorization = "Bearer " + token;

        let response;
        try {
            response = await fetch("/api" + path, {
                method: options.method || "GET",
                headers,
                body: hasBody ? JSON.stringify(options.body) : undefined,
            });
        } catch (e) {
            throw new ApiError("Cannot reach the server. Is it running?", 0);
        }

        if (response.status === 401 && token) throw endSession();

        const text = await response.text();
        let data = null;
        if (text) {
            try {
                data = JSON.parse(text);
            } catch (e) {
                data = null;
            }
        }
        if (!response.ok) {
            throw new ApiError((data && data.message) || "Request failed (" + response.status + ")", response.status);
        }
        return data;
    }

    const api = {
        get: (path) => request(path),
        post: (path, body) => request(path, { method: "POST", body }),
        put: (path, body) => request(path, { method: "PUT", body }),
        del: (path) => request(path, { method: "DELETE" }),
    };

    /** Downloads a file (such as the Excel export) with the login token attached. */
    async function download(path, fallbackName) {
        let response;
        try {
            response = await fetch("/api" + path, { headers: { Authorization: "Bearer " + session.token } });
        } catch (e) {
            throw new ApiError("Cannot reach the server. Is it running?", 0);
        }
        if (response.status === 401) throw endSession();
        if (!response.ok) throw new ApiError("The download failed (" + response.status + ").", response.status);

        const disposition = response.headers.get("Content-Disposition") || "";
        const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
        const name = match ? decodeURIComponent(match[1]) : fallbackName;

        const url = URL.createObjectURL(await response.blob());
        const link = document.createElement("a");
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }

    /** Call at the top of every page except the login page. */
    function requireLogin() {
        if (!session.token) {
            goToLogin();
            return false;
        }
        return true;
    }

    function logout() {
        session.clear();
        goToLogin();
    }

    global.JF = { api, ApiError, session, download, requireLogin, logout };
})(window);
