package com.oblador.keychain.components;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CancellationSignal;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.oblador.keychain.SecurityLevel;

import java.security.Key;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

@TargetApi(Build.VERSION_CODES.M)
public class BiometricPromptHelper extends BiometricPrompt.AuthenticationCallback {
  private CancellationSignal mBiometricPromptCancellationSignal;
  private BiometricPrompt mBiometricPrompt;
  private FragmentActivity mActivity;
  private BiometricAuthenticationResult mBiometricAuthenticationResult;

  public interface BiometricAuthenticationResult {
    void onError(int errorCode, @Nullable CharSequence errString);
    void onSuccess ();
  }

  public BiometricPromptHelper(FragmentActivity activity) {
    mActivity = activity;
  }

  // We don't really want to do anything here
  // the error message is handled by the info view.
  // And we don't want to throw an error, as the user can still retry.
  @Override
  public void onAuthenticationFailed() {}

  @Override
  public void onAuthenticationError(int errorCode, @Nullable CharSequence errString) {
      mBiometricPromptCancellationSignal.cancel();
      mBiometricAuthenticationResult.onError(errorCode, errString);
  }

  @Override
  public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
    mBiometricAuthenticationResult.onSuccess();
  }

  public boolean canStartFingerprintAuthentication() {
    return BiometricManager.from(mActivity).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
  }

  public void startFingerprintAuthentication(BiometricAuthenticationResult biometricAuthenticationResult) throws Exception {
    mBiometricAuthenticationResult = biometricAuthenticationResult;
    // If we have a previous cancellationSignal, cancel it.
    if (mBiometricPromptCancellationSignal != null) {
      mBiometricPromptCancellationSignal.cancel();
    }

    mBiometricPrompt = new BiometricPrompt(mActivity, Executors.newSingleThreadExecutor(), this);
    mBiometricPromptCancellationSignal = new CancellationSignal();

    BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
      .setTitle("Authentication required")
      .setNegativeButtonText("Cancel")
      .setSubtitle("Please use biometric authentication to unlock the app")
      .build();

    mActivity.runOnUiThread(new Runnable() {
      public void run() {
        mBiometricPrompt.authenticate(promptInfo);
      }
    });
  }
}
