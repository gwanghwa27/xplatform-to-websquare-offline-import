# CSV 기반 WebSquare Native Class Applicability Audit (Quick)

## 배경

사용자가 실제 v6 native 화면에서 추출한 `frame_components_websquare_v2.csv`
(2245 rows, 컴포넌트 태그/ID/Class 속성/Style 속성)를 근거로, XPlatform source
class(`round`/`Calendar03`/`input_point_00`/`sta_WFDA_Data`/`sta_WFDA_Label_01`
-- CSV에 0건 확인)를 그대로 복사하지 않고, CSV에 실제 나타난 WebSquare native
class(`w2group`/`w2tb_td`/`w2textbox`/`w2tb_th`/`w2selectbox`/`w2inputbox`/
`w2trigger`/`w2grid`)가 현재 converter에 안전하게 적용 가능한지 확인한다.
이번 라운드는 **class 후보 확정 + local 증거 수집만** 하며 Production 코드는
변경하지 않는다.

## CSV 재확인 (사용자 통계 검증)

```
group:      994 (w2tb_td-family 585, w2group-family 409) -- 사용자 수치와 일치
textbox:    330 (w2textbox-family 199, w2tb_th-family 131) -- 일치
inputbox:   245 (w2inputbox 100%) -- 일치
selectbox:  171 (w2selectbox 100%) -- 일치
trigger:    156 (w2trigger 100%) -- 일치
grid:        69 (w2grid 100%) -- 일치
round / Calendar03 / input_point_00 / sta_WFDA_Data / sta_WFDA_Label_01 = 0건
```
(family = base class + 정렬/required/readOnly 등 modifier token 조합, 예:
`w2group tac`/`w2group req`도 `w2group` family로 집계)

## 1. Local WebSquare devpack CSS 확인

`work/websquare-devpack-copy/tomcat/webapps/ROOT/websquare/_websquare_/`
(core `skin/stylesheet.css`, `skin/stylesheet_ext.css`, `theme/blue/skin/
stylesheet_ext.css` 포함) 실제 파일 근거:

| Selector | TARGET_CLASS_EXISTS | VISUAL_PROPERTIES | LAYOUT_AFFECTING_PROPERTIES |
|---|---|---|---|
| `.w2group` | YES | `background-color:#fff` | 없음 |
| `.w2tb_td` | YES | border/vertical-align/padding(여러 context-dependent 변형, `.w2tb`/crosstab 전용) | `height`, `position:relative`(adaptive-layout 변형에서만), `border-top/bottom/right` -- **테이블 셀 전용, 컨텍스트(`.w2tb` 부모) 필요** |
| `.w2textbox` | YES(단 core skin에는 `.w2textbox_tooltip`만 있고 base `.w2textbox`는 theme 파일에만 존재) | **없음(빈 규칙)** | **없음(빈 규칙)** -- `theme/blue/skin/stylesheet_ext.css`: `.w2textbox { }` |
| `.w2tb_th` | YES | `font-weight:bold`, `background:#f1f1f1`, `text-align:center` | `padding:3px 10px`, `width:96px`(crosstab 변형) -- **테이블 헤더 셀 전용 스타일(굵게+중앙정렬+회색배경)** |
| `.w2selectbox` | YES | `border:1px solid #b3b3b3`, `background-color:#fff` | `display:inline-block`, `vertical-align:middle`, `margin:0`, `padding:0` |
| `.w2inputbox` | **NO** | 해당 없음 | 해당 없음 -- core CSS/theme CSS/runtime JS 전체(`wbd_B5170_babel_main.js`)에서 단 한 곳도 발견되지 않음 |
| `.w2trigger` | YES | `border:1px solid #b3b3b3` | `cursor:pointer`, `vertical-align:middle`, `padding:0`, `margin:0` |
| `.w2grid` | YES | `background-color:#fff` | `position:relative` |
| `.btn_cm` | YES(project 전용 `cm/css/base.css`, framework skin 아님) | `color:#6A6C6F`, `font-weight:bold`, `background`(hover/active 상태별) | `display:inline-block`, `height:26px`, `padding:0 8px`, `margin:0 4px 0 0` |
| `.wq_gvw` | YES(project 전용 `cm/css/base.css`) | `background:#fff`, `border:1px solid #CED4DA` | `outline:0`, `box-sizing:border-box` |

`.btn_cm`/`.wq_gvw`는 framework 기본 skin이 아니라 이 devpack의 프로젝트 전용
공통 CSS(`cm/css/base.css`)에 정의돼 있다 -- 이미 Production이 사용 중인
class이며, 이번 라운드에서 변경하지 않는다(보호 대상, section 3.E).

