# Radio Rendering Root Cause Deep-Dive (Studio 재검증 실패 이후)

## 0. 폐쇄망 Studio 실패 evidence (최우선 근거)

사용자가 커밋 `a5403fa`(`renderType="radiogroup"` 추가)를 실제 폐쇄망
WebSquare Studio에서 재검증한 결과:

```
ALL GATES = PASS(정적 regression)
RADIO_RENDERING = STUDIO_FAILED
renderType="radiogroup" = NO_VISIBLE_EFFECT
```

즉 `renderType="radiogroup"`을 추가한 것만으로는 실제 Studio에서 Radio가
정상 표현되지 않았다. 이 사실을 이번 조사의 최우선 evidence로 삼는다 --
"native corpus 7/7이 renderType을 갖는다"는 evidence 자체는 여전히
유효하지만(실제로 그런 값을 갖고 있다는 관찰), "renderType만 추가하면
충분하다"는 **가설**은 실제 Studio 재현 결과로 기각됐다.

## 1. Native working Radio corpus 재조사 (전체 구조, item 포함)

`websquare-devpack-copy/tomcat/webapps/ROOT/ui/BM/*.xml`에서 `xf:select1
appearance="full"` 태그 전체(자식 포함)를 추출했다. `NATIVE_WORKING_RADIO_COUNT
= 7`(BM001M01, BM002M01, BM003M01, BM006M01, BM006P01, BM007M01, BM008M01).

대표 예(BM001M01.xml:426, 인코딩 깨짐은 devpack 파일 자체가 CP949로
저장돼 UTF-8로 읽어 발생 -- 값 자체는 한글 레이블):

```xml
<xf:select1 appearance="full" cols="" disabled="" id="" ref="data:dma_search.IS_USE"
    renderType="radiogroup" rows="" selectedIndex="0" style="">
    <xf:choices>
        <xf:item>
            <xf:label><![CDATA[전체]]></xf:label>
            <xf:value><![CDATA[]]></xf:value>
        </xf:item>
        <xf:item>
            <xf:label><![CDATA[사용]]></xf:label>
            <xf:value><![CDATA[Y]]></xf:value>
        </xf:item>
        <xf:item>
            <xf:label><![CDATA[미사용]]></xf:label>
            <xf:value><![CDATA[N]]></xf:value>
        </xf:item>
    </xf:choices>
</xf:select1>
```

**전수 결과 (7/7, 예외 0건)**:

| 항목 | 값(7건 공통) |
|---|---|
| TARGET_QNAME | `xf:select1` |
| appearance | `"full"` |
| renderType | `"radiogroup"` |
| **xf:choices/xf:item** | **7/7 전부 존재, item 2~3개, 각각 `<xf:label>`+`<xf:value>` 리터럴 CDATA 값 보유** |
| ref | `data:<dataset>.<column>`(선택된 값 binding, item 목록과 별개) |
| item 목록 binding 방식 | **정적 XML(런타임 setNodeSet 호출 0건, 이 corpus 자체엔 JS가 없어 확인 불가하나 최소 정적 구조는 항상 존재)** |
| parent QName/class | `xf:group class="w2tb_td"`(표 셀 구조 안, 1-3절 참고) |
| disabled/selectedIndex | 값이 있을 때도(0,1,-1) 없을 때도 있음 -- 인스턴스별, 패턴 아님 |

**NATIVE_WORKING_RADIO_COUNT = 7. 공통 구조 = `appearance="full"` +
`renderType="radiogroup"` + 정적 `<xf:choices><xf:item>`(1개 이상) 세
가지가 항상 함께 있다.** 이전 라운드는 세 번째(정적 item 구조)를
확인하지 못하고 두 번째(renderType)만 고쳐 실패했다.

## 2. Generated failing Radio 구조 (수정 전, a5403fa 시점)

corpus 안의 Radio 사용 fixture 2개(`ControlPropertyMatrix.xfdl`의 `rdo`,
`DatasetBinding.xfdl`의 `rdoCode`) 중 실제 itemset이 있는 쪽
(`rdoCode`)의 a5403fa 시점 생성 결과:

