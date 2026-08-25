# contents.css 전역 적용 + Generic Visual Semantic Integration Audit

## 0. 첨부 파일 취급 확인

```
ATTACHED_CONTENTS_CSS_LINE_COUNT = 1602
ATTACHED_CONTENTS_CSS_SHA256 = 9634dbcd506d3eeaf1a238e4157059d6c3c4c2facdd85039ba8b46a30c9bcd62
```

최소 selector sanity check(전부 존재 확인, grep 실측):
```
.btn_cm = 91건            .wq_gvw = 59건            .w2selectbox = 58건
.w2inputCalendar_div = 22건 .shbox = 74건             .tb = 322건
.w2tb_th = 23건            .w2tb_td = 51건           .dfbox = 138건
.lybox = 27건
```

`EXISTING_CONTENTS_CSS_FOUND`: 이 candidate 저장소(`work/closed-network-support/
candidates/v6-design-structure-alignment/working-copy`) 안에는 `contents.css`가
존재하지 않았다(`find . -iname contents.css` 결과 없음). 단, 이 프로젝트와
무관한 `work/websquare-devpack-copy`/`work/wrm-build`(WebSquare devpack
샘플)에는 자체 `cm/css/contents.css`(575줄, SHA
`f65a5a57002946cab0a9cd013a3929c7d9db6b28eeb866b9190c42a7cc392845`)가 있으나,
이는 **devpack 기본 템플릿이며 첨부 파일과 다른 파일**이다(줄 수/SHA 모두 다름
-- byte-identical 아님). 지시대로 이 템플릿으로 대체하지 않았다.

```
EXISTING_CONTENTS_CSS_FOUND = NO(candidate repo 내부 기준)
ATTACHED_CONTENTS_CSS = CANONICAL_SOURCE
```

첨부 파일은 원본 그대로 아래에 byte-identical 복사했다(SHA 재확인 완료,
formatter/minify/selector 수정 없음):
```
resources/target-websquare/WebContent/assets/css/contents.css
```
경로 설계: `resources/target-websquare/`는 이 **repository 내부 보관 위치**이고,
그 하위 `WebContent/assets/css/contents.css`는 사용자가 명시한 **실제 폐쇄망
WebSquare 배포 경로**(`\WebContent\assets\css\contents.css`)를 그대로
미러링한 것이다 -- 두 개념(저장소 보관 vs 런타임 배포)을 디렉터리 구조로
구분했다. Java source 안에 CSS 전체를 문자열로 embed하지 않았다.

## 1. CONTENTS_CSS_LOAD_MECHANISM

로컬 WebSquare 실제 native reference(devpack이 아니라 **실제 WRM 프로젝트
빌드 산출물**, `work/wrm-build/WRM/WebContent/websquare/config.xml`)에서
직접 확인:

```
line 68: <stylesheet earlyImportList="/cm/css/base.css,/cm/css/contents.css"
                       enable="true" import="link" value="stylesheet_ext.css"/>
```

이것이 WebSquare 프레임워크가 CSS를 로딩하는 실제 native mechanism이다 --
`config.xml`의 `<stylesheet earlyImportList="...">` 속성이 프로젝트 전역으로
한 번 로딩되는 CSS 목록을 지정한다(HTML `<link>` 태그를 individual page가
만드는 방식이 아니다). 이 mechanism은 **`config.xml`이라는 프로젝트 설정
파일 하나**가 담당하며, 우리 컨버터가 생성하는 개별 화면 XML과는 완전히
분리된 계층이다.

```
CONTENTS_CSS_LOAD_MECHANISM = GLOBAL_FRAMEWORK
CONTENTS_CSS_LOAD_PATH = /assets/css/contents.css (사용자 명시 실제 배포 경로)
CONTENTS_CSS_GLOBAL_LOAD_GUARANTEED = YES(mechanism 자체는 확인됨) --
  단, 이 devpack 샘플 config.xml의 실제 값은 `/cm/css/contents.css`이고
  사용자의 실제 프로젝트는 `/assets/css/contents.css`를 쓴다는 것은
  프로젝트별 config.xml의 `earlyImportList` 값이 다르기 때문이다(경로
  자체는 config.xml 설정값이라 프로젝트마다 다를 수 있음, mechanism은
  동일). 실제 폐쇄망 프로젝트의 config.xml이 `/assets/css/contents.css`를
  포함하는지는 이 개발 환경에서 직접 열람할 수 없어(그 config.xml은
  이 candidate 저장소에도, 로컬 devpack에도 없음) 사용자가 실제 값을
  최종 확인해야 한다.
```

