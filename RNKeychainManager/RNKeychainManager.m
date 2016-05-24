//
//  RNKeychainManager.m
//  RNKeychainManager
//
//  Created by Joel Arvidsson on 2015-05-20.
//  Copyright (c) 2015 Joel Arvidsson. All rights reserved.
//

#import <Security/Security.h>
#import "RNKeychainManager.h"
#import "RCTConvert.h"
#import "RCTBridge.h"
#import "RCTUtils.h"

static NSString * sBundleIdentifier = nil;

@implementation RNKeychainManager

@synthesize bridge = _bridge;
RCT_EXPORT_MODULE();

+ (void)setBundleIdentifier:(NSString *)bundleIdentifier {
    sBundleIdentifier = bundleIdentifier;
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
            
        default:
            return error.localizedDescription;
    }
}

NSDictionary * makeError(NSError *error)
{
    return RCTMakeAndLogError(messageForError(error), nil, [error dictionaryWithValuesForKeys:@[@"domain", @"code"]]);
}


RCT_EXPORT_METHOD(setGenericPasswordForService:(NSString*)service withUsername:(NSString*)username withPassword:(NSString*)password callback:(RCTResponseSenderBlock)callback){
    if(service == nil) {
        service = [[NSBundle mainBundle] bundleIdentifier];
    }
    
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword),  kSecClass, service, kSecAttrService, kCFBooleanTrue, kSecReturnAttributes, nil];
    
    // Remove any old values from the keychain
    OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
    
    // Create dictionary of parameters to add
    NSData* passwordData = [password dataUsingEncoding:NSUTF8StringEncoding];
    dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword), kSecClass, service, kSecAttrService, passwordData, kSecValueData, username, kSecAttrAccount, nil];
    
    // Try to save to keychain
    osStatus = SecItemAdd((__bridge CFDictionaryRef) dict, NULL);
    
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    callback(@[[NSNull null]]);
    
}

RCT_EXPORT_METHOD(getGenericPasswordForService:(NSString*)service callback:(RCTResponseSenderBlock)callback){
    if(service == nil) {
        service = [[NSBundle mainBundle] bundleIdentifier];
    }
    
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword), kSecClass, service, kSecAttrService, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];
    
    // Look up server in the keychain
    NSDictionary* found = nil;
    CFTypeRef foundTypeRef = NULL;
    OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) dict, (CFTypeRef*)&foundTypeRef);
    
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    found = (__bridge NSDictionary*)(foundTypeRef);
    if (!found) {
        return callback(@[[NSNull null]]);
    }
    
    // Found
    NSString* username = (NSString*) [found objectForKey:(__bridge id)(kSecAttrAccount)];
    NSString* password = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];
    
    callback(@[[NSNull null], username, password]);
    
}

RCT_EXPORT_METHOD(resetGenericPasswordForService:(NSString*)service callback:(RCTResponseSenderBlock)callback){
    if(service == nil) {
        service = [[NSBundle mainBundle] bundleIdentifier];
    }
    
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassGenericPassword), kSecClass, service, kSecAttrService, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];
    
    // Remove any old values from the keychain
    OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    callback(@[[NSNull null]]);
    
}

RCT_EXPORT_METHOD(setInternetCredentialsForServer:(NSString*)server withUsername:(NSString*)username withPassword:(NSString*)password callback:(RCTResponseSenderBlock)callback){
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword),  kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, nil];
    
    // Remove any old values from the keychain
    OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
    
    // Create dictionary of parameters to add
    NSData* passwordData = [password dataUsingEncoding:NSUTF8StringEncoding];
    dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, server, kSecAttrServer, passwordData, kSecValueData, username, kSecAttrAccount, nil];
    
    // Try to save to keychain
    osStatus = SecItemAdd((__bridge CFDictionaryRef) dict, NULL);
    
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    callback(@[[NSNull null]]);
    
}

