/**
 * Copyright 2023 Pinterest, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.slate.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.pinterest.slate.validation.ValidationException;

public class LDAPUtils {

  private static final String SEARCH_SCOPE = System.getProperty("LDAP_SEARCH_SCOPE");
  private static final Logger logger = Logger.getLogger(LDAPUtils.class.getName());
  private static final Pattern UID_EXTRACTION = Pattern.compile(".*(uid=([a-zA-Z]+),).*");
  private static final String LDAP_URL = System.getProperty("LDAP_URL");

  private LDAPUtils() {
  }

  public static List<String> getLDAPMembers(String ldapGroup) throws Exception {
    Hashtable<Object, Object> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, LDAP_URL);
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    try {
      DirContext ctx = null;
      try {
        List<String> members = new ArrayList<>();
        ctx = new InitialDirContext(env);
        NamingEnumeration<SearchResult> search = ctx.search(SEARCH_SCOPE,
            "(&(objectClass=*)(cn=" + ldapGroup + "))", getSimpleSearchControls());
        if (search.hasMore()) {
          SearchResult next = search.next();
          if (next.getAttributes().get("member").size() > 0) {
            Attribute result = next.getAttributes().get("member");
            for (int i = 0; i < result.size(); i++) {
              members.add(extractString(result.get(i).toString(), UID_EXTRACTION, 2));
            }
          }
        } else {
          throw new Exception("Ldap group not found");
        }
        search.close();
        ctx.close();
        return members;
      } catch (NamingException e) {
        logger.log(Level.SEVERE, "Failed to query ldap for: " + ldapGroup, e);
        throw new ValidationException("Resource Owner LDAP group validation failed", e);
      } finally {
        if (ctx != null) {
          ctx.close();
        }
      }
    } catch (Exception e) {
      throw new Exception("Invalid ldap group:" + ldapGroup, e);
    }
  }

  public static String extractString(String input, Pattern pattern, int group) {
    Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return matcher.group(group);
    } else {
      return null;
    }
  }

  public static SearchControls getSimpleSearchControls() {
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchControls.setTimeLimit(30000);
    searchControls.setCountLimit(10);
    String[] attrIDs = { "cn", "member" };
    searchControls.setReturningAttributes(attrIDs);
    return searchControls;
  }

  public static void main(String[] args) throws Exception {
    System.out.println(LDAPUtils.getLDAPMembers("ads-indexing"));
  }

}