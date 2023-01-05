import { TExecutionStatus } from '../execution/types';

export type TTaskType = 'APPROVAL' | 'NON_VERIFIABLE' | 'VERIFYABLE';

export interface ITask {
    additionalData: string;
    assigneeGroupName: string;
    assigneeUser: string;
    comment: null | string;
    createTime: string;
    description: string;
    executionId: string;
    processId: string;
    taskId: string;
    summary: string;
    taskStatus: TExecutionStatus;
    taskType: TTaskType;
    updateTime: string;
}
