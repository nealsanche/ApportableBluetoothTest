//
//  HelloWorldLayer.m
//  TestGameKit
//
//  Created by Paul Thorsteinson on 2013-10-09.
//  Copyright Paul Thorsteinson 2013. All rights reserved.
//


// Import the interfaces
#import "HelloWorldLayer.h"

// Needed to obtain the Navigation Controller
#import "AppDelegate.h"

#pragma mark - HelloWorldLayer

// HelloWorldLayer implementation
@implementation HelloWorldLayer

// Helper class method that creates a Scene with the HelloWorldLayer as the only child.
+(CCScene *) scene
{
	// 'scene' is an autorelease object.
	CCScene *scene = [CCScene node];
	
	// 'layer' is an autorelease object.
	HelloWorldLayer *layer = [HelloWorldLayer node];
	
	// add layer as a child to scene
	[scene addChild: layer];
	
	// return the scene
	return scene;
}

#define kSessionID @"DoesThiSWorl"

// on "init" you need to initialize your instance
-(id) init
{
	// always call "super" init
	// Apple recommends to re-assign "self" with the "super's" return value
	if( (self=[super init]) ) {
        
        self.connectedClients = [NSMutableArray array];
        self.availableServers = [NSMutableArray array];
        
		
		// create and initialize a Label
		CCLabelTTF *label = [CCLabelTTF labelWithString:@"Net test" fontName:@"Marker Felt" fontSize:64];

		// ask director for the window size
		CGSize size = [[CCDirector sharedDirector] winSize];
	
		// position the label on the center of the screen
		label.position =  ccp( size.width /2 , size.height/2 );
		
		// add the label as a child to this Layer
		[self addChild: label];
		
		
		
		//
		// Leaderboards and Achievements
		//
		
		// Default font size will be 28 points.
		[CCMenuItemFont setFontSize:28];
		

		
		// Achievement Menu Item using blocks
		CCMenuItem *itemAchievement = [CCMenuItemFont itemWithString:@"Server" block:^(id sender) {
			
			[self server];
			
		}];
		
		// Leaderboard Menu Item using blocks
		CCMenuItem *itemLeaderboard = [CCMenuItemFont itemWithString:@"Client" block:^(id sender) {
			
			[self client:sender];
			
		}];

		
		CCMenu *menu = [CCMenu menuWithItems:itemAchievement, itemLeaderboard, nil];
		
		[menu alignItemsHorizontallyWithPadding:20];
		[menu setPosition:ccp( size.width/2, size.height/2 - 50)];
		
		// Add the menu to the layer
		[self addChild:menu];

	}
	return self;
}

- (void)server{
    
    _session = [[GKSession alloc] initWithSessionID:kSessionID displayName:nil sessionMode:GKSessionModeServer];
    _session.delegate = self;
    _session.available = YES;
    [_session setDataReceiveHandler:self withContext:nil];
}

- (void)client:(CCMenuItemFont *)menuItem{
    
    if (!_session) {
        _session = [[GKSession alloc] initWithSessionID:kSessionID displayName:nil sessionMode:GKSessionModeClient];
        _session.delegate = self;
        _session.available = YES;
        [_session setDataReceiveHandler:self withContext:nil];
        [menuItem setString:@"Join"];
    }else if(_session && [_availableServers count] > 0){
        [_session connectToPeer:_availableServers[0] withTimeout:5.0f];
    }   

}

// on "dealloc" you need to release all your retained objects
- (void) dealloc
{
	// in case you have something to dealloc, do it in this method
	// in this particular example nothing needs to be released.
	// cocos2d will automatically release all the children (Label)
	
	// don't forget to call "super dealloc"
	[super dealloc];
}

- (void)session:(GKSession *)session peer:(NSString *)peerID didChangeState:(GKPeerConnectionState)state
{

	NSLog(@"MatchmakingServer: peer %@ changed state %d", peerID, state);

    
	switch (state)
	{
		case GKPeerStateAvailable:
            if (![_availableServers containsObject:peerID]) {
                [_availableServers addObject:peerID];
            }
            
			break;
            
		case GKPeerStateUnavailable:
            if (![_availableServers containsObject:peerID]) {
                [_availableServers removeObject:peerID];
            }
            
			break;
            
            // A new client has connected to the server.
		case GKPeerStateConnected:
            
            if (![_connectedClients containsObject:peerID]) {
               [_connectedClients addObject:peerID];
                self.serverPeerID = peerID;
                [self sendRandomPacket];
            }
			
            
			break;
            
            // A client has disconnected from the server.
		case GKPeerStateDisconnected:
            if (![_connectedClients containsObject:peerID]) {
                [_connectedClients removeObject:peerID];
            }
			break;
            
		case GKPeerStateConnecting:
            NSLog(@"Connecting");
			break;
	}
}

- (void)sendRandomPacket{
    BasicPacket basicPacket;
    basicPacket.number = arc4random_uniform(99);
    
    NSError* error = nil;
	NSData* packet = [NSData dataWithBytes:&basicPacket length:sizeof(packet)];
    
    CCLOG(@"SEND number %d", basicPacket.number);

    
    
    BOOL result = [_session sendData:packet toPeers:@[self.serverPeerID] withDataMode:GKSendDataReliable error:&error];
    
    if (error) {
        CCLOG(@"NETWORK ERROR:  %@", [error localizedDescription]);
    }else if (!error && !result) {
        CCLOG(@"NETWORK ERROR:  No error but did not queue it up");
    }
}

- (void)session:(GKSession *)session didReceiveConnectionRequestFromPeer:(NSString *)peerID
{

	NSLog(@"MatchmakingServer: connection request from peer %@", peerID);
		NSError *error;
		if ([session acceptConnectionFromPeer:peerID error:&error])
			NSLog(@"MatchmakingServer: Connection accepted from peer %@", peerID);
		else
			NSLog(@"MatchmakingServer: Error accepting connection from peer %@, %@", peerID, error);
}

- (void)session:(GKSession *)session connectionWithPeerFailed:(NSString *)peerID withError:(NSError *)error
{
	NSLog(@"MatchmakingServer: connection with peer %@ failed %@", peerID, error);

}


- (void)addRandomLabel:(NSInteger)number{
    
    NSString *text = [NSString stringWithFormat:@"%d", number];
    CCLabelTTF *label = [CCLabelTTF labelWithString:text fontName:@"Helvetica" fontSize:12.0];
    label.position = ccp(arc4random_uniform(100), arc4random_uniform(150));
    [self addChild:label];
}

- (void)session:(GKSession *)session didFailWithError:(NSError *)error
{

	NSLog(@"MatchmakingServer: session failed %@", error);

    
	if ([[error domain] isEqualToString:GKSessionErrorDomain])
	{
		if ([error code] == GKSessionCannotEnableError)
		{

			[self endSession];
		}
	}
}

- (void)endSession{
    [_session disconnectFromAllPeers];
	_session.available = NO;
	_session.delegate = nil;
	_session = nil;
}

- (void)connectToServerWithPeerID:(NSString *)peerID
{
	
	_serverPeerID = peerID;
	[_session connectToPeer:peerID withTimeout:_session.disconnectTimeout];
}

- (void) receiveData:(NSData *)data fromPeer:(NSString *)peer inSession: (GKSession *)session context:(void *)context{
    
    //CCLOG(@"Receive data, delegate %@", _delegate);
     BasicPacket* basicPacket = (BasicPacket*)[data bytes];
    
    NSLog(@"Number received: %d", basicPacket->number);
    [self addRandomLabel:basicPacket->number ];
}

@end
