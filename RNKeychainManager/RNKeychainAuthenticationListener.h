//
//  TouchIdPromptListener.h
//  RNKeychain
//
//  Created by Steffen Blümm on 05/04/17.
//  Copyright © 2017 Joel Arvidsson. All rights reserved.
//

#import <Foundation/Foundation.h>

/**
 This is a protocol to be implemented by the AppDelegate in case
 the AppDelegate takes precautions to obfuscate the screen when
 the app resigns active state.
 Thus the AppDelegate can avoid to obfuscating the screen when
 the TouchId-prompt is brought up by the OS
 */
@protocol RNKeychainAuthenticationListener <NSObject>

@property (nonatomic, assign) BOOL willPromptForAuthentication;

@end
