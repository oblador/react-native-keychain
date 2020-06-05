//
//  RNKeychainManager.m
//  RNKeychainManager
//
//  Created by Joel Arvidsson on 2015-05-20.
//  Copyright (c) 2015 Joel Arvidsson. All rights reserved.
//

#import <Security/Security.h>
#import "RNKeychainManager.h"
#import <React/RCTConvert.h>
#import <React/RCTBridge.h>
#import <React/RCTUtils.h>

#import <LocalAuthentication/LAContext.h>
#import <UIKit/UIKit.h>

@implementation RNKeychainManager

@synthesize bridge = _bridge;
RCT_EXPORT_MODULE();

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

- (dispatch_queue_t)methodQueue
{
  return dispatch_queue_create("com.oblador.KeychainQueue", DISPATCH_QUEUE_SERIAL);
}

// Messages from the comments in <Security/SecBase.h>
NSString *messageForError(NSError *error)
{
  switch (error.code) {
    case errSecUnimplemented:
      return @"Function or operation not implemented.";

    case errSecIO:
      return @"I/O error.";

    case errSecOpWr:
      return @"File already open with with write permission.";

    case errSecParam:
      return @"One or more parameters passed to a function where not valid.";

    case errSecAllocate:
      return @"Failed to allocate memory.";

    case errSecUserCanceled:
      return @"User canceled the operation.";

    case errSecBadReq:
      return @"Bad parameter or invalid state for operation.";

    case errSecNotAvailable:
      return @"No keychain is available. You may need to restart your computer.";

    case errSecDuplicateItem:
      return @"The specified item already exists in the keychain.";

    case errSecItemNotFound:
      return @"The specified item could not be found in the keychain.";

    case errSecInteractionNotAllowed:
      return @"User interaction is not allowed.";

    case errSecDecode:
      return @"Unable to decode the provided data.";

    case errSecAuthFailed:
      return @"The user name or passphrase you entered is not correct.";

    case errSecMissingEntitlement:
      return @"Internal error when a required entitlement isn't present.";

    default:
      return error.localizedDescription;
  }
}

NSString *codeForError(NSError *error)
{
  return [NSString stringWithFormat:@"%li", (long)error.code];
}

void rejectWithError(RCTPromiseRejectBlock reject, NSError *error)
{
  return reject(codeForError(error), messageForError(error), nil);
}

CFStringRef accessibleValue(NSDictionary *options)
{
  if (options && options[@"accessible"] != nil) {
    NSDictionary *keyMap = @{
      @"AccessibleWhenUnlocked": (__bridge NSString *)kSecAttrAccessibleWhenUnlocked,
      @"AccessibleAfterFirstUnlock": (__bridge NSString *)kSecAttrAccessibleAfterFirstUnlock,
      @"AccessibleAlways": (__bridge NSString *)kSecAttrAccessibleAlways,
      @"AccessibleWhenPasscodeSetThisDeviceOnly": (__bridge NSString *)kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
      @"AccessibleWhenUnlockedThisDeviceOnly": (__bridge NSString *)kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
      @"AccessibleAfterFirstUnlockThisDeviceOnly": (__bridge NSString *)kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
      @"AccessibleAlwaysThisDeviceOnly": (__bridge NSString *)kSecAttrAccessibleAlwaysThisDeviceOnly
    };
    NSString *result = keyMap[options[@"accessible"]];
    if (result) {
      return (__bridge CFStringRef)result;
    }
  }
  return kSecAttrAccessibleAfterFirstUnlock;
}

NSString *serviceValue(NSDictionary *options)
{
  if (options && options[@"service"] != nil) {
    return options[@"service"];
  }
  return [[NSBundle mainBundle] bundleIdentifier];
}

NSString *accessGroupValue(NSDictionary *options)
{
  if (options && options[@"accessGroup"] != nil) {
    return options[@"accessGroup"];
  }
  return nil;
}

NSString *authenticationPromptValue(NSDictionary *options)
{
  if (options && options[@"authenticationPrompt"] != nil && options[@"authenticationPrompt"][@"title"]) {
    return options[@"authenticationPrompt"][@"title"];
  }
  return nil;
}

#pragma mark - Proposed functionality - Helpers

#define kAuthenticationType @"authenticationType"
#define kAuthenticationTypeBiometrics @"AuthenticationWithBiometrics"

