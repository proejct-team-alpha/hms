// Item Manager Data

const initialItems = [
  { id: 1, code: 'MED-001', name: '타이레놀 500mg', category: '의약품', stock: 150, minStock: 50, unit: '정', location: '제1약국', lastUpdated: '2023-10-25' },
  { id: 2, code: 'MED-002', name: '아목시실린 250mg', category: '의약품', stock: 30, minStock: 100, unit: '캡슐', location: '제1약국', lastUpdated: '2023-10-24' },
  { id: 3, code: 'SUP-001', name: '일회용 주사기 3ml', category: '소모품', stock: 500, minStock: 200, unit: '개', location: '중앙공급실', lastUpdated: '2023-10-26' },
  { id: 4, code: 'SUP-002', name: '알코올 솜', category: '소모품', stock: 1000, minStock: 300, unit: '박스', location: '중앙공급실', lastUpdated: '2023-10-26' },
  { id: 5, code: 'EQP-001', name: '혈압계', category: '의료기기', stock: 15, minStock: 5, unit: '대', location: '제1진료실', lastUpdated: '2023-10-20' },
  { id: 6, code: 'MED-003', name: '이부프로펜 400mg', category: '의약품', stock: 200, minStock: 50, unit: '정', location: '제1약국', lastUpdated: '2023-10-25' },
  { id: 7, code: 'SUP-003', name: '수술용 장갑 (M)', category: '소모품', stock: 150, minStock: 100, unit: '박스', location: '수술실', lastUpdated: '2023-10-26' },
  { id: 8, code: 'EQP-002', name: '청진기', category: '의료기기', stock: 20, minStock: 10, unit: '개', location: '제2진료실', lastUpdated: '2023-10-21' },
  { id: 9, code: 'MED-004', name: '소화제', category: '의약품', stock: 80, minStock: 100, unit: '정', location: '제2약국', lastUpdated: '2023-10-25' },
  { id: 10, code: 'SUP-004', name: '거즈', category: '소모품', stock: 300, minStock: 150, unit: '롤', location: '중앙공급실', lastUpdated: '2023-10-26' },
  { id: 11, code: 'EQP-003', name: '체온계', category: '의료기기', stock: 30, minStock: 15, unit: '개', location: '제3진료실', lastUpdated: '2023-10-22' },
  { id: 12, code: 'MED-005', name: '항히스타민제', category: '의약품', stock: 120, minStock: 80, unit: '정', location: '제1약국', lastUpdated: '2023-10-25' },
];

const initialHistory = [
  { id: 1, itemId: 1, itemName: '타이레놀 500mg', type: 'IN', quantity: 100, date: '2023-10-25 10:00', manager: '김관리', note: '정기 입고' },
  { id: 2, itemId: 2, itemName: '아목시실린 250mg', type: 'OUT', quantity: 20, date: '2023-10-24 14:30', manager: '이관리', note: '내과 요청' },
  { id: 3, itemId: 3, itemName: '일회용 주사기 3ml', type: 'IN', quantity: 300, date: '2023-10-26 09:15', manager: '박관리', note: '긴급 발주' },
  { id: 4, itemId: 4, itemName: '알코올 솜', type: 'OUT', quantity: 50, date: '2023-10-26 11:00', manager: '최관리', note: '외과 요청' },
  { id: 5, itemId: 5, itemName: '혈압계', type: 'IN', quantity: 5, date: '2023-10-20 16:45', manager: '정관리', note: '신규 구매' },
];

function getItems() {
  const stored = localStorage.getItem('medicare_items');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('medicare_items', JSON.stringify(initialItems));
  return initialItems;
}

function saveItems(items) {
  localStorage.setItem('medicare_items', JSON.stringify(items));
}

function getHistory() {
  const stored = localStorage.getItem('medicare_history');
  if (stored) return JSON.parse(stored);
  localStorage.setItem('medicare_history', JSON.stringify(initialHistory));
  return initialHistory;
}

function saveHistory(history) {
  localStorage.setItem('medicare_history', JSON.stringify(history));
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
    lastUpdated: new Date().toISOString().split('T')[0]
  };
  items.push(newItem);
  saveItems(items);
}

function addHistory(data) {
  const history = getHistory();
  const newHistory = {
    ...data,
    id: Date.now(),
    date: new Date().toISOString().slice(0, 16).replace('T', ' '),
  };
  history.unshift(newHistory);
  saveHistory(history);
  
  // Update stock
  const items = getItems();
  const itemIndex = items.findIndex(i => i.id === parseInt(data.itemId));
  if (itemIndex !== -1) {
    const item = items[itemIndex];
    const newStock = data.type === 'IN' ? item.stock + parseInt(data.quantity) : item.stock - parseInt(data.quantity);
    items[itemIndex] = { ...item, stock: newStock, lastUpdated: newHistory.date.split(' ')[0] };
    saveItems(items);
  }
}
