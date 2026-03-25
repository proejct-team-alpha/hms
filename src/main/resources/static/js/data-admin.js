// Admin Data

const initialDepartments = [
  { id: 1, name: '내과', code: 'IM', doctorCount: 5, status: 'ACTIVE' },
  { id: 2, name: '외과', code: 'GS', doctorCount: 3, status: 'ACTIVE' },
  { id: 3, name: '소아청소년과', code: 'PED', doctorCount: 2, status: 'ACTIVE' },
  { id: 4, name: '정형외과', code: 'OS', doctorCount: 4, status: 'INACTIVE' },
];

const initialRules = [
  { id: 1, title: '면회 시간 안내', content: '평일: 18:00~20:00, 주말: 10:00~12:00, 18:00~20:00', isActive: true, category: '일반안내' },
  { id: 2, title: '주차 요금 안내', content: '외래 진료 환자 4시간 무료, 입원 환자 보호자 1대 무료', isActive: true, category: '주차/시설' },
  { id: 3, title: '코로나19 관련 지침 (구버전)', content: '모든 내원객 마스크 착용 의무화', isActive: false, category: '방역지침' },
];

const initialStaff = [
  { id: 1, name: '김관리', role: 'ADMIN', dept: '행정부', phone: '010-1111-2222' },
  { id: 2, name: '이의사', role: 'DOCTOR', dept: '내과', phone: '010-3333-4444' },
  { id: 3, name: '박간호', role: 'NURSE', dept: '외래간호팀', phone: '010-5555-6666' },
  { id: 4, name: '최원무', role: 'STAFF', dept: '원무과', phone: '010-7777-8888' },
  { id: 5, name: '정약사', role: 'STAFF', dept: '약제과', phone: '010-9999-0000' },
  { id: 6, name: '강검사', role: 'STAFF', dept: '검사실', phone: '010-1234-5678' },
  { id: 7, name: '윤영양', role: 'STAFF', dept: '영양실', phone: '010-8765-4321' },
  { id: 8, name: '한시설', role: 'STAFF', dept: '시설관리팀', phone: '010-1111-3333' },
  { id: 9, name: '오보안', role: 'STAFF', dept: '보안팀', phone: '010-2222-4444' },
  { id: 10, name: '임전산', role: 'STAFF', dept: '전산팀', phone: '010-3333-5555' },
  { id: 11, name: '백홍보', role: 'STAFF', dept: '홍보팀', phone: '010-4444-6666' },
];

const initialReservations = [
  { id: 1, time: '09:00', name: '김환자', phone: '010-1111-2222', dept: '내과', doctor: '이의사', status: 'RESERVED', symptoms: '기침, 가래' },
  { id: 2, time: '09:30', name: '이환자', phone: '010-3333-4444', dept: '외과', doctor: '박의사', status: 'RECEIVED', symptoms: '복통' },
  { id: 3, time: '10:00', name: '박환자', phone: '010-5555-6666', dept: '소아청소년과', doctor: '최의사', status: 'COMPLETED', symptoms: '발열' },
  { id: 4, time: '10:30', name: '최환자', phone: '010-7777-8888', dept: '정형외과', doctor: '정의사', status: 'CANCELLED', symptoms: '발목 통증' },
  { id: 5, time: '11:00', name: '정환자', phone: '010-9999-0000', dept: '내과', doctor: '이의사', status: 'IN_PROGRESS', symptoms: '두통' },
];

const initialItems = [
  { id: 1, name: '알코올 솜', category: '의료소모품', stock: 500, minStock: 1000, unit: '박스', code: 'ITEM-0001', location: '중앙공급실' },
  { id: 2, name: '일회용 주사기 (3cc)', category: '의료소모품', stock: 2500, minStock: 1000, unit: '개', code: 'ITEM-0002', location: '중앙공급실' },
  { id: 3, name: '수액 세트', category: '의료소모품', stock: 800, minStock: 500, unit: '세트', code: 'ITEM-0003', location: '중앙공급실' },
  { id: 4, name: '타이레놀 정', category: '의약품', stock: 1200, minStock: 500, unit: '정', code: 'ITEM-0004', location: '약제과' },
  { id: 5, name: 'A4 용지', category: '사무용품', stock: 15, minStock: 20, unit: '박스', code: 'ITEM-0005', location: '행정부' },
];

// Items
function getItems() {
  const stored = localStorage.getItem('medicare_items');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('medicare_items', JSON.stringify(initialItems));
  return initialItems;
}