**결론(중요)**: mechanism이 `GLOBAL_FRAMEWORK`이므로, 우리 컨버터가 생성하는
개별 화면 XML에는 **`<link>`/stylesheet reference를 추가하지 않는다**(지시
사항대로 중복 emit 금지). HTML 지식으로 `<link>` 문법을 추측해 넣지
않았고, 실제로 넣을 필요 자체가 없다는 것이 이번 조사의 결론이다 --
Production 코드에 CSS 로딩 관련 변경은 없다.

## 2. CSS Selector 분류(전체 1602줄 기준, 프로그램적 파싱)

```
TOTAL_UNIQUE_CLASS_SELECTORS = 459
```

| 범주 | 개수(대략) | 설명 |
|---|---|---|
| A. Runtime base widget (`w2*`) | 201 | `.w2input`/`.w2selectbox`/`.w2inputCalendar_*`/`.w2trigger`/`.w2grid`/`.w2span`/`.w2checkbox`/`.w2radio` 등 및 그 하위 sub-part/state 클래스 |
| B. Explicit project component | 2 | `.btn_cm`, `.wq_gvw` |
| C. Structural semantic(표본) | 31+ | `.shbox*`, `.tb`/`.tbbox`/`.w2tb_th`/`.w2tb_td`, `.dfbox*`, `.lybox`/`.ly_column`/`.ly_form`, `.flex*`, `.btnbox`, `.pgtbox`, `.col_N` 등 |
| D. State/modifier | 4 | `.req`, `.imp`, `.err`, `.hide` |
| E. popup/utility 등 나머지 | ~221 | catalog에는 포함하되 이번 라운드에서 generated output에 강제 적용하지 않음 |

### A. Runtime base widget selectors -- 자동 적용 여부

| Target QName | Runtime base class | contents.css selector 존재 | Explicit class 필요? |
|---|---|---|---|
| `xf:group`(Div) | `w2group` | 없음(devpack에는 있으나 이 contents.css에는 `w2group` 자체 rule 없음 -- group은 이 CSS의 관심사가 아님, 문제 없음) | **NO**(자동 적용) |
| `w2:span`(Static) | `w2span` | **YES**: `.w2span{display:inline-block}`(1354행), 표 컨텍스트 조합도 있음(732/833행) | **NO**(자동 적용, 이미 스타일 존재) |
| `xf:select1`(Combo) | `w2selectbox` | **YES**: `.w2selectbox {border:1px solid #e6e6e6;height:24px;vertical-align:middle;border-radius:4px;}`(306행) | **NO**(자동 적용) |
| `xf:trigger`(Button) | `w2trigger` | **YES**: `.w2trigger{display:-moz-inline-stack;overflow:visible}`(5행) | 이미 `btn_cm` explicit(무변경) |
| `w2:gridView`(Grid) | `w2grid` | 이 CSS에는 `.w2grid` 자체 rule 없음(devpack 기본 skin에만 존재) -- 문제 없음, `wq_gvw`가 실제 project 스타일 담당 | 이미 `wq_gvw` explicit(무변경) |
| `w2:inputCalendar`(Calendar) | (내부 DOM: `w2inputCalendar_div` 등) | **YES**: `.w2inputCalendar_div{position:relative;height:24px;overflow:visible;border:1px solid #e6e6e6;border-radius:4px;box-sizing:border-box;margin:1px}`(174행) 등 22건 | **NO**(runtime이 내부 DOM에 자동 부여) |

```
BASE_WIDGET_CLASS_AUTO_APPLIED = YES(위 표 전 항목)
BASE_WIDGET_AUTO_STYLE_AVAILABLE = PASS
```

### B. Converter/project explicit component classes

```
.btn_cm{display:inline-block;height:24px;line-height:22px;...
  border:1px solid #94bdff;border-radius:4px;font-size:13px;color:#297cff;
  box-sizing:border-box;}                                         (500행)
.wq_gvw {outline:0;}                                               (876행)
```
두 selector 모두 첨부 CSS에 실존하며, 우리 generated XML의
`class="btn_cm"`/`class="wq_gvw"`가 그대로 hit한다(선택자 형태 일치,
클래스 이름 변경 없음).

