import { v4 as uuidv4 } from 'uuid';
import { createSlice } from '@reduxjs/toolkit';
import { RootState } from '../index';
import { IBuilderState, IWorkspaceTabData } from '../types';
import { addEdgeElementsBetweenNodes, isNewNode, organizeGraphElements } from '../../const/graphHelper';

const defaultTabId = uuidv4();

const initialState: IBuilderState = {
    resourceDefinitions: null,
    workspaceTabs: {},
    // This is a sequence or counter to name a workspace tab with a unique number
    workspaceTabCount: 0,
    selectedTabId: defaultTabId,
    workspaceLoaded: false,
    openResourceEditor: false,
    nodeToEdit: null,
    contextMenuData: null,
    staleResourceIdsMap: {},
};

const builderSlice = createSlice({
    name: 'builder',
    initialState,
    reducers: {
        setResourceDefinitions: (state, action) => {
            state.resourceDefinitions = action.payload;
        },
        addWorkspaceTab: (state, action) => {
            const tabId = uuidv4();
            const makeActive = action?.payload?.makeActive ?? false;
            state.workspaceTabCount += 1;
            state.workspaceTabs = {
                ...state.workspaceTabs,
                [tabId]: {
                    nodes: [],
                    edges: [],
                    title: 'Workspace-' + state.workspaceTabCount,
                },
            };
            if (makeActive) {
                state.selectedTabId = tabId;
            }
        },
        deleteWorkspaceTab: (state, action) => {
            const tabId = action?.payload ?? null;
            if (tabId && state.workspaceTabs[tabId]) {
                const newWorkspaceTabs = { ...state.workspaceTabs };
                delete newWorkspaceTabs[tabId];
                state.workspaceTabs = newWorkspaceTabs;
                // if selectedtab does not exist in new object
                if (!newWorkspaceTabs[state.selectedTabId]) {
                    state.selectedTabId = Object.keys(newWorkspaceTabs)[0];
                }
            }
        },
        setWorkspaceLoaded: (state, action) => {
            state.workspaceLoaded = action.payload;
        },
        setSelectedTabId: (state, action) => {
            state.selectedTabId = action.payload;
        },
        initializeWorkspaceTabs: (state, action) => {
            state.workspaceTabs = action.payload;
        },
        setWorkspaceTabCount: (state, action) => {
            state.workspaceTabCount = action.payload;
        },
        updateWorkspaceTabElements: (state, action) => {
            const { tabId, nodes, edges } = action.payload ?? {};
            if (!tabId) {
                return;
            }
            const tabData = state.workspaceTabs[tabId];
            if (tabData) {
                const newTabData = { ...tabData };
                if (nodes) {
                    newTabData.nodes = nodes;
                }
                if (edges) {
                    newTabData.edges = edges;
                }
                state.workspaceTabs = {
                    ...state.workspaceTabs,
                    [tabId]: newTabData,
                };
            }
        },
        setOpenResourceEditor: (state, action) => {
            state.openResourceEditor = action.payload;
        },
        setNodeToEdit: (state, action) => {
            state.nodeToEdit = action.payload;
        },
        updateOrAddNodeInElements: (state, action) => {
            const { tabId, node, addedFromSearch } = action.payload ?? {};
            if (!tabId || !node || !node.id) {
                return;
            }
            const tabData: IWorkspaceTabData = state.workspaceTabs[tabId];
            if (!tabData) {
                return;
            }
            const nodeIndex = (tabData.nodes || []).findIndex((e) => e.id === node.id);
            let newNodes = [...tabData.nodes];
            let newEdges = [...tabData.edges];
            // if exists, update node
            if (nodeIndex >= 0) {
                // when an existing element is again searched and added, dont add
                if (addedFromSearch) {
                    return;
                }
                newNodes[nodeIndex] = node;
            } else {
                newNodes.push(node);
                // when existing resources are searched & added, add edges if any between existing nodes
                if (!isNewNode(node)) {
                    newEdges = addEdgeElementsBetweenNodes(newNodes, newEdges);
                }
            }
            state.workspaceTabs = {
                ...state.workspaceTabs,
                [tabId]: {
                    ...tabData,
                    nodes: newNodes,
                    edges: newEdges,
                },
            };
        },
        addNodesToWorkspace: (state, action) => {
            const { nodes } = action.payload ?? {};
            if (!nodes || nodes.length === 0) {
                return;
            }
            const tabId = state.selectedTabId;
            const tabData: IWorkspaceTabData = state.workspaceTabs[tabId];
            if (!tabData) {
                return;
            }
            let allNodes = tabData.nodes.concat(nodes);
            const allEdges = addEdgeElementsBetweenNodes(allNodes, tabData.edges, true, true);
            allNodes = organizeGraphElements(allNodes, allEdges);

            state.workspaceTabs = {
                ...state.workspaceTabs,
                [tabId]: {
                    ...tabData,
                    nodes: allNodes,
                    edges: allEdges,
                },
            };
        },
        setContextMenuData: (state, action) => {
            state.contextMenuData = action.payload;
        },
        setStaleResourceIds: (state, action) => {
            const { tabId, staleIds } = action.payload ?? {};
            // dont check for staleIds here as it can be empty during reset.
            if (!tabId) {
                return;
            }
            state.staleResourceIdsMap = {
                ...state.staleResourceIdsMap,
                [tabId]: staleIds,
            };
        },
        setWorkspaceTabName: (state, action) => {
            const { tabId, tabName } = action.payload;
            if (state.workspaceTabs[tabId] && tabName) {
                state.workspaceTabs = { ...state.workspaceTabs };
                state.workspaceTabs[tabId].title = tabName;
            }
        },
    },
});

export const {
    initializeWorkspaceTabs,
    updateWorkspaceTabElements,
    setSelectedTabId,
    setWorkspaceTabCount,
    setWorkspaceLoaded,
    addWorkspaceTab,
    deleteWorkspaceTab,
    setNodeToEdit,
    setOpenResourceEditor,
    updateOrAddNodeInElements,
    addNodesToWorkspace,
    setContextMenuData,
    setResourceDefinitions,
    setStaleResourceIds,
    setWorkspaceTabName,
} = builderSlice.actions;

/* ---------------------------------------- Actions ----------------------------------------- */

export const selectBuilderState = (state: RootState) => state.builder;

export const selectWorkspaceTabData = (state: RootState, tabId: string) => state.builder.workspaceTabs[tabId] ?? {};

// Contains 'actions' and 'reducer'
export default builderSlice;
