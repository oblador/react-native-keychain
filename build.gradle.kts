// Copyright (c) Facebook, Inc. and its affiliates.

// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

buildscript {
  extra.apply {
    set("minSdkVersion", 16)
    set("compileSdkVersion", 28)
    set("targetSdkVersion", 28)
    set("buildToolsVersion", "29.0.2")
  }
  repositories {
    mavenLocal()
    google()
    jcenter()
    maven { url = uri("https://plugins.gradle.org/m2/") }
  }
  dependencies {
    classpath("com.android.tools.build:gradle:4.0.0-alpha08")

    /* https://github.com/radarsh/gradle-test-logger-plugin */
    classpath("com.adarshr:gradle-test-logger-plugin:2.0.0")
  }
}

//plugins {
//  id("com.adarshr.test-logger") version "1.7.0" apply false
//}

allprojects {
  repositories {
    mavenLocal()
    google()
    jcenter()
    // All of React Native (JS, Obj-C sources, Android binaries) is installed from npm
    maven {
      url = uri("$rootDir/KeychainExample/node_modules/react-native/android")
    }
  }
}

allprojects {
  configurations.all {
    resolutionStrategy.eachDependency {
      when (requested.group) {
        "com.android.support" -> useVersion("28.0.0")
        "android.arch.lifecycle" -> useVersion("1.1.1")
        "android.arch.core" -> useVersion("1.1.1")
        "com.facebook.fresco" -> useVersion("2.0.+")
      }

      when ("${requested.group}:${requested.name}") {
        "com.facebook.react:react-native" -> useVersion("0.61.2")
        "com.facebook.soloader:soloader" -> useVersion("0.6.+")
      }
    }
  }
}
