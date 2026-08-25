# Tab Runtime Script Injection 조사 (Production 변경 없음)

## 배경

사용자가 첨부한 8번 이미지는 Tab 외부 콘텐츠(STT00080_TSub01.xfdl,
STT00080_TSub02.xfdl 등)를 쓰는 화면의 `<script>`에서 `"scwin.
__xpComponentIdMap"`, `"scwin.__xpTabRuntime = (function() {...})"`로
시작하는 대형 runtime script를 지목하며, 생성 원인과 항상 필요한지를
물었다.

## 1. 실제 call path (코드 추적)

### 1-1. 주입 조건 (단일 지점, 이미 중앙화됨)

```
POLICY_DEFINITION:  TabRuntimePlan.isRuntimeRequired()
  = bridgeTarget || !operations.isEmpty() || !crossScreenReferences.isEmpty()
    || !scopeBridgeReferences.isEmpty()
POLICY_CALLER (script 본문 rewrite):
  WebSquareScriptConverter.convert(..., TabRuntimePlan) 74~88행
  -- isRuntimeRequired() true일 때만 TabRuntimeScriptConverter/
     CrossScreenScriptConverter/ScopeBridgeScriptConverter로 원본 JS의
     .set_url()/.addTabpage()/... 호출부를 scwin.__xpTabRuntime.* 호출로
     치환
POLICY_CALLER (library 자체 주입):
  WebSquareGenerator.java 375~377행
  -- 같은 isRuntimeRequired() true일 때만
     TabRuntimeScriptGenerator.generate(...)로 큰 runtime script 본문을
     생성해 <script> 뒤에 붙임
XML_ATTRIBUTE_WRITER: 없음(속성이 아니라 <script> 텍스트 삽입)
GENERATED_XML: <script>...(원본 변환 코드)...\n(runtime script)...</script>
```

두 주입 지점(스크립트 치환, 라이브러리 삽입)이 **동일한
`isRuntimeRequired()` 판정 하나**를 공유하므로 "라이브러리는 들어갔는데
호출부는 옛날 API를 그대로 쓴다"(또는 반대) 같은 불일치는 구조적으로
발생하지 않는다 -- 이미 단일 정책 지점으로 설계돼 있었다(이번 라운드
이전부터).

### 1-2. `isRuntimeRequired()`를 구성하는 4개 조건의 실제 의미

`TabOperationAnalyzer`가 스크립트에서 다음 7개 XPlatform Tab API 호출을
스캔한다(`METHODS` 상수): `set_url`, `addTabpage`, `insertTabpage`,
`removeTabpage`, `set_tabindex`, `set_tabpageindex`, `set_selectedindex`.
이들은 `TabOperation.Type`(`SET_URL`/`ADD_PAGE`/`INSERT_PAGE`/
`REMOVE_PAGE`/`SELECT_PAGE`) 중 하나로 분류되어 `operations`에 쌓인다.
receiver가 실제로 Tab 컴포넌트를 가리키는지도 `isScreenTabReceiver`로
확인한다(아무 변수의 `.set_url` 호출이 아니라, 실제 Tab 참조로 좁혀짐).

- `bridgeTarget`: 이 화면 **자신이** 다른 화면의 Tab 안에 외부 XFDL로
  로드되는 대상인 경우(자신의 onload 완료를 부모에게 알려야 함).
- `SET_URL`/`ADD_PAGE`/`INSERT_PAGE`/`REMOVE_PAGE`: 동적으로 tab
  content를 바꾸는 연산 -- 실제로 WFrame 비동기 로드/교체가 필요하다.
- `crossScreenReferences`/`scopeBridgeReferences`: 스크립트가 다른
  화면(다른 Tab의 자식 페이지)의 컴포넌트/데이터셋을 직접 참조하는 경우.
- `SELECT_PAGE`(`set_tabindex`/`set_tabpageindex`/`set_selectedindex`):
  **이미 로드된** tabpage 중 하나를 활성화하는 단순 index 전환.

### 1-3. SELECT_PAGE의 실제 변환 결과 (재검토 대상)

`TabRuntimeScriptConverter.java` 48행:
```java
if(op.getType()==TabOperation.Type.SELECT_PAGE){
    String ref=a.length>0?a[0]:page;
    return "scwin.__xpTabRuntime.selectPage("+tab+","+ref+")";
}
```
`selectPage`는 `TabRuntimeScriptGenerator.java` 91행에서
`resolveTab`/`enqueue`/`pageRef`/`stateFor`/`syncSelected` 등 전체 상태
기계를 거쳐 `tab.setSelectedTabIndex(ref)`를 호출한다 -- 즉 **단순 index
전환(SELECT_PAGE)만 있는 화면도 SET_URL/ADD_PAGE와 동일하게 전체 대형
runtime script가 주입된다.**

