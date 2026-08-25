# Explicit/State Class Policy 재확인 + Radio Fix + Tab Runtime Script 조사

> **후속 정정(2026-08-25)**: 이 문서의 Radio 관련 결론(`renderType=
> "radiogroup"` 추가만으로 충분하다는 판단)은 실제 폐쇄망 Studio
> 재검증에서 `STUDIO_FAILED`(NO_VISIBLE_EFFECT)로 기각됐다. 실제
> root cause(정적 `<xf:choices>` item 구조 부재)와 후속 fix는
> `analysis/radio-rendering-root-cause.md`를 참고할 것 -- 이 문서의
> Radio 부분(1-2, 2, 3, 4, 5절의 RADIO_RENDERING 관련 서술)은
> 최신 상태가 아니다. Class/state policy(1-1절)와 Tab runtime script
> (1-4절) 조사 결과는 여전히 유효하다(이번 후속 라운드에서 변경하지
> 않음).

## 0. 전제 (무변경 확인)

```
CONTENTS_CSS_LOADING = ALREADY_GLOBAL (websquare/config.xml
  earlyImportList, analysis/contents-css-integration-audit.md 확정,
  이번 라운드 무변경 -- config.xml 손대지 않음)
```

이번 라운드는 (1) 기존 explicit/state class 정책(Round G,
`analysis/target-class-state-policy-audit.md`)을 사용자가 새로 제공한
증거(실제 v6 Studio 스크린샷 6장, "목표 형태" 화면 5장)에 비추어
재확인하고, (2) Radio 표현 문제의 실제 원인을 devpack 실제 배포 화면
전수조사로 규명·수정하고, (3) Tab runtime script 주입 조건을 조사한
결과를 담는다.

## 1. 조사 보고

### 1-1. explicit/state class call path (기존 결론 재확인, 변경 없음)

Round G(`target-class-state-policy-audit.md`)에서 이미:
- `btn_cm`(Button→xf:trigger), `wq_gvw`(Grid→w2:gridView) --
  `resolveVideoEvidenceBaseClass(String targetTag)` 하나로 이미 generic
  (QName 기반, sourceTag 조건 없음). **무변경.**
- `w2selectbox_disabled`(Combo→xf:select1 minimal) --
  `resolveVideoEvidenceDisabledClass(String targetTag, String
  appearance)`로 이미 리팩터링 완료. **무변경.**
- `w2tb_tb`/`w2tb_td` -- `GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED=true`라
  도달 불가한 dead code, 한 곳에만 존재(산개 아님). **무변경(HOLD 유지).**
- Div/Static/Calendar/Edit/TextArea/CheckBox/GroupBox/PopupDiv --
  전부 `RUNTIME_AUTO`(런타임이 base class를 자동 emit, converter가
  explicit class를 쓰지 않음). **무변경.**
- structural class(shbox/dfbox/tbbox/lybox/btnbox/pgtbox/rcard/flex/
  req/imp/err) -- source XPlatform 쪽에 대응하는 semantic evidence가
  없어 `HOLD_INSUFFICIENT_EVIDENCE`. **이번 라운드에도 재확인, 무변경**
  (아래 1-3 참고 -- devpack corpus로 target 쪽 구조는 더 풍부하게
  확인했지만, source 쪽 mapping evidence는 여전히 없음).

이번 라운드에 새로 다룬 것은 **Radio**뿐이다(아래 1-2).

### 1-2. Radio 표현 이상 -- 원인 규명 + 수정 (신규)

상세: `analysis/radio-rendertype-evidence.md`. 요약:

```
POLICY_DEFINITION:    resolveTargetRenderType(String targetTag, String appearance)  [신규]
POLICY_CALLER:        applyComponentSpecificProperties, "Radio".equals(sourceTag) 분기
XML_ATTRIBUTE_WRITER:  target.setAttribute("renderType", renderType)  (null이면 미emit)
GENERATED_XML:         <xf:select1 appearance="full" renderType="radiogroup" .../>
```

- **원인**: 로컬 devpack에 실제 배포된 v6 native 업무 화면(XPlatform
  변환물 아님, `ui/BM,HM,SP/*.xml` 26개 파일) 전수 스캔 결과
  `appearance="full"`(Radio 계열) select1은 **7/7(100%)** 전부
  `renderType="radiogroup"`을 갖고, 우리 converter는 이 attribute를
  전혀 emit하지 않았다(`appearance="full"`만 설정). `appearance=
  "minimal"`(Combo)은 같은 corpus에서 47건 중 3건(6%)만 renderType이
  있어 매핑하지 않았다(evidence 부족, HOLD 유지 -- Combo는 지금처럼
  renderType 없이도 dropdown shell 자체는 그려지는 것으로 보임).
