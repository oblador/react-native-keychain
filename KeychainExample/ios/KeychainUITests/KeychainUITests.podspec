Pod::Spec.new do |s|
  s.name = 'KeychainUITests'
  s.version = '1.0.0'
  s.summary = 'Local integration tests for react-native-keychain.'
  s.homepage = 'https://github.com/oblador/react-native-keychain'
  s.license = 'MIT'
  s.author = 'react-native-keychain contributors'
  s.source = { :git => 'https://github.com/oblador/react-native-keychain.git' }
  s.ios.deployment_target = '15.1'
  s.source_files = '*.{m,mm}'
  s.frameworks = 'XCTest', 'Security'

end
