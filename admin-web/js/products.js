document.addEventListener("DOMContentLoaded", () => {
    loadProducts();

    const addProductBtn = document.getElementById("add-product-btn");
    if (addProductBtn) {
        addProductBtn.addEventListener("click", handleAddProduct);
    }
});

async function loadProducts() {
    const tbody = document.getElementById("products-list-body");
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">Loading inventory...</td></tr>`;

    try {
        const products = await fetchWithAuth("/admin/products");
        tbody.innerHTML = "";

        if (!products || products.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;">No products available.</td></tr>`;
            return;
        }

        products.forEach(item => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${item.id}</td>
                <td><strong>${item.name}</strong></td>
                <td>₱${Number(item.price).toFixed(2)}</td>
                <td>${item.stockQuantity}</td>
                <td>${item.preferredUnit || 'pcs'}</td>
                <td>
                    <button class="btn btn-outline" style="padding:4px 10px; font-size:12px;" onclick="editProduct('${item.id}', ${item.price}, ${item.stockQuantity})">Edit Price/Stock</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:#F87171;">Failed to load product catalog.</td></tr>`;
    }
}

async function handleAddProduct() {
    const name = prompt("Product Name:");
    if (!name) return;
    const price = parseFloat(prompt("Unit Price (₱):"));
    const stockQuantity = parseInt(prompt("Initial Stock Quantity:"));
    const preferredUnit = prompt("Preferred Unit (e.g., box, pcs, kg):", "pcs");

    try {
        await fetchWithAuth("/admin/products", {
            method: "POST",
            body: JSON.stringify({ name, price, stockQuantity, preferredUnit })
        });
        alert("Product added successfully!");
        loadProducts();
    } catch (err) {
        alert("Failed to add product: " + err.message);
    }
}

async function editProduct(id, currentPrice, currentStock) {
    const newPrice = prompt("Update Unit Price (₱):", currentPrice);
    if (newPrice === null) return;
    const newStock = prompt("Update Stock Quantity:", currentStock);
    if (newStock === null) return;

    try {
        await fetchWithAuth(`/admin/products/${id}`, {
            method: "PUT",
            body: JSON.stringify({ price: parseFloat(newPrice), stockQuantity: parseInt(newStock) })
        });
        loadProducts();
    } catch (err) {
        alert("Failed to update product: " + err.message);
    }
}