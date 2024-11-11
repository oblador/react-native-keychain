# Enumeration: ACCESS\_CONTROL

Enum representing access control options.

## Enumeration Members

### APPLICATION\_PASSWORD

> **APPLICATION\_PASSWORD**: `"ApplicationPassword"`

Constraint to use an application-provided password for data encryption key generation.

#### Defined in

[enums.ts:36](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L36)

***

### BIOMETRY\_ANY

> **BIOMETRY\_ANY**: `"BiometryAny"`

Constraint to access an item with Touch ID for any enrolled fingers.

#### Defined in

[enums.ts:30](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L30)

***

### BIOMETRY\_ANY\_OR\_DEVICE\_PASSCODE

> **BIOMETRY\_ANY\_OR\_DEVICE\_PASSCODE**: `"BiometryAnyOrDevicePasscode"`

Constraint to access an item with Touch ID for any enrolled fingers or passcode.

#### Defined in

[enums.ts:38](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L38)

***

### BIOMETRY\_CURRENT\_SET

> **BIOMETRY\_CURRENT\_SET**: `"BiometryCurrentSet"`

Constraint to access an item with Touch ID for currently enrolled fingers.

#### Defined in

[enums.ts:32](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L32)

***

### BIOMETRY\_CURRENT\_SET\_OR\_DEVICE\_PASSCODE

> **BIOMETRY\_CURRENT\_SET\_OR\_DEVICE\_PASSCODE**: `"BiometryCurrentSetOrDevicePasscode"`

Constraint to access an item with Touch ID for currently enrolled fingers or passcode.

#### Defined in

[enums.ts:40](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L40)

***

### DEVICE\_PASSCODE

> **DEVICE\_PASSCODE**: `"DevicePasscode"`

Constraint to access an item with the device passcode.

#### Defined in

[enums.ts:34](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L34)

***

### USER\_PRESENCE

> **USER\_PRESENCE**: `"UserPresence"`

Constraint to access an item with either Touch ID or passcode.

#### Defined in

[enums.ts:28](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/enums.ts#L28)
