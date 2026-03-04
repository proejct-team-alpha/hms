document.addEventListener('DOMContentLoaded', function () {
    var ctx = document.getElementById('timeChart').getContext('2d');
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['09:00', '10:00', '11:00', '13:00', '14:00', '15:00'],
            datasets: [
                {
                    label: '예약',
                    data: [3, 5, 4, 6, 7, 4],
                    backgroundColor: '#818cf8',
                    borderRadius: 4,
                    barPercentage: 0.6,
                    categoryPercentage: 0.8
                },
                {
                    label: '접수',
                    data: [2, 4, 4, 3, 5, 1],
                    backgroundColor: '#34d399',
                    borderRadius: 4,
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
                    position: 'top',
                    labels: {
                        usePointStyle: true,
                        boxWidth: 8
                    }
                },
                tooltip: {
                    backgroundColor: '#f8fafc',
                    titleColor: '#1e293b',
                    bodyColor: '#475569',
                    borderColor: '#e2e8f0',
                    borderWidth: 1,
                    padding: 12,
                    boxPadding: 6
                }
            },
            scales: {
                x: {
                    grid: {
                        display: false,
                        drawBorder: false
                    },
                    ticks: {
                        color: '#64748b',
                        font: { size: 12 }
                    }
                },
                y: {
                    grid: {
                        color: '#e2e8f0',
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
