# Enumeration: ACCESSIBLE

Enum representing when a keychain item is accessible.

## Enumeration Members

### AFTER\_FIRST\_UNLOCK

> **AFTER\_FIRST\_UNLOCK**: `"AccessibleAfterFirstUnlock"`

The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user.

#### Defined in

[enums.ts:12](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L12)

***

### AFTER\_FIRST\_UNLOCK\_THIS\_DEVICE\_ONLY

> **AFTER\_FIRST\_UNLOCK\_THIS\_DEVICE\_ONLY**: `"AccessibleAfterFirstUnlockThisDeviceOnly"`

The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user. Items with this attribute never migrate to a new device.

#### Defined in

[enums.ts:20](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L20)

***

### ALWAYS

> **ALWAYS**: `"AccessibleAlways"`

The data in the keychain item can always be accessed regardless of whether the device is locked.

#### Defined in

[enums.ts:14](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L14)

***

### WHEN\_PASSCODE\_SET\_THIS\_DEVICE\_ONLY

> **WHEN\_PASSCODE\_SET\_THIS\_DEVICE\_ONLY**: `"AccessibleWhenPasscodeSetThisDeviceOnly"`

The data in the keychain can only be accessed when the device is unlocked. Only available if a passcode is set on the device. Items with this attribute never migrate to a new device.

#### Defined in

[enums.ts:16](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L16)

***

### WHEN\_UNLOCKED

> **WHEN\_UNLOCKED**: `"AccessibleWhenUnlocked"`

The data in the keychain item can be accessed only while the device is unlocked by the user.

#### Defined in

[enums.ts:10](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L10)

***

### WHEN\_UNLOCKED\_THIS\_DEVICE\_ONLY

> **WHEN\_UNLOCKED\_THIS\_DEVICE\_ONLY**: `"AccessibleWhenUnlockedThisDeviceOnly"`

The data in the keychain item can be accessed only while the device is unlocked by the user. Items with this attribute do not migrate to a new device.

#### Defined in

[enums.ts:18](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L18)
