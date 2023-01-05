package com.pinterest.slate.security;

import java.security.Principal;
import java.util.Set;

import javax.ws.rs.core.SecurityContext;

public class SlateSecurityContext implements SecurityContext {

  private static final String SLATE_AUTH = "slateauth";
  private UserPrincipal principal;
  private Set<String> roles;

  public SlateSecurityContext(UserPrincipal principal, Set<String> roles) {
    this.principal = principal;
    this.roles = roles;
  }

  @Override
  public Principal getUserPrincipal() {
    return principal;
  }

  @Override
  public boolean isUserInRole(String role) {
    return roles.contains(role);
  }

  @Override
  public boolean isSecure() {
    return true;
  }

  @Override
  public String getAuthenticationScheme() {
    return SLATE_AUTH;
  }

  @Override
  public String toString() {
    return "SlateSecurityContext [principal=" + principal + ", roles=" + roles + "]";
  }

}
