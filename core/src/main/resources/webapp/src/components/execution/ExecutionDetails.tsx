import React, { useEffect, useState } from 'react';
import { Alert, Box } from '@mui/material';
import PlanViewer from '../../topology/PlanViewer';
import { IExecutionGraph, IExecutionPlan, TExecutionDetailTabKey } from './types';

const UPDATE_FREQUENCY = 2000;

interface IExecutionDetailProps {
    executionId: string;
    defaultTab?: TExecutionDetailTabKey;
}

interface IExecutionInfo {
    executionGraph: IExecutionGraph;
    plan: IExecutionPlan;
    hideMessage: boolean;
}

const ExecutionDetails: React.FC<IExecutionDetailProps> = ({ executionId, defaultTab }) => {
    const [executionInfo, setExecutionInfo] = useState<null | IExecutionInfo>(null);
    const [errorMsg, setErrorMsg] = useState<null | string>(null);

    const fetchExecutionId = () => {
        setErrorMsg(null);
        fetch('/api/v2/graphs/' + executionId, {
            method: 'GET',
        })
            .then((response) => {
                if (response.status == 200) {
                    return response.json();
                } else {
                    throw Error(response.statusText);
                }
            })
            .then((json) => {
                setExecutionInfo({
                    executionGraph: json,
                    plan: json.executionPlan,
                    hideMessage: true,
                });
            })
            .catch((error) => {
                console.error('error', error);
                setErrorMsg(error?.message ?? error);
            });
    };

    useEffect(() => {
        // fetch details on load
        fetchExecutionId();
    }, []);

    useEffect(() => {
        if (!executionInfo) {
            return;
        }
        const enableUpdates = executionInfo.executionGraph.status == 'RUNNING';
        let intervalId: null | ReturnType<typeof setInterval> = null;
        if (enableUpdates) {
            intervalId = setInterval(() => {
                fetchExecutionId();
            }, UPDATE_FREQUENCY);
        }
        return () => {
            if (intervalId) {
                clearInterval(intervalId);
            }
        };
    }, [executionInfo]);

    if (errorMsg) {
        return <Alert severity="error">{errorMsg}</Alert>;
    }
    return (
        <Box
            padding={2}
            paddingTop={0}
            display="flex"
            sx={{
                width: '100%',
                height: '100%',
                overflow: 'scroll',
            }}
        >
            {executionInfo && (
                <PlanViewer
                    planInfo={executionInfo}
                    enableStatus={true}
                    defaultTab={defaultTab}
                    height="100%"
                    width="100%"
                    dagWidth="800px"
                />
            )}
        </Box>
    );
};

export default ExecutionDetails;
