(function () {
  "use strict";

  const PALETTE = [
    "#2563eb", "#10b981", "#f59e0b", "#ef4444",
    "#8b5cf6", "#06b6d4", "#ec4899", "#84cc16",
    "#f97316", "#6366f1"
  ];

  const currencyFormatter = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
  });

  let categoryChartInstance = null;
  let trendChartInstance = null;

  function getContextPath() {
    const root = document.getElementById("pf-expenditure-analytics");
    return root ? (root.dataset.contextPath || "") : "";
  }

  function showError(message) {
    const banner = document.getElementById("pf-chart-error");
    if (!banner) return;
    if (message) {
      banner.textContent = message;
      banner.style.display = "block";
    } else {
      banner.textContent = "";
      banner.style.display = "none";
    }
  }

  function getThemeColors() {
    const styles = getComputedStyle(document.documentElement);
    return {
      cardBg: styles.getPropertyValue("--bg-card").trim() || "#1D1D1B",
      textPrimary: styles.getPropertyValue("--text-primary").trim() || "#D0CEC7",
      textSecondary: styles.getPropertyValue("--text-secondary").trim() || "#8E8C86",
      border: styles.getPropertyValue("--border").trim() || "#2B2B28"
    };
  }

  function setEmptyState(wrapId, emptyId, isEmpty) {
    const wrap = document.getElementById(wrapId);
    const empty = document.getElementById(emptyId);
    if (wrap) wrap.style.display = isEmpty ? "none" : "block";
    if (empty) empty.style.display = isEmpty ? "block" : "none";
  }

  function renderCategoryChart(categories, totalSpent) {
    const numericTotal = Number(totalSpent) || 0;
    const isEmpty = !Array.isArray(categories) || categories.length === 0 || numericTotal <= 0;
    setEmptyState("pf-category-wrap", "pf-category-empty", isEmpty);

    if (categoryChartInstance) {
      categoryChartInstance.destroy();
      categoryChartInstance = null;
    }
    if (isEmpty) return;

    const labels = categories.map((item) => item.label);
    const values = categories.map((item) => Number(item.amount) || 0);
    const colors = labels.map((_, idx) => PALETTE[idx % PALETTE.length]);
    const theme = getThemeColors();

    const canvas = document.getElementById("pfCategoryChart");
    if (!canvas) return;

    categoryChartInstance = new Chart(canvas.getContext("2d"), {
      type: "doughnut",
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: colors,
          borderColor: theme.cardBg,
          borderWidth: 2,
          hoverOffset: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "62%",
        plugins: {
          legend: {
            position: "bottom",
            labels: {
              boxWidth: 12,
              padding: 14,
              color: theme.textPrimary
            }
          },
          tooltip: {
            callbacks: {
              label: function (context) {
                const value = Number(context.raw) || 0;
                const pct = numericTotal > 0 ? ((value / numericTotal) * 100).toFixed(1) : "0.0";
                return ` ${context.label}: ${currencyFormatter.format(value)} (${pct}%)`;
              }
            }
          }
        }
      }
    });
  }

  function renderTrendChart(trend, granularity) {
    const heading = document.getElementById("pf-trend-heading");
    if (heading) {
      heading.textContent = granularity === "daily"
        ? "Daily Expenditure Trend (Last 30 Days)"
        : "Monthly Expenditure Trend";
    }

    const isEmpty = !Array.isArray(trend) || trend.length === 0;
    setEmptyState("pf-trend-wrap", "pf-trend-empty", isEmpty);

    if (trendChartInstance) {
      trendChartInstance.destroy();
      trendChartInstance = null;
    }
    if (isEmpty) return;

    const labels = trend.map((item) => item.label);
    const values = trend.map((item) => Number(item.amount) || 0);
    const theme = getThemeColors();

    const canvas = document.getElementById("pfTrendChart");
    if (!canvas) return;

    trendChartInstance = new Chart(canvas.getContext("2d"), {
      type: "bar",
      data: {
        labels: labels,
        datasets: [{
          label: "Total Spent",
          data: values,
          backgroundColor: "rgba(37, 99, 235, 0.80)",
          borderColor: "#1d4ed8",
          borderWidth: 1,
          borderRadius: 6,
          maxBarThickness: 44
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function (context) {
                return ` Spent: ${currencyFormatter.format(Number(context.raw) || 0)}`;
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: theme.textSecondary }
          },
          y: {
            beginAtZero: true,
            grid: { color: theme.border },
            ticks: {
              color: theme.textSecondary,
              callback: function (value) {
                return currencyFormatter.format(value);
              }
            }
          }
        }
      }
    });
  }

  async function fetchAndRenderCharts(granularity) {
    showError(null);
    if (typeof Chart === "undefined") {
      showError("Chart library failed to load. Check your network connection and refresh.");
      return;
    }

    const endpoint = `${getContextPath()}/user/api/expense-charts?granularity=${encodeURIComponent(granularity)}`;

    try {
      const response = await fetch(endpoint, {
        method: "GET",
        credentials: "same-origin",
        headers: { "Accept": "application/json" }
      });

      if (response.status === 401 || response.status === 403) {
        showError("Your session has expired or lacks permission. Please sign in again.");
        return;
      }

      const contentType = response.headers.get("content-type") || "";
      if (!contentType.includes("application/json")) {
        showError("Session redirected to login. Refresh the page and sign in again.");
        return;
      }

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      renderCategoryChart(data.categories, data.totalSpent);
      renderTrendChart(data.trend, data.granularity || granularity);
    } catch (err) {
      console.error("Expenditure chart load failed:", err);
      showError("Could not load expenditure charts. Check Tomcat server logs and refresh.");
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    const analyticsSection = document.getElementById("pf-expenditure-analytics");
    if (!analyticsSection) return;

    const buttons = analyticsSection.querySelectorAll(".pf-toggle-btn");
    buttons.forEach((btn) => {
      btn.addEventListener("click", function () {
        buttons.forEach((b) => {
          b.classList.remove("pf-active", "btn-primary");
          b.classList.add("btn-outline");
        });
        btn.classList.remove("btn-outline");
        btn.classList.add("pf-active", "btn-primary");
        fetchAndRenderCharts(btn.dataset.granularity || "monthly");
      });
    });

    window.refreshExpenditureCharts = function () {
      const active = analyticsSection.querySelector(".pf-toggle-btn.pf-active");
      fetchAndRenderCharts(active ? active.dataset.granularity : "monthly");
    };

    fetchAndRenderCharts("monthly");
  });
})();