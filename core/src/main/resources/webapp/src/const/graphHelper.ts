import dagre from 'dagre';
import {
    addEdge,
    getConnectedEdges,
    getIncomers,
    getOutgoers,
    MarkerType,
    Position,
    Node,
    Edge,
    XYPosition,
} from 'reactflow';
import { TNode, TEdge, INodeData, IResourceDefinition, IResourceMap } from './types';
import { TTaskNode, TTaskEdge, IExecutionPlan } from './../components/execution/types';
import { NEW_NODE_ID_PREFIX, NEW_EDGE_ID_PREFIX, NodeWidth, NodeDefaultHeight } from './constants';
import { getRandomInt } from './basicUtils';
import { remove, cloneDeep } from 'lodash';
import { fetchResource } from './datasources';

export const getNewNodeId = () => NEW_NODE_ID_PREFIX + `_${Date.now()}`;

export const getNewEdgeId = () => NEW_EDGE_ID_PREFIX + `_${Date.now()}`;

export const isNewNode = (node: TNode): boolean => {
    if (node.id.startsWith(NEW_NODE_ID_PREFIX)) {
        return true;
    }
    return false;
};

export const isNewEdge = (edge: TEdge): boolean => {
    if (edge.id.startsWith(NEW_EDGE_ID_PREFIX)) {
        return true;
    }
    return false;
};

export const computeChildPositionInParent = (): XYPosition => {
    // This is a relative position to parent. This is a rough calculation to show node in ~center which seems to look fine in most cases.
    return {
        x: getRandomInt(10, Math.floor(NodeWidth / 2)),
        y: getRandomInt(NodeDefaultHeight, NodeDefaultHeight * 3),
    };
};

export const undoDeleteNodes = (nodes: TNode[], ids: string[]): TNode[] => {
    const newNodes = [...nodes];
    const nodeIdIndexMap: Record<string, number> = {};
    newNodes.forEach((node, i) => {
        nodeIdIndexMap[node.id] = i;
    });
    ids.forEach((id) => {
        const index = nodeIdIndexMap[id];
        // clone node before making update
        if (index >= 0) {
            newNodes[index] = cloneDeep(newNodes[index]);
            const nodeData = newNodes[index].data;
            if (nodeData && nodeData.deleted) {
                nodeData.deleted = false;
            }
        }
    });
    return newNodes;
};

// This method is part of react-flow v9, but removed from 10.  So copied the source here and slightly modified as its used in couple places.
// When nodes are removed, this will automatically remove any connected edges too
export const removeElements = (idsToRemove: string[], nodes: TNode[], edges: TEdge[]): [TNode[], TEdge[]] => {
    return [
        nodes.filter((node) => !idsToRemove.includes(node.id)),
        edges.filter((edge) => !(idsToRemove.includes(edge.target) || idsToRemove.includes(edge.source))),
    ];
};

