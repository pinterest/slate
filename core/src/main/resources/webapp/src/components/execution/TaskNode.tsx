import React, { useMemo } from 'react';
import { Box } from '@mui/material';
import { Handle, Position, NodeProps } from 'reactflow';
import { ITaskNodeData } from './types';

interface ITaskNodeProps {
    node: NodeProps<ITaskNodeData>;
}

const TaskNode: React.FC<ITaskNodeProps> = ({ node }) => {
    const { taskJson } = (node.data ?? {}) as ITaskNodeData;
    const { task } = taskJson;
    const bgColor = useMemo(() => {
        if (task?.status === 'RUNNING') {
            return '#d6b638';
        } else if (task?.status === 'FAILED') {
            return '#FF5B45';
        } else if (task?.status === 'CANCELLED') {
            return 'black';
        } else if (task?.status === 'SUCCEEDED') {
            return '#00B56F';
        }
        return '#555555';
    }, [node]);

    return (
        <Box
            className="react-flow__node-default"
            sx={{
                color: 'white',
                backgroundColor: bgColor,
                '&:hover': {
                    background: '#98a3d7',
                },
            }}
        >
            <Handle id="target" type="target" position={Position.Left} style={{ background: '#555' }} />
            <div>
                <div style={{ fontSize: '0.8vw' }}>{taskJson.label}</div>
                <div style={{ fontSize: '5pt' }}>{node.id}</div>
            </div>
            <Handle type="source" position={Position.Right} id="succeeded" style={{ background: '#555' }} />
            <Handle type="source" position={Position.Bottom} id="failed" style={{ left: 40, background: '#555' }} />
            <Handle type="source" position={Position.Bottom} id="cancelled" style={{ left: 110, background: '#555' }} />
        </Box>
    );
};

export default TaskNode;
