import React, { useMemo } from 'react';
import { Typography, DialogTitle, Dialog, DialogContent, DialogActions, Button } from '@mui/material';
import { TNode, TEdge } from '../../const/types';
import { isNewNode, isNewEdge } from '../../const/graphHelper';
import { useWorkspaceElementsContext } from './context/WorkspaceElementsContext';

interface IDeleteConfirmationProps {}

const DeleteConfirmation: React.FC<IDeleteConfirmationProps> = ({}) => {
    const { elementsToDelete, showDeleteConfirmation, removeElementsInGraph, resetDeleteDialog } =
        useWorkspaceElementsContext();

    const existingElements: [TNode[], TEdge[]] = useMemo(() => {
        let nodes: TNode[] = [];
        let edges: TEdge[] = [];
        if (elementsToDelete.length == 2) {
            nodes = elementsToDelete[0].filter((obj) => !isNewNode(obj));
            edges = elementsToDelete[1].filter((obj) => !isNewEdge(obj));
        }
        return [nodes, edges];
    }, [elementsToDelete]);

    const existingNodes = existingElements[0];
    const existingEdges = existingElements[1];

    return (
        <Dialog open={showDeleteConfirmation && elementsToDelete.length > 0} onClose={resetDeleteDialog}>
            <DialogTitle id="simple-dialog-title">Confirmation</DialogTitle>
            <DialogContent dividers>
                <Typography>
                    Below resource(s) / edge(s) are existing elements. Do you want to remove them from this workspace
                    view (or) actually delete ?
                </Typography>
                {existingNodes.length > 0 && (
                    <>
                        <Typography>
                            <b>Resources</b>
                        </Typography>
                        <ul>
                            {existingNodes.map((obj, i) => {
                                return <li key={i}>{obj.id}</li>;
                            })}
                        </ul>
                    </>
                )}
                {existingEdges.length > 0 && (
                    <>
                        <Typography>
                            <b>Edges</b>
                        </Typography>
                        <ul>
                            {existingEdges.map((obj, i) => {
                                return <li key={i}>{`${obj.source} --- ${obj.target}`}</li>;
                            })}
                        </ul>
                    </>
                )}
            </DialogContent>
            <DialogActions>
                <Button
                    variant="outlined"
                    onClick={() => {
                        if (elementsToDelete.length == 2) {
                            removeElementsInGraph(elementsToDelete[0], elementsToDelete[1], true);
                        }
                    }}
                >
                    Delete from workspace
                </Button>
                <Button
                    variant="outlined"
                    color="error"
                    onClick={() => {
                        if (elementsToDelete.length == 2) {
                            removeElementsInGraph(elementsToDelete[0], elementsToDelete[1], false);
                        }
                    }}
                >
                    Delete in backend
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default DeleteConfirmation;