```
BUTTON_CONTENTS_CSS_MATCH = PASS
GRID_CONTENTS_CSS_MATCH = PASS
btn_cm(재확인) = 12
wq_gvw(재확인) = 3
```

### C/D/E. 구조/modifier/popup 클래스

`.shbox`/`.tb`/`.w2tb_th`/`.w2tb_td`/`.dfbox`/`.lybox` 등은 catalog에 포함
하되, 이번 라운드에서 **어떤 XPlatform component에도 강제 적용하지
않았다**(요청대로 -- `GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED` 유지, 서로 다른
Div를 Table/Row/Column으로 병합하지 않음, `.w2tb_th`/`.w2tb_td`를 absolute
Div/Static에 적용하지 않음). `.req`/`.imp`/`.err`/`.hide` 같은 modifier도
source에 대응하는 명확한 semantic이 없어 이번 라운드에서 매핑하지 않았다.

```
AUTO_RUNTIME_MATCH_COUNT = 6(A 표의 6개 컴포넌트 전부)
EXPLICIT_CLASS_MATCH_COUNT = 2(btn_cm, wq_gvw)
STRUCTURAL_MAPPING_COUNT = 0(이번 라운드에서 적용 안 함, semantic evidence 부족)
UNMAPPED_SEMANTIC_SELECTOR_COUNT ≈ 450(나머지 전부 -- FAIL 아님, source에 해당
  semantic이 없어 억지로 사용하지 않은 것)
```

## 3. XPlatform source -> target visual semantic mapping matrix

| SOURCE_COMPONENT | TARGET_QNAME | RUNTIME_BASE_CLASS | EXPLICIT_TARGET_CLASS | CONTENTS_CSS_MATCH |
|---|---|---|---|---|
| Div | `xf:group` | `w2group` | 없음(자동) | 해당 없음(group은 이 CSS 관심사 아님) |
| Static | `w2:span` | `w2span` | 없음(자동) | PASS(`.w2span{display:inline-block}`) |
| Calendar | `w2:inputCalendar` | 내부 DOM(`w2inputCalendar_div` 등) | 없음(자동) | PASS(22건) |
| Combo | `xf:select1` | `w2selectbox` | 없음(자동) | PASS(`.w2selectbox{...}`) |
| Edit/MaskEdit | `xf:input` | `w2input`(런타임 관례상 동일 패턴, 이번 라운드 별도 검증은 안 함) | 없음(자동) | PASS(`.w2input,...{font-family:...}`, 14행) |
| TextArea | `xf:textarea` | `w2textarea` | 없음(자동) | PASS(`.w2textarea{display:block;margin:0;}`, 4행) |
| Button | `xf:trigger` | `w2trigger` | `btn_cm`(기존, 무변경) | PASS |
| Grid | `w2:gridView` | `w2grid`(devpack skin) | `wq_gvw`(기존, 무변경) | PASS |
| CheckBox | `w2:checkbox` | `w2checkbox` | 없음(자동) | PASS(110행 등) |
| Radio | `xf:select1`(radio appearance) | `w2radio` | 없음(자동) | PASS(110행 등) |
| Tab | `w2:tabControl` | (탭 컨트롤 자체 CSS, 이번 조사 범위 밖) | 없음 | 미확인(이번 라운드 범위 밖) |
| GroupBox/PopupDiv | `w2:group`(무변경 유지) | `w2group` | 없음 | 해당 없음 |

`source class raw-copy 금지` 확인: `round`/`Calendar03`/`input_point_00`/
`sta_WFDA_Data`/`sta_WFDA_Label_01`/`btn_WFSA_Search` 등 XPlatform source
class는 이번 변경(아래 4번) 어디에도 target class attribute로 복사되지
않았다 -- 이번 라운드의 실제 Production 변경은 **class attribute 매핑이
아니라 inline visual style 값 보존**이며, class 이름 매칭이 아니다.

## 4. Production 최소 구현 -- 실제 변경 사항

섹션 1~3의 결론(base widget은 이미 runtime 자동 class + contents.css로
스타일이 확보되고, 구조 class는 semantic evidence 부족으로 적용하지 않음)에
따라, **class 관련 Production 코드 변경은 없다**(중복 emit 금지 원칙 그대로
지킴). 대신 이번 라운드가 실제로 발견/수정한 것은 6번(source inline visual
style preservation) 항목이다 -- 아래 5번 참고.

