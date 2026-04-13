const express = require('express');
const db = require('../data/mockDb');

const router = express.Router();

// hkStatCd: MU=객실정비요청, DND=방해금지, CLR=해제
const HK_STAT_NM = { MU: '객실정비요청', DND: '방해금지', CLR: '해제' };

// 현재 하우스키핑 상태 조회
router.get('/', (req, res) => {
    const { rsvNo } = req.query;

    if (!rsvNo) {
        return res.json({ resCd: '9001', resMsg: '필수값 누락', map: {} });
    }

    const rsv = db.reservations[rsvNo];
    if (!rsv) {
        return res.json({ resCd: '9404', resMsg: '예약 없음', map: {} });
    }

    const latest = [...db.housekeepingRequests].reverse().find(r => r.rsvNo === rsvNo);

    res.json({
        resCd: '0000',
        resMsg: 'SUCCESS',
        map: {
            rsvNo: rsvNo,
            roomNo: rsv.roomNo,
            hkStatCd: latest ? latest.hkStatCd : 'CLR',
            hkStatNm: latest ? latest.hkStatNm : '해제',
            lastReqDt: latest ? latest.reqDt : null,
            lastReqTm: latest ? latest.reqTm : null
        }
    });
});

// 하우스키핑 상태 변경
router.post('/', (req, res) => {
    const { rsvNo, hkStatCd, reqMemo } = req.body || {};

    if (!rsvNo || !hkStatCd) {
        return res.json({ resCd: '9001', resMsg: '필수값 누락', map: {} });
    }
    if (!HK_STAT_NM[hkStatCd]) {
        return res.json({ resCd: '9002', resMsg: '상태코드 오류', map: {} });
    }

    const rsv = db.reservations[rsvNo];
    if (!rsv) {
        return res.json({ resCd: '9404', resMsg: '예약 없음', map: {} });
    }

    const now = new Date();
    const record = {
        reqNo: 'HK' + now.getTime().toString().slice(-10),
        rsvNo: rsvNo,
        roomNo: rsv.roomNo,
        hkStatCd: hkStatCd,
        hkStatNm: HK_STAT_NM[hkStatCd],
        reqMemo: reqMemo || '',
        reqDt: now.toISOString().slice(0, 10).replace(/-/g, ''),
        reqTm: now.toTimeString().slice(0, 5).replace(':', '')
    };
    db.housekeepingRequests.push(record);

    res.json({
        resCd: '0000',
        resMsg: 'SUCCESS',
        map: {
            reqNo: record.reqNo,
            hkStatCd: record.hkStatCd,
            hkStatNm: record.hkStatNm
        }
    });
});

module.exports = router;