// Deleting a node can result in deleting its associated i/o edges too. So this method returns the final nodes, edges for the graph.
// ex: When a user want to remove an existing resource just from builder view, all connected edges should be also removed.
export const deleteNode = (
    nodes: TNode[],
    edges: TEdge[],
    nodeToRemove: TNode,
    removeInView: boolean,
    removeEdgesToo: boolean = false
): [TNode[], TEdge[]] => {
    // since redux store data is readonly, nodes, edges are expected to be cloned before passing to this method
    let finalNodes = [...nodes];
    let finalEdges = [...edges];
    // new nodes
    if (isNewNode(nodeToRemove)) {
        // update i/o json on all connected nodes first.
        const incomingNodes: TNode[] = getIncomers(nodeToRemove, finalNodes, finalEdges);
        incomingNodes.forEach((node) => {
            const outputs = node.data?.outputResources ?? {};
            Object.keys(outputs).forEach((handle) => {
                remove(outputs[handle], (id: string) => id === nodeToRemove.id);
            });
        });
        const outNodes: TNode[] = getOutgoers(nodeToRemove, finalNodes, finalEdges);
        outNodes.forEach((node) => {
            const inputs = node.data?.inputResources ?? {};
            Object.keys(inputs).forEach((handle) => {
                remove(inputs[handle], (id: string) => id === nodeToRemove.id);
            });
        });
        finalNodes = disconnectAnyParentChildConnectionOnDelete(finalNodes, nodeToRemove);
        if (removeEdgesToo) {
            // this will automatically remove any connected edges too
            [finalNodes, finalEdges] = removeElements([nodeToRemove.id], finalNodes, finalEdges);
        } else {
            // just remove the node from array;
            remove(finalNodes, (obj) => obj.id === nodeToRemove.id);
        }
        return [finalNodes, finalEdges];
    }
    // Existing nodes
    if (removeInView) {
        // do no preserve any newly added edges for this existing resource and so remove it from io json
        const nodeIdMap: Record<string, TNode> = {};
        finalNodes.forEach((obj) => {
            nodeIdMap[obj.id] = obj;
        });
        // any newly added edges
        const newEdges: TEdge[] = getConnectedEdges([nodeToRemove], finalEdges).filter((edge) => isNewEdge(edge));
        const inEdges = newEdges.filter((edge) => edge.target === nodeToRemove.id);
        inEdges.forEach((edge) => {
            const outputsMap = nodeIdMap[edge.source].data?.outputResources ?? {};
            if (edge.sourceHandle && outputsMap[edge.sourceHandle]) {
                remove(outputsMap[edge.sourceHandle], (id: string) => id === nodeToRemove.id);
            }
        });
        const outEdges = newEdges.filter((edge) => edge.source === nodeToRemove.id);
        outEdges.forEach((edge) => {
            const inputsMap = nodeIdMap[edge.target].data?.inputResources ?? {};
            if (edge.targetHandle && inputsMap[edge.targetHandle]) {
                remove(inputsMap[edge.targetHandle], (id: string) => id === nodeToRemove.id);
            }
        });
        // No need to remove parent child connection as this is just removing from view
        if (removeEdgesToo) {
            // this will automatically remove any connected edges too
            [finalNodes, finalEdges] = removeElements([nodeToRemove.id], finalNodes, finalEdges);
        } else {
            // just remove the node from array;
            remove(finalNodes, (obj) => obj.id === nodeToRemove.id);
        }
    } else {
        // this gives the option to undo delete
        // any edges will be removed before sending to plan
        if (nodeToRemove.data) {
            nodeToRemove.data.deleted = true;
        }
    }
    return [finalNodes, finalEdges];
};

export const canDeleteEdge = (nodes: TNode[], edgeToRemove: TEdge, removeInView: boolean): boolean => {
    const nodeIdMap: Record<string, TNode> = {};
    nodes.forEach((node) => {
        nodeIdMap[node.id] = node;
    });
    const sourceNode = nodeIdMap[edgeToRemove.source];
    const targetNode = nodeIdMap[edgeToRemove.target];
    // if both source & target exists & either one has  deleted flag true, then dont delete the edge and it will be deleted before plan
    if (sourceNode && targetNode && (sourceNode.data?.deleted || targetNode.data?.deleted)) {
        return false;
    }
    /* To disable existing edge deletion, uncomment below code
    if (!isNewEdge(edgeToRemove)) {
        // only remove when source/target are removed from view.
        if (!sourceNode || !targetNode) {
            return true;
        }
        return false;
    } */
    return true;
};

/*
    Deleting an edge may result in updating node json i/o handles. So this method will return both nodes, edges
    Newly added edges: Will always be removed and its source/target nodes io json will be udpated
    Existing edges: Will always be removed.
        removeInView is false, then its considered actual edge deletion and nodes io json will be updated        
 */
