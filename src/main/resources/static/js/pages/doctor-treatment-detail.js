function handleComplete(event) {
    var notes = document.getElementById('treatmentNotes').value;
    if (!notes.trim()) {
        alert('진료 기록을 입력해주세요.');
        return;
    }

    var btn = event.currentTarget;
    btn.innerHTML = '<div class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div> 진료 완료';
    btn.disabled = true;
    btn.classList.add('opacity-50');

    setTimeout(function () {
        alert('진료가 완료되었습니다.');
        window.location.href = 'treatment-list.html';
    }, 1000);
}

document.addEventListener('DOMContentLoaded', function () {
    var urlParams = new URLSearchParams(window.location.search);
    var id = urlParams.get('id');

    if (id === '3') {
        document.getElementById('patientName').textContent = '강감찬';
        document.getElementById('patientSymptoms').textContent = '복통';
    } else if (id === '7') {
        document.getElementById('patientName').textContent = '안중근';
        document.getElementById('patientSymptoms').textContent = '소화불량';
    } else if (id === '1') {
        document.getElementById('patientName').textContent = '홍길동';
        document.getElementById('patientSymptoms').textContent = '기침, 발열';
    } else if (id === '11') {
        document.getElementById('patientName').textContent = '유재석';
        document.getElementById('patientSymptoms').textContent = '피로감';
    } else {
        document.getElementById('patientName').textContent = '알 수 없음';
        document.getElementById('patientSymptoms').textContent = '정보 없음';
    }
});
