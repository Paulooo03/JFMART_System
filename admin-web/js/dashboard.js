document.addEventListener("DOMContentLoaded", () => {
    const logoutBtn = document.getElementById("logout-btn");
    if (logoutBtn) logoutBtn.addEventListener("click", logout);

    displayEmptyForecast();
});

function displayEmptyForecast() {
    const forecastPanel = document.querySelector(".forecast-panel");
    if (!forecastPanel) return;

    // Display empty state layout matching dark theme
    forecastPanel.innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; min-height: 280px; text-align: center;">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom: 12px; opacity: 0.5;">
                <line x1="18" y1="20" x2="18" y2="10"></line>
                <line x1="12" y1="20" x2="12" y2="4"></line>
                <line x1="6" y1="20" x2="6" y2="14"></line>
            </svg>
            <p style="font-size: 16px; font-weight: 500; color: #94A3B8; margin-bottom: 4px;">No Forecast Data Available</p>
            <p style="font-size: 13px; color: #64748B;">Prediction logs will appear here once generated.</p>
        </div>
        <p class="panel-caption">Product-Demand Forecast</p>
    `;
}