export const deleteEdge = (
    nodes: TNode[],
    edges: TEdge[],
    edgeToRemove: TEdge,
    removeInView: boolean
): [TNode[], TEdge[]] => {
    // since redux store data is readonly, nodes, edges are expected to be cloned before passing to this method
    let finalNodes = [...nodes];
    let finalEdges = [...edges];
    const nodeIdMap: Record<string, TNode> = {};
    finalNodes.forEach((obj) => {
        nodeIdMap[obj.id] = obj;
    });
    const sourceNode = nodeIdMap[edgeToRemove.source];
    const targetNode = nodeIdMap[edgeToRemove.target];
    // if both source & target exists & either one has  deleted flag true, then dont delete the edge and it will be deleted before plan
    if (sourceNode && targetNode && (sourceNode.data?.deleted || targetNode.data?.deleted)) {
        return [finalNodes, finalEdges];
    }
    // if its an existing edge and just remove in view, then delete it without updating node json
    if (!isNewEdge(edgeToRemove) && removeInView) {
        remove(finalEdges, (obj) => obj.id === edgeToRemove.id);
        return [finalNodes, finalEdges];
    }
    // for new (or) to remove existing edge permanently
    // source & target node may not exist. So check before changing node io json
    if (sourceNode && sourceNode.data && edgeToRemove.sourceHandle) {
        const outputs = sourceNode.data.outputResources ?? {};
        if (outputs[edgeToRemove.sourceHandle]) {
            remove(outputs[edgeToRemove.sourceHandle], (id: string) => id === edgeToRemove.target);
        }
    }
    if (targetNode && targetNode.data && edgeToRemove.targetHandle) {
        const inputs = targetNode.data.inputResources ?? {};
        if (inputs[edgeToRemove.targetHandle]) {
            remove(inputs[edgeToRemove.targetHandle], (id: string) => id === edgeToRemove.source);
        }
    }
    remove(finalEdges, (obj) => obj.id === edgeToRemove.id);
    return [finalNodes, finalEdges];
};

export const addNewEdge = (
    nodes: TNode[],
    edges: TEdge[],
    source: string,
    sourceHandle: string,
    target: string,
    targetHandle: string
): [TNode[], TEdge[]] => {
    const newNodes = [...nodes];
    let newEdges = [...edges];
    const sourceIndex = newNodes.findIndex((ele) => ele.id === source);
    const targetIndex = newNodes.findIndex((ele) => ele.id === target);
    if (sourceIndex < 0 || targetIndex < 0) {
        return [newNodes, newEdges];
    }
    // instead of cloning whole element, just clone source & target nodes as we are doing inplace updates.
    newNodes[sourceIndex] = cloneDeep(newNodes[sourceIndex]);
    newNodes[targetIndex] = cloneDeep(newNodes[targetIndex]);
    const sourceNode = newNodes[sourceIndex];
    const targetNode = newNodes[targetIndex];
    if (!sourceNode.data || !targetNode.data) {
        return [newNodes, newEdges];
    }
    const sourceData = sourceNode.data;
    const targetData = targetNode.data;
    // update json
    if (!sourceData.outputResources) {
        sourceData.outputResources = {};
    }
    if (!sourceData.outputResources[sourceHandle]) {
        sourceData.outputResources[sourceHandle] = [];
    }
    if (!sourceData.outputResources[sourceHandle].includes(targetNode.id)) {
        sourceData.outputResources[sourceHandle].push(targetNode.id);
    }

    if (!targetData.inputResources) {
        targetData.inputResources = {};
    }
    if (!targetData.inputResources[targetHandle]) {
        targetData.inputResources[targetHandle] = [];
    }
    if (!targetData.inputResources[targetHandle].includes(sourceNode.id)) {
        targetData.inputResources[targetHandle].push(sourceNode.id);
    }
    // add edge
    newEdges = addEdge(
        {
            source,
            target,
            sourceHandle,
            targetHandle,
            type: 'smoothstep',
            animated: true,
            markerEnd: MarkerType.ArrowClosed,
            data: {},
            id: getNewEdgeId(),
        },
        newEdges
    );
    return [newNodes, newEdges];
};

