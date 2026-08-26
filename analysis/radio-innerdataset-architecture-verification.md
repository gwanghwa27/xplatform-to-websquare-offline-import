# Radio innerdataset Architecture 재검증 (STT00001 실제 파일 부재, Production 무변경)

## 0. 전제 -- 사용자 확인 evidence 반영

사용자가 실제 폐쇄망에서 실패한 STT00001의 Radio가 XFDL 내부
Dataset을 `innerdataset`으로 참조하는 구조임을 확인했다:

```
SOURCE_RADIO_ITEM_SOURCE = INNER_DATASET
SOURCE_DATASET_SCOPE = SAME_XFDL
SOURCE_INNERDATASET_AVAILABLE = YES
```

단, `STT00001.xfdl` 실제 파일은 이 세션에 여전히 없다(이전 라운드에서
확인한 대로 -- Downloads 전체/프로젝트 디렉터리 재검색 결과 없음).
따라서 다음은 여전히 미확정이다:

```
SOURCE_DATASET_LITERAL_ROWS = 미확정
SOURCE_CODECOLUMN = 미확정
SOURCE_DATACOLUMN = 미확정
```

이번 라운드는 실제 파일 없이, **dcb7dfe의 architecture가 "같은
XFDL 내부 innerdataset Radio" 패턴 전반을 generic하게 지원하는지**를
코드 재점검 + 실제 corpus(합성 fixture + 실제 STT00030.xfdl) evidence로
검증한다.

## 1. `appendStaticChoicesIfLiteralDataset`/`findDatasetById` 재점검

코드(`WebSquareGenerator.java`)를 다시 읽고 흐름을 추적했다:

```
Radio innerdataset="..." codecolumn="..." datacolumn="..."
  -> BindingAnalyzer.walk() (기존 코드, 이번 라운드에 손대지 않음)
     normalizeDataset(getAttribute("innerdataset")) -- "@" prefix 제거
     -> ItemsetBinding(currentPath, inner, code, data)로 BindingModel에 저장
  -> WebSquareGenerator.applyBindings()
     bindingModel.findItemset(sourcePath, localId)로 조회
     -> "Radio"면 appendStaticChoicesIfLiteralDataset(out, target, itemset, sourcePath) 호출
        -> findDatasetById(itemset.getDatasetId())
           -- findDescendants(sourceDocument.getDocumentElement(), "Dataset"/"DataSet")
           -- **전체 subtree를 getElementsByTagName("*")으로 스캔**(고정 경로 가정 없음)
           -- id 속성 매칭(정확히 일치하는 첫 Dataset)
        -> findDirectChild(dataset, "Rows") -> directChildren(rows, "Row")
        -> 각 Row의 <Col id="datacolumn">/<Col id="codecolumn">를 찾아
           <xf:choices><xf:item><xf:label/><xf:value/></xf:item>...</xf:choices> 생성
```

`findDatasetById`가 **고정 경로(`Objects` 직계 자식 등)를 전혀
가정하지 않는다** -- `findDescendants`는 `Element.getElementsByTagName("*")`
기반 전체 문서 순회이므로 Dataset이 어디에 중첩돼 있어도(깊이 무관)
찾는다. "fixture에서 우연히 찾은 구조"가 아니라 애초에 경로 독립적
설계다 -- 아래 2, 3절에서 실제 corpus/실제 STT00030으로 이를 재확인
했다.

## 2. Dataset 위치 variation -- 실제 corpus 조사 결과

corpus(`sample-phase3-project`) + 실제 `STT00030.xfdl`을 전수 조사한
결과, 실제로 **2가지 위치 패턴이 이미 존재**한다:

| 패턴 | 발견 파일 | 예 |
|---|---|---|
| `Form > Objects > Dataset`(1단계 wrapper) | corpus 9개 파일(DatasetBinding, ComponentMethodConversion, ScopeShadowing, TabChildCallsParent 등) + **실제 STT00030.xfdl**(Dataset 7개 전부) | `<Objects><Dataset id="dsTransInfo" .../>...</Objects>` |
| `Form > Dataset`(wrapper 없음, 직계 자식) | corpus 1개 파일(`GridAdvancedPhase3.xfdl`) | `<Form ...><Dataset id="dsMain">...` |

