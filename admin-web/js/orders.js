document.addEventListener("DOMContentLoaded", () => {
    loadOrders();
});

async function loadOrders() {
    const tbody = document.getElementById("orders-list-body");
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Loading incoming orders...</td></tr>`;

    try {
        const orders = await fetchWithAuth("/admin/orders?status=PENDING");
        tbody.innerHTML = "";

        if (!orders || orders.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No pending orders found.</td></tr>`;
            return;
        }

        orders.forEach(order => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td><strong>#${order.id}</strong></td>
                <td>${order.clientName}</td>
                <td>
                    <div>${order.itemSummary}</div>
                    ${order.instructions ? `<small style="color:#80C4F6;">Note: ${order.instructions}</small>` : ''}
                </td>
                <td>₱${Number(order.totalAmount).toLocaleString()}</td>
                <td><span class="badge badge-warning">${order.status}</span></td>
                <td>
                    <button class="btn btn-primary" style="padding:4px 10px; font-size:12px;" onclick="updateOrderStatus('${order.id}', 'CONFIRMED')">Confirm</button>
                    <button class="btn btn-outline" style="padding:4px 10px; font-size:12px;" onclick="editOrderDetails('${order.id}')">Edit/Clarify</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#F87171;">Failed to load order queue.</td></tr>`;
    }
}

async function updateOrderStatus(orderId, newStatus) {
    try {
        await fetchWithAuth(`/admin/orders/${orderId}/status`, {
            method: "PATCH",
            body: JSON.stringify({ status: newStatus })
        });
        alert(`Order #${orderId} status updated to ${newStatus}`);
        loadOrders();
    } catch (err) {
        alert("Failed to update status: " + err.message);
    }
}

async function editOrderDetails(orderId) {
    const note = prompt("Add clarifying notes or modify order instruction:");
    if (note === null) return;

    try {
        await fetchWithAuth(`/admin/orders/${orderId}/clarify`, {
            method: "PUT",
            body: JSON.stringify({ instructions: note })
        });
        loadOrders();
    } catch (err) {
        alert("Failed to update order notes: " + err.message);
    }
}