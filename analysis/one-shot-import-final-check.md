# Final pre-import check — one-shot closed-network guarantee

## 1. CLASS_POLICY_DEFINITION

```
CLASS_POLICY_DEFINITION = [WebSquareGenerator] resolveVideoEvidenceBaseClass(String targetTag)
                           [WebSquareGenerator] resolveVideoEvidenceDisabledClass(String targetTag, String appearance)
```
둘 다 `src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java`
안의 private method다. 외부 config/YAML/properties 파일은 없다(순수 Java
static lookup logic).

```
JAVA_SOURCE_EDIT_REQUIRED = YES
RECOMPILE_REQUIRED = YES
```
새 mapping을 추가하려면 이 두 함수(또는 같은 패턴의 새 자매 함수)를
수정하고 재컴파일해야 한다 -- 외부 config 파일 수정만으로는 불가능한
현재 architecture다(2번/6번에서 이 점을 그대로 인정하고 답한다).

## 2. 가정 시나리오 -- SOURCE_SEMANTIC_A→shbox / B→dfbox / C→tb 추가

| 질문 | 답 |
|---|---|
| 새로운 외부 source 반입 필요? | **NO** -- `WebSquareGenerator.java`는 이미 저장소(따라서 `closed-network-import/`와 같은 commit)에 존재, 폐쇄망 checkout 안에서 바로 수정 가능 |
| Java 수정 필요? | **YES** -- 현재 architecture(순수 Java static lookup)에서는 불가피 |
| mapping/config 수정만으로 가능? | **NO** -- 외부 mapping/config 파일 자체가 없음(6번 참고, 현재는 만들지 않기로 결정) |
| 재compile만 하면 되는가? | Java 수정 + 재컴파일(`javac`, `BUILD-AND-VERIFY.cmd`/`.sh`의 [2/6] 단계) + 149-fixture regression 재실행이 필요. 전부 폐쇄망 checkout 안에서 완결된다(외부 fetch 없음). |

## 3. Full editable source 포함 여부

`closed-network-import/`는 저장소의 **하위 디렉터리**이며, 저장소
자체(`candidate/v6-design-structure-alignment` 브랜치, 현재 HEAD
`9b33010`)가 이미 완전한 독립 프로젝트다(`README-KO.md` 0번에 명시).
실제 확인(현재 HEAD, `closed-network-import/`와 **같은 commit, 같은
checkout**):

```
src/main/java/com/example/xfdltracker/converter/*.java  = 9개 파일
  (WebSquareGenerator.java, ComponentLayoutConverter.java 등 -- class
  policy가 정의된 바로 그 파일들 포함)
sample-phase3-project/Form, /Script 등           = 149-fixture regression corpus
audit/phase1_sha_verifier.py, phase1_sha_manifest.json = Phase1 SHA 회귀 도구
build.sh/.bat, convert-sample.sh/.bat, verify-offline.sh/.bat = 기존 빌드/검증 스크립트
```

```
FULL_EDITABLE_SOURCE_INCLUDED = YES
  (범위: closed-network-import/의 sibling인 저장소 전체 -- README-KO.md
  0번이 이미 "1회 반입 = 이 git 브랜치/디렉터리 전체를 폐쇄망에 한 번
  복사하는 것"이라고 명시. closed-network-import/ 폴더 단독으로는 소스가
  없다 -- 이는 의도된 설계다, 저장소 전체가 이미 "완전한 candidate
  working-copy"이므로 그 안에 다시 전체를 중복 보관하지 않았다.)
COMPILER_REQUIRED_FILES_INCLUDED = YES(src/main/java 전체 76개 .java 파일,
  이전 라운드 BUILD-AND-VERIFY.sh/.cmd 실제 실행으로 clean compile PASS
  이미 확인됨)
MAPPING_POLICY_SOURCE_INCLUDED = YES(WebSquareGenerator.java,
  ComponentMappingRegistry.java 둘 다 src/main/java 안에 존재)
REGRESSION_SOURCE_INCLUDED = YES(sample-phase3-project 149 fixture,
  audit/phase1_sha_verifier.py + manifest, 전부 존재 및 실제 실행 확인됨)
```

