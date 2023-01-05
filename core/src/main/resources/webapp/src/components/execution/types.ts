import { Node, Edge } from 'reactflow';

export type TExecutionStatus = 'RUNNING' | 'NOT_STARTED' | 'FAILED' | 'SUCCEEDED' | 'CANCELLED';

export interface IStdMessage {
    timestamp: number;
    message: string;
}
export interface IExecutionTaskJson {
    endTimeMs: number;
    instanceId: string;
    nextPointers: Record<string, Array<string>>;
    startTimeMs: number;
    status: TExecutionStatus;
    stdErr: Array<IStdMessage>;
    stdOut: Array<IStdMessage>;
    taskDefinitionId: string;
}

export interface IExecutionPlanProcess {
    allTasks: Record<string, IExecutionTaskJson>;
    currenTaskSet: Array<string>;
    endStatus: TExecutionStatus;
    endTimeMs: number;
    executionId: string;
    maxConcurrentTasks: number;
    // process context json varies depending on graph nodes
    processContext: Record<string, any>;
    processId: string;
    processType: null | string;
    startTaskId: string;
    startTimeMs: number;
}

export interface IProposedResource {
    deleted: boolean;
    // depends on type of resource
    desiredState: Record<string, any>;
    environment: string;
    id: string;
    inputResources: any;
    lastUpdateTimestamp: number;
    outputResources: any;
    owner: string;
    project: string;
    region: string;
    resourceDefinitionClass: string;
    resourceLockOwner: null | string;
    resourceWatchList: Array<unknown>;
}

export interface IExecutionPlan {
    currentResource: null | Record<string, any>;
    newId: string;
    oldId: string;
    // process will be null when there are no changes to execute (backend may dump in other dependencies)
    process: null | IExecutionPlanProcess;
    proposedResource: IProposedResource;
    upstreamVertices: Array<unknown>;
}

export interface IExecutionGraph {
    allEdgeMutations: unknown;
    currentPlanSet: Array<string>;
    endTime: string;
    executionId: string;
    executionPlan: Record<string, IExecutionPlan>;
    requester: string;
    startTime: string;
    stateStoragePath: string;
    status: TExecutionStatus;
}

export interface ITaskNodeData {
    contextJson: unknown;
    taskJson: {
        label: string;
        task: IExecutionTaskJson;
    };
}

/* 
    ReactFlowRenderer types. Added separately here, so these can be changed to interface and can be extended (Ex: type is optional but in slate its required)
    ReactFlowRenderer added same generic type (T) for data in both node & edge. But in slate there is no data for edge. So set it as unknown
*/
export type TTaskNode = Node<ITaskNodeData>;
export type TTaskEdge = Edge<unknown>;

export type TExecutionDetailTabKey = 'plan_graph' | 'plan_json' | 'delta_graph' | 'json_diff_view' | 'resource_diff';