**두 패턴 모두 실제 evidence로 확인됐다**(추측이 아님). `findDatasetById`가
경로 독립적이므로 이론상 둘 다 지원하지만, 직접 검증하기 위해 두 번째
패턴(Objects 없음)과 실제 STT00030의 `@` prefix 패턴을 **동시에** 갖는
합성 테스트(`Radio innerdataset="@dsFlat"`, `Dataset id="dsFlat"`가
`Form` 직계 자식, `Rows` 2건)를 만들어 fresh 변환했다 -- 결과:

```xml
<xf:select1 appearance="full" id="rdoFlat" renderType="radiogroup" style="...">
    <xf:choices>
        <xf:item><xf:label><![CDATA[Yes]]></xf:label><xf:value><![CDATA[Y]]></xf:value></xf:item>
        <xf:item><xf:label><![CDATA[No]]></xf:label><xf:value><![CDATA[N]]></xf:value></xf:item>
    </xf:choices>
</xf:select1>
```

정확히 기대한 대로 생성됨을 실제 실행으로 확인했다(테스트 fixture는
검증 후 삭제, Production 코드에는 포함되지 않음 -- 화면 ID
하드코딩과 무관한 일회성 architecture 검증용).

```
DATASET_LOOKUP_COVERAGE = HIGH
  (Objects wrapper 있음/없음 2개 실제 패턴 전부 확인, 경로 독립적 설계라
  다른 중첩 depth도 동일 메커니즘으로 커버될 것으로 판단 -- 전체
  subtree 스캔이므로 "3단계 중첩"류 미확인 케이스도 동일하게 동작할
  것이나 실제 evidence는 1~2단계까지만 확인했다)
```

## 3. innerdataset 값 normalization -- 실제 corpus evidence

```
grep 결과:
  corpus:        innerdataset="dsCode" (prefix 없음, 2건)
  실제 STT00030: innerdataset="@ds_BRoombo" (Combo, "@" prefix 있음, 1건)
```

**실제 STT00030.xfdl에 `@` prefix가 진짜로 존재함을 확인했다**(추측이
아니라 실제 파일에서 발견). 기존 `BindingAnalyzer.normalizeDataset()`
(이번 라운드 이전부터 존재하던 코드, 이번에 새로 추가하지 않음)가 이미
`while (s.startsWith("@")) s = s.substring(1);`로 이를 정확히 처리하고
있었다 -- 위 합성 테스트(`innerdataset="@dsFlat"`)로 실제 동작도
재확인했다(정상적으로 `dsFlat`으로 정규화되어 Dataset을 찾음).

`@dsCode`/`"dsCode"`(따옴표 포함)/`this.dsCode`/`parent.dsCode` 같은
다른 변형은 **corpus/실제 STT00030 어디에도 없다** -- 따라서 추가
normalization을 만들지 않는다(추측 normalization 금지 원칙 준수).

```
INNERDATASET_READ = YES (BindingAnalyzer, "@" prefix 정규화까지 실제
  evidence로 확인)
```

참고로 이 STT00030 사례(`MNG_BOCD` Combo)는 **`ds_BRoombo`가
실제 선언된 Dataset id `ds_BRcombo`와 철자가 다른, 소스 자체의
미해결 참조("oombo" vs "combo")**다 -- 우리 코드 버그가 아니라 소스
데이터 자체의 typo이며, 기존 `BindingAnalyzer`의 "innerdataset
미해결" 경고가 이미 이런 경우를 감지하도록 설계돼 있다(`if
(!datasetIds.contains(inner)) model.addWarning(...)`, 이번 라운드
변경 없음). `findDatasetById`도 이런 미해결 참조에 대해 안전하게
`null`을 반환하고 아무것도 하지 않는다(크래시 없음, 정적 choices
생성 안 함, 기존 런타임 setNodeSet 동작 그대로 유지) -- 이 typo
사례로 실제 안전한 fallback 동작까지 확인됐다(이 Combo 자체는 이번
Radio 전용 fix 대상이 아니므로 새 코드 경로에 진입하지 않지만,
`findDatasetById`의 안전성은 동일 메커니즘이므로 근거로 유효하다).

## 4. codecolumn/datacolumn 처리 확인

```
grep 결과: corpus + 실제 STT00030 전부 소문자 codecolumn=""/datacolumn=""
  형태만 발견(camelCase/다른 속성명 variation 0건)
