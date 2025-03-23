# Enumeration: SECURITY\_RULES

Enum representing security rules for storage.

## Platform

Android

## Enumeration Members

### AUTOMATIC\_UPGRADE

> **AUTOMATIC\_UPGRADE**: `"automaticUpgradeToMoreSecuredStorage"`

Upgrade secret to the best available storage as soon as it is available and user request secret extraction. Upgrade not applied till we request the secret. This rule only applies to secrets stored with FacebookConseal.

#### Defined in

[enums.ts:123](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L123)

***

### NONE

> **NONE**: `"none"`

No special security rules applied.

#### Defined in

[enums.ts:121](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L121)
