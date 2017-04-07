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

#import "RNKeychainAuthenticationListener.h"

@implementation RNKeychainManager

@synthesize bridge = _bridge;
RCT_EXPORT_MODULE();

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

#pragma mark - Proposed functionality

#define kAuthenticationType "AuthenticationTypeKey"
#define kBiometrics "Biometrics"

#define kCustomPromptMessage "CustomPrompt"

//LAPolicyDeviceOwnerAuthenticationWithBiometrics | LAPolicyDeviceOwnerAuthentication

RCT_EXPORT_METHOD(canCheckAuthentication:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    LAPolicy policyToEvaluate = LAPolicyDeviceOwnerAuthentication;
    
    if (options && options[kAuthenticationType]) {
        if ([ options[kAuthenticationType] isEqualToString:kBiometrics ]) {
            policyToEvaluate = LAPolicyDeviceOwnerAuthenticationWithBiometrics;
        }
    }
    
    NSError *aerr = nil;
    BOOL canBeProtected = [self canCheckAuthentication:policyToEvaluate error:&aerr ];
    
    if (aerr || !canBeProtected) {
        return rejectWithError(reject, aerr);
    } else {
        return resolve(@(YES));
    }
}

- (BOOL) canCheckAuthentication:(LAPolicy)policyToEvaluate error:(NSError **)err {
    return [[[ LAContext alloc] init ] canEvaluatePolicy:policyToEvaluate error:err ];
}

RCT_EXPORT_METHOD(setSecurePassword:(NSString *)password withUsername:(NSString *)username withService:(NSString *)service withOptions:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    // Delete old entry for that key if Available
    NSError *aerr = nil;
    BOOL canAuthenticate = [ self canCheckAuthentication:LAPolicyDeviceOwnerAuthentication error:&aerr ];
    if (aerr || !canAuthenticate) {
        return rejectWithError(reject, aerr);
    }
    
    NSMutableDictionary *dict = @{ kSecClass : (__bridge id)(kSecClassGenericPassword),
                                   kSecAttrService: service,
                                   kSecReturnAttributes: kCFBooleanTrue
                                   }.mutableCopy;
    
    OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
    
    // make new entry
    dict = @{ kSecClass : (__bridge id)(kSecClassGenericPassword),
              kSecAttrAccessible : accessibleValue(options),
              kSecAttrService : service,
              kSecAttrAccount : username
              }.mutableCopy;
    
    CFErrorRef error = NULL;
    SecAccessControlRef sacRef = SecAccessControlCreateWithFlags(kCFAllocatorDefault,
                                                                 kSecAttrAccessibleWhenUnlockedThisDeviceOnly, //kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                                                                 kSecAccessControlTouchIDCurrentSet|kSecAccessControlOr|kSecAccessControlDevicePasscode,
                                                                 &error);
    
    if (error) {
        // ok: failed
        return rejectWithError(reject, aerr);
    }
    
    NSData *passwordData = [password dataUsingEncoding:NSUTF8StringEncoding];
    [ dict setObject:(__bridge id)sacRef forKey:kSecAttrAccessControl ];
    
    [ dict setObject:passwordData forKey:kSecValueData ];
    
    // Try to save to keychain
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        OSStatus osStatus = SecItemAdd((__bridge CFDictionaryRef) dict, NULL);
        
        dispatch_async(dispatch_get_main_queue(), ^{
            if (osStatus != noErr && osStatus != errSecItemNotFound) {
                NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
                return rejectWithError(reject, error);
            } else {
                return resolve(@(YES));
            }
        });
        
    });
}

RCT_EXPORT_METHOD(getSecurePasswordForService:(NSString *)service withOptions:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    NSString *promptMessage = @"Authenticate to retrieve secret!";
    if (options && options[kCustomPromptMessage]) {
        promptMessage = options[kCustomPromptMessage];
    }
    
    NSMutableDictionary *dict = @{ kSecClass : (__bridge id)(kSecClassGenericPassword),
                                   kSecAttrService : service,
                                   kSecReturnAttributes : kCFBooleanTrue,
                                   kSecReturnData : kCFBooleanTrue,
                                   kSecMatchLimit : kSecMatchLimitOne,
                                   kSecUseOperationPrompt : promptMessage
                                   }.mutableCopy;
    
    // Look up password for service in the keychain
    [ self notifyAuthenticationListener: YES ];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSDictionary* found = nil;
        CFTypeRef foundTypeRef = NULL;
        OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) dict, (CFTypeRef*)&foundTypeRef);
        
        dispatch_async(dispatch_get_main_queue(), ^{
            [ self notifyAuthenticationListener: NO ];
            
            if (osStatus != noErr && osStatus != errSecItemNotFound) {
                NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
                return rejectWithError(reject, error);
            }
            
            found = (__bridge NSDictionary*)(foundTypeRef);
            if (!found) {
                return resolve(@(NO));
            }
            
            // Found
            NSString* username = (NSString *) [found objectForKey:(__bridge id)(kSecAttrAccount)];
            NSString* password = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];
            
            return resolve(@{
                             @"service": service,
                             @"username": username,
                             @"password": password
                             });
        })
        
    });
}

