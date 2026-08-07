import { IExecutionPlan, TExecutionStatus } from './types';

export const GROUP_APPROVAL_TASK_DEFINITION_ID = 'groupApprovalTask';

export interface IExecutionApproval {
    processId: string;
    taskId: string;
    group: string;
    summary: string;
    description: string;
    status: TExecutionStatus;
}

const readString = (value: unknown): string => (typeof value === 'string' ? value : '');

export const getExecutionApprovals = (plan: Record<string, IExecutionPlan>): IExecutionApproval[] => {
    const approvals: IExecutionApproval[] = [];

    Object.values(plan).forEach(({ process }) => {
        if (!process) {
            return;
        }

        Object.entries(process.allTasks).forEach(([taskId, task]) => {
            if (task.taskDefinitionId !== GROUP_APPROVAL_TASK_DEFINITION_ID) {
                return;
            }

            const rawContext = process.processContext?.[taskId];
            const context =
                rawContext && typeof rawContext === 'object' && !Array.isArray(rawContext)
                    ? (rawContext as Record<string, unknown>)
                    : {};

            approvals.push({
                processId: process.processId,
                taskId,
                group: readString(context.approvalGroup),
                summary: readString(context.summary) || taskId,
                description: readString(context.description),
                status: task.status,
            });
        });
    });

    return approvals;
};
