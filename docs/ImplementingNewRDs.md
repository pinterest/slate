# Implementing New Resource Definitions (RDs)

This is a guide on how to implement new RDs in Slate. The guide will cover how the different components need to be implemented in order to create LifecycleProcess to handle e2e changes to allow users to execute their end goal.

## Preface

Let’s start by getting a few basic concepts right as that will make it much easier to implement the ResourceDefinitions.

- Slate’s design is focused on providing “Cloud Managed Service” experience as a layer for your Service i.e. provisioning, updates, deletions etc. for Resources are all handled through Slate. 
- Slate tries to model every action triggered on one or more Resource as a ResourceChange and invokes your code for when it sees a change to a Resource of your service or it’s dependencies (upstream or downstream)
- Slate uses JSON as the language for declarative Resource state, serialization and internal storage
- Once you launch Slate ResourceDefinition your customers should NOT be able to use any other means to change the Resource state as it will cause out of sync state issue

## Implementing a ResourceDefinition 

**Resource**

A Resource is an entity that you are attempting to manage via Slate and for which you will create a ResourceDefinition. Resources operated on in Slate as objects, transported as JSON. 

Below is the structure of a Resource object in Slate.

```
 String id // ID
 String resourceDefinitionClass // ResourceDefinition class
 String resourceLockOwner // used internally to store if the Resource is currently being changed
 JsonObject desiredState // State of the Resource that is saved
 String environment // Environment
 String project // Project
 String owner // LDAP group
 String region // ID region
 Set<String> resourceWatchList // unused at the moment
 Map<String, Set<String>> inputResources // what Resources are input to this Resource (dependency or data)
 Map<String, Set<String>> outputResources // what Resources are output from this Resource (dependency or data)
 boolean deleted // marker for if this Resource is deleted
 long lastUpdateTimestamp; // last timestamp Resource was updated used as version
```

**Barebones**

To implement a Barebones ResourceDefinition you need to implement the:

- No arg constructor and initialize UI, Config schema and required EdgeDefinitions fields
- The planChange() method
- Initialize metadata fields, Tags, Description, Wiki, Chat Link

Here’s an example (please read the code comments to see what each piece does):

```
public class DemoResourceDef implements ResourceDefinition {

// NOTE: fields are needed for JSON serialization getter methods alone will not serialize

// Edge Definition rules enable Slate to check what can and cannot connect to this ResourceDefinition
  private Map<String, EdgeDefinition> requiredInboundEdgeTypes = ImmutableMap.of("i1",
      new EdgeDefinition(DemoResourceDef.class, 0, 1), "i2");
  private Map<String, EdgeDefinition> requiredOutboundEdgeTypes = ImmutableMap.of("o1",
      new EdgeDefinition(DemoResourceDef.class, 0, 1));

// Metadata Field Name of this ResourceDefinition (must not conflict with existing ResourceDefinitions)
  private String simpleName = "Demo";
// Metadata Field who is the author of the ResourceDefinition
  private String author = "Slate Team";
// Field containing the config schema (must be JSON schema format)
  private JsonObject configSchema;
// Field containing the UI schema (must be JSON schema format)
  private JsonObject uiSchema;
// Metadata Field Field containing the Slack link
  private String chatLink = "your slack link";

// Constructor to initialize this ResourceDefinition, this is called once when the Factory initializes all ResourceDefinitions loaded on a Slate Server (multiple instances can have multiple initialization)
  public DemoResourceDef() {
    configSchema = ResourceDefinition.super.getConfigSchema();
    uiSchema = ResourceDefinition.super.getUiSchemaInternal();
  }

// IMPORTANT METHOD: this is the planning method for your ResourceDefinition and will be called by Slate GraphEngine every time there is a change for a Resource of ResourceDefinition type
  @Override
  public Plan planChange(ResourceChange change) throws PlanException {
    LifecycleProcess process = new LifecycleProcess();
    process.setProcessContext(new JsonObject());
    process.setStartTaskId(Task.SUCCEED_PROCESS_TASK);
    Resource pR = change.getProposedResourceObject();
    if (pR.getId() == null) {
      pR.setId("demo_resource_" + System.currentTimeMillis());
    }
    return Plan.of(pR, process, null);
  }

  @Override
  public String getChatLink() {
    return chatLink;
  }

  @Override
  public JsonObject getInternalSchema() {
    try {
      // loading schema from a file, it can also be inlined
      return ResourceFactory.readJsonFile("resources/" + getSimpleName() + "/configschema.json");
    } catch (IOException e) {
      e.printStackTrace();
      return new JsonObject();
    }
  }

  @Override
  public Map<String, EdgeDefinition> getRequiredOutboundEdgeTypes() {
    return requiredOutboundEdgeTypes;
  }

  public Map<String, EdgeDefinition> getRequiredInboundEdgeTypes() {
    return requiredInboundEdgeTypes;
  }

  @Override
  public String getAuthor() {
    return author;
  }

  // Tags are used to group by ResourceDefinitions in the UI so they are organized, typically ResourceDefinitions from a team will belong together
  @Override
  public Set<String> getTags() {
    return ImmutableSet.of("Misc");
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  public JsonObject getConfigSchema() {
    return configSchema;
  }

  public void setConfigSchema(JsonObject configSchema) {
    this.configSchema = configSchema;
  }

  public JsonObject getUiSchema() {
    return uiSchema;
  }

  public void setUiSchema(JsonObject uiSchema) {
    this.uiSchema = uiSchema;
  }

  @Override
  public Resource newInstance(String id) { 
    return new Resource(id, DemoResourceDef.class, 1, 1);
  }

  @Override
  public String getShortDescription() {
    return "Simple do nothing demo Resource to test the Slate UI";
  }

  @Override
  public String getDocumentationLink() {
    return null;
  }

}
```

