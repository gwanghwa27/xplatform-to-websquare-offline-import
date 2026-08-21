# Phase 3 Screen Conversion Report

## `Form/ComponentMethodConversion.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 1
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT index -> getSelectedIndex/setSelectedIndex : select/radio only
  - COMPONENT getCellProperty -> TODO : GridView property API is not 1:1
  - COMPONENT getBindCellIndex -> getColumnIndex : body bind column only

## `Form/ControlPropertyMatrix.xfdl`

- Components: total=18, supported=5, partial=12, TODO=1
- Properties: mapped=89, TODO=2
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT index -> getSelectedIndex/setSelectedIndex : select/radio only

## `Form/DatasetBinding.xfdl`

- Components: total=3, supported=1, partial=2, TODO=0
- Properties: mapped=18, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 2
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/EventMatrix.xfdl`

- Components: total=2, supported=1, partial=1, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=13, mapped=13, TODO=0
- Datasets: 0
- Scripts: internal=13, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/ExternalXjsBasic.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=1, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/ExternalXjsCircularDependency.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=2, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/ExternalXjsDependency.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=2, external globals=1, unresolved/ambiguous=0
- Transactions: 0

## `Form/ExternalXjsDirectGlobal.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=1, unresolved/ambiguous=0
- Transactions: 0

## `Form/ExternalXjsDuplicateFunction.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=1
- Transactions: 0

## `Form/ExternalXjsGlobalVariable.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=1, external globals=1, unresolved/ambiguous=0
- Transactions: 0

## `Form/ExternalXjsSameFileDuplicate.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=1
- Transactions: 0

## `Form/ExternalXjsTopLevelInit.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=1, external globals=1, unresolved/ambiguous=0
- Transactions: 0

## `Form/GridAdvancedPhase3.xfdl`

- Components: total=1, supported=0, partial=1, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 2
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/Main/TabExternalRelativePath.xfdl`

- Components: total=5, supported=1, partial=4, TODO=0
- Properties: mapped=14, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/NamespacePrefixed.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 1
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/NestedContainer.xfdl`

- Components: total=3, supported=2, partial=1, TODO=0
- Properties: mapped=13, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/ScopeShadowing.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 1
- Scripts: internal=2, external functions=2, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAddPage.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAddPageOverloads.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=2, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAddPageWithArguments.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAsyncAddSetUrlCallChild.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAsyncLazyCallChild.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAsyncLazyReadComponent.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAsyncLazyReadDataset.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAsyncLazySyncReturn.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAsyncLoadedSyncReturn.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAsyncRapidSelection.xfdl`

- Components: total=4, supported=0, partial=4, TODO=0
- Properties: mapped=11, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAsyncRapidSetUrl.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabAsyncRemoveReAdd.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=2, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabAsyncStaleLoadCallback.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabChildArguments.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabChildCallsParent.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 1
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabChildReadsParentComponent.xfdl`

- Components: total=3, supported=1, partial=2, TODO=0
- Properties: mapped=12, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabChildReadsParentDataset.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 1
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabCircularScreenDependency.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabContainer.xfdl`

- Components: total=5, supported=2, partial=3, TODO=0
- Properties: mapped=17, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabContentIdIsolation.xfdl`

- Components: total=3, supported=1, partial=2, TODO=0
- Properties: mapped=12, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT text -> getValue/setValue : label/text semantics component-specific

## `Form/TabContentLifecycle.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDuplicateContent.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicContent.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=3, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabDynamicIdCollision.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabDynamicIndexedSetUrl.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicPathXjs.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=1, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicReplaceState.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicRuntimeOnly.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicSameScreenMultiInstance.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabDynamicSelection.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicServicePath.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicSetUrlConditional.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicSetUrlStatic.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabDynamicSetUrlVariable.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabEagerLifecycle.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabExternalContent.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=13, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabExternalNested.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabExternalPreload.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=10, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabExternalXjs.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabInlineContent.xfdl`

- Components: total=3, supported=1, partial=2, TODO=0
- Properties: mapped=11, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT text -> getValue/setValue : label/text semantics component-specific

## `Form/TabInsertPage.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT insertTabpage -> scwin.__xpTabRuntime.insertPage : mapped with addTabIndex; uncommon overloads require review

