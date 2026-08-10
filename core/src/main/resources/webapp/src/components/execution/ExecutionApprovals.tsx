import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
import { IExecutionPlan, TExecutionStatus } from './types';
import { getExecutionApprovals } from './approvalUtils';
import { ResourceJsonDiff } from './ExecPlanJsonDiff';
import ApprovalReviewButton from './ApprovalReviewButton';

const TASK_REFRESH_FREQUENCY = 5000;
const DESCRIPTION_PREVIEW_LENGTH = 240;

interface IExecutionApprovalsProps {
    plan: Record<string, IExecutionPlan>;
}

const taskKey = (processId: string, taskId: string): string => `${processId}:${taskId}`;

interface IDescriptionPreview {
    text: string;
    hasHiddenContext: boolean;
}

const truncateDescription = (description: string): string => {
    const preview = description.slice(0, DESCRIPTION_PREVIEW_LENGTH);
    const lastWhitespace = preview.lastIndexOf(' ');
    const cutoff = lastWhitespace > DESCRIPTION_PREVIEW_LENGTH / 2 ? lastWhitespace : preview.length;
    return `${preview.slice(0, cutoff).replace(/\s+$/, '')}…`;
};

const getDescriptionPreview = (description: string): IDescriptionPreview => {
    const trimmed = description.trim();
    const isStructuredOnly = /^(?:json\s*:|\{|\[)/i.test(trimmed);
    if (isStructuredOnly) {
        return { text: '', hasHiddenContext: true };
    }

    const structuredContextIndexes = [
        description.search(/(?:^|\n)\s*(?:task\s+)?json\s*:/i),
        description.search(/(?:^|\n)\s*```(?:json)?/i),
    ].filter((index) => index >= 0);
    const structuredContextStart = structuredContextIndexes.length ? Math.min(...structuredContextIndexes) : -1;
    const prose = structuredContextStart >= 0 ? description.slice(0, structuredContextStart) : description;
    const plainText = prose
        .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
        .replace(/[*_`#>]/g, '')
        .replace(/\s+-\s+/g, ' · ')
        .replace(/\s+/g, ' ')
        .trim();
    const isTruncated = plainText.length > DESCRIPTION_PREVIEW_LENGTH;

    return {
        text: isTruncated ? truncateDescription(plainText) : plainText,
        hasHiddenContext: structuredContextStart >= 0 || isTruncated,
    };
};

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
    const [selectedContext, setSelectedContext] = useState<null | { description: string; summary: string }>(null);

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
        <Box height="100%" overflow="auto" paddingTop={1}>
            <ApprovalReviewButton />
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
                            const resourcePlan = resourcePlans.get(approval.resourceId);
                            const changesExpanded = expandedChanges.has(key);
                            const description = getDescriptionPreview(approval.description);

                            return (
                                <Box
                                    key={key}
                                    border={1}
                                    borderColor="divider"
                                    borderRadius={1}
                                    padding={1.5}
                                    minWidth={0}
                                    overflow="hidden"
                                >
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
                                        <Chip
                                            size="small"
                                            label={status.label}
                                            color={status.color}
                                            sx={{ flexShrink: 0 }}
                                        />
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
                                    {description.text && (
                                        <Typography
                                            variant="body2"
                                            mt={1}
                                            sx={{
                                                display: '-webkit-box',
                                                WebkitBoxOrient: 'vertical',
                                                WebkitLineClamp: 2,
                                                overflow: 'hidden',
                                                overflowWrap: 'anywhere',
                                                wordBreak: 'break-word',
                                            }}
                                        >
                                            {description.text}
                                        </Typography>
                                    )}
                                    {(description.hasHiddenContext || resourcePlan) && (
                                        <Box display="flex" flexWrap="wrap" alignItems="center" gap={1} mt={0.5}>
                                            {description.hasHiddenContext && (
                                                <Button
                                                    size="small"
                                                    variant="text"
                                                    onClick={() =>
                                                        setSelectedContext({
                                                            description: approval.description,
                                                            summary: approval.summary,
                                                        })
                                                    }
                                                    sx={{ paddingLeft: 0, textTransform: 'none' }}
                                                >
                                                    View full description
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
                maxWidth="md"
            >
                <DialogTitle>
                    Approval description
                    {selectedContext && (
                        <Typography variant="body2" color="text.secondary">
                            {selectedContext.summary}
                        </Typography>
                    )}
                </DialogTitle>
                <DialogContent
                    dividers
                    sx={{
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
                    {selectedContext && <ReactMarkdown>{selectedContext.description}</ReactMarkdown>}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setSelectedContext(null)}>Close</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default ExecutionApprovals;
