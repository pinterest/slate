import React, { useMemo, useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';
import { Box, Typography, Tooltip } from '@mui/material';
import { Handle, Position, NodeProps } from 'reactflow';
import { IResourceDefinition, INodeData } from '../../const/types';
import { NEW_NODE_ID_PREFIX, NodeWidth } from '../../const/constants';
import { setContextMenuData } from '../../store/slice/builder';
import NodeHoverActions from './NodeHoverActions';

interface ICustomNodeProps {
    definition: IResourceDefinition;
    data: NodeProps<INodeData>;
}

const CustomNode: React.FC<ICustomNodeProps> = ({ definition, data }) => {
    const dispatch = useDispatch();

    const nodeId = data.data.id;

    const HEIGHT = 60; // height of the default resource in relative terms
    const fontSize = '2pt';

    const inputs = Object.keys(definition.requiredInboundEdgeTypes ?? {});
    const inputSpacing = HEIGHT / (inputs.length + 1);
    const outputs = Object.keys(definition.requiredOutboundEdgeTypes ?? {});
    const outputSpacing = HEIGHT / (outputs.length + 1);

    const isSelected = data.selected;

    const isDeleted = data.data.deleted;

    let backgroundColor = nodeId.startsWith(NEW_NODE_ID_PREFIX) ? '#9b9d9e' : '#004B91';
    if (isSelected) {
        backgroundColor = '#2196f3';
    } else if (isDeleted) {
        backgroundColor = '#f44336';
    }
    let border = nodeId.startsWith(NEW_NODE_ID_PREFIX) ? '1px dashed black' : '1px solid black';
    if (isDeleted && isSelected) {
        border = '1px solid red';
    }
    const handleContextMenu = (event: React.MouseEvent) => {
        event.preventDefault();
        // show context menu only when this node is selected
        if (!isSelected) {
            return;
        }
        dispatch(
            setContextMenuData({
                mouseX: event.clientX - 2,
                mouseY: event.clientY - 4,
                nodeId: data.id,
            })
        );
    };

    const divRef = useRef<null | HTMLElement>(null);
    const parentDivRef = useRef<null | HTMLElement>(null);

    const isParentNode = useMemo(() => {
        if (data.data.childResources?.length) {
            return true;
        }
        // for new resources based on resource definition
        if (definition.requiredChildEdgeTypes?.length) {
            return true;
        }
    }, [definition, data.data]);

    useEffect(() => {
        if (divRef.current) {
            parentDivRef.current = divRef.current?.parentElement;
        }
    }, [divRef.current]);

    useEffect(() => {
        if (parentDivRef?.current && isParentNode) {
            parentDivRef.current.style.border = '1px solid grey';
        }
    }, [isParentNode]);

    return (
        <Box
            ref={divRef}
            className="react-flow__node-default"
            sx={{
                /* 
                    styles like border can be directly set on its parent-div too like we are doing for parent resource, but to have white bg space below 
                    this node for childs, styles are applied to this div. Otherwise bgcolor will be applied to the whole area and childs will be invisible.
                */
                display: 'flex',
                justifyContent: 'center',
                backgroundColor: backgroundColor,
                color: 'white',
                '&:hover': {
                    background: isSelected ? undefined : '#98a3d7',

                    '.nodePopoverMenu': {
                        display: 'block',
                    },
                },
                border: border,
                borderRadius: '4px',
                width: isParentNode ? '100%' : `${NodeWidth}px`,
            }}
            onContextMenu={handleContextMenu}
        >
            {inputs.map((k, index) => {
                return (
                    <Handle
                        key={k}
                        id={k}
                        type="target"
                        position={Position.Left}
                        style={{
                            background: '#575',
                            top: inputSpacing * (index + 1),
                        }}
                        isConnectable={isDeleted ? false : undefined}
                    >
                        <Typography
                            style={{
                                fontSize: fontSize,
                                left: '8px',
                                position: 'relative',
                            }}
                        >
                            {k}
                        </Typography>
                    </Handle>
                );
            })}
            <Box sx={{ position: 'absolute', top: '-28px', display: 'none' }} className="nodePopoverMenu">
                <NodeHoverActions hoveredNodeId={nodeId} />
            </Box>
            <Box>
                <Typography style={{ wordBreak: 'break-all', fontSize: '12px' }}>
                    {data.data.id ? data.data.id : 'N/A'}
                </Typography>
                <Tooltip
                    enterDelay={500}
                    leaveDelay={50}
                    title={
                        <div>
                            <Typography>Inputs: {inputs.length != 0 ? inputs : 'none'}</Typography>
                            <Typography>Outputs: {outputs.length != 0 ? outputs : 'none'}</Typography>
                        </div>
                    }
                    arrow
                >
                    <Typography style={{ fontSize: '8pt', color: '#00ffff' }}>{definition.simpleName}</Typography>
                </Tooltip>
            </Box>
            {outputs.map((k, index) => {
                return (
                    <Handle
                        key={k}
                        id={k}
                        type="source"
                        position={Position.Right}
                        style={{
                            background: '#555',
                            top: outputSpacing * (index + 1),
                        }}
                        isConnectable={isDeleted ? false : undefined}
                    >
                        <Typography
                            style={{
                                fontSize: fontSize,
                                float: 'right',
                                left: '-8px',
                                position: 'relative',
                            }}
                        >
                            {k}
                        </Typography>
                    </Handle>
                );
            })}
        </Box>
    );
};

export default CustomNode;
