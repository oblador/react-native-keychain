require 'json'
version = JSON.parse(File.read('package.json'))["version"]

Pod::Spec.new do |s|

  s.name           = "RNKeychain"
  s.version        = version
  s.summary        = "Keychain Access for React Native."
  s.homepage       = "https://github.com/oblador/react-native-keychain"
  s.license        = "MIT"
  s.author         = { "Joel Arvidsson" => "joel@oblador.se" }
  s.ios.deployment_target = '9.0'
  s.tvos.deployment_target = '9.0'
  s.source         = { :git => "https://github.com/oblador/react-native-keychain.git", :tag => "v#{s.version}" }
  s.source_files   = 'RNKeychainManager/**/*.{h,m}'
  s.preserve_paths = "**/*.js"
  s.dependency 'React'

end
