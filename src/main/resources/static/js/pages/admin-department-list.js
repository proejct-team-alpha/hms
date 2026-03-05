function openModal(name, code, count, status) {
    name = name || '';
    code = code || '';
    count = count || 0;
    status = status || 'ACTIVE';

    document.getElementById('modalTitle').textContent = name ? '진료과 수정' : '진료과 등록';
    document.getElementById('deptName').value = name;
    document.getElementById('deptCode').value = code;
    document.getElementById('deptDoctorCount').value = count;
    document.getElementById('deptStatus').value = status;

    var modal = document.getElementById('deptModal');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function closeModal() {
    var modal = document.getElementById('deptModal');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('deptForm').addEventListener('submit', function (e) {
        e.preventDefault();
        alert('저장되었습니다.');
        closeModal();
    });
});
