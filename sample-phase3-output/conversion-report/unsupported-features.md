# Phase 3 Unsupported / TODO Features

## `Form/ComponentMethodConversion.xfdl`

- PARTIAL COMPONENT Combo: itemset binding supported in Phase 3
- PARTIAL COMPONENT Grid: Formats/head/body/bind and selected input types
- API: COMPONENT index -> getSelectedIndex/setSelectedIndex : select/radio only
- API: COMPONENT getCellProperty -> TODO : GridView property API is not 1:1
- API: COMPONENT getBindCellIndex -> getColumnIndex : body bind column only

## `Form/ControlPropertyMatrix.xfdl`

- PARTIAL COMPONENT MaskEdit: mask semantics require property/script review
- PARTIAL COMPONENT Combo: itemset binding supported in Phase 3
- PARTIAL COMPONENT ListBox: visual list behavior may differ
- PARTIAL COMPONENT Radio: itemset binding supported in Phase 3
- PARTIAL COMPONENT CheckBox: single-value checkbox baseline
- PARTIAL COMPONENT Calendar: date/edit format partially mapped; uiplugin.inputCalendar (edit box + picker), not bare uiplugin.calendar (picker-only) -- see V6_COMPONENT_MAPPING_MISMATCH fix
- PARTIAL COMPONENT Spin: basic geometry/value only
- PARTIAL COMPONENT ImageViewer: basic image source/property mapping only
- PROPERTY ImageViewer.image: XPlatform URL/service alias must be resolved before WebSquare src mapping
- PARTIAL COMPONENT ProgressBar: basic value/property mapping only
- PARTIAL COMPONENT PopupDiv: popup runtime behavior requires manual migration
- PARTIAL COMPONENT WebBrowser: URL/navigation semantics require review
- PROPERTY WebBrowser.url: component-specific URL semantics; Tabpage static XFDL URL is handled by TabContentResolver
- PARTIAL COMPONENT FileUpload: server protocol/manual migration required
- COMPONENT FileDownload: no safe static UI mapping selected
- API: COMPONENT index -> getSelectedIndex/setSelectedIndex : select/radio only

## `Form/DatasetBinding.xfdl`

- PARTIAL COMPONENT Combo: itemset binding supported in Phase 3
- PARTIAL COMPONENT Radio: itemset binding supported in Phase 3

## `Form/EventMatrix.xfdl`

- PARTIAL COMPONENT Combo: itemset binding supported in Phase 3
- PARTIAL EVENT onsize: resize payload differs
- PARTIAL EVENT onmouseenter: mouseenter vs mouseover bubbling semantics differ
- PARTIAL EVENT onmouseleave: mouseleave vs mouseout bubbling semantics differ
- PARTIAL EVENT oncloseup: component-specific support
- PARTIAL EVENT ondropdown: component-specific support
- PARTIAL EVENT onitemchanged: item payload differs by component

## `Form/ExternalXjsDuplicateFunction.xfdl`

- AMBIGUOUS XJS SYMBOL: FUNCTION gfnDuplicate -> Script/DupA.xjs:1|Script/DupB.xjs:1

## `Form/ExternalXjsSameFileDuplicate.xfdl`

- AMBIGUOUS XJS SYMBOL: FUNCTION gfnSameFile -> Script/SameFileDuplicate.xjs:1|Script/SameFileDuplicate.xjs:2

## `Form/ExternalXjsTopLevelInit.xfdl`

- XJS INCLUDE TODO: TOP_LEVEL_XJS_INIT: Script/SideEffect.xjs -> [line 1: trace("side effect at include time");]

## `Form/GridAdvancedPhase3.xfdl`

- PARTIAL COMPONENT Grid: Formats/head/body/bind and selected input types

## `Form/Main/TabExternalRelativePath.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/NestedContainer.xfdl`

- PARTIAL COMPONENT GroupBox: group semantics/title require review

