import { Node, Edge } from 'reactflow';

export interface IPosition {
    x: number;
    y: number;
}

export interface INodeData {
    id: string;
    owner: string;
    project: string;
    region: string;
    environment: string;

    resourceDefinitionClass?: string;
    resourceLockOwner?: null | string;
    resourceWatchList?: Array<unknown>;

    // desired state is dependent on resource.
    desiredState: Record<string, unknown>;
    lastUpdateTimestamp?: number;
    inputResources?: Record<string, string[]>;
    outputResources?: Record<string, string[]>;
    parentResource?: string;
    childResources?: string[];
    deleted?: boolean;
}

/* 
    ReactFlowRenderer types. Added separately here, so these can be changed to interface and can be extended (Ex: type is optional but in slate its required)
    ReactFlowRenderer added same generic type (T) for data in both node & edge. But in slate there is no data for edge. So set it as unknown
*/
export type TNode = Node<INodeData>;
export type TEdge = Edge<unknown>;

export interface IChildEdgeType {
    connectedResourceType: string;
    maxCardinality: number;
    minCardinality: number;
}

export interface IResourceDefinition {
    author: string;
    chatLink: string;
    configSchema: Record<string, any>;
    documentationLink: string;
    requiredInboundEdgeTypes: Record<string, any>;
    requiredOutboundEdgeTypes: Record<string, any>;
    requiredChildEdgeTypes: null | IChildEdgeType[];
    requiredParentEdgeTypes: null | IChildEdgeType;
    shortDescription: string;
    simpleName: string;
    uiSchema: Record<string, any>;
}

export type IResourceMap = Record<string, IResourceDefinition>;

export type IResourceTagMap = Record<string, string[]>;

export interface IResourceDefinitions {
    resourceMap: IResourceMap;
    resourceTagMap: IResourceTagMap;
}

export interface IPlanInfo {
    time: number;
    plan: unknown;
    deltaGraph: Record<string, INodeData>;
    executionGraph?: unknown;
    isExecution?: boolean;
    error?: unknown;
}

export enum SidebarType {
    Resource = 'resource',
    Recipe = 'recipe',
}