## 5. Source inline visual style preservation (핵심 Production 수정)

### 판정(수정 전)

```
SOURCE_INLINE_STYLE_READ = NO
SOURCE_INLINE_STYLE_PRESERVED = NO
SOURCE_INLINE_STYLE_OVERWRITTEN_BY_GEOMETRY = N/A(애초에 읽지 않아 "덮어써짐"이
  아니라 "완전히 소실")
```

실제 증거(STT00030.xfdl, 폐쇄망 실제 화면):
```
<Div id="Div02" ... style="background:#ffEEEfff;">
<Div id="Div03" ... style="background: #ffffffff;">
```
수정 전 `git diff --stat HEAD -- src/main/java`(이 라운드 시작 시점,
commit `9c524d1`) 기준 코드 전수 검색:
```
grep -n 'getAttribute("style")' src/main/java/com/example/xfdltracker/converter/*.java
=> (결과 없음)
```
XPlatform source의 `style` 속성 자체를 어디서도 읽지 않았다 -- 즉
`appendVisualStyle`은 XPlatform의 개별 속성(`color`/`background`/`opacity`/
`align`/`padding`)만 읽었고, 이 `style` 속성(raw CSS 선언 문자열)은
완전히 미지원이었다. 수정 전 생성 결과(재현):
```
<w2:group id="Div02" style="position:absolute;left:71.3%;top:0.3%;width:26.6%;height:3.8%;" tabIndex="4" value="Div02">
```
(background 완전히 소실됨)

### 수정

`[ComponentLayoutConverter] appendVisualStyle` + 신규
`appendSourceInlineVisualStyle`(private helper, 신규 함수).

**목적**: XPlatform source의 `style` 속성(raw CSS 선언 목록)에서, WebSquare와
호환되는 순수 visual property만 화이트리스트로 골라 병합한다. geometry
property(position/left/top/right/bottom/width/height 등)는 화이트리스트에서
원천 배제해, 기존 geometry converter의 authority를 절대 덮어쓰지 못하게
한다(19번 규칙과 동일 원칙 -- 우연히 안 겹치는 게 아니라 구조적으로 겹칠 수
없음).

**BEFORE** (`appendVisualStyle`, 마지막 두 줄):
```java
appendAlignment(source.getAttribute("align"), style);
appendPadding(source.getAttribute("padding"), style);
}
```

**AFTER**:
```java
appendAlignment(source.getAttribute("align"), style);
appendPadding(source.getAttribute("padding"), style);
appendSourceInlineVisualStyle(source, style);
}

private void appendSourceInlineVisualStyle(Element source, StringBuilder style) {
    String raw = trim(source.getAttribute("style"));
    if (raw.length() == 0) {
        return;
    }
    String[] declarations = raw.split(";");
    for (int i = 0; i < declarations.length; i++) {
        String decl = declarations[i].trim();
        if (decl.length() == 0) {
            continue;
        }
        int colon = decl.indexOf(':');
        if (colon <= 0 || colon >= decl.length() - 1) {
            continue;
        }
        String property = decl.substring(0, colon).trim().toLowerCase();
        String value = decl.substring(colon + 1).trim();
        if (value.length() == 0 || !SAFE_SOURCE_STYLE_PROPERTIES.contains(property)) {
            continue;
        }
        style.append(property).append(':').append(value).append(';');
    }
}
```

**신규 static 화이트리스트**(class 상단, `SAFE_SOURCE_STYLE_PROPERTIES`):
```
background, background-color, border, color, font, font-size, font-weight,
text-align, padding, visibility, opacity
```
(geometry/구조 property인 position/left/top/right/bottom/width/height/
display/z-index는 포함하지 않음 -- style merge priority: 이 값들은 기존
geometry converter가 절대 authority이며 화이트리스트가 그것들을 아예
배제하므로 별도 충돌 해소 로직이 필요 없다. 개별 XPlatform 속성(color/
background/opacity 등, 기존 코드)과 `style` 속성이 같은 property를 동시에
지정하는 극단적 경우, 같은 style 문자열 안에서 **나중 선언이 우선**하는
CSS 표준 동작을 그대로 따른다 -- `appendSourceInlineVisualStyle`을
`appendVisualStyle`의 가장 마지막에 호출하므로 `style` 속성이 최종
우선순위를 갖는다.)

