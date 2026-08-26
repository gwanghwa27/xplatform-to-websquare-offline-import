# w2:group vs xf:group Studio A/B 진단 (Quick)

## 배경

이전 라운드([div-qname-audit 관련 대화 로그])에서 로컬 WebSquare devpack 런타임
증거만으로는 `DIV_TARGET_QNAME_MISMATCH`를 확정할 수 없었다(`W2_GROUP_DESIGN_RENDER_
SUPPORTED = EVIDENCE_INSUFFICIENT` -- Studio 디자인 타임 캔버스 도구가 로컬
devpack에 없음). 이번 라운드는 실제 확보된 `STT00030.xml`(폐쇄망 실제 fresh
output, 사용자 제공)을 기반으로, Studio에서 사용자가 직접 A/B 비교할 수 있는
진단 XML 2개만 만든다. Production 수정은 하지 않는다.

## 산출물

- `tools/diagnostic/qname-ab/STT00030_group_A_w2.xml` -- 실제 `STT00030.xml`
  그대로(무수정, byte-identical 복사).
- `tools/diagnostic/qname-ab/STT00030_group_B_xf.xml` -- A에서 `Div01`/`Div00`/
  `Div02`/`Div03` 4개 컨테이너의 QName만 `w2:group` -> `xf:group`으로 변경(여는
  태그/닫는 태그 각 4개, 총 8줄). 그 외 문자 하나도 변경하지 않았다(id/style/
  position:absolute/percentage/child order/Calendar/Combo/Button/Grid/
  grp_resultArea/grp_main/script/dataCollection 전부 A와 byte-identical).

이 두 파일은 진단용 artifact이며 Production 산출물이 아니다. `STT00030` id는
이 diagnostic 파일 이름/내용에만 존재하고, Production Java 코드에는 어디에도
하드코딩하지 않았다.

**Provenance(repository hygiene policy 적용, 상세: `repository-external-artifact-policy.md`)**:
이 두 파일은 실제 업무 화면(STT00030) 데이터를 담고 있어 Git 추적 대상에서
제외했다(`EXTERNAL_ARTIFACT`, `git rm --cached` 적용). local working copy에는
그대로 남아 있으며, 아래 자동 비교 결과와 결론은 이 문서에 보존한다.

## 자동 비교 Gate (실행 결과)

두 파일 모두 XML well-formed 확인(Python `xml.dom.minidom`):
```
STT00030_group_A_w2.xml PARSE_OK
STT00030_group_B_xf.xml PARSE_OK
```

`xml.etree.ElementTree` 기반 구조 비교(두 트리를 document-order로 동시 순회,
각 위치의 tag/attrib/자식 개수를 비교):
```
AB_CHANGED_ELEMENT_COUNT = 4          (Div01, Div00, Div02, Div03)
AB_CHANGED_ATTRIBUTE_COUNT = 0        (id/style/tabIndex/value 등 전부 동일)
AB_CHANGED_STYLE_COUNT = 0            (attribute 비교에 포함, style 단독 diff 없음)
AB_CHANGED_CHILD_COUNT = 0            (각 엘리먼트의 자식 개수 전부 동일)
AB_CHANGED_QNAME_ONLY = PASS
```

원시 텍스트 diff(`diff A B`)로도 교차 확인: 정확히 8줄(4개 여는 태그 + 4개
닫는 태그)만 변경, 그 사이 내용(Calendar/Combo/Button/span 등)은 한 글자도
바뀌지 않음.

A:
```
Div01 = w2:group
Div00 = w2:group
Div02 = w2:group
Div03 = w2:group
```

B:
```
Div01 = xf:group
Div00 = xf:group
Div02 = xf:group
Div03 = xf:group
```

`grp_resultArea`/`grp_main`/`Grid01_gridGroup`/`Grid00_gridGroup`은 A/B 둘 다
무변경(기존 `xf:group` 그대로).

## Production Java diff

```
git diff --stat HEAD -- src/main/java  =>  (empty)
```
`NO_PRODUCTION_CODE_CHANGE`. 이번 라운드는 diagnostic 파일 2개만 생성했다.

## 사용자 Studio 판정용 (다음 단계, 사용자가 실행)

두 파일을 동일한 폐쇄망 Studio 환경에서 열어 비교:

1. **A와 B가 동일하게 보임** (Div 내부 Calendar/Combo/Button, 우측 Div02/Div03
   Button 모두 여전히 안 보임/동일하게 실패) -> `DIV_TARGET_QNAME_MISMATCH = NO`.
   QName 가설을 종료하고 다음 원인(예: Div 자신의 `position:absolute`가 Studio
   Design Canvas에서 자식의 containing block으로 실제 인정되는지, 또는 다른
   구조적 요인)으로 조사를 이동한다.
2. **B에서 개선** (Div 내부 Calendar/Combo/Button 또는 우측 Button 표시가
   나아짐) -> `DIV_TARGET_QNAME_MISMATCH = CONFIRMED`. 다음 라운드에서
   `ComponentMappingRegistry`의 `Div`/`GroupBox`/`PopupDiv`/`Tabpage` 타겟
   QName을 `w2:group` -> `xf:group`으로 generic 변경하는 Production 수정을
   진행한다(특정 화면 하드코딩 없이, 매핑 테이블 단위로).
3. **둘 다 동일하게 실패**는 위 1)과 동일 결론.

## Status

```
DIV_TARGET_QNAME_MISMATCH = UNRESOLVED (Studio A/B 실측 대기)
STATIC_VERIFIED (A/B 파일 구조 비교만, 실제 Studio 렌더링 결과 아님)
STUDIO_DESIGN_REQUIRED
```

Studio 재검증 전 `FIXED`/`CONFIRMED`/`PATCH_READY`를 주장하지 않는다.
`position:relative`(grp_main, commit `defe9dc`) 수정은 이번 라운드에서
유지/revert 어느 쪽도 하지 않았다 -- 무변경.
