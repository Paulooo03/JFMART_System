"use strict";

document.addEventListener("DOMContentLoaded", () => {
    if (!JF.requireLogin()) return;
    const { h, money, statusBadge } = UI;

    const errorBox = document.getElementById("page-error");
    const select = document.getElementById("client-select");
    const exportButton = document.getElementById("export-btn");
    const summaryBox = document.getElementById("summary");
    const historyBox = document.getElementById("order-history-view");
    const transactionsBox = document.getElementById("client-transactions-view");

    function stat(value, label, alert) {
        return h("div", { class: "card-stat" + (alert ? " alert" : "") }, h("div", { class: "value", text: value }), h("div", { class: "label", text: label }));
    }

    async function loadClients() {
        const clients = await JF.api.get("/admin/clients");
        select.append(h("option", { value: "", text: "Choose a client..." }));
        clients
            .filter((c) => c.accountStatus !== "PENDING_APPROVAL")
            .forEach((c) => select.append(h("option", { value: String(c.clientId), text: c.hospitalName })));
        const preset = UI.queryParam("clientId");
        if (preset) {
            select.value = preset;
            await loadRecords();
        }
    }

    async function loadRecords() {
        const clientId = select.value;
        exportButton.disabled = !clientId;
        UI.clear(summaryBox);
        if (!clientId) {
            UI.clear(historyBox).append(h("p", { class: "muted", text: "Choose a client to see their orders." }));
            UI.clear(transactionsBox).append(h("p", { class: "muted", text: "Choose a client to see their invoices and payments." }));
            return;
        }

        try {
            const [statement, orders] = await Promise.all([
                JF.api.get("/admin/clients/" + clientId + "/statement"),
                JF.api.get("/admin/orders?clientId=" + clientId),
            ]);
            errorBox.textContent = "";

            summaryBox.append(
                stat(money(statement.totalInvoiced), "Total invoiced"),
                stat(money(statement.totalPaid), "Total paid"),
                stat(money(statement.outstandingBalance), "Outstanding balance"),
                stat(money(statement.overdueBalance), "Overdue", Number(statement.overdueBalance) > 0)
            );

            UI.clear(historyBox);
            if (orders.length === 0) historyBox.append(h("p", { class: "muted", text: "No orders yet." }));
            orders.forEach((order) =>
                historyBox.append(
                    h(
                        "div",
                        { class: "row" },
                        h("div", { class: "title" }, order.referenceNumber + " ", statusBadge(order.status)),
                        h("div", { class: "muted", text: "Ordered " + UI.fmtDate(order.createdAt) + (order.deliveredAt ? " · Delivered " + UI.fmtDate(order.deliveredAt) : "") }),
                        h("div", { text: order.items.length + " item(s) · " + money(order.totalAmount) })
                    )
                )
            );

            UI.clear(transactionsBox);
            if (statement.invoices.length === 0) transactionsBox.append(h("p", { class: "muted", text: "No invoices yet." }));
            statement.invoices.forEach((invoice) =>
                transactionsBox.append(
                    h(
                        "div",
                        { class: "row" },
                        h("div", { class: "title" }, (invoice.salesInvoiceNumber ? "Invoice " + invoice.salesInvoiceNumber : "No sales invoice no. yet") + " ", statusBadge(invoice.paymentStatus)),
                        h("div", { class: "muted", text: "Order " + (invoice.orderReference || "") + " · Invoiced " + UI.fmtDate(invoice.startDate) + " · Due " + UI.fmtDate(invoice.dueDate) }),
                        h("div", { text: "Amount " + money(invoice.amount) + " · Paid " + money(invoice.amountPaid) + " · Balance " + money(invoice.balance) }),
                        ...invoice.payments.map((p) =>
                            h("div", { class: "muted", text: "Paid " + money(p.amountPaid) + " on " + UI.fmtDate(p.paymentDate) + " (" + p.paymentMethod + (p.referenceNumber ? ", ref. " + p.referenceNumber : "") + ")" })
                        )
                    )
                )
            );
        } catch (error) {
            UI.showError(errorBox, error);
        }
    }

    select.addEventListener("change", loadRecords);
    exportButton.addEventListener("click", () =>
        UI.runAction(exportButton, errorBox, async () => {
            await JF.download("/admin/clients/" + select.value + "/export", "client-records.xlsx");
            UI.toast("The Excel file was downloaded.");
        })
    );

    loadClients().catch((error) => UI.showError(errorBox, error));
});