## `Form/TabAddPage.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.tabA source=function fnAdd(){ this.tabMain.addTabpage("tabA","A"); this.tabMain.tabA.set_url("../TabContent/Search.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAddPageOverloads.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAddPageWithArguments.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.argPage source=function fnAdd(){ this.tabMain.addTabpage("argPage","Args",{USER_ID:100,MODE:"EDIT"}); this.tabMain.argPage.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAsyncAddSetUrlCallChild.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageA source=function fnOpen(){ this.tabMain.addTabpage("pageA","A"); this.tabMain.pageA.set_url("../TabContent/RuntimeBridgeChild.xfdl"); return this.tabMain.pageA.form.fnGetValue(); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAsyncLazyCallChild.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnLazy(){ return this.tabMain.pageChild.form.fnGetValue(); }

## `Form/TabAsyncLazyReadComponent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnRead(){ return this.tabMain.pageChild.form.edtName.value; }

## `Form/TabAsyncLazyReadDataset.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnRead(){ return this.tabMain.pageChild.form.dsSearch.getRowCount(); }

## `Form/TabAsyncLazySyncReturn.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.child source=function fnRead(){ return this.tabMain.child.form.fnGetValue(); }

## `Form/TabAsyncLoadedSyncReturn.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.child source=function fnRead(){ return this.tabMain.child.form.fnGetValue(); }

## `Form/TabAsyncRapidSelection.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabAsyncRapidSetUrl.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageA source=function fnRapid(){ this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageA source=function fnRapid(){ this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); }

## `Form/TabAsyncRemoveReAdd.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.A source=function fnCycle(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("A"); this.tabMain.addTabpage("A","A2"); this.tabMain.A.set_url("../TabContent/Detail.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- TAB dynamic content loading: TAB STRUCTURE API removeTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAsyncStaleLoadCallback.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageA source=function fnReplace(){ this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/History.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageA source=function fnReplace(){ this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/History.xfdl"); }

## `Form/TabChildArguments.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.argPage source=function fnArgs(){ this.tabMain.addTabpage("argPage","ARG",{USER_ID:100,MODE:"EDIT"}); this.tabMain.argPage.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabChildCallsParent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabChildReadsParentComponent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabChildReadsParentDataset.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabCircularScreenDependency.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabContainer.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabContentIdIsolation.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- API: COMPONENT text -> getValue/setValue : label/text semantics component-specific

## `Form/TabContentLifecycle.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabDuplicateContent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabDynamicContent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabDynamic.pageA source=function changePage(){ this.tabDynamic.pageA.set_url("../TabContent/Search.xfdl"); }
- TAB dynamic content loading: url assignment line 3 target=tabDynamic.pageA source=function changePage2(){ this.tabDynamic.pageA.url = "../TabContent/Detail.xfdl"; }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 4
- TAB dynamic content loading: TAB STRUCTURE API removeTabpage line 4
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabDynamicIdCollision.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabDynamicIndexedSetUrl.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.tabpages[0] source=function fnByIndex(){ this.tabMain.tabpages[0].set_url("../TabContent/Search.xfdl"); }
- TAB dynamic content loading: set_url line 3 target=tabMain.tabpages["pageDetail"] source=function fnByName(){ this.tabMain.tabpages["pageDetail"].set_url("../TabContent/Detail.xfdl"); }

## `Form/TabDynamicPathXjs.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 3 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url(gvRuntimePath); }

## `Form/TabDynamicReplaceState.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.search source=function fnReplace(){ this.tabMain.search.set_url("../TabContent/Detail.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.search source=function fnReplace(){ this.tabMain.search.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabDynamicRuntimeOnly.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageSearch source=function fnOpen(name){ this.tabMain.pageSearch.set_url("../TabContent/"+name+".xfdl"); }

