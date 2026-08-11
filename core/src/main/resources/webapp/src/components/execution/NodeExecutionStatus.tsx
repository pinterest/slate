import React, { useMemo } from 'react';
import { Box, Typography } from '@mui/material';
import { IExecutionPlan, TTaskNode } from './types';
import ExecutionApprovals from './ExecutionApprovals';
import { GROUP_APPROVAL_TASK_DEFINITION_ID } from './approvalUtils';

interface INodeExecutionStatusProps {
    node: TTaskNode | null;
    plan: Record<string, IExecutionPlan>;
}

const NodeExecutionStatus: React.FC<INodeExecutionStatusProps> = ({ node, plan }) => {
    const focusedApprovalKey = useMemo(() => {
        const taskId = node?.data?.taskJson?.label;
        if (!node || !taskId) {
            return null;
        }
        const match = Object.entries(plan ?? {}).find(
            ([planId, planEntry]) =>
                Boolean(planEntry.process?.allTasks?.[taskId]) && node.id === `${planId}-${taskId}`
        );
        const process = match?.[1].process;
        if (!process) {
            return null;
        }
        if (node.data?.taskJson?.task?.taskDefinitionId === GROUP_APPROVAL_TASK_DEFINITION_ID) {
            return `${process.processId}:${taskId}`;
        }
        const approvalTaskId = Object.entries(process.allTasks ?? {}).find(
            ([, task]) => task.taskDefinitionId === GROUP_APPROVAL_TASK_DEFINITION_ID
        )?.[0];
        return approvalTaskId ? `${process.processId}:${approvalTaskId}` : null;
    }, [node?.id, plan]);

    return (
        <Box justifyContent="center" flex="1" height="100%" width="100%">
            <Typography align="center" variant="subtitle1" style={{ background: '#f6f4f4' }}>
                <b>Execution approvals</b>
            </Typography>
            <Box justifyContent="center" flex="1" height="100%" width="100%">
                <ExecutionApprovals plan={plan} focusedApprovalKey={focusedApprovalKey} />
            </Box>
        </Box>
    );
};

export default NodeExecutionStatus;