**Plan Implementation**

Slate works by asking ResourceDefinitions a deterministic / reproducible plan for a change if one exists. The ResourceDefinition is responsible for programmatically understanding the delta in state of Resource. Slate then uses the aggregated plans across all Resources being modified in a given change to construct an ExecutionPlan i.e. the plan that can actually be run. 

**How to handle Iterative Planning?**

Planning is an iterative process, Slate will continue to trigger the planChange() method for your ResourceDefinition (RD) until your returned Plan object stops changing / erroring out i.e. converges (checked using equals()). This approach is very similar to query optimizers in databases. 
In order to accomplish a stable Plan you are free to modify the state of the Resource to let you place markers e.g. let’s say you are trying to allocate a workload to a cluster and there is a possibility that in another iteration your code may generate a different allocation, now this can lead to your Plan object changing i.e. not converging causing Slate to trigger additional iterations. You can therefore save the allocation in the state of the Resource the first time you have a successful allocation and the next time simply refer to the cached allocation (like a getOrCreate() method) to prevent Plan changes.

Note that the Iterative Planning process in Slate is a very powerful tool, as it allows dependency requirements to be fixed iteratively. E.g. Singer needs PubSub topic to publish data so Singer RD has PubSub edge defined, before Slate customers would have to know the PubSub topic configurations and specify them in Singer property file with Slate this is now changed to the Edge definition so Singer RD can query the deltaGraph (with the edge Resource id) in the ResourceChange to get the state of the PubSub topic and auto resolve the configs. Here’s where the iterative planning comes in, imagine a user requested a new Singer->Kafka pipeline in a single request to Slate, now the Kafka topic hasn’t been provisioned or even allocated yet so naturally we can’t and shouldn’t be able to provision Singer Resource without knowing where it will write to. When the planner triggers the planChange() method, it should throw PlanException for Singer RD because the PubSub topic config doesn’t have sufficient data (no Kafka cluster information), next the planner will try the PubSub RD, that will succeed because it has no blocking dependencies and will generate a plan after allocating the topic to a cluster. Now the planner (iteratively) will run the planning again, this time when it runs the Singer RD it will be successful because it’s able to get the necessary Kafka topic configs.

So what happens when the plan keeps changing or erroring? This is where there is a max iteration count setting at the Slate level, currently this value is 100. So if after 100 iterations the plan is NOT stable or is erroring out it will propagate that error to the requesting client (UI or API)

**Declaring Execution Dependencies**

There are scenarios which need to be handled for coordinating the order of execution of Plans & LifecycleProcess e.g. Resource A should be updated before Resource B. Slate provides the option to specify the upstream dependencies in a Plan, these can be specified using the Resource Ids of the upstream dependencies for a given change e.g. Singer provisioning depends on Kafka provisioning so, Singer RD will specify the corresponding Kafka Resource Id and this way the Kafka LifecycleProcess will get executed first followed by the Singer one.

**Input & Output Resources**
Input and Output Resources (handles) the Slate construct for declaratively creating structure around connectivity between resources e.g. dbclientapp and db, pubsubtopic and consumer. It’s used to solve a number of problems:

- Declarative infra / data path / application architecture
- Perform cascading updates (one Resource change triggers other Resources to be updated)
- Programmatically infer configurations / arguments

Input/Output Resources can be thought of as named arguments and named return types of a function where the function is the Resource.

This functionality allows you to reference dependencies for a Resource and plan for them. Slate uses them to automatically check if there are any cascading updates needed. E.g. if you change partition count for a Kafka topic the downstream consumer may need to update its config. In this case the end user only declaratively created the the data flow architecture and Slate + ResourceDefinitions take care of the heavy lifting of what needs to happen to handle the change.

**Implementing a LifecycleProcess**

Slate executes everything through a LifecycleProcess. A LifecycleProcess is a sequence of tasks executed conditionally. Below is an example of  a real LifecycleProcess from Slate (PubSub RD). 

Each LifecycleProcess is composed of Tasks, a task is a declarative container for what should happen (TaskDefinition) and what are the outcomes of it. A Task can have 3 outcomes states:

1. Succeeded
2. Failed
3. Canceled

As the LifecycleProcess developer you can decide what happens when a Task in your process hits these states by specifying a list of Tasks that should be executed at each of these states.