#define kAccessControlType @"accessControl"
#define kAccessControlUserPresence @"UserPresence"
#define kAccessControlBiometryAny @"BiometryAny"
#define kAccessControlBiometryCurrentSet @"BiometryCurrentSet"
#define kAccessControlDevicePasscode @"DevicePasscode"
#define kAccessControlApplicationPassword @"ApplicationPassword"
#define kAccessControlBiometryAnyOrDevicePasscode @"BiometryAnyOrDevicePasscode"
#define kAccessControlBiometryCurrentSetOrDevicePasscode @"BiometryCurrentSetOrDevicePasscode"

#define kBiometryTypeTouchID @"TouchID"
#define kBiometryTypeFaceID @"FaceID"

LAPolicy authPolicy(NSDictionary *options)
{
  if (options && options[kAuthenticationType]) {
    if ([ options[kAuthenticationType] isEqualToString:kAuthenticationTypeBiometrics ]) {
      return LAPolicyDeviceOwnerAuthenticationWithBiometrics;
    }
  }
  return LAPolicyDeviceOwnerAuthentication;
}

SecAccessControlCreateFlags accessControlValue(NSDictionary *options)
{
  if (options && options[kAccessControlType] && [options[kAccessControlType] isKindOfClass:[NSString class]]) {
    if ([options[kAccessControlType] isEqualToString: kAccessControlUserPresence]) {
      return kSecAccessControlUserPresence;
    }
    else if ([options[kAccessControlType] isEqualToString: kAccessControlBiometryAny]) {
      return kSecAccessControlTouchIDAny;
    }
    else if ([options[kAccessControlType] isEqualToString: kAccessControlBiometryCurrentSet]) {
      return kSecAccessControlTouchIDCurrentSet;
    }
    else if ([options[kAccessControlType] isEqualToString: kAccessControlDevicePasscode]) {
      return kSecAccessControlDevicePasscode;
    }
    else if ([options[kAccessControlType] isEqualToString: kAccessControlBiometryAnyOrDevicePasscode]) {
      return kSecAccessControlTouchIDAny|kSecAccessControlOr|kSecAccessControlDevicePasscode;
    }
    else if ([options[kAccessControlType] isEqualToString: kAccessControlBiometryCurrentSetOrDevicePasscode]) {
      return kSecAccessControlTouchIDCurrentSet|kSecAccessControlOr|kSecAccessControlDevicePasscode;
    }
    else if ([options[kAccessControlType] isEqualToString: kAccessControlApplicationPassword]) {
      return kSecAccessControlApplicationPassword;
    }
  }
  return 0;
}

- (void)insertKeychainEntry:(NSDictionary *)attributes
                withOptions:(NSDictionary * __nullable)options
                   resolver:(RCTPromiseResolveBlock)resolve
                   rejecter:(RCTPromiseRejectBlock)reject
{
  NSString *accessGroup = accessGroupValue(options);
  CFStringRef accessible = accessibleValue(options);
  SecAccessControlCreateFlags accessControl = accessControlValue(options);

  NSMutableDictionary *mAttributes = attributes.mutableCopy;

  if (accessControl) {
    NSError *aerr = nil;
    BOOL canAuthenticate = [[LAContext new] canEvaluatePolicy:LAPolicyDeviceOwnerAuthentication error:&aerr];
    if (aerr || !canAuthenticate) {
      return rejectWithError(reject, aerr);
    }

    CFErrorRef error = NULL;
    SecAccessControlRef sacRef = SecAccessControlCreateWithFlags(kCFAllocatorDefault,
                                                                 accessible,
                                                                 accessControl,
                                                                 &error);

    if (error) {
      return rejectWithError(reject, aerr);
    }
    mAttributes[(__bridge NSString *)kSecAttrAccessControl] = (__bridge id)sacRef;
  } else {
    mAttributes[(__bridge NSString *)kSecAttrAccessible] = (__bridge id)accessible;
  }

  if (accessGroup != nil) {
    mAttributes[(__bridge NSString *)kSecAttrAccessGroup] = accessGroup;
  }

  attributes = [NSDictionary dictionaryWithDictionary:mAttributes];

  OSStatus osStatus = SecItemAdd((__bridge CFDictionaryRef) attributes, NULL);

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  } else {
    NSString *service = serviceValue(options);
    return resolve(@{
      @"service": service,
      @"storage": @"keychain"
    });
  }
}

- (OSStatus)deletePasswordsForService:(NSString *)service
{
  NSDictionary *query = @{
    (__bridge NSString *)kSecClass: (__bridge id)(kSecClassGenericPassword),
    (__bridge NSString *)kSecAttrService: service,
    (__bridge NSString *)kSecReturnAttributes: (__bridge id)kCFBooleanTrue,
    (__bridge NSString *)kSecReturnData: (__bridge id)kCFBooleanFalse
  };

  return SecItemDelete((__bridge CFDictionaryRef) query);
}

