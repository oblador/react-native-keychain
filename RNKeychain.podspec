require 'json'

version = JSON.parse(File.read('package.json'))["version"]

Pod::Spec.new do |s|

  s.name           = "RNKeychain"
  s.version        = version
  s.summary        = "Keychain Access for React Native."
  s.homepage       = "https://github.com/oblador/react-native-keychain"
  s.license        = "MIT"
  s.author         = { "Joel Arvidsson" => "joel@oblador.se" }
  s.ios.deployment_target = '15.1'
  s.tvos.deployment_target = '15.1'
  s.osx.deployment_target = '10.13'
  s.visionos.deployment_target = '1.0' if s.respond_to?(:visionos)
  s.source         = { :git => "https://github.com/oblador/react-native-keychain.git", :tag => "v#{s.version}" }
  s.source_files   = 'ios/RNKeychainManager/**/*.{h,m,mm,cpp}'
  s.preserve_paths = "**/*.js"
  s.frameworks     = 'Security', 'LocalAuthentication'

  install_modules_dependencies(s)
end
