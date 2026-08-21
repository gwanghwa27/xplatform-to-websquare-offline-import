# Phase 3 Tab External Content Dependency

## `Form/Main/TabExternalRelativePath.xfdl` → `divWrap.tabNested.pageSearch`

- Parent Screen: `Form/Main/TabExternalRelativePath.xfdl`
- Tab: `divWrap.tabNested`
- TabPage: `divWrap.tabNested.pageSearch`
- Content: `../../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/Main/TabExternalRelativePath.xfdl` → `divWrap.tabNested.pageDetail`

- Parent Screen: `Form/Main/TabExternalRelativePath.xfdl`
- Tab: `divWrap.tabNested`
- TabPage: `divWrap.tabNested.pageDetail`
- Content: `..\\..\\TabContent\\Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../../TabContent/Detail.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/Main/TabExternalRelativePath.xfdl` → `divWrap.tabNested.pageProject`

- Parent Screen: `Form/Main/TabExternalRelativePath.xfdl`
- Tab: `divWrap.tabNested`
- TabPage: `divWrap.tabNested.pageProject`
- Content: `TabContent/SecondTab.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/SecondTab.xfdl`
- Generated Target: `TabContent/SecondTab.xml`
- WebSquare Content src: `../../TabContent/SecondTab.xml`
- Resolution: `PROJECT_RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 프로젝트 루트 기준 경로

## Dynamic Tab Review: `Form/TabAddPage.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.tabA source=function fnAdd(){ this.tabMain.addTabpage("tabA","A"); this.tabMain.tabA.set_url("../TabContent/Search.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## Dynamic Tab Review: `Form/TabAddPageOverloads.xfdl`

- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## Dynamic Tab Review: `Form/TabAddPageWithArguments.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.argPage source=function fnAdd(){ this.tabMain.addTabpage("argPage","Args",{USER_ID:100,MODE:"EDIT"}); this.tabMain.argPage.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## Dynamic Tab Review: `Form/TabAsyncAddSetUrlCallChild.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageA source=function fnOpen(){ this.tabMain.addTabpage("pageA","A"); this.tabMain.pageA.set_url("../TabContent/RuntimeBridgeChild.xfdl"); return this.tabMain.pageA.form.fnGetValue(); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## `Form/TabAsyncLazyCallChild.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabAsyncLazyCallChild.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncLazyReadComponent.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabAsyncLazyReadComponent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncLazyReadDataset.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabAsyncLazyReadDataset.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncLazySyncReturn.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabAsyncLazySyncReturn.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncLoadedSyncReturn.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabAsyncLoadedSyncReturn.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncRapidSelection.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabAsyncRapidSelection.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncRapidSelection.xfdl` → `tabMain.B`

- Parent Screen: `Form/TabAsyncRapidSelection.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.B`
- Content: `../TabContent/Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../TabContent/Detail.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncRapidSelection.xfdl` → `tabMain.C`

- Parent Screen: `Form/TabAsyncRapidSelection.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.C`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabAsyncRapidSetUrl.xfdl` → `tabMain.pageA`

- Parent Screen: `Form/TabAsyncRapidSetUrl.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageA`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabAsyncRapidSetUrl.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageA source=function fnRapid(){ this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); }

## Dynamic Tab Review: `Form/TabAsyncRemoveReAdd.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.A source=function fnCycle(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("A"); this.tabMain.addTabpage("A","A2"); this.tabMain.A.set_url("../TabContent/Detail.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2
- [TAB TODO] TAB STRUCTURE API removeTabpage line 2

## `Form/TabAsyncStaleLoadCallback.xfdl` → `tabMain.pageA`

- Parent Screen: `Form/TabAsyncStaleLoadCallback.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageA`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabAsyncStaleLoadCallback.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageA source=function fnReplace(){ this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/History.xfdl"); }

## Dynamic Tab Review: `Form/TabChildArguments.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.argPage source=function fnArgs(){ this.tabMain.addTabpage("argPage","ARG",{USER_ID:100,MODE:"EDIT"}); this.tabMain.argPage.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## `Form/TabChildCallsParent.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabChildCallsParent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/ChildBridgeParent.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ChildBridgeParent.xfdl`
- Generated Target: `TabContent/ChildBridgeParent.xml`
- WebSquare Content src: `../TabContent/ChildBridgeParent.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 2, parent line 3]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabChildReadsParentComponent.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabChildReadsParentComponent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/ChildBridgeParent.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ChildBridgeParent.xfdl`
- Generated Target: `TabContent/ChildBridgeParent.xml`
- WebSquare Content src: `../TabContent/ChildBridgeParent.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 2, parent line 3]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabChildReadsParentDataset.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabChildReadsParentDataset.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/ChildBridgeParent.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ChildBridgeParent.xfdl`
- Generated Target: `TabContent/ChildBridgeParent.xml`
- WebSquare Content src: `../TabContent/ChildBridgeParent.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 2, parent line 3]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabCircularScreenDependency.xfdl` → `tabMain.cycle`

- Parent Screen: `Form/TabCircularScreenDependency.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.cycle`
- Content: `../TabContent/CircularTabA.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/CircularTabA.xfdl`
- Generated Target: `TabContent/CircularTabA.xml`
- WebSquare Content src: `../TabContent/CircularTabA.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabContentIdIsolation.xfdl` → `tabIso.pageChild`

- Parent Screen: `Form/TabContentIdIsolation.xfdl`
- Tab: `tabIso`
- TabPage: `tabIso.pageChild`
- Content: `../TabContent/IdIsolationChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/IdIsolationChild.xfdl`
- Generated Target: `TabContent/IdIsolationChild.xml`
- WebSquare Content src: `../TabContent/IdIsolationChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabContentLifecycle.xfdl` → `tabLife.pageLife`

- Parent Screen: `Form/TabContentLifecycle.xfdl`
- Tab: `tabLife`
- TabPage: `tabLife.pageLife`
- Content: `../TabContent/LifecycleChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/LifecycleChild.xfdl`
- Generated Target: `TabContent/LifecycleChild.xml`
- WebSquare Content src: `../TabContent/LifecycleChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabDuplicateContent.xfdl` → `tabDup.pageA`

- Parent Screen: `Form/TabDuplicateContent.xfdl`
- Tab: `tabDup`
- TabPage: `tabDup.pageA`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabDuplicateContent.xfdl` → `tabDup.pageB`

- Parent Screen: `Form/TabDuplicateContent.xfdl`
- Tab: `tabDup`
- TabPage: `tabDup.pageB`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabDynamicContent.xfdl`

- [TAB TODO] set_url line 2 target=tabDynamic.pageA source=function changePage(){ this.tabDynamic.pageA.set_url("../TabContent/Search.xfdl"); }
- [TAB TODO] url assignment line 3 target=tabDynamic.pageA source=function changePage2(){ this.tabDynamic.pageA.url = "../TabContent/Detail.xfdl"; }
- [TAB TODO] TAB STRUCTURE API addTabpage line 4
- [TAB TODO] TAB STRUCTURE API removeTabpage line 4

## Dynamic Tab Review: `Form/TabDynamicIdCollision.xfdl`

- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## Dynamic Tab Review: `Form/TabDynamicIndexedSetUrl.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.tabpages[0] source=function fnByIndex(){ this.tabMain.tabpages[0].set_url("../TabContent/Search.xfdl"); }
- [TAB TODO] set_url line 3 target=tabMain.tabpages["pageDetail"] source=function fnByName(){ this.tabMain.tabpages["pageDetail"].set_url("../TabContent/Detail.xfdl"); }

## Dynamic Tab Review: `Form/TabDynamicPathXjs.xfdl`

- [TAB TODO] set_url line 3 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url(gvRuntimePath); }

## `Form/TabDynamicReplaceState.xfdl` → `tabMain.search`

- Parent Screen: `Form/TabDynamicReplaceState.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.search`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabDynamicReplaceState.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.search source=function fnReplace(){ this.tabMain.search.set_url("../TabContent/Detail.xfdl"); }

## Dynamic Tab Review: `Form/TabDynamicRuntimeOnly.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageSearch source=function fnOpen(name){ this.tabMain.pageSearch.set_url("../TabContent/"+name+".xfdl"); }

## Dynamic Tab Review: `Form/TabDynamicSameScreenMultiInstance.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.A source=function fnAdd(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.addTabpage("B","B"); this.tabMain.B.set_url("../TabContent/Search.xfdl"); }
- [TAB TODO] set_url line 2 target=tabMain.B source=function fnAdd(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.addTabpage("B","B"); this.tabMain.B.set_url("../TabContent/Search.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## Dynamic Tab Review: `Form/TabDynamicServicePath.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url("TabForm::Detail.xfdl"); }

## Dynamic Tab Review: `Form/TabDynamicSetUrlConditional.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageSearch source=function fnOpen(type){ if(type=="A") this.tabMain.pageSearch.set_url("../TabContent/Search.xfdl"); else this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## Dynamic Tab Review: `Form/TabDynamicSetUrlStatic.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url("../TabContent/Search.xfdl"); }

## Dynamic Tab Review: `Form/TabDynamicSetUrlVariable.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ var url="../TabContent/Search.xfdl"; this.tabMain.pageSearch.set_url(url); }

## `Form/TabEagerLifecycle.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabEagerLifecycle.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabExternalContent.xfdl` → `tabMain.tabSearch`

- Parent Screen: `Form/TabExternalContent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.tabSearch`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabExternalContent.xfdl` → `tabMain.tabDetail`

- Parent Screen: `Form/TabExternalContent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.tabDetail`
- Content: `TabForm::Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../TabContent/Detail.xml`
- Resolution: `SERVICE_PREFIX`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: TypeDefinition Service url 기준

## `Form/TabExternalNested.xfdl` → `outerTab.first`

- Parent Screen: `Form/TabExternalNested.xfdl`
- Tab: `outerTab`
- TabPage: `outerTab.first`
- Content: `../TabContent/FirstTab.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/FirstTab.xfdl`
- Generated Target: `TabContent/FirstTab.xml`
- WebSquare Content src: `../TabContent/FirstTab.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabExternalPreload.xfdl` → `tabPreload.pageSearch`

- Parent Screen: `Form/TabExternalPreload.xfdl`
- Tab: `tabPreload`
- TabPage: `tabPreload.pageSearch`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabExternalPreload.xfdl` → `tabPreload.pageDetail`

- Parent Screen: `Form/TabExternalPreload.xfdl`
- Tab: `tabPreload`
- TabPage: `tabPreload.pageDetail`
- Content: `../TabContent/Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../TabContent/Detail.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabExternalXjs.xfdl` → `tabXjs.pageXjs`

- Parent Screen: `Form/TabExternalXjs.xfdl`
- Tab: `tabXjs`
- TabPage: `tabXjs.pageXjs`
- Content: `../TabContent/SearchXjs.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/SearchXjs.xfdl`
- Generated Target: `TabContent/SearchXjs.xml`
- WebSquare Content src: `../TabContent/SearchXjs.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabInsertPage.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.tabNew source=function fnInsert(){ this.tabMain.insertTabpage("tabNew",0,"NEW"); this.tabMain.tabNew.set_url("../TabContent/Detail.xfdl"); }
- [TAB TODO] TAB STRUCTURE API insertTabpage line 2

## Dynamic Tab Review: `Form/TabInsertPageOverloads.xfdl`

- [TAB TODO] TAB STRUCTURE API insertTabpage line 2

## `Form/TabLazyLifecycle.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabLazyLifecycle.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabLifecycleDynamicAdd.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.child source=function fnAdd(){ this.tabMain.addTabpage("child","Child"); this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## `Form/TabLifecycleInactiveReplace.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabLifecycleInactiveReplace.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabLifecycleInactiveReplace.xfdl` → `tabMain.B`

- Parent Screen: `Form/TabLifecycleInactiveReplace.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.B`
- Content: `../TabContent/Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../TabContent/Detail.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabLifecycleInactiveReplace.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.B source=function fnReplaceInactive(){ this.tabMain.B.set_url("../TabContent/LifecycleChild.xfdl"); }

## `Form/TabLifecycleOnloadFailure.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabLifecycleOnloadFailure.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/OnloadFailureChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/OnloadFailureChild.xfdl`
- Generated Target: `TabContent/OnloadFailureChild.xml`
- WebSquare Content src: `../TabContent/OnloadFailureChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabLifecycleRemoveSelected.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabLifecycleRemoveSelected.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabLifecycleRemoveSelected.xfdl` → `tabMain.B`

- Parent Screen: `Form/TabLifecycleRemoveSelected.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.B`
- Content: `../TabContent/Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../TabContent/Detail.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabLifecycleRemoveSelected.xfdl`

- [TAB TODO] TAB STRUCTURE API removeTabpage line 2

## `Form/TabLifecycleSelectedReplace.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabLifecycleSelectedReplace.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabLifecycleSelectedReplace.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.child source=function fnReplace(){ this.tabMain.set_tabindex(0); this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }

## `Form/TabLifecycleSetUrlReplace.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabLifecycleSetUrlReplace.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabLifecycleSetUrlReplace.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.child source=function fnReplace(){ this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }

## `Form/TabLifecycleStaticEager.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabLifecycleStaticEager.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/LifecycleChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/LifecycleChild.xfdl`
- Generated Target: `TabContent/LifecycleChild.xml`
- WebSquare Content src: `../TabContent/LifecycleChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabLifecycleStaticLazy.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabLifecycleStaticLazy.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/LifecycleChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/LifecycleChild.xfdl`
- Generated Target: `TabContent/LifecycleChild.xml`
- WebSquare Content src: `../TabContent/LifecycleChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabMissingContent.xfdl` → `tabMissing.pageMissing`

- Parent Screen: `Form/TabMissingContent.xfdl`
- Tab: `tabMissing`
- TabPage: `tabMissing.pageMissing`
- Content: `../TabContent/MissingSearch.xfdl`
- Source Attribute: `url`
- Resolved File: ``
- Generated Target: ``
- WebSquare Content src: ``
- Resolution: `PATH`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `UNRESOLVED`
- XJS Dependencies: []
- Note: 프로젝트 source tree에서 대상 XFDL을 찾지 못함: ../TabContent/MissingSearch.xfdl

## `Form/TabMixedContent.xfdl` → `tabMixed.pageMixed`

- Parent Screen: `Form/TabMixedContent.xfdl`
- Tab: `tabMixed`
- TabPage: `tabMixed.pageMixed`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Warnings: mixed inline/external; external page wins, inline children are not flattened

## `Form/TabNestedDynamic.xfdl` → `tabMain.first`

- Parent Screen: `Form/TabNestedDynamic.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.first`
- Content: `../TabContent/NestedDynamicFirst.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/NestedDynamicFirst.xfdl`
- Generated Target: `TabContent/NestedDynamicFirst.xml`
- WebSquare Content src: `../TabContent/NestedDynamicFirst.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabOnloadFailureHost.xfdl` → `tabMain.failChild`

- Parent Screen: `Form/TabOnloadFailureHost.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.failChild`
- Content: `../TabContent/OnloadFailureChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/OnloadFailureChild.xfdl`
- Generated Target: `TabContent/OnloadFailureChild.xml`
- WebSquare Content src: `../TabContent/OnloadFailureChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabOwnerFrameComponent.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabOwnerFrameComponent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/OwnerBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/OwnerBridgeChild.xfdl`
- Generated Target: `TabContent/OwnerBridgeChild.xml`
- WebSquare Content src: `../TabContent/OwnerBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [getOwnerFrame line 3, arguments line 3, getOwnerFrame line 4]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabOwnerFrameDataset.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabOwnerFrameDataset.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/OwnerBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/OwnerBridgeChild.xfdl`
- Generated Target: `TabContent/OwnerBridgeChild.xml`
- WebSquare Content src: `../TabContent/OwnerBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [getOwnerFrame line 3, arguments line 3, getOwnerFrame line 4]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabOwnerFrameFunction.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabOwnerFrameFunction.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/OwnerBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/OwnerBridgeChild.xfdl`
- Generated Target: `TabContent/OwnerBridgeChild.xml`
- WebSquare Content src: `../TabContent/OwnerBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [getOwnerFrame line 3, arguments line 3, getOwnerFrame line 4]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabOwnerFrameHost.xfdl` → `tabMain.ownerChild`

- Parent Screen: `Form/TabOwnerFrameHost.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.ownerChild`
- Content: `../TabContent/OwnerBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/OwnerBridgeChild.xfdl`
- Generated Target: `TabContent/OwnerBridgeChild.xml`
- WebSquare Content src: `../TabContent/OwnerBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [getOwnerFrame line 3, arguments line 3, getOwnerFrame line 4]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentAccess.xfdl` → `tabParent.pageChild`

- Parent Screen: `Form/TabParentAccess.xfdl`
- Tab: `tabParent`
- TabPage: `tabParent.pageChild`
- Content: `../TabContent/ParentAccessChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentAccessChild.xfdl`
- Generated Target: `TabContent/ParentAccessChild.xml`
- WebSquare Content src: `../TabContent/ParentAccessChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [getOwnerFrame line 2]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentCallsChild.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabParentCallsChild.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentCallsChildIndexed.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabParentCallsChildIndexed.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentDepth2.xfdl` → `tabMain.first`

- Parent Screen: `Form/TabParentDepth2.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.first`
- Content: `../TabContent/ParentDepth2First.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepth2First.xfdl`
- Generated Target: `TabContent/ParentDepth2First.xml`
- WebSquare Content src: `../TabContent/ParentDepth2First.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentDepth3.xfdl` → `tabMain.first`

- Parent Screen: `Form/TabParentDepth3.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.first`
- Content: `../TabContent/ParentDepth3First.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepth3First.xfdl`
- Generated Target: `TabContent/ParentDepth3First.xml`
- WebSquare Content src: `../TabContent/ParentDepth3First.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentDepthMain.xfdl` → `tabMain.first`

- Parent Screen: `Form/TabParentDepthMain.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.first`
- Content: `../TabContent/ParentDepthFirst.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepthFirst.xfdl`
- Generated Target: `TabContent/ParentDepthFirst.xml`
- WebSquare Content src: `../TabContent/ParentDepthFirst.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentDirect.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabParentDirect.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/ChildBridgeParent.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ChildBridgeParent.xfdl`
- Generated Target: `TabContent/ChildBridgeParent.xml`
- WebSquare Content src: `../TabContent/ChildBridgeParent.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 2, parent line 3]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentReadsChildComponent.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabParentReadsChildComponent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentReadsChildDataset.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabParentReadsChildDataset.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentShadowHost.xfdl` → `tabMain.shadowChild`

- Parent Screen: `Form/TabParentShadowHost.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.shadowChild`
- Content: `../TabContent/ParentShadowChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentShadowChild.xfdl`
- Generated Target: `TabContent/ParentShadowChild.xml`
- WebSquare Content src: `../TabContent/ParentShadowChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 2]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabParentUnresolvedDepth.xfdl` → `tabMain.child`

- Parent Screen: `Form/TabParentUnresolvedDepth.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.child`
- Content: `../TabContent/ParentDepthUnresolvedChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepthUnresolvedChild.xfdl`
- Generated Target: `TabContent/ParentDepthUnresolvedChild.xml`
- WebSquare Content src: `../TabContent/ParentDepthUnresolvedChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 1]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabPreloadDynamicUrl.xfdl` → `tabMain.pageSearch`

- Parent Screen: `Form/TabPreloadDynamicUrl.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageSearch`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabPreloadDynamicUrl.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabRemovePage.xfdl` → `tabMain.tabA`

- Parent Screen: `Form/TabRemovePage.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.tabA`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabRemovePage.xfdl`

- [TAB TODO] TAB STRUCTURE API removeTabpage line 2

## Dynamic Tab Review: `Form/TabRemoveReAdd.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.tabA source=function fnCycle(){ this.tabMain.addTabpage("tabA","A"); this.tabMain.tabA.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("tabA"); this.tabMain.addTabpage("tabA","A2"); this.tabMain.tabA.set_url("../TabContent/Detail.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2
- [TAB TODO] TAB STRUCTURE API removeTabpage line 2

## `Form/TabSameScreenMultiInstance.xfdl` → `tabMain.a`

- Parent Screen: `Form/TabSameScreenMultiInstance.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.a`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabSameScreenMultiInstance.xfdl` → `tabMain.b`

- Parent Screen: `Form/TabSameScreenMultiInstance.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.b`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabStateArgumentIsolation.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.A source=function fnArgs(){ this.tabMain.addTabpage("A","A",{MODE:"A"}); this.tabMain.A.set_url("../TabContent/RuntimeBridgeChild.xfdl"); this.tabMain.addTabpage("B","B",{MODE:"B"}); this.tabMain.B.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- [TAB TODO] set_url line 2 target=tabMain.B source=function fnArgs(){ this.tabMain.addTabpage("A","A",{MODE:"A"}); this.tabMain.A.set_url("../TabContent/RuntimeBridgeChild.xfdl"); this.tabMain.addTabpage("B","B",{MODE:"B"}); this.tabMain.B.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2

## `Form/TabStateGenerationIsolation.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabStateGenerationIsolation.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabStateGenerationIsolation.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.A source=function fnGen(){ this.tabMain.A.set_url("../TabContent/Detail.xfdl"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); }

## Dynamic Tab Review: `Form/TabStateRemoveCleanup.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.A source=function fnCycle(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("A"); }
- [TAB TODO] TAB STRUCTURE API addTabpage line 2
- [TAB TODO] TAB STRUCTURE API removeTabpage line 2

## `Form/TabStateRetention.xfdl` → `tabMain.search`

- Parent Screen: `Form/TabStateRetention.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.search`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStateRetention.xfdl` → `tabMain.detail`

- Parent Screen: `Form/TabStateRetention.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.detail`
- Content: `../TabContent/Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../TabContent/Detail.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStateSameScreenMultiInstance.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabStateSameScreenMultiInstance.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStateSameScreenMultiInstance.xfdl` → `tabMain.B`

- Parent Screen: `Form/TabStateSameScreenMultiInstance.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.B`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStateSwitchRetention.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabStateSwitchRetention.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStateSwitchRetention.xfdl` → `tabMain.B`

- Parent Screen: `Form/TabStateSwitchRetention.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.B`
- Content: `../TabContent/Detail.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Detail.xfdl`
- Generated Target: `TabContent/Detail.xml`
- WebSquare Content src: `../TabContent/Detail.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStateUrlReplacement.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabStateUrlReplacement.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabStateUrlReplacement.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.A source=function fnReplace(){ this.tabMain.A.form.edtName.value="ABC"; this.tabMain.A.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabStateXjsGlobalIsolation.xfdl` → `tabMain.A`

- Parent Screen: `Form/TabStateXjsGlobalIsolation.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.A`
- Content: `../TabContent/StateXjsChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/StateXjsChild.xfdl`
- Generated Target: `TabContent/StateXjsChild.xml`
- WebSquare Content src: `../TabContent/StateXjsChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabRuntimeState.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStateXjsGlobalIsolation.xfdl` → `tabMain.B`

- Parent Screen: `Form/TabStateXjsGlobalIsolation.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.B`
- Content: `../TabContent/StateXjsChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/StateXjsChild.xfdl`
- Generated Target: `TabContent/StateXjsChild.xml`
- WebSquare Content src: `../TabContent/StateXjsChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `EAGER`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabRuntimeState.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabStaticDynamicMixed.xfdl` → `tabMain.pageSearch`

- Parent Screen: `Form/TabStaticDynamicMixed.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageSearch`
- Content: `../TabContent/Search.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/Search.xfdl`
- Generated Target: `TabContent/Search.xml`
- WebSquare Content src: `../TabContent/Search.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `Form/TabStaticDynamicMixed.xfdl`

- [TAB TODO] set_url line 2 target=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabUnresolvedChildComponent.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabUnresolvedChildComponent.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `Form/TabUnresolvedChildFunction.xfdl` → `tabMain.pageChild`

- Parent Screen: `Form/TabUnresolvedChildFunction.xfdl`
- Tab: `tabMain`
- TabPage: `tabMain.pageChild`
- Content: `../TabContent/RuntimeBridgeChild.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/RuntimeBridgeChild.xfdl`
- Generated Target: `TabContent/RuntimeBridgeChild.xml`
- WebSquare Content src: `../TabContent/RuntimeBridgeChild.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: [Script/TabChildCommon.xjs]
- Note: 현재 XFDL 기준 상대 경로

## `TabContent/CircularTabA.xfdl` → `tabA.pageB`

- Parent Screen: `TabContent/CircularTabA.xfdl`
- Tab: `tabA`
- TabPage: `tabA.pageB`
- Content: `CircularTabB.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/CircularTabB.xfdl`
- Generated Target: `TabContent/CircularTabB.xml`
- WebSquare Content src: `./CircularTabB.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `TabContent/CircularTabB.xfdl` → `tabB.pageA`

- Parent Screen: `TabContent/CircularTabB.xfdl`
- Tab: `tabB`
- TabPage: `tabB.pageA`
- Content: `CircularTabA.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/CircularTabA.xfdl`
- Generated Target: `TabContent/CircularTabA.xml`
- WebSquare Content src: `./CircularTabA.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `TabContent/FirstTab.xfdl` → `innerTab.innerPage`

- Parent Screen: `TabContent/FirstTab.xfdl`
- Tab: `innerTab`
- TabPage: `innerTab.innerPage`
- Content: `./SecondTab.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/SecondTab.xfdl`
- Generated Target: `TabContent/SecondTab.xml`
- WebSquare Content src: `./SecondTab.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## Dynamic Tab Review: `TabContent/NestedDynamicFirst.xfdl`

- [TAB TODO] set_url line 2 target=tabInner.innerSeed source=function openSecond(){ this.tabInner.innerSeed.set_url("SecondTab.xfdl"); }

## `TabContent/ParentDepth2First.xfdl` → `tabInner.second`

- Parent Screen: `TabContent/ParentDepth2First.xfdl`
- Tab: `tabInner`
- TabPage: `tabInner.second`
- Content: `ParentDepth2Second.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepth2Second.xfdl`
- Generated Target: `TabContent/ParentDepth2Second.xml`
- WebSquare Content src: `./ParentDepth2Second.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 1]
- Note: 현재 XFDL 기준 상대 경로

## `TabContent/ParentDepth3First.xfdl` → `tab2.second`

- Parent Screen: `TabContent/ParentDepth3First.xfdl`
- Tab: `tab2`
- TabPage: `tab2.second`
- Content: `ParentDepth3Second.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepth3Second.xfdl`
- Generated Target: `TabContent/ParentDepth3Second.xml`
- WebSquare Content src: `./ParentDepth3Second.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `TabContent/ParentDepth3Second.xfdl` → `tab3.third`

- Parent Screen: `TabContent/ParentDepth3Second.xfdl`
- Tab: `tab3`
- TabPage: `tab3.third`
- Content: `ParentDepth3Third.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepth3Third.xfdl`
- Generated Target: `TabContent/ParentDepth3Third.xml`
- WebSquare Content src: `./ParentDepth3Third.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 1]
- Note: 현재 XFDL 기준 상대 경로

## `TabContent/ParentDepthFirst.xfdl` → `tabFirst.second`

- Parent Screen: `TabContent/ParentDepthFirst.xfdl`
- Tab: `tabFirst`
- TabPage: `tabFirst.second`
- Content: `ParentDepthSecond.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepthSecond.xfdl`
- Generated Target: `TabContent/ParentDepthSecond.xml`
- WebSquare Content src: `./ParentDepthSecond.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Note: 현재 XFDL 기준 상대 경로

## `TabContent/ParentDepthSecond.xfdl` → `tabSecond.third`

- Parent Screen: `TabContent/ParentDepthSecond.xfdl`
- Tab: `tabSecond`
- TabPage: `tabSecond.third`
- Content: `ParentDepthThird.xfdl`
- Source Attribute: `url`
- Resolved File: `TabContent/ParentDepthThird.xfdl`
- Generated Target: `TabContent/ParentDepthThird.xml`
- WebSquare Content src: `./ParentDepthThird.xml`
- Resolution: `RELATIVE`
- Loading Mode: `LAZY`
- Runtime Scope: `independent wframe scope`
- Status: `CONVERTED`
- XJS Dependencies: []
- Child Parent/Owner Access Review: [parent line 2, parent line 3]
- Note: 현재 XFDL 기준 상대 경로

## Shared converted screen reuse

동일 XFDL은 출력 파일을 한 번만 생성하며, 각 Tab Content는 별도 wframe/scope 인스턴스로 해당 파일을 로드합니다.

- `TabContent/Search.xfdl`: referenced by 21 Tab contents
- `TabContent/Detail.xfdl`: referenced by 8 Tab contents
- `TabContent/SecondTab.xfdl`: referenced by 2 Tab contents
- `TabContent/RuntimeBridgeChild.xfdl`: referenced by 18 Tab contents
- `TabContent/ChildBridgeParent.xfdl`: referenced by 4 Tab contents
- `TabContent/CircularTabA.xfdl`: referenced by 2 Tab contents
- `TabContent/LifecycleChild.xfdl`: referenced by 3 Tab contents
- `TabContent/OnloadFailureChild.xfdl`: referenced by 2 Tab contents
- `TabContent/OwnerBridgeChild.xfdl`: referenced by 4 Tab contents
- `TabContent/StateXjsChild.xfdl`: referenced by 2 Tab contents

