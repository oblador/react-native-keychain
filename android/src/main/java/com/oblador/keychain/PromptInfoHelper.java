package com.oblador.keychain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.biometric.BiometricPrompt;

import com.facebook.react.bridge.ReadableMap;
import com.oblador.keychain.KeychainModule.AuthPromptOptions;

/*package*/ class PromptInfoHelper {
  /**
   * Extract user specified prompt info from options.
   *
   * @param promptInfoOptionsMap Map containing the following entries:<br>
   *                <li>AuthPromptOptions.TITLE       - title,</li>
   *                <li>AuthPromptOptions.SUBTITLE    - subtitle (optional),</li>
   *                <li>AuthPromptOptions.DESCRIPTION - description (optional),</li>
   *                <li>AuthPromptOptions.CANCEL      - caption for the cancellation button. This must be provided
   *                                                    when BiometricPrompt.KEY_ALLOW_DEVICE_CREDENTIAL is false.</li>
   *
   *
   * @return prompt info for the biometric dialog
   */
  @NonNull
  static BiometricPrompt.PromptInfo getPromptInfo(@Nullable final ReadableMap promptInfoOptionsMap) {
    if (promptInfoOptionsMap == null) {
      throw new IllegalArgumentException("Prompt info map is missing");
    }

    String title = promptInfoOptionsMap.getString(AuthPromptOptions.TITLE);
    if (title == null) {
      throw new IllegalArgumentException("Entry <AuthPromptOptions.TITLE> is missing");
    }

    return new BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(promptInfoOptionsMap.getString(AuthPromptOptions.SUBTITLE))
      .setDescription(promptInfoOptionsMap.getString(AuthPromptOptions.DESCRIPTION))
      .setNegativeButtonText(promptInfoOptionsMap.getString(AuthPromptOptions.CANCEL))
      .build();
  }
}
