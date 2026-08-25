# contents.css Generic Class/State Mapping Audit

## 0. 전제

`CONTENTS_CSS_GLOBAL_LOAD = CONFIRMED`(직전 라운드 확정, `analysis/
contents-css-integration-audit.md`). 이번 라운드는 CSS 로딩을 다시 다루지
않는다 -- `GENERATED_PAGE_CSS_LINK_REQUIRED = NO`,
`CONVERTER_CSS_LOADING_IMPLEMENTATION_REQUIRED = NO` 그대로 유지.

## 1. Hardcoded class 관련 literal 전수 inventory (PHASE A)

`class`/`className`/`cssClass`/`styleClass`/`disabledClass`/`readonlyClass`/
`focusClass`/`selectedClass`, `setAttribute("class"`/`"disabledClass"`
등을 `src/main/java` 전체에서 검색(grep, 특정 3개 문자열만이 아니라 전체):

```
HARDCODED_TARGET_CLASS_COUNT = 4
HARDCODED_STATE_CLASS_COUNT = 1
```

| LITERAL | [ClassName] method | TARGET_COMPONENT | ATTRIBUTE | CURRENT_CONDITION(수정 전) | GENERATED_XML_EXAMPLE |
|---|---|---|---|---|---|
| `btn_cm` | `[WebSquareGenerator] resolveVideoEvidenceBaseClass` | `xf:trigger`(Button) | `class` | **이미 generic**: `targetTag.equals("xf:trigger")` -- QName 기반, sourceTag 아님 | `<xf:trigger class="btn_cm" .../>` |
| `wq_gvw` | `[WebSquareGenerator] resolveVideoEvidenceBaseClass` | `w2:gridView`(Grid wrapper) | `class` | **이미 generic**: `targetTag.equals("w2:gridView")` | `<xf:group id="...gridGroup"><w2:gridView class="wq_gvw" .../></xf:group>` |
| `w2tb_tb` | `[WebSquareGenerator] convertLayoutAsTable` | table wrapper(구조적, `TABLE_LAYOUT_HIGH_CONFIDENCE` 경로 전용) | `class`,`tagname` | 리터럴이지만 이미 table-wrapper 생성 코드 한 곳에만 국한, **현재 `GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED=true`라 도달 불가(dead code)** | 해당 경로 비활성 |
| `w2tb_td` | `[WebSquareGenerator] convertLayoutAsTable`(cell 생성부) | table cell wrapper(구조적, 동일 경로) | `class`,`tagname` | 위와 동일, dead code | 해당 경로 비활성 |
| `w2selectbox_disabled` | `[WebSquareGenerator] applyComponentSpecificProperties`("Combo" 분기) | `xf:select1`(Combo) | `disabledClass` | **수정 전**: `sourceTag.equals("Combo")` 단일 branch 안에 문자열 하드코딩(`if combo then "w2selectbox_disabled"` 패턴 그대로) | `<xf:select1 appearance="minimal" disabledClass="w2selectbox_disabled" .../>` |

`w2tb_tb`/`w2tb_td`는 이번 라운드에서 건드리지 않았다 -- 이미 table-wrapper
생성 코드 한 곳에만 scoped된 구조적 literal이고(다른 곳에 흩어져 있지
않음), 현재 heuristic이 PAUSED라 실제로 생성되지 않는 dead code라
"scattered literal 문제"의 실제 사례가 아니다.

`cssclass`(XPlatform 자체 source 속성, `PropertyMappingRegistry.direct(
"cssclass","class")`를 통해 target `class`로 direct copy)는 별도(11번 참고)
-- 이것은 "class 값 하드코딩"이 아니라 "source가 명시한 값을 그대로
전달하는 기존 direct-mapping 메커니즘"이다.

## 2. class 분류

