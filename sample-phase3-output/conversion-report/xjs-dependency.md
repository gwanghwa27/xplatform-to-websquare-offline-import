# Phase 3 XJS Dependency Report

## Repository

- `Script/CircularA.xjs`: functions=1, globals=0, includes=[CircularB.xjs], topLevelInit=0
- `Script/CircularB.xjs`: functions=1, globals=0, includes=[CircularA.xjs], topLevelInit=0
- `Script/Common.xjs`: functions=4, globals=1, includes=[Utility.xjs], topLevelInit=0
- `Script/DupA.xjs`: functions=1, globals=0, includes=[], topLevelInit=0
- `Script/DupB.xjs`: functions=1, globals=0, includes=[], topLevelInit=0
- `Script/SameFileDuplicate.xjs`: functions=1, globals=0, includes=[], topLevelInit=0
- `Script/ScopeHelper.xjs`: functions=2, globals=0, includes=[], topLevelInit=0
- `Script/SideEffect.xjs`: functions=1, globals=1, includes=[], topLevelInit=1
- `Script/TabChildCommon.xjs`: functions=1, globals=0, includes=[], topLevelInit=0
- `Script/TabCommon.xjs`: functions=1, globals=0, includes=[], topLevelInit=0
- `Script/TabPathCommon.xjs`: functions=0, globals=1, includes=[], topLevelInit=0
- `Script/TabRuntimeCommon.xjs`: functions=1, globals=1, includes=[], topLevelInit=0
- `Script/TabRuntimeState.xjs`: functions=1, globals=1, includes=[], topLevelInit=0
- `Script/Utility.xjs`: functions=2, globals=1, includes=[], topLevelInit=0

## Screen: `Form/ComponentMethodConversion.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/ControlPropertyMatrix.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/DatasetBinding.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/EventMatrix.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/ExternalXjsBasic.xfdl`

- Referenced XJS: [Script/Common.xjs]
- Imported functions: [gfnHello]
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnHello [Script/Common.xjs]

### Selected symbols

- FUNCTION gfnHello — `Script/Common.xjs:3`

### Unused functions in referenced XJS

- `Script/Common.xjs`: gfnA
- `Script/Common.xjs`: gfnUser
- `Script/Common.xjs`: gfnUnused

## Screen: `Form/ExternalXjsCircularDependency.xfdl`

- Referenced XJS: [Script/CircularA.xjs, Script/CircularB.xjs]
- Imported functions: [gfnCycleA, gfnCycleB]
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnCycleA [Script/CircularA.xjs]
- gfnCycleA -> gfnCycleB [Script/CircularB.xjs]
- gfnCycleB -> gfnCycleA [CYCLE]

### Selected symbols

- FUNCTION gfnCycleA — `Script/CircularA.xjs:2`
- FUNCTION gfnCycleB — `Script/CircularB.xjs:2`

### Unused functions in referenced XJS

- 없음

## Screen: `Form/ExternalXjsDependency.xfdl`

- Referenced XJS: [Script/Common.xjs, Script/Utility.xjs]
- Imported functions: [gfnA, gfnB]
- Imported globals: [gvUserId]
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnA [Script/Common.xjs]
- gfnA -> gfnB [Script/Utility.xjs]
- gfnB -> gvUserId [GLOBAL Script/Utility.xjs]

### Selected symbols

- FUNCTION gfnA — `Script/Common.xjs:7`
- FUNCTION gfnB — `Script/Utility.xjs:2`
- GLOBAL gvUserId — `Script/Utility.xjs:1`

### Unused functions in referenced XJS

- `Script/Common.xjs`: gfnHello
- `Script/Common.xjs`: gfnUser
- `Script/Common.xjs`: gfnUnused
- `Script/Utility.xjs`: gfnValidate

## Screen: `Form/ExternalXjsDirectGlobal.xfdl`

- Referenced XJS: [Script/Utility.xjs]
- Imported functions: []
- Imported globals: [gvUserId]
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN:run -> gvUserId [GLOBAL Script/Utility.xjs]

