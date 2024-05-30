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

/**
 * Google BlockStore API storage.
 * @see <a href="https://developers.google.com/identity/blockstore/android">BlockStore Docs</a>
 * <p>
 * IMPORANT NOTES:
 * 1) Block Store data is persisted across the app uninstall/reinstall only when the user enables Backup services (it can be checked at Settings > Google > Backup)
 * 2) Cloud backup and restore is enabled only when:
 *      - End to End encryption is enabled
 *      - Source device runs API 23+
 *      - Target device runs API 31+ (API 29+ for Pixel phones)
 * 3) E2E encryption is supported:
 *      - on devices running Android 9 (API 29) and above
 *      - when device have a screen lock set with a PIN, pattern, or password
 **/
@SuppressWarnings({"unused", "WeakerAccess"})
public class BlockStoreStorage implements KeyValueStorage {
  public static final String TAG = BlockStoreStorage.class.getSimpleName();

  @NonNull
  private final BlockstoreClient blockstoreClient;

  // E2E flag will be updated async in the constructor and if still uninitialized, before the first saving request.
  private volatile Boolean isE2EEncryptionAvailable = null;

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
      Map<String, BlockstoreData> blockstoreData = Tasks.await(task).getBlockstoreDataMap(); // fetch data synchronously

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
    // Save username asynchronously
    final String keyForUsername = getKeyForUsername(service);
    final byte[] valueForUsername = encryptionResult.username;
    StoreBytesData usernameRequest = createSaveRequest(keyForUsername, valueForUsername);
    blockstoreClient
      .storeBytes(usernameRequest)
      .addOnSuccessListener(result -> Log.d(TAG, "Saving key=" + keyForUsername + " to BlockStore API SUCCEEDED, wrote " + result + " bytes."))
      .addOnFailureListener(error -> Log.e(TAG, "Saving key=" + keyForUsername + " to BlockStore API FAILED: " + error));

    // Save password asynchronously
    final String keyForPassword = getKeyForPassword(service);
    final byte[] valueForPassword = encryptionResult.password;
    StoreBytesData passwordRequest = createSaveRequest(keyForPassword, valueForPassword);
    blockstoreClient
      .storeBytes(passwordRequest)
      .addOnSuccessListener(result -> Log.d(TAG, "Saving key=" + keyForPassword + " to BlockStore API SUCCEEDED, wrote " + result + " bytes."))
      .addOnFailureListener(error -> Log.e(TAG, "Saving key=" + keyForPassword + " to BlockStore API FAILED: " + error));

    // Save cipher storage asynchronously
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

  // --------

  @NonNull
  private StoreBytesData createSaveRequest(String key, byte[] bytes) {

    // flag should have been already updated async in constructor, but just in case the request failed, update it again now
    if (isE2EEncryptionAvailable == null) {
      updateE2EEncryptionAvailabilityFlagSync();
    }

    boolean setShouldBackupToCloud = isE2EEncryptionAvailable != null && isE2EEncryptionAvailable;

    return new StoreBytesData.Builder()
      .setShouldBackupToCloud(setShouldBackupToCloud)
      .setKey(key)
      .setBytes(bytes)
      .build();
  }

  @NonNull
  private RetrieveBytesRequest createRetrieveRequest(String... keys) {
    return new RetrieveBytesRequest.Builder()
      .setKeys(Arrays.asList(keys))
      .build();
  }

  @NonNull
  private RetrieveBytesRequest createRetrieveAllRequest() {
    return new RetrieveBytesRequest.Builder()
      .setRetrieveAll(true)
      .build();
  }

  @NonNull
  private DeleteBytesRequest createDeleteRequest(String... keys) {
    return new DeleteBytesRequest.Builder()
      .setKeys(Arrays.asList(keys))
      .build();
  }

  private void updateE2EEncryptionAvailabilityFlagAsync() {
    blockstoreClient.isEndToEndEncryptionAvailable().addOnSuccessListener(available -> isE2EEncryptionAvailable = available);
  }

  private void updateE2EEncryptionAvailabilityFlagSync() {
    try {
      isE2EEncryptionAvailable = Tasks.await(blockstoreClient.isEndToEndEncryptionAvailable());
    } catch (Exception exception) {
      Log.e(TAG, "Awaiting for BlockStore API task inside updateE2EEncryptionAvailabilityFlag() failed: " + exception.getMessage());
    }
  }
}
