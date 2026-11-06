#import <XCTest/XCTest.h>
#import <TargetConditionals.h>

@interface RNKeychainAuthenticationTests : XCTestCase
@end

@implementation RNKeychainAuthenticationTests
- (void)testCancelProtectedReadPreservesCredential {
  XCTSkipIf(TARGET_OS_SIMULATOR, @"Simulator does not enforce Keychain biometric ACLs. Run on an enrolled physical iPhone.");
  self.continueAfterFailure = NO;
  XCUIApplication *app = [[XCUIApplication alloc] initWithBundleIdentifier:@"keychain.example"];
  [app launch];
  XCTAssertTrue([app.buttons[@"cleanup"] waitForExistenceWithTimeout:20]);
  [app.buttons[@"cleanup"] tap];
  [self waitForStatus:@"cleanup:ok" app:app];
  [app.buttons[@"internet"] tap];
  [app.buttons[@"save"] tap];
  [self waitForStatus:@"save:ok" app:app];
  [self addTeardownBlock:^{
    [app terminate];
    [app launch];
    [app.buttons[@"cleanup"] tap];
    [self waitForStatus:@"cleanup:ok" app:app];
  }];
  [app.buttons[@"presence"] tap];
  [self waitForStatus:@"presence:ok" app:app];
  [app.buttons[@"load"] tap];
  XCUIApplication *springboard = [[XCUIApplication alloc] initWithBundleIdentifier:@"com.apple.springboard"];
  XCUIElement *cancel = springboard.buttons[@"Cancel"];
  XCTAssertTrue([cancel waitForExistenceWithTimeout:10]);
  [cancel tap];
  [self waitForStatus:@"load:error:E_AUTH_CANCELED" app:app];
  [app.buttons[@"presence"] tap];
  [self waitForStatus:@"presence:ok" app:app];
  [app.buttons[@"cleanup"] tap];
  [self waitForStatus:@"cleanup:ok" app:app];
}

- (void)waitForStatus:(NSString *)status app:(XCUIApplication *)app {
  NSPredicate *matches = [NSPredicate predicateWithFormat:@"label == %@", status];
  XCTNSPredicateExpectation *ready = [[XCTNSPredicateExpectation alloc] initWithPredicate:matches object:app.staticTexts[@"status"]];
  XCTAssertEqual([XCTWaiter waitForExpectations:@[ready] timeout:10], XCTWaiterResultCompleted);
}
@end