- (OSStatus)deleteCredentialsForServer:(NSString *)server
{
  NSDictionary *query = @{
    (__bridge NSString *)kSecClass: (__bridge id)(kSecClassInternetPassword),
    (__bridge NSString *)kSecAttrServer: server,
    (__bridge NSString *)kSecReturnAttributes: (__bridge id)kCFBooleanTrue,
    (__bridge NSString *)kSecReturnData: (__bridge id)kCFBooleanFalse
  };

  return SecItemDelete((__bridge CFDictionaryRef) query);
}

#pragma mark - RNKeychain

#if TARGET_OS_IOS
RCT_EXPORT_METHOD(canCheckAuthentication:(NSDictionary * __nullable)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  LAPolicy policyToEvaluate = authPolicy(options);

  NSError *aerr = nil;
  BOOL canBeProtected = [[LAContext new] canEvaluatePolicy:policyToEvaluate error:&aerr];

  if (aerr || !canBeProtected) {
    return resolve(@(NO));
  } else {
    return resolve(@(YES));
  }
}
#endif

#if TARGET_OS_IOS
RCT_EXPORT_METHOD(getSupportedBiometryType:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSError *aerr = nil;
  LAContext *context = [LAContext new];
  BOOL canBeProtected = [context canEvaluatePolicy:LAPolicyDeviceOwnerAuthentication error:&aerr];

  if (!aerr && canBeProtected) {
    if (@available(iOS 11, *)) {
      if (context.biometryType == LABiometryTypeFaceID) {
        return resolve(kBiometryTypeFaceID);
      }
    }
    return resolve(kBiometryTypeTouchID);
  }

  return resolve([NSNull null]);
}
#endif

RCT_EXPORT_METHOD(setGenericPasswordForOptions:(NSDictionary *)options
                  withUsername:(NSString *)username
                  withPassword:(NSString *)password
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *service = serviceValue(options);
  NSDictionary *attributes = attributes = @{
    (__bridge NSString *)kSecClass: (__bridge id)(kSecClassGenericPassword),
    (__bridge NSString *)kSecAttrService: service,
    (__bridge NSString *)kSecAttrAccount: username,
    (__bridge NSString *)kSecValueData: [password dataUsingEncoding:NSUTF8StringEncoding]
  };

  [self deletePasswordsForService:service];

  [self insertKeychainEntry:attributes withOptions:options resolver:resolve rejecter:reject];
}

RCT_EXPORT_METHOD(getGenericPasswordForOptions:(NSDictionary * __nullable)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *service = serviceValue(options);
  NSString *authenticationPrompt = authenticationPromptValue(options);

  NSDictionary *query = @{
    (__bridge NSString *)kSecClass: (__bridge id)(kSecClassGenericPassword),
    (__bridge NSString *)kSecAttrService: service,
    (__bridge NSString *)kSecReturnAttributes: (__bridge id)kCFBooleanTrue,
    (__bridge NSString *)kSecReturnData: (__bridge id)kCFBooleanTrue,
    (__bridge NSString *)kSecMatchLimit: (__bridge NSString *)kSecMatchLimitOne,
    (__bridge NSString *)kSecUseOperationPrompt: authenticationPrompt
  };

  // Look up service in the keychain
  NSDictionary *found = nil;
  CFTypeRef foundTypeRef = NULL;
  OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) query, (CFTypeRef*)&foundTypeRef);

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  found = (__bridge NSDictionary*)(foundTypeRef);
  if (!found) {
    return resolve(@(NO));
  }

  // Found
  NSString *username = (NSString *) [found objectForKey:(__bridge id)(kSecAttrAccount)];
  NSString *password = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];

  CFRelease(foundTypeRef);
  return resolve(@{
    @"service": service,
    @"username": username,
    @"password": password,
    @"storage": @"keychain"
  });
}

RCT_EXPORT_METHOD(resetGenericPasswordForOptions:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *service = serviceValue(options);

  OSStatus osStatus = [self deletePasswordsForService:service];

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  return resolve(@(YES));
}

RCT_EXPORT_METHOD(setInternetCredentialsForServer:(NSString *)server
                  withUsername:(NSString*)username
                  withPassword:(NSString*)password
                  withOptions:(NSDictionary * __nullable)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self deleteCredentialsForServer:server];

  NSDictionary *attributes = @{
    (__bridge NSString *)kSecClass: (__bridge id)(kSecClassInternetPassword),
    (__bridge NSString *)kSecAttrServer: server,
    (__bridge NSString *)kSecAttrAccount: username,
    (__bridge NSString *)kSecValueData: [password dataUsingEncoding:NSUTF8StringEncoding]
  };

  [self insertKeychainEntry:attributes withOptions:options resolver:resolve rejecter:reject];
}