| Type | 예 | CLASS_SOURCE | 근거 |
|---|---|---|---|
| A. runtime base | `w2group`/`w2span`/`w2selectbox`/`w2trigger`/`w2grid`/`w2inputCalendar_*` | `RUNTIME_AUTO` | 직전 라운드(`websquare-native-class-applicability-audit.md`, `contents-css-integration-audit.md`)에서 runtime이 자동 emit함을 실측 확인 -- 이번 라운드에서도 explicit class로 emit하지 않음(무변경) |
| B. runtime/state | `w2selectbox_disabled` | `EXPLICIT_STATE_ATTRIBUTE` | 아래 4번 |
| C. 업무 explicit semantic | `btn_cm`/`wq_gvw` | `EXPLICIT_SEMANTIC` | v6 실제 화면 영상 판독 evidence, QName 1:1 대응(아래 3번) |
| D. modifier | `req`/`imp`/`err`/`hide` | `SEMANTIC_MODIFIER` | 이번 corpus에 대응하는 source semantic 없음 -- 미적용(HOLD) |

## 3. btn_cm / wq_gvw -- "특별 취급 제거" 재확인

코드를 실제로 읽은 결과, **이 둘은 이미 특별한 if/else 분기가 아니라
`resolveVideoEvidenceBaseClass(String targetTag)` 하나의 함수(QName ->
class lookup table)**로 구현돼 있었다(이전 라운드에서 이미 이렇게 설계됨,
v6 영상 판독 evidence 기반). 호출부는 `copyBasicProperties`
한 곳(모든 mapped component에 대해 universally 호출, sourceTag 조건 없음)
이고, `appendClassTokenIfAbsent`(dedup-safe merge helper, 기존 class 보존)
를 통해 병합한다. 즉:

```
POLICY_DEFINITION: resolveVideoEvidenceBaseClass(String targetTag)
POLICY_CALLER:     copyBasicProperties(모든 mapped component, 무조건 호출)
XML_ATTRIBUTE_WRITER: appendClassTokenIfAbsent(target, videoBaseClass)
GENERATED_XML:     <xf:trigger class="btn_cm" .../> / <w2:gridView class="wq_gvw" .../>
```

이미 "TARGET_QNAME -> TARGET_CLASS_DECISION" generic policy이므로, 이번
라운드에서 **다시 구현하지 않았다**(중복 구현 금지, 기존 설계 존중). 결과
문자열은 그대로(`btn_cm`/`wq_gvw`), 코드는 애초에 하드코딩 산개 상태가
아니었다.

## 4. w2selectbox_disabled -- 실제 리팩터링

### 질문별 evidence 확인

1. **xf:select1/selectbox에서 disabledClass가 필요한가?** -- 기존 코드
   주석(analysis/v6-video-source-analysis.md 인용)에 따르면 실제 폐쇄망
   v6 화면에서 관측된 `xf:select1(appearance=minimal)` 3/3 전부
   `disabledClass="w2selectbox_disabled"`를 가짐.
2. **WebSquare runtime default는?** -- 코드 주석에 "component-intrinsic
   고정값, source 조건 없음"으로 이미 기록돼 있음(즉 개별 인스턴스의 실제
   disabled/enabled 상태와 무관하게 항상 선언).
3. **contents.css에 `.w2selectbox_disabled` rule이 존재하는가?** -- YES,
   `resources/target-websquare/WebContent/assets/css/contents.css`
   11행: `.w2input_disabled,.w2input_readonly,.w2selectbox_disabled,
   .w2selectbox_disabled,.w2upload_disabled .w2upload_input,
   .w2textarea_disabled{border:1px solid #e6e6e6 !important;
   background:#fafafa !important;color:#bdbeca;cursor:default}`,
   73행: `.w2selectbox_disabled .w2selectbox_label {color: #bdbeca;}`,
   80행: selectbox 화살표 아이콘 disabled 스타일도 존재.
4. **다른 target component의 disabled state class는?** -- 이번 라운드
   조사 범위에서는 select1/Combo만 evidence가 있고 다른 컴포넌트(Radio
   appearance=full 포함)는 evidence 없음 -- 매핑하지 않음(아래 HOLD).
5. **source disabled=true일 때만 필요한가?** -- 아니오, 기존 evidence가
   이미 "source 조건 없음"(항상 선언)으로 확인했음 -- 그대로 유지.
6. **항상 선언 vs theme hook?** -- 항상 선언하는 theme hook으로 확인됨
   (실측 3/3, 조건부 아님).

