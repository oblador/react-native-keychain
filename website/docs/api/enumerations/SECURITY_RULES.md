# Enumeration: SECURITY\_RULES

Enum representing security rules for storage. (Android only)

## Enumeration Members

### AUTOMATIC\_UPGRADE

> **AUTOMATIC\_UPGRADE**: `"automaticUpgradeToMoreSecuredStorage"`

Upgrade secret to the best available storage as soon as it is available and user request secret extraction. Upgrade not applied till we request the secret. This rule only applies to secrets stored with FacebookConseal.

#### Defined in

[index.ts:106](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L106)

***

### NONE

> **NONE**: `"none"`

No special security rules applied.

#### Defined in

[index.ts:104](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L104)
