package com.oblador.keychain.workaround;

import androidx.annotation.NonNull;

import java.util.List;

public class ListDeviceFilter implements IDeviceFilter {
  private final List<IDeviceFilter> filters;

  public ListDeviceFilter(@NonNull final List<IDeviceFilter> filters) {
    this.filters = filters;
  }

  @Override
  public boolean isDeviceAffected() {
    for (IDeviceFilter deviceFilter : filters) {
      if (deviceFilter.isDeviceAffected()) {
        return true;
      }
    }

    return false;
  }
}
