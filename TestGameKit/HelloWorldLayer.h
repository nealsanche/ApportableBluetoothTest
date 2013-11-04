//
//  HelloWorldLayer.h
//  TestGameKit
//
//  Created by Paul Thorsteinson on 2013-10-09.
//  Copyright Paul Thorsteinson 2013. All rights reserved.
//


#import <GameKit/GameKit.h>
#import "BluetoothSocket.h"
#import "BluetoothConnectionManager.h"

// When you import this file, you import all the cocos2d classes
#import "cocos2d.h"


typedef struct
{
	
	NSInteger number;
} BasicPacket;

// HelloWorldLayer
@interface HelloWorldLayer : CCLayer <GKSessionDelegate>
{
    BluetoothConnectionManager *_bcm;
}

@property (nonatomic, strong) NSMutableArray *connectedClients;
@property (nonatomic, strong) GKSession *session;


// returns a CCScene that contains the HelloWorldLayer as the only child
+(CCScene *) scene;


@property (nonatomic, strong) NSMutableArray *availableServers;
@property (nonatomic, strong) NSString *serverPeerID;

- (void)didConnectToServer:(BluetoothSocket *)server;
- (void)connectionReceived:(BluetoothSocket *)clientDevice;


@end
