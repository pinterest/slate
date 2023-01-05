import React, { useState, createContext, useContext } from 'react';
import { useDispatch } from 'react-redux';
import { updateWorkspaceTabElements, setNodeToEdit, setOpenResourceEditor } from '../../../store/slice/builder';
import { cloneDeep } from 'lodash';
import { TNode, TEdge } from '../../../const/types';
import {
    deleteNode,
    deleteEdge,
    undoDeleteNodes,
    isNewEdge,
    isNewNode,
    canDeleteEdge,
} from '../../../const/graphHelper';

interface IWorkspaceElementsContext {
    wsNodes: TNode[];
    wsEdges: TEdge[];
    showDeleteConfirmation: boolean;
    elementsToDelete: [TNode[], TEdge[]];
    isPlanRunAfterGraphUpdate: boolean;
    selectNodeAndOpenResourceEditor: (a: string) => void;
    removeElementsById: (a: string[]) => void;
    undoDeleteNodesById: (a: string[]) => void;
    removeElementsInGraph: (a: TNode[], b: TEdge[], c: boolean) => void;
    updateElements: (a: TNode[], b: TEdge[], c?: boolean) => void;
    resetDeleteDialog: () => void;
    setIsPlanRunAfterGraphUpdate: (a: boolean) => void;
}

export const WorkspaceElementsContext = createContext<IWorkspaceElementsContext>({
    wsNodes: [],
    wsEdges: [],
    showDeleteConfirmation: false,
    elementsToDelete: [[], []],
    isPlanRunAfterGraphUpdate: false,
    selectNodeAndOpenResourceEditor: (a: string) => {},
    removeElementsById: (a: string[]) => {},
    undoDeleteNodesById: (a: string[]) => {},
    removeElementsInGraph: (a: TNode[], b: TEdge[], c: boolean) => {},
    updateElements: (a: TNode[], b: TEdge[], c?: boolean) => {},
    resetDeleteDialog: () => {},
    setIsPlanRunAfterGraphUpdate: (a: boolean) => {},
});

interface IWorkspaceElementsProviderProps {
    wsNodes: TNode[];
    wsEdges: TEdge[];
    tabId: string;
}
export const WorkspaceElementsProvider: React.FC<IWorkspaceElementsProviderProps> = ({
    wsNodes,
    wsEdges,
    tabId,
    children,
}) => {
    const dispatch = useDispatch();
    const [isPlanRunAfterGraphUpdate, setIsPlanRunAfterGraphUpdate] = useState(false);
    const [elementsToDelete, setElementsToDelete] = useState<[TNode[], TEdge[]]>([[], []]);
    const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);

    const updateElements = (nodes: TNode[], edges: TEdge[], canExecuteWithoutPlan: boolean = false) => {
        if (!canExecuteWithoutPlan) {
            setIsPlanRunAfterGraphUpdate(false);
        }
        dispatch(updateWorkspaceTabElements({ tabId, nodes, edges }));
    };

    const selectNodeAndOpenResourceEditor = (id: string) => {
        const index = wsNodes.findIndex((obj) => obj.id === id);
        if (index >= 0) {
            dispatch(setNodeToEdit(wsNodes[index]));
            dispatch(setOpenResourceEditor(true));
        }
    };

    const onRemoveElements = (nodesToRemove: TNode[], edgesToRemove: TEdge[]) => {
        // filter existing nodes & edges
        const hasExistingNodes = nodesToRemove.some((obj) => !isNewNode(obj));
        const hasExistingEdges = edgesToRemove.some((obj) => !isNewEdge(obj));
        if (hasExistingNodes || hasExistingEdges) {
            setElementsToDelete([nodesToRemove, edgesToRemove]);
            setShowDeleteConfirmation(true);
            return;
        } else {
            removeElementsInGraph(nodesToRemove, edgesToRemove, true);
        }
    };

    const removeElementsInGraph = (nodesToRemove: TNode[], edgesToRemove: TEdge[], justRemoveFromView: boolean) => {
        const removeInView = justRemoveFromView;
        // do a clone of all nodes/edges as parent/child node json may also get updated.
        let newNodes = cloneDeep(wsNodes);
        let newEdges = cloneDeep(wsEdges);

        for (let i = 0; i < nodesToRemove.length; i++) {
            // nodes[i] is from redux store. so it cannot be updated directly. So fetch cloned node to allow inline updates on object
            const node = newNodes.find((obj) => obj.id === nodesToRemove[i].id);
            if (node) {
                [newNodes, newEdges] = deleteNode(newNodes, newEdges, node, removeInView, true);
            }
        }
        for (let i = 0; i < edgesToRemove.length; i++) {
            // edges[i] is from redux store. so it cannot be updated directly. So fetch cloned edge to allow inline updates on object
            const edge = newEdges.find((obj) => obj.id === edgesToRemove[i].id);
            // edge may already had been removed as part of node deletion, so verify it exists
            if (edge && canDeleteEdge(newNodes, edge, removeInView)) {
                [newNodes, newEdges] = deleteEdge(newNodes, newEdges, edge, removeInView);
            }
        }
        updateElements(newNodes, newEdges);
        resetDeleteDialog();
    };

    const resetDeleteDialog = () => {
        setElementsToDelete([[], []]);
        setShowDeleteConfirmation(false);
    };

    const undoDeleteNodesById = (ids: string[]) => {
        const newNodes = undoDeleteNodes(wsNodes, ids);
        updateElements(newNodes, wsEdges);
    };

    const removeElementsById = (ids: string[]) => {
        const nodesToRemove = wsNodes.filter((obj) => ids.includes(obj.id));
        const edgesToRemove = wsEdges.filter((obj) => ids.includes(obj.id));
        onRemoveElements(nodesToRemove, edgesToRemove);
    };

    const contextValue: IWorkspaceElementsContext = {
        wsNodes,
        wsEdges,
        showDeleteConfirmation,
        elementsToDelete,
        isPlanRunAfterGraphUpdate,
        selectNodeAndOpenResourceEditor,
        removeElementsById,
        undoDeleteNodesById,
        removeElementsInGraph,
        updateElements,
        resetDeleteDialog,
        setIsPlanRunAfterGraphUpdate,
    };
    return <WorkspaceElementsContext.Provider value={contextValue}>{children}</WorkspaceElementsContext.Provider>;
};

export const useWorkspaceElementsContext = () => useContext(WorkspaceElementsContext);
