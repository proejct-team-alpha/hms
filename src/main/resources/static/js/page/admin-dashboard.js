(function () {
  const CHART_API_URL = '/admin/dashboard/chart';
  const REFRESH_INTERVAL_MS = 60_000;

  let isFetching = false;

  document.addEventListener('DOMContentLoaded', () => {
    if (typeof feather !== 'undefined') {
      feather.replace();
    }

    refreshCharts();
    setInterval(() => {
      refreshCharts();
    }, REFRESH_INTERVAL_MS);
  });

  async function refreshCharts() {
    if (isFetching) {
      return;
    }

    isFetching = true;

    try {
      const chartData = await fetchChartData();
      hideError('daily-patient-error');
      hideError('category-stock-error');

      renderDailyPatientChart(chartData.dailyPatients || []);
      renderCategoryChart(chartData.categoryCounts || []);
    } catch (error) {
      console.error('대시보드 차트 로딩 실패:', error);
      showError('daily-patient-error');
      showError('category-stock-error');
      clearCanvas('daily-patient-canvas');
      clearCanvas('category-stock-canvas');
    } finally {
      isFetching = false;
    }
  }

  async function fetchChartData() {
    const response = await fetch(CHART_API_URL, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error('HTTP ' + response.status);
    }

    const payload = await response.json();
    const chartBody = payload && payload.body;

    if (!chartBody) {
      throw new Error('차트 응답 body가 없습니다.');
    }

    return chartBody;
  }

  function renderDailyPatientChart(dailyPatients) {
    const canvas = document.getElementById('daily-patient-canvas');
    if (!canvas) {
      return;
    }

    setupCanvasSize(canvas);
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const padding = { top: 20, right: 16, bottom: 42, left: 16 };

    ctx.clearRect(0, 0, width, height);

    const maxCount = Math.max(1, ...dailyPatients.map((item) => Number(item.patientCount) || 0));
    const chartHeight = height - padding.top - padding.bottom;
    const chartWidth = width - padding.left - padding.right;
    const barWidth = dailyPatients.length > 0 ? Math.max(10, chartWidth / (dailyPatients.length * 1.8)) : 0;
    const stepX = dailyPatients.length > 0 ? chartWidth / dailyPatients.length : 0;

    drawHorizontalGrid(ctx, width, height, padding, 5);

    if (dailyPatients.length === 0) {
      drawEmptyText(ctx, width, height);
      return;
    }

    dailyPatients.forEach((item, index) => {
      const count = Number(item.patientCount) || 0;
      const x = padding.left + (index * stepX) + (stepX - barWidth) / 2;
      const barHeight = (count / maxCount) * chartHeight;
      const y = height - padding.bottom - barHeight;

      ctx.fillStyle = '#4f46e5';
      roundRect(ctx, x, y, barWidth, barHeight, 4, true, false);

      const label = formatDateLabel(item.date);
      ctx.fillStyle = '#64748b';
      ctx.font = '12px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(label, x + barWidth / 2, height - 16);
    });
  }

  function renderCategoryChart(categoryCounts) {
    const canvas = document.getElementById('category-stock-canvas');
    if (!canvas) {
      return;
    }

    setupCanvasSize(canvas);
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const padding = { top: 20, right: 16, bottom: 20, left: 110 };

    ctx.clearRect(0, 0, width, height);

    if (categoryCounts.length === 0) {
      drawEmptyText(ctx, width, height);
      return;
    }

    const maxCount = Math.max(1, ...categoryCounts.map((item) => Number(item.totalCount) || 0));
    const chartWidth = width - padding.left - padding.right;
    const rowHeight = (height - padding.top - padding.bottom) / categoryCounts.length;

    categoryCounts.forEach((item, index) => {
      const count = Number(item.totalCount) || 0;
      const y = padding.top + (index * rowHeight) + rowHeight * 0.2;
      const barHeight = rowHeight * 0.6;
      const barWidth = (count / maxCount) * chartWidth;

      ctx.fillStyle = '#475569';
      ctx.font = '13px sans-serif';
      ctx.textAlign = 'right';
      ctx.textBaseline = 'middle';
      ctx.fillText(item.categoryName || '-', padding.left - 12, y + barHeight / 2);

      ctx.fillStyle = '#e2e8f0';
      roundRect(ctx, padding.left, y, chartWidth, barHeight, 6, true, false);

      ctx.fillStyle = '#f97316';
      roundRect(ctx, padding.left, y, Math.max(4, barWidth), barHeight, 6, true, false);

      ctx.fillStyle = '#334155';
      ctx.textAlign = 'left';
      ctx.fillText(String(count), padding.left + Math.min(barWidth + 8, chartWidth + 8), y + barHeight / 2);
    });
  }

  function drawHorizontalGrid(ctx, width, height, padding, lineCount) {
    ctx.strokeStyle = '#e2e8f0';
    ctx.lineWidth = 1;

    for (let i = 0; i <= lineCount; i += 1) {
      const y = padding.top + ((height - padding.top - padding.bottom) * i) / lineCount;
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();
    }
  }

  function drawEmptyText(ctx, width, height) {
    ctx.fillStyle = '#94a3b8';
    ctx.font = '14px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('데이터 없음', width / 2, height / 2);
  }

  function setupCanvasSize(canvas) {
    const rect = canvas.getBoundingClientRect();
    const pixelRatio = window.devicePixelRatio || 1;
    const displayWidth = Math.floor(rect.width * pixelRatio);
    const displayHeight = Math.floor(rect.height * pixelRatio);

    if (canvas.width !== displayWidth || canvas.height !== displayHeight) {
      canvas.width = displayWidth;
      canvas.height = displayHeight;
    }
  }

  function clearCanvas(canvasId) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) {
      return;
    }

    setupCanvasSize(canvas);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }

  function formatDateLabel(dateString) {
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) {
      return '-';
    }

    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return month + '/' + day;
  }

  function showError(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
      element.classList.remove('hidden');
    }
  }

  function hideError(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
      element.classList.add('hidden');
    }
  }

  function roundRect(ctx, x, y, width, height, radius, fill, stroke) {
    const r = Math.min(radius, width / 2, height / 2);
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + width, y, x + width, y + height, r);
    ctx.arcTo(x + width, y + height, x, y + height, r);
    ctx.arcTo(x, y + height, x, y, r);
    ctx.arcTo(x, y, x + width, y, r);
    ctx.closePath();

    if (fill) {
      ctx.fill();
    }
    if (stroke) {
      ctx.stroke();
    }
  }
})();