### Selected symbols

- GLOBAL gvUserId — `Script/Utility.xjs:1`

### Unused functions in referenced XJS

- `Script/Utility.xjs`: gfnB
- `Script/Utility.xjs`: gfnValidate

## Screen: `Form/ExternalXjsDuplicateFunction.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: [FUNCTION gfnDuplicate -> Script/DupA.xjs:1|Script/DupB.xjs:1]
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/ExternalXjsGlobalVariable.xfdl`

- Referenced XJS: [Script/Common.xjs, Script/Utility.xjs]
- Imported functions: [gfnUser]
- Imported globals: [gvUserId]
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnUser [Script/Common.xjs]
- gfnUser -> gvUserId [GLOBAL Script/Utility.xjs]

### Selected symbols

- FUNCTION gfnUser — `Script/Common.xjs:11`
- GLOBAL gvUserId — `Script/Utility.xjs:1`

### Unused functions in referenced XJS

- `Script/Common.xjs`: gfnHello
- `Script/Common.xjs`: gfnA
- `Script/Common.xjs`: gfnUnused
- `Script/Utility.xjs`: gfnB
- `Script/Utility.xjs`: gfnValidate

## Screen: `Form/ExternalXjsSameFileDuplicate.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: [FUNCTION gfnSameFile -> Script/SameFileDuplicate.xjs:1|Script/SameFileDuplicate.xjs:2]
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/ExternalXjsTopLevelInit.xfdl`

- Referenced XJS: [Script/SideEffect.xjs]
- Imported functions: [gfnSideEffect]
- Imported globals: [gvSide]
- Unresolved: []
- Ambiguous: []
- Include warnings: [TOP_LEVEL_XJS_INIT: Script/SideEffect.xjs -> [line 1: trace("side effect at include time");]]

### Dependency chain

- SCREEN -> gfnSideEffect [Script/SideEffect.xjs]
- gfnSideEffect -> gvSide [GLOBAL Script/SideEffect.xjs]

### Selected symbols

- FUNCTION gfnSideEffect — `Script/SideEffect.xjs:3`
- GLOBAL gvSide — `Script/SideEffect.xjs:2`

### Unused functions in referenced XJS

- 없음

## Screen: `Form/GridAdvancedPhase3.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/Main/TabExternalRelativePath.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/NamespacePrefixed.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/NestedContainer.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/ScopeShadowing.xfdl`

- Referenced XJS: [Script/ScopeHelper.xjs]
- Imported functions: [gfnShadow, gfnDatasetShadow]
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnShadow [Script/ScopeHelper.xjs]
- SCREEN -> gfnDatasetShadow [Script/ScopeHelper.xjs]

### Selected symbols

- FUNCTION gfnShadow — `Script/ScopeHelper.xjs:1`
- FUNCTION gfnDatasetShadow — `Script/ScopeHelper.xjs:7`

### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAddPage.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAddPageOverloads.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAddPageWithArguments.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncAddSetUrlCallChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncLazyCallChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncLazyReadComponent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncLazyReadDataset.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncLazySyncReturn.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncLoadedSyncReturn.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncRapidSelection.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncRapidSetUrl.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncRemoveReAdd.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabAsyncStaleLoadCallback.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabChildArguments.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabChildCallsParent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabChildReadsParentComponent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabChildReadsParentDataset.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabCircularScreenDependency.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabContainer.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabContentIdIsolation.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabContentLifecycle.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDuplicateContent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicContent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicIdCollision.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicIndexedSetUrl.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicPathXjs.xfdl`

- Referenced XJS: [Script/TabRuntimeCommon.xjs]
- Imported functions: []
- Imported globals: [gvRuntimePath]
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN:fnOpen -> gvRuntimePath [GLOBAL Script/TabRuntimeCommon.xjs]

### Selected symbols

- GLOBAL gvRuntimePath — `Script/TabRuntimeCommon.xjs:1`

### Unused functions in referenced XJS

