import React, { useState, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { Tooltip, Stack, IconButton } from '@mui/material';
import { DeleteOutline, Edit, ContentCopy, AccountTreeOutlined } from '@mui/icons-material';
import { selectBuilderState } from '../../store/slice/builder';
import { TNode } from '../../const/types';
import TraversalModal from './TraversalModal';
import { useWorkspaceElementsContext } from './context/WorkspaceElementsContext';

interface INodeHoverActionsProps {
    hoveredNodeId: string;
}

const NodeHoverActions: React.FC<INodeHoverActionsProps> = ({ hoveredNodeId }) => {
    const [showTraverseModal, setShowTraverseModal] = useState(false);
    const [traverseModalNode, setTraverseModalNode] = useState<null | TNode>(null);
    const {
        wsNodes,
        removeElementsById: onDelete,
        selectNodeAndOpenResourceEditor: onEdit,
        undoDeleteNodesById: onUndoDelete,
    } = useWorkspaceElementsContext();

    const nodesSelected = useMemo(() => {
        return wsNodes.filter((obj) => obj.selected);
    }, [wsNodes]);

    const hoveredNode: TNode | null = useMemo(() => {
        const node = wsNodes.find((obj) => obj.id == hoveredNodeId) as TNode | null;
        return node;
    }, [hoveredNodeId, wsNodes]);

    // Show actions only when the mouse is hovered on node. If multiple nodes are selected, do not show actions
    const actionItemsDom: null | React.ReactElement<typeof Tooltip>[] = useMemo(() => {
        if (!hoveredNode || nodesSelected.length > 1) {
            return null;
        }

        const isDeleted = hoveredNode.data?.deleted === true;
        if (isDeleted) {
            return [
                <Tooltip title="Undo Delete" placement="top" arrow key="undelete">
                    <IconButton
                        style={{ padding: '4px' }}
                        color="primary"
                        onClick={() => {
                            onUndoDelete([hoveredNodeId]);
                        }}
                    >
                        <DeleteOutline fontSize="small" />
                    </IconButton>
                </Tooltip>,
            ];
        }
        return [
            <Tooltip title="Edit Resource" placement="top" arrow key="edit">
                <IconButton
                    style={{ padding: '4px' }}
                    color="primary"
                    onClick={() => {
                        onEdit(hoveredNodeId);
                    }}
                >
                    <Edit fontSize="small" />
                </IconButton>
            </Tooltip>,

            <Tooltip title="Copy ID" placement="top" arrow key="copy">
                <IconButton
                    style={{ padding: '4px' }}
                    color="primary"
                    onClick={() => {
                        navigator.clipboard.writeText(hoveredNodeId ?? '');
                    }}
                >
                    <ContentCopy fontSize="small" />
                </IconButton>
            </Tooltip>,

            <Tooltip title="Fetch Connections" placement="top" arrow key="fetch">
                <IconButton
                    style={{ padding: '4px' }}
                    color="primary"
                    onClick={() => {
                        if (hoveredNode) {
                            setShowTraverseModal(true);
                            setTraverseModalNode(hoveredNode);
                        }
                    }}
                >
                    <AccountTreeOutlined fontSize="small" />
                </IconButton>
            </Tooltip>,

            <Tooltip title="Delete Resource" placement="top" arrow key="delete">
                <IconButton
                    style={{ padding: '4px' }}
                    color="secondary"
                    onClick={() => {
                        onDelete([hoveredNodeId]);
                    }}
                >
                    <DeleteOutline fontSize="small" />
                </IconButton>
            </Tooltip>,
        ];
    }, [hoveredNode, hoveredNodeId, nodesSelected]);

    if (!hoveredNode || !actionItemsDom) {
        return null;
    }

    return (
        <>
            <Stack direction="row" spacing={1}>
                {actionItemsDom}
            </Stack>
            {traverseModalNode && (
                <TraversalModal
                    show={showTraverseModal}
                    node={traverseModalNode}
                    onClose={() => {
                        setShowTraverseModal(false);
                        setTraverseModalNode(null);
                    }}
                />
            )}
        </>
    );
};

export default NodeHoverActions;
