package com.oblador.keychain;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.google.android.gms.auth.blockstore.Blockstore;
import com.google.android.gms.auth.blockstore.BlockstoreClient;
import com.google.android.gms.auth.blockstore.DeleteBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse;
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse.BlockstoreData;
import com.google.android.gms.auth.blockstore.StoreBytesData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.oblador.keychain.KeychainModule.KnownCiphers;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
public class BlockStoreStorage implements KeyValueStorage {
  public static final String TAG = "BlockStoreStorage";

  @NonNull
  private final BlockstoreClient blockstoreClient;
  private Boolean isE2EEncryptionAvailable = null;


  public BlockStoreStorage(@NonNull final ReactApplicationContext reactContext) {
    blockstoreClient = Blockstore.getClient(reactContext);
    updateE2EEncryptionAvailabilityFlagAsync();
  }

  @Nullable
  public ResultSet getEncryptedEntry(@NonNull final String service) {
    String usernameKey = getKeyForUsername(service);
    String passwordKey = getKeyForPassword(service);
    String cipherStorageKey = getKeyForCipherStorage(service);

    RetrieveBytesRequest retrieveRequest = createRetrieveRequest(usernameKey, passwordKey, cipherStorageKey);
    Task<RetrieveBytesResponse> task = blockstoreClient.retrieveBytes(retrieveRequest)
      .addOnSuccessListener(__ -> Log.d(TAG, "getEncryptedEntry: Fetching keys=(" + usernameKey + "," + passwordKey + "," + cipherStorageKey + ") from BlockStore API SUCCEEDED."))
      .addOnFailureListener(error -> Log.e(TAG, "getEncryptedEntry: Fetching keys=(" + usernameKey + "," + passwordKey + "," + cipherStorageKey + ") from BlockStore API FAILED: " + error.getMessage()));

    try {
      Map<String, BlockstoreData> blockstoreData = Tasks.await(task).getBlockstoreDataMap();

      BlockstoreData usernameData = blockstoreData.get(usernameKey);
      BlockstoreData passwordData = blockstoreData.get(passwordKey);
      BlockstoreData cipherStorageData = blockstoreData.get(cipherStorageKey);

      byte[] username = usernameData != null ? usernameData.getBytes() : null;
      byte[] password = passwordData != null ? passwordData.getBytes() : null;
      String cipherStorageName = cipherStorageData != null ? new String(cipherStorageData.getBytes(), StandardCharsets.UTF_8) : null;

      if (username == null || password == null) {
        return null;
      }

      if (cipherStorageName == null) {
        // If the CipherStorage name is not found, we assume it is because the entry was written by an older
        // version of this library. The older version used Facebook Conceal, so we default to that.
        cipherStorageName = KnownCiphers.FB;
      }

      return new ResultSet(cipherStorageName, username, password);
    } catch (Exception exception) {
      Log.e(TAG, "getEncryptedEntry: Awaiting for BlockStore API task failed: " + exception.getMessage());
      return null;
    }
  }

  public void removeEntry(@NonNull final String service) {
    final String keyForUsername = getKeyForUsername(service);
    final String keyForPassword = getKeyForPassword(service);
    final String keyForCipherStorage = getKeyForCipherStorage(service);

    DeleteBytesRequest request = createDeleteRequest(keyForUsername, keyForPassword, keyForCipherStorage);

    blockstoreClient.deleteBytes(request)
      .addOnSuccessListener(__ -> Log.d(TAG, "Removing keys=" + keyForUsername + "," + keyForPassword + "," + keyForCipherStorage + ") from BlockStore API SUCCEEDED."))
      .addOnFailureListener(error -> Log.e(TAG, "Removing keys=" + keyForUsername + "," + keyForPassword + "," + keyForCipherStorage + ") from BlockStore API FAILED: " + error.getMessage()));
  }

