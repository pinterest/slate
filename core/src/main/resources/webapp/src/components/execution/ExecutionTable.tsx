import React, { useEffect, useState } from 'react';
import {
    Table,
    TableBody,
    TableContainer,
    TableCell,
    TableHead,
    TableRow,
    Paper,
    Box,
    Chip,
    Link,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useRowStyles } from '../../topology/MgmtView';
import { useStyles } from '../../AppStyles';
import { IExecutionGraph } from './types';

interface IExecutionTableProps {
    myExecutions: Array<IExecutionGraph>;
    showRequester?: boolean;
}

const ExecutionTable: React.FC<IExecutionTableProps> = ({ myExecutions, showRequester }) => {
    const classes = useStyles();
    const rowStyles = useRowStyles();
    const navigate = useNavigate();

    const openDetailDialog = (execId: string) => {
        navigate(`/executions/${execId}`);
    };

    return (
        <Box>
            <TableContainer component={Paper}>
                <Table className={classes.table} aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell>Details</TableCell>
                            <TableCell>Execution Id</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Start Time</TableCell>
                            <TableCell>End Time</TableCell>
                            {showRequester && <TableCell>Requester</TableCell>}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {myExecutions.map((row) => (
                            <TableRow
                                className={`${rowStyles.root} ${classes.execTableRow}`}
                                key={row.executionId}
                                onClick={() => {
                                    openDetailDialog(row.executionId);
                                }}
                            >
                                <TableCell>
                                    <Link
                                        component="button"
                                        variant="body2"
                                        onClick={() => {
                                            openDetailDialog(row.executionId);
                                        }}
                                    >
                                        View Graph
                                    </Link>
                                </TableCell>
                                <TableCell component="th" scope="row">
                                    {row.executionId}
                                </TableCell>
                                <TableCell>
                                    <Chip
                                        label={row.status}
                                        variant="outlined"
                                        color={
                                            row.status == 'SUCCEEDED'
                                                ? 'success'
                                                : row.status == 'FAILED'
                                                ? 'error'
                                                : row.status == 'RUNNING'
                                                ? 'primary'
                                                : 'secondary'
                                        }
                                    />
                                </TableCell>
                                <TableCell>{new Date(row.startTime + ' GMT').toLocaleString()}</TableCell>
                                <TableCell>
                                    {row.endTime ? new Date(row.endTime + ' GMT').toLocaleString() : ''}
                                </TableCell>
                                {showRequester && <TableCell>{row.requester}</TableCell>}
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

export default ExecutionTable;
