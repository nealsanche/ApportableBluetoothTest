//
//  HelloWorldLayer.h
//  TestGameKit
//
//  Created by Paul Thorsteinson on 2013-10-09.
//  Copyright Paul Thorsteinson 2013. All rights reserved.
//


#import <GameKit/GameKit.h>
#import "GCDAsyncSocket.h"
#import "Rendezvous.h"

// When you import this file, you import all the cocos2d classes
#import "cocos2d.h"


typedef struct
{
	NSInteger number;
} BasicPacket;

// HelloWorldLayer
@interface HelloWorldLayer : CCLayer <GKSessionDelegate, GCDAsyncSocketDelegate, RendezvousDelegate>
{
    Rendezvous *_rendezvous;

    GCDAsyncSocket *_listenSocket;
    GCDAsyncSocket *_socket;
    NSMutableArray *_connectedSockets;
    NSData *_responseTerminatorData;

    long _udpTag;
}

@property (nonatomic, strong) NSMutableArray *connectedClients;
@property (nonatomic, strong) GKSession *session;
@property (nonatomic, strong) NSMutableArray *availableServers;
@property (nonatomic, strong) NSString *serverPeerID;

// returns a CCScene that contains the HelloWorldLayer as the only child
+(CCScene *) scene;

@end
