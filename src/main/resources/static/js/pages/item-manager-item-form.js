document.addEventListener('DOMContentLoaded', function () {
    var urlParams = new URLSearchParams(window.location.search);
    var itemId = urlParams.get('id');

    if (itemId) {
        document.getElementById('pageTitle').textContent = '물품 정보 수정';

        if (itemId === '1') {
            document.getElementById('itemName').value = '주사기 (10ml)';
            document.getElementById('itemCategory').value = '의료소모품';
            document.getElementById('itemStock').value = '50';
            document.getElementById('itemMinStock').value = '100';
            document.getElementById('itemUnit').value = '개';
        } else if (itemId === '2') {
            document.getElementById('itemName').value = '타이레놀 정';
            document.getElementById('itemCategory').value = '의약품';
            document.getElementById('itemStock').value = '500';
            document.getElementById('itemMinStock').value = '100';
            document.getElementById('itemUnit').value = '정';
        }
    }

    document.getElementById('itemForm').addEventListener('submit', function (e) {
        e.preventDefault();
        alert('물품 정보가 저장되었습니다.');
        window.location.href = 'item-list.html';
    });
});
