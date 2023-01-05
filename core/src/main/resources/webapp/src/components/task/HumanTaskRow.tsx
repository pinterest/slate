import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import {
    Box,
    Button,
    Collapse,
    Typography,
    IconButton,
    TableCell,
    TableRow,
    Link,
    Stack,
    Dialog,
    DialogTitle,
    DialogContent,
} from '@mui/material';
import { KeyboardArrowDown, KeyboardArrowUp, Close } from '@mui/icons-material';
import { ITask } from './types';
import { TExecutionStatus } from '../execution/types';
import { useStyles } from '../../AppStyles';
import { useSnackBar } from '../../context/SnackbarContext';
import ExecutionDetails from '../execution/ExecutionDetails';

interface IHumanTaskRowProps {
    row: ITask;
}

const HumanTaskRow: React.FC<IHumanTaskRowProps> = ({ row }) => {
    const [open, setOpen] = useState<boolean>(false);
    const classes = useStyles();
    const { showSnackbar } = useSnackBar();
    const [showExecDetailDialog, setShowExecDetailDialog] = useState(false);

    const updateTask = (processId: string, taskId: string, status: TExecutionStatus) => {
        fetch('/api/v2/tasks/' + processId + '/' + taskId + '/' + status, {
            method: 'PUT',
        })
            .then((response) => {
                if (response.status == 200 || response.status == 204) {
                    showSnackbar({
                        type: 'success',
                        message: 'Action is successful',
                    });
                } else {
                    throw Error(response.statusText);
                }
            })
            .catch((error) => {
                console.error('error', error);
                showSnackbar({
                    type: 'error',
                    message: 'Error performing the action',
                });
            });
    };

    const closeDialog = () => {
        setShowExecDetailDialog(false);
    };

    return (
        <React.Fragment>
            <TableRow
                className={classes.tasksTableRow}
                onClick={() => {
                    setOpen(!open);
                }}
            >
                <TableCell>
                    <IconButton
                        aria-label="expand row"
                        size="small"
                        onClick={() => {
                            setOpen(!open);
                        }}
                    >
                        {open ? <KeyboardArrowUp /> : <KeyboardArrowDown />}
                    </IconButton>
                </TableCell>
                <TableCell component="th" scope="row">
                    {row.taskType}
                </TableCell>
                <TableCell>{row.summary}</TableCell>
                <TableCell>{row.createTime}</TableCell>
                <TableCell>
                    <Link href={'/executions/' + row.executionId}>{row.executionId}</Link>
                </TableCell>
                <TableCell>
                    <Link
                        onClick={(e) => {
                            e.stopPropagation();
                            setShowExecDetailDialog(true);
                        }}
                    >
                        View Changes
                    </Link>
                </TableCell>
                <TableCell>
                    {row.taskType == 'APPROVAL' ? (
                        <Stack direction="row" spacing={2}>
                            <Button
                                size="small"
                                variant="outlined"
                                color="success"
                                onClick={(e) => {
                                    e && e.stopPropagation();
                                    updateTask(row.processId, row.taskId, 'SUCCEEDED');
                                }}
                            >
                                Approve
                            </Button>
                            <Button
                                size="small"
                                variant="outlined"
                                color="error"
                                onClick={(e) => {
                                    e && e.stopPropagation();
                                    updateTask(row.processId, row.taskId, 'FAILED');
                                }}
                            >
                                Deny
                            </Button>
                        </Stack>
                    ) : (
                        <div />
                    )}
                </TableCell>
            </TableRow>
            <TableRow>
                <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={6}>
                    <Collapse in={open} timeout="auto" unmountOnExit>
                        <Box margin={1}>
                            <Typography variant="h6" gutterBottom component="div">
                                Description
                            </Typography>
                            <div
                                style={{
                                    width: '80vw',
                                    overflow: 'scroll',
                                }}
                            >
                                <ReactMarkdown children={row.description} />
                            </div>
                        </Box>
                    </Collapse>
                </TableCell>
            </TableRow>
            <Dialog
                classes={{
                    paper: classes.executionDialogPaper,
                }}
                open={showExecDetailDialog}
                onClose={closeDialog}
            >
                <DialogTitle sx={{ padding: '8px 24px', background: '#f7f4f4' }}>
                    <Box display="flex" justifyContent="space-between">
                        <Box display="flex" flexDirection="row" alignItems="center">
                            <IconButton edge="start" color="inherit" onClick={closeDialog} aria-label="close">
                                <Close />
                            </IconButton>
                            <Typography marginLeft={2}>
                                Execution: <b>{row.executionId}</b>
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
                    {row.executionId && <ExecutionDetails executionId={row.executionId} defaultTab="resource_diff" />}
                </DialogContent>
            </Dialog>
        </React.Fragment>
    );
};

export default HumanTaskRow;
