/**
 * Reference: <https://developer.apple.com/library/ios/documentation/Security/Reference/keychainservices/index.html#//apple_ref/doc/constant_group/Keychain_Item_Accessibility_Constants>
 * @providesModule KeychainAccessibilityEnum
 */
'use strict';

var KeychainAccessibilityEnum = module.exports = {
  whenUnlocked: true,
  afterFirstUnlock: true,
  always: true,
  whenPasscodeSetThisDeviceOnly: true,
  whenUnlockedThisDeviceOnly: true,
  afterFirstUnlockThisDeviceOnly: true,
  alwaysThisDeviceOnly: true,
};
