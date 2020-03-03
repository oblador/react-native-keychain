package com.oblador.keychain;

import android.security.keystore.KeyInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mockito.MockSettings;
import org.mockito.Mockito;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.UnrecoverableKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import javax.crypto.SecretKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings({"WeakerAccess"})
public final class MocksForProvider {
  public static final String KEY_GENERATOR = "KeyGenerator";
  public static final String KEY_PAIR_GENERATOR = "KeyPairGenerator";
  public static final String KEY_FACTORY = "KeyFactory";
  public static final String KEY_STORE = "KeyStore";
  public static final String KEY_CIPHER = "Cipher";
  public static final String SECRET_KEY_FACTORY = "SecretKeyFactory";

  public final MockSettings settings = withSettings();//.verboseLogging();

  public final Provider.Service service = Mockito.mock(Provider.Service.class, settings);
  public final KeyPairGeneratorSpi kpgSpi = Mockito.mock(KeyPairGeneratorSpi.class, settings);
  public final FakeKeyGeneratorSpi kgSpi = Mockito.mock(FakeKeyGeneratorSpi.class, settings);
  public final FakeSecretKeyFactorySpi skfSpi = Mockito.mock(FakeSecretKeyFactorySpi.class, settings);
  public final FakeKeyFactorySpi kfSpi = Mockito.mock(FakeKeyFactorySpi.class, settings);
  public final FakeKeyStoreSpi ksSpi = Mockito.mock(FakeKeyStoreSpi.class, settings);
  public final FakeCipherSpi cSpi = Mockito.mock(FakeCipherSpi.class, settings);
  public final KeyPair keyPair = Mockito.mock(KeyPair.class, settings);
  public final PrivateKey privateKey = Mockito.mock(PrivateKey.class, settings);
  public final KeyInfo keyInfo = Mockito.mock(KeyInfo.class, settings);
  public final SecretKey secretKey = Mockito.mock(SecretKey.class, settings);
  public final Key key = Mockito.mock(Key.class, settings);

  public void configure(@NonNull final String type, @NonNull final Provider provider, @Nullable final Map<String, Object> configuration) {
    try {
      innerConfiguration(type, provider, configuration);
    } catch (Throwable fail) {
      fail.printStackTrace(System.out);
    }
  }

  private void innerConfiguration(@NonNull final String type, @NonNull final Provider provider, @Nullable final Map<String, Object> configuration)
    throws InvalidKeySpecException, NoSuchAlgorithmException, UnrecoverableKeyException {
    when(service.getProvider()).thenReturn(provider);
    when(kpgSpi.generateKeyPair()).thenReturn(keyPair);
    when(keyPair.getPrivate()).thenReturn(privateKey);

    when(keyInfo.isInsideSecureHardware()).thenReturn(returnForIsInsideSecureHardware(configuration));

    when(kgSpi.engineGenerateKey()).thenReturn(secretKey);
    when(skfSpi.engineGetKeySpec(any(), any())).thenReturn(keyInfo);
    when(kfSpi.engineGetKeySpec(any(), any())).thenReturn(keyInfo);
    when(ksSpi.engineGetKey(any(), any())).thenReturn(key);

    switch (type) {
      case KEY_GENERATOR:
        when(service.newInstance(any())).thenReturn(kgSpi);
        break;
      case KEY_PAIR_GENERATOR:
        when(service.newInstance(any())).thenReturn(kpgSpi);
        break;
      case KEY_FACTORY:
        when(service.newInstance(isNull())).thenReturn(kfSpi);
        break;
      case KEY_STORE:
        when(service.newInstance(isNull())).thenReturn(ksSpi);
        break;
      case SECRET_KEY_FACTORY:
        when(service.newInstance(isNull())).thenReturn(skfSpi);
        break;
      case KEY_CIPHER:
        when(service.newInstance(isNull())).thenReturn(cSpi);
        break;
      default:
        System.err.println("requested unsupported type: " + type);
        break;
    }
  }

  private boolean returnForIsInsideSecureHardware(@Nullable final Map<String, Object> configuration) {
    return getBool(configuration, "isInsideSecureHardware", true);
  }

  private boolean getBool(@Nullable final Map<String, Object> configuration,
                          @NonNull final String key,
                          final boolean $default) {
    if (null == configuration) return $default;

    return Boolean.parseBoolean("" + configuration.getOrDefault(key, $default));
  }
}
