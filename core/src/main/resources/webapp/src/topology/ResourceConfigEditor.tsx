import React, { useEffect, useState } from 'react';
import { Box, Dialog, DialogTitle, DialogContent, Typography, Tabs, Tab } from '@mui/material';
import NewNodePrompt from '../components/builder/NewNodePrompt';
import RenderTools from './RenderTools';
import { TNode, INodeData } from '../const/types';
import { useDispatch, useSelector } from 'react-redux';
import { selectBuilderState, setOpenResourceEditor } from '../store/slice/builder';
import JsonPrettier from '../components/common/JsonPrettier';
import RenderMetrics from './RenderMetrics';

interface IResourceConfigEditorProps {
    node: null | TNode;
    handleNodeUpdate: (node: TNode) => void;
}

const ResourceConfigEditor: React.FC<IResourceConfigEditorProps> = ({ node, handleNodeUpdate }) => {
    const dispatch = useDispatch();
    const { openResourceEditor, resourceDefinitions } = useSelector(selectBuilderState);
    const [tabIndex, setTabIndex] = useState(0);

    useEffect(() => {
        setTabIndex(0);
    }, [node]);

    if (!node || !resourceDefinitions) {
        return null;
    }

    const nodeData = node.data as INodeData;

    const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
        setTabIndex(newValue);
    };

    return (
        <Dialog
            open={openResourceEditor}
            onClose={() => {
                dispatch(setOpenResourceEditor(false));
            }}
            onKeyDown={(e) => {
                // when ctrl+a is pressed, the event propagates to parent and all nodes are being selected. so stop propogation
                e.stopPropagation();
            }}
            fullWidth={true}
            scroll={'paper'}
            maxWidth={'md'}
            PaperProps={{
                style: {
                    height: '80%',
                    padding: 0,
                },
            }}
        >
            <DialogTitle>
                <Typography variant="h6">Resource Id: {nodeData.id}</Typography>
                <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                    <Tabs value={tabIndex} onChange={handleTabChange} aria-label="basic tabs example">
                        <Tab label="Edit Resource" />
                        <Tab label="Tools" />
                        <Tab label="Metrics" />
                        <Tab label="Raw" />
                    </Tabs>
                </Box>
            </DialogTitle>
            <DialogContent>
                {tabIndex === 0 && (
                    <NewNodePrompt
                        handleNodeUpdate={handleNodeUpdate}
                        node={node}
                        resourceDefinition={resourceDefinitions.resourceMap}
                    />
                )}
                {tabIndex === 1 && <RenderTools key="renderTools" resource={node} />}
                {tabIndex === 2 && <RenderMetrics resource={node} />}
                {tabIndex === 3 && <JsonPrettier data={node.data ?? {}} />}
            </DialogContent>
        </Dialog>
    );
};

export default ResourceConfigEditor;
