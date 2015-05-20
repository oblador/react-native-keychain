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

@implementation RNKeychainManager

@synthesize bridge = _bridge;
RCT_EXPORT_MODULE();


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
  
  if (osStatus) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
      callback(@[error]);
  }
  
  callback(@[[NSNull null]]);

}

RCT_EXPORT_METHOD(getInternetCredentialsForServer:(NSString*)server callback:(RCTResponseSenderBlock)callback){
  
  // Create dictionary of search parameters
  NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];
  
  // Look up server in the keychain
  NSDictionary* found = nil;
  CFDictionaryRef foundCF;
  OSStatus osStatus = SecItemCopyMatching((__bridge CFDictionaryRef) dict, (CFTypeRef*)&foundCF);
  
  found = (__bridge NSDictionary*)(foundCF);
  if (!found) return callback(@[[NSNull null]]);
  
  // Found
  NSString* username = (NSString*) [found objectForKey:(__bridge id)(kSecAttrAccount)];
  NSString* password = [[NSString alloc] initWithData:[found objectForKey:(__bridge id)(kSecValueData)] encoding:NSUTF8StringEncoding];

  if (osStatus) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
    callback(@[error]);
  }
  
  callback(@[[NSNull null], username, password]);
  
}

RCT_EXPORT_METHOD(resetInternetCredentialsForServer:(NSString*)server callback:(RCTResponseSenderBlock)callback){

  // Create dictionary of search parameters
  NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:(__bridge id)(kSecClassInternetPassword), kSecClass, server, kSecAttrServer, kCFBooleanTrue, kSecReturnAttributes, kCFBooleanTrue, kSecReturnData, nil];

  // Remove any old values from the keychain
  OSStatus osStatus = SecItemDelete((__bridge CFDictionaryRef) dict);
  if (osStatus) {
    NSError *error = [NSError errorWithDomain:NSOSStatusErrorDomain code:osStatus userInfo:nil];
      callback(@[error]);
  }
  
  callback(@[[NSNull null]]);

}

@end
