import React, { useState, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Typography, Menu, MenuItem, ListItemIcon } from '@mui/material';
import { DeleteOutline, Edit, ContentCopy, AccountTreeOutlined } from '@mui/icons-material';
import { selectBuilderState, setContextMenuData } from '../../store/slice/builder';
import { useWorkspaceElementsContext } from './context/WorkspaceElementsContext';
import { TNode } from '../../const/types';
import TraversalModal from './TraversalModal';

interface IContextMenuProps {
    onDelete: (ids: string[]) => void;
    onEdit: (id: string) => void;
    onUndoDelete: (ids: string[]) => void;
}

// Having common context menu instead of having it in each node, will be useful for multi node selection.
const ContextMenu: React.FC<IContextMenuProps> = ({ onDelete, onEdit, onUndoDelete }) => {
    const dispatch = useDispatch();
    const { contextMenuData } = useSelector(selectBuilderState);
    const { wsNodes } = useWorkspaceElementsContext();
    const [showTraverseModal, setShowTraverseModal] = useState(false);
    const [traverseModalNode, setTraverseModalNode] = useState<null | TNode>(null);

    const nodesSelected = useMemo(() => {
        return wsNodes.filter((obj) => obj.selected);
    }, [wsNodes]);

    const { nodeId, mouseX, mouseY } = contextMenuData ?? {};

    const handleMenuClose = () => {
        dispatch(setContextMenuData(null));
    };

    const menuItemsDom = useMemo(() => {
        if (!nodesSelected || nodesSelected.length < 1) {
            return null;
        }
        let menuItems: React.ReactElement<typeof MenuItem>[] = [];
        const hasDeletedNode = nodesSelected.some((obj) => obj.data?.deleted === true);
        const allDeletedNodes = nodesSelected.every((obj) => obj.data?.deleted === true);
        if (allDeletedNodes) {
            menuItems.push(
                <MenuItem
                    key="undel"
                    onClick={() => {
                        handleMenuClose();
                        onUndoDelete(nodesSelected.map((obj) => obj.id));
                    }}
                >
                    <ListItemIcon>
                        <DeleteOutline fontSize="small" />
                    </ListItemIcon>
                    <Typography variant="inherit">Undo Delete</Typography>
                </MenuItem>
            );
            return menuItems;
        }
        let disableAllActions = false;
        if (hasDeletedNode) {
            disableAllActions = true;
        }
        menuItems = [
            <MenuItem
                key="edit"
                disabled={disableAllActions || nodesSelected.length > 1 ? true : false}
                onClick={() => {
                    handleMenuClose();
                    nodeId && onEdit(nodeId);
                }}
            >
                <ListItemIcon>
                    <Edit fontSize="small" />
                </ListItemIcon>
                <Typography variant="inherit">Edit Resource</Typography>
            </MenuItem>,
            <MenuItem
                key="del"
                disabled={disableAllActions}
                onClick={() => {
                    handleMenuClose();
                    onDelete((nodesSelected ?? []).map((obj) => obj.id));
                }}
            >
                <ListItemIcon>
                    <DeleteOutline fontSize="small" />
                </ListItemIcon>
                <Typography variant="inherit">Delete</Typography>
            </MenuItem>,
            <MenuItem
                key="copy"
                disabled={disableAllActions || nodesSelected.length > 1 ? true : false}
                onClick={() => {
                    handleMenuClose();
                    navigator.clipboard.writeText(nodeId ?? '');
                }}
            >
                <ListItemIcon>
                    <ContentCopy fontSize="small" />
                </ListItemIcon>
                <Typography variant="inherit">Copy Resource ID</Typography>
            </MenuItem>,
            <MenuItem
                key="traverse"
                disabled={disableAllActions || nodesSelected.length > 1 ? true : false}
                onClick={() => {
                    if (nodesSelected.length == 1) {
                        setShowTraverseModal(true);
                        setTraverseModalNode(nodesSelected[0]);
                    }
                }}
            >
                <ListItemIcon>
                    <AccountTreeOutlined fontSize="small" />
                </ListItemIcon>
                <Typography variant="inherit">Fetch Connections</Typography>
            </MenuItem>,
        ];
        return menuItems;
    }, [nodeId, nodesSelected?.length, nodesSelected]);

    if (!nodeId || !mouseX || !mouseY || !menuItemsDom) {
        return null;
    }

    return (
        <>
            <Menu
                open={true}
                onClose={handleMenuClose}
                anchorReference="anchorPosition"
                anchorPosition={{ top: mouseY, left: mouseX }}
            >
                {menuItemsDom}
            </Menu>
            {traverseModalNode && (
                <TraversalModal
                    show={showTraverseModal}
                    node={traverseModalNode}
                    onClose={() => {
                        setShowTraverseModal(false);
                        setTraverseModalNode(null);
                        handleMenuClose();
                    }}
                />
            )}
        </>
    );
};

export default ContextMenu;
