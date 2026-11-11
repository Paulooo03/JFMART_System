document.addEventListener("DOMContentLoaded", () => {
    loadRecords();

    const exportOrdersBtn = document.getElementById("export-orders-btn");
    const exportTxnBtn = document.getElementById("export-transactions-btn");

    if (exportOrdersBtn) {
        exportOrdersBtn.addEventListener("click", () => {
            downloadExcelFile("/admin/records/export/orders", "Order_History_Report.xlsx");
        });
    }

    if (exportTxnBtn) {
        exportTxnBtn.addEventListener("click", () => {
            downloadExcelFile("/admin/records/export/transactions", "Client_Transactions_Report.xlsx");
        });
    }
});

async function loadRecords() {
    const historyBox = document.getElementById("order-history-view");
    const txnBox = document.getElementById("client-transactions-view");

    historyBox.innerHTML = "<p>Loading history...</p>";
    txnBox.innerHTML = "<p>Loading transactions...</p>";

    try {
        const [history, transactions] = await Promise.all([
            fetchWithAuth("/admin/records/history"),
            fetchWithAuth("/admin/records/transactions")
        ]);

        // 1. Order History
        historyBox.innerHTML = "";
        if (!history || history.length === 0) {
            historyBox.innerHTML = "<p style='color:#94A3B8;'>No historical orders logged.</p>";
        } else {
            history.forEach(item => {
                historyBox.innerHTML += `
                    <div style="border-bottom: 1px solid #263540; padding: 10px 0;">
                        <div style="font-weight:600;">Order #${item.id} - ${item.clientName}</div>
                        <div style="font-size:13px; color:#94A3B8;">Date: ${item.date} | Total: ₱${item.totalAmount}</div>
                        <div style="font-size:12px; color:#34D399;">Status: ${item.status}</div>
                    </div>
                `;
            });
        }

        // 2. Client Transactions (Includes Invoice # as per FR-11)
        txnBox.innerHTML = "";
        if (!transactions || transactions.length === 0) {
            txnBox.innerHTML = "<p style='color:#94A3B8;'>No transaction logs found.</p>";
        } else {
            transactions.forEach(tx => {
                txnBox.innerHTML += `
                    <div style="border-bottom: 1px solid #263540; padding: 10px 0;">
                        <div style="font-weight:600;">Invoice #${tx.invoiceNumber}</div>
                        <div style="font-size:13px; color:#94A3B8;">Client: ${tx.clientName}</div>
                        <div style="font-size:13px; color:#80C4F6;">Amount: ₱${tx.amount} (${tx.paymentType})</div>
                    </div>
                `;
            });
        }
    } catch (err) {
        historyBox.innerHTML = "<p style='color:#F87171;'>Error loading history logs.</p>";
        txnBox.innerHTML = "<p style='color:#F87171;'>Error loading transactions.</p>";
    }
}