# Enumeration: SECURITY\_RULES

Enum representing security rules for storage.

## Platform

Android

## Enumeration Members

### AUTOMATIC\_UPGRADE

> **AUTOMATIC\_UPGRADE**: `"automaticUpgradeToMoreSecuredStorage"`

Upgrade secret to the best available storage as soon as it is available and user request secret extraction. Upgrade not applied till we request the secret. This rule only applies to secrets stored with FacebookConseal.

#### Defined in

[enums.ts:158](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L158)

***

### NONE

> **NONE**: `"none"`

No special security rules applied.

#### Defined in

[enums.ts:156](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L156)
