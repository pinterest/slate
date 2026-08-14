import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Autocomplete,
    Badge,
    Box,
    Button,
    Chip,
    Divider,
    IconButton,
    InputAdornment,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Stack,
    TextField,
    ToggleButton,
    Tooltip,
    Typography,
    useMediaQuery,
    useTheme,
} from '@mui/material';
import {
    Autorenew,
    CancelOutlined,
    CheckCircleOutline,
    Clear,
    ErrorOutline,
    ExpandMore,
    RadioButtonUnchecked,
    Search,
} from '@mui/icons-material';
import { IExecutionTaskJson, TExecutionStatus } from './types';
import JsonPrettier from '../common/JsonPrettier';
import { formatDuration } from './approvalUtils';

export interface IApprovalTaskDetails {
    taskId: string;
    task: IExecutionTaskJson;
    context: unknown;
}

type TStatusFilter = 'ALL' | TExecutionStatus;

type TChipColor = 'default' | 'error' | 'info' | 'success' | 'warning';

export const taskStatusColor = (status: TExecutionStatus): TChipColor => {
    switch (status) {
        case 'SUCCEEDED':
            return 'success';
        case 'FAILED':
            return 'error';
        case 'RUNNING':
            return 'info';
        case 'CANCELLED':
            return 'warning';
        default:
            return 'default';
    }
};

const statusTextColor = (status: TExecutionStatus): string => {
    const color = taskStatusColor(status);
    return color === 'default' ? 'text.secondary' : `${color}.main`;
};

const StatusIcon: React.FC<{ status: TExecutionStatus; fontSize?: 'small' | 'inherit' }> = ({
    status,
    fontSize = 'small',
}) => {
    const color = taskStatusColor(status);
    switch (status) {
        case 'FAILED':
            return <ErrorOutline color="error" fontSize={fontSize} />;
        case 'CANCELLED':
            return <CancelOutlined color="warning" fontSize={fontSize} />;
        case 'RUNNING':
            return <Autorenew color="info" fontSize={fontSize} />;
        case 'SUCCEEDED':
            return <CheckCircleOutline color="success" fontSize={fontSize} />;
        default:
            return <RadioButtonUnchecked color={color === 'default' ? 'disabled' : color} fontSize={fontSize} />;
    }
};

// Filters shown in the rail, in the order most useful for debugging.
const FILTER_ORDER: TExecutionStatus[] = ['FAILED', 'CANCELLED', 'RUNNING', 'SUCCEEDED', 'NOT_STARTED'];

const FILTER_LABEL: Record<TExecutionStatus, string> = {
    FAILED: 'Failed',
    CANCELLED: 'Cancelled',
    RUNNING: 'Running',
    SUCCEEDED: 'Succeeded',
    NOT_STARTED: 'Not started',
};

const transitionLabel = (transition: string): string => {
    switch (transition) {
        case 'SUCCEEDED':
            return 'On success';
        case 'FAILED':
            return 'On failure';
        case 'CANCELLED':
            return 'If cancelled';
        default:
            return `On ${transition.replace(/[_-]+/g, ' ').toLowerCase()}`;
    }
};

const transitionOrder = (transition: string): number => {
    const index = ['SUCCEEDED', 'FAILED', 'CANCELLED'].indexOf(transition);
    return index >= 0 ? index : 3;
};

const hasStdErr = (task: IExecutionTaskJson): boolean => Boolean(task.stdErr?.length);

const isFailureStatus = (status: TExecutionStatus): boolean =>
    status === 'FAILED' || status === 'CANCELLED';

const hasContent = (value: unknown): boolean => {
    if (value === null || value === undefined) {
        return false;
    }
    if (Array.isArray(value)) {
        return value.length > 0;
    }
    if (typeof value === 'object') {
        return Object.keys(value).length > 0;
    }
    return true;
};

interface IAdvancedSectionProps {
    title: string;
    data?: Object;
    emptyMessage?: string;
    defaultExpanded?: boolean;
    errorMarker?: boolean;
    meta?: string;
}