## `Form/TabDynamicSameScreenMultiInstance.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.A source=function fnAdd(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.addTabpage("B","B"); this.tabMain.B.set_url("../TabContent/Search.xfdl"); }
- TAB dynamic content loading: set_url line 2 target=tabMain.B source=function fnAdd(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.addTabpage("B","B"); this.tabMain.B.set_url("../TabContent/Search.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabDynamicSelection.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabDynamicServicePath.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url("TabForm::Detail.xfdl"); }

## `Form/TabDynamicSetUrlConditional.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageSearch source=function fnOpen(type){ if(type=="A") this.tabMain.pageSearch.set_url("../TabContent/Search.xfdl"); else this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabDynamicSetUrlStatic.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url("../TabContent/Search.xfdl"); }

## `Form/TabDynamicSetUrlVariable.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ var url="../TabContent/Search.xfdl"; this.tabMain.pageSearch.set_url(url); }

## `Form/TabEagerLifecycle.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnRead(){ return this.tabMain.pageChild.form.fnGetValue(); }

## `Form/TabExternalContent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabExternalNested.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabExternalPreload.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabExternalXjs.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabInlineContent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- API: COMPONENT text -> getValue/setValue : label/text semantics component-specific

## `Form/TabInsertPage.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.tabNew source=function fnInsert(){ this.tabMain.insertTabpage("tabNew",0,"NEW"); this.tabMain.tabNew.set_url("../TabContent/Detail.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API insertTabpage line 2
- API: COMPONENT insertTabpage -> scwin.__xpTabRuntime.insertPage : mapped with addTabIndex; uncommon overloads require review

## `Form/TabInsertPageOverloads.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: TAB STRUCTURE API insertTabpage line 2
- API: COMPONENT insertTabpage -> scwin.__xpTabRuntime.insertPage : mapped with addTabIndex; uncommon overloads require review

## `Form/TabLazyLifecycle.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnRead(){ return this.tabMain.pageChild.form.fnGetValue(); }

## `Form/TabLifecycleDynamicAdd.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.child source=function fnAdd(){ this.tabMain.addTabpage("child","Child"); this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabLifecycleInactiveReplace.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.B source=function fnReplaceInactive(){ this.tabMain.B.set_url("../TabContent/LifecycleChild.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.B source=function fnReplaceInactive(){ this.tabMain.B.set_url("../TabContent/LifecycleChild.xfdl"); }

## `Form/TabLifecycleOnloadFailure.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.child source=function fnCall(){ return this.tabMain.child.form.fnValue(); }

## `Form/TabLifecycleRemoveSelected.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: TAB STRUCTURE API removeTabpage line 2

## `Form/TabLifecycleSelectedReplace.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.child source=function fnReplace(){ this.tabMain.set_tabindex(0); this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.child source=function fnReplace(){ this.tabMain.set_tabindex(0); this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }

## `Form/TabLifecycleSetUrlReplace.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.child source=function fnReplace(){ this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.child source=function fnReplace(){ this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }

## `Form/TabLifecycleStaticEager.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabLifecycleStaticLazy.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabMissingContent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB CONTENT UNRESOLVED: tabMissing.pageMissing -> ../TabContent/MissingSearch.xfdl : 프로젝트 source tree에서 대상 XFDL을 찾지 못함: ../TabContent/MissingSearch.xfdl

## `Form/TabMixedContent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB MIXED INLINE/EXTERNAL: tabMixed.pageMixed -> external screen kept; inline children require manual composition
- TAB ANALYSIS WARNING: mixed inline/external Tabpage: tabMixed.pageMixed external=../TabContent/Search.xfdl (external content kept; inline child conversion suppressed)

## `Form/TabNestedDynamic.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabOnloadFailureHost.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.failChild source=function fnCall(){ return this.tabMain.failChild.form.fnValue(); }

## `Form/TabOpenerBridge.xfdl`

- TAB child/parent scope access review: opener line 2
- TAB child/parent scope access review: opener line 3

## `Form/TabOpenerFunction.xfdl`

- TAB child/parent scope access review: opener line 2

## `Form/TabOwnerFrameComponent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabOwnerFrameDataset.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabOwnerFrameFunction.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabOwnerFrameHost.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabParentAccess.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabParent.pageChild source=function readChildFromParent(){ trace(this.tabParent.pageChild.edtChild.value); }

## `Form/TabParentAndOpener.xfdl`

- TAB child/parent scope access review: parent line 2
- TAB child/parent scope access review: opener line 2

## `Form/TabParentCallsChild.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnCall(){ var v=this.tabMain.pageChild.form.fnSearch("ABC"); trace(v); }

## `Form/TabParentCallsChildIndexed.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD_INDEXED line 2 tab=tabMain source=function fnCallIndex(){ return this.tabMain.tabpages[0].form.fnSearch("IDX"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD_INDEXED line 3 tab=tabMain source=function fnCallName(){ return this.tabMain.tabpages["pageChild"].form.fnSearch("NAME"); }

## `Form/TabParentDepth2.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabParentDepth3.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabParentDepthMain.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabParentDirect.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabParentReadsChildComponent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnRead(){ trace(this.tabMain.pageChild.form.edtName.value); this.tabMain.pageChild.form.edtName.value="PARENT"; }

## `Form/TabParentReadsChildDataset.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnRead(){ trace(this.tabMain.pageChild.form.dsSearch.getRowCount()); trace(this.tabMain.pageChild.form.dsSearch.getColumn(0,"NAME")); }

## `Form/TabParentShadowHost.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabParentUnresolvedDepth.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabPreloadDynamicUrl.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabRemovePage.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: TAB STRUCTURE API removeTabpage line 2

## `Form/TabRemoveReAdd.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.tabA source=function fnCycle(){ this.tabMain.addTabpage("tabA","A"); this.tabMain.tabA.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("tabA"); this.tabMain.addTabpage("tabA","A2"); this.tabMain.tabA.set_url("../TabContent/Detail.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- TAB dynamic content loading: TAB STRUCTURE API removeTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabRuntimeEvent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PROPERTY Tab.canchange (inventory 없음)
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- PARTIAL EVENT canchange: Tab cancellable pre/post index payload requires adapter
- PARTIAL EVENT onchanged: old/new value payload differs by component

## `Form/TabRuntimeLexicalProtection.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabSameScreenMultiInstance.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.a source=function fnRead(){ trace(this.tabMain.a.form.edtKeyword.value); trace(this.tabMain.b.form.edtKeyword.value); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.b source=function fnRead(){ trace(this.tabMain.a.form.edtKeyword.value); trace(this.tabMain.b.form.edtKeyword.value); }

## `Form/TabStateArgumentIsolation.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.A source=function fnArgs(){ this.tabMain.addTabpage("A","A",{MODE:"A"}); this.tabMain.A.set_url("../TabContent/RuntimeBridgeChild.xfdl"); this.tabMain.addTabpage("B","B",{MODE:"B"}); this.tabMain.B.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- TAB dynamic content loading: set_url line 2 target=tabMain.B source=function fnArgs(){ this.tabMain.addTabpage("A","A",{MODE:"A"}); this.tabMain.A.set_url("../TabContent/RuntimeBridgeChild.xfdl"); this.tabMain.addTabpage("B","B",{MODE:"B"}); this.tabMain.B.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabStateGenerationIsolation.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.A source=function fnGen(){ this.tabMain.A.set_url("../TabContent/Detail.xfdl"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.A source=function fnGen(){ this.tabMain.A.set_url("../TabContent/Detail.xfdl"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); }

## `Form/TabStateRemoveCleanup.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.A source=function fnCycle(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("A"); }
- TAB dynamic content loading: TAB STRUCTURE API addTabpage line 2
- TAB dynamic content loading: TAB STRUCTURE API removeTabpage line 2
- API: COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabStateRetention.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `Form/TabStateSameScreenMultiInstance.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.A source=function fnState(){ this.tabMain.A.form.edtName.value="AAA"; this.tabMain.B.form.edtName.value="BBB"; }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.B source=function fnState(){ this.tabMain.A.form.edtName.value="AAA"; this.tabMain.B.form.edtName.value="BBB"; }

## `Form/TabStateSwitchRetention.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.A source=function fnSwitch(){ this.tabMain.set_tabindex(0); this.tabMain.A.form.edtName.value="ABC"; this.tabMain.set_tabindex(1); this.tabMain.set_tabindex(0); return this.tabMain.A.form.edtName.value; }

## `Form/TabStateUrlReplacement.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.A source=function fnReplace(){ this.tabMain.A.form.edtName.value="ABC"; this.tabMain.A.set_url("../TabContent/Detail.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.A source=function fnReplace(){ this.tabMain.A.form.edtName.value="ABC"; this.tabMain.A.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabStateXjsGlobalIsolation.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.A source=function fnCheck(){ var a=this.tabMain.A.form.fnNext(); var b=this.tabMain.B.form.fnNext(); return a+":"+b; }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.B source=function fnCheck(){ var a=this.tabMain.A.form.fnNext(); var b=this.tabMain.B.form.fnNext(); return a+":"+b; }

## `Form/TabStaticDynamicMixed.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## `Form/TabUnresolvedChildComponent.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnBad(){ trace(this.tabMain.pageChild.form.edtUnknown.value); }

## `Form/TabUnresolvedChildFunction.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB child/parent scope access review: PARENT_TO_TAB_CHILD line 2 tabPage=tabMain.pageChild source=function fnBad(){ this.tabMain.pageChild.form.fnUnknown(); }

## `Form/TransactionSample.xfdl`

- transaction -> WebSquare submission manual migration required
- API: GLOBAL transaction -> scwin.xpTransaction : structured transaction report; submission manual migration

## `Form/UnsupportedFeatures.xfdl`

- COMPONENT FileDownload: no safe static UI mapping selected
- PARTIAL COMPONENT Grid: Formats/head/body/bind and selected input types
- UNRESOLVED FUNCTION: gfnUnknown
- API: GLOBAL open -> $p.openPopup : argument model differs; not auto-rewritten
- API: GLOBAL setTimer -> TODO : timer owner/lifecycle wrapper required

## `TabContent/ChildBridgeParent.xfdl`

- TAB child/parent scope access review: parent line 2
- TAB child/parent scope access review: parent line 3

## `TabContent/CircularTabA.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/CircularTabB.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/FirstTab.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/IdIsolationChild.xfdl`

- API: COMPONENT text -> getValue/setValue : label/text semantics component-specific

## `TabContent/NestedDynamicFirst.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page
- TAB dynamic content loading: set_url line 2 target=tabInner.innerSeed source=function openSecond(){ this.tabInner.innerSeed.set_url("SecondTab.xfdl"); }

## `TabContent/OnloadFailureChild.xfdl`

- UNRESOLVED FUNCTION: Error

## `TabContent/OwnerBridgeChild.xfdl`

- TAB child/parent scope access review: getOwnerFrame line 3
- TAB child/parent scope access review: arguments line 3
- TAB child/parent scope access review: getOwnerFrame line 4

## `TabContent/ParentAccessChild.xfdl`

- TAB child/parent scope access review: getOwnerFrame line 2

## `TabContent/ParentDepth2First.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/ParentDepth2Second.xfdl`

- TAB child/parent scope access review: parent line 1

## `TabContent/ParentDepth3First.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/ParentDepth3Second.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/ParentDepth3Third.xfdl`

- TAB child/parent scope access review: parent line 1

## `TabContent/ParentDepthFirst.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/ParentDepthSecond.xfdl`

- PARTIAL COMPONENT Tab: inline Tabpage and static external XFDL url are converted; dynamic/mixed runtime behavior requires review
- PARTIAL COMPONENT Tabpage: inline tree preserved; external XFDL remains an independent WFrame content page

## `TabContent/ParentDepthThird.xfdl`

- TAB child/parent scope access review: parent line 2
- TAB child/parent scope access review: parent line 3

## `TabContent/ParentDepthUnresolvedChild.xfdl`

- TAB child/parent scope access review: parent line 1

## `TabContent/ParentShadowChild.xfdl`

- TAB child/parent scope access review: parent line 2

