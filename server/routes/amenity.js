const express = require('express');
const db = require('../data/mockDb');

const router = express.Router();

// 어메니티 품목 마스터 조회
router.get('/items', (req, res) => {
    res.json({
        resCd: '0000',
        resMsg: 'SUCCESS',
        map: {
            totCnt: Object.keys(db.amenityItems).length,
            list: Object.values(db.amenityItems)
        }
    });
});

// 어메니티 요청 목록 조회
router.get('/list', (req, res) => {
    const { rsvNo } = req.query;
    const list = db.amenityRequests.filter(r => !rsvNo || r.rsvNo === rsvNo);
    res.json({
        resCd: '0000',
        resMsg: 'SUCCESS',
        map: {
            totCnt: list.length,
            list: list
        }
    });
});

// 어메니티 요청 등록
router.post('/', (req, res) => {
    const { rsvNo, roomNo, itemList, reqMemo } = req.body || {};

    if (!rsvNo || !roomNo || !Array.isArray(itemList) || itemList.length === 0) {
        return res.json({ resCd: '9001', resMsg: '필수값 누락', map: {} });
    }

    const rsv = db.reservations[rsvNo];
    if (!rsv) {
        return res.json({ resCd: '9404', resMsg: '예약 없음', map: {} });
    }

    for (const it of itemList) {
        const master = db.amenityItems[it.itemCd];
        if (!master) {
            return res.json({ resCd: '9002', resMsg: '품목코드 오류: ' + it.itemCd, map: {} });
        }
        if (it.qty > master.maxQty) {
            return res.json({ resCd: '9003', resMsg: '최대수량 초과: ' + master.itemNm, map: {} });
        }
    }

    const now = new Date();
    const reqNo = 'AM' + now.getTime().toString().slice(-10);
    const record = {
        reqNo: reqNo,
        rsvNo: rsvNo,
        roomNo: roomNo,
        itemList: itemList,
        reqMemo: reqMemo || '',
        procStatCd: 'REQ',
        procStatNm: '접수',
        reqDt: now.toISOString().slice(0, 10).replace(/-/g, ''),
        reqTm: now.toTimeString().slice(0, 5).replace(':', '')
    };
    db.amenityRequests.push(record);

    res.json({
        resCd: '0000',
        resMsg: 'SUCCESS',
        map: {
            reqNo: reqNo,
            procStatCd: 'REQ',
            estArrMin: 15
        }
    });
});

module.exports = router;
