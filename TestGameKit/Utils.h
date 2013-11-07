//
//  Utils.h
//  TestGameKit
//
//  Created by Neal Sanche on 11/5/2013.
//  Copyright (c) 2013 Paul Thorsteinson. All rights reserved.
//
#ifdef ANDROID
#import <Foundation/Foundation.h>
#import <BridgeKit/JavaObject.h>

@interface Utils : JavaObject

+ (NSString *)getIPAddress:(BOOL)useIPV4;

@end
#endif
