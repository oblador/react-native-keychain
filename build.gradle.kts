// Copyright (c) Facebook, Inc. and its affiliates.

// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

buildscript {
  extra.apply {
    set("minSdkVersion", 21)
    set("compileSdkVersion", 30)
    set("targetSdkVersion", 30)
    set("buildToolsVersion", "30.0.2")
  }
  repositories {
    mavenLocal()
    google()
    jcenter()
    maven { url = uri("https://plugins.gradle.org/m2/") }
  }
  dependencies {
    /* https://mvnrepository.com/artifact/com.android.tools.build/gradle?repo=google */
    classpath("com.android.tools.build:gradle:4.2.2")

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
    mavenCentral()
    // All of React Native (JS, Obj-C sources, Android binaries) is installed from npm
    maven {
      url = uri("$rootDir/KeychainExample/node_modules/react-native/android")
    }
  }
}

val updateLibrarySourcesInExample by tasks.registering(Copy::class) {
  into("${rootProject.projectDir}/KeychainExample/node_modules/react-native-keychain/")

  from("${rootProject.projectDir}/android/src/"){
    into("android/src")
  }

  from("${rootProject.projectDir}/typings/"){
    into("typings")
  }

  from("${rootProject.projectDir}/RNKeychainManager"){
    into("RNKeychainManager")
  }

  from("${rootProject.projectDir}/RNKeychain.xcodeproj"){
    into("RNKeychain.xcodeproj")
  }

  from("${rootProject.projectDir}/index.js")
}

tasks.register("build") {
  dependsOn(
    updateLibrarySourcesInExample,
    gradle.includedBuild("android").task(":app:assemble")
  )
}
