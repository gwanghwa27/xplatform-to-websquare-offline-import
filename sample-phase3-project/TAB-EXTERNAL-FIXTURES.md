# Phase 3 Tab External Content Fixtures

| Fixture | 검증 목적 |
|---|---|
| `Form/TabInlineContent.xfdl` | inline Tabpage 기존 동작 유지 |
| `Form/TabExternalContent.xfdl` | relative + Service prefix external XFDL |
| `Form/Main/TabExternalRelativePath.xfdl` | nested container + relative/backslash/project-relative path |
| `Form/TabExternalNested.xfdl` | Main → FirstTab → SecondTab recursive dependency |
| `Form/TabExternalXjs.xfdl` | child XFDL XJS selective dependency |
| `Form/TabDuplicateContent.xfdl` | 동일 XFDL 2 Tab 참조, physical output 중복 금지 |
| `Form/TabMissingContent.xfdl` | unresolved file report, fake page 생성 금지 |
| `Form/TabDynamicContent.xfdl` | set_url/url assignment/add/remove TODO |
| `Form/TabContentLifecycle.xfdl` | child Form onload → child onpageload |
| `Form/TabContentIdIsolation.xfdl` | parent/child 동일 component ID scope 분리 |
| `Form/TabExternalPreload.xfdl` | preload=true eager mapping + selectedTabIndex |
| `Form/TabMixedContent.xfdl` | inline + external mixed TODO, flatten 금지 |
| `Form/TabParentAccess.xfdl` | parent → external child direct dotted access report |
| `TabContent/ParentAccessChild.xfdl` | child getOwnerFrame candidate report |
| `Script/TabCommon.xjs` | external child XJS dependency |

`default_typedef.xml`에는 `TabForm`과 `Script` Service를 정의하여 logical service prefix 경로도 검증한다.
