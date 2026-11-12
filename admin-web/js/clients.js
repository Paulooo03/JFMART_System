"use strict";

document.addEventListener("DOMContentLoaded", () => {
    if (!JF.requireLogin()) return;
    const { h, money, badge, statusBadge, button, field } = UI;

    const errorBox = document.getElementById("page-error");
    const pendingPanel = document.getElementById("pending-panel");
    const pendingBody = document.getElementById("pending-body");
    const listBody = document.getElementById("clients-list-body");
    const search = document.getElementById("client-search");
    let clients = [];

    async function load() {
        try {
            clients = await JF.api.get("/admin/clients");
            errorBox.textContent = "";
            render();
        } catch (error) {
            UI.showError(errorBox, error);
        }
    }

    function render() {
        const term = search.value.trim().toLowerCase();
        const matches = (c) =>
            !term || [c.hospitalName, c.contactPerson, c.username, c.email, c.department].some((v) => (v || "").toLowerCase().includes(term));

        const pending = clients.filter((c) => c.accountStatus === "PENDING_APPROVAL");
        const others = clients.filter((c) => c.accountStatus !== "PENDING_APPROVAL" && matches(c));

        pendingPanel.classList.toggle("hidden", pending.length === 0);
        UI.clear(pendingBody).append(...pending.map(pendingRow));

        UI.clear(listBody);
        if (others.length === 0) {
            listBody.append(h("tr", null, h("td", { colspan: 7, class: "muted", text: "No clients to show." })));
        } else {
            listBody.append(...others.map(clientRow));
        }
    }

    function nameCell(c) {
        return h("td", null, h("strong", { text: c.hospitalName }), h("br"), h("small", { class: "muted", text: c.department || "No department" }));
    }

    function contactCell(c) {
        return h("td", null, c.contactPerson, h("br"), h("small", { class: "muted", text: c.phoneNumber + " · " + c.email }));
    }

    function paymentBadge(c) {
        if (!(Number(c.outstandingBalance) > 0)) return badge("No balance", "muted");
        if (!c.nextDueDate) return badge("Balance due", "warning");
        const days = UI.daysUntil(c.nextDueDate);
        if (days < 0) return badge("Overdue by " + -days + " day" + (days === -1 ? "" : "s"), "danger");
        if (days <= 5) return badge("Due in " + days + " day" + (days === 1 ? "" : "s"), "warning");
        return badge(days + " days left", "success");
    }

    function pendingRow(c) {
        return h(
            "tr",
            null,
            nameCell(c),
            contactCell(c),
            h("td", { text: c.deliveryAddress }),
            h("td", { text: c.username }),
            h(
                "td",
                null,
                h(
                    "div",
                    { class: "actions" },
                    button("Approve", (e) => setAccount(e.currentTarget, c, "approve", "approved"), "btn-primary"),
                    button("Reject", (e) => setAccount(e.currentTarget, c, "reject", "rejected"), "btn-danger")
                )
            )
        );
    }

    function clientRow(c) {
        const overdue = Number(c.overdueBalance) > 0;
        const toggle =
            c.accountStatus === "ACTIVE"
                ? button("Disable", (e) => disable(e.currentTarget, c), "btn-danger")
                : button("Enable", (e) => setAccount(e.currentTarget, c, "approve", "enabled"), "btn-outline");
        return h(
            "tr",
            null,
            nameCell(c),
            contactCell(c),
            h("td", { text: c.deliveryAddress }),
            h("td", { class: "num" + (overdue ? " danger-text" : ""), text: money(c.outstandingBalance) }),
            h("td", null, paymentBadge(c)),
            h("td", null, statusBadge(c.accountStatus)),
            h(
                "td",
                null,
                h(
                    "div",
                    { class: "actions" },
                    h("a", { class: "btn btn-outline btn-small", href: "records.html?clientId=" + c.clientId, text: "Records" }),
                    button("Edit", () => openForm(c)),
                    toggle
                )
            )
        );
    }

    async function setAccount(buttonEl, c, action, doneWord) {
        await UI.runAction(buttonEl, null, async () => {
            await JF.api.post("/admin/clients/" + c.userId + "/" + action);
            UI.toast(c.hospitalName + " " + doneWord + ".");
            await load();
        });
    }

    async function disable(buttonEl, c) {
        const ok = await UI.confirmDialog({
            title: "Disable " + c.hospitalName + "?",
            message: "They will not be able to sign in or place orders until you enable the account again.",
            confirmText: "Disable",
            danger: true,
        });
        if (ok) await setAccount(buttonEl, c, "reject", "disabled");
    }

    /** Add a client (existing === undefined) or edit one. */
    function openForm(existing) {
        const editing = Boolean(existing);
        const input = (value, extra) => h("input", { class: "input", type: "text", value: value || "", ...extra });
        const username = input("", { autocomplete: "off" });
        const password = input("", { type: "password", autocomplete: "new-password" });
        const hospital = input(existing && existing.hospitalName);
        const contact = input(existing && existing.contactPerson);
        const phone = input(existing && existing.phoneNumber, { type: "tel" });
        const email = input(existing && existing.email, { type: "email" });
        const address = input(existing && existing.deliveryAddress);
        const department = input(existing && existing.department);
        const errorBox = h("div", { class: "error-message left" });
        const wide = (el) => {
            el.classList.add("wide");
            return el;
        };

        const form = h(
            "div",
            null,
            h(
                "div",
                { class: "form-grid" },
                wide(field("Client / hospital name", hospital)),
                field("Contact person", contact),
                field("Contact number", phone),
                wide(field("Email address", email)),
                wide(field("Delivery address", address)),
                wide(field("Department (if applicable)", department)),
                editing ? null : field("Username for the client", username),
                editing ? null : field("Password (at least 8 characters)", password)
            ),
            errorBox
        );

        const save = button(editing ? "Save changes" : "Add client", async () => {
            const required = [hospital, contact, phone, email, address, ...(editing ? [] : [username, password])];
            if (required.some((el) => !el.value.trim())) {
                errorBox.textContent = "Fill in every field except department.";
                return;
            }
            if (!editing && password.value.length < 8) {
                errorBox.textContent = "The password needs at least 8 characters.";
                return;
            }
            const details = {
                hospitalName: hospital.value.trim(),
                contactPerson: contact.value.trim(),
                phoneNumber: phone.value.trim(),
                email: email.value.trim(),
                deliveryAddress: address.value.trim(),
                department: department.value.trim() || null,
            };
            await UI.runAction(save, errorBox, async () => {
                if (editing) {
                    await JF.api.put("/admin/clients/" + existing.clientId, details);
                } else {
                    await JF.api.post("/admin/clients", { ...details, username: username.value.trim(), password: password.value });
                }
                modal.close();
                UI.toast(editing ? "Client updated." : "Client added. They can sign in now.");
                await load();
            });
        }, "btn-primary");

        const modal = UI.openModal({
            title: editing ? "Edit " + existing.hospitalName : "Add a new client",
            body: form,
            actions: [button("Cancel", () => modal.close()), save],
        });
    }

    document.getElementById("add-client-btn").addEventListener("click", () => openForm());
    search.addEventListener("input", render);
    load();
});
