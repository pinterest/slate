import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Box, Button, Chip, Stack, Typography } from '@mui/material';
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

const statusDisplay = (status: TExecutionStatus): { label: string; color: 'default' | 'error' | 'success' | 'warning' } => {
    switch (status) {
        case 'SUCCEEDED':
            return { label: 'Approved', color: 'success' };
        case 'FAILED':
            return { label: 'Denied / failed', color: 'error' };
        case 'RUNNING':
            return { label: 'Awaiting approval', color: 'warning' };
        case 'CANCELLED':
            return { label: 'Cancelled', color: 'default' };
        default:
            return { label: 'Not started', color: 'default' };
    }
};

const ExecutionApprovals: React.FC<IExecutionApprovalsProps> = ({ plan }) => {
    const { showSnackbar } = useSnackBar();
    const approvals = useMemo(() => getExecutionApprovals(plan), [plan]);
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

    if (!approvals.length) {
        return (
            <Typography mt={2} align="center" variant="subtitle1">
                No approvals are required for this execution
            </Typography>
        );
    }

    return (
        <Box height="100%" overflow="auto" paddingTop={1}>
            {actionsUnavailable && (
                <Alert severity="warning" sx={{ marginBottom: 1 }}>
                    Approval actions are temporarily unavailable.
                </Alert>
            )}
            <Stack spacing={1}>
                {approvals.map((approval) => {
                    const key = taskKey(approval.processId, approval.taskId);
                    const canAct = actionableTasks.has(key);
                    const isUpdating = updatingTask === key;
                    const status = statusDisplay(approval.status);

                    return (
                        <Box key={key} border={1} borderColor="divider" borderRadius={1} padding={1.5}>
                            <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
                                <Box minWidth={0}>
                                    <Typography variant="subtitle1">
                                        <b>{approval.summary}</b>
                                    </Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Approval group: {approval.group || 'Unknown'}
                                    </Typography>
                                </Box>
                                <Chip size="small" label={status.label} color={status.color} />
                            </Stack>
                            {approval.description && (
                                <Box mt={1}>
                                    <ReactMarkdown>{approval.description}</ReactMarkdown>
                                </Box>
                            )}
                            {canAct && (
                                <Stack direction="row" spacing={1} mt={1}>
                                    <Button
                                        size="small"
                                        variant="outlined"
                                        color="success"
                                        disabled={isUpdating}
                                        onClick={() => updateTask(approval.processId, approval.taskId, 'SUCCEEDED')}
                                    >
                                        Approve
                                    </Button>
                                    <Button
                                        size="small"
                                        variant="outlined"
                                        color="error"
                                        disabled={isUpdating}
                                        onClick={() => updateTask(approval.processId, approval.taskId, 'FAILED')}
                                    >
                                        Deny
                                    </Button>
                                </Stack>
                            )}
                        </Box>
                    );
                })}
            </Stack>
        </Box>
    );
};

export default ExecutionApprovals;
