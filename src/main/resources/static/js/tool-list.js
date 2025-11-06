"use strict";

const container = document.getElementById('toolsContainer');
const pagination = document.getElementById('pagination');
let currentPage = 0;
const pageSize = 6;

// === Load tools từ API ===
async function loadTools(page = 0) {
    const params = new URLSearchParams();
    const keyword = document.getElementById('keyword')?.value || "";
    const category = document.getElementById('category')?.value || "";
    const status = document.getElementById('status')?.value || "";
    const minPrice = document.getElementById('minPrice')?.value || "";
    const maxPrice = document.getElementById('maxPrice')?.value || "";
    const sortValue = document.getElementById('sort')?.value || "newest";
    const loginMethod = document.getElementById('loginMethod')?.value || "";

    if (keyword) params.append('keyword', keyword);
    if (category) params.append('categoryId', category);
    if (status) params.append('status', status);
    if (minPrice) params.append('minPrice', minPrice);
    if (maxPrice) params.append('maxPrice', maxPrice);
    if (loginMethod) params.append('loginMethod', loginMethod);
    params.append('sort', sortValue);
    params.append('page', page);
    params.append('size', pageSize);

    container.innerHTML = `
    <div class="text-center py-5">
      <div class="spinner-border text-success" role="status"></div>
      <p class="text-muted mt-2">Loading tools...</p>
    </div>
  `;

    try {
        const res = await fetch('/seller/tools/api?' + params.toString());
        if (!res.ok) throw new Error('Failed to fetch tools');
        const data = await res.json();

        /// ✅ API trả về Page<Tool> từ Spring Boot
        const tools = data.content || [];
        renderTools(tools);
        renderPagination(data.number, data.totalPages);
        currentPage = data.number;

    } catch (err) {
        console.error(err);
        container.innerHTML = `<p class="text-danger text-center mt-4">Failed to load tools.</p>`;
    }
}

// === Load categories từ API ===
async function loadCategories() {
    try {
        const res = await fetch("/seller/tools/categories");
        if (!res.ok) throw new Error("Failed to load categories");
        const categories = await res.json();

        const select = document.getElementById('category');
        select.innerHTML = '<option value="">All</option>';
        categories.forEach(cat => {
            const opt = document.createElement('option');
            opt.value = cat.categoryId;
            opt.textContent = cat.categoryName;
            select.appendChild(opt);
        });
    } catch (e) {
        console.error("Load categories error:", e);
    }
}

// === Render tool cards ===
function renderTools(tools) {
    container.innerHTML = '';
    if (!tools || tools.length === 0) {
        container.innerHTML = '<p class="text-center text-muted mt-4">No tools found.</p>';
        return;
    }

    const ctx = window.location.origin;

    tools.forEach(tool => {
        let imagePath = `${ctx}/uploads/tools/default-placeholder.png`;
        if (tool.image) {
            if (/^https?:\/\//i.test(tool.image)) {
                imagePath = tool.image;
            } else if (tool.image.startsWith('/')) {
                imagePath = `${ctx}${tool.image}`;
            } else {
                if (tool.image.includes("uploads/")) {
                    imagePath = `${ctx}/${tool.image.startsWith('/') ? tool.image.substring(1) : tool.image}`;
                } else {
                    imagePath = `${ctx}/uploads/images/${tool.image}`;
                }
            }
        }

        const categoryName = tool.category ? tool.category.categoryName : 'No category';
        const priceBlock = (tool.licenses && tool.licenses.length > 0)
            ? tool.licenses.map(l => {
                const days = (l.durationDays == null) ? '∞' : `${l.durationDays}d`;
                const price = (typeof l.price === 'number') ? l.price.toLocaleString() : l.price;
                return `${days}: ${price}₫`;
            }).join('<br>')
            : 'No license yet';

        const updated = tool.updatedAt
            ? new Date(tool.updatedAt).toLocaleString('en-GB')
            : (tool.createdAt ? new Date(tool.createdAt).toLocaleString('en-GB') : '—');

        const badgeClass = getStatusBadgeClass(tool.status);

        // ✅ Thêm logic hiện nút Deactivate nếu status = PUBLISHED
        let actionSection = '';
        if (tool.status === 'PUBLISHED') {
            actionSection = `
                <button class="btn btn-outline-danger btn-sm px-3 rounded-pill shadow-sm"
                        onclick="deactivateTool(${tool.toolId})">
                    <i class="bi bi-x-circle"></i> Deactivate
                </button>
            `;
        } else {
            actionSection = `<span class="badge rounded-pill ${badgeClass} px-3 py-2">${tool.status ?? '—'}</span>`;
        }

        container.insertAdjacentHTML('beforeend', `
      <div class="col-md-4 mb-4">
        <div class="tool-card border-0 shadow-sm rounded-4 overflow-hidden bg-white h-100 d-flex flex-column">
          <div class="position-relative">
            <img src="${imagePath}" alt="Tool image" style="width:100%;height:180px;object-fit:cover;">
          </div>
          <div class="card-body d-flex flex-column justify-content-between flex-grow-1 p-3">
            <div>
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold mb-0 text-dark text-truncate" title="${escapeHtml(tool.toolName || '')}">
                  ${escapeHtml(tool.toolName || '')}
                </h5>
                <span class="badge rounded-pill bg-light text-dark border">Qty: ${tool.quantity ?? 0}</span>
              </div>
              <p class="text-muted small mb-1"><i class="bi bi-tag"></i> ${escapeHtml(categoryName)}</p>
              <p class="text-muted small mb-2"><i class="bi bi-clock"></i> ${updated}</p>
              <p class="fw-semibold text-primary mb-2" style="white-space:pre-line;font-size:0.9rem;">${priceBlock}</p>
            </div>
            <div class="mt-auto d-flex justify-content-between align-items-center mt-3">
              <a href="/seller/tools/edit/${tool.toolId}" class="btn btn-outline-primary btn-sm px-3 rounded-pill shadow-sm">
                <i class="bi bi-pencil"></i> Edit
              </a>
              ${actionSection}
            </div>
          </div>
        </div>
      </div>
    `);
    });
}

