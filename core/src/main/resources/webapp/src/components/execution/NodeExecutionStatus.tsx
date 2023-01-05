import React, { useState } from 'react';
import { Box, Typography, Tab } from '@mui/material';
import { TabContext, TabList, TabPanel } from '@material-ui/lab';
import { TTaskNode } from './types';
import JsonPrettier from '../common/JsonPrettier';

interface INodeExecutionStatusProps {
    node: TTaskNode;
}

const NodeExecutionStatus: React.FC<INodeExecutionStatusProps> = ({ node }) => {
    const [selectedTab, setSelectedTab] = useState('1');

    const { data } = node;
    if (!data) {
        return null;
    }
    return (
        <Box justifyContent="center" flex="1" height="100%" width="100%">
            <Typography align="center" variant="subtitle1" style={{ background: '#f6f4f4' }}>
                <b>{data.taskJson?.label}</b>
                <span style={{ marginLeft: '2px', fontSize: '14px' }}>({data.taskJson?.task?.status})</span>
            </Typography>
            <Box justifyContent="center" flex="1" height="100%" width="100%">
                <TabContext value={selectedTab}>
                    <TabList
                        onChange={(_, newVal) => {
                            setSelectedTab(newVal);
                        }}
                    >
                        <Tab label="Task Status" value="1" />
                        <Tab label="Process Context" value="2" />
                    </TabList>
                    <TabPanel value="1" style={{ padding: '0px', height: '100%' }}>
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
                    </TabPanel>
                    <TabPanel
                        value="2"
                        style={{
                            padding: '0px',
                            paddingTop: '12px',
                            height: '100%',
                            overflow: 'scroll',
                        }}
                    >
                        {data.contextJson ? (
                            <JsonPrettier data={data.contextJson as Object} />
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
