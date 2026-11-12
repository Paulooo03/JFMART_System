"use strict";

document.addEventListener("DOMContentLoaded", () => {
    if (!JF.requireLogin()) return;
    const { h, money, badge, button, field } = UI;

    const errorBox = document.getElementById("page-error");
    const listBody = document.getElementById("products-list-body");
    const search = document.getElementById("product-search");
    const showInactive = document.getElementById("show-inactive");
    let products = [];

    async function load() {
        try {
            products = await JF.api.get("/admin/products");
            errorBox.textContent = "";
            render();
        } catch (error) {
            UI.showError(errorBox, error);
        }
    }

    function render() {
        const term = search.value.trim().toLowerCase();
        const shown = products.filter((p) => (showInactive.checked || p.active) && (!term || p.name.toLowerCase().includes(term)));
        UI.clear(listBody);
        if (shown.length === 0) {
            listBody.append(h("tr", null, h("td", { colspan: 5, class: "muted", text: "No products to show." })));
            return;
        }
        listBody.append(...shown.map(row));
    }

    function row(p) {
        return h(
            "tr",
            null,
            h("td", null, h("strong", { text: p.name })),
            h("td", { class: "num", text: money(p.price) }),
            h("td", { text: p.unit }),
            h("td", null, p.active ? badge("Active", "success") : badge("Inactive", "muted")),
            h(
                "td",
                null,
                h(
                    "div",
                    { class: "actions" },
                    button("Edit", () => openForm(p)),
                    button(p.active ? "Deactivate" : "Activate", (e) => toggleActive(e.currentTarget, p), p.active ? "btn-danger" : "btn-outline")
                )
            )
        );
    }

    async function toggleActive(buttonEl, p) {
        await UI.runAction(buttonEl, null, async () => {
            await JF.api.put("/admin/products/" + p.id, { name: p.name, price: p.price, unit: p.unit, active: !p.active });
            UI.toast(p.name + (p.active ? " is hidden from clients." : " is visible to clients again."));
            await load();
        });
    }

    /** Add a product (existing === undefined) or edit one. */
    function openForm(existing) {
        const editing = Boolean(existing);
        const name = h("input", { class: "input", type: "text", value: existing ? existing.name : "" });
        const price = h("input", { class: "input", type: "text", inputmode: "decimal", value: existing ? String(existing.price) : "" });
        const unit = h("input", { class: "input", type: "text", list: "unit-suggestions", value: existing ? existing.unit : "" });
        const suggestions = h("datalist", { id: "unit-suggestions" }, ["kg", "pcs", "pack", "box", "tray", "liter"].map((u) => h("option", { value: u })));
        const active = h("input", { type: "checkbox", checked: existing ? existing.active : true });
        const errorBox = h("div", { class: "error-message left" });

        const save = button(editing ? "Save changes" : "Add product", async () => {
            const priceText = price.value.trim();
            if (!name.value.trim() || !unit.value.trim()) {
                errorBox.textContent = "Enter a name and a unit.";
                return;
            }
            if (!/^\d+(\.\d{1,2})?$/.test(priceText)) {
                errorBox.textContent = "Enter the price as a number with at most two decimals, for example 320 or 45.50.";
                return;
            }
            const body = { name: name.value.trim(), price: Number(priceText), unit: unit.value.trim(), active: active.checked };
            await UI.runAction(save, errorBox, async () => {
                if (editing) await JF.api.put("/admin/products/" + existing.id, body);
                else await JF.api.post("/admin/products", body);
                modal.close();
                UI.toast(editing ? "Product updated." : "Product added.");
                await load();
            });
        }, "btn-primary");

        const modal = UI.openModal({
            title: editing ? "Edit " + existing.name : "Add a product",
            narrow: true,
            body: h(
                "div",
                null,
                field("Product name", name),
                field("Unit price (₱)", price),
                field("Preferred unit (kg, pack, box...)", unit),
                suggestions,
                h("label", { class: "check" }, active, " Visible to clients"),
                errorBox
            ),
            actions: [button("Cancel", () => modal.close()), save],
        });
    }

    document.getElementById("add-product-btn").addEventListener("click", () => openForm());
    search.addEventListener("input", render);
    showInactive.addEventListener("change", render);
    load();
});
