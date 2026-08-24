# Top-Level Percent Basis Audit (760px / 736px Quick Audit)

## 배경

폐쇄망 실제 fresh output(STT00030 계열)에서 관찰된 값:

```
grp_resultArea: width:100.0%; height:760px;
grp_main:       width:100.0%; height:736px;
GRP_MAIN_STYLE_EMPTY=NO
STYLE_WIDTH_SYNTAX=VALID
```

Studio에서는 여전히 상단 조건영역/Button 등이 보이지 않는다. 이번 라운드의 가설은
"top-level absolute percentage component의 percentage 계산 분모(760 또는 Form
선언값)와 실제 렌더링 시 containing block(grp_main, 736px)이 서로 다르다"는 것이었다.

## 판정: TOP_LEVEL_PERCENT_BASIS_MISMATCH = NO

코드 추적 결과, 이 mismatch는 현재 HEAD에 **존재하지 않는다**. 아래는 실제 소스 파일
기준(이 candidate 저장소, `src/main/java` 무변경 상태) 근거다.

### 1) grp_main 자신의 rendered height

`ComponentLayoutConverter.buildMainContentAreaStyle(Document)`
(ComponentLayoutConverter.java:441-450):

```java
public String buildMainContentAreaStyle(Document source) {
    double contentHeight = resolveContentExtentHeight(source);
    if (contentHeight <= 0.0) {
        return buildMainAreaStyle(source);
    }
    StringBuilder style = new StringBuilder();
    style.append("width:").append(formatPercent(100.0)).append(";");
    style.append("height:").append(formatNumber(contentHeight)).append("px;");
    return style.toString();
}
```

`grp_main`의 실제 rendered height는 `resolveContentExtentHeight(source)` 값 그대로다
(px, 반올림만 적용). 관찰된 736px은 바로 이 값이다.

### 2) top-level 자식(Div01/Div00/Grid01_gridGroup/Grid00_gridGroup/Div02/Div03)의
percentage 계산 분모

`WebSquareGenerator.convertLayoutAsTable`
(WebSquareGenerator.java:759-825), root Form Layout 분기(`isRootFormLayout`):

```java
double[] basis = layoutConverter.resolveLayoutBasis(layout);
if (basis == null) {
    if (inheritedBasisWidth > 0.0 && inheritedBasisHeight > 0.0) {
        basis = new double[] {inheritedBasisWidth, inheritedBasisHeight};
    } else {
        basis = layoutConverter.resolveFormBasis(layout.getOwnerDocument());
    }
}
double basisWidth = basis == null ? -1.0 : basis[0];
double basisHeight = basis == null ? -1.0 : basis[1];
if (isRootFormLayout) {
    double contentExtentHeight = layoutConverter.resolveContentExtentHeight(children);
    if (contentExtentHeight > 0.0 && (basisHeight <= 0.0 || contentExtentHeight < basisHeight)) {
        basisHeight = contentExtentHeight;
    }
}
...
if (!"TABLE_LAYOUT_HIGH_CONFIDENCE".equals(classification)) {
    convertChildren(
            out, layout, targetParent, parentPath, analysis, depth, null,
            basisWidth, basisHeight, true);
    return;
}
```

`GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED = true`이므로 root Layout은 항상 이
flat pass-through 분기(TABLE_LAYOUT_HIGH_CONFIDENCE 아님)를 탄다. 즉 root Layout의
모든 직계 자식(Div01/Div00/Grid01/Grid00/Div02/Div03 -- Grid는 `Grid01_gridGroup`
wrapper로 감싸지지만 wrapper style도 이 같은 `basisHeight`로 계산됨,
WebSquareGenerator.java:592-610)은 `basisHeight = resolveContentExtentHeight(children)`
(단, 이 값이 기존 basis보다 작을 때만 축소)로 계산된다.

### 3) 두 값이 "같은 함수, 같은 자식 목록"에서 나온다

- `buildMainContentAreaStyle` -> `resolveContentExtentHeight(Document)`
  (ComponentLayoutConverter.java:457-474) -- `findFirstElement(source, "Layout")`로
  찾은 Layout의 직계 자식들의 `max(top+height)`.
- `convertLayoutAsTable`의 root 분기 -> `resolveContentExtentHeight(children)`
  (ComponentLayoutConverter.java:482-500) -- `directElementChildren(layout)`로 이미
  확보한 **같은 Layout**의 직계 자식 목록의 `max(top+height)`.

`findFirstElement(source, "Layout")`은 `getElementsByTagName("*")` 기반 document-order
탐색이라(ComponentLayoutConverter.java:626-641) 항상 최상위(가장 얕은) Layout을 먼저
반환하며, 이는 `convertChildren`이 parentPath=""(아직 어떤 Div도 거치지 않음) 상태로
만나는 첫 Layout과 동일 element다(하위 Div의 nested Layout은 document order상 더
늦게 나타남). 즉 두 계산은 **같은 Layout 엘리먼트, 같은 직계 자식 목록**에 대해
같은 함수(`resolveContentExtentHeight`)를 호출하므로, 우연한 숫자 일치가 아니라
구조적으로 항상 같은 값을 낸다.

