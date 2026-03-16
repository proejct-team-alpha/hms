// Mock Data for Nurse HTML pages
const mockReservations = [
  { id: 1, name: '홍길동', time: '09:00', status: 'COMPLETED', symptoms: '감기 몸살', type: 'ONLINE', dept: '내과', doctor: '김의사', phone: '010-1234-5678' },
  { id: 2, name: '김철수', time: '09:30', status: 'IN_PROGRESS', symptoms: '두통', type: 'PHONE', dept: '신경과', doctor: '이의사', phone: '010-2345-6789' },
  { id: 3, name: '이영희', time: '10:00', status: 'RECEIVED', symptoms: '복통이 심하고 구토 증세가 있습니다.', type: 'WALKIN', dept: '내과', doctor: '김의사', phone: '010-3456-7890' },
  { id: 4, name: '박지민', time: '10:30', status: 'RECEIVED', symptoms: '정기 검진', type: 'ONLINE', dept: '가정의학과', doctor: '박의사', phone: '010-4567-8901' },
  { id: 5, name: '최동훈', time: '11:00', status: 'RESERVED', symptoms: '허리 통증', type: 'PHONE', dept: '정형외과', doctor: '최의사', phone: '010-5678-9012' },
  { id: 6, name: '정수아', time: '11:30', status: 'RESERVED', symptoms: '피로감', type: 'ONLINE', dept: '내과', doctor: '김의사', phone: '010-6789-0123' },
];

function getReservations() {
  const stored = localStorage.getItem('nurse_reservations');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('nurse_reservations', JSON.stringify(mockReservations));
  return mockReservations;
}

function updateReservationStatus(id, newStatus) {
  const reservations = getReservations();
  const updated = reservations.map(r => r.id === id ? { ...r, status: newStatus } : r);
  localStorage.setItem('nurse_reservations', JSON.stringify(updated));
  return updated;
}

function updatePatientInfo(id, newData) {
  const reservations = getReservations();
  const updated = reservations.map(r => r.id === id ? { ...r, ...newData } : r);
  localStorage.setItem('nurse_reservations', JSON.stringify(updated));
  return updated;
}

function getReservationById(id) {
  return getReservations().find(r => r.id === id);
}

// Helper to get URL parameters
function getQueryParam(param) {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get(param);
}
