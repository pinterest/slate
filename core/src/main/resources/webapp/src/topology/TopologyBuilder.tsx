import React, { useMemo, useState, Fragment, useEffect, useRef, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { IWorkspaceTabData } from '../store/types';
import {
    selectBuilderState,
    initializeWorkspaceTabs,
    setSelectedTabId,
    setWorkspaceTabCount,
    setNodeToEdit,
    setWorkspaceLoaded,
    setOpenResourceEditor,
    updateOrAddNodeInElements,
    setStaleResourceIds,
} from '../store/slice/builder';
import ReactFlow, {
    MiniMap,
    Controls,
    Background,
    Connection,
    NodeProps,
    useReactFlow,
    isNode,
    isEdge,
    NodeChange,
    EdgeChange,
    applyNodeChanges,
    applyEdgeChanges,
} from 'reactflow';
import localforage from 'localforage';
import { useStyles } from '../AppStyles.js';
import OptionBar from './OptionBar';
import { Box, Typography, DialogTitle, Dialog, DialogContent, DialogActions, Alert, Button } from '@mui/material';
import { BuilderWorkspaceStorageKey, NodeWidth, NodeDefaultHeight } from '../const/constants';
import { v4 as uuidv4 } from 'uuid';
import PlanViewer from './PlanViewer.js';
import ResourceConfigEditor from './ResourceConfigEditor';
import WorkspaceTabs from '../components/builder/WorkspaceTabs';
import CustomNode from '../components/builder/CustomNode';
import ContextMenu from '../components/builder/ContextMenu';
import DeleteConfirmation from '../components/builder/DeleteConfirmation';
import { fetchResource } from '../const/datasources';
import {
    getNewNodeId,
    addNewEdge,
    computeGraphForPlan,
    arrangeNodesByParentsFirst,
    getParentNodes,
    isNodeAParent,
    isNodeAChild,
    getNodeParentType,
    connectChildWithParent,
    computeChildPositionInParent,
} from '../const/graphHelper';
import { TNode, TEdge, INodeData, IPlanInfo } from '../const/types';
import { useSnackBar } from '../context/SnackbarContext';
import { useLoadingSpinner } from '../context/LoadingSpinnerContext';
import { useWorkspaceElementsContext } from '../components/builder/context/WorkspaceElementsContext';
import { cloneDeep } from 'lodash';
import '../builder.scss';

localforage.config({
    name: 'topologybuild',
    storeName: 'slate',
});

function getWindowDimensions() {
    const { innerWidth: width, innerHeight: height } = window;
    return {
        width,
        height,
    };
}

interface ITopologyBuilderProps {}
const TopologyBuilder: React.FC<ITopologyBuilderProps> = () => {
    const dispatch = useDispatch();
    const classes = useStyles();
    const { showSnackbar } = useSnackBar();
    const { showLoadingOverlay } = useLoadingSpinner();
    const reactFlowWrapper = useRef<HTMLDivElement>(null);

    const [snapToGrid, setSnapToGrid] = useState(false);
    const [grid, setGrid] = useState(true);
    const [planDialog, setPlanDialog] = useState(false);
    const [confirmExecution, setConfirmExecution] = useState(false);
    const [showStaleResourceDialog, setShowStaleResourceDialog] = useState(false);
    const [refreshResourceError, setRefreshResourceError] = useState<string | null>(null);
    const [planInfo, setPlanInfo] = useState<null | IPlanInfo>(null);
    const [windowDimensions, setWindowDimensions] = useState(getWindowDimensions());
    const [previousPlanInfo, setPreviousPlanInfo] = useState<null | IPlanInfo>(null);
    const dragNodeRef = useRef<null | TNode>(null);

    const reactFlowInstance = useReactFlow();
    const { getIntersectingNodes } = useReactFlow();

    const {
        selectedTabId,
        workspaceTabs,
        workspaceTabCount,
        workspaceLoaded,
        resourceDefinitions,
        staleResourceIdsMap,
        openResourceEditor,
        nodeToEdit,
    } = useSelector(selectBuilderState);
    const staleResourceIds = staleResourceIdsMap[selectedTabId] ?? [];

    const {
        wsNodes,
        wsEdges,
        showDeleteConfirmation,
        isPlanRunAfterGraphUpdate,
        updateElements,
        selectNodeAndOpenResourceEditor,
        undoDeleteNodesById,
        removeElementsById,
        setIsPlanRunAfterGraphUpdate,
    } = useWorkspaceElementsContext();

    useEffect(() => {
        // when data is updated, store the current graph to show diff view of delta graph
        setPreviousPlanInfo(planInfo);
    }, [wsNodes, wsEdges]);

    useEffect(() => {
        setPreviousPlanInfo(null);
        setPlanInfo(null);
    }, [selectedTabId]);

    // detect stale resources when opening a workspace
    useEffect(() => {
        if (!wsNodes || wsNodes.length < 1) {
            return;
        }
        // if stale resources are already present, then user didn't want to refresh them.
        // so no need to refetch
        if (staleResourceIds.length > 0) {
            return;
        }
        // existing resources have last updated timestamp.
        const resourceLastTimestampMap: Record<string, number> = {};
        wsNodes.forEach((node) => {
            if (node.data && node.data.id && node.data.lastUpdateTimestamp) {
                resourceLastTimestampMap[node.data.id] = node.data.lastUpdateTimestamp;
            }
        });
        const resIds = Object.keys(resourceLastTimestampMap);
        if (resIds.length < 1) {
            return;
        }
        // TODO: update this api call after backend change
        /*
        fetch('/api/v2/resources/lastupdatetimestamps?data=' + resIds, {
            headers: {
                'Content-Type': 'application/json',
            },
        })
            .then((response) => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error('Lastupdatetimestamps fetch failed for:' + resIds);
                }
            })
            .then((data) => {
                const staleIds = Object.keys(data ?? {}).filter((id) => {
                    if (resourceLastTimestampMap[id] && resourceLastTimestampMap[id] < data[id]) {
                        return true;
                    }
                    return false;
                });
                if (staleIds.length) {
                    dispatch(setStaleResourceIds({ tabId: selectedTabId, staleIds }));
                    setShowStaleResourceDialog(true);
                }
            });
            */
    }, [workspaceLoaded, selectedTabId]);

    const onSave = useCallback(() => {
        if (!reactFlowInstance) {
            return;
        }
        const flowObj = reactFlowInstance.toObject();
        const data = {
            workspaceTabCount,
            selectedTabId: selectedTabId,
            workspaceTabs: {
                ...workspaceTabs,
                [selectedTabId]: {
                    ...workspaceTabs[selectedTabId],
                    nodes: flowObj.nodes,
                    edges: flowObj.edges,
                },
            },
        };
        // When connectors are present rsInstance object contains a reference to a DOMNode which can't be saved directly using localForage.
        // So stringify it before storing
        localforage.setItem(BuilderWorkspaceStorageKey, JSON.stringify(data));
        showSnackbar({
            type: 'info',
            message: 'Saved locally',
        });
    }, [reactFlowInstance, selectedTabId, workspaceTabs]);

    const onRestore = useCallback(async () => {
        // Avoid loading workspace if already loaded when navigating between tabs (or) when searching for a resource
        if (workspaceLoaded) {
            return;
        }
        const objJson: string | null = await localforage.getItem(BuilderWorkspaceStorageKey);
        let tabsObj: any = null;
        if (objJson) {
            try {
                tabsObj = JSON.parse(objJson);
            } catch (err) {
                // to support older non-serialized
                tabsObj = objJson;
            }
        }
        let tabId = null;
        let workspaceTabsObj: Record<string, IWorkspaceTabData> = {};
        let tabCount = null;
        // format with tabs
        if (tabsObj && tabsObj.selectedTabId) {
            Object.keys(tabsObj.workspaceTabs).forEach((key) => {
                // check for old data format i.e, if elements are present
                // this will be null | array.
                const tabElements: undefined | Array<any> = tabsObj.workspaceTabs[key].elements;
                if (tabElements) {
                    // old format where reactflow has elements
                    const nodes: TNode[] = tabElements.filter((ele: any) => isNode(ele));
                    const edges: TEdge[] = tabElements.filter((ele: any) => isEdge(ele));
                    // remove the property
                    delete tabsObj.workspaceTabs[key].elements;
                    const tabData: IWorkspaceTabData = {
                        ...tabsObj.workspaceTabs[key],
                        nodes: nodes,
                        edges: edges,
                    };
                    workspaceTabsObj[key] = tabData;
                } else {
                    // current data format, so just assign it.
                    workspaceTabsObj[key] = tabsObj.workspaceTabs[key];
                }
            });
            tabId = tabsObj.selectedTabId;
            tabCount = tabsObj.workspaceTabCount;
        } else {
            // old format without tabs
            tabId = uuidv4();
            workspaceTabsObj[tabId] = {
                nodes: (tabsObj?.elements ?? []).filter((ele: any) => isNode(ele)),
                edges: (tabsObj?.elements ?? []).filter((ele: any) => isEdge(ele)),
                title: 'Workspace-1',
            };
        }
        dispatch(setSelectedTabId(tabId));
        // old format doesn't have tabCount saved in cache. So compute if not present
        dispatch(setWorkspaceTabCount(tabCount ?? Object.keys(workspaceTabsObj).length));
        dispatch(initializeWorkspaceTabs(workspaceTabsObj));
        dispatch(setWorkspaceLoaded(true));
        // const [x = 0, y = 0] = flowObj.position;
        // transform({ x, y, zoom: flow.zoom || 0 });
        showSnackbar({
            type: 'info',
            message: 'Reloaded saved workspace ' + BuilderWorkspaceStorageKey,
        });
    }, [dispatch, workspaceLoaded]);

    useEffect(() => {
        onRestore();
        const handleResize = () => {
            setWindowDimensions(getWindowDimensions());
        };
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    const onConnect = (params: TEdge | Connection) => {
        if (!params.source || !params.target || !params.sourceHandle || !params.targetHandle) {
            return;
        }
        const [newNodes, newEdges] = addNewEdge(
            wsNodes,
            wsEdges,
            params.source,
            params.sourceHandle,
            params.target,
            params.targetHandle
        );
        updateElements(newNodes, newEdges);
    };

    const onDragOver = (event: React.DragEvent<HTMLDivElement>) => {
        event.preventDefault();
        if (event.dataTransfer) {
            event.dataTransfer.dropEffect = 'move';
        }
    };
    const onDrop = (event: React.DragEvent<HTMLDivElement>) => {
        if (!reactFlowInstance || !reactFlowWrapper.current || !event.dataTransfer) {
            return;
        }
        event.preventDefault();
        const reactFlowBounds = reactFlowWrapper.current.getBoundingClientRect();
        const type = event.dataTransfer.getData('application/reactflow');
        const position = reactFlowInstance.project({
            x: event.clientX - reactFlowBounds.left,
            y: event.clientY - reactFlowBounds.top,
        });
        const nodeId = getNewNodeId();
        const newNode: TNode = {
            id: nodeId,
            type,
            position,
            data: {
                id: nodeId,
                owner: '',
                project: '',
                region: '',
                environment: '',
                desiredState: {},
            },
            // This will be added by react flow upon drag or any movement.
            // Without this field intersections will be empty until the node is moved/dragged (Ex: when a parent node is added and if a child is dropped onto it or is dragged into it)
            positionAbsolute: position,
        };
        const resMap = resourceDefinitions?.resourceMap ?? {};
        const isChildNode = isNodeAChild(resMap, newNode);
        const isParentNode = isNodeAParent(resMap, newNode);
        // in nested parent child scenario, a node can be both parent & child. So use if block
        // for parent, double the dimension to differentiate
        if (isParentNode) {
            newNode.style = {
                width: NodeWidth * 2,
                height: NodeDefaultHeight * 3,
            };
        }
        // for child nodes, check if they are dropped on a parent.
        if (isChildNode) {
            // for intersection, we should pass a rect or an existing node
            // width, height = 1 to ensure that drop cursor to be placed inside a node for intersection to be true
            const rect = {
                ...position,
                width: 1,
                height: 1,
            };
            const parentType = getNodeParentType(resMap, newNode);
            // filter intersections to find out eligible parent.
            const eligibleParentNodes = getIntersectingNodes(rect, true, getParentNodes(resMap, wsNodes)).filter(
                (obj) => obj.type == parentType
            );
            if (eligibleParentNodes && eligibleParentNodes.length == 1) {
                const droppedOntoNode = eligibleParentNodes[0];
                newNode.parentNode = droppedOntoNode.id;
                // newNode.extent = 'parent'; // to disable automatic parent width, height expansion when moving a child
                newNode.expandParent = true;
                // position helps display child inside parent as its a relative position. So use this computed pos instead of dropped pos
                newNode.position = computeChildPositionInParent();
                // added for debugging
                console.log(`Node ${newNode.id} dropped onto its parent type ${droppedOntoNode.id}.`);
            }
        }
        dispatch(setNodeToEdit(newNode));
        dispatch(setOpenResourceEditor(true));
    };

    const selectAllNodes = () => {
        const newNodes = wsNodes.map((obj) => {
            return { ...obj, selected: true };
        });
        updateElements(newNodes, wsEdges, true);
    };

    /*
        When a node is dragged onto another node fully, create a parent child relation if applicable        
    */
    const onNodeDragStop = useCallback(
        (dragNode: TNode) => {
            const resMap = resourceDefinitions?.resourceMap;
            if (!resMap || !dragNode.type) {
                return;
            }
            const isNodeDraggedAChild = isNodeAChild(resMap, dragNode);
            const draggedNodeParentType = getNodeParentType(resMap, dragNode);
            // no action needed when non-child nodes are dragged
            if (!isNodeDraggedAChild) {
                return;
            }
            const intersections = getIntersectingNodes(dragNode, true, getParentNodes(resMap, wsNodes));
            // when a child node is dragged in its parent dimensions, intersection will have its parent node. So check that and filter possible parents based on type
            const allowedParents = intersections.filter(
                (obj) => obj.type == draggedNodeParentType && obj.id !== dragNode.parentNode
            );
            if (allowedParents.length > 0) {
                // There won't be multiple records, but if there are, choose the first one
                const parentToBe = allowedParents[0];
                let newNodes = cloneDeep(wsNodes);
                const pNode = newNodes.find((obj) => obj.id === parentToBe.id);
                const cNode = newNodes.find((obj) => obj.id === dragNode.id);
                if (pNode && cNode) {
                    // added for debugging
                    console.log(`Node ${cNode.id} dragged onto its parent type ${pNode.id}. So connecting both nodes`);
                    cNode.parentNode = pNode.id;
                    cNode.expandParent = true;
                    cNode.position = computeChildPositionInParent();
                    newNodes = connectChildWithParent(newNodes, cNode);
                    // after the connection remove the highlighted classname, otherwise child nodes will be invisible
                    pNode.className = '';
                    updateElements(newNodes, wsEdges);
                }
            }
        },
        [wsNodes, wsEdges, resourceDefinitions]
    );

    /*
        When a non child node is dragged onto parent, warn user by showing red background. This can ideally be part of onNodeDrag handler, but
        For controlled flow, nodeChangesHandler will be fired first follwed by onNodeDrag i.e, node updates will be done twice in redux store for each drag pos(x,y)
        Due to 2 redux state changes immediately (in nodeChangesHandler + onNodeDrag), the drag effect is slightly flickering and not smooth
        To avoid that & improve render performance, calling this method nodeChangesHandler by utlizing dragNodeRef and returning updated node data to sent to redux store once        
    */
    const getChangesForNodeDrag = useCallback(
        (allNodes: TNode[], nodeDragged: TNode): TNode[] => {
            const resMap = resourceDefinitions?.resourceMap;
            if (!resMap || !nodeDragged.type) {
                return allNodes;
            }
            // Note: node update is needed always to reset previously applied classname when nodes are moved out
            const newNodes = allNodes.map((n) => ({ ...n, className: '' }));
            const isDraggedNodeAChild = isNodeAChild(resMap, nodeDragged);
            const draggedNodeParentType = getNodeParentType(resMap, nodeDragged);
            if (!isDraggedNodeAChild) {
                return newNodes;
            }
            const highlightValidClass = 'highlight-valid-parent';
            const highlightInValidClass = 'highlight-invalid-parent';
            // Check intersection only for parent nodes and not needed on regular nodes
            const parentNodes = getParentNodes(resMap, newNodes);
            /* 
                1) When a parent node is dragged, intersections will have its childs too
                2) when a child node is dragged in its parent dimensions, intersection will have its parent node.
                We need to check for second only as first scenario will not happen as this method returns if drag node is not a child,
            */
            const intersections = getIntersectingNodes(nodeDragged, true, parentNodes).filter(
                (obj) => obj.id !== nodeDragged.parentNode
            );
            intersections.forEach((obj) => {
                if (draggedNodeParentType == obj.type) {
                    obj.className = highlightValidClass;
                } else {
                    obj.className = highlightInValidClass;
                }
            });
            return newNodes;
        },
        [resourceDefinitions]
    );

    const nodeChangesHandler = (changes: NodeChange[]) => {
        // based on current react flow only one change type is fired at a time

        // deletes are handled though separate method.
        if (changes.length && changes[0].type == 'remove') {
            const nodeIdsToDel: string[] = [];
            changes.forEach((change) => {
                // in parent child nodes, parent delete will include child ids too
                if (change.type == 'remove') {
                    nodeIdsToDel.push(change.id);
                }
            });
            if (nodeIdsToDel.length > 0) {
                removeElementsById(nodeIdsToDel);
            }
            return;
        }
        if (changes.length && changes[0].type == 'select' && wsEdges.some((obj) => obj.selected)) {
            // As node, edge deletions are fired separately, the delete confirmation dialog will be dismissed for one when both are allowed to delete.
            // so block edge + node selection.
            return;
        }
        /*
            dimension, position, selection
                For subflows, react flow updates the style (width, height) field of parent node if expand parent option is set on child node.
            Since the nodes are from redux store and readonly, causing "Cannot assign to read only property 'style' of object"
            clone nodes before applying changes. This is needed for dimension & position changes.
        */
        let newNodes = applyNodeChanges(changes, cloneDeep(wsNodes));
        if (dragNodeRef.current?.id) {
            newNodes = getChangesForNodeDrag(newNodes, dragNodeRef.current);
        }
        updateElements(newNodes, wsEdges, true);
    };

    const edgeChangesHandler = (changes: EdgeChange[]) => {
        // Note: node deletions will fire edge deletions too automatically by react flow.
        // edge deletes are handled though separate method.
        if (changes.length && changes[0].type == 'remove') {
            const edgeIdsToDel: string[] = [];
            changes.forEach((change) => {
                if (change.type == 'remove') {
                    // check if edge is selected before deleting, as node deletion will first fire edge deletion and then node
                    // and we have a confirmation popup before node delete.
                    const edge = wsEdges.find((obj) => obj.id === change.id);
                    if (edge && edge.selected) {
                        edgeIdsToDel.push(change.id);
                    }
                }
            });
            if (edgeIdsToDel.length > 0) {
                removeElementsById(edgeIdsToDel);
            }
            return;
        }
        if (changes.length && changes[0].type == 'select' && wsNodes.some((obj) => obj.selected)) {
            // As node, edge deletions are fired separately, the delete confirmation dialog will be dismissed for one when both are allowed to delete.
            // so block both edge + node selection.
            return;
        }
        updateElements(wsNodes, applyEdgeChanges(changes, wsEdges), true);
    };

    const generateExecutionConfirmationDialog = () => {
        return (
            <Dialog onClose={() => setConfirmExecution(false)} open={confirmExecution}>
                <DialogTitle id="simple-dialog-title">Confirmation</DialogTitle>
                <DialogContent dividers>
                    <Typography>Are you sure about executing this graph?</Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => executePlan()}>Yes</Button>
                    <Button onClick={() => setConfirmExecution(false)} autoFocus>
                        No
                    </Button>
                </DialogActions>
            </Dialog>
        );
    };

    const submitPlan = () => {
        const graph = computeGraphForPlan(wsNodes, wsEdges);
        showLoadingOverlay(true);
        fetch('/api/v2/graphs/plan', {
            method: 'POST',
            headers: new Headers({
                'Content-Type': 'application/json',
            }),
            body: JSON.stringify(graph),
        })
            .then((response) => {
                if (response.status == 200) {
                    return response.json();
                } else {
                    throw response;
                }
            })
            .then((plan) => {
                if (plan) {
                    setPlanInfo({
                        time: Date.now(),
                        plan: plan,
                        deltaGraph: graph,
                    });
                    setPlanDialog(true);
                    setIsPlanRunAfterGraphUpdate(true);
                }
            })
            .catch((error) => {
                console.error(error);
                error.text().then((errMsg: string) => {
                    setPlanInfo({
                        time: Date.now(),
                        plan: null,
                        deltaGraph: graph,
                        error: errMsg,
                    });
                    setPlanDialog(true);
                    showSnackbar({
                        type: 'error',
                        message: errMsg,
                    });
                });
            })
            .finally(() => {
                showLoadingOverlay(false);
            });
    };

    const executePlan = () => {
        let graph = computeGraphForPlan(wsNodes, wsEdges);
        showLoadingOverlay(true);
        fetch('/api/v2/graphs/execute', {
            method: 'POST',
            headers: new Headers({
                'Content-Type': 'application/json',
            }),
            body: JSON.stringify(graph),
        })
            .then((response) => {
                if (response.status == 200) {
                    return response.json();
                } else {
                    throw Error(response.statusText);
                }
            })
            .then((json) => {
                setPlanInfo({
                    time: Date.now(),
                    executionGraph: json,
                    plan: json.executionPlan,
                    deltaGraph: graph,
                    isExecution: true,
                });
                setConfirmExecution(false);
                setPlanDialog(true);
            })
            .catch((error) => {
                // show toast
                setPlanInfo({
                    time: Date.now(),
                    plan: null,
                    deltaGraph: graph,
                    error: error,
                });
            })
            .finally(() => {
                showLoadingOverlay(false);
            });
    };

    const generatePlanDialog = () => {
        return (
            <Dialog
                onClose={() => setPlanDialog(false)}
                open={planDialog}
                classes={{
                    paper: classes.builderPlanDialogPaper,
                }}
            >
                <DialogContent>
                    <PlanViewer
                        planInfo={planInfo}
                        previousPlanInfo={previousPlanInfo}
                        height="100%"
                        width="100%"
                        dagWidth="100%"
                    />
                </DialogContent>
            </Dialog>
        );
    };

    const resourceRefreshConfirmationDialog = () => {
        return (
            <Dialog open={showStaleResourceDialog && staleResourceIds.length > 0}>
                <DialogTitle id="simple-dialog-title">Confirmation</DialogTitle>
                <DialogContent dividers>
                    <Typography>Below resources in this workspace are stale. Do you want to update them</Typography>
                    <ul>
                        {staleResourceIds.map((id, i) => {
                            return <li key={i}>{id}</li>;
                        })}
                    </ul>
                    {refreshResourceError && <Alert severity="error">{refreshResourceError}</Alert>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={refreshStaleResources}>Yes</Button>
                    <Button onClick={() => setShowStaleResourceDialog(false)}>No</Button>
                </DialogActions>
            </Dialog>
        );
    };

    const refreshStaleResources = async () => {
        if (staleResourceIds.length < 1) {
            return;
        }
        setRefreshResourceError(null);
        let hasError = false;
        staleResourceIds.forEach(async (id) => {
            if (!hasError) {
                await fetchResource(
                    id,
                    (data) => {
                        if (!data || !data.id) {
                            return;
                        }
                        const eleIndex = wsNodes.findIndex((e) => e.id === data.id);
                        if (eleIndex < 0) {
                            return;
                        }
                        // node will have position, type and id
                        const node = {
                            ...wsNodes[eleIndex],
                            data: data,
                        };
                        dispatch(updateOrAddNodeInElements({ tabId: selectedTabId, node: node }));
                    },
                    (err) => {
                        hasError = true;
                        setRefreshResourceError(`Error refreshing resource: ${id} -> ${err.message}`);
                    }
                );
            }
        });
        if (!hasError) {
            // reset stale ids
            dispatch(setStaleResourceIds({ tabId: selectedTabId, staleIds: [] }));
            setShowStaleResourceDialog(false);
        }
    };

    const handleNodeUpdate = (nodeData: TNode) => {
        setIsPlanRunAfterGraphUpdate(false);
        const resMap = resourceDefinitions?.resourceMap ?? {};
        let newNodes = [...wsNodes];
        // before adding to redux, if this is a newly created tmp resource and a parent, then show message
        const index = wsNodes.findIndex((obj) => obj.id == nodeData.id);
        if (index < 0 && nodeData.type) {
            // new tmp node
            newNodes = cloneDeep(newNodes); // as connection needs json update
            newNodes.push(nodeData);
            const isParent = isNodeAParent(resMap, nodeData);
            const isChild = isNodeAChild(resMap, nodeData);
            // for nested parent child scenario, a node can both be a child & parent. So use if blocks instead of ifelse
            if (isChild && nodeData.parentNode) {
                newNodes = connectChildWithParent(newNodes, nodeData);
            }
            /*
                react flow has a limitation that parent nodes should appear before childs in the array. Without this, added child nodes will not be selectable inside parent
                Ex: create a child node first -> then parent -> drag child to parent. This will not work properly without arranging parents first.
                This can also be done just before dragging, but can do it here too everytime a tmp node is created. The same will be done when existing resources are added to workspace
                This should be performed after connection as this method relies on data json
            */
            newNodes = arrangeNodesByParentsFirst(resMap, newNodes);
            if (isParent) {
                showSnackbar({
                    type: 'simple',
                    message: 'Drop any child resources onto this node to connect',
                    props: {
                        autoHideDuration: 5000,
                    },
                });
            }
        } else {
            // existing node
            newNodes[index] = nodeData;
        }
        updateElements(newNodes, wsEdges);
        dispatch(setNodeToEdit(null));
        dispatch(setOpenResourceEditor(false));
    };

    const nodeTypes = useMemo(() => {
        const val: {
            [key: string]: React.FC<NodeProps<INodeData>>;
        } = {};
        if (!resourceDefinitions || !resourceDefinitions.resourceMap) {
            return val;
        }
        for (const [key, value] of Object.entries(resourceDefinitions.resourceMap)) {
            val[key] = (props) => {
                return <CustomNode definition={value} data={props} />;
            };
        }
        return val;
    }, [resourceDefinitions]);

    return (
        <Box
            sx={{ flex: 1 }}
            tabIndex={0}
            onKeyDown={(e) => {
                const ctrlPressed = e.ctrlKey || e.metaKey; // windows + mac
                const key = e.key;
                if (ctrlPressed && key && key.toLowerCase() == 's') {
                    e.preventDefault();
                    e.stopPropagation();
                    onSave();
                } else if (ctrlPressed && key && key.toLowerCase() == 'a') {
                    e.preventDefault();
                    e.stopPropagation();
                    selectAllNodes();
                }
            }}
        >
            <OptionBar
                snapToGrid={snapToGrid}
                setSnapToGrid={setSnapToGrid}
                grid={grid}
                setGrid={setGrid}
                submitPlan={submitPlan}
                enableExecute={isPlanRunAfterGraphUpdate}
                executePlan={() => {
                    setConfirmExecution(true);
                }}
            />
            {openResourceEditor && <ResourceConfigEditor node={nodeToEdit} handleNodeUpdate={handleNodeUpdate} />}
            {planDialog && generatePlanDialog()}
            {confirmExecution && generateExecutionConfirmationDialog()}
            {showStaleResourceDialog && resourceRefreshConfirmationDialog()}
            {
                <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                    <WorkspaceTabs />
                    <Box
                        id="resourceCanvas"
                        sx={{
                            flex: 1,
                        }}
                        ref={reactFlowWrapper}
                    >
                        {/* 
                            We are using controlled flow to have the single source of truth in redux store.
                                Cons: Some node properties like selected, draggable, width, height will be part of redux store and while saving workspace data to local storage, it *may be good to wipe off unwated fields but not a required step.
                                Node dragging has slightly poor performance as every x,y change should go through redux store -> reactflow, but is fine as users don't keep on dragging
                                
                            For uncontrolled flow:
                                node & edge updates (create, edit, delete) should be done through rfInstance that comes with functions for updating the internal state which will be complex and may not have all required functions to manage internal state.
                                Source of truth will be in redux store & react flow and have to make sure they are in sync
                        */}
                        <ReactFlow
                            // don't support edge updates as its complex to maintain.
                            onEdgeUpdate={undefined}
                            onConnect={onConnect}
                            onNodeDoubleClick={(event, node) => {
                                selectNodeAndOpenResourceEditor(node.id);
                            }}
                            nodes={wsNodes}
                            edges={wsEdges}
                            nodeTypes={nodeTypes}
                            snapToGrid={snapToGrid}
                            snapGrid={[15, 15]}
                            onDrop={onDrop}
                            onDragOver={onDragOver}
                            deleteKeyCode="Backspace"
                            minZoom={0.1}
                            maxZoom={6}
                            onNodesChange={nodeChangesHandler}
                            onEdgesChange={edgeChangesHandler}
                            onNodeDragStart={(e, node) => {
                                dragNodeRef.current = node;
                            }}
                            onNodeDragStop={(e, node) => {
                                onNodeDragStop(node);
                                dragNodeRef.current = null;
                            }}
                        >
                            <MiniMap />
                            <Controls />
                            {grid ? <Background /> : <Fragment />}
                        </ReactFlow>
                    </Box>
                </Box>
            }
            <ContextMenu
                onDelete={removeElementsById}
                onEdit={selectNodeAndOpenResourceEditor}
                onUndoDelete={undoDeleteNodesById}
            />
            {showDeleteConfirmation && <DeleteConfirmation />}
        </Box>
    );
};

export default TopologyBuilder;
