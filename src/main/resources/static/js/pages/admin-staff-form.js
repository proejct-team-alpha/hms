document.addEventListener('DOMContentLoaded', function () {
    var urlParams = new URLSearchParams(window.location.search);
    var id = urlParams.get('id');

    if (id) {
        document.getElementById('pageTitle').textContent = '직원 정보 수정';
        document.getElementById('staffPassword').placeholder = '변경할 경우에만 입력';
        document.getElementById('staffPassword').required = false;
        document.getElementById('staffConfirmPassword').required = false;

        if (id === '1') {
            document.getElementById('staffName').value = '김관리';
            document.getElementById('staffRole').value = 'ADMIN';
            document.getElementById('staffDept').value = '행정부';
            document.getElementById('staffPhone').value = '010-1111-2222';
        } else if (id === '2') {
            document.getElementById('staffName').value = '이의사';
            document.getElementById('staffRole').value = 'DOCTOR';
            document.getElementById('staffDept').value = '내과';
            document.getElementById('staffPhone').value = '010-3333-4444';
        } else if (id === '3') {
            document.getElementById('staffName').value = '박간호';
            document.getElementById('staffRole').value = 'NURSE';
            document.getElementById('staffDept').value = '외래간호팀';
            document.getElementById('staffPhone').value = '010-5555-6666';
        } else if (id === '4') {
            document.getElementById('staffName').value = '최원무';
            document.getElementById('staffRole').value = 'STAFF';
            document.getElementById('staffDept').value = '원무과';
            document.getElementById('staffPhone').value = '010-7777-8888';
        }
    } else {
        document.getElementById('staffPassword').required = true;
        document.getElementById('staffConfirmPassword').required = true;
    }

    document.getElementById('staffForm').addEventListener('submit', function (e) {
        e.preventDefault();

        var password = document.getElementById('staffPassword').value;
        var confirmPassword = document.getElementById('staffConfirmPassword').value;

        if (password || confirmPassword) {
            if (password !== confirmPassword) {
                alert('비밀번호가 일치하지 않습니다.');
                return;
            }
        }

        alert('직원 정보가 저장되었습니다.');
        window.location.href = 'staff-list.html';
    });
});
