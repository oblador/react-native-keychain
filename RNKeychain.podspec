require 'json'

version = JSON.parse(File.read('package.json'))["version"]
folly_compiler_flags = '-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1 -Wno-comma -Wno-shorten-64-to-32'

Pod::Spec.new do |s|

  s.name           = "RNKeychain"
  s.version        = version
  s.summary        = "Keychain Access for React Native."
  s.homepage       = "https://github.com/oblador/react-native-keychain"
  s.license        = "MIT"
  s.author         = { "Joel Arvidsson" => "joel@oblador.se" }
  s.ios.deployment_target = '9.0'
  s.tvos.deployment_target = '9.0'
  s.osx.deployment_target = '10.13'
  s.visionos.deployment_target = '1.0'
  s.source         = { :git => "https://github.com/oblador/react-native-keychain.git", :tag => "v#{s.version}" }
  s.source_files   = 'ios/RNKeychainManager/**/*.{h,m}'
  s.preserve_paths = "**/*.js"

  if respond_to?(:install_modules_dependencies, true)
    # React Native Core dependency
    install_modules_dependencies(s)
  else
    s.dependency "React-Core"

    # Don't install the dependencies when we run `pod install` in the old architecture.
    if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
      s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
      s.pod_target_xcconfig    = {
          "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
          "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1",
          "CLANG_CXX_LANGUAGE_STANDARD" => "c++17"
      }
      s.dependency "React-Codegen"
      s.dependency "RCT-Folly"
      s.dependency "RCTRequired"
      s.dependency "RCTTypeSafety"
      s.dependency "ReactCommon/turbomodule/core"
    end
  end

end
