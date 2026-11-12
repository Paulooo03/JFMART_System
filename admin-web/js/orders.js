"use strict";

document.addEventListener("DOMContentLoaded", () => {
    if (!JF.requireLogin()) return;
    const { h, money, badge, statusBadge, button, field } = UI;

    const TABS = [
        ["PENDING", "Pending"],
        ["CONFIRMED", "Confirmed"],
        ["PREPARING", "Preparing"],
        ["OUT_FOR_DELIVERY", "Out for delivery"],
        ["DELIVERED", "Delivered"],
        ["CANCELLED", "Cancelled"],
        ["ALL", "All"],
    ];
    const ACTIVE_DELIVERY = ["ASSIGNED", "PREPARING", "OUT_FOR_DELIVERY"];

    const errorBox = document.getElementById("page-error");
    const tabsBox = document.getElementById("status-tabs");
    const listBody = document.getElementById("orders-list-body");

    const requested = UI.queryParam("status");
    let current = TABS.some(([key]) => key === requested) ? requested : "PENDING";

    // ---- the list ----

    async function load() {
        try {
            const query = current === "ALL" ? "" : "?status=" + current;
            const orders = await JF.api.get("/admin/orders" + query);
            errorBox.textContent = "";
            renderTabs();
            renderList(orders);
        } catch (error) {
            UI.showError(errorBox, error);
        }
    }

    function renderTabs() {
        UI.clear(tabsBox).append(
            ...TABS.map(([key, label]) =>
                h("button", {
                    type: "button",
                    class: "tab" + (key === current ? " active" : ""),
                    text: label,
                    onclick: () => {
                        current = key;
                        load();
                    },
                })
            )
        );
    }

    function itemSummary(order) {
        const shown = order.items.slice(0, 3).map((i) => i.quantity + " × " + i.name);
        const more = order.items.length - shown.length;
        return shown.join(", ") + (more > 0 ? ", +" + more + " more" : "");
    }

    function renderList(orders) {
        UI.clear(listBody);
        if (orders.length === 0) {
            listBody.append(h("tr", null, h("td", { colspan: 6, class: "muted", text: "No orders in this list." })));
            return;
        }
        listBody.append(
            ...orders.map((order) =>
                h(
                    "tr",
                    null,
                    h("td", null, h("strong", { text: order.referenceNumber }), h("br"), h("small", { class: "muted", text: UI.fmtDate(order.createdAt) })),
                    h("td", { text: order.clientName || "—" }),
                    h(
                        "td",
                        null,
                        h("div", { text: itemSummary(order) }),
                        order.specialInstructions ? h("small", { class: "muted", text: "Note: " + order.specialInstructions }) : null
                    ),
                    h("td", { class: "num" }, money(order.totalAmount), order.hasUnpricedItems ? h("div", null, badge("Needs pricing", "warning")) : null),
                    h("td", null, statusBadge(order.status)),
                    h("td", null, button(order.status === "PENDING" ? "Review" : "Open", () => openReview(order.id), "btn-primary"))
                )
            )
        );
    }

    // ---- the review dialog ----

    async function openReview(orderId) {
        const modal = UI.openModal({
            title: "Order details",
            body: h("p", { class: "muted", text: "Loading..." }),
            actions: [button("Close", () => modal.close())],
        });
        await refreshReview(modal, orderId);
    }

    async function refreshReview(modal, orderId) {
        try {
            const [order, deliveries] = await Promise.all([
                JF.api.get("/orders/" + orderId),
                JF.api.get("/orders/" + orderId + "/deliveries"),
            ]);
            UI.clear(modal.body).append(reviewBody(order, deliveries, modal));
        } catch (error) {
            UI.clear(modal.body).append(h("p", { class: "error-message left", text: error.message }));
        }
    }

    function reviewBody(order, deliveries, modal) {
        const errorArea = h("div", { class: "error-message left" });
        const editable = order.status === "PENDING";

        // Redraws with the order the server returned after a change, and refreshes the list behind the dialog
        const redraw = (updated) => {
            UI.clear(modal.body).append(reviewBody(updated, deliveries, modal));
            load();
        };
        const reload = async () => {
            await refreshReview(modal, order.id);
            load();
        };

        function itemRow(item) {
            const nameCell = h(
                "td",
                null,
                h("strong", { text: item.name }),
                item.customRequest ? " " : null,
                item.customRequest ? badge("Requested item", "info") : null,
                item.instructions ? h("div", { class: "muted", text: item.instructions }) : null
            );
            if (!editable) {
                return h(
                    "tr",
                    null,
                    nameCell,
                    h("td", { class: "num", text: String(item.quantity) }),
                    h("td", { text: item.unit }),
                    h("td", { class: "num", text: money(item.unitPrice) }),
                    h("td", { class: "num", text: money(item.lineTotal) }),
                    h("td")
                );
            }

            const qty = h("input", { class: "input qty", type: "number", min: 1, step: 1, value: String(item.quantity) });
            const price = h("input", {
                class: "input money",
                type: "number",
                min: 0,
                step: "0.01",
                placeholder: "Set price",
                value: item.unitPrice === null ? "" : String(item.unitPrice),
            });
            const save = button("Save", async () => {
                const quantity = Number(qty.value);
                if (!Number.isInteger(quantity) || quantity < 1) {
                    errorArea.textContent = "The quantity must be a whole number of at least 1.";
                    return;
                }
                const body = { quantity };
                const priceText = price.value.trim();
                if (priceText !== "") {
                    if (!/^\d+(\.\d{1,2})?$/.test(priceText)) {
                        errorArea.textContent = "Enter the price as a number with at most two decimals.";
                        return;
                    }
                    body.unitPrice = Number(priceText);
                }
                await UI.runAction(save, errorArea, async () => {
                    redraw(await JF.api.put("/admin/orders/" + order.id + "/items/" + item.id, body));
                    UI.toast("Saved " + item.name + ".");
                });
            }, "btn-primary");
            const remove = button("Remove", async () => {
                const ok = await UI.confirmDialog({
                    title: "Remove " + item.name + "?",
                    message: "It will be taken off this order.",
                    confirmText: "Remove",
                    danger: true,
                });
                if (!ok) return;
                await UI.runAction(remove, errorArea, async () => {
                    redraw(await JF.api.del("/admin/orders/" + order.id + "/items/" + item.id));
                });
            }, "btn-danger");

            return h(
                "tr",
                null,
                nameCell,
                h("td", { class: "num" }, qty),
                h("td", { text: item.unit }),
                h("td", { class: "num" }, price),
                h("td", { class: "num", text: money(item.lineTotal) }),
                h("td", null, h("div", { class: "actions" }, save, remove))
            );
        }

        const details = h(
            "dl",
            { class: "kv" },
            h("dt", { text: "Client" }),
            h("dd", { text: order.clientName || "—" }),
            h("dt", { text: "Order" }),
            h("dd", null, order.referenceNumber + " ", statusBadge(order.status)),
            h("dt", { text: "Ordered" }),
            h("dd", { text: UI.fmtDateTime(order.createdAt) }),
            h("dt", { text: "Client's notes" }),
            h("dd", { text: order.specialInstructions || "None" }),
            order.deliveredAt ? h("dt", { text: "Delivered" }) : null,
            order.deliveredAt ? h("dd", { text: UI.fmtDateTime(order.deliveredAt) }) : null,
            order.cancelledReason ? h("dt", { text: "Cancelled because" }) : null,
            order.cancelledReason ? h("dd", { text: order.cancelledReason }) : null
        );

        const itemsTable = h(
            "div",
            { class: "table-wrap" },
            h(
                "table",
                { class: "data-table" },
                h(
                    "thead",
                    null,
                    h(
                        "tr",
                        null,
                        h("th", { text: "Item" }),
                        h("th", { class: "num", text: "Qty" }),
                        h("th", { text: "Unit" }),
                        h("th", { class: "num", text: "Unit price" }),
                        h("th", { class: "num", text: "Line total" }),
                        h("th")
                    )
                ),
                h("tbody", null, order.items.map(itemRow))
            )
        );

        const totalLine = h(
            "p",
            null,
            h("strong", { text: "Total: " + money(order.totalAmount) }),
            order.hasUnpricedItems ? " " : null,
            order.hasUnpricedItems ? badge("Price every requested item before confirming", "warning") : null
        );

        return h(
            "div",
            null,
            details,
            h("p", { class: "section-title", text: "Items" }),
            itemsTable,
            totalLine,
            deliverySection(order, deliveries, errorArea, reload),
            errorArea,
            h("div", { class: "modal-actions" }, orderActions(order, errorArea, reload))
        );
    }

    function deliverySection(order, deliveries, errorArea, reload) {
        const hasActive = deliveries.some((d) => ACTIVE_DELIVERY.includes(d.status));
        const parts = [];

        if (order.status === "CONFIRMED" && !hasActive) {
            const date = h("input", { class: "input compact", type: "date", min: UI.today(), value: UI.today() });
            const schedule = button("Schedule delivery", async () => {
                if (!date.value) {
                    errorArea.textContent = "Pick a delivery date.";
                    return;
                }
                await UI.runAction(schedule, errorArea, async () => {
                    await JF.api.post("/admin/orders/" + order.id + "/delivery", { scheduledDate: date.value });
                    UI.toast("Delivery scheduled for " + date.value + ".");
                    await reload();
                });
            }, "btn-primary");
            parts.push(
                h("p", { class: "section-title", text: "Delivery" }),
                h("div", { class: "notice", text: "This order is confirmed. Choose the day the courier should deliver it." }),
                h("div", { class: "toolbar" }, date, schedule)
            );
        }

        if (deliveries.length > 0) {
            parts.push(
                h("p", { class: "section-title", text: "Delivery attempts" }),
                ...deliveries.map((d) =>
                    h(
                        "div",
                        { class: "toolbar" },
                        statusBadge(d.status),
                        h("span", { text: "Scheduled " + UI.fmtDate(d.scheduledDate) + " to " + d.deliveryLocation }),
                        d.unsuccessfulReason ? h("span", { class: "danger-text", text: "Reason: " + d.unsuccessfulReason }) : null,
                        d.deliveredAt ? h("span", { class: "muted", text: "Delivered " + UI.fmtDateTime(d.deliveredAt) }) : null
                    )
                )
            );
        }
        return h("div", null, parts);
    }

    function orderActions(order, errorArea, reload) {
        const actions = [];

        if (order.status === "PENDING") {
            const confirm = button("Confirm order", async () => {
                await UI.runAction(confirm, errorArea, async () => {
                    await JF.api.post("/admin/orders/" + order.id + "/confirm");
                    UI.toast("Order confirmed. You can schedule its delivery now.");
                    await reload();
                });
            }, "btn-primary");
            confirm.disabled = order.hasUnpricedItems || order.items.length === 0;
            actions.push(confirm);
        }

        if (order.status === "PENDING" || order.status === "CONFIRMED") {
            actions.push(button("Cancel order", () => askCancel(order, reload), "btn-danger"));
        }
        return actions;
    }

    function askCancel(order, reload) {
        const reason = h("textarea", { class: "input", rows: 3, maxlength: 256, placeholder: "Why is it cancelled? (optional)" });
        const errorArea = h("div", { class: "error-message left" });
        const go = button("Cancel this order", async () => {
            await UI.runAction(go, errorArea, async () => {
                await JF.api.post("/admin/orders/" + order.id + "/cancel", { reason: reason.value.trim() || null });
                modal.close();
                UI.toast("Order cancelled.");
                await reload();
            });
        }, "btn-danger");
        const modal = UI.openModal({
            title: "Cancel " + order.referenceNumber + "?",
            narrow: true,
            body: h("div", null, field("Reason", reason), errorArea),
            actions: [button("Keep order", () => modal.close()), go],
        });
    }

    load();
});