/*
    This will add any input/output edges present between existing resources in the graph. 
    For new nodes, user will manually add the edge, so no need to compute. but if needed, just change filter logic and edges will be computed for new nodes too

    @parameters:
        includeNewNodes: used for recipe import to workspace where node_id will start with "tmp_"
*/
export const addEdgeElementsBetweenNodes = (
    nodes: TNode[],
    edges: TEdge[],
    animated: boolean = false,
    includeNewNodes: boolean = false
): TEdge[] => {
    let newEdges = edges;
    try {
        // separate nodes (only existing nodes) & edges.
        const nodeIdMap: Record<string, TNode> = {};
        nodes.forEach((element) => {
            if (includeNewNodes || !isNewNode(element)) {
                nodeIdMap[element.id] = element;
            }
        });
        Object.values(nodeIdMap).forEach((node) => {
            const nodeEdges: TEdge[] = getConnectedEdges([node], edges);
            const nodeData = node.data;
            // Iterate through either node inputs (or) outputs and verify if edges are present.
            // Since we check all input edges on a node & verify it with output edges of other nodes, no need to iterate on outputs again.
            if (nodeData) {
                const inputHandleMap = nodeData.inputResources ?? {};
                // Since we are iterating on inputs, the handle here will become the targetHandle on an edge
                Object.keys(inputHandleMap).forEach((targetHandle) => {
                    // filter out the ones present in graph
                    const sourceNodeIds = inputHandleMap[targetHandle].filter((id) => nodeIdMap[id]);
                    sourceNodeIds.forEach((sourceNodeId) => {
                        // TODO: In the current schema, only nodeids are stored as a set. But to connect nodes, we need to find the source & target handle.
                        // Till the backend is changed, identify the source handle by checking output resources of the source node
                        const sourceNodeOutputs = nodeIdMap[sourceNodeId].data?.outputResources ?? {};
                        let sourceHandle: string | null = null;
                        for (const outputHandle in sourceNodeOutputs) {
                            // There can be duplicates, but there are no such scenarios in production, so going with this approach temporarily
                            if (sourceNodeOutputs[outputHandle].indexOf(node.id) >= 0) {
                                sourceHandle = outputHandle;
                                break;
                            }
                        }
                        // check if edge exists
                        const inExists = nodeEdges.some(
                            (edge) =>
                                edge.target === node.id &&
                                edge.targetHandle === targetHandle &&
                                edge.source === sourceNodeId &&
                                edge.sourceHandle === sourceHandle
                        );
                        if (!inExists) {
                            // added for debugging
                            console.log(`Adding missing edge from source: ${sourceNodeId} to target: ${node.id}`);
                            newEdges = addEdge(
                                {
                                    source: sourceNodeId,
                                    sourceHandle: sourceHandle,
                                    target: node.id,
                                    targetHandle: targetHandle,
                                    type: 'smoothstep',
                                    animated: animated,
                                    markerEnd: MarkerType.ArrowClosed,
                                    data: {},
                                    style: {},
                                    // don't set id with new edge id as this is an existing edge on resources
                                },
                                newEdges
                            );
                        }
                    });
                });
            }
        });
        return newEdges;
    } catch (error) {
        console.error(`Error creating edges: `, error);
        return edges;
    }
};

/*
    Deleted property of a resource can be set to true to mark the resource for deletion in backend. Since we show deleted nodes (along with edges) in the graph with red color,
    update the input/output json for connected nodes to remove edges before sending to plan/execute
*/
export const updateIOResourceJsonForDeletedNodes = (nodes: TNode[], edges: TEdge[]): TNode[] => {
    const newNodes = nodes;
    const deletedNodes: TNode[] = [];
    newNodes.forEach((obj) => {
        if (obj?.data?.deleted === true) {
            deletedNodes.push(obj);
        }
    });
    deletedNodes.forEach((node) => {
        // update i/o json on all connected nodes.
        const incomingNodes: TNode[] = getIncomers(node, newNodes, edges);
        incomingNodes.forEach((inode) => {
            const outputs = inode.data?.outputResources ?? {};
            Object.keys(outputs).forEach((handle) => {
                remove(outputs[handle], (id: string) => id === node.id);
            });
        });
        const outNodes: TNode[] = getOutgoers(node, newNodes, edges);
        outNodes.forEach((onode) => {
            const inputs = onode.data?.inputResources ?? {};
            Object.keys(inputs).forEach((handle) => {
                remove(inputs[handle], (id: string) => id === node.id);
            });
        });
    });
    return newNodes;
};

