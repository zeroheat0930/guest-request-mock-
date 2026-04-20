/**
 * UI 다국어 메시지 사전
 * sessionStorage.getItem('concierge.perUseLang') → ko_KR / en_US / ja_JP / zh_CN
 */
const messages = {
  // LNB
  'brand.name': { ko: '다올 컨시어지', en: 'DAOL Concierge', ja: 'ダオルコンシェルジュ', zh: '达奥礼宾' },
  'brand.sub':  { ko: '고객 서비스',   en: 'Guest Services',  ja: 'ゲストサービス',       zh: '宾客服务' },
  'welcome':    { ko: '고객님',         en: 'Guest',           ja: 'お客様',                zh: '贵宾' },
  'welcome.sub':{ ko: '환영합니다',     en: 'Welcome',         ja: 'ようこそ',              zh: '欢迎光临' },
  // {0} = 방번호. 각 뷰 상단 게스트 정보 바
  'guest.room.label': { ko: '{0}호 고객님', en: 'Room {0} · Guest', ja: '{0}号室 お客様', zh: '{0}号房 贵宾' },

  // Amenity
  'amenity.title':   { ko: '어메니티 요청',    en: 'Amenity Request',      ja: 'アメニティリクエスト', zh: '客房用品' },
  'amenity.sub':     { ko: '필요한 품목을 선택하세요', en: 'Select items you need', ja: '必要なアイテムを選択', zh: '请选择所需物品' },
  'amenity.select':  { ko: '품목 선택',        en: 'Select Items',         ja: 'アイテム選択',         zh: '选择物品' },
  'amenity.hint':    { ko: '원하는 수량을 입력하세요', en: 'Enter desired quantity', ja: '数量を入力してください', zh: '请输入数量' },
  'amenity.max':     { ko: '최대',             en: 'Max',                  ja: '最大',                 zh: '最多' },
  'amenity.memo':    { ko: '메모 (선택)',       en: 'Memo (optional)',      ja: 'メモ（任意）',         zh: '备注（选填）' },
  'amenity.submit':  { ko: '요청하기',          en: 'Submit Request',       ja: 'リクエスト送信',       zh: '提交请求' },
  'amenity.success': { ko: '요청이 접수되었습니다', en: 'Request submitted',  ja: 'リクエストを受け付けました', zh: '请求已提交' },
  'amenity.noitem':  { ko: '품목 수량을 입력하세요', en: 'Please select items', ja: 'アイテムを選択してください', zh: '请选择物品' },
  'amenity.loading': { ko: '품목 불러오는 중...', en: 'Loading items...', ja: 'アイテムを読み込み中...', zh: '加载中...' },

  // Housekeeping
  'hk.title':    { ko: '객실 정비',      en: 'Housekeeping',        ja: 'ハウスキーピング',   zh: '客房服务' },
  'hk.current':  { ko: '현재 객실 상태', en: 'Current Status',      ja: '現在の客室状態',     zh: '当前状态' },
  'hk.change':   { ko: '상태 변경',      en: 'Change Status',       ja: 'ステータス変更',     zh: '更改状态' },
  'hk.mu':       { ko: '룸 정비',        en: 'Make Up Room',        ja: '客室清掃',           zh: '请求打扫' },
  'hk.mu.desc':  { ko: '객실 청소를 요청합니다', en: 'Request room cleaning', ja: '客室清掃をリクエスト', zh: '请求清扫房间' },
  'hk.dnd':      { ko: '방해 금지',      en: 'Do Not Disturb',      ja: 'お休み中',           zh: '请勿打扰' },
  'hk.dnd.desc': { ko: '입실 중 방문을 삼가해 주세요', en: 'Please do not disturb', ja: '邪魔しないでください', zh: '请勿打扰' },
  'hk.clr':      { ko: '정비 완료',      en: 'Clear',               ja: 'クリア',             zh: '取消' },
  'hk.clr.desc': { ko: '현재 상태를 해제합니다', en: 'Clear current request', ja: 'リクエストを解除', zh: '取消当前请求' },
  'hk.current.badge': { ko: '현재', en: 'Current', ja: '現在', zh: '当前' },

  // Late Checkout
  'late.title':      { ko: '레이트 체크아웃', en: 'Late Checkout',       ja: 'レイトチェックアウト', zh: '延迟退房' },
  'late.stdTime':    { ko: '기본 체크아웃',   en: 'Standard Checkout',   ja: '標準チェックアウト',  zh: '标准退房时间' },
  'late.selectTime': { ko: '요청 체크아웃 시각', en: 'Select Desired Time', ja: '希望時間を選択',   zh: '选择延迟时间' },
  'late.check':      { ko: '가능 여부 확인',  en: 'Check Availability',  ja: '空き状況確認',       zh: '查询费用' },
  'late.checking':   { ko: '확인 중...',       en: 'Checking...',         ja: '確認中...',           zh: '查询中...' },
  'late.fee':        { ko: '추가 요금',        en: 'Additional Fee',      ja: '追加料金',            zh: '额外费用' },
  'late.free':       { ko: '무료',             en: 'Free',                ja: '無料',                zh: '免费' },
  'late.submit':     { ko: '레이트 체크아웃 신청하기', en: 'Request Late Checkout', ja: 'レイトチェックアウト申請', zh: '申请延迟退房' },
  'late.avail':      { ko: '✓ 가능',           en: '✓ Available',         ja: '✓ 利用可',            zh: '✓ 可用' },
  'late.unavail':    { ko: '✗ 불가',           en: '✗ Not Available',     ja: '✗ 不可',              zh: '✗ 不可用' },
  'late.step1':      { ko: '시간 선택',        en: 'Select Time',         ja: '時間選択',            zh: '选择时间' },
  'late.step2':      { ko: '요금 확인',        en: 'Check Rate',          ja: '料金確認',            zh: '查询费用' },
  'late.step3':      { ko: '신청 완료',        en: 'Confirmed',           ja: '申請完了',            zh: '申请完成' },

  // Parking
  'park.title':   { ko: '주차 차량 등록', en: 'Parking Registration', ja: '駐車登録',       zh: '停车登记' },
  'park.carNo':   { ko: '차량번호',       en: 'License Plate',        ja: 'ナンバープレート', zh: '车牌号' },
  'park.carTp':   { ko: '차종',           en: 'Vehicle Type',         ja: '車種',            zh: '车型' },
  'park.memo':    { ko: '메모',           en: 'Memo',                 ja: 'メモ',            zh: '备注' },
  'park.optional':{ ko: '선택사항',       en: 'optional',             ja: '任意',            zh: '选填' },
  'park.submit':  { ko: '차량 등록하기',  en: 'Register Vehicle',     ja: '車両を登録',      zh: '登记车辆' },
  'park.list':    { ko: '등록된 차량',    en: 'Registered Vehicles',  ja: '登録済み車両',    zh: '已登记车辆' },
  'park.empty':   { ko: '등록된 차량이 없습니다', en: 'No vehicles registered', ja: '登録車両はありません', zh: '暂无登记车辆' },
  'park.empty.sub':{ ko: '위 양식으로 차량을 등록해 주세요', en: 'Register using the form above', ja: '上のフォームから登録してください', zh: '请使用上方表单登记' },
  'park.loading': { ko: '차량 목록 불러오는 중...', en: 'Loading vehicle list...', ja: '車両リストを読み込み中...', zh: '加载车辆列表...' },
  'park.success': { ko: '차량 등록 완료 — 프론트데스크에 전달되었습니다', en: 'Vehicle registered — sent to front desk', ja: '車両登録完了 — フロントに送信しました', zh: '车辆登记成功 — 已发送至前台' },
  'park.errEmpty':{ ko: '차량번호를 입력하세요', en: 'Please enter license plate', ja: 'ナンバープレートを入力してください', zh: '请输入车牌号' },
  'park.errLen':  { ko: '차량번호 형식 오류 (4~20자)', en: 'Invalid plate (4-20 chars)', ja: 'ナンバープレート形式エラー（4〜20文字）', zh: '车牌号格式错误（4-20位）' },
  'park.carNo.placeholder': { ko: '예: 12가 3456', en: 'e.g. ABC 1234', ja: '例: 品川 300 あ 1234', zh: '例: 京A12345' },
  'park.memo.placeholder':  { ko: '색상이나 특이사항을 입력하세요', en: 'Color or special notes', ja: '色や特記事項を入力してください', zh: '颜色或特殊说明' },

  // Nearby
  'nearby.title':    { ko: '주변 안내',    en: 'Nearby Places',  ja: '周辺案内',     zh: '周边信息' },
  'nearby.food':     { ko: '음식점',       en: 'Restaurant',     ja: 'レストラン',   zh: '餐厅' },
  'nearby.cafe':     { ko: '카페',         en: 'Cafe',           ja: 'カフェ',       zh: '咖啡店' },
  'nearby.conv':     { ko: '편의점',       en: 'Convenience',    ja: 'コンビニ',     zh: '便利店' },
  'nearby.tour':     { ko: '관광지',       en: 'Tourism',        ja: '観光',         zh: '旅游' },
  'nearby.pharmacy': { ko: '약국',         en: 'Pharmacy',       ja: '薬局',         zh: '药局' },
  'nearby.loading':  { ko: '불러오는 중...', en: 'Loading...',   ja: '読み込み中...', zh: '加载中...' },
  'nearby.walk':     { ko: '도보',         en: 'walk',           ja: '徒歩',         zh: '步行' },
  'nearby.min':      { ko: '분',           en: 'min',            ja: '分',           zh: '分钟' },
  'nearby.retry':    { ko: '잠시 후 다시 시도해주세요', en: 'Please try again', ja: 'しばらくしてからもう一度', zh: '请稍后再试' },
  'nearby.noResult': { ko: '근처에 결과가 없어요', en: 'No results nearby', ja: '近くに結果がありません', zh: '附近没有结果' },
  'nearby.noResult.sub': { ko: '다른 카테고리를 선택해 보세요', en: 'Try a different category', ja: '別のカテゴリを選択してください', zh: '请选择其他类别' },
  'nearby.map':      { ko: '카카오맵',     en: 'Map',            ja: 'マップ',       zh: '地图' },

  // History
  'history.title':        { ko: '요청 내역',       en: 'My Requests',         ja: 'リクエスト履歴',     zh: '请求记录' },
  'history.sub':          { ko: '내 요청 내역을 확인하세요', en: 'View your request history', ja: 'リクエスト履歴を確認', zh: '查看您的请求记录' },
  'history.loading':      { ko: '불러오는 중...',   en: 'Loading...',          ja: '読み込み中...',      zh: '加载中...' },
  'history.empty':        { ko: '요청 내역이 없습니다', en: 'No requests yet',  ja: 'リクエストはありません', zh: '暂无请求记录' },
  'history.type.amenity': { ko: '어메니티',         en: 'Amenity',             ja: 'アメニティ',         zh: '客房用品' },
  'history.type.hk':      { ko: '객실정비',         en: 'Housekeeping',        ja: 'ハウスキーピング',   zh: '客房服务' },
  'history.type.parking': { ko: '주차',             en: 'Parking',             ja: '駐車',               zh: '停车' },
  'history.type.lateco':  { ko: '레이트체크아웃',   en: 'Late Checkout',       ja: 'レイトチェックアウト', zh: '延迟退房' },
  'history.stat.req':     { ko: '대기',             en: 'Pending',             ja: '待機',               zh: '待处理' },
  'history.stat.inprog':  { ko: '진행중',           en: 'In Progress',         ja: '進行中',             zh: '处理中' },
  'history.stat.done':    { ko: '완료',             en: 'Completed',           ja: '完了',               zh: '已完成' },

  // Chat
  'chat.title': { ko: 'AI 컨시어지', en: 'AI Concierge', ja: 'AIコンシェルジュ', zh: 'AI礼宾' },

  // Network / Error
  'network.offline': { ko: '네트워크 연결이 끊어졌습니다', en: 'Network connection lost', ja: 'ネットワーク接続が切断されました', zh: '网络连接已断开' },
  'error.server':    { ko: '서버에 연결할 수 없습니다',     en: 'Cannot connect to server', ja: 'サーバーに接続できません',         zh: '无法连接到服务器' },

  // Common
  'loading':      { ko: '불러오는 중...',      en: 'Loading...',          ja: '読み込み中...',       zh: '加载中...' },
  'error':        { ko: '오류가 발생했습니다', en: 'An error occurred',    ja: 'エラーが発生しました', zh: '发生错误' },
  'auth.loading': { ko: '인증 중...',          en: 'Authenticating...',   ja: '認証中...',           zh: '认证中...' },
  'auth.scan':    { ko: 'QR 코드를 스캔하거나 객실 태블릿을 이용해주세요', en: 'Please scan the QR code or use the room tablet', ja: 'QRコードをスキャンするか、客室タブレットをご利用ください', zh: '请扫描二维码或使用客房平板' },
  'auth.expired': { ko: '세션이 만료되었습니다. QR 코드를 다시 스캔해주세요', en: 'Session expired. Please scan the QR code again', ja: 'セッションが期限切れです。QRコードを再スキャンしてください', zh: '会话已过期，请重新扫描二维码' },
  'guest.room':   { ko: '호',                  en: 'Room',                ja: '号室',                zh: '号房' },
};

export function getLang() {
  try {
    const lang = sessionStorage.getItem('concierge.perUseLang') || 'ko_KR';
    return lang.substring(0, 2); // 'ko', 'en', 'ja', 'zh'
  } catch {
    return 'ko';
  }
}

export function t(key, ...args) {
  const entry = messages[key];
  if (!entry) return key;
  const lang = getLang();
  let str = entry[lang] || entry['ko'] || key;
  args.forEach((v, i) => { str = str.replace(`{${i}}`, v); });
  return str;
}
