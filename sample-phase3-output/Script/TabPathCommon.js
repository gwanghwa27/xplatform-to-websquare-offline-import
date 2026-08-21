// ============================================================
// [XPlatform 원본 스크립트 - 주석으로 보존]
// ============================================================
// var gvTabServicePath = "TabForm::Detail.xfdl";
// 
// ============================================================

// ============================================================
// [WebSquare 변환 스크립트 - 1차 변환]
// 운영 적용 전에 TODO 표시 항목을 확인하세요.
// ============================================================
var scwin = (typeof scwin === "undefined") ? {} : scwin;

// TODO: WebSquare submission을 구성한 뒤 이 호환 함수를 교체하세요.
scwin.xpTransaction = function() {
    console.warn("[마이그레이션 TODO] XPlatform transaction 호출을 WebSquare submission으로 변환해야 합니다.", arguments);
};

var gvTabServicePath = "TabForm::Detail.xfdl";
