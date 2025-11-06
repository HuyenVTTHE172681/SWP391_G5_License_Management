document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("filterForm");
    const btnFilter = document.getElementById("btnFilter");
    const btnReset = document.getElementById("btnReset");

    let revenueChart, methodChart, ratingChart;

    // 🧩 Hàm clear chart
    function clearCharts() {
        if (revenueChart) revenueChart.destroy();
        if (methodChart) methodChart.destroy();
        if (ratingChart) ratingChart.destroy();
    }

    // 🧩 Render chart doanh thu & phương thức
    async function renderCharts() {
        const params = new URLSearchParams({
            filter: form.filter.value,
            method: form.method.value,
            start: form.start.value,
            end: form.end.value
        });

        const res = await fetch(`/seller/tools/sales-report/data?${params}`);
        const data = await res.json();

        clearCharts();

        if (data.empty || !data.revenue?.length) {
            document.querySelector("[data-summary='revenue']").textContent = "0 ₫";
            document.querySelector("[data-summary='orders']").textContent = "0";
            document.querySelector("[data-summary='feedbacks']").textContent = "0";
            return;
        }

        document.querySelector("[data-summary='revenue']").textContent = data.summary.totalRevenue + " ₫";
        document.querySelector("[data-summary='orders']").textContent = data.summary.totalOrders;
        document.querySelector("[data-summary='feedbacks']").textContent = data.summary.totalFeedbacks;

        const ctxRev = document.getElementById("revenueChart").getContext("2d");
        const ctxMeth = document.getElementById("methodChart").getContext("2d");

        revenueChart = new Chart(ctxRev, {
            type: "line",
            data: {
                labels: data.revenue.map(r => r.period),
                datasets: [{
                    label: "Doanh thu",
                    data: data.revenue.map(r => r.amount),
                    borderColor: "#0d6efd",
                    backgroundColor: "rgba(13,110,253,0.2)",
                    fill: true,
                    tension: 0.3
                }]
            },
            options: { plugins: { legend: { display: false } }, responsive: true }
        });

        methodChart = new Chart(ctxMeth, {
            type: "doughnut",
            data: {
                labels: data.method.map(r => r.method),
                datasets: [{
                    data: data.method.map(r => r.count),
                    backgroundColor: ["#20c997", "#0dcaf0"]
                }]
            },
            options: { plugins: { legend: { position: "bottom" } } }
        });
    }

    // 🧩 Render Feedback
    async function renderFeedback() {
        const res = await fetch("/seller/tools/sales-report/feedback");
        const feedback = await res.json();

        document.getElementById("totalFeedback").textContent = feedback.totalFeedbacks || 0;
        document.getElementById("avgRating").textContent = feedback.averageRating || "0.0";

        const ctx = document.getElementById("ratingChart").getContext("2d");
        const detailsDiv = document.getElementById("ratingDetails");
        detailsDiv.innerHTML = "";

        if (!feedback.details?.length) {
            detailsDiv.innerHTML = "<p class='text-muted'>Không có dữ liệu feedback</p>";
            return;
        }

        ratingChart = new Chart(ctx, {
            type: "bar",
            data: {
                labels: feedback.details.map(r => `${r.star} Sao`),
                datasets: [{
                    label: "Số lượng",
                    data: feedback.details.map(r => r.count),
                    backgroundColor: "#ffc107"
                }]
            },
            options: {
                responsive: true,
                scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
                plugins: { legend: { display: false } }
            }
        });

        feedback.details.forEach(r => {
            const percent = feedback.totalFeedbacks ? (r.count / feedback.totalFeedbacks * 100).toFixed(1) : 0;
            detailsDiv.innerHTML += `
                <div class="mb-2">
                    <div class="d-flex justify-content-between mb-1">
                        <span>${r.star} Sao</span><span>${r.count}</span>
                    </div>
                    <div class="progress" style="height:8px">
                        <div class="progress-bar bg-warning" role="progressbar" style="width:${percent}%"></div>
                    </div>
                </div>`;
        });
    }

    // 🧩 Nút Lọc
    btnFilter.addEventListener("click", async () => {
        btnFilter.disabled = true;
        await renderCharts();
        await renderFeedback();
        btnFilter.disabled = false;
    });

    // 🧩 Nút Reset
    btnReset.addEventListener("click", async () => {
        form.reset();
        await renderCharts();
        await renderFeedback();
    });

    // 🧩 Load mặc định khi mở trang
    renderCharts();
    renderFeedback();
});
