# Phase 3 Tab Cross-Screen Reference

## `Form/TabAsyncAddSetUrlCallChild.xfdl:2`

- Function: `fnOpen`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageA`
- Target Symbol: `fnGetValue`
- Symbol Type: `FUNCTION`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child 함수 호출은 activateTab 이후 Promise 반환 가능

## `Form/TabAsyncLazyCallChild.xfdl:2`

- Function: `fnLazy`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnGetValue`
- Symbol Type: `FUNCTION`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child 함수 호출은 activateTab 이후 Promise 반환 가능

## `Form/TabAsyncLazyReadComponent.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabAsyncLazyReadDataset.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `dsSearch`
- Symbol Type: `DATASET`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabAsyncLazySyncReturn.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `child`
- Target Symbol: `fnGetValue`
- Symbol Type: `FUNCTION`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child 함수 호출은 activateTab 이후 Promise 반환 가능

## `Form/TabAsyncLoadedSyncReturn.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `child`
- Target Symbol: `fnGetValue`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `Form/TabEagerLifecycle.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnGetValue`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `Form/TabLazyLifecycle.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnGetValue`
- Symbol Type: `FUNCTION`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child 함수 호출은 activateTab 이후 Promise 반환 가능

## `Form/TabLifecycleOnloadFailure.xfdl:2`

- Function: `fnCall`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/OnloadFailureChild.xfdl`
- Tab / Page: `tabMain` / `child`
- Target Symbol: `fnValue`
- Symbol Type: `FUNCTION`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child 함수 호출은 activateTab 이후 Promise 반환 가능

## `Form/TabOnloadFailureHost.xfdl:2`

- Function: `fnCall`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/OnloadFailureChild.xfdl`
- Tab / Page: `tabMain` / `failChild`
- Target Symbol: `fnValue`
- Symbol Type: `FUNCTION`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child 함수 호출은 activateTab 이후 Promise 반환 가능

## `Form/TabParentAccess.xfdl:2`

- Function: `readChildFromParent`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/ParentAccessChild.xfdl`
- Tab / Page: `tabParent` / `pageChild`
- Target Symbol: `edtChild`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabParentCallsChild.xfdl:2`

- Function: `fnCall`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnSearch`
- Symbol Type: `FUNCTION`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child 함수 호출은 activateTab 이후 Promise 반환 가능

## `Form/TabParentCallsChildIndexed.xfdl:2`

- Function: `fnCallIndex`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnSearch`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `Form/TabParentCallsChildIndexed.xfdl:3`

- Function: `fnCallName`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnSearch`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `Form/TabParentReadsChildComponent.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabParentReadsChildComponent.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabParentReadsChildDataset.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `dsSearch`
- Symbol Type: `DATASET`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabParentReadsChildDataset.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `dsSearch`
- Symbol Type: `DATASET`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabSameScreenMultiInstance.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/Search.xfdl`
- Tab / Page: `tabMain` / `a`
- Target Symbol: `edtKeyword`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabSameScreenMultiInstance.xfdl:2`

- Function: `fnRead`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/Search.xfdl`
- Tab / Page: `tabMain` / `b`
- Target Symbol: `edtKeyword`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabStateSameScreenMultiInstance.xfdl:2`

- Function: `fnState`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `A`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabStateSameScreenMultiInstance.xfdl:2`

- Function: `fnState`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `B`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabStateSwitchRetention.xfdl:2`

- Function: `fnSwitch`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `A`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabStateSwitchRetention.xfdl:2`

- Function: `fnSwitch`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `A`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Message: lazy child Component/Dataset 동기 접근은 child render 전 사용할 수 없어 runtime 확인 필요

## `Form/TabStateUrlReplacement.xfdl:2`

- Function: `fnReplace`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `A`
- Target Symbol: `edtName`
- Symbol Type: `COMPONENT`
- Status: `RESOLVED`

## `Form/TabStateUrlReplacement.xfdl:2`

- Function: `fnReplace`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/Detail.xfdl`
- Tab / Page: `tabMain` / `A`
- Target Symbol: `edtName`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `Form/TabStateXjsGlobalIsolation.xfdl:2`

- Function: `fnCheck`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/StateXjsChild.xfdl`
- Tab / Page: `tabMain` / `A`
- Target Symbol: `fnNext`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `Form/TabStateXjsGlobalIsolation.xfdl:2`

- Function: `fnCheck`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/StateXjsChild.xfdl`
- Tab / Page: `tabMain` / `B`
- Target Symbol: `fnNext`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `Form/TabUnresolvedChildComponent.xfdl:2`

