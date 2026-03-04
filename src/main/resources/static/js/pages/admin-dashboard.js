document.addEventListener('DOMContentLoaded', function () {
    // 환자 수 차트
    var patientCtx = document.getElementById('patientChart').getContext('2d');
    new Chart(patientCtx, {
        type: 'line',
        data: {
            labels: ['02/25', '02/26', '02/27', '02/28', '03/01', '03/02', '03/03'],
            datasets: [{
                label: '환자 수',
                data: [45, 52, 38, 65, 48, 55, 42],
                borderColor: '#6366f1',
                backgroundColor: '#6366f1',
                borderWidth: 3,
                pointBackgroundColor: '#6366f1',
                pointBorderColor: '#ffffff',
                pointBorderWidth: 2,
                pointRadius: 4,
                pointHoverRadius: 6,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#ffffff',
                    titleColor: '#1e293b',
                    bodyColor: '#475569',
                    borderColor: '#e2e8f0',
                    borderWidth: 1,
                    padding: 12
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#64748b', font: { size: 12 } }
                },
                y: {
                    grid: { color: '#f1f5f9', drawBorder: false, borderDash: [3, 3] },
                    ticks: { color: '#64748b', font: { size: 12 } }
                }
            }
        }
    });

    // 재고 현황 차트 (가로 막대)
    var inventoryCtx = document.getElementById('inventoryChart').getContext('2d');
    new Chart(inventoryCtx, {
        type: 'bar',
        data: {
            labels: ['의료소모품', '의약품', '사무용품', '기타비품'],
            datasets: [{
                label: '재고량',
                data: [85, 62, 45, 30],
                backgroundColor: '#f59e0b',
                borderRadius: 4,
                barPercentage: 0.6
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#ffffff',
                    titleColor: '#1e293b',
                    bodyColor: '#475569',
                    borderColor: '#e2e8f0',
                    borderWidth: 1,
                    padding: 12
                }
            },
            scales: {
                x: {
                    grid: { color: '#f1f5f9', drawBorder: false, borderDash: [3, 3] },
                    ticks: { display: false }
                },
                y: {
                    grid: { display: false },
                    ticks: { color: '#64748b', font: { size: 12 } }
                }
            }
        }
    });
});
