//
//  RNKeychainManager.h
//  RNKeychainManager
//
//  Created by Joel Arvidsson on 2015-05-20.
//  Copyright (c) 2015 Joel Arvidsson. All rights reserved.
//

#import <React/RCTLog.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import "RNKeychainSpec.h"

@interface RNKeychainManager : NSObject <NativeKeychainManagerSpec>
#else
#import <React/RCTBridgeModule.h>

@interface RNKeychainManager : NSObject <RCTBridgeModule>
#endif

@end