```

`BindingAnalyzer.walk()`가 `element.getAttribute("codecolumn")`/
`element.getAttribute("datacolumn")`로 정확한 리터럴 이름을 그대로
읽는다(대소문자 변형 대응 없음 -- 그러나 evidence가 없으므로 추가
대응을 만들지 않는다).

```
CODECOLUMN_READ = YES
DATACOLUMN_READ = YES
ITEMSET_BINDING_MODEL_COMPLETE = YES
  (innerdataset/codecolumn/datacolumn 3개 모두 BindingAnalyzer가
  읽어 ItemsetBinding에 보존, WebSquareGenerator가 그대로 사용)
```

codecolumn/datacolumn이 누락된 경우(빈 문자열)는 기존 코드(이번
라운드 이전부터 존재)가 이미 `if (itemset.getCodeColumn().length() > 0
&& itemset.getDataColumn().length() > 0)` guard로 걸러 itemset 처리
전체(런타임 setNodeSet 포함)를 건너뛴다 -- 이 guard 안에서만 내 신규
함수가 호출되므로 누락 케이스는 안전하게 자동 제외된다.

## 5. Literal row 유무에 따른 정책 (이미 구현된 대로 재확인)

```java
Element rows = findDirectChild(dataset, "Rows");
if (rows == null) return;                      // Rows 자체가 없음 -> 정적 choices 생성 안 함
List<Element> sourceRows = directChildren(rows, "Row");
if (sourceRows.isEmpty()) return;               // <Rows/> 비어있음 -> 정적 choices 생성 안 함
```

이미 요청한 정책과 정확히 일치한다:

```
if SAME_XFDL_DATASET && LITERAL_ROWS_PRESENT:  STATIC_CHOICES = YES  (구현됨)
if SAME_XFDL_DATASET && LITERAL_ROWS_EMPTY:    STATIC_CHOICES = NO   (구현됨, return으로 조기 종료)
```

빈 Dataset(서버 io() 전용)에 임의로 item을 만들어내지 않는다 --
`items.isEmpty()`이면 `<xf:choices>` 자체를 붙이지 않는 추가 안전장치도
있다(코드 1797행).

```
LITERAL_ROWS_SUPPORTED = YES
EMPTY_RUNTIME_DATASET_POLICY = NO_STATIC_CHOICES(정확히 요청대로 구현됨,
  fake item 생성 0건)
```

## 6. Static choices + Runtime setNodeSet 동시 존재 -- 안전성 재확인

devpack 실제 런타임 코드(`wbm_B5170_babel_main.js`, minified)를 더
깊이 판독했다:

```js
// widget-level setNodeSet
l.prototype.setNodeSet=function(e,t,r){try{
  this.modelControl.unbindItemset(),
  e=n.D.isNull(e)?this.itemsetObj.nodeset:e, ...
  this.modelControl.setItemset(e,t,r),
  this.itemsetObj=this.modelControl.itemsetObj,
  this.itemsetObj.nodeset.match("data:")
    ? this._setDataCollectionItemArr(this.id)
    : this.itemArr=this.modelControl.getItemsetData(),
  ...
}}

// modelControl.unbindItemset (다른 위치, ae 관련 아님 -- 별도 control)
...this.modelControl.unbindItemset(),this.itemsetObj={nodeset:"",value:"",label:"",items:[]},
  null!=this.itemTable&&(this.itemTable.remove(),this.itemTable=null),...

// modelControl.setItemset
ae.prototype.setItemset=function(e,t,r){try{
  this.itemsetObj={nodeset:e||"",label:t||"",value:r||""}
}catch(e){...}}
```

해석:

1. `setNodeSet()`은 항상 먼저 `unbindItemset()`으로 기존 itemset 모델
   상태(`itemsetObj`, 내부 `itemTable`)를 **초기화**한다.
2. 그 다음 `setItemset()`으로 새 `nodeset`/`label`/`value`를 설정하고,
   `this.itemArr`을 **그 nodeset으로부터 다시 계산**한다
   (`getItemsetData()` 또는 `_setDataCollectionItemArr`).
3. 위젯의 실제 렌더링은 이 `itemArr`을 기반으로 한다(위젯 초기화
   시점의 정적 XML `<xf:choices>`가 아니라, 이 시점에 재계산된
   `itemArr`).

**중요**: 우리 fix가 정적 `<xf:choices>`에 넣는 데이터는 **`setNodeSet()`이
가리키는 것과 정확히 같은 dataset의 같은 codecolumn/datacolumn 값**이다
(`appendStaticChoicesIfLiteralDataset`가 `itemset.getDatasetId()`/
`getCodeColumn()`/`getDataColumn()`을 그대로 사용하므로 두 값의
출처가 100% 동일). 따라서 실제 페이지 로드가 완료돼 `setNodeSet()`이
실행되면, `itemArr`은 정적 XML에 있던 것과 **내용이 동일한 값으로
재계산**된다 -- 값이 달라지거나 중복되는 것이 아니라 같은 데이터로
한 번 더(사실상 동일하게) 설정되는 구조로 판단된다.

```
STATIC_AND_RUNTIME_BINDING_COMPATIBLE = YES
  (근거: unbind-then-rebind 구조 + 두 경로가 가리키는 데이터 출처가
  100% 동일 -- 코드 실측 기반, MEDIUM-HIGH confidence)