- **수정**: `Radio` 분기에서 `renderType="radiogroup"`을 함께 emit.
  QName("xf:select1")+appearance("full") 조합에만 반응하는 policy
  함수라 화면별/sourceTag별 예외 처리가 아니다 -- 다른 XPlatform
  component가 향후 같은 QName+appearance로 매핑되더라도 자동 적용된다.
- **itemset 자체(정적 `<xf:choices>` vs 런타임 `setNodeSet()`)는
  변경하지 않았다** -- devpack `ui/phase4test/Form/DatasetBinding.xml`이
  동일한 self-closing + `setNodeSet()` 패턴을 실제 devpack 예제로
  보여줘 기존 방식이 WebSquare가 지원하는 정상 API 사용임을 확인했다.
  STT00001.xml의 `Div01_Radio00` 원본 itemset이 실제로 비어 있었는지는
  이 저장소에 원본 XFDL이 없어(사용자 실사용 파일) 확정할 수 없다 --
  renderType 수정 후에도 화면이 비어 있다면 그 화면 고유의 itemset
  binding 문제일 수 있으므로 폐쇄망 Studio 재확인이 필요하다.

### 1-3. contents.css 관련 target-side 구조 재확인 (1~5번 이미지)

사용자가 "목표 형태"로 지목한 1~5번 이미지(계정별잔액점검 화면)는
`shbox`/`shbox_inner`/`btn_shbox`/`btn_cm`/`dfbox`/`f1`/`count_box`/
`tbbox`/`w2tb tb`/`w2tb_th`/`w2tb_td`/`wq_gvw` 구조를 보여준다. 이
구조는 devpack `ui/BM,HM/*.xml`에 실제로 존재하는 것과 정확히 일치한다
(예: `ui/BM/BM001M01.xml` 422~458행이 `xf:group class="w2tb_th"`,
`xf:group class="w2tb_td"`, `xf:group class="btn_shbox"`,
`xf:trigger class="btn_cm sch"`, `xf:group class="dfbox"` 구조를 그대로
가짐). 즉 **target 쪽에 이 class들이 실존하고 어떤 구조로 조합되는지는
이미 확인 가능**하다.