## 결론

```
TOP_LEVEL_PERCENT_CALCULATION_BASIS = resolveContentExtentHeight(root Layout 직계 자식)
TOP_LEVEL_RENDERED_PERCENT_BASIS    = resolveContentExtentHeight(root Layout 직계 자식) (동일 호출)
TOP_LEVEL_PERCENT_BASIS_MISMATCH    = NO
```

관찰된 736px(grp_main)이 정확히 이 값이며, top-level 자식(Div01/Div00/
Grid01_gridGroup/Grid00_gridGroup/Div02/Div03)의 top%/height%도 같은 736px을
분모로 계산된다(코드상 구조적 보장, `commit 19318a5 "fix: 중첩 그룹 높이 기준과
760px 화면 배치 수정"`에서 도입, 이번 candidate 세션 동안 `src/main/java` 무변경
이므로 현재 HEAD에도 그대로 적용됨).

이 판정 기준(`## 2. 판정 기준`)이 요구하는 "실제 숫자로 역산" 비교는 개별
Div/Grid의 실제 source top/height px 원본 수치가 필요하나, 그 값은 폐쇄망 실제
STT00030 fresh output/source XFDL에만 존재하며 이 개발 환경(이 candidate 저장소)에는
해당 실제 업무 화면 fixture가 없다(corpus에 STT00030 계열 fixture 없음, 확인:
`find . -iname "*STT00030*"` 결과 없음). 따라서 "예시 숫자(760 vs 736 두 후보 중
Grid00 실제 rectangle과 일치하는 쪽)"의 직접 대조는 이 환경에서 수행할 수 없다.
다만 위 코드 추적은 숫자 일치 여부를 관찰이 아니라 **호출 그래프 구조로 증명**하므로
(같은 함수, 같은 인자), 개별 실측치 없이도 mismatch 없음을 확정할 수 있다.

## Grid00 sanity check에 대해

`## 3. Grid00 sanity check`가 요구하는 top:22.3%/height:77.7% 같은 실제 값은 이
환경에 없다(위와 동일한 이유). 다만 구조적 보장에 따라: Grid00_gridGroup의
top%/height%는 `basisHeight=736`(root 기준, content-extent shrink 적용)으로
계산되고, grp_main의 실제 rendered height도 동일하게 736px이므로, **어느 후보값을
가정하든 계산 분모와 렌더링 분모는 항상 736으로 일치**한다(760으로 계산되고
736으로 렌더링되는 경우는 현재 코드 경로상 발생할 수 없다 -- `isRootFormLayout`
분기가 무조건 shrink를 적용하기 때문).

## Production 변경 여부

`TOP_LEVEL_PERCENT_BASIS_MISMATCH = NO`이므로 이번 라운드의 명시적 조건("mismatch일
때만 수정")에 따라 **Production Java는 변경하지 않았다**(`NO_PRODUCTION_CODE_CHANGE`).

## 남은 Studio 실패의 root cause에 대해

Studio에서 상단 조건영역/Button이 여전히 안 보이는 현상의 원인은 이번 라운드가
확정하려던 "top-level percent 분모 불일치"가 아니다(위에서 반증). 즉 이번 조사로
그 가설은 기각되었고, 실제 root cause는 이 라운드의 범위(Form -> grp_main direct
child basis) 밖에 있다. 이 문서는 그 다른 원인을 특정하지 않는다(추측 금지 원칙에
따라 코드로 반증 가능한 것만 판정). 다음 조사 후보(코드에서 관찰됐으나 이번
라운드에서 검증/수정하지 않은 것):

- `buildRootStyle`/`buildMainAreaStyle`/`buildMainContentAreaStyle` 모두
  `position:relative`를 emit하지 않는다(ComponentLayoutConverter.java:69,
  "Studio 실측 확정(ISSUE-20260818-001)"로 문서화된 기존 결정 -- 이번 라운드에서
  재검토하지 않음, 임의로 되돌리지 않음).
- Grid00/Grid01(Grid group wrapper) 내부 `w2:gridView` 자체의 native 렌더링
  최소 크기/DataSet 바인딩 여부(이번 감사 범위 밖).

## 검증 상태

```
XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE(해당 없음 -- 이번 라운드는 기각 판정,
  코드 변경 없음)
STATIC_VERIFIED (코드 추적만 수행, 실제 STT00030 fresh output 개별 자식 수치는
  이 환경에서 접근 불가)
STUDIO_DESIGN_REQUIRED (이번 판정과 무관하게 유효, Studio 재현 시 남은 root
  cause를 별도 라운드에서 추적 필요)
```