## 2. Runtime auto-class 여부

`wbd_B5170_babel_main.js`의 실제 태그 생성 코드(각 위젯의 `htmlStr`/`push`
문자열, 예: `class='w2group "+this.options.className+"'`)와, XForms 태그
local-name -> `pluginName` 매핑 테이블(`{secret:"input",selectbox:"select1",
radio:"select1",...}`)을 근거로 확인:

| Source -> Target | pluginName 매핑 | 자동 emit 문자열 | 판정 |
|---|---|---|---|
| `xf:group`/`w2:group` -> group | `pluginName:"group"` | `class='w2group ...'` | `BASE_CLASS_AUTO_APPLIED = YES` |
| `w2:span`(Static 현재 target) -> **span**(주의: textbox 아님) | `pluginName:"span"` | `class='w2span ...'` | `BASE_CLASS_AUTO_APPLIED = YES`(단, base class는 `w2span`이지 CSV의 `w2textbox`가 아니다 -- 아래 3.B 참고) |
| `xf:select1`(Combo 현재 target) -> selectbox | 태그->plugin 매핑 표에 `selectbox:"select1"` 명시, `pluginName:"selectbox"` | `class='w2selectbox'` | `BASE_CLASS_AUTO_APPLIED = YES` |
| `xf:trigger`(Button 현재 target) -> trigger | `pluginName:"trigger"` | `class='w2trigger ...'` | `BASE_CLASS_AUTO_APPLIED = YES` |
| `w2:gridView`(Grid 현재 target) -> gridView | `pluginName:"gridView"` | `class='w2grid "+...+"'` | `BASE_CLASS_AUTO_APPLIED = YES` |

**자동 적용되는 base class는 XML에 중복 emit하지 않는다**(원칙 유지) -- 즉
`w2group`/`w2selectbox`/`w2trigger`/`w2grid`는 이미 runtime이 알아서 붙이므로
generated XML에 명시적으로 추가할 필요가 없다(추가해도 무해하지만 무의미한
중복).

## 3. STT00030 적용 후보 판정

### A. Div01/Div00/Div02/Div03

현재 target: `xf:group`(직전 라운드 fix, commit `e29951e`).

`BASE_CLASS_AUTO_APPLIED = YES`(group plugin이 이미 `w2group`을 자동 부여).

```
DIV_NATIVE_CLASS_CANDIDATE = NONE_AUTO_APPLIED
```
명시적 `class="w2group"` 추가는 불필요(이미 runtime이 자동 부여) -- 이번
라운드에서도, 다음 라운드에서도 이 class를 XML에 emit할 필요 없음.
`w2tb_td`는 명시적으로 적용 금지(요청사항대로, 테이블 셀 전용 컨텍스트라
Div의 coordinate/absolute hierarchy와 근본적으로 맞지 않음).

### B. Static

현재 target: `w2:span`(무변경 대상).

**핵심 발견**: `w2:span`의 실제 runtime 자동 class는 `w2span`이지 CSV의
`w2textbox`가 아니다(2번 표 참고, `pluginName:"span"` != `pluginName`
`"textbox"`류). 즉 CSV의 `textbox`/`w2textbox` 994건 중 textbox 330건은
`w2:span`과는 다른 native v6 위젯(별도의 텍스트/데이터 표시 컴포넌트)에서
나온 것으로 보이며, 우리 컨버터의 `w2:span` target과 직접 대응한다는 근거가
없다. CSS 증거도 `.w2textbox{}`가 **빈 규칙**이라(theme 파일) 이 class를
추가해도 현재 devpack 기준으로는 시각적 효과가 전혀 없다.

`.w2tb_th`는 CSS 근거상 `font-weight:bold;background:#f1f1f1;text-align:
center` -- **표 헤더 셀 전용 스타일**이며, 일반 필드 라벨(`sta_WFDA_Label_01`
같은 좌측 정렬 plain text label)의 시각적 의도와 명백히 다르다. 또한
runtime JS에서 `w2tb_th`는 `class=` 로 **emit되는** 곳이 없고
`getElementsByClassName("w2tb_th")`로 **읽히기만** 한다(adaptive 표
row-height 동기화 로직 전용) -- 즉 authored 시점에 사람이 표 구조 안에서
직접 부여하는 class이지, 일반 label에 범용으로 붙이는 class가 아니다.

```
STATIC_DATA_CLASS_CANDIDATE = UNRESOLVED
STATIC_LABEL_CLASS_CANDIDATE = UNRESOLVED
```
CSV 빈도는 높지만(textbox 199건, w2tb_th 131건) component semantics/CSS
근거가 `w2:span`과 실제로 맞는다는 확증이 없어 후보로 승인하지 않는다
(요청사항의 "CSV만으로 확정하지 않는다" 원칙 그대로 적용).

