CREATE DATABASE IF NOT EXISTS slate;
USE slate;
CREATE TABLE IF NOT EXISTS humantask (
  task_id varchar(255) NOT NULL,
  process_id varchar(500) NOT NULL,
  execution_id varchar(500) NOT NULL,
  assignee_group_name varchar(255) NOT NULL,
  assignee_user varchar(255) DEFAULT NULL,
  task_type varchar(100) NOT NULL,
  additional_data text,
  task_status varchar(100) NOT NULL,
  summary varchar(200) DEFAULT NULL,
  description varchar(5000) DEFAULT NULL,
  comment varchar(5000) DEFAULT NULL,
  create_time timestamp NULL DEFAULT NULL,
  update_time timestamp NULL DEFAULT NULL,
  PRIMARY KEY (process_id,task_id)
);
CREATE TABLE IF NOT EXISTS executiongraph (
  execution_id varchar(500) NOT NULL,
  requester varchar(255) DEFAULT NULL,
  start_time timestamp NULL DEFAULT NULL,
  end_time timestamp NULL DEFAULT NULL,
  status varchar(50) DEFAULT NULL,
  state_path varchar(255) DEFAULT NULL,
  PRIMARY KEY (execution_id)
);
CREATE TABLE IF NOT EXISTS resource (
  id varchar(500) NOT NULL,
  resource_definition_class varchar(255) DEFAULT NULL,
  resource_lock_owner varchar(255) DEFAULT NULL,
  current_state_refresh_timestamp bigint DEFAULT NULL,
  desired_state mediumtext,
  owner varchar(255) DEFAULT NULL,
  region varchar(255) DEFAULT NULL,
  project varchar(255) DEFAULT NULL,
  environment varchar(255) DEFAULT NULL,
  resource_watch_list text,
  input_resource_ids text,
  output_resource_ids text,
  deleted boolean,
  last_update_timestamp timestamp NULL DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS proposedresource (
  id varchar(500) NOT NULL,
  resource_lock_owner varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

alter table resource add column parent_resource_id text after output_resource_ids;
alter table resource add column child_resource_ids varchar(500) after parent_resource_id;
