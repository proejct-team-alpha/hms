(function () {
  const CHART_API_URL = '/admin/dashboard/chart';
  const REFRESH_INTERVAL_MS = 60_000;
  const DAILY_PATIENT_COLORS = {
    border: '#6366f1',
    fill: 'rgba(99, 102, 241, 0.08)',
    hover: 'rgba(99, 102, 241, 0.18)',
    grid: '#f1f5f9',
    tick: '#94a3b8',
    tooltip: '#1e293b'
  };
  const ITEM_FLOW_COLORS = {
    inbound: '#6366f1',
    outbound: '#cbd5e1'
  };

  let isFetching = false;
  let dailyPatientChart = null;

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
      hideError('item-flow-error');

      renderDailyPatientChart(chartData.dailyPatients || []);
      renderItemFlowChart(chartData.itemFlowDays || []);
    } catch (error) {
      console.error('대시보드 차트 로딩 실패:', error);
      showError('daily-patient-error');
      showError('item-flow-error');
      clearCanvas('daily-patient-canvas');
      clearItemFlowChart();
    } finally {
      isFetching = false;
    }
  }

  async function fetchChartData() {
    const response = await fetch(CHART_API_URL, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
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

    if (typeof Chart === 'undefined') {
      clearCanvas('daily-patient-canvas');
      return;
    }

    const labels = dailyPatients.map((item) => formatDateLabel(item.date));
    const values = dailyPatients.map((item) => Number(item.patientCount) || 0);
    const todayKey = getLocalDateKey();
    const todayIndex = dailyPatients.findIndex((item) => item.date === todayKey);

    if (values.length === 0) {
      destroyDailyPatientChart();
      setupCanvasSize(canvas);
      const emptyCtx = canvas.getContext('2d');
      emptyCtx.clearRect(0, 0, canvas.width, canvas.height);
      drawEmptyText(emptyCtx, canvas.width, canvas.height);
      return;
    }

    destroyDailyPatientChart();

    dailyPatientChart = new Chart(canvas.getContext('2d'), {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: '환자 수',
            data: values,
            backgroundColor: DAILY_PATIENT_COLORS.fill,
            borderColor: DAILY_PATIENT_COLORS.border,
            borderWidth: 3,
            fill: true,
            tension: 0.32,
            // Keep the point markers visible so each day's position is readable at a glance.
            pointRadius(context) {
              return context.dataIndex === todayIndex ? 5 : 3;
            },
            pointHoverRadius(context) {
              return context.dataIndex === todayIndex ? 7 : 6;
            },
            pointBackgroundColor(context) {
              return context.dataIndex === todayIndex ? '#ffffff' : DAILY_PATIENT_COLORS.border;
            },
            pointBorderColor: DAILY_PATIENT_COLORS.border,
            pointBorderWidth(context) {
              return context.dataIndex === todayIndex ? 3 : 2;
            },
            pointHoverBackgroundColor(context) {
              return context.dataIndex === todayIndex ? '#ffffff' : DAILY_PATIENT_COLORS.border;
            },
            pointHoverBorderColor: DAILY_PATIENT_COLORS.border,
            pointHoverBorderWidth(context) {
              return context.dataIndex === todayIndex ? 3 : 2;
            }
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        layout: {
          padding: {
            top: 10,
            bottom: 0,
            left: 0,
            right: 0
          }
        },
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: DAILY_PATIENT_COLORS.tooltip,
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            displayColors: false,
            padding: 12,
            callbacks: {
              label(context) {
                return '환자 수: ' + context.raw + '명';
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              stepSize: 1,
              precision: 0,
              color: DAILY_PATIENT_COLORS.tick,
              font: {
                family: "'Pretendard', sans-serif",
                size: 11
              }
            },
            grid: {
              color: DAILY_PATIENT_COLORS.grid,
              drawBorder: false
            },
            border: {
              display: false
            }
          },
          x: {
            ticks: {
              color: DAILY_PATIENT_COLORS.tick,
              maxRotation: 0,
              minRotation: 0,
              autoSkip: false,
              font: {
                family: "'Pretendard', sans-serif",
                size: 11
              },
              callback(value) {
                return this.getLabelForValue(value);
              }
            },
            grid: {
              display: false,
              drawBorder: false
            },
            border: {
              display: false
            }
          }
        }
      }
    });
  }

  function renderItemFlowChart(itemFlowDays) {
    const container = document.getElementById('item-flow-chart');
    if (!container) {
      return;
    }

    if (!Array.isArray(itemFlowDays) || itemFlowDays.length === 0) {
      container.innerHTML = renderItemFlowEmptyState();
      return;
    }

    container.innerHTML = `
      <div class="h-[220px] w-full flex items-end justify-between gap-2 pb-6 border-b border-slate-100 relative">
        <div class="absolute inset-0 flex flex-col justify-between pointer-events-none pb-6">
          <div class="w-full border-t border-dashed border-slate-200 h-0"></div>
          <div class="w-full border-t border-dashed border-slate-200 h-0"></div>
          <div class="w-full border-t border-dashed border-slate-200 h-0"></div>
          <div class="w-full border-t border-dashed border-slate-200 h-0"></div>
        </div>
        ${itemFlowDays.map((item) => renderItemFlowColumn(item)).join('')}
      </div>
    `;
  }

  function renderItemFlowColumn(item) {
    const label = item.label || '-';
    const inAmount = Number(item.inAmount) || 0;
    const outAmount = Number(item.outAmount) || 0;
    const inHeight = clampHeight(item.inHeight);
    const outHeight = clampHeight(item.outHeight);

    return `
      <div class="flex-1 flex flex-col items-center gap-2 z-10">
        <div class="flex items-end gap-1 w-full justify-center" style="height:170px">
          <div class="w-4 rounded-t-sm" style="height:${inHeight}%; background:${ITEM_FLOW_COLORS.inbound}" title="입고 ${inAmount}개"></div>
          <div class="w-4 rounded-t-sm" style="height:${outHeight}%; background:${ITEM_FLOW_COLORS.outbound}" title="출고 ${outAmount}개"></div>
        </div>
        <div class="text-xs text-slate-500">${label}</div>
      </div>
    `;
  }

  function renderItemFlowEmptyState() {
    return `
      <div class="h-[220px] flex items-center justify-center text-sm text-slate-400">
        데이터 없음
      </div>
    `;
  }

  function clampHeight(height) {
    const numericHeight = Number(height) || 0;
    if (numericHeight <= 0) {
      return 0;
    }
    return Math.max(4, Math.min(100, numericHeight));
  }

  function destroyDailyPatientChart() {
    if (dailyPatientChart) {
      dailyPatientChart.destroy();
      dailyPatientChart = null;
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
    if (canvasId === 'daily-patient-canvas') {
      destroyDailyPatientChart();
    }

    const canvas = document.getElementById(canvasId);
    if (!canvas) {
      return;
    }

    setupCanvasSize(canvas);
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }

  function clearItemFlowChart() {
    const container = document.getElementById('item-flow-chart');
    if (container) {
      container.innerHTML = '';
    }
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

  function getLocalDateKey() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
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
})();