**중요한 설계 확인**: "패키지"의 단위는 `closed-network-import/` 폴더
단독이 아니라 **그 폴더를 포함하는 저장소 전체(같은 git commit)**다.
`BUILD-AND-VERIFY.sh`(`REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"`)/`.cmd`
(`pushd "%SCRIPT_DIR%.."`) 둘 다 이미 이 전제로 설계돼 있고, 직전
라운드에서 실제 cmd.exe/sh 실행으로 이 전제가 동작함을 실증했다(clean
compile, 149/149 conversion, class-policy invariant, XML well-formed,
Phase1 SHA 전부 PASS). 즉 "저장소를 통째로 폐쇄망에 반입"하는 것 자체가
1회 반입이며, `closed-network-import/`는 그 반입 이후 검증을 자동화하는
kit일 뿐 별도의 축소된 sub-package가 아니다.

## 4/5. ONE_SHOT_SOURCE_IMPORT 판정

3번에서 확인한 대로 저장소 전체가 이미 반입 단위이고, 그 안에 compiler
필요 파일/mapping policy 소스/regression 자료가 전부 포함돼 있으므로:

```
ONE_SHOT_SOURCE_IMPORT = PASS
```

"폐쇄망 반입 1회"는 "폐쇄망 안에서 코드 수정 0회"를 의미하지 않는다는
전제를 그대로 따른다 -- Java 수정 + 재컴파일 + regression 재실행이
전부 폐쇄망 checkout 내부에서 완결되므로, 외부 저장소에서 파일을 다시
가져올 필요가 없다.

## 6. Optional data-driven policy 검토 -- 결론: 지금은 만들지 않음