// === Badge color ===
function getStatusBadgeClass(status) {
    switch (status) {
        case 'PENDING':   return 'bg-warning text-dark';
        case 'APPROVED':  return 'bg-success';
        case 'REJECTED':  return 'bg-danger';
        case 'PUBLISHED': return 'bg-primary';
        case 'DEACTIVATED':  return 'bg-secondary';
        default:          return 'bg-light text-dark';
    }
}

// === Pagination ===
function renderPagination(page, totalPages) {
    if (!totalPages || totalPages <= 1) {
        pagination.innerHTML = '';
        return;
    }

    pagination.innerHTML = `
    <div class="d-flex justify-content-center align-items-center gap-3 mt-4">
      <button class="btn btn-outline-secondary btn-sm"
              ${page <= 0 ? 'disabled' : ''}
              onclick="loadTools(${page - 1})">← Prev</button>
      <span>Page ${page + 1} / ${totalPages}</span>
      <button class="btn btn-outline-secondary btn-sm"
              ${page >= totalPages - 1 ? 'disabled' : ''}
              onclick="loadTools(${page + 1})">Next →</button>
    </div>
  `;
}

// === Filter ===
function applyFilters() {
    currentPage = 0;
    loadTools(0);
}

// === Escape HTML ===
function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[m]));
}

// === On page load ===
document.addEventListener('DOMContentLoaded', () => {
    loadCategories();
    loadTools();
});
// ✅ Ngăn reload form khi nhấn Go
const form = document.querySelector('.filter-card form');
if (form) {
    form.addEventListener('submit', e => {
        e.preventDefault();
        applyFilters();
    });
}

/// === ✅ Hàm deactivate tool (fix redirect + thông báo rõ ràng) ===
async function deactivateTool(toolId) {
    if (!confirm("Are you sure you want to deactivate this tool?")) return;

    try {
        const res = await fetch(`/seller/tools/${toolId}/deactivate`, {
            method: "POST",
            headers: {
                "X-Requested-With": "XMLHttpRequest" // 🔥 Ngăn Spring redirect toàn trang
            }
        });

        // Nếu backend trả JSON hoặc text
        const text = await res.text();

        if (res.ok) {
            alert("✅ Tool has been deactivated successfully!");
            loadTools(currentPage); // reload danh sách hiện tại
        } else {
            alert("❌ Failed to deactivate tool: " + text);
        }
    } catch (err) {
        console.error("Deactivate error:", err);
        alert("⚠️ Error while deactivating tool: " + err.message);
    }
}