  public void storeEncryptedEntry(@NonNull final String service, @NonNull final EncryptionResult encryptionResult) {
    // Username
    final String keyForUsername = getKeyForUsername(service);
    final byte[] valueForUsername = encryptionResult.username;
    StoreBytesData usernameRequest = createSaveRequest(keyForUsername, valueForUsername);
    blockstoreClient
      .storeBytes(usernameRequest)
      .addOnSuccessListener(result -> Log.d(TAG, "Saving key=" + keyForUsername + " to BlockStore API SUCCEEDED, wrote " + result + " bytes."))
      .addOnFailureListener(error -> Log.e(TAG, "Saving key=" + keyForUsername + " to BlockStore API FAILED: " + error));

    // Password
    final String keyForPassword = getKeyForPassword(service);
    final byte[] valueForPassword = encryptionResult.password;
    StoreBytesData passwordRequest = createSaveRequest(keyForPassword, valueForPassword);
    blockstoreClient
      .storeBytes(passwordRequest)
      .addOnSuccessListener(result -> Log.d(TAG, "Saving key=" + keyForPassword + " to BlockStore API SUCCEEDED, wrote " + result + " bytes."))
      .addOnFailureListener(error -> Log.e(TAG, "Saving key=" + keyForPassword + " to BlockStore API FAILED: " + error));

    // Cipher Storage
    final String keyForCipherStorage = getKeyForCipherStorage(service);
    final byte[] valueForCipherStorage = encryptionResult.cipherName.getBytes(StandardCharsets.UTF_8);
    StoreBytesData cipherStorageRequest = createSaveRequest(keyForCipherStorage, valueForCipherStorage);
    blockstoreClient
      .storeBytes(cipherStorageRequest)
      .addOnSuccessListener(result -> Log.d(TAG, "Saving key=" + keyForCipherStorage + " to BlockStore API SUCCEEDED, wrote " + result + " bytes."))
      .addOnFailureListener(error -> Log.e(TAG, "Saving key=" + keyForCipherStorage + " to BlockStore API FAILED: " + error));
  }

  /**
   * List all types of cipher which are involved in en/decryption of the data stored herein.
   * <p>
   * A cipher type is stored together with the datum upon encryption so the datum can later be decrypted using correct
   * cipher. This way, a {@link BlockStoreStorage} can involve different ciphers for different data. This method returns all
   * ciphers involved with this storage.
   *
   * @return set of cipher names
   */
  @NonNull
  public Set<String> getUsedCipherNames() {
    RetrieveBytesRequest request = createRetrieveAllRequest();
    Task<RetrieveBytesResponse> task = blockstoreClient.retrieveBytes(request);
    Map<String, BlockstoreData> dataMap;
    Set<String> keys = new HashSet<>();
    try {
      dataMap = Tasks.await(task).getBlockstoreDataMap();
    } catch (Exception exception) {
      Log.e(TAG, "Awaiting for BlockStore API task inside getAllKeys() failed: " + exception.getMessage());
      return keys;
    }

    for (String key : dataMap.keySet()) {
      if (!isKeyForCipherStorage(key)) continue;

      BlockstoreData data = dataMap.get(key);
      if (data == null) continue;

      String cipher = new String(data.getBytes(), StandardCharsets.UTF_8);
      keys.add(cipher);
    }
    return keys;
  }

  @NonNull
  public static String getKeyForUsername(@NonNull final String service) {
    return service + ":" + "u";
  }

  @NonNull
  public static String getKeyForPassword(@NonNull final String service) {
    return service + ":" + "p";
  }

  @NonNull
  public static String getKeyForCipherStorage(@NonNull final String service) {
    return service + ":" + "c";
  }

  public static boolean isKeyForCipherStorage(@NonNull final String key) {
    return key.endsWith(":c");
  }

  // --------

  private StoreBytesData createSaveRequest(String key, byte[] bytes) {

    // flag should have been already updated async in constructor, but just in case the request failed, update it again now
    if (isE2EEncryptionAvailable == null) {
      updateE2EEncryptionAvailabilityFlag();
    }

    boolean setShouldBackupToCloud = isE2EEncryptionAvailable != null && isE2EEncryptionAvailable;

    return new StoreBytesData.Builder()
      .setShouldBackupToCloud(setShouldBackupToCloud)
      .setKey(key)
      .setBytes(bytes)
      .build();
  }

  private RetrieveBytesRequest createRetrieveRequest(String... keys) {
    return new RetrieveBytesRequest.Builder()
      .setKeys(Arrays.asList(keys))
      .build();
  }

  private RetrieveBytesRequest createRetrieveAllRequest() {
    return new RetrieveBytesRequest.Builder()
      .setRetrieveAll(true)
      .build();
  }

  private DeleteBytesRequest createDeleteRequest(String... keys) {
    return new DeleteBytesRequest.Builder()
      .setKeys(Arrays.asList(keys))
      .build();
  }

  private void updateE2EEncryptionAvailabilityFlagAsync() {
    blockstoreClient.isEndToEndEncryptionAvailable().addOnSuccessListener(available -> isE2EEncryptionAvailable = available);
  }

  private void updateE2EEncryptionAvailabilityFlag() {
    try {
      isE2EEncryptionAvailable = Tasks.await(blockstoreClient.isEndToEndEncryptionAvailable());
    } catch (Exception exception) {
      Log.e(TAG, "Awaiting for BlockStore API task inside updateE2EEncryptionAvailabilityFlag() failed: " + exception.getMessage());
    }
  }
}
