package com.pinterest.slate.security;

import java.security.Principal;

public class UserPrincipal implements Principal {
  private String username;
  private boolean isAdmin;

  public UserPrincipal(String username, boolean isAdmin) {
    this.username = username;
    this.isAdmin = isAdmin;
  }

  @Override
  public String getName() {
    return username;
  }

  @Override
  public String toString() {
    return "UserPrincipal [username=" + username + "]";
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean admin) {
    isAdmin = admin;
  }
}
