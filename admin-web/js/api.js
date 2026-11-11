const API_BASE_URL = "http://localhost:8080/api";

/**
 * Universal wrapper for API calls with JWT header injection
 */
async function fetchWithAuth(endpoint, options = {}) {
    const token = localStorage.getItem("jwt_token");

    // Redirect to login if token is missing (except on login page)
    if (!token && !window.location.pathname.endsWith("index.html") && window.location.pathname !== "/admin-web/") {
        window.location.href = "index.html";
        return null;
    }

    const headers = {
        "Content-Type": "application/json",
        ...(token && { "Authorization": `Bearer ${token}` }),
        ...options.headers
    };

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers
        });

        // Handle unauthorized or expired session
        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem("jwt_token");
            window.location.href = "index.html";
            return null;
        }

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP error! Status: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error("API Call Error:", error);
        throw error;
    }
}

/**
 * Blob downloader helper for Excel exports (FR-11)
 */
async function downloadExcelFile(endpoint, filename) {
    const token = localStorage.getItem("jwt_token");
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (!response.ok) throw new Error("Export download failed.");

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
    } catch (err) {
        alert("Failed to export Excel file: " + err.message);
    }
}

/**
 * Shared logout handler
 */
function logout() {
    localStorage.removeItem("jwt_token");
    window.location.href = "index.html";
}