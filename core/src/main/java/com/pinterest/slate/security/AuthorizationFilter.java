package com.pinterest.slate.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import com.pinterest.slate.SlateConfig;

@Provider
@Priority(1000)
public class AuthorizationFilter implements ContainerRequestFilter {

  private static final Logger LOG = Logger
      .getLogger(AuthorizationFilter.class.getCanonicalName());
  public static final String ADMIN_ROLE_NAME = "admin";
  private Set<String> allowedAdminGroups = new HashSet<>();
  private Set<String> allowedAdminUsers = new HashSet<>();
  private static final Set<String> ADMIN_ROLE_SET = new HashSet<>(Arrays.asList(ADMIN_ROLE_NAME));
  private static final Set<String> EMPTY_ROLE_SET = new HashSet<>();
  public static final Pattern SPIFFE_EXTRACTOR = Pattern.compile(".*URI=(.*)\\].*");
  private boolean enableDevelopment;

  public void configure(SlateConfig config) throws Exception {
    Set<String> adminGroups = config.getAdminGroups();
    Set<String> adminUsers = config.getAdminUsers();
    enableDevelopment = config.isEnableDevelopment();
    if (adminUsers != null) {
      allowedAdminUsers.addAll(adminUsers);
      LOG.info("Following users will be allowed admin access:" + allowedAdminUsers);
    }
    if (adminGroups != null) {
      allowedAdminGroups.addAll(adminGroups);
      LOG.info("Following groups will be allowed admin access:" + allowedAdminGroups);
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    requestContext.setSecurityContext(new SlateSecurityContext(new UserPrincipal("genericuser", false), EMPTY_ROLE_SET));
  }

}