- `Script/TabRuntimeCommon.xjs`: gfnRuntimePath

## Screen: `Form/TabDynamicReplaceState.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicRuntimeOnly.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicSameScreenMultiInstance.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicSelection.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicServicePath.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicSetUrlConditional.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicSetUrlStatic.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabDynamicSetUrlVariable.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabEagerLifecycle.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabExternalContent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabExternalNested.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabExternalPreload.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabExternalXjs.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabInlineContent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabInsertPage.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabInsertPageOverloads.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLazyLifecycle.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleDynamicAdd.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleInactiveReplace.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleOnloadFailure.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleRemoveSelected.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleSelectedReplace.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleSetUrlReplace.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleStaticEager.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabLifecycleStaticLazy.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabMissingContent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabMixedContent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabNestedDynamic.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabOnloadFailureHost.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabOpenerBridge.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabOpenerFunction.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabOwnerFrameComponent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabOwnerFrameDataset.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabOwnerFrameFunction.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabOwnerFrameHost.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentAccess.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentAndOpener.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentCallsChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentCallsChildIndexed.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentDepth2.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentDepth3.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentDepthMain.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentDirect.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentReadsChildComponent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentReadsChildDataset.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentShadowHost.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabParentUnresolvedDepth.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabPreloadDynamicUrl.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabRemovePage.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabRemoveReAdd.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabRuntimeEvent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabRuntimeLexicalProtection.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabSameScreenMultiInstance.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateArgumentIsolation.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateGenerationIsolation.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateRemoveCleanup.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateRetention.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateSameScreenMultiInstance.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateSwitchRetention.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateUrlReplacement.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStateXjsGlobalIsolation.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabStaticDynamicMixed.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabUnresolvedChildComponent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TabUnresolvedChildFunction.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/TransactionSample.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `Form/UnsupportedFeatures.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: [gfnUnknown]
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnUnknown [UNRESOLVED]

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ChildBridgeParent.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/CircularTabA.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/CircularTabB.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/Detail.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/FirstTab.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/IdIsolationChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/LifecycleChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/NestedDynamicFirst.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/OnloadFailureChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: [Error]
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> Error [UNRESOLVED]

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/OwnerBridgeChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentAccessChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepth2First.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepth2Second.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepth3First.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepth3Second.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepth3Third.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepthFirst.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepthSecond.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepthThird.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentDepthUnresolvedChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/ParentShadowChild.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/RuntimeBridgeChild.xfdl`

- Referenced XJS: [Script/TabChildCommon.xjs]
- Imported functions: [gfnChildLoaded]
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnChildLoaded [Script/TabChildCommon.xjs]

### Selected symbols

- FUNCTION gfnChildLoaded — `Script/TabChildCommon.xjs:1`

### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/Search.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/SearchXjs.xfdl`

- Referenced XJS: [Script/TabCommon.xjs]
- Imported functions: [gfnTabSearch]
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnTabSearch [Script/TabCommon.xjs]

### Selected symbols

- FUNCTION gfnTabSearch — `Script/TabCommon.xjs:1`

### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/SecondTab.xfdl`

- Referenced XJS: []
- Imported functions: []
- Imported globals: []
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- 없음

### Selected symbols


### Unused functions in referenced XJS

- 없음

## Screen: `TabContent/StateXjsChild.xfdl`

- Referenced XJS: [Script/TabRuntimeState.xjs]
- Imported functions: [gfnNextChildCounter]
- Imported globals: [gvChildCounter]
- Unresolved: []
- Ambiguous: []
- Include warnings: []

### Dependency chain

- SCREEN -> gfnNextChildCounter [Script/TabRuntimeState.xjs]
- gfnNextChildCounter -> gvChildCounter [GLOBAL Script/TabRuntimeState.xjs]

### Selected symbols

- FUNCTION gfnNextChildCounter — `Script/TabRuntimeState.xjs:2`
- GLOBAL gvChildCounter — `Script/TabRuntimeState.xjs:1`

### Unused functions in referenced XJS

- 없음