현재 structural class mapping은 evidence가 **0건**이다(전부 HOLD --
`target-class-state-policy-audit.md` 8번). 즉 지금 config-driven registry를
새로 만들면, 채울 실제 데이터가 없는 빈 인프라만 추가하는 셈이다(과설계).
2번/4번에서 이미 확인했듯, Java 수정으로 대응 가능하고 그것이 "1회 반입"
요건을 충족하는 데 충분하므로, 지금 시점에는 **만들지 않는다**(요청사항의
"단지 미래에 편할 것 같다는 이유로 과도한 architecture를 새로 만들지
않는다" 원칙 그대로).

미래에 실제 evidence가 2~3건을 넘어서면, `resolveVideoEvidenceBaseClass`/
`resolveVideoEvidenceDisabledClass`와 같은 패턴의 새 자매 함수를
추가하는 것으로 충분하다(이미 증명된 패턴, 새 파일 형식/파서 불필요).

## 7. 남은 4개 literal 정확한 분류

| LITERAL | POLICY_METADATA_VALUE | SCATTERED_BRANCH | CENTRAL_POLICY_CONTROLLED |
|---|---|---|---|
| `btn_cm` | **YES**(단일 policy 함수의 return 값) | **NO** | **YES**(`resolveVideoEvidenceBaseClass`, 호출부 1곳) |
| `wq_gvw` | **YES** | **NO** | **YES**(동일 함수) |
| `w2tb_tb` | **YES**(구조적 상수, 8번 참고) | **NO** | 아래 8번 참고 -- `resolveVideoEvidence*`와 같은 registry는 아니지만, 이미 단일 생성 지점에 tightly-scoped |
| `w2tb_td` | **YES** | **NO** | 위와 동일 |

`btn_cm`/`wq_gvw`는 **hardcoding defect가 아니다** -- 중앙 policy 함수의
metadata 값(리터럴 문자열 자체가 존재하는 것은 어떤 lookup 구현에서도
불가피하며, 문제는 "그 값이 여러 곳에 흩어져 각기 다른 조건으로
재선언되는가"인데 여기서는 아니다).

## 8. w2tb_tb / w2tb_td 상세 조사

**어떤 source component에서**: 특정 XPlatform component 타입이 아니다 --
`convertLayoutAsTable`이 한 `Layout`의 직계 자식들을
`TABLE_LAYOUT_HIGH_CONFIDENCE`(표 형태 topology)로 분류했을 때만
생성되는 **synthetic 구조 wrapper**(row/cell 그룹화 자체가 XPlatform
원본에 없는 WebSquare 전용 구조).

**어떤 target QName/attribute에**: `xf:group`(`tagname="table"`,
`class="w2tb_tb"` -- table wrapper, WebSquareGenerator.java:830-835) /
`xf:group`(`tagname="td"`, `class="w2tb_td"` -- cell wrapper, :867-872).

**왜 필요한지**: 실제 v6 정상 화면(BCI01M0000) source 영상 직접 판독
evidence(주석 인용, WebSquareGenerator.java:721-734)에서
`tagname="table" class="w2tb_tb"` > `tr`(class 없음) >
`tagname="td" class="w2tb_td"` 구조가 100% 대응으로 관측됨 -- WebSquare
런타임이 이 `tagname`+`class` 조합을 실제 HTML `<table>`/`<td>`로
렌더링하는 신호.

**contents.css/runtime semantic과의 관계**: `.w2tb_td`/`.w2tb_th` 둘 다
`resources/target-websquare/WebContent/assets/css/contents.css`에
실제 rule이 존재한다(직전 라운드 확인 -- `.w2tb_td{height:23px;
margin:0;border:1px solid #b3b3b3;vertical-align:middle}` 등,
`.w2tb_th{font-weight:bold;padding:3px 10px;text-align:center;
background:#f1f1f1}`). 즉 이 두 class는 evidence가 이미 충분하다 --
CSV 조사에서 근거가 부족했던 `shbox`/`dfbox` 등과 다르다.

**현재 상태 -- 왜 손대지 않았는가**: 이 코드는
`GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED = true`(XPLATFORM_VISUAL_PARITY
라운드, 실제 Studio 재현 근거로 의도적으로 켜둔 flag)로 인해 도달 불가능한
**dead code**다(classification이 절대 `"TABLE_LAYOUT_HIGH_CONFIDENCE"`
문자열과 같아지지 않으므로 `convertLayoutAsTable`의 828행 이후 블록이
실행되지 않음, 항상 `convertChildren`으로 조기 반환). 즉:

- 소스 component별 조건 분기가 아니라, table 판정이 될 때만 나오는
  **구조적 상수**(하드코딩된 "결정"이 아니라 "wrapper 자신의 고정
  tagname/class 선언"에 가까움) -- `resolveVideoEvidenceBaseClass`와
  성격이 다르지만 "여러 source 조건에 따라 값이 바뀌는 scattered
  hardcoding" 문제의 사례도 아니다.
- 현재 output에 전혀 영향이 없다(dead code) -- 손대도/안 대도
  `UNEXPECTED_GENERATED_DIFF`에는 영향 없음.
- Table heuristic 자체가 PAUSED인 이유(과거 라운드의 실제 Studio 회귀
  재현)와 무관한 리팩터링을 이번에 강행할 근거가 없다 -- "안전하게 같은
  policy로 흡수 가능하고 output-identical"이라는 조건은 만족하지만,
  **지금 만질 필요/이득이 없다**(heuristic이 다시 켜지는 별도 라운드가
  생기면 그때 함께 재검토하는 것이 자연스럽다).

**결론**: 근거는 충분하지만 dead code라 이번 최종 반입 전에 변경하지
않았다(`NO_PRODUCTION_CHANGE_REQUIRED` 원칙 우선).

## 9. 최종 one-shot gate

```
CLOSED_NETWORK_PACKAGE_HAS_FULL_EDITABLE_SOURCE = YES
CLOSED_NETWORK_PACKAGE_CAN_REBUILD = YES(직전 라운드 실제 cmd.exe/sh 실행으로 증명됨)
CLOSED_NETWORK_PACKAGE_CAN_RERUN_REGRESSION = YES(동일 근거)
CLOSED_NETWORK_PACKAGE_CAN_EDIT_CLASS_POLICY_LOCALLY = YES(WebSquareGenerator.java가
  저장소 안에 존재, 폐쇄망 checkout에서 직접 수정 가능)
EXTERNAL_SECOND_SOURCE_IMPORT_REQUIRED = NO

ONE_SHOT_SOURCE_IMPORT = PASS
```

## 10. 결론

```
NO_PRODUCTION_CHANGE_REQUIRED
```
이번 점검은 audit-only로 종료한다. `closed-network-import/` bundle을
다시 만들지 않았고, `MANIFEST.sha256`/`BUILD-AND-VERIFY.sh`/`.cmd`도
무변경(이전 라운드에서 이미 실제 실행 검증 완료, 이번 점검 결과 추가
파일이 필요하지 않음이 확인됐을 뿐).