```xml
<xf:select1 appearance="full" id="rdoCode" renderType="radiogroup"
    style="position:absolute;left:1.4%;top:55.6%;width:42.9%;height:44.4%;"/>
```

**self-closing -- `<xf:choices>` 없음.** renderType은 있었지만 native
working 7건이 공통으로 가진 세 번째 요소(정적 item 구조)가 없었다 --
이것이 Studio 실패의 직접 원인으로 확인됐다(아래 5번).

## 3. 핵심 structural diff

| CATEGORY | NATIVE_WORKING_VALUE | GENERATED_FAILING_VALUE(a5403fa) | LIKELY_RENDERING_RELEVANCE |
|---|---|---|---|
| QNAME_DIFF | `xf:select1` | `xf:select1` | 없음(동일) |
| ATTRIBUTE_DIFF(appearance) | `"full"` | `"full"` | 없음(동일) |
| ATTRIBUTE_DIFF(renderType) | `"radiogroup"` | `"radiogroup"`(a5403fa에서 이미 추가) | **LOW**(추가했지만 단독으로는 효과 없음 -- Studio 실측 확인) |
| **ITEM_STRUCTURE_DIFF** | **정적 `<xf:choices>` 1~3 item, 리터럴 label/value** | **없음(self-closing)** | **HIGH(핵심 원인으로 판정, 아래 5번)** |
| BINDING_DIFF(선택값 ref) | `ref="data:X.Y"` 있는 경우/없는 경우 혼재 | `ref` 없음(이 fixture는 value binding 자체가 없음) | LOW(선택값 binding은 item 렌더링과 무관한 별개 관심사) |
| PARENT_STRUCTURE_DIFF | `xf:group class="w2tb_td"`(표 구조 안) | `xf:group id="grp_main"`(단순 절대좌표 그룹) | LOW(구조적 class는 HOLD 영역, 이번 조사 범위 아님 -- 16절) |
| CLASS_DIFF | class 없음(7건 전부 select1 자체엔 class 미부여) | class 없음(동일) | 없음 |
| STYLE_DIFF | 대부분 빈 문자열(`style=""`, Studio 내부 편집기가 생성한 원본이라 명시 좌표 없음) | `position:absolute;left:...;width:...;height:...`(우리 converter의 percent geometry) | LOW(다른 컴포넌트도 동일 정책 사용, Radio 특유 문제 아님, 9절) |
| EVENT_DIFF | 없음(이 corpus는 순수 마크업) | 없음 | 해당 없음 |

## 4. Source Radio item 데이터 call path 추적 (핵심)

```
SOURCE_RADIO_ITEM_SOURCE = INNER_DATASET
  (XPlatform Radio는 <Radio innerdataset="dsCode" codecolumn="CD"
  datacolumn="NM" .../> 형태 -- DatasetBinding.xfdl:8행 확인)

SOURCE_RADIO_ITEMS_READ = YES
  (BindingAnalyzer.java 37~43행이 innerdataset/codecolumn/datacolumn을
  읽어 ItemsetBinding으로 model에 저장 -- 이미 존재하던 코드, 이번에 새로
  만들지 않음)

SOURCE_RADIO_ITEMS_STORED_IN_MODEL = YES
  (BindingModel.addItemset()에 저장되고, WebSquareGenerator.applyBindings가
  bindingModel.findItemset(sourcePath, localId)로 조회)

SOURCE_RADIO_ITEMS_EMITTED(a5403fa 시점, 수정 전) = NO(item 값 자체가
  target XML에 전혀 나타나지 않음 -- 오직 런타임 JS
  `rdoCode.setNodeSet("data:dsCode","NM","CD")`로만 참조)

GENERATED_RADIO_ITEM_COUNT(a5403fa 시점) = 0(self-closing)
```

**그런데 결정적으로, item의 실제 VALUES(코드/라벨)는 conversion
시점에 이미 100% 알려져 있었다** -- XPlatform source의 `dsCode` Dataset
자체가 리터럴 `<Rows><Row><Col id="CD">1</Col><Col id="NM">One</Col>
</Row></Rows>`를 XFDL 안에 갖고 있고(DatasetBinding.xfdl:5행), 이 값은
이미 target `<w2:dataList id="dsCode">`(생성된 XML의 `<xf:model>`
영역)에 정적으로 존재한다(`appendDatasetInitialData`, 기존 코드,
Round 3/Phase3부터 이미 동작). 즉 **item 데이터 자체가 없어서가
아니라, 이미 알고 있는 데이터를 select1의 `<xf:choices>`로 옮겨적지
않은 것**이 원인이었다.

