// ============================================================
// [XPlatform 원본 스크립트 - 주석으로 보존]
// ============================================================
// function gfnShadow()
// {
//     var btnSame = { value:"LOCAL" };
//     trace(btnSame.value);
//     this.btnSame.value = "SCREEN";
// }
// function gfnDatasetShadow()
// {
//     var dsMain = { getColumn:function(r,c){ return "local"; } };
//     trace(dsMain.getColumn(0, "NAME"));
//     trace(this.dsMain.getColumn(0, "NAME"));
// }
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

scwin.gfnShadow = function()
{
    var btnSame = { value:"LOCAL" };
    console.log(btnSame.value);
    this.btnSame.value = "SCREEN";
}
scwin.gfnDatasetShadow = function()
{
    var dsMain = { getColumn:function(r, c){ return "local"; } };
    console.log(dsMain.getColumn(0, "NAME"));
    console.log(this.dsMain.getColumn(0, "NAME"));
}
