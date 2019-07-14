// Copyright (c) Facebook, Inc. and its affiliates.

// This source code is licensed under the MIT license found in the
// LICENSE file in the root directory of this source tree.

buildscript {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.1")
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }
}