## 5. Root Cause 판정

```
RADIO_ROOT_CAUSE = IDENTIFIED

원인: WebSquareGenerator.applyBindings의 itemset 처리 로직
(Combo/ListBox/Radio 공통)이 item 목록을 오직 런타임 JS
`<targetId>.setNodeSet(...)` 호출로만 표현하고, 정적
<xf:choices><xf:item> XML을 전혀 emit하지 않았다. WebSquare Studio의
Design-time renderer는 page-load JS(setNodeSet 포함)를 실행하지
않으므로, item이 0개로 보인다. Combo(appearance=minimal)는 dropdown
shell 자체가 item 수와 무관하게 그려져 크게 티가 안 나지만, Radio
(appearance=full)는 item 단위로 렌더링되는 위젯이라 item이 0개면
위젯 자체가 아무것도 그리지 않는다(native 7/7 evidence가 뒷받침).
renderType="radiogroup"은 native 패턴의 일부이지만 그 자체만으로는
Studio가 Radio를 그리게 하지 못한다(실측 확인) -- item 구조 부재가
더 근본적인/필요조건 원인이었다.
```

### H1~H7 판정

```
H1. source Radio의 item/innerdataset 정보가 converter model에서 소실됨
    -> REJECTED(BindingAnalyzer가 이미 정확히 읽어 model에 저장하고
       있었다 -- 소실이 아니라 "읽었지만 target XML로 emit하지 않음"이
       문제였다)
H2. xf:select1은 생성되지만 xf:choices/xf:item이 불완전함
    -> CONFIRMED(정확히는 "불완전"이 아니라 "전혀 없음" -- 이번 라운드
       root cause)
H3. item label/value binding 방식이 native와 다름
    -> CONFIRMED(native = 정적 XML, 기존 generated = 런타임 JS 전용 --
       이번 fix로 native와 동일하게 정적 XML도 함께 emit)
H4. appearance/renderType 조합 외에 native working Radio가 요구하는
    attribute가 있음
    -> REJECTED(attribute 자체는 아니었다 -- 요구하는 것은 attribute가
       아니라 자식 XML 구조(xf:choices)였다)
H5. Radio width/height/orientation conversion이 부적절함
    -> INSUFFICIENT_EVIDENCE(geometry 자체를 이번에 바꾸지 않았고,
       native 쪽도 style이 대부분 비어있어 직접 비교 근거가 약함 --
       9절 참고, 이번 fix로 격리해 별도 관찰 필요)
H6. target QName 자체가 잘못됨
    -> REJECTED(native 7/7 전부 xf:select1 사용, QName은 맞다)
H7. contents.css .w2radio selector가 hit되는 runtime DOM이 생성되지
    않음
    -> CONFIRMED(8절 재확인: `.w2radio`/`.w2radio_item`/`.w2radio_label`
       selector는 contents.css 109~162행에 실제로 존재한다 -- CSS
       자체는 정상이었다. 문제는 그 selector가 대상으로 하는
       item 단위 DOM(`.w2radio_item` + `input[type=radio]` +
       `.w2radio_label`)이 item이 0개였기 때문에 애초에 생성되지
       않았던 것 -- 즉 CSS 원인이 아니라 H2/H3(item 구조 부재)의
       직접적 결과다)
```

## 6. RenderType 정책 재판정

```
RENDERTYPE_RADIOGROUP_POLICY = KEEP_BUT_INSUFFICIENT

근거: native working Radio 7/7 전부 renderType="radiogroup"을 가지므로
값 자체를 제거할 근거는 없다(native 패턴과의 불일치를 만들 이유가
없음). 그러나 폐쇄망 Studio 실측 결과 이것만으로는 부족했고, 정적
xf:choices/xf:item 구조가 함께 있어야 한다(4~5절). 따라서 renderType은
유지하되 "이것만으로 충분하다"는 이전 가설은 폐기하고, item 구조
추가를 주 fix로 취급한다.
```

