import React, { useMemo, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Dialog, DialogTitle, DialogContent, IconButton, Typography, Button } from '@mui/material';
import ExecutionTable from './ExecutionTable';
import { IExecutionGraph } from './types';
import { useStyles } from '../../AppStyles';
import CloseIcon from '@mui/icons-material/Close';
import ExecutionDetails from './ExecutionDetails';

const ACTIVE_EXECUTIONS = 5000;
const RECENT_EXECUTIONS = 60000;

interface IExecutionsProps {}

const Executions: React.FC<IExecutionsProps> = () => {
    const classes = useStyles();
    const navigate = useNavigate();
    const searchParams: { executionId?: string } = useParams();
    const { executionId } = searchParams;

    const showRequester = useMemo(() => {
        return executionId != null;
    }, []);

    const [activeExecutions, setActiveExecutions] = useState<Array<IExecutionGraph>>([]);
    const [recentExecutions, setRecentExecutions] = useState<Array<IExecutionGraph>>([]);

    const fetchMyRecentExecutions = () => {
        fetch('/api/v2/graphs/my/recent', {
            method: 'GET',
        })
            .then((response) => response.json())
            .then((json) => {
                setRecentExecutions(json);
            });
    };

    const fetchMyActiveExecutions = () => {
        fetch('/api/v2/graphs/my/active', {
            method: 'GET',
        })
            .then((response) => response.json())
            .then((json) => {
                setActiveExecutions(json);
            });
    };

    const fetchExecution = (executionId: string) => {
        fetch('/api/v2/graphs/' + executionId, {
            method: 'GET',
        })
            .then((response) => response.json())
            .then((json) => {
                setActiveExecutions([json]);
            });
    };

    useEffect(() => {
        const intervals: Array<ReturnType<typeof setInterval>> = [];
        if (!executionId) {
            fetchMyRecentExecutions();
            fetchMyActiveExecutions();
            intervals.push(
                setInterval(() => {
                    fetchMyActiveExecutions();
                }, ACTIVE_EXECUTIONS)
            );

            intervals.push(
                setInterval(() => {
                    fetchMyRecentExecutions();
                }, RECENT_EXECUTIONS)
            );
        } else {
            fetchExecution(executionId);
        }
        return () => {
            intervals.forEach((i) => {
                clearInterval(i);
            });
        };
    }, [executionId]);

    const closeDialog = () => {
        navigate('/executions');
    };

    return (
        <Box display="flex" flexDirection="column" width="100%" marginLeft={'10px'} marginRight={'10px'}>
            <h2>Active Executions</h2>
            <ExecutionTable myExecutions={activeExecutions} showRequester={showRequester} />
            <hr />
            <h2>Recent Executions</h2>
            <ExecutionTable myExecutions={recentExecutions} />
            <Dialog
                classes={{
                    paper: classes.executionDialogPaper,
                }}
                open={executionId != null}
                onClose={closeDialog}
            >
                <DialogTitle sx={{ padding: '8px 24px', background: '#f7f4f4' }}>
                    <Box display="flex" justifyContent="space-between">
                        <Box display="flex" flexDirection="row" alignItems="center">
                            <IconButton edge="start" color="inherit" onClick={closeDialog} aria-label="close">
                                <CloseIcon />
                            </IconButton>
                            <Typography marginLeft={2}>
                                Execution: <b>{executionId}</b>
                            </Typography>
                        </Box>
                        <Box>
                            <Button variant="outlined" onClick={closeDialog}>
                                Close
                            </Button>
                        </Box>
                    </Box>
                </DialogTitle>
                <DialogContent sx={{ padding: 0 }}>
                    {executionId && <ExecutionDetails executionId={executionId} />}
                </DialogContent>
            </Dialog>
        </Box>
    );
};

export default Executions;
