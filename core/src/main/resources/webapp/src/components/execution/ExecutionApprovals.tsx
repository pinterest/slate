import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, alpha, Box, Button, Chip, Stack, Typography, useTheme } from '@mui/material';
import ReactMarkdown from 'react-markdown';
import { useSnackBar } from '../../context/SnackbarContext';
import { ITask } from '../task/types';
import { IExecutionPlan, TExecutionStatus } from './types';
import { getExecutionApprovals } from './approvalUtils';

const TASK_REFRESH_FREQUENCY = 5000;

interface IExecutionApprovalsProps {
    plan: Record<string, IExecutionPlan>;
}

const taskKey = (processId: string, taskId: string): string => `${processId}:${taskId}`;

const statusDisplay = (
    status: TExecutionStatus,
    blocked: boolean
): { label: string; color: 'default' | 'error' | 'info' | 'success' | 'warning' } => {
    if (blocked) {
        return { label: 'Blocked', color: 'warning' };
    }
    switch (status) {
        case 'SUCCEEDED':
            return { label: 'Approved', color: 'success' };
        case 'FAILED':
            return { label: 'Denied / failed', color: 'error' };
        case 'RUNNING':
            return { label: 'Awaiting approval', color: 'info' };
        case 'CANCELLED':
            return { label: 'Cancelled', color: 'default' };
        default:
            return { label: 'Not started', color: 'default' };
    }
};

