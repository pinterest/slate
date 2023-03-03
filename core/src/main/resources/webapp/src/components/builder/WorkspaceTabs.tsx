import React, { useMemo, useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Tabs, Tab, Box, IconButton, Typography } from '@mui/material';
import {
    selectBuilderState,
    setSelectedTabId,
    addWorkspaceTab,
    deleteWorkspaceTab,
    setWorkspaceTabName,
} from '../../store/slice/builder';
import { Close, AddCircle } from '@mui/icons-material';
import { IWorkspaceTabData } from '../../store/types';
import { useSnackBar } from '../../context/SnackbarContext';

interface IWorkspaceTabsProps {}

const TabName: React.FC<{ tabId: string; tabName: string }> = ({ tabId, tabName }) => {
    const dispatch = useDispatch();
    const { selectedTabId, workspaceTabs } = useSelector(selectBuilderState);
    const { showSnackbar } = useSnackBar();
    const [name, setName] = useState(tabName);
    const [isNameFocused, setIsNamedFocused] = React.useState(false);

    useEffect(() => {
        setIsNamedFocused(false);
    }, [selectedTabId]);

    useEffect(() => {
        setName(tabName);
    }, [tabName]);

    const validateName = () => {
        if (name.toUpperCase() === tabName.toUpperCase()) {
            return true;
        }
        const values = Object.values(workspaceTabs) as IWorkspaceTabData[];
        const obj = values.find((obj: IWorkspaceTabData) => {
            return obj.title.toUpperCase() === name.toUpperCase();
        });
        if (obj) {
            return false;
        }
        return true;
    };

    const handleChange = () => {
        if (name.toUpperCase() !== tabName.toUpperCase()) {
            dispatch(setWorkspaceTabName({ tabId, tabName: name }));
        }
        setIsNamedFocused(false);
    };

    return (
        <Box>
            {!isNameFocused ? (
                <Typography
                    onDoubleClick={() => {
                        setIsNamedFocused(true);
                    }}
                >
                    {tabName}
                </Typography>
            ) : (
                <input
                    autoFocus
                    type="text"
                    value={name}
                    onChange={(event) => {
                        setName(event.target.value);
                    }}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                            if (validateName()) {
                                handleChange();
                            } else {
                                showSnackbar({
                                    type: 'error',
                                    message: 'Tab name already exists',
                                });
                            }
                        }
                    }}
                    onBlur={() => {
                        if (validateName()) {
                            handleChange();
                        }
                        setIsNamedFocused(false);
                    }}
                />
            )}
        </Box>
    );
};

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
            const workspaceName = workspaceTabs[id].title;
            return (
                <Tab
                    label={<TabName tabId={id} tabName={workspaceName} />}
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
                            <Close fontSize="small" />
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
                <AddCircle />
            </IconButton>
        </Box>
    );
};

export default WorkspaceTabs;
