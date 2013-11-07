//
//  Rendezvous.h
//  TestGameKit
//
//  Created by Neal Sanche on 11/6/2013.
//  Copyright (c) 2013 Paul Thorsteinson. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <BridgeKit/JavaObject.h>
#import <BridgeKit/AndroidContext.h>

@class Rendezvous;

@protocol RendezvousDelegate <NSObject>

@required
- (void)rendezvous:(Rendezvous *)rendezvous handleClientNominationWithAddress:(NSString *)serverAddress serverPort:(int)serverPort;
- (void)rendezvous:(Rendezvous *)rendezvous handleServerNominationWithAddress:(NSString *)clientAddress;

@end

@interface Rendezvous : JavaObject
{
    
}

@property (nonatomic, retain) id<RendezvousDelegate> delegate;

- (id)initWithContext:(AndroidContext *)context;

- (void)_startRendezvous:(int)serverPort;
- (void)_cancelRendezvous;

- (void)handleClientNominationWithAddress:(NSString *)serverAddress serverPort:(int)serverPort;
- (void)handleServerNominationWithAddress:(NSString *)clientAddress;

@end