const AdvancedSection: React.FC<IAdvancedSectionProps> = ({
    title,
    data,
    emptyMessage,
    defaultExpanded = false,
    errorMarker = false,
    meta,
}) => {
    const isEmpty = data === undefined;
    return (
        <Accordion
            defaultExpanded={defaultExpanded}
            disableGutters
            square
            elevation={0}
            sx={{
                borderTop: 1,
                borderColor: 'divider',
                '&:before': { display: 'none' },
            }}
        >
            <AccordionSummary
                expandIcon={<ExpandMore fontSize="small" />}
                sx={{
                    minHeight: 0,
                    px: 0,
                    '& .MuiAccordionSummary-content': { my: 0.75, alignItems: 'center' },
                }}
            >
                {errorMarker && <ErrorOutline color="error" fontSize="small" sx={{ mr: 0.75 }} />}
                <Typography variant="body2">{title}</Typography>
                {meta && (
                    <Typography variant="caption" color="text.secondary" sx={{ ml: 0.75 }}>
                        {meta}
                    </Typography>
                )}
            </AccordionSummary>
            <AccordionDetails sx={{ px: 0, pt: 0, pb: 1, minWidth: 0 }}>
                {isEmpty ? (
                    <Typography variant="caption" color="text.secondary">
                        {emptyMessage}
                    </Typography>
                ) : (
                    <JsonPrettier data={data as Object} variant="light" />
                )}
            </AccordionDetails>
        </Accordion>
    );
};

interface IAdvancedTaskDetailsProps {
    details: IApprovalTaskDetails;
    onNavigate: (taskId: string) => void;
    isKnownTask: (taskId: string) => boolean;
    taskOrder: ReadonlyMap<string, number>;
}

const AdvancedTaskDetails: React.FC<IAdvancedTaskDetailsProps> = ({
    details,
    onNavigate,
    isKnownTask,
    taskOrder,
}) => {
    const { taskId, task, context } = details;
    const stdErrPresent = hasStdErr(task);
    const stdOutPresent = Boolean(task.stdOut?.length);
    const contextPresent = hasContent(context);
    const duration = formatDuration(task.startTimeMs, task.endTimeMs);
    const downstreamGroups = useMemo(() => {
        return Object.entries(task.nextPointers ?? {})
            .sort(([left], [right]) => transitionOrder(left) - transitionOrder(right))
            .map(([transition, taskIds]) => ({
                transition,
                taskIds: Array.from(
                    new Set(taskIds.filter((next) => next && isKnownTask(next)))
                ).sort(
                    (left, right) =>
                        (taskOrder.get(left) ?? Number.MAX_SAFE_INTEGER) -
                        (taskOrder.get(right) ?? Number.MAX_SAFE_INTEGER)
                ),
            }))
            .filter(({ taskIds }) => taskIds.length > 0);
    }, [task.nextPointers, isKnownTask, taskOrder]);

    return (
        <Box>
            <Stack direction="row" alignItems="baseline" justifyContent="space-between" spacing={1}>
                <Typography variant="subtitle2" sx={{ overflowWrap: 'anywhere', minWidth: 0 }}>
                    {taskId}
                </Typography>
                <Stack direction="row" alignItems="center" spacing={0.5} sx={{ flexShrink: 0 }}>
                    <StatusIcon status={task.status} fontSize="inherit" />
                    <Typography variant="caption" color={statusTextColor(task.status)}>
                        {task.status}
                    </Typography>
                </Stack>
            </Stack>
            <Typography
                variant="caption"
                color="text.secondary"
                display="block"
                sx={{ overflowWrap: 'anywhere' }}
            >
                {task.taskDefinitionId}
                {duration ? ` · ${duration}` : ''}
            </Typography>
            {downstreamGroups.length > 0 && (
                <Stack spacing={0.5} mt={0.75}>
                    {downstreamGroups.map(({ transition, taskIds }) => (
                        <Box
                            key={transition}
                            display="flex"
                            flexWrap="wrap"
                            alignItems="center"
                            gap={0.5}
                        >
                            <Typography
                                variant="caption"
                                color="text.secondary"
                                sx={{ minWidth: 72 }}
                            >
                                {transitionLabel(transition)} →
                            </Typography>
                            {taskIds.map((next) => (
                                <Chip
                                    key={next}
                                    size="small"
                                    variant="outlined"
                                    clickable
                                    onClick={() => onNavigate(next)}
                                    label={next}
                                    sx={{
                                        height: 20,
                                        maxWidth: 200,
                                        '& .MuiChip-label': {
                                            px: 0.75,
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                        },
                                    }}
                                />
                            ))}
                        </Box>
                    ))}
                </Stack>
            )}
            <Box mt={1}>
                <AdvancedSection
                    title="Standard error"
                    data={stdErrPresent ? task.stdErr : undefined}
                    emptyMessage="No standard error"
                    defaultExpanded={stdErrPresent}
                    errorMarker={stdErrPresent}
                    meta={
                        stdErrPresent
                            ? `${task.stdErr.length} line${task.stdErr.length > 1 ? 's' : ''}`
                            : undefined
                    }
                />
                <AdvancedSection
                    title="Standard output"
                    data={stdOutPresent ? task.stdOut : undefined}
                    emptyMessage="No standard output"
                    defaultExpanded={stdOutPresent}
                />
                <AdvancedSection
                    title="Process context"
                    data={contextPresent ? (context as Object) : undefined}
                    emptyMessage="No process context"
                    defaultExpanded={contextPresent}
                />
                <AdvancedSection title="Complete task JSON" data={task} defaultExpanded />
            </Box>
        </Box>
    );
};

