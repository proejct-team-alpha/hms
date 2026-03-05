function handleSubmit(event) {
    event.preventDefault();

    var dateInput = document.getElementById('dateInput');
    var today = new Date().toISOString().split('T')[0];
    var isToday = dateInput.value === today;

    if (isToday) {
        alert('방문 접수가 완료되었습니다.');
    } else {
        alert('방문 예약이 등록되었습니다.');
    }

    window.location.href = 'reception-list.html';
}

document.addEventListener('DOMContentLoaded', function () {
    var now = new Date();
    var dateInput = document.getElementById('dateInput');
    var timeInput = document.getElementById('timeInput');

    dateInput.value = now.toISOString().split('T')[0];
    timeInput.value = now.toTimeString().slice(0, 5);
});