```

**단, 위험을 숨기지 않기 위해 명시한다**: 이것은 minified runtime
코드 정적 판독에 기반한 결론이며, 실제 브라우저에서 페이지를 로드해
DOM을 관찰한 것은 아니다(이 세션 도구로 실제 WebSquare 브라우저
runtime을 기동할 수 없음). "중복 렌더링"의 가능성을 완전히 0%로
단정하지는 않는다 -- 다만 근거(재계산 방식 + 동일 데이터 출처)가
`UNKNOWN`으로 남길 만큼 약하지 않다고 판단해 `YES`로 보고한다.
실제 폐쇄망 브라우저에서 Radio를 클릭/재조회해보는 것으로 최종 확인
가능하다(Studio Design-time이 아니라 실제 페이지 실행 확인).

## 7. STT00001 실제 파일 부재 -- 판정 수정

```
REAL_SCREEN_FRESH_CONVERSION_POSSIBLE = NO (여전히 파일 없음)
REAL_SCREEN_SOURCE_PATTERN_CONFIDENCE = HIGH
  근거: (a) 사용자가 실제 화면의 Radio가 SAME_XFDL innerdataset
  구조임을 확인, (b) 실제 STT00030.xfdl(같은 프로젝트의 다른 실제
  화면)에 동일한 innerdataset 관례("@" prefix 포함)가 실제로 존재함을
  이번에 직접 확인, (c) dcb7dfe의 dataset lookup이 실제 corpus의 2개
  위치 variation(Objects 유무) 전부와 실제 "@" prefix를 정확히
  처리함을 합성 테스트로 실행 확인.
REAL_SCREEN_RADIO_FIX_APPLIED = NOT_CONFIRMED (fresh conversion 없이는
  최종 확정 불가 -- 유지)
```

## 8. Production 수정 여부

architecture 재검증 결과, dcb7dfe의 `appendStaticChoicesIfLiteralDataset`/
`findDatasetById`는 "동일 XFDL 내부 innerdataset + literal Rows +
codecolumn/datacolumn" 패턴을 화면/경로 가정 없이 이미 generic하게
처리하고 있음을 실제 실행(합성 테스트) + 실제 corpus/실제 STT00030
evidence로 확인했다. **발견된 결함 없음 -> 추가 Production 수정하지
않는다.**

```
INNERDATASET_LITERAL_RADIO_SUPPORT = PASS
REAL_SCREEN_REVERIFY_NEEDED = YES
```

## 9. 최종 보고

```
SOURCE_RADIO_ITEM_SOURCE = INNER_DATASET
SOURCE_DATASET_SCOPE = SAME_XFDL

DATASET_LOOKUP_COVERAGE = HIGH
INNERDATASET_READ = YES
CODECOLUMN_READ = YES
DATACOLUMN_READ = YES

LITERAL_ROWS_SUPPORTED = YES
EMPTY_RUNTIME_DATASET_POLICY = NO_STATIC_CHOICES

STATIC_AND_RUNTIME_BINDING_COMPATIBLE = YES (코드 실측 기반,
  MEDIUM-HIGH confidence, 실제 브라우저 관찰은 아님)

DCB7DFE_ARCHITECTURE_MATCHES_REAL_PATTERN = YES

ADDITIONAL_PRODUCTION_FIX_REQUIRED = NO

REAL_SCREEN_RADIO_FIX_APPLIED = NOT_CONFIRMED

RADIO_REVERIFY_READY = YES
```