### 리팩터링

**BEFORE**(`applyComponentSpecificProperties`, `"Combo".equals(sourceTag)`
분기):
```java
} else if ("Combo".equals(sourceTag)) {
    target.setAttribute("appearance", "minimal");
    target.setAttribute("disabledClass", "w2selectbox_disabled");
```

**AFTER**:
```java
} else if ("Combo".equals(sourceTag)) {
    String appearance = "minimal";
    target.setAttribute("appearance", appearance);
    String disabledClass = resolveVideoEvidenceDisabledClass(target.getTagName(), appearance);
    if (disabledClass != null) {
        target.setAttribute("disabledClass", disabledClass);
    }
```

**신규 policy 함수**(`resolveVideoEvidenceBaseClass`의 자매 함수, 같은
위치):
```java
private String resolveVideoEvidenceDisabledClass(String targetTag, String appearance) {
    if ("xf:select1".equals(targetTag) && "minimal".equals(appearance)) {
        return "w2selectbox_disabled";
    }
    return null;
}
```

```
POLICY_DEFINITION: resolveVideoEvidenceDisabledClass(String targetTag, String appearance)
POLICY_CALLER:     applyComponentSpecificProperties(Combo 분기, appearance 결정 직후)
XML_ATTRIBUTE_WRITER: target.setAttribute("disabledClass", disabledClass)  (null이면 미emit)
GENERATED_XML:     <xf:select1 appearance="minimal" disabledClass="w2selectbox_disabled" .../>
```

Radio(`appearance="full"`)는 `resolveVideoEvidenceDisabledClass("xf:select1",
"full")`가 `null`을 반환하므로 attribute 자체가 emit되지 않는다(기존
동작과 동일 -- Radio 분기는 애초에 disabledClass를 설정하지 않았음).

이제 "Combo"라는 문자열 자체는 여전히 `applyComponentSpecificProperties`의
분기 조건(appearance="minimal" 설정 위치를 결정하기 위해 필요, source
component type 판별 자체는 불가피)으로 남아있지만, **class 값 결정
로직(`"w2selectbox_disabled"`라는 리터럴)은 QName+appearance 기반의
독립된 policy 함수로 옮겨졌다** -- 다른 source component가 같은 target
QName+appearance 조합으로 변환되더라도(예: 향후 새 XPlatform component가
`xf:select1(appearance=minimal)`로 매핑되는 경우) 같은 정책이 자동
재사용된다.

## 5. contents.css를 selector capability source로 -- 설계 선택

무거운 runtime CSS parser는 추가하지 않았다(매 conversion마다 CSS를
파싱하는 구현은 과설계 -- 요청 사항의 "무조건 넣으라는 의미는 아니다"에
해당). 대신:

- 이미 존재하는 `resolveVideoEvidenceBaseClass`/신규
  `resolveVideoEvidenceDisabledClass`가 **build-time/static metadata**
  방식(옵션 A)이다 -- Java 함수 자체가 evidence 기반 lookup table이며,
  이것이 이번 조사에서 확인한 "실제 코드 구조와 가장 자연스러운 최소
  변경"(옵션 D, 기존 architecture 확장)이다.
