//
//  Rendezvous.m
//  TestGameKit
//
//  Created by Neal Sanche on 11/6/2013.
//  Copyright (c) 2013 Paul Thorsteinson. All rights reserved.
//
#import "Rendezvous.h"
#import <BridgeKit/AndroidContext.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wobjc-property-implementation"
#pragma clang diagnostic ignored "-Wincomplete-implementation"

@implementation Rendezvous

@synthesize delegate;

+ (void)initializeJava
{
    // Note: this must be called for any class that registers custom
    // java apis, without this call the inheritance may not work as expected

    [super initializeJava];

    // Bridge registration methods must be called on the class and NOT self
    // even though that this is a static method (this preserves inheritance
    // to the correct java class

    [Rendezvous registerConstructorWithSelector:@selector(initWithContext:)
                                      arguments:[AndroidContext className], nil];

    [Rendezvous registerInstanceMethod:@"startRendezvous"
                       selector:@selector(_startRendezvous:)
                    returnValue:nil
                      arguments:[JavaClass intPrimitive], nil];

    [Rendezvous registerInstanceMethod:@"cancelRendezvous"
                              selector:@selector(_cancelRendezvous)
                           returnValue:nil
                             arguments:nil];

    [Rendezvous registerCallback:@"handleClientNomination"
                                     selector:@selector(handleClientNominationWithAddress:serverPort:)
                                     returnValue:nil
                                       arguments:[NSString className], [JavaClass intPrimitive], nil];

    [Rendezvous registerCallback:@"handleServerNomination"
                        selector:@selector(handleServerNominationWithAddress:)
                     returnValue:nil
                       arguments:[NSString className], nil];

}

- (void)dealloc
{
    [self.delegate release];
    self.delegate = nil;

    [super dealloc];
}

+ (NSString *)className
{
    return @"com.robotsandpencils.rendezvous.Rendezvous";
}

- (void)handleClientNominationWithAddress:(NSString *)serverAddress serverPort:(int)serverPort
{
    NSLog(@"Client Nomination: %@ %d", serverAddress, serverPort);
    [delegate rendezvous:self handleClientNominationWithAddress:serverAddress serverPort:serverPort];
}

- (void)handleServerNominationWithAddress:(NSString *)clientAddress
{
    NSLog(@"Server Nomination: %@", clientAddress);
    [delegate rendezvous:self handleServerNominationWithAddress:clientAddress];
}

#pragma clang diagnostic pop

@end