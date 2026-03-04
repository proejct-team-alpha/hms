document.addEventListener('DOMContentLoaded', function () {
    var ctx = document.getElementById('inventoryChart').getContext('2d');

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['02/25', '02/26', '02/27', '02/28', '03/01', '03/02', '03/03'],
            datasets: [
                {
                    label: '입고',
                    data: [45, 52, 38, 65, 48, 55, 42],
                    backgroundColor: '#6366f1',
                    borderRadius: { topLeft: 4, topRight: 4 },
                    barPercentage: 0.6,
                    categoryPercentage: 0.8
                },
                {
                    label: '출고',
                    data: [30, 45, 42, 50, 35, 48, 38],
                    backgroundColor: '#cbd5e1',
                    borderRadius: { topLeft: 4, topRight: 4 },
                    barPercentage: 0.6,
                    categoryPercentage: 0.8
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: '#ffffff',
                    titleColor: '#1e293b',
                    bodyColor: '#475569',
                    borderColor: '#e2e8f0',
                    borderWidth: 1,
                    padding: 12,
                    boxPadding: 6,
                    usePointStyle: true,
                    callbacks: {
                        label: function (context) {
                            return context.dataset.label + ': ' + context.parsed.y + '건';
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#64748b',
                        font: { size: 12 }
                    }
                },
                y: {
                    grid: {
                        color: '#f1f5f9',
                        drawBorder: false,
                        borderDash: [3, 3]
                    },
                    ticks: {
                        color: '#64748b',
                        font: { size: 12 }
                    }
                }
            }
        }
    });
});
