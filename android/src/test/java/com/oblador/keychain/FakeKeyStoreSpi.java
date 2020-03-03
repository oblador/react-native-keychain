package com.oblador.keychain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

public final class FakeKeyStoreSpi extends KeyStoreSpi {

  @Override
  public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException {
    return null;
  }

  @Override
  public Certificate[] engineGetCertificateChain(String alias) {
    return new Certificate[0];
  }

  @Override
  public Certificate engineGetCertificate(String alias) {
    return null;
  }

  @Override
  public Date engineGetCreationDate(String alias) {
    return null;
  }

  @Override
  public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {

  }

  @Override
  public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {

  }

  @Override
  public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {

  }

  @Override
  public void engineDeleteEntry(String alias) throws KeyStoreException {

  }

  @Override
  public Enumeration<String> engineAliases() {
    return null;
  }

  @Override
  public boolean engineContainsAlias(String alias) {
    return false;
  }

  @Override
  public int engineSize() {
    return 0;
  }

  @Override
  public boolean engineIsKeyEntry(String alias) {
    return false;
  }

  @Override
  public boolean engineIsCertificateEntry(String alias) {
    return false;
  }

  @Override
  public String engineGetCertificateAlias(Certificate cert) {
    return null;
  }

  @Override
  public void engineStore(OutputStream stream, char[] password) throws CertificateException, IOException, NoSuchAlgorithmException {

  }

  @Override
  public void engineLoad(InputStream stream, char[] password) throws CertificateException, IOException, NoSuchAlgorithmException {

  }
}
