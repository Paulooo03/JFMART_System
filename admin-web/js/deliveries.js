"use strict";

document.addEventListener("DOMContentLoaded", () => {
    if (!JF.requireLogin()) return;
    const { h, button, field, statusBadge } = UI;

    const ACTIVE = ["ASSIGNED", "PREPARING", "OUT_FOR_DELIVERY"];
    const errorBox = document.getElementById("page-error");
    const waitingPanel = document.getElementById("waiting-panel");
    const waitingBody = document.getElementById("waiting-body");
    const body = document.getElementById("deliveries-body");
    const statusFilter = document.getElementById("status-filter");
    const dateFilter = document.getElementById("date-filter");
    let deliveries = [];

    async function load() {
        try {
            const [all, confirmed] = await Promise.all([JF.api.get("/admin/deliveries"), JF.api.get("/admin/orders?status=CONFIRMED")]);
            deliveries = all;
            errorBox.textContent = "";
            const busy = new Set(all.filter((d) => ACTIVE.includes(d.status)).map((d) => d.orderId));
            renderWaiting(confirmed.filter((o) => !busy.has(o.id)));
            renderDeliveries();
        } catch (error) {
            UI.showError(errorBox, error);
        }
    }

    function renderWaiting(orders) {
        waitingPanel.classList.toggle("hidden", orders.length === 0);
        UI.clear(waitingBody).append(
            ...orders.map((order) =>
                h(
                    "tr",
                    null,
                    h("td", null, h("strong", { text: order.referenceNumber })),
                    h("td", { text: order.clientName || "—" }),
                    h("td", { text: order.items.map((i) => i.quantity + " × " + i.name).join(", ") }),
                    h("td", null, button("Schedule", () => askDate("Schedule " + order.referenceNumber, UI.today(), (date) => JF.api.post("/admin/orders/" + order.id + "/delivery", { scheduledDate: date }), "Delivery scheduled."), "btn-primary"))
                )
            )
        );
    }

    function renderDeliveries() {
        const status = statusFilter.value;
        const date = dateFilter.value;
        const shown = deliveries.filter((d) => (!status || d.status === status) && (!date || d.scheduledDate === date));
        UI.clear(body);
        if (shown.length === 0) {
            body.append(h("tr", null, h("td", { colspan: 7, class: "muted", text: "No deliveries to show." })));
            return;
        }
        body.append(
            ...shown.map((d) =>
                h(
                    "tr",
                    null,
                    h("td", null, h("strong", { text: d.referenceNumber })),
                    h("td", { text: d.clientName || "—" }),
                    h("td", { text: d.deliveryLocation }),
                    h("td", { class: "nowrap", text: UI.fmtDate(d.scheduledDate) }),
                    h("td", null, statusBadge(d.status)),
                    h(
                        "td",
                        null,
                        d.deliveredAt ? h("div", { class: "success-text", text: "Delivered " + UI.fmtDateTime(d.deliveredAt) }) : null,
                        d.unsuccessfulReason ? h("div", { class: "danger-text", text: d.unsuccessfulReason }) : null
                    ),
                    h(
                        "td",
                        null,
                        d.status === "ASSIGNED"
                            ? button("Reschedule", () => askDate("Reschedule " + d.referenceNumber, d.scheduledDate, (date) => JF.api.put("/admin/deliveries/" + d.id + "/schedule", { scheduledDate: date }), "Delivery rescheduled."))
                            : null
                    )
                )
            )
        );
    }

    /** Asks for a date, then runs `save(date)`. */
    function askDate(title, initial, save, doneMessage) {
        const date = h("input", { class: "input", type: "date", min: UI.today(), value: initial < UI.today() ? UI.today() : initial });
        const errorArea = h("div", { class: "error-message left" });
        const go = button("Save", async () => {
            if (!date.value) {
                errorArea.textContent = "Pick a date.";
                return;
            }
            await UI.runAction(go, errorArea, async () => {
                await save(date.value);
                modal.close();
                UI.toast(doneMessage);
                await load();
            });
        }, "btn-primary");
        const modal = UI.openModal({
            title,
            narrow: true,
            body: h("div", null, field("Delivery date", date), errorArea),
            actions: [button("Cancel", () => modal.close()), go],
        });
    }

    statusFilter.addEventListener("change", renderDeliveries);
    dateFilter.addEventListener("change", renderDeliveries);
    document.getElementById("clear-filters").addEventListener("click", () => {
        statusFilter.value = "";
        dateFilter.value = "";
        renderDeliveries();
    });
    load();
});
