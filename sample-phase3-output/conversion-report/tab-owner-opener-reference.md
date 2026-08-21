# Phase 3 Parent / Owner Frame / Opener Reference

## `Form/TabOpenerBridge.xfdl:2`

- Function: `fnOpener`
- Kind: `POPUP_OPENER`
- Depth: `0`
- Target Screen: ``
- Target Symbol: `fnCallback`
- Symbol Type: `UNKNOWN`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Expression: `function fnOpener(){ return opener.fnCallback("A"); }`
- Message: Popup opener는 Tab parent와 별도 scope; 명시적 opener scope/window가 있을 때 runtime bridge 사용

## `Form/TabOpenerFunction.xfdl:2`

- Function: `fnCall`
- Kind: `POPUP_OPENER`
- Depth: `0`
- Target Screen: ``
- Target Symbol: `fnCallback`
- Symbol Type: `UNKNOWN`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Expression: `function fnCall(){ return opener.fnCallback("A"); }`
- Message: Popup opener는 Tab parent와 별도 scope; 명시적 opener scope/window가 있을 때 runtime bridge 사용

## `Form/TabParentAndOpener.xfdl:2`

- Function: `fnBoth`
- Kind: `POPUP_OPENER`
- Depth: `0`
- Target Screen: ``
- Target Symbol: `fnCallback`
- Symbol Type: `UNKNOWN`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Expression: `function fnBoth(){ var a=parent.parent.fnParent(); var b=opener.fnCallback(); return a+b; }`
- Message: Popup opener는 Tab parent와 별도 scope; 명시적 opener scope/window가 있을 때 runtime bridge 사용

## `TabContent/OwnerBridgeChild.xfdl:3`

- Function: `fnOwnerCalls`
- Kind: `OWNER_FRAME`
- Depth: `0`
- Target Screen: `TabContent/OwnerBridgeChild.xfdl`
- Target Symbol: `fnLocalOwner`
- Symbol Type: `FUNCTION`
- Status: `RESOLVED`
- Expression: `function fnOwnerCalls(){ var a=this.getOwnerFrame().form.fnLocalOwner("A"); var b=this.getOwnerFrame().form.edtOwner.value; var c=this.getOwnerFrame().form.dsOwner.getRowCount(); var args=this.getOwnerFrame().arguments; return a+b+c; }`
- Message: getOwnerFrame().form은 현재 Form의 owner WFrame scope로 변환

## `TabContent/OwnerBridgeChild.xfdl:3`

- Function: `fnOwnerCalls`
- Kind: `OWNER_FRAME`
- Depth: `0`
- Target Screen: `TabContent/OwnerBridgeChild.xfdl`
- Target Symbol: `edtOwner`
- Symbol Type: `COMPONENT`
- Status: `RESOLVED`
- Expression: `function fnOwnerCalls(){ var a=this.getOwnerFrame().form.fnLocalOwner("A"); var b=this.getOwnerFrame().form.edtOwner.value; var c=this.getOwnerFrame().form.dsOwner.getRowCount(); var args=this.getOwnerFrame().arguments; return a+b+c; }`
- Message: getOwnerFrame().form은 현재 Form의 owner WFrame scope로 변환

## `TabContent/OwnerBridgeChild.xfdl:3`

- Function: `fnOwnerCalls`
- Kind: `OWNER_FRAME`
- Depth: `0`
- Target Screen: `TabContent/OwnerBridgeChild.xfdl`
- Target Symbol: `dsOwner`
- Symbol Type: `DATASET`
- Status: `RESOLVED`
- Expression: `function fnOwnerCalls(){ var a=this.getOwnerFrame().form.fnLocalOwner("A"); var b=this.getOwnerFrame().form.edtOwner.value; var c=this.getOwnerFrame().form.dsOwner.getRowCount(); var args=this.getOwnerFrame().arguments; return a+b+c; }`
- Message: getOwnerFrame().form은 현재 Form의 owner WFrame scope로 변환

## `TabContent/OwnerBridgeChild.xfdl:4`

- Function: `fnOwnerParent`
- Kind: `OWNER_FRAME`
- Depth: `1`
- Target Screen: ``
- Target Symbol: `fnHost`
- Symbol Type: `UNKNOWN`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Expression: `function fnOwnerParent(){ return this.getOwnerFrame().getOwnerFrame().form.fnHost(); }`
- Message: 다중 getOwnerFrame() chain은 Frame tree 문맥이 필요하여 runtime owner resolver 사용

## `TabContent/OwnerBridgeChild.xfdl:3`

- Function: `fnOwnerCalls`
- Kind: `OWNER_FRAME`
- Depth: `0`
- Target Screen: `TabContent/OwnerBridgeChild.xfdl`
- Target Symbol: `arguments`
- Symbol Type: `UNKNOWN`
- Status: `RUNTIME_VERIFY_REQUIRED`
- Expression: `function fnOwnerCalls(){ var a=this.getOwnerFrame().form.fnLocalOwner("A"); var b=this.getOwnerFrame().form.edtOwner.value; var c=this.getOwnerFrame().form.dsOwner.getRowCount(); var args=this.getOwnerFrame().arguments; return a+b+c; }`
- Message: Owner Frame arguments는 WFrame parameter/dataObject bridge로 변환하며 실제 argument reference semantics는 runtime 확인 필요

