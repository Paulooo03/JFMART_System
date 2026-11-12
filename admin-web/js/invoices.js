"use strict";

document.addEventListener("DOMContentLoaded", () => {
    if (!JF.requireLogin()) return;
    const { h, money, badge, statusBadge, button, field } = UI;

    const METHODS = ["Cash", "Bank transfer", "Check", "GCash", "Other"];
    const errorBox = document.getElementById("page-error");
    const body = document.getElementById("invoices-body");
    const totalBox = document.getElementById("balance-total");
    const clientFilter = document.getElementById("client-filter");
    const statusFilter = document.getElementById("status-filter");
    const overdueFilter = document.getElementById("overdue-filter");

    // Filters can be preset from a link, for example the dashboard's overdue card
    statusFilter.value = UI.queryParam("status") || "";
    overdueFilter.checked = UI.queryParam("overdue") === "true";

    async function loadClients() {
        const clients = await JF.api.get("/admin/clients");
        clientFilter.append(h("option", { value: "", text: "All clients" }));
        clients
            .filter((c) => c.accountStatus !== "PENDING_APPROVAL")
            .forEach((c) => clientFilter.append(h("option", { value: String(c.clientId), text: c.hospitalName })));
        clientFilter.value = UI.queryParam("clientId") || "";
    }

    async function load() {
        const query = new URLSearchParams();
        if (clientFilter.value) query.set("clientId", clientFilter.value);
        if (statusFilter.value) query.set("status", statusFilter.value);
        if (overdueFilter.checked) query.set("overdue", "true");
        try {
            const invoices = await JF.api.get("/admin/invoices" + (query.toString() ? "?" + query : ""));
            errorBox.textContent = "";
            render(invoices);
        } catch (error) {
            UI.showError(errorBox, error);
        }
    }

    function render(invoices) {
        const balance = invoices.reduce((sum, i) => sum + Number(i.balance), 0);
        totalBox.textContent = "Balance shown: " + money(balance);

        UI.clear(body);
        if (invoices.length === 0) {
            body.append(h("tr", null, h("td", { colspan: 10, class: "muted", text: "No invoices match these filters." })));
            return;
        }
        body.append(...invoices.map(row));
    }

    function row(invoice) {
        const owes = Number(invoice.balance) > 0;
        return h(
            "tr",
            null,
            h("td", { text: "#" + invoice.invoiceNumber }),
            h("td", { text: invoice.salesInvoiceNumber || "—" }),
            h("td", null, h("strong", { text: invoice.clientName || "—" }), h("br"), h("small", { class: "muted", text: invoice.orderReference || "" })),
            h("td", { class: "nowrap", text: UI.fmtDate(invoice.startDate) }),
            h("td", { class: "nowrap" + (invoice.overdue ? " danger-text" : ""), text: UI.fmtDate(invoice.dueDate) }),
            h("td", { class: "num", text: money(invoice.amount) }),
            h("td", { class: "num", text: money(invoice.amountPaid) }),
            h("td", { class: "num", text: money(invoice.balance) }),
            h("td", null, h("div", { class: "badge-stack" }, statusBadge(invoice.paymentStatus), invoice.overdue ? badge("Overdue", "danger") : null)),
            h(
                "td",
                null,
                h(
                    "div",
                    { class: "actions" },
                    button(invoice.salesInvoiceNumber ? "Edit no." : "Set no.", () => askSalesNumber(invoice)),
                    owes ? button("Record payment", () => askPayment(invoice), "btn-primary") : null
                )
            )
        );
    }

    function askSalesNumber(invoice) {
        const number = h("input", { class: "input", type: "text", maxlength: 64, value: invoice.salesInvoiceNumber || "" });
        const errorArea = h("div", { class: "error-message left" });
        const save = button("Save", async () => {
            if (!number.value.trim()) {
                errorArea.textContent = "Enter the sales invoice number.";
                return;
            }
            await UI.runAction(save, errorArea, async () => {
                await JF.api.put("/admin/invoices/" + invoice.invoiceNumber + "/sales-invoice-number", { salesInvoiceNumber: number.value.trim() });
                modal.close();
                UI.toast("Sales invoice number saved.");
                await load();
            });
        }, "btn-primary");
        const modal = UI.openModal({
            title: "Sales invoice number for " + (invoice.orderReference || "#" + invoice.invoiceNumber),
            narrow: true,
            body: h("div", null, field("Sales invoice number (from your paper invoice)", number), errorArea),
            actions: [button("Cancel", () => modal.close()), save],
        });
    }

    function askPayment(invoice) {
        const amount = h("input", { class: "input", type: "text", inputmode: "decimal", value: String(invoice.balance) });
        const method = h("select", { class: "input" }, METHODS.map((m) => h("option", { value: m, text: m })));
        const date = h("input", { class: "input", type: "date", max: UI.today(), value: UI.today() });
        const reference = h("input", { class: "input", type: "text", maxlength: 100, placeholder: "Optional" });
        const errorArea = h("div", { class: "error-message left" });

        const save = button("Record payment", async () => {
            const text = amount.value.trim();
            if (!/^\d+(\.\d{1,2})?$/.test(text) || Number(text) <= 0) {
                errorArea.textContent = "Enter the amount as a number above zero with at most two decimals.";
                return;
            }
            if (Number(text) > Number(invoice.balance)) {
                errorArea.textContent = "That is more than the balance of " + money(invoice.balance) + ".";
                return;
            }
            if (!date.value) {
                errorArea.textContent = "Pick the payment date.";
                return;
            }
            await UI.runAction(save, errorArea, async () => {
                await JF.api.post("/admin/invoices/" + invoice.invoiceNumber + "/payments", {
                    amountPaid: Number(text),
                    paymentMethod: method.value,
                    paymentDate: date.value,
                    referenceNumber: reference.value.trim() || null,
                });
                modal.close();
                UI.toast("Payment recorded.");
                await load();
            });
        }, "btn-primary");

        const earlier = invoice.payments.map((p) =>
            h("div", { class: "muted", text: money(p.amountPaid) + " on " + UI.fmtDate(p.paymentDate) + " (" + p.paymentMethod + (p.referenceNumber ? ", ref. " + p.referenceNumber : "") + ")" })
        );

        const modal = UI.openModal({
            title: "Record a payment",
            narrow: true,
            body: h(
                "div",
                null,
                h("p", { text: (invoice.clientName || "") + " · " + (invoice.orderReference || "") }),
                h("p", { class: "muted", text: "Amount " + money(invoice.amount) + " · Paid " + money(invoice.amountPaid) + " · Balance " + money(invoice.balance) }),
                ...earlier,
                h("div", { class: "section-title" }),
                field("Amount paid (₱)", amount),
                field("Payment method", method),
                field("Payment date", date),
                field("Reference number (check no., transfer ref.)", reference),
                errorArea
            ),
            actions: [button("Cancel", () => modal.close()), save],
        });
    }

    clientFilter.addEventListener("change", load);
    statusFilter.addEventListener("change", load);
    overdueFilter.addEventListener("change", load);

    loadClients().then(load).catch((error) => UI.showError(errorBox, error));
});