RCT_EXPORT_METHOD(hasInternetCredentialsForServer:(NSString *)server
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSMutableDictionary *queryParts = [[NSMutableDictionary alloc] init];
  queryParts[(__bridge NSString *)kSecClass] = (__bridge id)(kSecClassInternetPassword);
  queryParts[(__bridge NSString *)kSecAttrServer] = server;
  queryParts[(__bridge NSString *)kSecMatchLimit] = (__bridge NSString *)kSecMatchLimitOne;

  if (@available(iOS 9, *)) {
    queryParts[(__bridge NSString *)kSecUseAuthenticationUI] = (__bridge NSString *)kSecUseAuthenticationUIFail;
  }

  NSDictionary *query = [queryParts copy];

  // Look up server in the keychain
  OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) query, nil);

  switch (osStatus) {
    case noErr:
    case errSecInteractionNotAllowed:
      return resolve(@(YES));

    case errSecItemNotFound:
      return resolve(@(NO));
  }

  NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
  return rejectWithError(reject, error);
}

RCT_EXPORT_METHOD(getInternetCredentialsForServer:(NSString *)server
                  withOptions:(NSDictionary * __nullable)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSDictionary *query = @{
    (__bridge NSString *)kSecClass: (__bridge id)(kSecClassInternetPassword),
    (__bridge NSString *)kSecAttrServer: server,
    (__bridge NSString *)kSecReturnAttributes: (__bridge id)kCFBooleanTrue,
    (__bridge NSString *)kSecReturnData: (__bridge id)kCFBooleanTrue,
    (__bridge NSString *)kSecMatchLimit: (__bridge NSString *)kSecMatchLimitOne
  };

  // Look up server in the keychain
  NSDictionary *found = nil;
  CFTypeRef foundTypeRef = NULL;
  OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) query, (CFTypeRef*)&foundTypeRef);

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  found = (__bridge NSDictionary*)(foundTypeRef);
  if (!found) {
    return resolve(@(NO));
  }

  // Found
  NSString *username = (NSString *) [found objectForKey:(__bridge id)(kSecAttrAccount)];
  NSString *password = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];

  CFRelease(foundTypeRef);
  return resolve(@{
    @"server": server,
    @"username": username,
    @"password": password,
    @"storage": @"keychain"
  });

}

RCT_EXPORT_METHOD(resetInternetCredentialsForServer:(NSString *)server
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  OSStatus osStatus = [self deleteCredentialsForServer:server];

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  return resolve(@(YES));
}

#if TARGET_OS_IOS && !TARGET_OS_UIKITFORMAC
RCT_EXPORT_METHOD(requestSharedWebCredentials:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  SecRequestSharedWebCredential(NULL, NULL, ^(CFArrayRef credentials, CFErrorRef error) {
    if (error != NULL) {
      NSError *nsError = (__bridge NSError *)error;
      return reject([NSString stringWithFormat:@"%li", (long)nsError.code], nsError.description, nil);
    }

    if (CFArrayGetCount(credentials) > 0) {
      CFDictionaryRef credentialDict = CFArrayGetValueAtIndex(credentials, 0);
      NSString *server = (__bridge NSString *)CFDictionaryGetValue(credentialDict, kSecAttrServer);
      NSString *username = (__bridge NSString *)CFDictionaryGetValue(credentialDict, kSecAttrAccount);
      NSString *password = (__bridge NSString *)CFDictionaryGetValue(credentialDict, kSecSharedPassword);

      return resolve(@{
        @"server": server,
        @"username": username,
        @"password": password,
        @"storage": @"keychain"
      });
    }
    return resolve(@(NO));
  });
}


RCT_EXPORT_METHOD(setSharedWebCredentialsForServer:(NSString *)server
                  withUsername:(NSString *)username
                  withPassword:(NSString *)password
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  SecAddSharedWebCredential(
    (__bridge CFStringRef)server,
    (__bridge CFStringRef)username,
    (__bridge CFStringRef)password,
    ^(CFErrorRef error)
  {

    if (error != NULL) {
      NSError *nsError = (__bridge NSError *)error;
      return reject([NSString stringWithFormat:@"%li", (long)nsError.code], nsError.description, nil);
    }

    resolve(@(YES));
  });
}
#endif

@end
