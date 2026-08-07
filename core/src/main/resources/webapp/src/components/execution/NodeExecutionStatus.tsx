import React, { useEffect, useState } from 'react';
import { Box, Typography, Tab } from '@mui/material';
import { TabContext, TabList, TabPanel } from '@material-ui/lab';
import { IExecutionPlan, TTaskNode } from './types';
import JsonPrettier from '../common/JsonPrettier';
import ExecutionApprovals from './ExecutionApprovals';

interface INodeExecutionStatusProps {
    node: TTaskNode | null;
    plan: Record<string, IExecutionPlan>;
}

const NodeExecutionStatus: React.FC<INodeExecutionStatusProps> = ({ node, plan }) => {
    const [selectedTab, setSelectedTab] = useState('approvals');

    useEffect(() => {
        if (node) {
            setSelectedTab('task_status');
        }
    }, [node?.id]);

    const data = node?.data;
    return (
        <Box justifyContent="center" flex="1" height="100%" width="100%">
            <Typography align="center" variant="subtitle1" style={{ background: '#f6f4f4' }}>
                {data ? (
                    <>
                        <b>{data.taskJson?.label}</b>
                        <span style={{ marginLeft: '2px', fontSize: '14px' }}>
                            ({data.taskJson?.task?.status})
                        </span>
                    </>
                ) : (
                    <b>Execution approvals</b>
                )}
            </Typography>
            <Box justifyContent="center" flex="1" height="100%" width="100%">
                <TabContext value={selectedTab}>
                    <TabList
                        onChange={(_, newVal) => {
                            setSelectedTab(newVal);
                        }}
                    >
                        <Tab label="Approvals" value="approvals" />
                        <Tab label="Task Status" value="task_status" disabled={!data} />
                        <Tab label="Process Context" value="process_context" disabled={!data} />
                    </TabList>
                    <TabPanel value="approvals" style={{ padding: '0px', height: 'calc(100% - 48px)' }}>
                        <ExecutionApprovals plan={plan} />
                    </TabPanel>
                    <TabPanel value="task_status" style={{ padding: '0px', height: '100%' }}>
                        {data && (
                            <>
                                <Box maxHeight="25%" display="flex" flexDirection="column">
                                    <Typography style={{ paddingTop: '8px' }}>Standard Output</Typography>
                                    <JsonPrettier data={data.taskJson?.task?.stdOut} />
                                </Box>
                                <Box maxHeight="25%" display="flex" flexDirection="column">
                                    <Typography style={{ paddingTop: '8px' }}>Error</Typography>
                                    <JsonPrettier data={data.taskJson?.task?.stdErr} />
                                </Box>
                                <Box height="50%" display="flex" flexDirection="column">
                                    <Typography style={{ paddingTop: '8px' }}>Complete JSON</Typography>
                                    <JsonPrettier data={data.taskJson} />
                                </Box>
                            </>
                        )}
                    </TabPanel>
                    <TabPanel
                        value="process_context"
                        style={{
                            padding: '0px',
                            paddingTop: '12px',
                            height: '100%',
                            overflow: 'scroll',
                        }}
                    >
                        {data?.contextJson ? (
                            <JsonPrettier data={data.contextJson as Record<string, unknown>} />
                        ) : (
                            <Typography mt={2} align="center" variant="subtitle1">
                                No process context found
                            </Typography>
                        )}
                    </TabPanel>
                </TabContext>
            </Box>
        </Box>
    );
};

export default NodeExecutionStatus;