## 7. 이번 Production 수정

### 변경 파일 (1개, 기존과 동일 파일)

`src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java`

### 변경 내역

1. `applyBindings`에 `Document out` 파라미터 추가(정적 `xf:item`
   element를 만들려면 target document가 필요 -- 호출부 2곳
   기계적으로 갱신, 로직 변화 없음).
2. 신규 함수 `appendStaticChoicesIfLiteralDataset(Document out, Element
   target, ItemsetBinding itemset, String sourcePath)`: itemset이
   가리키는 source `<Dataset>`이 **리터럴 `<Rows><Row>` 데이터를 실제로
   갖고 있을 때만**(conversion 시점에 값이 100% 확정된 경우만) 그 값을
   그대로 읽어 `<xf:choices><xf:item>...`을 생성해 target에 붙인다.
   Rows가 없거나 비어 있으면(서버 io() 호출로만 채워지는 진짜 동적
   dataset) 아무것도 하지 않는다 -- **존재하지 않는 데이터를 추측해서
   만들어내지 않는다.**
3. 신규 헬퍼 `findDatasetById(String datasetId)`: source document에서
   `Dataset`/`DataSet` 태그로 id 매칭.
4. `applyBindings`의 Radio 분기에서만(`"Radio".equals(sourceTag)`)
   위 함수를 호출 -- **Combo/ListBox는 건드리지 않았다**(evidence가
   Radio에만 있고, Combo는 이미 "정상으로 보인다"는 기존 관찰이 있어
   scope를 넓히지 않음, 11절 원칙 준수).
5. 기존 런타임 `setNodeSet()` 호출은 **그대로 유지**(제거하지 않음).
   근거: devpack 실제 런타임 JS(`wbm_B5170_babel_main.js`)를 실측한
   결과, `setNodeSet`은
   ```
   l.prototype.setNodeSet=function(e,t,r){try{this.modelControl.unbindItemset(),
   ...,this.modelControl.setItemset(e,t,r),...}
   ```
   구조로 **먼저 기존 itemset binding을 `unbindItemset()`으로 해제한
   뒤 `setItemset()`으로 재설정**한다(unbind-then-rebind). 즉 이미
   정적 `<xf:choices>`가 있는 상태에서 `setNodeSet()`이 호출돼도 그
   시점에 안전하게 대체되는 구조로 확인했다 -- 정적 구조와 런타임
   호출을 동시에 유지해도 충돌하지 않는다고 판단한 근거다(실제
   브라우저 페이지 로드까지 재현한 것은 아니며, 이 판단이 폐쇄망
   Studio Design-time 표현을 고치는 것과는 별개로 실제 browser
   runtime에서도 문제없는지는 사용자가 실사용 화면에서 최종 확인
   필요).

### 왜 이것이 generic한가

- 화면명/컴포넌트 id 조건 없음. 오직 (a) sourceTag가 Radio인지,
  (b) 그 Radio의 itemset이 가리키는 source Dataset이 리터럴 Rows를
  갖는지 두 가지 evidence 기반 조건만 사용한다.
- Dataset이 진짜 동적(서버 io() 전용)이면 자동으로 아무 것도
  추가되지 않는다 -- 이 구분 자체가 이미 XPlatform 소스 구조에서
  나온다(코드가 dataset 종류를 임의로 추측하지 않는다).
- 다른 컴포넌트(Button/Grid/Combo/Calendar 등)는 전혀 건드리지
  않았다.

## 8. contents.css Radio selector 확인

`resources/target-websquare/WebContent/assets/css/contents.css`를
재확인한 결과, **실제로 Radio 전용 rule이 존재한다**(109~162행,
"Radio & Checkbox" 섹션):

```css
.w2checkbox .w2checkbox_item,.w2radio .w2radio_item{position:relative;display:inline-block;padding-left:21px;min-height:24px;line-height:24px;}
.w2checkbox .w2checkbox_label,.w2radio .w2radio_label{font-size:13px;...}
.w2radio .w2radio_item input[type="radio"]{position:absolute;left:0;top:3px;opacity:0;}
.w2radio_label:before{position:absolute;left:0px;top:4px;content:'';...border-radius:50%;background:#EEE}
input[type="radio"]:checked + .w2radio_label:before{border-color:#31B0E5;}
input[type="radio"]:checked + .w2radio_label:after{content:'';...border-radius:50%;background:#31B0E5}
/* table radio */
.w2radio .w2radio_td_input{position:relative;height:18px;}
```

