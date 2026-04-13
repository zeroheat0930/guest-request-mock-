const express = require('express');
const cors = require('cors');

const amenityRouter = require('./routes/amenity');
const housekeepingRouter = require('./routes/housekeeping');
const lateCheckoutRouter = require('./routes/lateCheckout');

const app = express();
app.use(cors());
app.use(express.json());

// 간단한 요청 로깅
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
    next();
});

app.use('/api/guest/amenity', amenityRouter);
app.use('/api/guest/housekeeping', housekeepingRouter);
app.use('/api/guest/late-checkout', lateCheckoutRouter);

app.get('/api/health', (req, res) => {
    res.json({ resCd: '0000', resMsg: 'OK', map: { serverTm: new Date().toISOString() } });
});

app.use((req, res) => {
    res.status(404).json({ resCd: '9404', resMsg: 'Not Found', map: {} });
});

const PORT = 3000;
app.listen(PORT, () => {
    console.log(`[MOCK] Guest Request API listening on http://localhost:${PORT}`);
});
