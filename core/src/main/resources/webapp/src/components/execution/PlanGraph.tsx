import React, { useState, useEffect, useMemo } from 'react';
import { Box } from '@mui/material';
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
    const [selectedNodeId, setSelectedNodeId] = useState<null | string>(null);
    const [rfNodes, setRFNodes] = useState<TTaskNode[]>([]);
    const [rfEdges, setRFEdges] = useState<TTaskEdge[]>([]);

    useEffect(() => {
        if (!plan) {
            return;
        }
        const [initialNodes, edges] = buildGraphElementsFromPlan(plan);
        const nodes = organizeGraphElements(initialNodes, edges);
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

    const selectedNode = useMemo(
        () => rfNodes.find((node) => node.id === selectedNodeId) ?? null,
        [rfNodes, selectedNodeId]
    );

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
                                    setSelectedNodeId(node.id);
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
                    <NodeExecutionStatus node={selectedNode} plan={plan} />
                </Box>
            )}
        </Box>
    );
};

export default PlanGraph;
