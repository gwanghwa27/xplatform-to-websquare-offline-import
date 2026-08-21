# Phase 3 Tab Content Unresolved / Dynamic

## [TAB TODO] dynamic content loading `Form/TabAddPage.xfdl`

- set_url line 2 target=tabMain.tabA source=function fnAdd(){ this.tabMain.addTabpage("tabA","A"); this.tabMain.tabA.set_url("../TabContent/Search.xfdl"); }
- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabAddPageOverloads.xfdl`

- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabAddPageWithArguments.xfdl`

- set_url line 2 target=tabMain.argPage source=function fnAdd(){ this.tabMain.addTabpage("argPage","Args",{USER_ID:100,MODE:"EDIT"}); this.tabMain.argPage.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabAsyncAddSetUrlCallChild.xfdl`

- set_url line 2 target=tabMain.pageA source=function fnOpen(){ this.tabMain.addTabpage("pageA","A"); this.tabMain.pageA.set_url("../TabContent/RuntimeBridgeChild.xfdl"); return this.tabMain.pageA.form.fnGetValue(); }
- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabAsyncRapidSetUrl.xfdl`

- set_url line 2 target=tabMain.pageA source=function fnRapid(){ this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabAsyncRemoveReAdd.xfdl`

- set_url line 2 target=tabMain.A source=function fnCycle(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("A"); this.tabMain.addTabpage("A","A2"); this.tabMain.A.set_url("../TabContent/Detail.xfdl"); }
- TAB STRUCTURE API addTabpage line 2
- TAB STRUCTURE API removeTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabAsyncStaleLoadCallback.xfdl`

- set_url line 2 target=tabMain.pageA source=function fnReplace(){ this.tabMain.pageA.set_url("../TabContent/Search.xfdl"); this.tabMain.pageA.set_url("../TabContent/Detail.xfdl"); this.tabMain.pageA.set_url("../TabContent/History.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabChildArguments.xfdl`

- set_url line 2 target=tabMain.argPage source=function fnArgs(){ this.tabMain.addTabpage("argPage","ARG",{USER_ID:100,MODE:"EDIT"}); this.tabMain.argPage.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabDynamicContent.xfdl`

- set_url line 2 target=tabDynamic.pageA source=function changePage(){ this.tabDynamic.pageA.set_url("../TabContent/Search.xfdl"); }
- url assignment line 3 target=tabDynamic.pageA source=function changePage2(){ this.tabDynamic.pageA.url = "../TabContent/Detail.xfdl"; }
- TAB STRUCTURE API addTabpage line 4
- TAB STRUCTURE API removeTabpage line 4

## [TAB TODO] dynamic content loading `Form/TabDynamicIdCollision.xfdl`

- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabDynamicIndexedSetUrl.xfdl`

- set_url line 2 target=tabMain.tabpages[0] source=function fnByIndex(){ this.tabMain.tabpages[0].set_url("../TabContent/Search.xfdl"); }
- set_url line 3 target=tabMain.tabpages["pageDetail"] source=function fnByName(){ this.tabMain.tabpages["pageDetail"].set_url("../TabContent/Detail.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabDynamicPathXjs.xfdl`

- set_url line 3 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url(gvRuntimePath); }

## [TAB TODO] dynamic content loading `Form/TabDynamicReplaceState.xfdl`

- set_url line 2 target=tabMain.search source=function fnReplace(){ this.tabMain.search.set_url("../TabContent/Detail.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabDynamicRuntimeOnly.xfdl`

- set_url line 2 target=tabMain.pageSearch source=function fnOpen(name){ this.tabMain.pageSearch.set_url("../TabContent/"+name+".xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabDynamicSameScreenMultiInstance.xfdl`

- set_url line 2 target=tabMain.A source=function fnAdd(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.addTabpage("B","B"); this.tabMain.B.set_url("../TabContent/Search.xfdl"); }
- set_url line 2 target=tabMain.B source=function fnAdd(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.addTabpage("B","B"); this.tabMain.B.set_url("../TabContent/Search.xfdl"); }
- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabDynamicServicePath.xfdl`

- set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url("TabForm::Detail.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabDynamicSetUrlConditional.xfdl`

- set_url line 2 target=tabMain.pageSearch source=function fnOpen(type){ if(type=="A") this.tabMain.pageSearch.set_url("../TabContent/Search.xfdl"); else this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabDynamicSetUrlStatic.xfdl`

- set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ this.tabMain.pageSearch.set_url("../TabContent/Search.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabDynamicSetUrlVariable.xfdl`

- set_url line 2 target=tabMain.pageSearch source=function fnOpen(){ var url="../TabContent/Search.xfdl"; this.tabMain.pageSearch.set_url(url); }

## [TAB TODO] dynamic content loading `Form/TabInsertPage.xfdl`

- set_url line 2 target=tabMain.tabNew source=function fnInsert(){ this.tabMain.insertTabpage("tabNew",0,"NEW"); this.tabMain.tabNew.set_url("../TabContent/Detail.xfdl"); }
- TAB STRUCTURE API insertTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabInsertPageOverloads.xfdl`

- TAB STRUCTURE API insertTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabLifecycleDynamicAdd.xfdl`

- set_url line 2 target=tabMain.child source=function fnAdd(){ this.tabMain.addTabpage("child","Child"); this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }
- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabLifecycleInactiveReplace.xfdl`

- set_url line 2 target=tabMain.B source=function fnReplaceInactive(){ this.tabMain.B.set_url("../TabContent/LifecycleChild.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabLifecycleRemoveSelected.xfdl`

- TAB STRUCTURE API removeTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabLifecycleSelectedReplace.xfdl`

- set_url line 2 target=tabMain.child source=function fnReplace(){ this.tabMain.set_tabindex(0); this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabLifecycleSetUrlReplace.xfdl`

- set_url line 2 target=tabMain.child source=function fnReplace(){ this.tabMain.child.set_url("../TabContent/LifecycleChild.xfdl"); }

## [TAB CONTENT UNRESOLVED] `Form/TabMissingContent.xfdl`

- tab: `tabMissing`
- tabPage: `tabMissing.pageMissing`
- content: `../TabContent/MissingSearch.xfdl`
- reason: 프로젝트 source tree에서 대상 XFDL을 찾지 못함: ../TabContent/MissingSearch.xfdl

## [TAB TODO] dynamic content loading `Form/TabPreloadDynamicUrl.xfdl`

- set_url line 2 target=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabRemovePage.xfdl`

- TAB STRUCTURE API removeTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabRemoveReAdd.xfdl`

- set_url line 2 target=tabMain.tabA source=function fnCycle(){ this.tabMain.addTabpage("tabA","A"); this.tabMain.tabA.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("tabA"); this.tabMain.addTabpage("tabA","A2"); this.tabMain.tabA.set_url("../TabContent/Detail.xfdl"); }
- TAB STRUCTURE API addTabpage line 2
- TAB STRUCTURE API removeTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabStateArgumentIsolation.xfdl`

- set_url line 2 target=tabMain.A source=function fnArgs(){ this.tabMain.addTabpage("A","A",{MODE:"A"}); this.tabMain.A.set_url("../TabContent/RuntimeBridgeChild.xfdl"); this.tabMain.addTabpage("B","B",{MODE:"B"}); this.tabMain.B.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- set_url line 2 target=tabMain.B source=function fnArgs(){ this.tabMain.addTabpage("A","A",{MODE:"A"}); this.tabMain.A.set_url("../TabContent/RuntimeBridgeChild.xfdl"); this.tabMain.addTabpage("B","B",{MODE:"B"}); this.tabMain.B.set_url("../TabContent/RuntimeBridgeChild.xfdl"); }
- TAB STRUCTURE API addTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabStateGenerationIsolation.xfdl`

- set_url line 2 target=tabMain.A source=function fnGen(){ this.tabMain.A.set_url("../TabContent/Detail.xfdl"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabStateRemoveCleanup.xfdl`

- set_url line 2 target=tabMain.A source=function fnCycle(){ this.tabMain.addTabpage("A","A"); this.tabMain.A.set_url("../TabContent/Search.xfdl"); this.tabMain.removeTabpage("A"); }
- TAB STRUCTURE API addTabpage line 2
- TAB STRUCTURE API removeTabpage line 2

## [TAB TODO] dynamic content loading `Form/TabStateUrlReplacement.xfdl`

- set_url line 2 target=tabMain.A source=function fnReplace(){ this.tabMain.A.form.edtName.value="ABC"; this.tabMain.A.set_url("../TabContent/Detail.xfdl"); }

## [TAB TODO] dynamic content loading `Form/TabStaticDynamicMixed.xfdl`

- set_url line 2 target=tabMain.pageSearch source=function fnChange(){ this.tabMain.pageSearch.set_url("../TabContent/Detail.xfdl"); }

## [TAB TODO] dynamic content loading `TabContent/NestedDynamicFirst.xfdl`

- set_url line 2 target=tabInner.innerSeed source=function openSecond(){ this.tabInner.innerSeed.set_url("SecondTab.xfdl"); }

