import React, { useMemo, useCallback } from 'react';
import { Box, Accordion, AccordionSummary, AccordionDetails } from '@mui/material';
import { ExpandMore } from '@mui/icons-material';
import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer';
import { IExecutionPlan } from './types';
import { sortObjectByKeys } from '../../const/basicUtils';

interface IExecPlanJsonDiffProps {
    execPlan: Record<string, IExecutionPlan>;
}

const ExecPlanJsonDiff: React.FC<IExecPlanJsonDiffProps> = ({ execPlan }) => {
    const newAndExistingResources: [IExecutionPlan[], IExecutionPlan[]] = useMemo(() => {
        const newList: IExecutionPlan[] = [];
        const existingList: IExecutionPlan[] = [];
        Object.keys(execPlan).forEach((key) => {
            if (execPlan[key].process) {
                if (execPlan[key].currentResource) {
                    existingList.push(execPlan[key]);
                } else {
                    newList.push(execPlan[key]);
                }
            }
        });
        return [newList, existingList];
    }, [execPlan]);

    const newResources = newAndExistingResources[0];
    const existingResources = newAndExistingResources[1];

    const getDiffViewDom = useCallback((plan: IExecutionPlan) => {
        // sort keys in order to get diff
        let oldObject = null;
        if (plan.currentResource) {
            oldObject = sortObjectByKeys(plan.currentResource, true);
        }
        const newObject = sortObjectByKeys(plan.proposedResource, true);
        return (
            <Accordion style={{ marginBottom: '12px' }}>
                <AccordionSummary expandIcon={<ExpandMore />} style={{ background: '#f6f4f4' }}>
                    <Box>
                        <span>{oldObject ? 'Existing Resource: ' : 'New Resource: '}</span>
                        <b>{newObject.id ?? oldObject?.id}</b>
                    </Box>
                </AccordionSummary>
                <AccordionDetails>
                    <ReactDiffViewer
                        oldValue={oldObject ? JSON.stringify(oldObject, null, 2) : ''}
                        newValue={JSON.stringify(newObject, null, 2)}
                        splitView={true}
                        hideLineNumbers={true}
                        showDiffOnly={true}
                        compareMethod={DiffMethod.WORDS}
                    />
                </AccordionDetails>
            </Accordion>
        );
    }, []);

    return (
        <Box>
            {existingResources.length > 0 && (
                <>
                    {existingResources.map((res, i) => (
                        <Box key={i + '-' + res.proposedResource.id}>{getDiffViewDom(res)}</Box>
                    ))}
                </>
            )}
            {newResources.length > 0 && (
                <>
                    {newResources.map((res, i) => (
                        <Box key={i + '-' + res.proposedResource.id}>{getDiffViewDom(res)}</Box>
                    ))}
                </>
            )}
        </Box>
    );
};

export default ExecPlanJsonDiff;