- Function: `fnBad`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `edtUnknown`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `Form/TabUnresolvedChildFunction.xfdl:2`

- Function: `fnBad`
- Direction: `PARENT_TO_CHILD`
- Target Screen: `TabContent/RuntimeBridgeChild.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnUnknown`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `TabContent/ChildBridgeParent.xfdl:2`

- Function: `fnChildCallParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildCallsParent.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnParent`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildCallsParent.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `edtParent`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildCallsParent.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `dsParent`
- Symbol Type: `DATASET`
- Status: `RESOLVED`

## `TabContent/ChildBridgeParent.xfdl:2`

- Function: `fnChildCallParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildReadsParentComponent.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnParent`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildReadsParentComponent.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `edtParent`
- Symbol Type: `COMPONENT`
- Status: `RESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildReadsParentComponent.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `dsParent`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `TabContent/ChildBridgeParent.xfdl:2`

- Function: `fnChildCallParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildReadsParentDataset.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `fnParent`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildReadsParentDataset.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `edtParent`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabChildReadsParentDataset.xfdl`
- Tab / Page: `tabMain` / `pageChild`
- Target Symbol: `dsParent`
- Symbol Type: `DATASET`
- Status: `RESOLVED`

## `TabContent/ChildBridgeParent.xfdl:2`

- Function: `fnChildCallParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabParentDirect.xfdl`
- Tab / Page: `tabMain` / `child`
- Target Symbol: `fnParent`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabParentDirect.xfdl`
- Tab / Page: `tabMain` / `child`
- Target Symbol: `edtParent`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `TabContent/ChildBridgeParent.xfdl:3`

- Function: `fnChildReadParent`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabParentDirect.xfdl`
- Tab / Page: `tabMain` / `child`
- Target Symbol: `dsParent`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `TabContent/ParentDepth2Second.xfdl:1`

- Function: `fnDepth2`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `TabContent/ParentDepth2First.xfdl`
- Tab / Page: `tabInner` / `second`
- Target Symbol: `fnFirst2`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepth2Second.xfdl:1`

- Function: `fnDepth2`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabParentDepth2.xfdl`
- Tab / Page: `tabInner` / `second`
- Target Symbol: `fnRoot2`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepth3Third.xfdl:1`

- Function: `fnDepth3`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `TabContent/ParentDepth3Second.xfdl`
- Tab / Page: `tab3` / `third`
- Target Symbol: `fnSecond3`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepth3Third.xfdl:1`

- Function: `fnDepth3`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `TabContent/ParentDepth3First.xfdl`
- Tab / Page: `tab3` / `third`
- Target Symbol: `fnFirst3`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepth3Third.xfdl:1`

- Function: `fnDepth3`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabParentDepth3.xfdl`
- Tab / Page: `tab3` / `third`
- Target Symbol: `fnRoot3`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepthThird.xfdl:2`

- Function: `fnDepth`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `TabContent/ParentDepthSecond.xfdl`
- Tab / Page: `tabSecond` / `third`
- Target Symbol: `fnSecond`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepthThird.xfdl:2`

- Function: `fnDepth`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `TabContent/ParentDepthFirst.xfdl`
- Tab / Page: `tabSecond` / `third`
- Target Symbol: `fnFirst`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepthThird.xfdl:2`

- Function: `fnDepth`
- Direction: `CHILD_TO_PARENT`
- Target Screen: `Form/TabParentDepthMain.xfdl`
- Tab / Page: `tabSecond` / `third`
- Target Symbol: `fnMain`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`

## `TabContent/ParentDepthThird.xfdl:3`

- Function: `fnTooDeep`
- Direction: `CHILD_TO_PARENT`
- Target Screen: ``
- Tab / Page: `tabSecond` / `third`
- Target Symbol: `fnMissing`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

## `TabContent/ParentDepthUnresolvedChild.xfdl:1`

- Function: `fnTooDeep`
- Direction: `CHILD_TO_PARENT`
- Target Screen: ``
- Tab / Page: `tabMain` / `child`
- Target Symbol: `fnMissing`
- Symbol Type: `UNKNOWN`
- Status: `UNRESOLVED`

