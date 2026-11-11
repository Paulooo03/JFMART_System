document.addEventListener("DOMContentLoaded", () => {
    loadClients();

    const addBtn = document.getElementById("add-client-btn");
    if (addBtn) {
        addBtn.addEventListener("click", handleAddClient);
    }
});

async function loadClients() {
    const tbody = document.getElementById("clients-list-body");
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Loading clients...</td></tr>`;

    try {
        const clients = await fetchWithAuth("/admin/clients");
        tbody.innerHTML = "";

        if (!clients || clients.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No clients registered yet.</td></tr>`;
            return;
        }

        clients.forEach(client => {
            const tr = document.createElement("tr");

            // Calculate 30-day payment requirement badge (FR-12)
            const daysLeft = client.daysUntilPaymentDue ?? 30;
            let statusBadge = `<span class="badge badge-success">${daysLeft} days left</span>`;
            if (daysLeft <= 5 && daysLeft > 0) {
                statusBadge = `<span class="badge badge-warning">Due soon (${daysLeft} days)</span>`;
            } else if (daysLeft <= 0) {
                statusBadge = `<span class="badge badge-danger">OVERDUE</span>`;
            }

            tr.innerHTML = `
                <td><strong>${client.name}</strong><br><small style="color:#94A3B8;">${client.department || 'N/A'}</small></td>
                <td>${client.contactInfo}</td>
                <td>${client.deliveryAddress}</td>
                <td>₱${Number(client.outstandingBalance || 0).toLocaleString()}</td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn btn-outline" style="padding:4px 12px; font-size:12px;" onclick="viewHistory('${client.id}')">History</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#F87171;">Failed to load client data.</td></tr>`;
    }
}

async function handleAddClient() {
    const name = prompt("Enter Client/Hospital Name:");
    if (!name) return;
    const contactInfo = prompt("Enter Contact Info:");
    const deliveryAddress = prompt("Enter Delivery Address:");

    try {
        await fetchWithAuth("/admin/clients", {
            method: "POST",
            body: JSON.stringify({ name, contactInfo, deliveryAddress })
        });
        alert("Client added successfully!");
        loadClients();
    } catch (err) {
        alert("Error adding client: " + err.message);
    }
}

function viewHistory(clientId) {
    window.location.href = `records.html?clientId=${clientId}`;
}