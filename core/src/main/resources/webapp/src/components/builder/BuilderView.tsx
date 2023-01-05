import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useSearchParams, useParams } from 'react-router-dom';
import { RootState } from '../../store/index';
import { ReactFlowProvider } from 'reactflow';
import {
    setResourceDefinitions,
    selectBuilderState,
    selectWorkspaceTabData,
    addNodesToWorkspace,
    addWorkspaceTab,
    setNodeToEdit,
    setOpenResourceEditor,
} from '../../store/slice/builder';
import { TNode } from '../../const/types';
import TopologyBuilder from '../../topology/TopologyBuilder';
import { useSnackBar } from '../../context/SnackbarContext';
import { useLoadingSpinner } from '../../context/LoadingSpinnerContext';
import { WorkspaceElementsProvider } from './context/WorkspaceElementsContext';
import Sidebar from './Sidebar';
import { Box } from '@mui/material';
import ReceipeModal from './recipe/RecipeModal';

interface IBuilderViewProps {}

const BuilderView: React.FC<IBuilderViewProps> = () => {
    const [searchParams, setSearchParams] = useSearchParams();
    const { resourceId } = useParams();
    const dispatch = useDispatch();
    const { showSnackbar } = useSnackBar();
    const { showLoadingOverlay } = useLoadingSpinner();
    const { resourceDefinitions, selectedTabId } = useSelector(selectBuilderState);
    const tabData = useSelector((state: RootState) => selectWorkspaceTabData(state, selectedTabId));
    const { nodes, edges } = tabData ?? {};

    useEffect(() => {
        fetchResourceDefinitions();
    }, []);

    const fetchResourceDefinitions = () => {
        showLoadingOverlay(true);
        fetch('/api/v2/resources/definitions')
            .then((response) => response.json())
            .then((json) => {
                dispatch(setResourceDefinitions(json));
                // once resource definitions are fetched, fetch resource if passed through URL.
                fetchResource();
            })
            .catch((error) => {
                console.error('error', error);
                showSnackbar({
                    type: 'error',
                    message: 'Looks like we are disconnected from the server',
                });
            })
            .finally(() => {
                showLoadingOverlay(false);
            });
    };

    const fetchResource = () => {
        if (!resourceId) {
            return;
        }
        fetch('/api/v2/resources/' + resourceId)
            .then((response) => {
                // for invalid resource id, the response will be ok but status code is 204
                if (response.status == 200) {
                    return response.json();
                } else {
                    console.error(response); // added for debugging
                    throw new Error('Resource fetch failed');
                }
            })
            .then((data) => {
                if (data && data.id) {
                    const node: TNode = {
                        position: { x: 0, y: 0 }, // will auto layout
                        type: data.resourceDefinitionClass,
                        data: data,
                        id: data.id,
                    };
                    dispatch(addWorkspaceTab({ makeActive: true }));
                    dispatch(addNodesToWorkspace({ nodes: [node] }));
                    dispatch(setNodeToEdit(node));
                    dispatch(setOpenResourceEditor(true));
                }
            })
            .catch((err) => {
                showSnackbar({
                    type: 'error',
                    message: `Could not find any resource with ID: ${resourceId}`,
                });
            });
    };

    if (!resourceDefinitions) {
        return null;
    }
    // Have react flow provider in parent component, to access react flow store & to programatically trigger its actions
    return (
        <React.Fragment>
            <Box sx={{ display: 'flex', width: '100%', height: '100%' }}>
                <Sidebar />
                <ReactFlowProvider>
                    <WorkspaceElementsProvider wsNodes={nodes ?? []} wsEdges={edges ?? []} tabId={selectedTabId}>
                        <TopologyBuilder />
                    </WorkspaceElementsProvider>
                </ReactFlowProvider>
            </Box>
            <ReceipeModal open={searchParams.has('recipe')} recipeName={searchParams.get('recipe')} />
        </React.Fragment>
    );
};

export default BuilderView;
