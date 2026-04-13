// 인메모리 가짜 DB (서버 재시작 시 초기화)

const reservations = {
    'R2026041300001': {
        rsvNo: 'R2026041300001',
        roomNo: '1205',
        perNm: 'HONG GILDONG',
        chkInDt: '20260413',
        chkOutDt: '20260415',
        chkOutTm: '1100',
        roomTpCd: 'DLX',
        perUseLang: 'ko_KR'
    },
    'R2026041300002': {
        rsvNo: 'R2026041300002',
        roomNo: '0807',
        perNm: 'JOHN SMITH',
        chkInDt: '20260413',
        chkOutDt: '20260414',
        chkOutTm: '1100',
        roomTpCd: 'STD',
        perUseLang: 'en_US'
    }
};

const amenityItems = {
    'AM001': { itemCd: 'AM001', itemNm: '수건',       itemNmEng: 'Towel',          maxQty: 4 },
    'AM002': { itemCd: 'AM002', itemNm: '생수',       itemNmEng: 'Mineral Water',  maxQty: 6 },
    'AM003': { itemCd: 'AM003', itemNm: '비누',       itemNmEng: 'Soap',           maxQty: 3 },
    'AM004': { itemCd: 'AM004', itemNm: '샴푸',       itemNmEng: 'Shampoo',        maxQty: 3 },
    'AM005': { itemCd: 'AM005', itemNm: '칫솔세트',   itemNmEng: 'Toothbrush Set', maxQty: 4 }
};

const amenityRequests = [];
const housekeepingRequests = [];
const lateCheckoutRequests = [];

module.exports = {
    reservations,
    amenityItems,
    amenityRequests,
    housekeepingRequests,
    lateCheckoutRequests
};