interface ITaskNavItemProps {
    index: number;
    details: IApprovalTaskDetails;
    selected: boolean;
    onSelect: (taskId: string) => void;
    registerRef: (taskId: string, node: HTMLDivElement | null) => void;
    optionId: string;
}

const TaskNavItem: React.FC<ITaskNavItemProps> = ({
    index,
    details,
    selected,
    onSelect,
    registerRef,
    optionId,
}) => {
    const { taskId, task } = details;
    const stdErrPresent = hasStdErr(task);
    return (
        <ListItemButton
            id={optionId}
            role="option"
            aria-selected={selected}
            selected={selected}
            tabIndex={-1}
            ref={(node: HTMLDivElement | null) => registerRef(taskId, node)}
            onClick={() => onSelect(taskId)}
            sx={{ alignItems: 'flex-start', gap: 1, py: 0.75 }}
        >
            <Typography
                variant="caption"
                color="text.secondary"
                sx={{ minWidth: 20, textAlign: 'right', mt: 0.25, flexShrink: 0 }}
            >
                {index + 1}
            </Typography>
            <ListItemIcon sx={{ minWidth: 0, mt: 0.25 }}>
                <StatusIcon status={task.status} />
            </ListItemIcon>
            <ListItemText
                primary={taskId}
                secondary={task.taskDefinitionId}
                primaryTypographyProps={{ noWrap: true, title: taskId }}
                secondaryTypographyProps={{ noWrap: true, title: task.taskDefinitionId, variant: 'caption' }}
                sx={{ minWidth: 0, my: 0 }}
            />
            {stdErrPresent && (
                <Tooltip title="Has standard error output" arrow>
                    <Badge color="error" variant="dot" sx={{ mt: 1, mr: 0.5 }} />
                </Tooltip>
            )}
        </ListItemButton>
    );
};

interface IAdvancedTaskExplorerProps {
    tasks: IApprovalTaskDetails[];
    initialTaskId: string;
}