export const computeGraphForPlan = (nodes: TNode[], edges: TEdge[]): Record<string, INodeData> => {
    // Do a clone to modify as this computation updates node data and is referring to objects in redux store directly
    const newNodes = cloneDeep(nodes);
    /*
        Uncomment to activate edge deletions.
        TODO: this is commented as backend cannot process both node deletion + remove edges on its connected nodes.
        
        updateIOResourceJsonForDeletedNodes(newNodes, edges);
    */
    const result: Record<string, INodeData> = {};
    // Fetch only nodes as there is no physical edge data stored in backend
    newNodes.forEach((node) => {
        if (node.data) {
            let resourceObject = node.data;
            resourceObject.resourceDefinitionClass = node.type;
            delete resourceObject.desiredState.owner;
            delete resourceObject.desiredState.project;
            delete resourceObject.desiredState.region;
            delete resourceObject.desiredState.environment;
            result[node.data.id] = resourceObject;
        }
    });
    // added for debugging
    console.log('Computed Graph', result);
    return result;
};

export const buildGraphElementsFromPlan = (plan: Record<string, IExecutionPlan>): [TTaskNode[], TTaskEdge[]] => {
    const nodes: TTaskNode[] = [];
    const edges: TTaskEdge[] = [];
    Object.keys(plan).forEach((v) => {
        const process = plan[v].process;
        if (process) {
            const tasks = process.allTasks;
            // add nodes
            Object.keys(tasks).map((t) => {
                const pContext = process.processContext ?? {};
                const task = tasks[t];
                nodes.push({
                    id: v + '-' + t,
                    data: { taskJson: { label: t, task: task }, contextJson: pContext[t] },
                    position: { x: 0, y: 0 },
                    type: 'taskNode',
                });
            });
            Object.keys(tasks).map((t) => {
                const task = tasks[t];
                const nexts = task.nextPointers;
                Object.keys(nexts).map((edgeLabel) => {
                    const edgeEntry = nexts[edgeLabel];
                    for (var e in edgeEntry) {
                        let otherNodeId = edgeEntry[e];
                        const edge: TTaskEdge = {
                            id: v + '-' + t + 'to' + v + '-' + otherNodeId + '-' + edgeLabel,
                            source: v + '-' + t,
                            sourceHandle: edgeLabel.toLowerCase(),
                            target: v + '-' + otherNodeId,
                            targetHandle: 'target',
                            label: edgeLabel,
                            type: 'smoothstep',
                            markerEnd: MarkerType.Arrow,
                            data: {},
                        };
                        edges.push(edge);
                    }
                });
            });
        }
    });
    return [nodes, edges];
};

export const organizeGraphElements = (nodes: Node[], edges: Edge[], direction = 'LR'): Node[] => {
    const dagreGraph = new dagre.graphlib.Graph();
    dagreGraph.setDefaultEdgeLabel(() => ({}));

    const nodeWidth = 250;
    const nodeHeight = 36;
    const isHorizontal = direction === 'LR';
    dagreGraph.setGraph({ rankdir: direction, marginx: 40, marginy: 40 });

    nodes.forEach((el) => {
        dagreGraph.setNode(el.id, { width: nodeWidth, height: nodeHeight });
    });
    edges.forEach((el) => {
        dagreGraph.setEdge(el.source, el.target);
    });

    dagre.layout(dagreGraph);

    return nodes.map((el) => {
        const nodeWithPosition = dagreGraph.node(el.id);
        el.targetPosition = isHorizontal ? Position.Left : Position.Top;
        el.sourcePosition = isHorizontal ? Position.Right : Position.Bottom;
        // unfortunately we need this little hack to pass a slightly different position
        // to notify react flow about the change. Moreover we are shifting the dagre node position
        // (anchor=center center) to the top left so it matches the react flow node anchor point (top left).
        el.position = {
            x: nodeWithPosition.x - nodeWidth / 2 + Math.random() / 1000,
            y: nodeWithPosition.y - nodeHeight / 2,
        };
        return el;
    });
};