RCT_EXPORT_METHOD(getInternetCredentialsForServer:(NSString*)server callback:(RCTResponseSenderBlock)callback){
    
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];
    
    // Look up server in the keychain
    NSDictionary* found = nil;
    CFTypeRef foundTypeRef = NULL;
    OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) dict, (CFTypeRef*)&foundTypeRef);
    
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    found = (__bridge NSDictionary*)(foundTypeRef);
    if (!found) {
        return callback(@[[NSNull null]]);
    }
    
    // Found
    NSString* username = (NSString*) [found objectForKey:(__bridge id)(kSecAttrAccount)];
    NSString* password = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];
    
    callback(@[[NSNull null], username, password]);
    
}

RCT_EXPORT_METHOD(resetInternetCredentialsForServer:(NSString*)server callback:(RCTResponseSenderBlock)callback){
    
    // Create dictionary of search parameters
    NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];
    
    // Remove any old values from the keychain
    OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    callback(@[[NSNull null]]);
    
}

- (NSDictionary *)genericPasswordDictWithAccount:(NSString *)account password:(NSString *)password andReturnData:(BOOL)returnData {
    NSString *bundleIdentifier = (sBundleIdentifier ? sBundleIdentifier : [[NSBundle mainBundle] bundleIdentifier]);
    
    if (password) {
        NSData *passwordData = [password dataUsingEncoding:NSUTF8StringEncoding];
        
        return @{
                 (__bridge NSString *)kSecClass: (__bridge id)(kSecClassGenericPassword),
                 (__bridge NSString *)kSecAttrService: bundleIdentifier,
                 (__bridge NSString *)kSecAttrAccount: account,
                 (__bridge NSString *)kSecValueData: passwordData,
                 (__bridge NSString *)kSecReturnAttributes: @(returnData),
                 (__bridge NSString *)kSecReturnData: @(returnData)
                 };
    }
    
    return @{
             (__bridge NSString *)kSecClass: (__bridge id)(kSecClassGenericPassword),
             (__bridge NSString *)kSecAttrService: bundleIdentifier,
             (__bridge NSString *)kSecAttrAccount: account,
             (__bridge NSString *)kSecReturnAttributes: @(returnData),
             (__bridge NSString *)kSecReturnData: @(returnData)
             };
}

RCT_EXPORT_METHOD(setSecureString:(NSString *)value forKey:(NSString *)key callback:(RCTResponseSenderBlock)callback) {
    // Remove any old values from the keychain
    NSDictionary *searchDict = [self genericPasswordDictWithAccount:key password:nil andReturnData:NO];
    OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef)searchDict);
    
    // Try to save to keychain
    NSDictionary *saveDict = [self genericPasswordDictWithAccount:key password:value andReturnData:NO];
    osStatus = SecItemAdd((__bridge CFDictionaryRef) saveDict, NULL);
    
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        if (osStatus == errSecDuplicateItem) {
            osStatus = SecItemUpdate((__bridge CFDictionaryRef) saveDict, NULL);
            
            if (osStatus != noErr && osStatus != errSecItemNotFound) {
                NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
                return callback(@[makeError(error)]);
            }
        }
        else {
            NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
            return callback(@[makeError(error)]);
        }
    }
    
    callback(@[[NSNull null]]);
}

RCT_EXPORT_METHOD(getSecureStringForKey:(NSString *)key callback:(RCTResponseSenderBlock)callback){
    // Look up server in the keychain
    NSDictionary *searchDict = [self genericPasswordDictWithAccount:key password:nil andReturnData:YES];
    
    NSDictionary *found = nil;
    CFTypeRef foundTypeRef = NULL;
    OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef)searchDict, (CFTypeRef*)&foundTypeRef);
    
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    found = (__bridge NSDictionary*)(foundTypeRef);
    if (!found) {
        return callback(@[[NSNull null]]);
    }
    
    // Found
    NSString *value = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];
    
    callback(@[[NSNull null], key, value]);
}

RCT_EXPORT_METHOD(resetSecureStringForKey:(NSString *)key callback:(RCTResponseSenderBlock)callback) {
    // Remove any old values from the keychain
    NSDictionary *searchDict = [self genericPasswordDictWithAccount:key password:nil andReturnData:YES];
    
    OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) searchDict);
    if (osStatus != noErr && osStatus != errSecItemNotFound) {
        NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
        return callback(@[makeError(error)]);
    }
    
    callback(@[[NSNull null]]);
}

@end