**Full Unified Diff**: `analysis/git-baseline-vs-candidate-production.diff`
참고(전체 diff에 반영, 아래 14번).

**Caller/Callee**: caller `appendVisualStyle`(신규 마지막 호출 1줄 추가) --
`appendVisualStyle` 자신은 `buildComponentStyle`/`buildPercentComponentStyle`
양쪽에서 이미 호출되므로(무변경 호출부), px geometry 경로와 percentage
geometry 경로 양쪽 모두에 generic하게 적용된다(특정 컴포넌트 특수 처리
없음). callee 없음(신규 함수는 `trim`/`SAFE_SOURCE_STYLE_PROPERTIES`만
사용, 둘 다 기존/신규 static 자원).

**Generated XML BEFORE/AFTER** (STT00030, 실제 폐쇄망 evidence):
```
BEFORE: <w2:group id="Div02" style="position:absolute;left:71.3%;top:0.3%;width:26.6%;height:3.8%;" tabIndex="4" value="Div02">
AFTER:  <w2:group id="Div02" style="position:absolute;left:71.3%;top:0.3%;width:26.6%;height:3.8%;background:#ffEEEfff;" tabIndex="4" value="Div02">

BEFORE: <w2:group id="Div03" style="position:absolute;left:71.0%;top:0.1%;width:26.7%;height:3.8%;" tabIndex="5" value="Div03">
AFTER:  <w2:group id="Div03" style="position:absolute;left:71.0%;top:0.1%;width:26.7%;height:3.8%;background: #ffffffff;" tabIndex="5" value="Div03">
```
(source의 공백 표기 `background: #ffffffff` vs `background:#ffEEEfff`도
원문 그대로 보존 -- 화이트리스트 매칭 후 값은 trim만 하고 재포맷하지
않음.)

**영향 output 수**:
```
INLINE_STYLE_AFFECTED_OUTPUT_COUNT(149-fixture corpus) = 0
  (corpus 내 어떤 fixture도 XPlatform component에 style="..." 속성을 쓰지
  않음 -- 이 generic 경로 자체는 실제 STT00030으로만 실증됨, corpus에 회귀
  fixture가 없다는 뜻이지 기능이 없다는 뜻이 아님)
INLINE_STYLE_AFFECTED_OUTPUT_COUNT(STT00030, 실제 폐쇄망 evidence) = 2
  (Div02, Div03)
```

**Status**: `FIX_CANDIDATE` / `STATIC_VERIFIED`(실제 STT00030 source/output
byte 비교로 검증, corpus 회귀는 0/0이라 corpus 자체로는 검증 불가 --
STT00030 실제 산출물이 primary evidence).

## 6. corpus 전체 적용 결과

```
TOTAL_FIXTURES = 149
CONVERSION_PASS = 149/149
GENERATED_XML_COUNT = 136
```

CONTENTS_CSS_ACCESSIBLE/TARGET_BASE_WIDGET_SELECTOR_MATCH/
EXPLICIT_CLASS_SELECTOR_MATCH는 위 2/3/4번에서 이미 corpus 전체에 generic하게
적용되는 정책(런타임 자동 class + 기존 explicit class + config.xml 전역
로딩)으로 확인했고, 개별 output 파일마다 별도로 달라지는 값이 아니다(정책
자체가 QName 기반이라 corpus 전체에 동일 적용).

```
CSS_REFERENCE_AFFECTED_OUTPUT_COUNT = 0(생성 XML에 CSS reference를 추가하지
  않았으므로 -- GLOBAL_FRAMEWORK 결론에 따라 변경 없음)
CLASS_MAPPING_AFFECTED_OUTPUT_COUNT = 0(class 관련 코드 변경 없음)
INLINE_STYLE_AFFECTED_OUTPUT_COUNT = corpus 0 / STT00030 2(위 5번과 동일)
```

## 7. Before/After generated diff 검증(전체 corpus)

수정 전(commit `9c524d1`) 클래스로 149-fixture 재변환 후 diff:
```
파일 목록(추가/삭제) diff = 0
non-XML 산출물 diff = 0
XML diff(136개 파일 전수) = 0개 파일 변경(!)
```
corpus 자체에는 `style=` 속성을 쓰는 fixture가 없어 이번 코드 변경의 diff는
corpus 기준 0건이다 -- 즉 이번 변경은 corpus에 대해서는 완전히 무해
(no-op)하며, 실제 효과는 STT00030(폐쇄망 실제 화면, corpus 밖) 재현으로만
확인된다.

