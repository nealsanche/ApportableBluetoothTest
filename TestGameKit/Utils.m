//
//  Utils.m
//  TestGameKit
//
//  Created by Neal Sanche on 11/5/2013.
//  Copyright (c) 2013 Paul Thorsteinson. All rights reserved.
//

#ifdef ANDROID
#import "Utils.h"

@implementation Utils

+ (void)initializeJava
{
    // Note: this must be called for any class that registers custom
    // java apis, without this call the inheritance may not work as expected

    [super initializeJava];

    // Bridge registration methods must be called on the class and NOT self
    // even though that this is a static method (this preserves inheritance
    // to the correct java class

    [Utils registerStaticMethod:@"getIPAddress"
                              selector:@selector(getIPAddress:)
                           returnValue:[NSString className]
                             arguments:[JavaClass boolPrimitive], nil];
}

+ (NSString *)className 
{ 
    return @"com.apportable.bluetooth.Utils"; 
}

@end

#endif