const ExecutionApprovals: React.FC<IExecutionApprovalsProps> = ({ plan }) => {
    const { showSnackbar } = useSnackBar();
    const theme = useTheme();
    const approvals = useMemo(() => getExecutionApprovals(plan), [plan]);
    const reviewPrompt = `/internal/summarize-slate-approvals ${window.location.href}`;
    const blockedCount = approvals.filter(
        (approval) => approval.status === 'NOT_STARTED' && approval.blockedBy.length > 0
    ).length;
    const awaitingCount = approvals.filter(
        (approval) => approval.status === 'RUNNING' && approval.blockedBy.length === 0
    ).length;
    const approvedCount = approvals.filter((approval) => approval.status === 'SUCCEEDED').length;
    const [actionableTasks, setActionableTasks] = useState<Set<string>>(new Set());
    const [actionsUnavailable, setActionsUnavailable] = useState(false);
    const [updatingTask, setUpdatingTask] = useState<null | string>(null);

    const fetchActionableTasks = useCallback(() => {
        fetch('/api/v2/tasks/mygrouptasks', {
            method: 'GET',
        })
            .then((response) => {
                if (!response.ok) {
                    throw Error(response.statusText);
                }
                return response.json();
            })
            .then((tasks: ITask[]) => {
                setActionableTasks(
                    new Set(
                        tasks
                            .filter((task) => task.taskType === 'APPROVAL')
                            .map((task) => taskKey(task.processId, task.taskId))
                    )
                );
                setActionsUnavailable(false);
            })
            .catch((error) => {
                console.error('Unable to load approval permissions', error);
                setActionableTasks(new Set());
                setActionsUnavailable(true);
            });
    }, []);

    useEffect(() => {
        fetchActionableTasks();
        const intervalId = setInterval(fetchActionableTasks, TASK_REFRESH_FREQUENCY);
        return () => clearInterval(intervalId);
    }, [fetchActionableTasks]);

    const updateTask = (processId: string, taskId: string, status: 'SUCCEEDED' | 'FAILED') => {
        const key = taskKey(processId, taskId);
        setUpdatingTask(key);
        fetch(
            `/api/v2/tasks/${encodeURIComponent(processId)}/${encodeURIComponent(taskId)}/${encodeURIComponent(status)}`,
            {
                method: 'PUT',
            }
        )
            .then((response) => {
                if (!response.ok) {
                    throw Error(response.statusText);
                }
                setActionableTasks((current) => {
                    const next = new Set(current);
                    next.delete(key);
                    return next;
                });
                showSnackbar({
                    type: 'success',
                    message: status === 'SUCCEEDED' ? 'Approval submitted' : 'Denial submitted',
                });
            })
            .catch((error) => {
                console.error('Unable to update approval', error);
                showSnackbar({
                    type: 'error',
                    message: 'Unable to update approval',
                });
                fetchActionableTasks();
            })
            .finally(() => {
                setUpdatingTask(null);
            });
    };

    return (
        <Box height="100%" overflow="auto" paddingTop={1}>
            <Box display="flex" justifyContent="flex-end" marginBottom={1}>
                <Button
                    className="helix-ultra-button"
                    data-button-name="Review Slate Changes"
                    data-pre-prompt={reviewPrompt}
                    data-initial-placeholder-text="Ask about this Slate execution"
                    data-create-new-chat="true"
                    variant="outlined"
                    color="primary"
                    size="small"
                    title="Open Helix Ultra to review this execution"
                    sx={{ textTransform: 'none' }}
                >
                    Review changes
                </Button>
            </Box>
            {actionsUnavailable && (
                <Alert severity="warning" sx={{ marginBottom: 1 }}>
                    Approval actions are temporarily unavailable.
                </Alert>
            )}
            {!approvals.length ? (
                <Typography mt={2} align="center" variant="subtitle1">
                    No approvals are required for this execution
                </Typography>
            ) : (
                <>
                    <Typography variant="body2" color="text.secondary" marginBottom={1}>
                        {awaitingCount} awaiting · {blockedCount} blocked · {approvedCount} approved
                    </Typography>
                    <Stack spacing={1}>
                        {approvals.map((approval) => {
                            const key = taskKey(approval.processId, approval.taskId);
                            const canAct = actionableTasks.has(key);
                            const isUpdating = updatingTask === key;
                            const isBlocked = approval.status === 'NOT_STARTED' && approval.blockedBy.length > 0;
                            const status = statusDisplay(approval.status, isBlocked);

                            return (
                                <Box key={key} border={1} borderColor="divider" borderRadius={1} padding={1.5}>
                                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
                                        <Box minWidth={0}>
                                            <Typography variant="subtitle1" sx={{ overflowWrap: 'anywhere' }}>
                                                <b>{approval.summary}</b>
                                            </Typography>
                                            <Typography variant="body2" color="text.secondary">
                                                Stage {approval.dependencyLevel + 1} · Approval group:{' '}
                                                {approval.group || 'Unknown'}
                                            </Typography>
                                        </Box>
                                        <Chip size="small" label={status.label} color={status.color} />
                                    </Stack>
                                    {isBlocked && (
                                        <Box
                                            mt={1}
                                            padding={1}
                                            bgcolor={alpha(theme.palette.warning.main, 0.12)}
                                            borderLeft={2}
                                            borderColor="warning.main"
                                        >
                                            <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                                                <b>Waiting for:</b> {approval.blockedBy[0].label}
                                            </Typography>
                                            {approval.blockedBy.length > 1 && (
                                                <Typography variant="caption" color="text.secondary">
                                                    +{approval.blockedBy.length - 1} additional direct blocker
                                                    {approval.blockedBy.length > 2 ? 's' : ''}
                                                </Typography>
                                            )}
                                        </Box>
                                    )}
                                    {approval.description && (
                                        <Box mt={1}>
                                            <ReactMarkdown>{approval.description}</ReactMarkdown>
                                        </Box>
                                    )}
                                    {canAct && !isBlocked && (
                                        <Stack direction="row" spacing={1} mt={1}>
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                color="success"
                                                disabled={isUpdating}
                                                onClick={() =>
                                                    updateTask(approval.processId, approval.taskId, 'SUCCEEDED')
                                                }
                                            >
                                                Approve
                                            </Button>
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                color="error"
                                                disabled={isUpdating}
                                                onClick={() =>
                                                    updateTask(approval.processId, approval.taskId, 'FAILED')
                                                }
                                            >
                                                Deny
                                            </Button>
                                        </Stack>
                                    )}
                                </Box>
                            );
                        })}
                    </Stack>
                </>
            )}
        </Box>
    );
};

export default ExecutionApprovals;
