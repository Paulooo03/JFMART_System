"use strict";

document.addEventListener("DOMContentLoaded", async () => {
    if (!JF.requireLogin()) return;
    document.getElementById("logout-btn").addEventListener("click", JF.logout);

    const summaryBox = document.getElementById("summary");
    const errorBox = document.getElementById("page-error");
    const { h, money } = UI;

    function card(href, value, label, alert) {
        return h("a", { class: "card-stat" + (alert ? " alert" : ""), href }, h("div", { class: "value", text: value }), h("div", { class: "label", text: label }));
    }

    try {
        const s = await JF.api.get("/admin/dashboard");
        summaryBox.append(
            card("clients.html", s.pendingApprovals, "Clients waiting for approval", s.pendingApprovals > 0),
            card("orders.html?status=PENDING", s.pendingOrders, "New orders to review", s.pendingOrders > 0),
            card("orders.html?status=CONFIRMED", s.awaitingDelivery, "Confirmed, need a delivery date", s.awaitingDelivery > 0),
            card("deliveries.html", s.deliveriesToday, "Deliveries scheduled today", false),
            card("invoices.html?overdue=true", money(s.overdueBalance), "Overdue (" + s.overdueClients + " client" + (s.overdueClients === 1 ? "" : "s") + ")", s.overdueClients > 0),
            card("invoices.html", money(s.totalOutstanding), "Total outstanding", false)
        );
    } catch (error) {
        UI.showError(errorBox, error);
    }
});
