function toggleActiveIcon() {
    var isActive = document.getElementById('ruleIsActive').checked;
    var container = document.getElementById('activeIconContainer');

    if (isActive) {
        container.innerHTML = '<i data-lucide="check-circle-2" class="w-5 h-5 text-green-500"></i>';
    } else {
        container.innerHTML = '<i data-lucide="x-circle" class="w-5 h-5 text-slate-400"></i>';
    }
    lucide.createIcons();
}

document.addEventListener('DOMContentLoaded', function () {
    var urlParams = new URLSearchParams(window.location.search);
    var id = urlParams.get('id');

    if (id) {
        document.getElementById('pageTitle').textContent = '병원 규칙 수정';

        if (id === '1') {
            document.getElementById('ruleTitle').value = '면회 시간 안내';
            document.getElementById('ruleCategory').value = '일반안내';
            document.getElementById('ruleContent').value = '평일: 18:00~20:00, 주말: 10:00~12:00, 18:00~20:00';
            document.getElementById('ruleIsActive').checked = true;
        } else if (id === '2') {
            document.getElementById('ruleTitle').value = '주차 요금 안내';
            document.getElementById('ruleCategory').value = '주차/시설';
            document.getElementById('ruleContent').value = '외래 진료 환자 4시간 무료, 입원 환자 보호자 1대 무료';
            document.getElementById('ruleIsActive').checked = true;
        } else if (id === '3') {
            document.getElementById('ruleTitle').value = '코로나19 관련 지침 (구버전)';
            document.getElementById('ruleCategory').value = '방역지침';
            document.getElementById('ruleContent').value = '모든 내원객 마스크 착용 의무화';
            document.getElementById('ruleIsActive').checked = false;
        }
        toggleActiveIcon();
    }

    document.getElementById('ruleForm').addEventListener('submit', function (e) {
        e.preventDefault();
        alert('규칙이 저장되었습니다.');
        window.location.href = 'rule-list.html';
    });
});
