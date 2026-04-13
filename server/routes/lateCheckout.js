const express = require('express');
const db = require('../data/mockDb');

const router = express.Router();

// 레이트 체크아웃 가능 여부 및 추가 요금 조회
router.get('/', (req, res) => {
    const { rsvNo, reqOutTm } = req.query;

    if (!rsvNo || !reqOutTm) {
        return res.json({ resCd: '9001', resMsg: '필수값 누락', map: {} });
    }

    const rsv = db.reservations[rsvNo];
    if (!rsv) {
        return res.json({ resCd: '9404', resMsg: '예약 없음', map: {} });
    }

    // HHMM 포맷 기준 계산
    const curOutH = parseInt(rsv.chkOutTm.slice(0, 2), 10);
    const reqH = parseInt(reqOutTm.slice(0, 2), 10);
    const diffH = reqH - curOutH;

    let availYn = 'Y';
    let addAmt = 0;
    let rateTpCd = 'FREE';
    let rateTpNm = '무료';

    if (diffH <= 0) {
        availYn = 'N';
        rateTpCd = 'NONE';
        rateTpNm = '불가';
    } else if (diffH <= 2) {
        addAmt = 0;
        rateTpCd = 'FREE';
        rateTpNm = '2시간 이내 무료';
    } else if (diffH <= 5) {
        addAmt = 50000;
        rateTpCd = 'HALF';
        rateTpNm = '반일 요금';
    } else if (diffH <= 8) {
        addAmt = 100000;
        rateTpCd = 'FULL';
        rateTpNm = '전일 요금';
    } else {
        availYn = 'N';
        rateTpCd = 'NONE';
        rateTpNm = '연장 불가';
    }

    res.json({
        resCd: '0000',
        resMsg: 'SUCCESS',
        map: {
            rsvNo: rsvNo,
            roomNo: rsv.roomNo,
            curChkOutTm: rsv.chkOutTm,
            reqChkOutTm: reqOutTm,
            availYn: availYn,
            addAmt: addAmt,
            curCd: 'KRW',
            rateTpCd: rateTpCd,
            rateTpNm: rateTpNm
        }
    });
});

// 레이트 체크아웃 신청
router.post('/', (req, res) => {
    const { rsvNo, reqOutTm, addAmt } = req.body || {};

    if (!rsvNo || !reqOutTm) {
        return res.json({ resCd: '9001', resMsg: '필수값 누락', map: {} });
    }

    const rsv = db.reservations[rsvNo];
    if (!rsv) {
        return res.json({ resCd: '9404', resMsg: '예약 없음', map: {} });
    }

    const now = new Date();
    const record = {
        reqNo: 'LC' + now.getTime().toString().slice(-10),
        rsvNo: rsvNo,
        roomNo: rsv.roomNo,
        curChkOutTm: rsv.chkOutTm,
        reqChkOutTm: reqOutTm,
        addAmt: addAmt || 0,
        procStatCd: 'REQ',
        procStatNm: '접수',
        reqDt: now.toISOString().slice(0, 10).replace(/-/g, ''),
        reqTm: now.toTimeString().slice(0, 5).replace(':', '')
    };
    db.lateCheckoutRequests.push(record);

    res.json({
        resCd: '0000',
        resMsg: 'SUCCESS',
        map: {
            reqNo: record.reqNo,
            procStatCd: 'REQ',
            aprUserNm: 'FRONT DESK'
        }
    });
});

module.exports = router;
