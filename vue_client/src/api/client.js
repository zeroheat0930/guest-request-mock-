import axios from 'axios';

const client = axios.create({
	baseURL: 'http://localhost:8080/api/gr',
	timeout: 5000
});

// 응답이 { resCd, resMsg, map } 포맷이므로 그대로 꺼내서 반환
client.interceptors.response.use(
	r => r.data,
	e => Promise.reject(e.response?.data || { resCd: '9999', resMsg: e.message, map: {} })
);

// 예약
export const fetchReservation     = (rsvNo) => client.get('/reservation', { params: { rsvNo } });
export const fetchReservationList = ()      => client.get('/reservation/list');

// 어메니티
export const fetchAmenityItems = ()       => client.get('/amenity/items');
export const fetchAmenityList  = (rsvNo)  => client.get('/amenity/list', { params: { rsvNo } });
export const requestAmenity    = (body)   => client.post('/amenity', body);

// 하우스키핑
export const fetchHousekeeping  = (rsvNo) => client.get('/housekeeping', { params: { rsvNo } });
export const updateHousekeeping = (body)  => client.post('/housekeeping', body);

// 레이트 체크아웃
export const checkLateCheckout   = (rsvNo, reqOutTm) => client.get('/late-checkout', { params: { rsvNo, reqOutTm } });
export const requestLateCheckout = (body)            => client.post('/late-checkout', body);
