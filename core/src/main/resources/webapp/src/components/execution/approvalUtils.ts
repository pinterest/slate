import { IExecutionPlan, TExecutionStatus } from './types';

export const GROUP_APPROVAL_TASK_DEFINITION_ID = 'groupApprovalTask';

export interface IExecutionApprovalDependency {
    id: string;
    label: string;
    status: TExecutionStatus;
    type: 'resource' | 'task';
}

export interface IExecutionApproval {
    processId: string;
    taskId: string;
    resourceId: string;
    group: string;
    summary: string;
    description: string;
    status: TExecutionStatus;
    dependencyLevel: number;
    blockedBy: IExecutionApprovalDependency[];
    startTimeMs: number;
    endTimeMs: number;
}

export const formatApprovalWait = (startTimeMs: number, nowMs: number): string => {
    if (!startTimeMs || nowMs < startTimeMs) {
        return '';
    }
    const minutes = Math.floor((nowMs - startTimeMs) / 60000);
    if (minutes < 1) {
        return 'Waiting for less than a minute';
    }
    if (minutes < 60) {
        return `Waiting for ${minutes} minute${minutes === 1 ? '' : 's'}`;
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
        return `Waiting for ${hours} hour${hours === 1 ? '' : 's'}`;
    }
    const days = Math.floor(hours / 24);
    return `Waiting for ${days} day${days === 1 ? '' : 's'}`;
};

export const formatApprovalDate = (endTimeMs: number): string => {
    if (!endTimeMs) {
        return '';
    }
    const date = new Date(endTimeMs);
    return `${date.getMonth() + 1}/${date.getDate()}/${date.getFullYear()}`;
};

export const formatApprovalTimestamp = (endTimeMs: number): string => {
    if (!endTimeMs) {
        return '';
    }
    return new Date(endTimeMs).toLocaleString(undefined, {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
    });
};

export const formatDuration = (startTimeMs: number, endTimeMs: number): string | null => {
    if (!startTimeMs || !endTimeMs || endTimeMs < startTimeMs) {
        return null;
    }
    const seconds = Math.round((endTimeMs - startTimeMs) / 1000);
    if (seconds < 60) {
        return `${seconds}s`;
    }
    const minutes = Math.floor(seconds / 60);
    const remainder = seconds % 60;
    return remainder ? `${minutes}m ${remainder}s` : `${minutes}m`;
};

const readString = (value: unknown): string => (typeof value === 'string' ? value : '');

const readRecord = (value: unknown): Record<string, unknown> =>
    value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : {};

const humanizeIdentifier = (value: string): string =>
    value
        .replace(/Definition$/, '')
        .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
        .replace(/[_-]+/g, ' ')
        .trim();

const getResourceLabel = (resourceId: string, planEntry: IExecutionPlan): string => {
    const resource = planEntry.proposedResource;
    const desiredState = readRecord(resource?.desiredState);
    const name =
        readString(desiredState.name) ||
        readString(desiredState.topicName) ||
        readString(desiredState.logName) ||
        readString(desiredState.filename);
    const resourceType = humanizeIdentifier(resource?.resourceDefinitionClass?.split('.').pop() ?? '');

    if (resourceType && name) {
        return `${resourceType}: ${name}`;
    }
    return resourceType || name || resourceId;
};

const getCurrentTaskLabel = (planEntry: IExecutionPlan): string => {
    const process = planEntry.process;
    if (!process) {
        return '';
    }

    for (const taskId of process.currenTaskSet ?? []) {
        const context = readRecord(process.processContext?.[taskId]);
        const summary = readString(context.summary);
        if (summary) {
            return summary;
        }
        if (process.allTasks[taskId]) {
            return humanizeIdentifier(taskId);
        }
    }
    return '';
};

export const getExecutionApprovals = (plan: Record<string, IExecutionPlan>): IExecutionApproval[] => {
    const approvals: IExecutionApproval[] = [];
    const planByResourceId = new Map<string, IExecutionPlan>();

    Object.entries(plan).forEach(([planId, planEntry]) => {
        planByResourceId.set(planId, planEntry);
        if (planEntry.proposedResource?.id) {
            planByResourceId.set(planEntry.proposedResource.id, planEntry);
        }
    });

    const dependencyLevel = (resourceId: string, visited: Set<string> = new Set()): number => {
        if (visited.has(resourceId)) {
            return 0;
        }
        const planEntry = planByResourceId.get(resourceId);
        if (!planEntry) {
            return 0;
        }
        const upstreamIds = (planEntry.upstreamVertices ?? []).filter(
            (upstreamId): upstreamId is string =>
                typeof upstreamId === 'string' && Boolean(planByResourceId.get(upstreamId)?.process)
        );
        if (!upstreamIds.length) {
            return 0;
        }

        const nextVisited = new Set(visited);
        nextVisited.add(resourceId);
        return 1 + Math.max(...upstreamIds.map((upstreamId) => dependencyLevel(upstreamId, nextVisited)));
    };

    Object.entries(plan).forEach(([planId, planEntry]) => {
        const { process } = planEntry;
        if (!process) {
            return;
        }
        const resourceId = planEntry.proposedResource?.id || planId;
        const blockedByResources: IExecutionApprovalDependency[] = [];
        (planEntry.upstreamVertices ?? [])
            .filter((upstreamId): upstreamId is string => typeof upstreamId === 'string')
            .forEach((upstreamId) => {
                const upstreamPlan = planByResourceId.get(upstreamId);
                if (!upstreamPlan?.process || upstreamPlan.process.endStatus === 'SUCCEEDED') {
                    return;
                }
                blockedByResources.push({
                    id: upstreamId,
                    label: getCurrentTaskLabel(upstreamPlan) || getResourceLabel(upstreamId, upstreamPlan),
                    status: upstreamPlan.process.endStatus,
                    type: 'resource',
                });
            });

        Object.entries(process.allTasks).forEach(([taskId, task]) => {
            if (task.taskDefinitionId !== GROUP_APPROVAL_TASK_DEFINITION_ID) {
                return;
            }

            const context = readRecord(process.processContext?.[taskId]);
            const blockedBy = [...blockedByResources];
            if (!blockedBy.length && task.status === 'NOT_STARTED' && process.endStatus === 'RUNNING') {
                (process.currenTaskSet ?? [])
                    .filter((currentTaskId) => currentTaskId !== taskId)
                    .forEach((currentTaskId) => {
                        const currentTask = process.allTasks[currentTaskId];
                        if (currentTask && currentTask.status !== 'SUCCEEDED') {
                            const currentContext = readRecord(process.processContext?.[currentTaskId]);
                            blockedBy.push({
                                id: currentTaskId,
                                label: readString(currentContext.summary) || humanizeIdentifier(currentTaskId),
                                status: currentTask.status,
                                type: 'task',
                            });
                        }
                    });
            }

            approvals.push({
                processId: process.processId,
                taskId,
                resourceId,
                group: readString(context.approvalGroup),
                summary: readString(context.summary) || taskId,
                description: readString(context.description),
                status: task.status,
                dependencyLevel: dependencyLevel(resourceId),
                blockedBy,
                startTimeMs: task.startTimeMs || 0,
                endTimeMs: task.endTimeMs || 0,
            });
        });
    });

    return approvals.sort((left, right) => left.dependencyLevel - right.dependencyLevel);
};