### C. Combo (xf:select1)

`BASE_CLASS_AUTO_APPLIED = YES` -- 태그->plugin 매핑 표에 `selectbox:"select1"`
로 명시돼 있고, `class='w2selectbox'` 자동 emit도 확인됨. 우리 자신의 기존
Production 출력(`STT00030.xml`)에도 이미 `disabledClass="w2selectbox_disabled"`
가 있어(class 계열 이름 자체에 `w2selectbox`가 내포) 이 대응이 맞다는
corroborating evidence다.

```
COMBO_NATIVE_CLASS_CANDIDATE = NONE_AUTO_APPLIED
```
명시적 `class="w2selectbox"` 추가 불필요(이미 자동 부여).

### D. Calendar

```
CALENDAR_CLASS_FROM_CSV = EVIDENCE_INSUFFICIENT
```
요청대로 이번 라운드에서 후보를 확정하지 않는다(CSV가 noisy하다는 사용자
전제 유지, 별도 라운드 필요).

### E. Button / Grid

`btn_cm`/`wq_gvw`는 무변경(보호 대상), 이번 라운드에서 손대지 않았다.

## 4. 최종 표

| Source role | Target QName | Native class candidate | Auto-applied | CSS evidence | Apply? |
|---|---|---|---|---|---|
| Div | `xf:group` | `w2group` | YES | `background-color:#fff`만, layout 속성 없음 | **NO**(이미 자동, 중복 emit 금지) |
| Static Data | `w2:span` | (CSV `w2textbox`는 대응 근거 없음) | YES(단 실제 auto class는 `w2span`) | `.w2textbox{}` 빈 규칙 | **NO**(UNRESOLVED) |
| Static Label | `w2:span` | (CSV `w2tb_th`는 대응 근거 없음) | NO(authored-only) | 표 헤더 전용 스타일(bold/center/gray) | **NO**(UNRESOLVED, 의미 불일치) |
| Combo | `xf:select1` | `w2selectbox` | YES | `display:inline-block` 등 | **NO**(이미 자동, 중복 emit 금지) |
| Calendar | `w2:inputCalendar` | 보류 | 미확인 | 미확인 | 보류 |
| Button | `xf:trigger` | `btn_cm`(기존) | YES(`w2trigger`도 자동, `btn_cm`은 별도 explicit) | 기존 유지 | 무변경(보호) |
| Grid | `w2:gridView` | `wq_gvw`(기존) | YES(`w2grid`도 자동, `wq_gvw`는 별도 explicit) | 기존 유지 | 무변경(보호) |

```
CSV_NATIVE_CLASS_POLICY = INSUFFICIENT
```
현재 CSV+local CSS 증거만으로는 **어떤 새 class도 추가 적용할 근거가 없다**
(Div/Combo는 이미 runtime 자동 적용이라 추가가 무의미, Static Data/Label은
CSS/semantics 근거 자체가 대응되지 않아 UNRESOLVED, Calendar는 보류). 이번
라운드는 이 판정으로 종료하며 Production 변경은 없다.

## Production 변경 여부

없음. `git diff --stat HEAD -- src/main/java`가 빈 결과임을 확인했다
(`NO_PRODUCTION_CODE_CHANGE`).

## 다음 Studio A/B 후보

이번 audit에서 나온 후보 중 실제로 Studio에서 시험해볼 가치가 있는 것은
**Static Data의 `w2:span` -> CSV상 `textbox`/`w2textbox` 계열 native 위젯으로의
QNAME 자체 변경 여부**다(class 추가가 아니라, Div 라운드처럼 target QName을
바꾸는 실험). 근거: CSV에서 `textbox` 태그가 994건 중 두 번째로 큰
그룹(330건)이고, `w2:span`의 실제 auto class(`w2span`)가 이 CSV 그룹과 전혀
겹치지 않는다는 것은 -- STT00030의 `sta_WFDA_Data0N`류(값 표시용 Static)가
실제 v6 네이티브 화면에서는 `w2:span`이 아니라 다른 QName(textbox 계열
위젯)으로 authored됐을 가능성을 시사한다. 다만 이 실험은 이번 라운드
범위(class 후보 확정) 밖이므로, 별도 라운드에서 정확한 native QName을 먼저
특정(로컬 devpack에 pluginName:"textbox"류 위젯이 실제 존재하는지 확인)한
뒤 Div 라운드와 동일한 A/B diagnostic 절차로 진행해야 한다.