There are 2 built-in Tasks in Slate succeedProcess and failProcess which are available as constants, the rest of the tasks are defined by you as the developer. 

Slate also contains a growing list of TaskDefinitions that will help you do common things like Human Approval, Make Http Call, Upload a file to S3, Delete a file on S3 … and so on.

Let’s build a simple process that works by uploading a file to S3 after it’s approved, if approval fails or upload fails the process will fail.

```
// Initialize the LifecycleProcess object
// Initialize the processContext JsonObject
JsonObject processContext = new JsonObject();
LifecycleProcess process = new LifecycleProcess();
process.setProcessContext(processContext);
String path = "ambudsharma/testslateupload/thisisanexample.txt";

// Populate the uploadTask context
processContext.add(PROPERTYSET_UPLOAD_TASK,
          new JsonObjectBuilder()
              .addP(PutS3ObjectTask.DATA, "This is a test file")
              .addP(PutS3ObjectTask.BUCKET, "mybucket")
 .addP(PutS3ObjectTask.KEY, path)
              .addP(PutS3ObjectTask.REGION, "us-east-1").build());
// Create the task object
// l() is just a helper function to replaces Arrays.asList()
Task uploadTask = new Task(PROPERTYSET_UPLOAD_TASK, PutS3ObjectTask.TASK_DEFINITION_ID,
          l(UPDATE_INSTRUCTION_TASK), l(Task.FAIL_PROCESS_TASK), l(Task.FAIL_PROCESS_TASK));
// Add the upload task to process
process.addTaskNode(uploadTask);

// Populate the approvalTask context
processContext.add(APPROVAL_TASK,
          new JsonObjectBuilder().addP(ASSIGNEE_GROUP, approvalGroup)
              .addP(SUMMARY, "Approve file upload:" + path)
              .addP(DESCRIPTION, "Approve file upload:" + path)
              .build());
// Create the task object
Task approvalTask = new Task(APPROVAL_TASK, GroupApprovalTaskDefinition.TASK_DEFINITION_ID,
          l(PROPERTYSET_UPLOAD_TASK), l(Task.FAIL_PROCESS_TASK), l(Task.FAIL_PROCESS_TASK));
// Add the approvalTask to process
process.addTaskNode(approvalTask);

// we have to tell the process where to start execution from?
process.setStartTaskId(APPROVAL_TASK);
return process;
```

## IDs for your Resources
IDing your Resources is a critical part of onboarding to Slate. Why? Because users will need to be able to find their Resources first before they can update/delete them so Slate needs to be able to identify which Resource is the user talking about. IDs in Slate are:
- Unique
- Immutable
- Compliant with ID format
- Generated (computed) by your ResourceDefinition code

Since IDs are immutable, environment and region cannot be changed for an existing Resource. This validation rule should be kept in mind while designing your ResourceDefinition. AWS ARN is an example of IDs that can be used in Slate.

**Cases you must consider**
New Resources with Random IDs
Your ResourceDefinition should not trust the user specified ID whether existing Resource or new Resource, you should always compute / generate the ID using your own code. E.g. the Slate UI will always create new Resources with the id tmp_ you should ignore this ID and instead recompute the ID yourself. 

**Duplicate Resources**
Consider a scenario where a user provides a desired state such that it will map out to an existing Resource but they have provided the wrong ID. As a result of this when you generate the ID for this Resource it will clash with existing Resource. Your ResourceDefinition is expected to handle this case and throw PlanException when such a case is encountered.


**Testing your code**
In Slate we use Junit 4 to test our code, usually there is 1 unit test class per RD or TaskDefinition(TD) but you can have more than one if needed.

**Testing a ResourceDefinition**
The most comprehensive way to run the tests is to perform E2E testing by sending the deltaGraph to the GraphEngine of Slate and letting it trigger the planning and execution of the change. You can set the before and after graph states in the blocks shown below (they can make code very verbose so you can also store these as files in test/resources and read them before execution). You can therefore simulate all the conditional scenarios for your ResourceDefinition as well as those involving other ResourceDefinitions to see if the desired results are achieved.
Note: Slate provides TaskSystem flag called isDev() which can enable tasks to behave in dev mode i.e. local development mode / unit test mode e.g. your local machine may not have access to an S3 bucket so you can simulate writing the file locally (the isDev flag is already configured by the primeAndRunGraph() method)

```
String proposed = """
	{
// proposed delta graph json
}
	""";
String current = """
	{
	// current delta graph json
	}
	""";
// execute this graph with in-memory database (note that it will automatically approve any human tasks)
ExecutionObjectBundle bundle = TestUtils.primeAndRunGraph(proposed, "target/<rdname>/<testname>", current, 10, myPreExecutionHook()); // if anything you would like to do before executing the graph you can with this callback hook e.g. pull some metrics / configs etc.

ExecutionGraph eg = bundle.getEg();
// your assertions / checks go here
assertNotNull(eg.getExecutionPlan().get("<resourceid>").getProcess());
```