/* 
    React flow has a limitation "It's important that parent nodes appear before their children in the nodes/ defaultNodes array to get processed correctly."
    When resources are added to builder, make sure they are ordered, otherwise the child nodes aren't placed properly inside parent node.
    @parameters:
        map: resource definition map. This is needed when temp nodes are present as they don't have data json to verify if its parent/child.
            This is an optional param if all passed nodes are existing ones.
        nodes: List to order by parents first
*/
export const arrangeNodesByParentsFirst = (map: null | IResourceMap, nodes: TNode[]): TNode[] => {
    const childs: TNode[] = [];
    const nonChilds: TNode[] = [];
    nodes.forEach((obj) => {
        if (isNodeAChild(map, obj)) {
            childs.push(obj);
        } else {
            nonChilds.push(obj);
        }
    });
    return nonChilds.concat(childs);
};

export const getParentNodes = (map: IResourceMap, nodes: TNode[]): TNode[] => {
    return nodes.filter((node) => isNodeAParent(map, node));
};

/*
    @parameters:
        resourceDefOrMap: resource definition or map. This is required when the passed node is a tmp node as it doesn't have data json to verify if its a parent.
            This is an optional param for existing node.
        node: Node to check
*/
export const isNodeAParent = (resourceDefOrMap: null | IResourceDefinition | IResourceMap, node: TNode): boolean => {
    if (!node.type) {
        return false;
    }
    // for existing resources, we can refer to child based on node data. So first check based on that
    if ('childResources' in node.data) {
        if (node.data.childResources?.length) {
            return true;
        } else {
            return false;
        }
    }
    // if there is no child resources key, then its a temp node. So need definition to check
    let definition: null | IResourceDefinition = null;
    if (!resourceDefOrMap) {
        definition = null;
    } else if (resourceDefOrMap.simpleName) {
        definition = resourceDefOrMap as IResourceDefinition;
    } else {
        definition = (resourceDefOrMap as IResourceMap)[node.type];
    }
    if (!definition) {
        throw new Error('Unable to check if resource is a parent due to missing resource map/definition');
    }
    if (definition.requiredChildEdgeTypes?.length) {
        return true;
    }
    return false;
};

/*
    @parameters:
        map: resource definition map. This is required when the passed node is a tmp node as it doesn't have data json to verify if its a child.
            This is an optional param for existing node.
        node: Node to check
*/
export const isNodeAChild = (map: null | IResourceMap, node: TNode): boolean => {
    if (!node.type) {
        return false;
    }
    // for existing resources, we can refer to parent based on node data. So first check based on that
    if ('parentResource' in node.data) {
        if (node.data.parentResource) {
            return true;
        } else {
            return false;
        }
    }
    // if there is no parent resource key, then its a temp node. So need definition to check
    if (!map || !map[node.type]) {
        throw new Error('Unable to check if resource is a child due to missing resource map/definition');
    }
    const definition = map[node.type];
    if (definition.requiredParentEdgeTypes?.connectedResourceType) {
        return true;
    }
    return false;
};

export const getNodeParentType = (map: IResourceMap, node: TNode): null | string => {
    if (!node.type || !map[node.type]) {
        return null;
    }
    const definition = map[node.type];
    return definition.requiredParentEdgeTypes?.connectedResourceType ?? null;
};

/*
    This method connects by updating the data json i.e, parent resource & child resources. 
    This will not update react flow owned parent child properties and the caller is responsible to set those
    @parameters:
        allNodes: all nodes which includes both child and a possible parent.
        childNode: this is the child node which should have a parentNode key to determine the parent.
    Returns all nodes connecting child with a parent if applicable
*/
export const connectChildWithParent = (allNodes: TNode[], childNode: TNode): TNode[] => {
    if (!childNode.parentNode) {
        return allNodes;
    }
    const parentNode = allNodes.find((obj) => obj.id === childNode.parentNode);
    if (!parentNode) {
        return allNodes;
    }
    // update parent data json
    const existingChildren = parentNode.data.childResources ?? [];
    if (!existingChildren.includes(childNode.id)) {
        existingChildren.push(childNode.id);
        parentNode.data.childResources = existingChildren;
    }
    // update child data json
    childNode.data.parentResource = parentNode.id;
    return allNodes;
};

