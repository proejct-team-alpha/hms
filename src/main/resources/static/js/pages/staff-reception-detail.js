function handleReceive(event) {
    var btn = event.currentTarget;
    btn.innerHTML = '<div class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div> 접수 완료 처리 (RECEIVED)';
    btn.disabled = true;
    btn.classList.add('opacity-50');

    setTimeout(function () {
        alert('접수가 완료되었습니다.');
        window.location.href = 'reception-list.html';
    }, 1000);
}

document.addEventListener('DOMContentLoaded', function () {
    var urlParams = new URLSearchParams(window.location.search);
    var id = urlParams.get('id');

    if (id === '1') {
        document.getElementById('patientName').textContent = '김철수';
        document.getElementById('patientPhone').textContent = '010-1234-5678';
        document.getElementById('patientType').textContent = 'ONLINE 예약';
        document.getElementById('patientTime').textContent = '오늘 10:00';
        document.getElementById('patientDept').textContent = '내과 / 김의사';
        document.getElementById('patientSymptoms').textContent = '기침, 발열';
    } else if (id === '2') {
        document.getElementById('patientName').textContent = '이영희';
        document.getElementById('patientPhone').textContent = '010-2345-6789';
        document.getElementById('patientType').textContent = 'ONLINE 예약';
        document.getElementById('patientTime').textContent = '오늘 09:30';
        document.getElementById('patientDept').textContent = '정형외과 / 박의사';
        document.getElementById('patientSymptoms').textContent = '허리 통증';
    } else if (id === '3') {
        document.getElementById('patientName').textContent = '박지민';
        document.getElementById('patientPhone').textContent = '010-3456-7890';
        document.getElementById('patientType').textContent = 'WALKIN 예약';
        document.getElementById('patientTime').textContent = '오늘 11:00';
        document.getElementById('patientDept').textContent = '소아과 / 이의사';
        document.getElementById('patientSymptoms').textContent = '복통';
    } else {
        document.getElementById('patientName').textContent = '알 수 없음';
        document.getElementById('patientPhone').textContent = '정보 없음';
        document.getElementById('patientTime').textContent = '정보 없음';
        document.getElementById('patientDept').textContent = '정보 없음';
    }
});
