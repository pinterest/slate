package com.pinterest.slate.validation;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;

import com.pinterest.slate.resources.Resource;

public class LDAPValidator implements ResourceValidator {

  private static final String MSG = "please make sure you are using a valid LDAP group. You can find your LDAP groups https://stk.pinadmin.com/ldap-checker or request a new LDAP group https://pinch/ldap";
  private static final Logger logger = Logger.getLogger(LDAPValidator.class.getCanonicalName());
  @SuppressWarnings("rawtypes")
  private Hashtable env;

  @SuppressWarnings({ "unchecked" })
  @Override
  public void init(Configuration config) throws Exception {
    env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldaps://ldap.pinadmin.com:636");
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
  }

  @Override
  public void validate(Resource resource) throws ValidationException {
    boolean failed = false;
    try {
      DirContext ctx = null;
      try {
        ctx = new InitialDirContext(env);
        NamingEnumeration<SearchResult> search = ctx.search(
            "ou=Security,ou=Prod,ou=groups,dc=pinterest,dc=com",
            "(&(objectClass=*)(cn=" + resource.getOwner() + "))", getSimpleSearchControls());
        if (!search.hasMore()) {
          SearchResult next = search.next();
          if (next.getAttributes().get("member").size() < 1) {
            failed = true;
          }
        }
        search.close();
        ctx.close();
      } catch (NamingException e) {
        logger.log(Level.SEVERE, "Failed to query ldap for: " + resource.getOwner(), e);
        throw new ValidationException("Resource Owner LDAP group validation failed", e);
      } finally {
        if (ctx != null) {
          ctx.close();
        }
      }
    } catch (Exception e) {
      throw new ValidationException("Invalid ldap group:" + resource.getOwner() + " " + MSG, e);
    }
    if (failed) {
      throw new ValidationException(
          "Resource Owner(" + resource.getOwner() + ") ldap group doesn't exist" + MSG);
    }
  }

  public static SearchControls getSimpleSearchControls() {
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchControls.setTimeLimit(30000);
    searchControls.setCountLimit(100);
    String[] attrIDs = { "cn", "member" };
    searchControls.setReturningAttributes(attrIDs);
    return searchControls;
  }

  public static void main(String[] args) throws Exception {
    LDAPValidator ldapValidation = new LDAPValidator();
    ldapValidation.init(new PropertiesConfiguration());
    Resource r = new Resource();
    for (String s : Arrays.asList("ads-infra", "ads-data", "ads-targeting", "merchant-success",
        "ads-measurement", "engineering", "observability", "visual-signal",
        "data-processing-platform", "cd-indexing", "axp-platform",
        "pinner-notifications", "native-publishing", "coredata", "core-services",
        "axp-billing", "ads-serving", "creator-platform", "eng-m10n", "homefeed")) {
      r.setOwner(s);
      ldapValidation.validate(r);
    }
  }

}
