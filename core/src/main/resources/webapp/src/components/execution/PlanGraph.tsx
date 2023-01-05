import React, { useState, useEffect, useMemo } from 'react';
import { Box, Typography } from '@mui/material';
import ReactFlow, { ReactFlowProvider, Background, NodeProps } from 'reactflow';
import { IExecutionPlan, TTaskNode, ITaskNodeData, TTaskEdge } from './types';
import NodeExecutionStatus from './NodeExecutionStatus';
import TaskNode from './TaskNode';
import { useStyles } from '../../AppStyles';
import { buildGraphElementsFromPlan, organizeGraphElements } from '../../const/graphHelper';

interface IPlanGraphProps {
    plan: Record<string, IExecutionPlan>;
    showExecStatus?: boolean;
    width?: string;
    height?: string;
    dagWidth?: string;
}

const PlanGraph: React.FC<IPlanGraphProps> = ({ plan, width, height, dagWidth, showExecStatus = false }) => {
    const classes = useStyles();
    const [selectedNode, setSelectedNode] = useState<null | TTaskNode>(null);
    const [rfNodes, setRFNodes] = useState<TTaskNode[]>([]);
    const [rfEdges, setRFEdges] = useState<TTaskEdge[]>([]);

    useEffect(() => {
        if (!plan) {
            return;
        }
        let [nodes, edges] = buildGraphElementsFromPlan(plan);
        nodes = organizeGraphElements(nodes, edges);
        setRFNodes(nodes ?? []);
        setRFEdges(edges ?? []);
    }, [plan]);

    const nodeTypes: Record<string, React.FC<NodeProps<ITaskNodeData>>> = useMemo(() => {
        return {
            taskNode: (props: NodeProps<ITaskNodeData>) => {
                return <TaskNode node={props} />;
            },
        };
    }, []);

    return (
        <Box display="flex" flex="1" flexDirection="row" width={width} height={height}>
            <Box className={classes.graphBorder} flex={showExecStatus ? undefined : '1'}>
                <ReactFlowProvider>
                    <div
                        style={{
                            width: dagWidth,
                            height: height,
                            display: 'flex',
                        }}
                    >
                        <ReactFlow
                            nodesDraggable={false}
                            nodesConnectable={false}
                            nodes={rfNodes}
                            edges={rfEdges}
                            nodeTypes={nodeTypes}
                            minZoom={0.5}
                            maxZoom={4}
                            onNodeClick={(e, node) => {
                                if (showExecStatus) {
                                    setSelectedNode(node);
                                }
                            }}
                        >
                            <Background />
                        </ReactFlow>
                    </div>
                </ReactFlowProvider>
            </Box>
            {showExecStatus && (
                <Box flex="1" paddingLeft={2} height="100%" width="100%" overflow="hidden">
                    {selectedNode ? (
                        <NodeExecutionStatus node={selectedNode} />
                    ) : (
                        <Box justifyContent="center" flex="1">
                            <Typography align="center" variant="h6" component="h2">
                                Select a node to view the task status
                            </Typography>
                        </Box>
                    )}
                </Box>
            )}
        </Box>
    );
};

export default PlanGraph;
