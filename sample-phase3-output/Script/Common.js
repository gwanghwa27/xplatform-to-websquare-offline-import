// ============================================================
// [XPlatform 원본 스크립트 - 주석으로 보존]
// ============================================================
// include "Utility.xjs";
// var gvCommonPrefix:String = "COMMON";
// function gfnHello()
// {
//     trace("hello");
// }
// function gfnA()
// {
//     gfnB();
// }
// function gfnUser()
// {
//     return gvUserId;
// }
// function gfnUnused()
// {
//     trace("unused");
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

// [XPlatform include 제거] Utility.xjs
var gvCommonPrefix = "COMMON";
scwin.gfnHello = function()
{
    console.log("hello");
}
scwin.gfnA = function()
{
    gfnB();
}
scwin.gfnUser = function()
{
    return gvUserId;
}
scwin.gfnUnused = function()
{
    console.log("unused");
}
