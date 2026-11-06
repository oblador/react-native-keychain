#import <XCTest/XCTest.h>
#import <Security/Security.h>
#import <RNKeychain/RNKeychainManager.h>

typedef void (^KeychainCall)(RCTPromiseResolveBlock, RCTPromiseRejectBlock);

@interface RNKeychainIntegrationTests : XCTestCase
@property(nonatomic) RNKeychainManager *manager;
@property(nonatomic) NSString *identifier;
@end

@implementation RNKeychainIntegrationTests

- (void)setUp {
  [super setUp];
  self.manager = [RNKeychainManager new];
  self.identifier = [@"keychain.native:" stringByAppendingString:NSUUID.UUID.UUIDString];
}

- (void)tearDown {
  // Delete only this test's identifiers, including both synchronization scopes.
  for (id itemClass in @[(__bridge id)kSecClassGenericPassword, (__bridge id)kSecClassInternetPassword]) {
    id key = [itemClass isEqual:(__bridge id)kSecClassGenericPassword] ? (__bridge id)kSecAttrService : (__bridge id)kSecAttrServer;
    SecItemDelete((__bridge CFDictionaryRef)@{
      (__bridge id)kSecClass: itemClass, key: self.identifier,
      (__bridge id)kSecAttrSynchronizable: (__bridge id)kSecAttrSynchronizableAny,
    });
  }
  [super tearDown];
}

- (id)invoke:(KeychainCall)call error:(NSString **)errorCode {
  XCTestExpectation *done = [self expectationWithDescription:@"native promise"];
  done.assertForOverFulfill = YES;
  __block id value;
  __block NSString *failure;
  call(^(id result) { value = result; [done fulfill]; },
       ^(NSString *code, NSString *message, NSError *error) { failure = code; [done fulfill]; });
  [self waitForExpectations:@[done] timeout:5];
  if (errorCode) *errorCode = failure;
  else XCTAssertNil(failure);
  return value;
}

- (NSDictionary *)options:(NSDictionary *)extra {
  NSMutableDictionary *options = [extra mutableCopy];
  options[@"service"] = self.identifier;
  options[@"server"] = self.identifier;
  // The JS wrappers supply this normalized prompt before crossing the native boundary.
  options[@"authenticationPrompt"] = @{@"title": @"Keychain test", @"cancel": @"Cancel"};
  return options;
}

- (id)save:(BOOL)internet options:(NSDictionary *)extra username:(NSString *)username {
  NSDictionary *options = [self options:extra];
  return [self invoke:^(RCTPromiseResolveBlock resolve, RCTPromiseRejectBlock reject) {
    if (internet) [self.manager setInternetCredentialsForServer:self.identifier username:username password:@"password 🔑" options:options resolve:resolve reject:reject];
    else [self.manager setGenericPasswordForOptions:options username:username password:@"password 🔑" resolve:resolve reject:reject];
  } error:nil];
}

- (id)read:(BOOL)internet options:(NSDictionary *)extra {
  NSDictionary *options = [self options:extra];
  return [self invoke:^(RCTPromiseResolveBlock resolve, RCTPromiseRejectBlock reject) {
    if (internet) [self.manager getInternetCredentialsForServer:self.identifier options:options resolve:resolve reject:reject];
    else [self.manager getGenericPasswordForOptions:options resolve:resolve reject:reject];
  } error:nil];
}

- (id)has:(BOOL)internet options:(NSDictionary *)extra {
  NSDictionary *options = [self options:extra];
  return [self invoke:^(RCTPromiseResolveBlock resolve, RCTPromiseRejectBlock reject) {
    if (internet) [self.manager hasInternetCredentialsForOptions:options resolve:resolve reject:reject];
    else [self.manager hasGenericPasswordForOptions:options resolve:resolve reject:reject];
  } error:nil];
}

- (void)reset:(BOOL)internet options:(NSDictionary *)extra {
  NSDictionary *options = [self options:extra];
  [self invoke:^(RCTPromiseResolveBlock resolve, RCTPromiseRejectBlock reject) {
    if (internet) [self.manager resetInternetCredentialsForOptions:options resolve:resolve reject:reject];
    else [self.manager resetGenericPasswordForOptions:options resolve:resolve reject:reject];
  } error:nil];
}

- (void)testRoundTripOverwriteAndClassIsolation {
  for (NSNumber *kind in @[@NO, @YES]) {
    BOOL internet = kind.boolValue;
    XCTAssertEqualObjects([self read:internet options:@{}], @NO);
    XCTAssertEqualObjects([self has:internet options:@{}], @NO);
    [self save:internet options:@{} username:@"original"];
    XCTAssertEqualObjects([self has:internet options:@{}], @YES);
    [self save:internet options:@{} username:@"replacement"];
  }
  // A fresh module instance must read OS-persisted records in both Keychain classes.
  self.manager = [RNKeychainManager new];
  for (NSNumber *kind in @[@NO, @YES]) {
    NSDictionary *value = [self read:kind.boolValue options:@{}];
    XCTAssertEqualObjects(value[@"username"], @"replacement");
    XCTAssertTrue([value[@"password"] isEqualToString:@"password 🔑"]);
    XCTAssertEqualObjects(value[kind.boolValue ? @"server" : @"service"], self.identifier);
  }
  [self reset:NO options:@{}];
  XCTAssertEqualObjects([self read:NO options:@{}], @NO);
  XCTAssertEqualObjects([self has:YES options:@{}], @YES);
  [self reset:YES options:@{}];
  [self reset:YES options:@{}];
  XCTAssertEqualObjects([self has:YES options:@{}], @NO);
}

