import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    Alert,
    alpha,
    Box,
    Button,
    Chip,
    Collapse,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    Typography,
    useTheme,
} from '@mui/material';
import { KeyboardArrowDown, KeyboardArrowUp } from '@mui/icons-material';
import ReactMarkdown from 'react-markdown';
import { useSnackBar } from '../../context/SnackbarContext';
import { ITask } from '../task/types';
import { IExecutionPlan, IExecutionTaskJson, TExecutionStatus } from './types';
import {
    formatApprovalTimestamp,
    formatApprovalWait,
    getExecutionApprovals,
} from './approvalUtils';
import { ResourceJsonDiff } from './ExecPlanJsonDiff';
import ApprovalReviewButton from './ApprovalReviewButton';
import AdvancedTaskExplorer, { IApprovalTaskDetails } from './AdvancedTaskExplorer';

const TASK_REFRESH_FREQUENCY = 5000;

interface IExecutionApprovalsProps {
    plan: Record<string, IExecutionPlan>;
    focusedApprovalKey?: string | null;
    focusedTaskKey?: string | null;
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

type TApprovalDialog =
    | { kind: 'description'; summary: string; description: string }
    | {
          kind: 'advanced';
          summary: string;
          tasks: IApprovalTaskDetails[];
          initialTaskId: string;
      };

const orderedTaskIds = (
    allTasks: Record<string, IExecutionTaskJson>,
    startTaskId: string
): string[] => {
    const ordered: string[] = [];
    const visited = new Set<string>();
    const queue = startTaskId && allTasks[startTaskId] ? [startTaskId] : [];

    while (queue.length) {
        const taskId = queue.shift();
        if (!taskId || visited.has(taskId) || !allTasks[taskId]) {
            continue;
        }
        visited.add(taskId);
        ordered.push(taskId);
        Object.values(allTasks[taskId].nextPointers ?? {})
            .flat()
            .forEach((nextTaskId) => {
                if (!visited.has(nextTaskId)) {
                    queue.push(nextTaskId);
                }
            });
    }

    Object.keys(allTasks).forEach((taskId) => {
        if (!visited.has(taskId)) {
            ordered.push(taskId);
        }
    });
    return ordered;
};

const ExecutionApprovals: React.FC<IExecutionApprovalsProps> = ({
    plan,
    focusedApprovalKey,
    focusedTaskKey,
}) => {
    const { showSnackbar } = useSnackBar();
    const theme = useTheme();
    const approvals = useMemo(() => getExecutionApprovals(plan), [plan]);
    const taskDetailsByProcessId = useMemo(() => {
        const byProcessId = new Map<string, IApprovalTaskDetails[]>();
        Object.values(plan).forEach((planEntry) => {
            const process = planEntry.process;
            if (!process) {
                return;
            }
            byProcessId.set(
                process.processId,
                orderedTaskIds(process.allTasks, process.startTaskId).map((taskId) => ({
                    taskId,
                    task: process.allTasks[taskId],
                    context: process.processContext?.[taskId] ?? null,
                }))
            );
        });
        return byProcessId;
    }, [plan]);
    const executionUrl = useMemo(() => {
        const executionId = Object.values(plan).find((planEntry) => planEntry.process)?.process?.executionId;
        return executionId ? `${window.location.origin}/executions/${executionId}` : undefined;
    }, [plan]);
    const resourcePlans = useMemo(() => {
        const byResourceId = new Map<string, IExecutionPlan>();
        Object.entries(plan).forEach(([planId, planEntry]) => {
            byResourceId.set(planId, planEntry);
            if (planEntry.proposedResource?.id) {
                byResourceId.set(planEntry.proposedResource.id, planEntry);
            }
        });
        return byResourceId;
    }, [plan]);
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
    const [expandedChanges, setExpandedChanges] = useState<Set<string>>(new Set());
    const [selectedContext, setSelectedContext] = useState<null | TApprovalDialog>(null);
    const focusedCardRef = useRef<null | HTMLDivElement>(null);

    useEffect(() => {
        if (focusedApprovalKey && focusedCardRef.current) {
            focusedCardRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }, [focusedApprovalKey]);

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

    const toggleChanges = (key: string) => {
        setExpandedChanges((current) => {
            const next = new Set(current);
            if (next.has(key)) {
                next.delete(key);
            } else {
                next.add(key);
            }
            return next;
        });
    };

    return (
        <Box
            height="100%"
            overflow="auto"
            paddingTop={1}
            paddingRight={2}
            sx={{ scrollbarGutter: 'stable' }}
        >
            {actionsUnavailable && (
                <Alert severity="warning" sx={{ marginBottom: 1 }}>
                    Approval actions are temporarily unavailable.
                </Alert>
            )}
            {!approvals.length ? (
                <>
                    <Box display="flex" justifyContent="flex-end">
                        <ApprovalReviewButton executionUrl={executionUrl} />
                    </Box>
                    <Typography mt={2} align="center" variant="subtitle1">
                        No approvals are required for this execution
                    </Typography>
                </>
            ) : (
                <>
                    <Stack
                        direction="row"
                        justifyContent="space-between"
                        alignItems="center"
                        spacing={1}
                        marginBottom={1}
                    >
                        <Typography variant="body2" color="text.secondary">
                            {awaitingCount} awaiting · {blockedCount} blocked · {approvedCount} approved
                        </Typography>
                        <ApprovalReviewButton executionUrl={executionUrl} />
                    </Stack>
                    <Stack spacing={1}>
                        {approvals.map((approval) => {
                            const key = taskKey(approval.processId, approval.taskId);
                            const canAct = actionableTasks.has(key);
                            const isUpdating = updatingTask === key;
                            const isBlocked = approval.status === 'NOT_STARTED' && approval.blockedBy.length > 0;
                            const status = statusDisplay(approval.status, isBlocked);
                            const resourcePlan = resourcePlans.get(approval.resourceId);
                            const changesExpanded = expandedChanges.has(key);
                            const isAwaiting = approval.status === 'RUNNING' && !isBlocked;
                            const waitingLabel = isAwaiting ? formatApprovalWait(approval.startTimeMs, Date.now()) : '';
                            const approvedTimestamp =
                                approval.status === 'SUCCEEDED'
                                    ? formatApprovalTimestamp(approval.endTimeMs)
                                    : '';
                            const isFocused = focusedApprovalKey === key;
                            const processTasks = taskDetailsByProcessId.get(approval.processId) ?? [];
                            const focusedTaskId =
                                isFocused && focusedTaskKey?.startsWith(`${approval.processId}:`)
                                    ? focusedTaskKey.slice(approval.processId.length + 1)
                                    : null;
                            const initialTaskId =
                                focusedTaskId && processTasks.some(({ taskId }) => taskId === focusedTaskId)
                                    ? focusedTaskId
                                    : approval.taskId;

                            return (
                                <Box
                                    key={key}
                                    ref={isFocused ? focusedCardRef : undefined}
                                    border={1}
                                    borderColor={isFocused ? 'primary.main' : 'divider'}
                                    bgcolor={isFocused ? alpha(theme.palette.primary.main, 0.06) : undefined}
                                    borderRadius={1}
                                    padding={1.5}
                                    minWidth={0}
                                    overflow="hidden"
                                >
                                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
                                        <Box minWidth={0}>
                                            <Typography
                                                variant="subtitle1"
                                                title={approval.summary}
                                                sx={{ overflowWrap: 'anywhere' }}
                                            >
                                                {approval.summary}
                                            </Typography>
                                            <Typography variant="body2">
                                                Approval group: <b>{approval.group || 'Unknown'}</b>
                                            </Typography>
                                        </Box>
                                        <Stack alignItems="flex-end" spacing={0.5} sx={{ flexShrink: 0 }}>
                                            <Chip size="small" label={status.label} color={status.color} />
                                            {waitingLabel ? (
                                                <Typography
                                                    variant="caption"
                                                    color="text.secondary"
                                                    textAlign="right"
                                                >
                                                    {waitingLabel}
                                                </Typography>
                                            ) : (
                                                approvedTimestamp && (
                                                    <Typography
                                                        variant="caption"
                                                        color="text.secondary"
                                                        textAlign="right"
                                                    >
                                                        Approved {approvedTimestamp}
                                                    </Typography>
                                                )
                                            )}
                                        </Stack>
                                    </Stack>
                                    {isBlocked && (
                                        <Box
                                            mt={1}
                                            padding={1}
                                            bgcolor={alpha(theme.palette.warning.main, 0.12)}
                                            borderLeft={2}
                                            borderColor="warning.main"
                                        >
                                            <Typography
                                                variant="body2"
                                                title={approval.blockedBy[0].label}
                                                sx={{ overflowWrap: 'anywhere' }}
                                            >
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
                                    {(approval.description || processTasks.length || resourcePlan) && (
                                        <Box display="flex" flexWrap="wrap" alignItems="center" gap={1} mt={0.5}>
                                            {approval.description && (
                                                <Button
                                                    size="small"
                                                    variant="text"
                                                    onClick={() =>
                                                        setSelectedContext({
                                                            kind: 'description',
                                                            description: approval.description,
                                                            summary: approval.summary,
                                                        })
                                                    }
                                                    sx={{ paddingLeft: 0, textTransform: 'none' }}
                                                >
                                                    View description
                                                </Button>
                                            )}
                                            {processTasks.length > 0 && (
                                                <Button
                                                    size="small"
                                                    variant="text"
                                                    onClick={() =>
                                                        setSelectedContext({
                                                            kind: 'advanced',
                                                            tasks: processTasks,
                                                            initialTaskId,
                                                            summary: approval.summary,
                                                        })
                                                    }
                                                    sx={{ paddingLeft: 0, textTransform: 'none' }}
                                                >
                                                    View advanced
                                                </Button>
                                            )}
                                            {resourcePlan && (
                                                <Button
                                                    size="small"
                                                    variant="text"
                                                    endIcon={
                                                        changesExpanded ? <KeyboardArrowUp /> : <KeyboardArrowDown />
                                                    }
                                                    aria-expanded={changesExpanded}
                                                    onClick={() => toggleChanges(key)}
                                                    sx={{ paddingLeft: 0, textTransform: 'none' }}
                                                >
                                                    {changesExpanded ? 'Hide resource changes' : 'View resource changes'}
                                                </Button>
                                            )}
                                        </Box>
                                    )}
                                    {resourcePlan && (
                                        <Collapse in={changesExpanded} timeout="auto" unmountOnExit>
                                            <Box
                                                border={1}
                                                borderColor="divider"
                                                borderRadius={1}
                                                overflow="hidden"
                                                marginBottom={1}
                                            >
                                                <Box padding={1} bgcolor="action.hover">
                                                    <Typography variant="body2">
                                                        <b>
                                                            {resourcePlan.currentResource
                                                                ? 'Proposed resource changes'
                                                                : 'New resource'}
                                                        </b>
                                                    </Typography>
                                                </Box>
                                                <Box
                                                    overflow="auto"
                                                    sx={{
                                                        '& table': { width: '100%' },
                                                        '& pre': {
                                                            whiteSpace: 'pre-wrap !important',
                                                            overflowWrap: 'anywhere',
                                                        },
                                                    }}
                                                >
                                                    <ResourceJsonDiff plan={resourcePlan} splitView={false} />
                                                </Box>
                                            </Box>
                                        </Collapse>
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
            <Dialog
                open={selectedContext !== null}
                onClose={() => setSelectedContext(null)}
                fullWidth
                maxWidth="lg"
            >
                <DialogTitle>
                    {selectedContext?.kind === 'advanced' ? 'Approval details' : 'Approval description'}
                    {selectedContext && (
                        <Typography variant="body2" color="text.secondary">
                            {selectedContext.summary}
                        </Typography>
                    )}
                </DialogTitle>
                <DialogContent
                    dividers
                    sx={{
                        padding: selectedContext?.kind === 'advanced' ? 0 : undefined,
                        '& p, & li, & a': {
                            overflowWrap: 'anywhere',
                            wordBreak: 'break-word',
                        },
                        '& pre': {
                            maxWidth: '100%',
                            overflowX: 'auto',
                            whiteSpace: 'pre-wrap',
                            overflowWrap: 'anywhere',
                        },
                    }}
                >
                    {selectedContext?.kind === 'advanced' ? (
                        <AdvancedTaskExplorer
                            key={`${selectedContext.summary}:${selectedContext.initialTaskId}`}
                            tasks={selectedContext.tasks}
                            initialTaskId={selectedContext.initialTaskId}
                        />
                    ) : (
                        selectedContext && <ReactMarkdown>{selectedContext.description}</ReactMarkdown>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setSelectedContext(null)}>Close</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default ExecutionApprovals;
