"use strict";

/**
 * Small helpers for building pages.
 * h() creates elements and adds text as text nodes, never as HTML, so names and notes typed by clients
 * can never become script. Do not use innerHTML with data anywhere in this site.
 */
(function (global) {
    // Values that must be set as live properties; everything else is set as a plain attribute
    // (some properties, such as an input's `list`, are read-only and would throw in strict mode)
    const LIVE_PROPERTIES = ["value", "checked", "disabled", "selected"];

    function h(tag, props, ...children) {
        const el = document.createElement(tag);
        if (props) {
            for (const [key, value] of Object.entries(props)) {
                if (value === undefined || value === null || value === false) continue;
                if (key === "class") el.className = value;
                else if (key === "text") el.textContent = value;
                else if (key === "for") el.htmlFor = value;
                else if (key.startsWith("on") && typeof value === "function") el.addEventListener(key.slice(2).toLowerCase(), value);
                else if (LIVE_PROPERTIES.includes(key)) el[key] = value;
                else el.setAttribute(key, value === true ? "" : value);
            }
        }
        for (const child of children.flat(Infinity)) {
            if (child === null || child === undefined || child === false) continue;
            el.append(child instanceof Node ? child : document.createTextNode(String(child)));
        }
        return el;
    }

    function clear(el) {
        while (el.firstChild) el.removeChild(el.firstChild);
        return el;
    }

    // ---- formatting ----

    function money(value) {
        if (value === null || value === undefined || value === "") return "—";
        return "₱" + Number(value).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    /** "2026-11-04" from an ISO date or timestamp. */
    function fmtDate(value) {
        return value ? String(value).slice(0, 10) : "—";
    }

    /** "2026-11-04 14:30" from an ISO timestamp. */
    function fmtDateTime(value) {
        return value ? String(value).replace("T", " ").slice(0, 16) : "—";
    }

    /** Today's date on this computer as yyyy-MM-dd. */
    function today() {
        const d = new Date();
        return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
    }

    /** Whole days from today until the given date (negative when it is in the past). */
    function daysUntil(dateString) {
        const day = 86400000;
        return Math.round((Date.parse(dateString + "T00:00:00") - Date.parse(today() + "T00:00:00")) / day);
    }

    function pretty(status) {
        const text = String(status).toLowerCase().replace(/_/g, " ");
        return text.charAt(0).toUpperCase() + text.slice(1);
    }

    // ---- badges ----

    const STATUS_KIND = {
        PENDING: "warning", UNPAID: "warning", ASSIGNED: "warning", PENDING_APPROVAL: "warning",
        CONFIRMED: "info", PREPARING: "info", OUT_FOR_DELIVERY: "info", PARTIALLY_PAID: "info",
        DELIVERED: "success", PAID: "success", ACTIVE: "success",
        CANCELLED: "danger", UNSUCCESSFUL: "danger", DISABLED: "danger",
    };

    function badge(text, kind) {
        return h("span", { class: "badge badge-" + (kind || "muted"), text });
    }

    function statusBadge(status) {
        return badge(pretty(status), STATUS_KIND[status] || "muted");
    }

    // ---- messages ----

    function toast(message, isError) {
        const el = h("div", { class: "toast" + (isError ? " error" : ""), role: "status", text: message });
        document.body.append(el);
        setTimeout(() => el.remove(), isError ? 6000 : 3500);
    }

    // ---- dialogs ----

    const openModals = [];

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && openModals.length > 0) openModals[openModals.length - 1].close();
    });

    /**
     * Shows a dialog. `body` is a node, `actions` is a list of buttons.
     * Returns { close, body } where body is the element you can refill (for example after saving).
     */
    function openModal({ title, body, actions, narrow }) {
        const bodyBox = h("div", null, body);
        const backdrop = h(
            "div",
            { class: "modal-backdrop" },
            h(
                "div",
                { class: "modal" + (narrow ? " narrow" : ""), role: "dialog", "aria-label": title },
                h("h3", { text: title }),
                bodyBox,
                h("div", { class: "modal-actions" }, actions || [])
            )
        );
        const modal = {
            body: bodyBox,
            close() {
                backdrop.remove();
                const index = openModals.indexOf(modal);
                if (index >= 0) openModals.splice(index, 1);
            },
        };
        backdrop.addEventListener("mousedown", (event) => {
            if (event.target === backdrop) modal.close();
        });
        openModals.push(modal);
        document.body.append(backdrop);
        return modal;
    }

    function button(label, onClick, kind) {
        return h("button", { type: "button", class: "btn btn-small " + (kind || "btn-outline"), onclick: onClick, text: label });
    }

    /** Resolves true when the user confirms. */
    function confirmDialog({ title, message, confirmText, danger }) {
        return new Promise((resolve) => {
            const modal = openModal({
                title,
                narrow: true,
                body: h("p", { text: message }),
                actions: [
                    button("Cancel", () => { modal.close(); resolve(false); }),
                    button(confirmText || "Confirm", () => { modal.close(); resolve(true); }, danger ? "btn-danger" : "btn-primary"),
                ],
            });
        });
    }

    function field(label, input) {
        return h("div", { class: "field" }, h("label", { text: label }), input);
    }

    /** Shows an error message inside a dialog or page area. */
    function showError(box, error) {
        box.textContent = error && error.message ? error.message : "Something went wrong.";
    }

    /** Runs an async action on a button, disabling it meanwhile, and reports failures in `errorBox`. */
    async function runAction(buttonEl, errorBox, action) {
        if (errorBox) errorBox.textContent = "";
        buttonEl.disabled = true;
        try {
            return await action();
        } catch (error) {
            if (errorBox) showError(errorBox, error);
            else toast(error.message, true);
            return undefined;
        } finally {
            buttonEl.disabled = false;
        }
    }

    function queryParam(name) {
        return new URLSearchParams(window.location.search).get(name);
    }

    global.UI = {
        h, clear, money, fmtDate, fmtDateTime, today, daysUntil, pretty,
        badge, statusBadge, toast, openModal, confirmDialog, button, field, showError, runAction, queryParam,
    };
})(window);