```
RADIO_CSS_SELECTOR_AVAILABLE = YES (`.w2radio`/`.w2radio_item`/
  `.w2radio_label`, 109~162행)
```

이 CSS는 실제 native radio `<input type="radio">`를 `opacity:0`으로
숨기고, `.w2radio_label`의 `:before`/`:after` 가상요소로 원형 UI를
그리는 방식이다 -- 즉 **`.w2radio_item` wrapper + `input[type=radio]`
+ `.w2radio_label` 3-요소가 item 개수만큼 실제로 DOM에 존재해야만
이 CSS가 적용될 대상이 생긴다.** item이 0개면 이 DOM 자체가 생성되지
않으므로 CSS가 아무리 정확해도 아무것도 보이지 않는다 -- **이 확인은
CSS 원인을 기각하는 것이 아니라 오히려 4~5절의 결론(item 구조 부재가
근본 원인)을 보강한다**: `.w2radio`류 class는 `RUNTIME_AUTO`(엔진이
item마다 자동으로 emit, converter가 관여하지 않음)로 보이며, 우리
converter가 explicit하게 이 class를 쓸 필요는 없다(다른 RUNTIME_AUTO
컴포넌트와 동일 패턴).

```
RADIO_RUNTIME_DOM_EXPECTED = renderType="radiogroup" + item 개수만큼
  <span class="w2radio_item"><input type="radio".../><label
  class="w2radio_label">...</label></span> 구조(CSS 규칙 역산 기반 추정,
  실제 렌더링된 DOM을 devtools로 직접 캡처하지는 못했다)
RADIO_RUNTIME_DOM_GENERATION_CONFIDENCE = MEDIUM(CSS 규칙과 native
  구조 evidence는 강하지만 실제 렌더링 DOM 직접 관찰 없음)
RADIO_FAILURE_CATEGORY = ITEM_BINDING(주 원인, 4~5절) -- CSS 자체는
  문제가 아니다(selector가 정확히 존재함을 확인했고, 문제는 그 CSS가
  hit할 DOM이 애초에 생성되지 않았던 것)
```

## 9. width/height 확인 (Radio 전용 수정은 하지 않음)