const AdvancedTaskExplorer: React.FC<IAdvancedTaskExplorerProps> = ({ tasks, initialTaskId }) => {
    const theme = useTheme();
    const isNarrow = useMediaQuery(theme.breakpoints.down('sm'));
    const [selectedTaskId, setSelectedTaskId] = useState(initialTaskId);
    const [query, setQuery] = useState('');
    const [statusFilter, setStatusFilter] = useState<TStatusFilter>('ALL');
    const itemRefs = useRef<Map<string, HTMLDivElement>>(new Map());

    const knownTaskIds = useMemo(() => new Set(tasks.map(({ taskId }) => taskId)), [tasks]);
    const isKnownTask = useCallback((taskId: string) => knownTaskIds.has(taskId), [knownTaskIds]);
    const taskOrder = useMemo(
        () => new Map(tasks.map(({ taskId }, index) => [taskId, index])),
        [tasks]
    );

    const counts = useMemo(() => {
        const byStatus = new Map<TExecutionStatus, number>();
        tasks.forEach(({ task }) => {
            byStatus.set(task.status, (byStatus.get(task.status) ?? 0) + 1);
        });
        return byStatus;
    }, [tasks]);

    const issueTasks = useMemo(
        () => tasks.filter(({ task }) => isFailureStatus(task.status) || hasStdErr(task)),
        [tasks]
    );
    const failureCount = issueTasks.length;

    const visibleTasks = useMemo(() => {
        const normalizedQuery = query.trim().toLowerCase();
        return tasks.filter(({ taskId, task }) => {
            if (statusFilter !== 'ALL' && task.status !== statusFilter) {
                return false;
            }
            if (!normalizedQuery) {
                return true;
            }
            return (
                taskId.toLowerCase().includes(normalizedQuery) ||
                task.taskDefinitionId.toLowerCase().includes(normalizedQuery)
            );
        });
    }, [tasks, query, statusFilter]);

    // Selection is derived from the full task set so filtering never blanks the detail pane.
    const selectedDetails = useMemo(
        () => tasks.find(({ taskId }) => taskId === selectedTaskId) ?? tasks[0],
        [tasks, selectedTaskId]
    );

    const optionId = useCallback((taskId: string) => `advanced-task-option-${taskId}`, []);

    const registerRef = useCallback((taskId: string, node: HTMLDivElement | null) => {
        if (node) {
            itemRefs.current.set(taskId, node);
        } else {
            itemRefs.current.delete(taskId);
        }
    }, []);

    useEffect(() => {
        const node = selectedDetails ? itemRefs.current.get(selectedDetails.taskId) : undefined;
        node?.scrollIntoView({ block: 'nearest' });
    }, [selectedDetails]);

    const selectedIssueIndex = issueTasks.findIndex(({ taskId }) => taskId === selectedTaskId);
    const nextIssueIndex = issueTasks.length ? (selectedIssueIndex + 1) % issueTasks.length : -1;
    const nextIssueLabel =
        nextIssueIndex >= 0 ? `Next issue (${nextIssueIndex + 1} of ${issueTasks.length})` : '';

    const jumpToNextIssue = useCallback(() => {
        const nextIssue = issueTasks[nextIssueIndex];
        if (nextIssue) {
            setStatusFilter('ALL');
            setQuery('');
            setSelectedTaskId(nextIssue.taskId);
        }
    }, [issueTasks, nextIssueIndex]);

    const handleListKeyDown = useCallback(
        (event: React.KeyboardEvent<HTMLUListElement>) => {
            if (!visibleTasks.length) {
                return;
            }
            const currentIndex = visibleTasks.findIndex(({ taskId }) => taskId === selectedDetails?.taskId);
            let nextIndex: number | null = null;
            switch (event.key) {
                case 'ArrowDown':
                    nextIndex = currentIndex < 0 ? 0 : Math.min(currentIndex + 1, visibleTasks.length - 1);
                    break;
                case 'ArrowUp':
                    nextIndex = currentIndex <= 0 ? 0 : currentIndex - 1;
                    break;
                case 'Home':
                    nextIndex = 0;
                    break;
                case 'End':
                    nextIndex = visibleTasks.length - 1;
                    break;
                default:
                    return;
            }
            if (nextIndex !== null) {
                event.preventDefault();
                setSelectedTaskId(visibleTasks[nextIndex].taskId);
            }
        },
        [visibleTasks, selectedDetails]
    );

    const searchField = (
        <TextField
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            size="small"
            fullWidth
            placeholder="Filter tasks…"
            aria-label="Filter tasks by id or definition"
            InputProps={{
                startAdornment: (
                    <InputAdornment position="start">
                        <Search fontSize="small" />
                    </InputAdornment>
                ),
                endAdornment: query ? (
                    <InputAdornment position="end">
                        <IconButton size="small" aria-label="Clear filter" onClick={() => setQuery('')}>
                            <Clear fontSize="small" />
                        </IconButton>
                    </InputAdornment>
                ) : undefined,
            }}
        />
    );

    const filters: Array<{ value: TStatusFilter; label: string; count: number }> = [
        { value: 'ALL', label: 'All', count: tasks.length },
        ...FILTER_ORDER.filter((status) => (counts.get(status) ?? 0) > 0).map((status) => ({
            value: status,
            label: FILTER_LABEL[status],
            count: counts.get(status) ?? 0,
        })),
    ];
    const filterToggles = (
        <Box
            role="group"
            aria-label="Filter tasks by status"
            display="grid"
            gridTemplateColumns="repeat(auto-fit, minmax(104px, 1fr))"
            gap={0.5}
            width="100%"
        >
            {filters.map(({ value, label, count }) => (
                <ToggleButton
                    key={value}
                    value={value}
                    selected={statusFilter === value}
                    size="small"
                    onClick={() => setStatusFilter(value)}
                    sx={{
                        border: 1,
                        borderColor: 'divider',
                        borderRadius: '4px !important',
                        minWidth: 0,
                        px: 1,
                        py: 0.375,
                        textTransform: 'none',
                        whiteSpace: 'nowrap',
                        width: '100%',
                    }}
                >
                    <Box
                        display="flex"
                        alignItems="center"
                        justifyContent="space-between"
                        gap={0.75}
                        width="100%"
                        minWidth={0}
                    >
                        <Typography variant="caption" noWrap>
                            {label}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            {count}
                        </Typography>
                    </Box>
                </ToggleButton>
            ))}
        </Box>
    );

    const detailPane = (
        <Box
            flex={1}
            minWidth={0}
            maxHeight={isNarrow ? undefined : '65vh'}
            overflow="auto"
            padding={1.5}
        >
            {selectedDetails ? (
                <AdvancedTaskDetails
                    key={selectedDetails.taskId}
                    details={selectedDetails}
                    onNavigate={setSelectedTaskId}
                    isKnownTask={isKnownTask}
                    taskOrder={taskOrder}
                />
            ) : (
                <Typography variant="body2" color="text.secondary">
                    No task details found
                </Typography>
            )}
        </Box>
    );

    if (isNarrow) {
        return (
            <Box padding={2}>
                <Stack spacing={1.5}>
                    <Autocomplete
                        options={tasks}
                        value={selectedDetails ?? null}
                        onChange={(_, value) => value && setSelectedTaskId(value.taskId)}
                        getOptionLabel={(option) => option.taskId}
                        isOptionEqualToValue={(a, b) => a.taskId === b.taskId}
                        disableClearable
                        renderOption={(props, option) => (
                            <li {...props} key={option.taskId}>
                                <Stack direction="row" alignItems="center" spacing={1} minWidth={0}>
                                    <StatusIcon status={option.task.status} />
                                    <Box minWidth={0}>
                                        <Typography variant="body2" noWrap>
                                            {option.taskId}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary" noWrap display="block">
                                            {option.task.taskDefinitionId}
                                        </Typography>
                                    </Box>
                                </Stack>
                            </li>
                        )}
                        renderInput={(params) => (
                            <TextField {...params} size="small" label="Select task" placeholder="Search tasks…" />
                        )}
                    />
                    {failureCount > 0 && (
                        <Button size="small" color="error" onClick={jumpToNextIssue} sx={{ alignSelf: 'flex-start', textTransform: 'none' }}>
                            {nextIssueLabel}
                        </Button>
                    )}
                </Stack>
                {detailPane}
            </Box>
        );
    }

    return (
        <Box display="flex" minWidth={0} width="100%" alignItems="stretch">
            <Box
                width="clamp(264px, 31%, 360px)"
                minWidth={264}
                flexShrink={1}
                borderRight={1}
                borderColor="divider"
                display="flex"
                flexDirection="column"
                maxHeight="65vh"
            >
                <Box padding={1.5} borderBottom={1} borderColor="divider">
                    <Stack spacing={1}>
                        {searchField}
                        {filterToggles}
                        <Box
                            display="flex"
                            flexWrap="wrap"
                            justifyContent="space-between"
                            alignItems="center"
                            columnGap={1}
                            rowGap={0.5}
                        >
                            <Typography variant="caption" color="text.secondary">
                                {visibleTasks.length} of {tasks.length} shown
                            </Typography>
                            {failureCount > 0 && (
                                <Button
                                    size="small"
                                    color="error"
                                    onClick={jumpToNextIssue}
                                    sx={{ textTransform: 'none', py: 0, minWidth: 0 }}
                                >
                                    {nextIssueLabel}
                                </Button>
                            )}
                        </Box>
                    </Stack>
                </Box>
                <List
                    role="listbox"
                    aria-label="Execution tasks"
                    aria-activedescendant={selectedDetails ? optionId(selectedDetails.taskId) : undefined}
                    tabIndex={0}
                    onKeyDown={handleListKeyDown}
                    sx={{ overflow: 'auto', flex: 1, py: 0 }}
                >
                    {visibleTasks.length ? (
                        visibleTasks.map((details) => (
                            <TaskNavItem
                                key={details.taskId}
                                index={tasks.indexOf(details)}
                                details={details}
                                selected={selectedDetails?.taskId === details.taskId}
                                onSelect={setSelectedTaskId}
                                registerRef={registerRef}
                                optionId={optionId(details.taskId)}
                            />
                        ))
                    ) : (
                        <Box padding={2}>
                            <Typography variant="body2" color="text.secondary">
                                No tasks match the current filter.
                            </Typography>
                        </Box>
                    )}
                </List>
            </Box>
            <Divider orientation="vertical" flexItem />
            {detailPane}
        </Box>
    );
};

export default AdvancedTaskExplorer;
