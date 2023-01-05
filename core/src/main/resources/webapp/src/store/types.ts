import { IResourceDefinitions, TNode, TEdge } from '../const/types';

export interface IWorkspaceTabData {
    nodes: TNode[];
    edges: TEdge[];
    title: string;
}

export interface IContextMenuData {
    mouseX: number;
    mouseY: number;
    nodeId: string;
}

export interface IBuilderState {
    resourceDefinitions: null | IResourceDefinitions;
    workspaceTabs: Record<string, IWorkspaceTabData>;
    selectedTabId: string;
    workspaceTabCount: number;
    workspaceLoaded: boolean;
    openResourceEditor: boolean;
    nodeToEdit: null | TNode;
    contextMenuData: null | IContextMenuData;
    staleResourceIdsMap: Record<string, string[]>;
}