`rdoCode`의 geometry는 `left:1.4%;top:55.6%;width:42.9%;height:44.4%`
(다른 컴포넌트와 동일한 percent geometry policy, `ComponentLayoutConverter`
공통 로직). native corpus 쪽은 `style=""`(빈 값, Studio 저작 도구가
아직 좌표를 명시하지 않은 원본이라 직접 비교 불가)이라 "Radio 전용
geometry 문제"라는 근거를 만들 수 없었다. **geometry policy는 이번
라운드에서 변경하지 않았다**(9번 항목이 요구하는 "Radio-specific
semantic evidence가 있을 때만 수정"이라는 조건을 만족하지 못함).

## 10. 검증

### 10-1. 전/후 corpus 전수 diff (a5403fa 대비, 이번 fix만 격리)

```
변경 파일 = 1개(Form/DatasetBinding.xml, 리터럴 dataset을 가진
  rdoCode에 <xf:choices> 추가)
그 외 135개 파일 = byte-identical(Form/ControlPropertyMatrix.xml
  포함 -- 이 파일의 rdo는 source에 애초에 innerdataset이 없어
  itemset 자체가 model에 없고, 따라서 이번 fix 코드 경로에 진입하지
  않음 -- 근거: sample-phase3-project/Form/ControlPropertyMatrix.xfdl
  <Radio id="rdo" left="270" top="120" width="160" height="60"/>,
  innerdataset 속성 자체가 없음)
```

### 10-2. BEFORE/AFTER 전체 Radio block (요청된 전체 구조 비교)

**BEFORE (a5403fa, renderType만 있음)**:
```xml
<xf:select1 appearance="full" id="rdoCode" renderType="radiogroup"
    style="position:absolute;left:1.4%;top:55.6%;width:42.9%;height:44.4%;"/>
```

**AFTER (이번 fix)**:
```xml
<xf:select1 appearance="full" id="rdoCode" renderType="radiogroup"
    style="position:absolute;left:1.4%;top:55.6%;width:42.9%;height:44.4%;">
    <xf:choices>
        <xf:item>
            <xf:label><![CDATA[One]]></xf:label>
            <xf:value><![CDATA[1]]></xf:value>
        </xf:item>
    </xf:choices>
</xf:select1>
```

(source `dsCode` dataset이 리터럴 row 1개(`CD=1,NM=One`)만 가지므로
item도 1개 -- 데이터를 부풀리지 않고 있는 그대로 반영했다.)

### 10-3. Full regression

```
clean compile = PASS(0 errors)
149/149 conversion = PASS(136 XML)
XML well-formed = PASS(136/136)
Phase1 SHA verifier = PASS
btn_cm=12 / wq_gvw=3 / w2selectbox_disabled=4 = 전부 무변경
HOLD structural class 유출 = 0(무변경)

RADIO_FIXTURE_COUNT = 2 (corpus 내 source <Radio> 사용 fixture)
RADIO_GENERATED_COUNT = 2 (appearance="full" 생성 수)
RADIO_WITH_ITEMS_COUNT = 1 (정적 xf:choices 보유, 리터럴 dataset 있는 쪽)
RADIO_EMPTY_ITEM_COUNT = 1 (source에 itemset 자체가 없는 rdo -- 버그
  아님, 원본에 애초에 없는 데이터를 만들어내지 않은 것)
RADIO_RENDERTYPE_COUNT = 2 (전부 유지)
RADIO_STRUCTURE_GATE = PASS
```

### 10-4. 영향 최소화 확인

```
RADIO_AFFECTED_OUTPUT_COUNT = 1
NON_RADIO_UNEXPECTED_DIFF_COUNT = 0
UNEXPECTED_QNAME_CHANGE_COUNT = 0
UNEXPECTED_GEOMETRY_CHANGE_COUNT = 0
UNEXPECTED_HIERARCHY_CHANGE_COUNT = 0
TAB_RUNTIME_SCRIPT_CHANGE_COUNT = 0
STRUCTURAL_CLASS_UNSAFE_MAPPING_COUNT = 0
```

## 11. 남은 불확실성 (정직하게 명시)

1. **실제 브라우저 runtime에서 정적 `<xf:choices>` + 런타임
   `setNodeSet()` 병행이 실제로 문제없이 동작하는지**는 devpack
   minified 코드의 `unbindItemset()`/`setItemset()` 구조 판독으로
   근거를 확보했으나, 실제 페이지를 로드해 관찰한 것은 아니다.
2. **폐쇄망 Studio Design-time에서 이번 fix로 Radio가 실제로 정상
   표현되는지는 아직 확인되지 않았다.** item 구조 부재가 유력한 원인
   이지만(4~5절), 다른 요인(8~9절에서 INSUFFICIENT_EVIDENCE로 남긴
   CSS/DOM 관련 사항)이 남아있을 가능성을 배제하지 않는다.
3. `ref`(선택된 값) attribute는 이 두 fixture 모두 값 binding이 없어
   (rdoCode는 itemset만 있고 value binding은 없음) 실제로 다뤄보지
   못했다 -- 향후 `ref`가 있는 Radio가 발견되면 별도 확인 필요.

이 불확실성 때문에 최종 상태를 `VERIFIED`가 아니라 `FIX_CANDIDATE`로
제한한다(아래 12절).

## 12. 최종 상태

```
RADIO_ROOT_CAUSE = IDENTIFIED
GENERIC_RADIO_FIX = IMPLEMENTED
RADIO_RENDERING = FIX_CANDIDATE (STUDIO_FAILED 아님, 그러나
  STUDIO_VERIFIED도 아님 -- 폐쇄망 재검증 대기)
RADIO_REVERIFY_READY = YES
RENDERTYPE_RADIOGROUP_POLICY = KEEP_BUT_INSUFFICIENT
STUDIO_DESIGN_VERIFIED = NO
FREEZE_READY = NO
FINAL_IMPORT_READY = NO
```
