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


@end