## `Form/TabInsertPageOverloads.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT insertTabpage -> scwin.__xpTabRuntime.insertPage : mapped with addTabIndex; uncommon overloads require review

## `Form/TabLazyLifecycle.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabLifecycleDynamicAdd.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabLifecycleInactiveReplace.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabLifecycleOnloadFailure.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabLifecycleRemoveSelected.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabLifecycleSelectedReplace.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabLifecycleSetUrlReplace.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabLifecycleStaticEager.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabLifecycleStaticLazy.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabMissingContent.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabMixedContent.xfdl`

- Components: total=3, supported=1, partial=2, TODO=0
- Properties: mapped=11, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabNestedDynamic.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabOnloadFailureHost.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabOpenerBridge.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabOpenerFunction.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabOwnerFrameComponent.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabOwnerFrameDataset.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabOwnerFrameFunction.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabOwnerFrameHost.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentAccess.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentAndOpener.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentCallsChild.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentCallsChildIndexed.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentDepth2.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentDepth3.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentDepthMain.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=2, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentDirect.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentReadsChildComponent.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentReadsChildDataset.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentShadowHost.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabParentUnresolvedDepth.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabPreloadDynamicUrl.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabRemovePage.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabRemoveReAdd.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabRuntimeEvent.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=7, TODO=1
- Events: total=2, mapped=2, TODO=0
- Datasets: 0
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabRuntimeLexicalProtection.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=2, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabSameScreenMultiInstance.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabStateArgumentIsolation.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabStateGenerationIsolation.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabStateRemoveCleanup.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT addTabpage -> scwin.__xpTabRuntime.addPage : common id/label/data pattern mapped to async addTab; uncommon overloads require review

## `Form/TabStateRetention.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabStateSameScreenMultiInstance.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabStateSwitchRetention.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=9, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabStateUrlReplacement.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabStateXjsGlobalIsolation.xfdl`

- Components: total=3, supported=0, partial=3, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabStaticDynamicMixed.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabUnresolvedChildComponent.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TabUnresolvedChildFunction.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `Form/TransactionSample.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 2
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 1
- API review candidates:
  - GLOBAL transaction -> scwin.xpTransaction : structured transaction report; submission manual migration

## `Form/UnsupportedFeatures.xfdl`

- Components: total=2, supported=0, partial=1, TODO=1
- Properties: mapped=8, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=1
- Transactions: 0
- API review candidates:
  - GLOBAL open -> $p.openPopup : argument model differs; not auto-rewritten
  - GLOBAL setTimer -> TODO : timer owner/lifecycle wrapper required

## `TabContent/ChildBridgeParent.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 1
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/CircularTabA.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/CircularTabB.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/Detail.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/FirstTab.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=7, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/IdIsolationChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0
- API review candidates:
  - COMPONENT text -> getValue/setValue : label/text semantics component-specific

## `TabContent/LifecycleChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/NestedDynamicFirst.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=6, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/OnloadFailureChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 0
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=1
- Transactions: 0

## `TabContent/OwnerBridgeChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 1
- Scripts: internal=3, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentAccessChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepth2First.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=3, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepth2Second.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepth3First.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepth3Second.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepth3Third.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepthFirst.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=2, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepthSecond.xfdl`

- Components: total=2, supported=0, partial=2, TODO=0
- Properties: mapped=2, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepthThird.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentDepthUnresolvedChild.xfdl`

- Components: total=0, supported=0, partial=0, TODO=0
- Properties: mapped=0, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/ParentShadowChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/RuntimeBridgeChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 1
- Scripts: internal=3, external functions=1, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/Search.xfdl`

- Components: total=2, supported=2, partial=0, TODO=0
- Properties: mapped=10, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 0
- Scripts: internal=2, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/SearchXjs.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=1, mapped=1, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=1, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/SecondTab.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=5, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=0, external functions=0, external globals=0, unresolved/ambiguous=0
- Transactions: 0

## `TabContent/StateXjsChild.xfdl`

- Components: total=1, supported=1, partial=0, TODO=0
- Properties: mapped=1, TODO=0
- Events: total=0, mapped=0, TODO=0
- Datasets: 0
- Scripts: internal=1, external functions=1, external globals=1, unresolved/ambiguous=0
- Transactions: 0