function saveItems(items) {
  localStorage.setItem('medicare_items', JSON.stringify(items));
}

function getItemById(id) {
  return getItems().find(i => i.id === parseInt(id));
}

function updateItem(id, data) {
  const items = getItems();
  const index = items.findIndex(i => i.id === parseInt(id));
  if (index !== -1) {
    items[index] = { ...items[index], ...data };
    saveItems(items);
  }
}

function addItem(data) {
  const items = getItems();
  const newItem = {
    ...data,
    id: Math.max(0, ...items.map(i => i.id)) + 1,
  };
  items.push(newItem);
  saveItems(items);
}

function deleteItem(id) {
  const items = getItems();
  saveItems(items.filter(i => i.id !== parseInt(id)));
}

// Departments
function getDepartments() {
  const stored = localStorage.getItem('medicare_departments');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('medicare_departments', JSON.stringify(initialDepartments));
  return initialDepartments;
}

function saveDepartments(departments) {
  localStorage.setItem('medicare_departments', JSON.stringify(departments));
}

function getDepartmentById(id) {
  return getDepartments().find(d => d.id === parseInt(id));
}

function updateDepartment(id, data) {
  const departments = getDepartments();
  const index = departments.findIndex(d => d.id === parseInt(id));
  if (index !== -1) {
    departments[index] = { ...departments[index], ...data };
    saveDepartments(departments);
  }
}

function addDepartment(data) {
  const departments = getDepartments();
  const newDept = {
    ...data,
    id: Math.max(0, ...departments.map(d => d.id)) + 1,
  };
  departments.push(newDept);
  saveDepartments(departments);
}

function deleteDepartment(id) {
  const departments = getDepartments();
  saveDepartments(departments.filter(d => d.id !== parseInt(id)));
}

// Rules
function getRules() {
  const stored = localStorage.getItem('medicare_rules');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('medicare_rules', JSON.stringify(initialRules));
  return initialRules;
}

function saveRules(rules) {
  localStorage.setItem('medicare_rules', JSON.stringify(rules));
}

function getRuleById(id) {
  return getRules().find(r => r.id === parseInt(id));
}

function updateRule(id, data) {
  const rules = getRules();
  const index = rules.findIndex(r => r.id === parseInt(id));
  if (index !== -1) {
    rules[index] = { ...rules[index], ...data };
    saveRules(rules);
  }
}

function addRule(data) {
  const rules = getRules();
  const newRule = {
    ...data,
    id: Math.max(0, ...rules.map(r => r.id)) + 1,
  };
  rules.push(newRule);
  saveRules(rules);
}

function deleteRule(id) {
  const rules = getRules();
  saveRules(rules.filter(r => r.id !== parseInt(id)));
}

// Staff
function getStaff() {
  const stored = localStorage.getItem('medicare_staff');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('medicare_staff', JSON.stringify(initialStaff));
  return initialStaff;
}

function saveStaff(staff) {
  localStorage.setItem('medicare_staff', JSON.stringify(staff));
}

function getStaffById(id) {
  return getStaff().find(s => s.id === parseInt(id));
}

function updateStaff(id, data) {
  const staff = getStaff();
  const index = staff.findIndex(s => s.id === parseInt(id));
  if (index !== -1) {
    staff[index] = { ...staff[index], ...data };
    saveStaff(staff);
  }
}

function addStaff(data) {
  const staff = getStaff();
  const newStaff = {
    ...data,
    id: Math.max(0, ...staff.map(s => s.id)) + 1,
  };
  staff.push(newStaff);
  saveStaff(staff);
}

function deleteStaff(id) {
  const staff = getStaff();
  saveStaff(staff.filter(s => s.id !== parseInt(id)));
}

// Reservations
function getReservations() {
  const stored = localStorage.getItem('medicare_reservations');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('medicare_reservations', JSON.stringify(initialReservations));
  return initialReservations;
}

function saveReservations(reservations) {
  localStorage.setItem('medicare_reservations', JSON.stringify(reservations));
}

function updateReservationStatus(id, status) {
  const reservations = getReservations();
  const index = reservations.findIndex(r => r.id === parseInt(id));
  if (index !== -1) {
    reservations[index].status = status;
    saveReservations(reservations);
  }
}

function getRoleLabel(role) {
  switch (role) {
    case 'ADMIN': return '관리자';
    case 'DOCTOR': return '의사';
    case 'NURSE': return '간호사';
    case 'STAFF': return '일반직원';
    case 'ITEM_MANAGER': return '물품담당자';
    default: return role;
  }
}
