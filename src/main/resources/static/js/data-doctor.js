// Mock Data for Doctor HTML pages
const mockReservations = [
  { id: 1, name: '홍길동', time: '09:00', status: 'COMPLETED', symptoms: '감기 몸살', type: 'ONLINE' },
  { id: 2, name: '김철수', time: '09:30', status: 'IN_PROGRESS', symptoms: '두통', type: 'PHONE' },
  { id: 3, name: '이영희', time: '10:00', status: 'RECEIVED', symptoms: '복통', type: 'WALKIN' },
  { id: 4, name: '박지민', time: '10:30', status: 'RECEIVED', symptoms: '정기 검진', type: 'ONLINE' },
  { id: 5, name: '최동훈', time: '11:00', status: 'RESERVED', symptoms: '허리 통증', type: 'PHONE' },
  { id: 6, name: '정수아', time: '11:30', status: 'RESERVED', symptoms: '피로감', type: 'ONLINE' },
];

function getReservations() {
  const stored = localStorage.getItem('doctor_reservations');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('doctor_reservations', JSON.stringify(mockReservations));
  return mockReservations;
}

function updateReservationStatus(id, newStatus) {
  const reservations = getReservations();
  const updated = reservations.map(r => r.id === id ? { ...r, status: newStatus } : r);
  localStorage.setItem('doctor_reservations', JSON.stringify(updated));
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
