document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('patientForm').addEventListener('submit', function (e) {
        e.preventDefault();

        var btn = document.getElementById('saveBtn');
        var icon = document.getElementById('saveIcon');
        var spinner = document.getElementById('loadingSpinner');

        btn.disabled = true;
        btn.classList.add('opacity-50');
        icon.classList.add('hidden');
        spinner.classList.remove('hidden');

        setTimeout(function () {
            btn.disabled = false;
            btn.classList.remove('opacity-50');
            icon.classList.remove('hidden');
            spinner.classList.add('hidden');

            alert('환자 정보가 성공적으로 수정되었습니다.');
        }, 1000);
    });
});
