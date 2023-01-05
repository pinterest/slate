import React, { useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Tabs, Tab, Box, IconButton } from '@mui/material';
import { selectBuilderState, setSelectedTabId, addWorkspaceTab, deleteWorkspaceTab } from '../../store/slice/builder';
import CloseIcon from '@mui/icons-material/Close';
import AddCircleIcon from '@mui/icons-material/AddCircle';

interface IWorkspaceTabsProps {}

const WorkspaceTabs: React.FC<IWorkspaceTabsProps> = () => {
    const dispatch = useDispatch();
    const { selectedTabId, workspaceTabs } = useSelector(selectBuilderState);

    const handleChange = (event: React.ChangeEvent<{}>, newValue: string) => {
        dispatch(setSelectedTabId(newValue));
    };

    const tabDom = useMemo(() => {
        const tabs = Object.keys(workspaceTabs);
        if (tabs.length < 1) {
            return null;
        }
        const list = tabs.map((id: string, index: number) => {
            return (
                <Tab
                    label={workspaceTabs[id].title}
                    value={id}
                    key={index}
                    sx={{
                        textTransform: 'none',
                        minHeight: '56px',
                    }}
                    icon={
                        <IconButton
                            disabled={tabs.length == 1}
                            component="span"
                            aria-label="delete workspace tab"
                            onClick={(evt: React.SyntheticEvent) => {
                                if (evt) {
                                    // to avoid selecting the tab while deleting
                                    evt.stopPropagation();
                                }
                                dispatch(deleteWorkspaceTab(id));
                            }}
                        >
                            <CloseIcon fontSize="small" />
                        </IconButton>
                    }
                    iconPosition="end"
                />
            );
        });
        return (
            <Tabs value={selectedTabId} onChange={handleChange} variant="scrollable" scrollButtons="auto">
                {list}
            </Tabs>
        );
    }, [workspaceTabs, Object.keys(workspaceTabs), selectedTabId]);

    return (
        <Box sx={{ display: 'flex', alignItems: 'center', width: '80%' }}>
            {tabDom}
            <IconButton
                color="primary"
                component="span"
                aria-label="add new workspace tab"
                onClick={() => {
                    dispatch(addWorkspaceTab({ makeActive: true }));
                }}
            >
                <AddCircleIcon />
            </IconButton>
        </Box>
    );
};

export default WorkspaceTabs;