그러나 사용자가 요구한 mapping 조건(1-C: "source semantic evidence가
있을 때만 mapping 후보로 올린다")을 만족하려면, **XPlatform 쪽에서 어떤
Div/구조가 "검색조건 박스"(→shbox)이고 어떤 것이 "데이터 섹션
박스"(→dfbox)인지"를 구분할 source-side 신호**가 있어야 한다. devpack
corpus는 v6 native로 손으로 작성된 화면이라 XPlatform origin이 없다 --
target 구조의 "정답"은 보여주지만 "이 XPlatform Div가 왜 shbox가
되어야 하는가"는 알려주지 않는다. 이번 세션에서 실제로 갖고 있는
XPlatform 소스(STT00030.xfdl, sample-phase3-project 149개 fixture)의
Div들을 다시 살펴봐도, "검색조건 영역 Div"와 "데이터 영역 Div"를
구조적으로 구분할 수 있는 공통 signal(예: 특정 sibling 순서, 특정
CheckBox/Combo 자식 조합, 특정 명명 규칙)이 안정적으로 존재한다는
근거를 찾지 못했다 -- 이번 라운드 fixture corpus에는 애초에 shbox류
패턴을 가진 원본이 없다(전부 단일 Div 또는 GroupBox 위주 테스트
fixture). 따라서:

```
STRUCTURAL_CLASS_MAPPING_CANDIDATE_COUNT = 0 (이번 라운드에도 안전하게
  적용 가능한 case 없음, 강제 매핑 금지 원칙 유지)
```

이 결론은 Round G와 동일하며, devpack corpus가 target 쪽 evidence를
더 풍부하게 해줬을 뿐 source 쪽 evidence 부재라는 근본 원인은 바뀌지
않았다. **억지로 매핑하지 않았다.**

### 1-4. Tab runtime script 생성 원인

상세: `analysis/tab-runtime-script-injection-audit.md`. 요약:

```
POLICY_DEFINITION: TabRuntimePlan.isRuntimeRequired()
  = bridgeTarget || !operations.isEmpty() || !crossScreenReferences.isEmpty()
    || !scopeBridgeReferences.isEmpty()
```
이미 단일 게이트로 중앙화돼 있다(script 치환부/library 삽입부 모두 동일
조건 공유). `SET_URL`/`ADD_PAGE`/`INSERT_PAGE`/`REMOVE_PAGE`/
`bridgeTarget`/cross-screen 참조가 있으면 확실히 필요하다(실제 비동기
WFrame 로드/교체가 일어남). `SELECT_PAGE`(단순 tab index 전환)만 있는
화면은 이론적으로 축소 여지가 있으나, 같은 Tab 안에 정적/동적 페이지가
섞였는지 안전하게 판별할 API가 현재 없어 **이번 라운드에는 수정하지
않았다**(무조건 제거 금지 원칙, 근거 불충분 시 HOLD). 현재 동작은
과설계(항상 큰 script 포함)일 뿐 **정확성 문제는 아니다** -- 화면이
깨지는 사례는 없다.

### 1-5. runtime auto class vs explicit class 구분표 (재확인)

| TARGET_QNAME | RUNTIME_AUTO_CLASS(엔진이 자동 emit, converter 미관여) | EXPLICIT_CLASS(converter가 emit) | STATE_CLASS(converter가 emit) |
|---|---|---|---|
| `xf:trigger` | `w2trigger` | `btn_cm` | 없음 |
| `w2:gridView` | `w2grid` | `wq_gvw` | 없음 |
| `xf:select1`(minimal) | `w2selectbox` | 없음 | `w2selectbox_disabled` |
| `xf:select1`(full) | `w2selectbox`(추정) | 없음 | 없음(evidence 없음, HOLD) |
| `xf:group` | `w2group` | 없음 | 없음 |
| `w2:span` | `w2span` | 없음 | 없음 |
| `w2:inputCalendar` | 내부 DOM class | 없음 | 없음(evidence 없음, HOLD) |
| `xf:input`/`xf:textarea` | `w2input`/`w2textarea` | 없음 | 없음 |
| `w2:checkbox` | `w2checkbox` | 없음 | 없음(evidence 없음, HOLD) |
| `w2:tabControl` | 미확인 | 없음 | 없음(HOLD) |

renderType(`radiogroup`)은 class가 아니라 별도 attribute라 이 표에는
포함하지 않았다 -- 별도로 1-2에 기록.

## 2. Mapping 정책 표

| SOURCE_SEMANTIC | TARGET_QNAME | TARGET_EXPLICIT_CLASS | TARGET_STATE_CLASS | EVIDENCE | STATUS |
|---|---|---|---|---|---|
| Button(모든 XPlatform Button) | `xf:trigger` | `btn_cm` | 없음 | v6 영상 판독(Round G) | APPLY(기존, 무변경) |
| Grid(모든 XPlatform Grid) | `w2:gridView` | `wq_gvw` | 없음 | v6 영상 판독(Round G) | APPLY(기존, 무변경) |
| Combo(appearance=minimal) | `xf:select1` | 없음 | `w2selectbox_disabled` | v6 영상 판독(Round G) | APPLY(기존, 무변경) |
| **Radio(appearance=full)** | `xf:select1` | 없음(renderType으로 처리) | 없음(evidence 없음) | **devpack ui/BM,HM,SP 7/7 전수(이번 라운드, 신규)** | **APPLY(renderType="radiogroup" 신규 적용)** |
| Div/GroupBox/PopupDiv | `xf:group`/`w2:group` | 없음 | 없음 | RUNTIME_AUTO | 무변경(class 불필요) |
| Static | `w2:span` | 없음 | 없음 | RUNTIME_AUTO | 무변경 |
| Calendar | `w2:inputCalendar` | 없음 | 없음 | evidence 없음 | HOLD |
| CheckBox | `w2:checkbox` | 없음 | 없음 | evidence 없음 | HOLD |
| Tab/Tabpage | `w2:tabControl`/`w2:group` | 없음 | 없음 | evidence 없음(target class) | HOLD |
| "검색조건 영역"(shbox 후보) | 불명 | `shbox`/`shbox_inner` | - | target만 확인, source 신호 없음 | HOLD_INSUFFICIENT_EVIDENCE |
| "데이터 섹션"(dfbox 후보) | 불명 | `dfbox` | - | target만 확인, source 신호 없음 | HOLD_INSUFFICIENT_EVIDENCE |
| "표 입력영역"(tbbox 후보) | 불명 | `tbbox`/`w2tb_th`/`w2tb_td` | - | target만 확인, w2tb_tb/w2tb_td는 dead code로 코드상 존재 | HOLD_INSUFFICIENT_EVIDENCE |

## 3. Production 수정안

### 변경 파일 (1개)

`src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java`
-- `applyComponentSpecificProperties`의 `"Radio".equals(sourceTag)`
분기 + 신규 sibling policy 함수 `resolveTargetRenderType(String
targetTag, String appearance)`.

### 왜 이 구조가 generic한가

- `resolveTargetRenderType`은 `resolveVideoEvidenceBaseClass`/
  `resolveVideoEvidenceDisabledClass`와 완전히 동일한 아키텍처
  패턴이다: **target QName(+appearance)만 입력받아 값을 결정**하고,
  sourceTag나 화면명은 절대 보지 않는다. 호출부(Radio 분기)는 이미
  결정한 target QName+appearance를 그대로 넘길 뿐이다.
- 새 external config 파일(YAML/JSON)은 만들지 않았다 -- 기존
  `target-class-state-policy-audit.md` 5번 항목의 판단(evidence
  기반 매핑이 소수일 때는 함수 자체가 이미 `CLASS_POLICY_SINGLE_
  SOURCE_OF_TRUTH`를 만족, 외부 설정 분리는 과설계)을 그대로 따랐다.
- 다른 컴포넌트(Button/Grid/Combo)는 이번 라운드에 전혀 건드리지
  않았다 -- 수정 파일 1개, 수정 지점 1곳(+ 함수 1개 추가)으로 최소화.

### Tab runtime script / structural class는 변경하지 않음

두 항목 모두 조사만 하고 코드는 그대로 두었다(2절, 1-3/1-4 참고) --
evidence 불충분 또는 위험도가 이득보다 커서 사용자가 명시한 "무조건
제거/부착 금지" 원칙을 지켰다.

## 4. 검증

### 4-1. 전체 corpus regression (149-fixture, fresh 재변환, JDK 21)

```
clean compile = PASS(0 errors)
149/149 conversion = PASS(136 XML 생성, BEFORE=AFTER)
XML well-formed = PASS(136/136)
Phase1 SHA verifier = PASS
CLASS/STATE INVARIANT(무변경 확인): btn_cm=12, wq_gvw=3,
  w2selectbox_disabled=4 (BEFORE=AFTER, 전부 동일)
HOLD structural class 유출 = 0(shbox/dfbox/tbbox/lybox/ly_column/
  ly_form/btnbox/pgtbox/rcard 전부 0건, 무변경)
```

### 4-2. Radio fix 적용 전/후 corpus 전수 diff

```
BEFORE/AFTER 변경 파일 수 = 2개
  Form/ControlPropertyMatrix.xml : renderType="radiogroup" 추가 1개소
  Form/DatasetBinding.xml        : renderType="radiogroup" 추가 1개소
그 외 134개 생성 XML = byte-identical(diff 0)
diff 상세: appearance="full"인 select1 태그에 renderType="radiogroup"
  속성 1개만 추가, geometry/hierarchy/다른 attribute 전부 무변경
```

### 4-3. 실제 STT00030 재확인

```
STT00030에는 Radio 컴포넌트가 없음(grep 결과 0건) -- 이번 fix로 인한
영향 없음, Combo(Div01_MNG_BOCD, disabledClass="w2selectbox_disabled")
출력은 fix 전후 byte-identical(무변경 확인).
```

### 4-4. output-identical 유지 항목 명시

```
UNCHANGED = Button/btn_cm, Grid/wq_gvw, Combo/w2selectbox_disabled,
  Div/Static/Calendar/CheckBox/Tab/GroupBox/PopupDiv 전체,
  contents.css loading, Tab runtime script 주입 조건/내용,
  구조적 class(shbox/dfbox/tbbox 등) 전체
CHANGED = Radio(appearance="full")에 renderType="radiogroup" 추가만
  (해당 2개 fixture에서만 관측, corpus 내 Radio 사용례는 이 2건이 전부)
```

### 4-5. 폐쇄망 1회 반입 기준 재수정 가능 여부

```
CLOSED_NETWORK_ONE_SHOT_IMPORT = YES
  (이번 fix도 기존 policy 함수와 동일한 위치/패턴에 추가된 순수 Java
  코드 변경이라, 이미 반입된 전체 editable source tree
  -- src/main/java 전체 -- 안에서 동일한 방식으로 편집/재컴파일/재검증
  가능. 새 외부 설정 파일이나 외부 리소스를 추가하지 않았다.)
```

## 5. 최종 상태 보고

```
EXPLICIT_CLASS_POLICY = VERIFIED (기존 정책 재확인, 무변경 -- Round G에서
  이미 FIX_CANDIDATE로 구현된 것을 이번에 코드 재검토로 확인)
STATE_CLASS_POLICY = VERIFIED (기존 정책 재확인, 무변경)
RADIO_RENDERING = FIX_CANDIDATE (renderType="radiogroup" 추가, devpack
  7/7 evidence 기반, corpus regression 0 unexpected diff로 검증 완료 --
  단, 실제 폐쇄망 Studio 육안 확인은 사용자 몫, STUDIO_DESIGN_VERIFIED
  아직 아님)
TAB_RUNTIME_SCRIPT_POLICY = ROOT_CAUSE_IDENTIFIED (기존 게이트가 이미
  중앙화·정확함을 확인, SELECT_PAGE-only 축소 여지는 판별 API 부재로
  이번 라운드에는 구현하지 않음 -- 코드 변경 없음)
CONTENTS_CSS_LOADING = ALREADY_GLOBAL (무변경)
CLOSED_NETWORK_ONE_SHOT_IMPORT = YES
```