- `analysis/contents-css-integration-audit.md`(직전 라운드)가 이미
  전체 selector inventory + 4-category 분류를 문서화했고, 이번 문서는
  그 카탈로그를 실제 코드 policy와 연결한다(`CLASS_POLICY_SINGLE_SOURCE_
  OF_TRUTH`는 "코드 policy 함수가 하나"라는 의미이지 "CSS 자체를 파싱하는
  기능"을 의미하지 않는다 -- 이번 조사에서는 후자가 불필요하다고 판단).
- 신규 external config 파일(YAML/JSON/properties)은 만들지 않았다 --
  현재 이 evidence 기반 mapping이 2개뿐이라(base class 2개 QName,
  disabledClass 1개 조합) 외부 설정 파일로 분리할 이득보다 복잡성 증가가
  크다고 판단(과설계 방지). 향후 evidence가 늘어나면(예: 여러 컴포넌트에
  여러 state class가 생기면) 이 두 함수를 하나의 `Map<String,String>`
  기반 registry로 통합하는 리팩터링이 자연스러운 다음 단계가 될 수 있으나,
  현재 규모에서는 함수 2개가 이미 `CLASS_POLICY_SINGLE_SOURCE_OF_TRUTH`를
  만족한다(각각 정확히 한 번만 정의되고, 한 곳에서만 호출됨).

```
CLASS_POLICY_SINGLE_SOURCE_OF_TRUTH = PASS
```

## 6. class merge 정책(재확인, 무변경)

`appendClassTokenIfAbsent`(기존, 무변경)가 이미 요청된 merge policy를
만족한다: 기존 class(예: `cssclass`에서 온 값) 보존, 중복 토큰 추가 방지,
공백 구분, deterministic order(항상 끝에 추가). `disabledClass`는 다른
어떤 코드도 같은 attribute를 쓰지 않아 병합 대상이 없다(단순
설정/미설정).

## 7. source class attribute 처리 여부

```
SOURCE_CLASS_ATTRIBUTE_AVAILABLE = YES("cssclass", XPlatform 자체 속성)
SOURCE_CLASS_ATTRIBUTE_READ = YES(PropertyMappingRegistry.direct("cssclass","class"),
  WebSquareGenerator.copyAttributeIfPresent(src,target,"cssclass","class"))
SOURCE_CLASS_SEMANTIC_RESOLUTION = DIRECT_PASSTHROUGH_BY_DESIGN
```

XPlatform 고유의 **skin/스타일 식별자** 속성인 `class`(예: `round`,
`btn_WFSA_Search`, `sta_WFDA_Label_01`)는 코드 전체에서 **한 번도 읽히지
않는다**(`grep 'getAttribute("class")'` 결과 -- target 자신의 이미 설정된
class를 읽는 1건(`appendClassTokenIfAbsent` 내부)뿐, source의 `class`
속성을 읽는 곳은 0건). 즉 "source class raw-copy 금지" 원칙은 이미 지켜지고
있었다.

반면 `cssclass`는 **이름부터 목적이 다른, XPlatform 자체의 명시적 CSS
class passthrough hook**이다(작성자가 "이 값을 WebSquare class로 그대로
쓰라"는 의도로 지정하는 속성) -- `class`(skin 식별자, 이름 매칭 금지
대상)와 `cssclass`(명시적 전달 hook)는 서로 다른 XPlatform attribute이며,
이 구분 자체가 이미 "이름 유사성만으로 매핑" 금지 원칙을 지키고 있다.
이번 라운드에서 이 기존 메커니즘을 변경하지 않았다(문제 evidence 없음).

## 8. structural class(shbox/dfbox/tb/lybox 등) -- HOLD 유지

`GENERIC_MAPPING_CANDIDATE = HOLD`(아래 항목 전부, 이유: source component
type/state만으로 이 class들의 적용 여부/대상을 안전하게 결정할 수 있는
증거가 없음 -- 억지 매핑 시 `GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED` 원칙과
충돌하거나 서로 다른 Div를 구조적으로 병합하게 될 위험):

```
shbox / shbox_inner / btn_shbox   -> HOLD_INSUFFICIENT_EVIDENCE
tb / tbbox / w2tb_th / w2tb_td    -> HOLD_INSUFFICIENT_EVIDENCE(단, w2tb_tb/
                                       w2tb_td 리터럴 자체는 이미 코드에
                                       존재 -- PAUSED 경로 전용, 3번 참고)
dfbox / df_tit                    -> HOLD_INSUFFICIENT_EVIDENCE
lybox / ly_column / ly_form       -> HOLD_INSUFFICIENT_EVIDENCE
flex / flex_col                   -> HOLD_INSUFFICIENT_EVIDENCE
btnbox / pgtbox / rcard           -> HOLD_INSUFFICIENT_EVIDENCE
req / imp / err / hide(modifier)  -> HOLD_INSUFFICIENT_EVIDENCE(source에
                                       대응 semantic 없음)
```

이 HOLD 항목들 때문에 이번 라운드 작업을 중단하지 않았다 -- 안전하게
구현 가능한 것(4번, disabledClass policy 리팩터링)은 구현했다.

```
STRUCTURAL_CLASS_UNSAFE_MAPPING_COUNT = 0(위 어떤 structural class도
  이번 라운드에서 generated output에 적용하지 않았음)
```

## 9. 전체 component matrix

| SOURCE | TARGET_QNAME | RUNTIME_BASE_CLASS | DEFAULT_EXPLICIT_CLASS | STATE_CLASS | CONTENTS.CSS_MATCH | CLASS_DECISION_SOURCE | IMPL_STATUS |
|---|---|---|---|---|---|---|---|
| Div | `xf:group` | `w2group` | 없음 | 없음 | 해당 없음(group 관심사 아님) | `RUNTIME_AUTO` | 무변경 |
| Static | `w2:span` | `w2span` | 없음 | 없음 | PASS(`.w2span{display:inline-block}`) | `RUNTIME_AUTO` | 무변경 |
| Button | `xf:trigger` | `w2trigger` | `btn_cm` | 없음 | PASS | `resolveVideoEvidenceBaseClass`(기존) | 무변경(이미 generic) |
| Grid | `w2:gridView` | `w2grid` | `wq_gvw` | 없음 | PASS | `resolveVideoEvidenceBaseClass`(기존) | 무변경(이미 generic) |
| Combo | `xf:select1`(minimal) | `w2selectbox` | 없음 | `w2selectbox_disabled` | PASS | `resolveVideoEvidenceDisabledClass`(**신규**) | **리팩터링 완료** |
| Radio | `xf:select1`(full) | `w2selectbox`(추정, 미검증) | 없음 | 없음(evidence 없음) | 미확인 | HOLD | 무변경 |
| Calendar | `w2:inputCalendar` | 내부 DOM(`w2inputCalendar_div` 등) | 없음 | 없음(evidence 없음) | PASS(22건) | `RUNTIME_AUTO` | 무변경 |
| Edit | `xf:input` | `w2input`(추정) | 없음 | 없음 | PASS(14행) | `RUNTIME_AUTO` | 무변경 |
| MaskEdit | `xf:input` | `w2input`(추정) | 없음 | 없음 | PASS | `RUNTIME_AUTO` | 무변경 |
| TextArea | `xf:textarea` | `w2textarea` | 없음 | 없음 | PASS(4행) | `RUNTIME_AUTO` | 무변경 |
| CheckBox | `w2:checkbox` | `w2checkbox` | 없음 | 없음(evidence 없음) | PASS | `RUNTIME_AUTO` | 무변경 |
| Tab | `w2:tabControl` | 미확인 | 없음 | 없음 | 미확인 | HOLD | 무변경 |
| GroupBox | `w2:group`(무변경) | `w2group` | 없음 | 없음 | 해당 없음 | `RUNTIME_AUTO` | 무변경 |
| PopupDiv | `w2:group`(무변경) | `w2group` | 없음 | 없음 | 해당 없음 | `RUNTIME_AUTO` | 무변경 |
| FileUpload | `w2:upload` | 미확인 | 없음 | 없음(evidence 없음, `.w2upload_disabled` 존재하나 미적용) | PASS(11행 등) | HOLD | 무변경 |
| WebBrowser | `w2:wframe` | 미확인 | 없음 | 없음 | 미확인 | HOLD | 무변경 |

## 10. corpus BEFORE/AFTER 실제 count (149-fixture, fresh 재변환)

```
TOTAL_FIXTURES = 149
CONVERSION_PASS(BEFORE, commit 0c14546) = 149/149
CONVERSION_PASS(AFTER, 이번 리팩터링) = 149/149

TOTAL_CLASS_ATTRIBUTES = 16 (BEFORE = AFTER)
UNIQUE_CLASS_VALUES = 3 (btn_cm, wq_gvw, detail-tab -- 마지막은 cssclass
  direct-copy로 온 fixture별 값, 무관)
  btn_cm = 12 (BEFORE = AFTER)
  wq_gvw = 3 (BEFORE = AFTER)

TOTAL_DISABLED_CLASS_ATTRIBUTES = 4 (BEFORE = AFTER)
UNIQUE_DISABLED_CLASS_VALUES = 1 (w2selectbox_disabled)
  w2selectbox_disabled = 4 (BEFORE = AFTER)

TOTAL_READONLY_TRUE_ATTRS = 2 (BEFORE = AFTER)

shbox/dfbox/tb/tbbox/lybox/ly_column/ly_form/btnbox/pgtbox/rcard/flex/
req/imp/err = 0 (BEFORE = AFTER, 전부 미적용 유지)
```

**BEFORE/AFTER 생성 XML 전수 diff(136개 파일) = 0개 파일 변경**(파일 목록
diff 0, non-XML diff 0). STT00030(corpus 밖, 실제 폐쇄망 evidence)도
`diff` byte-identical(exit 0) 확인 -- `disabledClass="w2selectbox_disabled"`
포함 전체 output 무변경.

```
BUTTON_DEFAULT_CLASS_POLICY_PASS = PASS
GRID_DEFAULT_CLASS_POLICY_PASS = PASS
COMBO_DISABLED_CLASS_POLICY_PASS = PASS
TARGET_CLASS_POLICY_DETERMINISTIC = PASS
HARDCODED_SCREEN_SPECIFIC_CLASS_COUNT = 0(`grep -rn "STT00030" src/main/java`
  결과 2건 -- 전부 `ComponentLayoutConverter`의 **Javadoc 주석**(evidence
  citation, "실제 STT00030 evidence: ..." 형태)이며, 실행 코드(조건문/분기/
  리터럴 값)에는 어디에도 없다. `if sourceTag.equals("STT00030")`류 로직은
  0건)
SCATTERED_TARGET_CLASS_LITERAL_COUNT = 0(btn_cm/wq_gvw/w2selectbox_disabled
  전부 각각 정확히 1개 policy 함수에만 존재, 호출부도 각 1곳)
```

## 11. Regression(현재 HEAD 기준 실제 재실행, JDK21 개발 환경)

```
clean compile = PASS(0 errors)
149/149 conversion = PASS
Generated XML count = 136/136, 전부 well-formed(+ STT00030.xml 별도 확인)
PAGE_JS(inline <script>) = 136/136 PASS
standalone JS = 15/15 PASS
Phase1 SHA verifier = PASS(Python + Java)
before/after generated diff(전체 corpus + STT00030) = 0건(100% 동일)

TARGET_CLASS_POLICY_GATE = PASS
TARGET_STATE_CLASS_POLICY_GATE = PASS
CONTENTS_CSS_SELECTOR_COMPATIBILITY_GATE = PASS(위 9번 매트릭스, 전부 PASS
  또는 정당한 HOLD)
NO_SCREEN_SPECIFIC_CLASS_MAPPING_GATE = PASS

UNEXPECTED_GEOMETRY_CHANGE_COUNT = 0
UNEXPECTED_QNAME_CHANGE_COUNT = 0
UNEXPECTED_HIERARCHY_CHANGE_COUNT = 0
UNEXPECTED_GENERATED_DIFF = 0
```

## 12. Production 변경 요약

1개 파일(`WebSquareGenerator.java`) 수정: `resolveVideoEvidenceDisabledClass`
신규 함수 추가 + `applyComponentSpecificProperties`의 Combo 분기가 그
함수를 호출하도록 변경. `resolveVideoEvidenceBaseClass`(btn_cm/wq_gvw)는
이미 generic해서 무변경. `ComponentLayoutConverter`(inline style 정책,
직전 라운드)는 이번 라운드에서 건드리지 않았다(책임 분리 유지 -- Geometry
Policy/Inline Visual Style Policy/Target Class Policy/Target State Class
Policy가 서로 다른 함수/파일에 남아있다).

## 상태

```
CONTENTS_CSS_INTEGRATION = FIX_CANDIDATE(직전 라운드 유지)
TARGET_CLASS_MAPPING = FIX_CANDIDATE
TARGET_STATE_MAPPING = FIX_CANDIDATE
STATIC_VERIFIED
CLOSED_NETWORK_REVERIFY_READY = YES
```