- (void) notifyAuthenticationListener:(BOOL)willPresent {
    id<UIApplicationDelegate> appDelegate = [ UIApplication sharedApplication ].delegate;
    
    if ([ appDelegate conformsToProtocol:@protocol(@"RNKeychainAuthenticationListener") ]) {
        ((id<RNKeychainAuthenticationListener>)appDelegate).willPromptForAuthentication = willPresent;
    }
}

#pragma mark - RNKeychain

RCT_EXPORT_METHOD(setGenericPasswordForOptions:(NSDictionary *)options withUsername:(NSString *)username withPassword:(NSString *)password resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *service = serviceValue(options);

  // Create dictionary of search parameters
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword), kSecClass, service, kSecAttrService, kCFBooleanTrue, kSecReturnAttributes, nil];

  if (options && options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Remove any old values from the keychain
  OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);

  // Create dictionary of parameters to add
  NSData *passwordData = [password dataUsingEncoding:NSUTF8StringEncoding];
  dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword), kSecClass, accessibleValue(options), kSecAttrAccessible, service, kSecAttrService, passwordData, kSecValueData, username, kSecAttrAccount, nil];

  if (options && options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Try to save to keychain
  osStatus = SecItemAdd((__bridge CFDictionaryRef) dict, NULL);

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  return resolve(@(YES));

}

RCT_EXPORT_METHOD(getGenericPasswordForOptions:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *service = serviceValue(options);

  // Create dictionary of search parameters
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword), kSecClass, service, kSecAttrService, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];

  if (options && options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Look up server in the keychain
  NSDictionary* found = nil;
  CFTypeRef foundTypeRef = NULL;
  OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) dict, (CFTypeRef*)&foundTypeRef);

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  found = (__bridge NSDictionary*)(foundTypeRef);
  if (!found) {
    return resolve(@(NO));
  }

  // Found
  NSString* username = (NSString *) [found objectForKey:(__bridge id)(kSecAttrAccount)];
  NSString* password = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];

  return resolve(@{
    @"service": service,
    @"username": username,
    @"password": password
  });

}

RCT_EXPORT_METHOD(resetGenericPasswordForOptions:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *service = serviceValue(options);

  // Create dictionary of search parameters
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword), kSecClass, service, kSecAttrService, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];

  if (options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Remove any old values from the keychain
  OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  return resolve(@(YES));

}

RCT_EXPORT_METHOD(setInternetCredentialsForServer:(NSString *)server withUsername:(NSString*)username withPassword:(NSString*)password withOptions:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  // Create dictionary of search parameters
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword),  kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, nil];

  if (options && options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Remove any old values from the keychain
  OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);

  // Create dictionary of parameters to add
  NSData* passwordData = [password dataUsingEncoding:NSUTF8StringEncoding];
  dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, accessibleValue(options), kSecAttrAccessible, server, kSecAttrServer, passwordData, kSecValueData, username, kSecAttrAccount, nil];

  if (options && options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Try to save to keychain
  osStatus = SecItemAdd((__bridge CFDictionaryRef) dict, NULL);

  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  return resolve(@(YES));
}

RCT_EXPORT_METHOD(getInternetCredentialsForServer:(NSString *)server withOptions:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  // Create dictionary of search parameters
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];

  if (options && options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Look up server in the keychain
  NSDictionary *found = nil;
  CFTypeRef foundTypeRef = NULL;
  OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) dict, (CFTypeRef*)&foundTypeRef);

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

  return resolve(@{
    @"server": server,
    @"username": username,
    @"password": password
  });

}

RCT_EXPORT_METHOD(resetInternetCredentialsForServer:(NSString *)server withOptions:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
  // Create dictionary of search parameters
  NSMutableDictionary *dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];

  if (options && options[@"accessGroup"]) {
    [dict setObject:options[@"accessGroup"] forKey:kSecAttrAccessGroup];
  }

  // Remove any old values from the keychain
  OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
  if (osStatus != noErr && osStatus != errSecItemNotFound) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    return rejectWithError(reject, error);
  }

  return resolve(@(YES));

}

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
        @"password": password
      });
    }
    return resolve(@(NO));
  });
}


RCT_EXPORT_METHOD(setSharedWebCredentialsForServer:(NSString *)server withUsername:(NSString *)username withPassword:(NSString *)password resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
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

@end