- (void)testSynchronizationScopesSurviveOverwriteAndReset {
  for (NSNumber *kind in @[@NO, @YES]) {
    BOOL internet = kind.boolValue;
    [self save:internet options:@{@"cloudSync": @YES} username:@"synced"];
    XCTAssertEqualObjects([self read:internet options:@{}], @NO);
    XCTAssertEqualObjects([self has:internet options:@{@"cloudSync": @NO}], @NO);
    [self save:internet options:@{} username:@"local"];
    [self save:internet options:@{@"cloudSync": @NO} username:@"local replacement"];
    XCTAssertEqualObjects([self read:internet options:@{}][@"username"], @"local replacement");
    XCTAssertEqualObjects([self read:internet options:@{@"cloudSync": @YES}][@"username"], @"synced");
    [self reset:internet options:@{@"cloudSync": @YES}];
    XCTAssertEqualObjects([self has:internet options:@{}], @YES);
    [self save:internet options:@{@"cloudSync": @YES} username:@"synced again"];
    [self reset:internet options:@{}];
    XCTAssertEqualObjects([self has:internet options:@{@"cloudSync": @NO}], @NO);
    XCTAssertEqualObjects([self has:internet options:@{@"cloudSync": @YES}], @YES);
    [self reset:internet options:@{@"cloudSync": @YES}];
  }
}

- (NSArray *)servers:(NSDictionary *)options {
  return [self invoke:^(RCTPromiseResolveBlock resolve, RCTPromiseRejectBlock reject) {
    [self.manager getAllInternetCredentialServers:options resolve:resolve reject:reject];
  } error:nil];
}

- (void)testListingPreservesServerDeduplicatesAccountsAndRespectsSyncScope {
  [self save:NO options:@{} username:@"generic"];
  XCTAssertFalse([[self servers:@{}] containsObject:self.identifier]);
  [self save:YES options:@{@"cloudSync": @YES} username:@"synced"];
  XCTAssertFalse([[self servers:@{}] containsObject:self.identifier]);
  XCTAssertTrue([[self servers:@{@"cloudSync": @YES}] containsObject:self.identifier]);
  // Seed an older/native writer's records, including two accounts for one server.
  for (NSString *account in @[@"one", @"two"]) {
    OSStatus status = SecItemAdd((__bridge CFDictionaryRef)@{
      (__bridge id)kSecClass: (__bridge id)kSecClassInternetPassword,
      (__bridge id)kSecAttrServer: self.identifier,
      (__bridge id)kSecAttrAccount: account,
      (__bridge id)kSecValueData: [@"legacy" dataUsingEncoding:NSUTF8StringEncoding],
      (__bridge id)kSecAttrSynchronizable: @NO,
    }, NULL);
    XCTAssertEqual(status, errSecSuccess);
  }
  NSArray *matches = [[self servers:@{}] filteredArrayUsingPredicate:[NSPredicate predicateWithFormat:@"SELF == %@", self.identifier]];
  XCTAssertEqual(matches.count, 1u);
  XCTAssertTrue([[self read:YES options:@{}][@"password"] isEqualToString:@"legacy"]);
  [self reset:YES options:@{}];
  XCTAssertFalse([[self servers:@{}] containsObject:self.identifier]);
  XCTAssertTrue([[self servers:@{@"cloudSync": @YES}] containsObject:self.identifier]);
}

- (void)testInvalidAccessGroupRejectsInsteadOfReturningMissingOrChangingOtherScope {
  for (NSNumber *kind in @[@NO, @YES]) {
    BOOL internet = kind.boolValue;
    [self save:internet options:@{} username:@"untouched"];
    NSDictionary *options = [self options:@{@"accessGroup": @"unentitled.keychain.test"}];
    NSString *error;
    id value = [self invoke:^(RCTPromiseResolveBlock resolve, RCTPromiseRejectBlock reject) {
      if (internet) [self.manager setInternetCredentialsForServer:self.identifier username:@"rejected" password:@"unused" options:options resolve:resolve reject:reject];
      else [self.manager setGenericPasswordForOptions:options username:@"rejected" password:@"unused" resolve:resolve reject:reject];
    } error:&error];
    XCTAssertNil(value);
    XCTAssertEqualObjects(error, @"E_INVALID_PARAMETERS");
    XCTAssertEqualObjects([self read:internet options:@{}][@"username"], @"untouched");
  }
}

- (void)testProtectedItemsStoreAccessControlAndAllowNoninteractiveExistenceChecks {
  for (NSNumber *kind in @[@NO, @YES]) {
    BOOL internet = kind.boolValue;
    [self save:internet options:@{@"accessControl": @"BiometryCurrentSet"} username:@"protected"];
    XCTAssertEqualObjects([self has:internet options:@{}], @YES);
    if (internet) XCTAssertTrue([[self servers:@{}] containsObject:self.identifier]);
    NSDictionary *query = @{
      (__bridge id)kSecClass: internet ? (__bridge id)kSecClassInternetPassword : (__bridge id)kSecClassGenericPassword,
      (internet ? (__bridge id)kSecAttrServer : (__bridge id)kSecAttrService): self.identifier,
      (__bridge id)kSecAttrSynchronizable: @NO,
      (__bridge id)kSecReturnAttributes: @YES,
      (__bridge id)kSecUseAuthenticationUI: (__bridge id)kSecUseAuthenticationUIFail,
    };
    CFTypeRef result = NULL;
    XCTAssertEqual(SecItemCopyMatching((__bridge CFDictionaryRef)query, &result), errSecSuccess);
    NSDictionary *attributes = CFBridgingRelease(result);
    XCTAssertNotNil(attributes[(__bridge id)kSecAttrAccessControl]);
  }
}
@end
