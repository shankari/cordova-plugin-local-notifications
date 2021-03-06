/*
 * AppDelegate+APPLocalNotificationAction.m
 *
 * Created by Elli Rego on 10/28/15.
 *
 */

 #import "AppDelegate+APPLocalNotificationAction.h"

 @implementation AppDelegate (APPLocalNotificationAction)

/**
 * Handle notification actions for iOS < 9.
 */
- (void)            application:(UIApplication *)application
     handleActionWithIdentifier:(NSString *)identifier
           forLocalNotification:(UILocalNotification *)notification
              completionHandler:(void (^)(void))completionHandler
 {
 	NSDictionary *userInfo = [NSDictionary dictionaryWithObject:notification forKey:@"localNotification"];
    [[NSNotificationCenter defaultCenter] postNotificationName:@"SendActionIdentifier" object:identifier userInfo:userInfo];
    
    completionHandler();
 }

/**
 * Handle notification actions with optional response info for iOS >= 9.
 */
 - (void) application:(UIApplication *)application handleActionWithIdentifier:(NSString *)identifier forLocalNotification:(UILocalNotification *)notification withResponseInfo:(NSDictionary *)responseInfo completionHandler:(void (^)())completionHandler   
{   
    NSDictionary *userInfo = [NSDictionary dictionaryWithObjectsAndKeys:notification, @"localNotification", responseInfo, @"responseInfo", nil];
    [[NSNotificationCenter defaultCenter] postNotificationName:@"SendActionIdentifier" object:identifier userInfo:userInfo];
  
    completionHandler();   
}  

- (void) application:(UIApplication *)application didReceiveLocalNotification:(nonnull UILocalNotification *)notification
{
    NSLog(@"Received local notification %@ while in the foreground, reposting", notification);
    [[NSNotificationCenter defaultCenter] postNotificationName:CDVLocalNotification object:notification];
}

 @end