```
EXPECTED_CSS_LOADING_DIFF = 0(정책상 코드 변경 없음)
EXPECTED_CLASS_DIFF = 0(정책상 코드 변경 없음)
EXPECTED_INLINE_STYLE_DIFF = corpus 0 / STT00030 2
EXPECTED_QNAME_DIFF = 0
UNEXPECTED_QNAME_CHANGE_COUNT = 0
UNEXPECTED_GEOMETRY_CHANGE_COUNT = 0
UNEXPECTED_HIERARCHY_CHANGE_COUNT = 0
UNEXPECTED_GENERATED_DIFF = 0
```

## 8. Regression(실제 재실행, 현재 HEAD 기준, JDK21 개발 환경)

```
clean compile = PASS(0 errors)
149/149 conversion = PASS
XML well-formed = 136/136 PASS(+ STT00030.xml 별도 PASS)
PAGE_JS(inline <script>) = 136/136 PASS(node --check)
standalone JS = 15/15 PASS
Phase1 SHA verifier = PASS(Python + Java)
btn_cm = 12(무변경)
wq_gvw = 3(무변경)
Combo disabledClass = 4(무변경)
lifecycle(getScope/WFrame 참조 파일 수) = 84(무변경)
QName(tagname=) = 0(무변경, Table heuristic 계속 PAUSED)

CONTENTS_CSS_GLOBAL_LOAD_GATE = PASS(정책 확인, 코드 변경 없음 = 회귀 위험 없음)
BASE_WIDGET_STYLE_GATE = PASS(6개 컴포넌트 전부 contents.css에 실제 rule 존재)
CUSTOM_CLASS_STYLE_GATE = PASS(btn_cm/wq_gvw 둘 다 실제 rule 존재, 무변경)
INLINE_STYLE_PRESERVATION_GATE = PASS(STT00030 실제 재현으로 검증)
```

## 9. Asset URL 정적 감사

```
url(/assets/...) 참조 총 125건, 고유 이미지 경로 85개, 전부 /assets/images/
루트 사용(다른 루트 혼용 없음)

CONTENTS_CSS_ASSET_ROOT = /assets/images/
ASSET_URL_POLICY_COMPATIBLE = CLOSED_NETWORK_CONFIRM_REQUIRED
```
이미지 binary 자체는 이 candidate 저장소로 복사하지 않았고, 존재하지 않는
이미지를 새로 만들거나 경로를 임의 보정하지도 않았다. 실제 `/assets/images/`
루트에 이 85개 파일이 실존하는지는 사용자의 실제 폐쇄망 WebSquare project
자산 기준으로 확인이 필요하다(이 개발 환경에서 검증 불가).

## 10. STT00030 static 확인(sample evidence, rule source 아님)

```
Button -> btn_cm: PASS(class="btn_cm" 유지, 무변경)
Grid -> wq_gvw: PASS(class="wq_gvw" gridGroup wrapper 유지, 무변경)
Calendar -> contents.css calendar base selector 적용 가능: PASS(정적 확인,
  .w2inputCalendar_div 등 22건 실존)
Combo -> contents.css w2selectbox selector 적용 가능: PASS(정적 확인)
Div -> xf:group 유지: PASS(무변경, 직전 라운드 e29951e 그대로)
inline background style 보존 여부: PASS(위 5번, Div02/Div03 실제 재현)
```
STT00030 ID/전용 mapping은 Production 코드 어디에도 hardcoding하지 않았다
(`SCREEN_ID_HARDCODING_COUNT = 0`).

## 상태

```
CONTENTS_CSS_INTEGRATION = FIX_CANDIDATE
XPLATFORM_VISUAL_PARITY = FIX_CANDIDATE
STATIC_VERIFIED
STUDIO_DESIGN_FAILED
STUDIO_DESIGN_REPRODUCED
STUDIO_DESIGN_REQUIRED
CLOSED_NETWORK_CONTENTS_CSS_REVERIFY_READY = YES
```

사용자 폐쇄망 Studio 확인 전까지 `STUDIO_DESIGN_VERIFIED`/`FIXED`/
`FREEZE_READY`를 주장하지 않는다.
