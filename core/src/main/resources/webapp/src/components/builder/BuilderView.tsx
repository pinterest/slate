import React, { useEffect, useRef } from 'react';
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
import {
    applyResourceDefaults,
    decodeGraphFragment,
    hasGraphFragment,
    nodesFromResourceMap,
} from '../../const/graphHandoff';
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
    const { resourceDefinitions, selectedTabId, workspaceLoaded } = useSelector(selectBuilderState);
    const hydratedFromFragmentRef = useRef(false);
    const tabData = useSelector((state: RootState) => selectWorkspaceTabData(state, selectedTabId));
    const { nodes, edges } = tabData ?? {};

    useEffect(() => {
        fetchResourceDefinitions();
    }, []);

    // Hydrate a create-only graph handed off via the URL fragment (#graph=...).
    // Gated on workspaceLoaded so we run *after* TopologyBuilder's onRestore has
    // set up a consistent tabs map + selectedTabId; running during mount races
    // that restore and the injected nodes get dropped. Also gated on
    // resourceDefinitions so applyResourceDefaults has the schemas it needs to
    // fill defaults — otherwise the injected nodes would miss form defaults (e.g.
    // a topic's customConfigs) and fail Plan.
    useEffect(() => {
        if (!workspaceLoaded || !resourceDefinitions || hydratedFromFragmentRef.current) {
            return;
        }
        hydratedFromFragmentRef.current = true;
        hydrateFromFragment();
    }, [workspaceLoaded, resourceDefinitions]);

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

    const hydrateFromFragment = async () => {
        // Nothing to do (and nothing to scrub) if there's no #graph= payload.
        if (!hasGraphFragment(window.location.hash)) {
            return;
        }
        const map = await decodeGraphFragment(window.location.hash);
        if (map) {
            // Populate schema defaults (mirrors the Builder form) so a handoff
            // node matches a form-built one; otherwise fields the backend Plan
            // dereferences (e.g. a topic's customConfigs) can be absent.
            const hydratedMap = applyResourceDefaults(map, resourceDefinitions);
            const nodes = nodesFromResourceMap(hydratedMap);
            dispatch(addWorkspaceTab({ makeActive: true }));
            dispatch(addNodesToWorkspace({ nodes }));
            dispatch(setNodeToEdit(null));
            dispatch(setOpenResourceEditor(false));
            showSnackbar({
                type: 'success',
                message: 'Loaded proposed graph — review, run Plan, then Execute.',
            });
        } else {
            // A #graph= payload was present but invalid (malformed, or not a
            // create-only graph). No-op the hydration, but tell the user why
            // nothing loaded, and still scrub it below.
            console.warn('Ignoring invalid #graph= handoff payload.');
            showSnackbar({
                type: 'error',
                message: 'That graph link was invalid or unsupported — nothing was loaded.',
            });
        }

        // Remove the payload from the address bar / current history entry —
        // whether it hydrated or was rejected — so it doesn't re-hydrate on
        // re-render, linger in the URL, or get re-shared. Preserve the existing
        // history state (React Router keeps its location key/idx there) so we
        // only drop the fragment, not the router metadata.
        window.history.replaceState(
            window.history.state,
            '',
            window.location.pathname + window.location.search
        );
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
