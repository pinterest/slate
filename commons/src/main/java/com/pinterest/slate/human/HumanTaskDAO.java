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
package com.pinterest.slate.human;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;

import io.dropwizard.hibernate.AbstractDAO;

public class HumanTaskDAO extends AbstractDAO<HumanTask> implements AbstractHumanTaskDAO {

  private static final Logger logger = Logger.getLogger(HumanTaskDAO.class.getCanonicalName());

  public HumanTaskDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public List<HumanTask> listPendingTasksForAssigneeUser(String user) {
    logger.fine(() -> "Requesting tasks for user:" + user);
    return list(namedQuery("tasksPendingForAssignee").setParameter("assigneeUser", user));
  }

  @Override
  public List<HumanTask> listPendingTasksForAssigneeGroup(Set<String> groups) {
    logger.fine(() -> "Requesting tasks for groups:" + groups);
    return list(namedQuery("tasksPendingForGroups").setParameter("assigneeGroupNames", groups));
  }

  @Override
  public HumanTask get(HumanTaskId id) {
    return super.get(id);
  }
  
  @Override
  public HumanTask persist(HumanTask entity) throws HibernateException {
    return super.persist(entity);
  }
}
