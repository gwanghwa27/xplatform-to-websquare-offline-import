# Absolute Containing Block Quick Audit (grp_main position:relative fix)

## 배경

이전 라운드([top-level-percent-basis-audit.md](top-level-percent-basis-audit.md))에서
"top-level absolute child의 percentage 계산 분모(736px)와 grp_main의 실제 rendered
height(736px)는 코드상 항상 일치한다"는 것을 확인했다. 하지만 그 판정은 XML
parent(`grp_main`)와 실제 CSS containing block이 같다는 **가정**에 의존했고, 그
가정 자체는 검증하지 않았다. 이번 라운드는 그 가정만 검증한다.

## 판정: ACTUAL_CSS_CONTAINING_BLOCK = BODY (수정 전), GRP_MAIN (수정 후)

### 0) 요약 (근거 파일/selector 명시)

| 엘리먼트 | position (수정 전) | position (수정 후) | 근거 |
|---|---|---|---|
| `body` | `relative` | `relative`(무변경) | `work/websquare-devpack-copy/tomcat/webapps/ROOT/websquare/_websquare_/skin/stylesheet.css` 1행, selector `body` -- `body{height:100%;margin:0;padding:0;font:...;position:relative}` |
| `.w2group`(프레임워크 기본 클래스) | 미선언(`static`) | 미선언(`static`, 무변경) | 동일 파일 selector `.w2group` -- `.w2group{background-color:#fff}`(position 규칙 없음); 동일 규칙이 `work/websquare-devpack-copy/tomcat/webapps/ROOT/websquare/_websquare_/uiplugin/group/group.css`에도 존재(`.w2group{background-color:#ffffff}`) |
| `grp_resultArea`(xf:group, `.w2group` 상속) | 미선언(`static`) | 미선언(`static`, 무변경) | inline style: `ComponentLayoutConverter.buildMainAreaStyle`(무수정, `position` 미emit) |
| `grp_main`(xf:group, `.w2group` 상속) | 미선언(`static`) | **`relative`(inline)** | inline style: `ComponentLayoutConverter.buildMainContentAreaStyle`(이번 라운드 수정, `position:relative;`를 두 반환 경로 모두에 emit) -- commit `defe9dc` |

`CSS_CONTAINING_BLOCK_EVIDENCE = STATIC_VERIFIED` (로컬 WebSquare devpack의 실제
CSS 파일 + 코드 inline style 확인. 실제 폐쇄망 Studio 렌더링에서 재확인은 별개.)

### 1) 생성 구조

`WebSquareGenerator.appendBody`(WebSquareGenerator.java:396-462):

```
body (XHTML "body" 태그, WebSquareGenerator.java:182 createElementNS(NS_XHTML, "body"))
  -> grp_resultArea (xf:group, buildMainAreaStyle)
    -> grp_main (xf:group, buildMainContentAreaStyle)
      -> top-level absolute children (Div01/Div00/Grid01_gridGroup/... )
```

### 2) 각 ancestor의 실제 CSS 근거 (로컬 WebSquare devpack 실측)

이 저장소에 실제 WebSquare devpack이 존재한다
(`work/websquare-devpack-copy/tomcat/webapps/ROOT/websquare/_websquare_/`). 이 라운드는
그 안의 실제 CSS/skin 파일을 근거로 사용했다(추측 아님):

- `skin/stylesheet.css` 최상단:
  `html{height:100%;overflow:auto}body{height:100%;margin:0;padding:0;font:12px
  Dotum,Helvetica,AppleGothic,Sans-serif;position:relative}`
  -> **실제 HTML `<body>` 태그는 프레임워크 기본 CSS로 이미 `position:relative`다.**
  WebSquareGenerator가 만드는 `body` 엘리먼트는 XHTML 네임스페이스의 진짜 `<body>`
  태그이므로(하위 `xf:group`처럼 런타임이 다른 태그로 재작성하는 컴포넌트가 아님)
  이 규칙이 그대로 적용된다.
- `skin/stylesheet.css` / `uiplugin/group/group.css`의 `.w2group` 규칙:
  `.w2group{background-color:#fff}` -- **position 선언이 없다(static, 기본값).**
  `xf:group`(grp_resultArea, grp_main 포함)이 런타임에서 얻는 기본 클래스가
  `w2group`이므로(엔진 JS의 group 렌더링 코드에서 `class='w2group ...'`로 태그를
  생성, `wbd_B5170_babel_main.js` 확인), 이 컴포넌트는 inline style에 `position`을
  명시하지 않는 한 항상 `position:static`이다.
- inline style: `buildMainAreaStyle`(grp_resultArea)과 (수정 전) `buildMainContentAreaStyle`
  (grp_main) 둘 다 `position`을 emit하지 않았다(코드 확인, ComponentLayoutConverter.java).

### 3) CSS 표준에 따른 결론(수정 전)

`position:absolute` 엘리먼트의 containing block은 **가장 가까운 positioned
ancestor**(position이 static이 아닌 조상)이다. 위 evidence로 체인을 따라가면:

```
body           position:relative  <- positioned
  grp_resultArea position:static  (미emit, .w2group 기본값)
    grp_main     position:static  (미emit, .w2group 기본값)   <- 수정 전
      child      position:absolute
```

`grp_resultArea`/`grp_main` 둘 다 positioned가 아니므로, 가장 가까운 positioned
ancestor는 **body**다. 즉 top-level absolute child의 실제 containing block은
`grp_main`(736px)이 아니라 `body`(실제 height는 프레임워크 CSS `height:100%`에 따라
뷰포트/Design Canvas 전체 -- 736px과 무관한 값)였다.

```
CALCULATION_BASIS         = 736px  (resolveContentExtentHeight, 이전 라운드에서 확정)
ACTUAL_RENDERED_BASIS(수정 전) = body actual height (뷰포트 기준, 736px과 무관)
TOP_LEVEL_PERCENT_BASIS_MISMATCH(수정 전) = YES
```

이것이 "상단 조건영역/Button 등이 여전히 안 보임"의 유력한 root cause다:
top:N.N%/height:N.N%가 736px 기준으로 계산됐지만, 실제로는 body(대개 훨씬 큰 뷰포트
높이) 기준으로 배치되어 화면 밖 또는 예상과 다른 위치로 렌더링된다.

## 수정

`ComponentLayoutConverter.buildMainContentAreaStyle`(grp_main 전용, grp_resultArea에는
영향 없음)의 두 반환 경로 모두에 `position:relative;`를 추가했다:

```java
public String buildMainContentAreaStyle(Document source) {
    double contentHeight = resolveContentExtentHeight(source);
    if (contentHeight <= 0.0) {
        return "position:relative;" + buildMainAreaStyle(source);
    }
    StringBuilder style = new StringBuilder();
    style.append("position:relative;");
    style.append("width:").append(formatPercent(100.0)).append(";");
    style.append("height:").append(formatNumber(contentHeight)).append("px;");
    return style.toString();
}
```

`buildMainAreaStyle`(grp_resultArea 전용) 자체는 무수정 -- 전역 position 변경이
아니라 grp_main 하나에만 국소적으로 적용된다.

```
CALCULATION_BASIS      = 736px
ACTUAL_RENDERED_BASIS(수정 후) = grp_main 자신의 rendered height = 736px
                          (position:relative이므로 grp_main이 자신의 absolute
                          자식들의 containing block이 됨)
TOP_LEVEL_PERCENT_BASIS_MISMATCH(수정 후) = NO
```

## Regression (이 개발 환경, JDK21 -- 폐쇄망 정식 JDK 1.8.0_111 아님, build-provenance.txt와
동일한 caveat)

- Clean compile: PASS (76 source files, 0 errors)
- Fresh conversion: PASS (성공=149, 실패=0)
- Generated XML count: 136/136 (기대치와 일치)
- XML well-formedness: 136/136 PASS(Python `xml.dom.minidom` 파싱)
- `grp_main style="position:relative..."`: 136/136 존재
- Before/after diff (수정 전 rebuild와 비교): **135/136 파일에서 정확히 1줄만 변경**
  (`grp_main`의 style 속성에 `position:relative;` 추가), 나머지 1개
  (`runtime/xplatform-tab-empty.xml`)는 기존에 문서화된 고정 placeholder 예외라 애초에
  `buildMainContentAreaStyle` 경로를 타지 않음 -- 무변경(기대대로).
- 파일 목록(추가/삭제) diff: 0(파일 집합 완전 동일)
- non-XML 산출물 diff: 0
- `UNEXPECTED_GENERATED_DIFF = 0`

이로써 이번 수정이 의도한 grp_main style 한 줄 외에 다른 어떤 산출물도 바꾸지 않았음을
확인했다(percentage precision, Table heuristic, Grid/Grid column, QName/class, nested
child basis 전부 무변경).

## Production 변경

- `src/main/java/com/example/xfdltracker/converter/ComponentLayoutConverter.java`
  `buildMainContentAreaStyle` 함수만 수정(위 diff).
- `analysis/git-baseline-vs-candidate-production.diff`,
  `analysis/freeze-vs-candidate-production.diff` 갱신(이번 변경 반영).
- `analysis/baseline-zip-vs-candidate-production.diff`는 이번 라운드에서 갱신하지
  않았다 -- 그 diff의 기준(BASELINE-ZIP, 최초 원본 프로젝트 zip)을 이 개발 환경에서
  추출/보유하고 있지 않아 재생성할 수 없다(추측 생성 금지). 필요 시 별도 라운드에서
  원본 zip을 확보해 갱신 필요.

## 검증 상태

```
XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE
STATIC_VERIFIED (로컬 WebSquare devpack CSS 실측 + 코드/산출물 regression 확인,
  실제 폐쇄망 Studio 렌더링 확인 아님)
STUDIO_DESIGN_REQUIRED (실제 화면에서 조건영역/Button이 보이는지는 폐쇄망 Studio에서
  최종 확인 필요)
```

Studio 확인 전 `FIXED`/`STUDIO_DESIGN_VERIFIED`를 주장하지 않는다.
