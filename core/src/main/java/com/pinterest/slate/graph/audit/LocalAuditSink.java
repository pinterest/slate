package com.pinterest.slate.graph.audit;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.commons.configuration2.Configuration;

import com.google.gson.Gson;
import com.pinterest.slate.graph.AbstractGraphAuditSink;
import com.pinterest.slate.graph.ExecutionGraph;

public class LocalAuditSink extends AbstractGraphAuditSink {
  
  private static final Gson gson = new Gson();
  private PrintWriter pr;
  
  @Override
  public void init(Configuration config) throws Exception {
    File file = new File(config.getString("file", "/tmp/slate_dev_audit.log"));
    pr = new PrintWriter(new FileWriter(file, true));
  }

  @Override
  public synchronized void audit(ExecutionGraph executionGraph) throws Exception {
    pr.println(gson.toJson(executionGraph));
    pr.flush();
  }

}
