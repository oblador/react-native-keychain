Pod::Spec.new do |s|

  s.name         = "RNKeychain"
  s.version      = "0.2.6"
  s.summary      = "Keychain Access for React Native."
  s.homepage     = "https://github.com/oblador/react-native-keychain"
  s.license      = "MIT"
  s.author       = { "Joel Arvidsson" => "joel@oblador.se" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/oblador/react-native-keychain.git", :tag => "v#{s.version}" }
  s.source_files = 'RNKeychainManager/**/*.{h,m}'
  s.preserve_paths = "**/*.js"
  s.dependency 'React'

end