/*
    This method removes any parent child connection if exists on node deletion by updating the data json i.e, parent resource & child resources.
    @parameters:
        allNodes: all nodes which includes both child and a possible parent.
        nodeToDel: Node to be deleted. This can be a regular node, a child or a parent.
    FYI: Currently, Parent/regular node deletion doesn't effect data json nor need json change. If required in future, we can support it
        So for now, this method effects only child node deletion which updates its parent data json.
    Returns all nodes modifying data json (if applicable)
*/
export const disconnectAnyParentChildConnectionOnDelete = (allNodes: TNode[], nodeToDel: TNode): TNode[] => {
    if (nodeToDel.data.parentResource || nodeToDel.parentNode) {
        // child node. find parent
        const childId = nodeToDel.id;
        const parentId = nodeToDel.data.parentResource;
        const parent = allNodes.find((obj) => obj.id == parentId);
        if (!parent || !parent.data.childResources || !parent.data.childResources.includes(childId)) {
            return allNodes;
        }
        parent.data.childResources = parent.data.childResources.filter((id) => id != childId);
    }
    return allNodes;
};

/*
    This a private method to fetch given resource and its parent/children and returns all the nodes in hierarchy
    @parameters:
        id: resource id to get
        getParent: To get parent of the passed resource id
        getChildren: Set this if you want to fetch the children of the passed resource id
        loadRecursively: To recusively load all-parents/all-children/both depending on passed params        
        loadedIds: Store ids fetched so far, otherwise this function runs infinitely as child fetches its parent & parent fetches its childs.
*/
const getResourceWithParentAndChilds = async (
    id: string,
    getParent: boolean,
    getChildren: boolean,
    loadRecursively: boolean,
    loadedIds: string[] = []
): Promise<null | TNode[]> => {
    if (loadedIds.includes(id)) {
        return null;
    }
    const data: null | INodeData = await fetchResource(id);
    if (!data) {
        return null;
    }
    const response: TNode[] = [];
    loadedIds.push(data.id);

    const isChildNode = !!data.parentResource;
    const isParentNode = (data.childResources?.length ?? 0) > 0;
    const node: TNode = {
        // randomize position as static co-ordinates creates overlapping nodes
        position: {
            x: getRandomInt(200, 600),
            y: getRandomInt(200, 400),
        },
        type: data.resourceDefinitionClass,
        data: data,
        id: data.id,
    };
    if (isChildNode && data.parentResource) {
        node.parentNode = data.parentResource;
        node.expandParent = true;
        node.position = computeChildPositionInParent();
        if (getParent) {
            const list = await getResourceWithParentAndChilds(
                data.parentResource,
                loadRecursively ? getParent : false,
                loadRecursively ? getChildren : false,
                loadRecursively,
                loadedIds
            );
            if (list) {
                response.push(...list);
            }
        }
    }

    response.push(node);

    // get child resources if needed
    if (isParentNode) {
        node.style = {
            width: NodeWidth * 2,
            height: NodeDefaultHeight * 3,
        };
        if (getChildren && data.childResources?.length) {
            // To fetch nodes sequentially, this should be a classic for loop instead of an arrow function
            for (let i = 0; i < data.childResources.length; i++) {
                const resId = data.childResources[i];
                const list = await getResourceWithParentAndChilds(
                    resId,
                    loadRecursively ? getParent : false,
                    loadRecursively ? getChildren : false,
                    loadRecursively,
                    loadedIds
                );
                if (list) {
                    response.push(...list);
                }
            }
        }
    }
    return response;
};

/*
    This function will fetch given resource and its parent/children and returns all the nodes in hierarchy
    @parameters:
        id: id of the resource to get
        loadParent: To get parent of the passed resource id
        loadChildren: Set this if you want to fetch the children of the passed resource id
        loadRecursively: To recusively load all-parents/all-children/both depending on passed params
*/
export const getResourceAndItsDependents = async (
    id: string,
    loadParent: boolean = true,
    loadChildren: boolean = false,
    loadRecursively: boolean = false
): Promise<TNode[]> => {
    const nodeList = await getResourceWithParentAndChilds(id, loadParent, loadChildren, loadRecursively, []);
    if (!nodeList || !nodeList.length) {
        return [];
    }
    // order nodes by parent first due to react flow limitation
    return arrangeNodesByParentsFirst(null, nodeList);
};
