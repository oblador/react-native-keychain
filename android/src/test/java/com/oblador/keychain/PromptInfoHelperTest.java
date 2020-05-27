package com.oblador.keychain;

import androidx.biometric.BiometricPrompt.PromptInfo;

import com.facebook.react.bridge.JavaOnlyMap;
import com.oblador.keychain.KeychainModule.AuthPromptOptions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PromptInfoHelperTest {

  @Test
  public void testGood() {
    // GIVEN:
    final JavaOnlyMap options = JavaOnlyMap.of(
        AuthPromptOptions.TITLE,        "title",
        AuthPromptOptions.SUBTITLE,     "subtitle",
        AuthPromptOptions.DESCRIPTION,  "description",
        AuthPromptOptions.CANCEL,       "cancel");

    // WHEN:
    PromptInfo result = PromptInfoHelper.getPromptInfo(options);

    // THEN:
    assertEquals("title",       result.getTitle());
    assertEquals("subtitle",    result.getSubtitle());
    assertEquals("description", result.getDescription());
    assertEquals("cancel",      result.getNegativeButtonText());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingTitleParameter() {
    // GIVEN:
    final JavaOnlyMap options = JavaOnlyMap.of(
        // AuthPromptOptions.TITLE,        "title", // <-- simulate missing
        AuthPromptOptions.SUBTITLE,     "subtitle",
        AuthPromptOptions.DESCRIPTION,  "description",
        AuthPromptOptions.CANCEL,       "cancel");

    // WHEN:
    PromptInfo result = PromptInfoHelper.getPromptInfo(options);

    // THEN:
    // Exception expected
  }
}