## 2. 필요/불필요 조건 분리

```
확실히 필요:      bridgeTarget=true, 또는 operations에 SET_URL/ADD_PAGE/
                   INSERT_PAGE/REMOVE_PAGE가 1건이라도 있음, 또는
                   crossScreenReferences/scopeBridgeReferences 존재
                   (외부 XFDL 비동기 로드·교체·다른 화면 참조가 실제로
                   일어남 -- READY_TIMEOUT_MS/generation tracking 등
                   비동기 상태 관리가 실질적으로 필요)
잠재적으로 과함:   operations가 SELECT_PAGE만으로 구성되고(SET_URL 등
                   없음) bridgeTarget/crossScreenReferences/
                   scopeBridgeReferences가 전부 비어 있는 경우 -- 이
                   경우 tab의 모든 page가 이미 정적으로 로드돼 있다면
                   (전부 인라인 Tabpage, 외부 XFDL 없음) 단순
                   `tab.setSelectedTabIndex(idx)` 직접 호출만으로
                   충분할 가능성이 있다.
```

## 3. 이번 라운드에서 수정하지 않은 이유 (명시적 판단)

"SELECT_PAGE만 있는 화면은 대형 script를 skip해도 된다"는 조건을 안전하게
generic gating 규칙으로 만들려면 추가로 확인해야 할 것이 있다:

1. 같은 Tab 안에 **정적 Tabpage와 외부 XFDL 페이지가 섞여 있는 경우**
   (`Tab` mapping 주석: "inline Tabpage and static external XFDL url are
   converted; dynamic/mixed runtime behavior requires review" -- 이미
   PARTIAL로 표시돼 있는 영역), SELECT_PAGE가 외부 XFDL 페이지를
   가리키면 그 페이지의 비동기 로드 완료를 기다려야 하므로 단순
   `setSelectedTabIndex` 직접 호출로는 깨질 수 있다.
2. 이를 안전하게 구분하려면 "이 Tab의 모든 page가 정적/인라인인가"를
   `TabContentPlan`/`TabRuntimePlan`에서 신뢰성 있게 판별하는 로직이
   필요한데, 현재 이 판별을 화면 전체 단위가 아니라 **Tab-of-interest
   단위**로 정확히 좁히는 기존 API가 없다(있는 것은 화면 전체
   `isRuntimeRequired()` 하나뿐).
3. 이 조건을 잘못 판단해 skip하면, 외부 XFDL이 섞인 화면에서 tab
   전환이 로드 완료 전에 실행되어 **깨지는 화면이 생길 위험**이 있다 --
   사용자가 명시적으로 금지한 "무조건 제거"에 해당하는 위험이다.
4. 반대로 지금처럼 항상 포함시키는 현재 동작은 **기능적으로 틀리지
   않다**(과설계/코드 크기 문제일 뿐, 정확성 문제가 아님) -- 즉
   "고쳐야 할 버그"가 아니라 "최적화 여지"다.

이번 라운드는 정확성 문제(Radio renderType)에는 이미 조치했고, 이
최적화 여지는 근거가 화면-Tab 단위 판별 API 부재로 아직 불완전하므로,
사용자의 "무조건 제거 금지" 원칙에 따라 **수정하지 않고 조사 결과만
보고**한다.

## 4. Skip 가능 조건 제안 (구현은 이번 라운드에 하지 않음, 향후 근거 보강 시 참고)

향후 다음 조건을 **전부** 만족할 때만 대형 script를 skip하고
`tab.setSelectedTabIndex()` 직접 호출로 대체하는 것을 고려할 수 있다:

```
1. bridgeTarget = false (이 화면 자신이 외부 bridge 대상이 아님)
2. crossScreenReferences/scopeBridgeReferences 모두 비어 있음
3. 화면 내 모든 Tab의 모든 Tabpage가 정적/인라인(외부 XFDL 참조 0건)
4. operations 중 SET_URL/ADD_PAGE/INSERT_PAGE/REMOVE_PAGE 0건
   (SELECT_PAGE만 존재하거나 operations 자체가 없음)
```

3번을 신뢰성 있게 판별하는 근거(Tab 단위 정적/동적 혼재 여부 API)가
보강되기 전까지는 스코프 밖으로 둔다.

## 결론

```
TAB_RUNTIME_SCRIPT_POLICY = ROOT_CAUSE_IDENTIFIED
  (생성 원인: TabRuntimePlan.isRuntimeRequired() 단일 게이트, 이미
  중앙화됨. SELECT_PAGE-only 화면에 대한 축소 여지가 있으나, 안전하게
  판별할 API가 아직 없어 이번 라운드에는 구현하지 않음 -- "무조건 제거
  금지" 원칙 준수.)
PRODUCTION_CHANGE = NONE(이 항목에 대해서는 조사만, 코드 변경 없음)
```
