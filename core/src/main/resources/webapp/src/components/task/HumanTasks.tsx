import React, { useEffect, useState } from 'react';
import {
    Box,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Typography,
} from '@mui/material';
import HumanTaskRow from './HumanTaskRow';
import { ITask } from './types';
import { useStyles } from '../../AppStyles';

interface IHumanTasksProps {}

const HumanTasks: React.FC<IHumanTasksProps> = ({}) => {
    const classes = useStyles();

    const [load, setLoad] = useState<boolean>(false);
    const [myGroupTasks, setMyGroupTasks] = useState<ITask[]>([]);
    const [myTasks, setMyTasks] = useState<ITask[]>([]);

    const fetchGroupTasks = () => {
        fetch('/api/v2/tasks/mygrouptasks', {
            method: 'GET',
        })
            .then((response) => response.json())
            .then((json) => {
                setMyGroupTasks(json);
            });
    };

    const fetchMyTasks = () => {
        fetch('/api/v2/tasks/mytasks', {
            method: 'GET',
        })
            .then((response) => response.json())
            .then((json) => {
                setMyTasks(json);
            });
    };

    useEffect(() => {
        fetchGroupTasks();
        fetchMyTasks();
        let intervalId = setInterval(() => {
            fetchGroupTasks();
            fetchMyTasks();
        }, 5000);
        setLoad(false);
        return () => {
            clearInterval(intervalId);
        };
    }, []);

    return (
        <Box display="flex" flexDirection="column" width="100%" marginLeft={'10px'} marginRight={'10px'}>
            <Typography mt={2} variant="h6" paddingBottom={1}>
                Tasks for me
            </Typography>
            <TableContainer component={Paper}>
                <Table className={classes.tasksTable} aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell>Details</TableCell>
                            <TableCell>Task Type</TableCell>
                            <TableCell>Summary</TableCell>
                            <TableCell>Creation Time</TableCell>
                            <TableCell>Execution Link</TableCell>
                            <TableCell>{''}</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {myTasks.map((row) => (
                            <HumanTaskRow key={row.processId + '_' + row.taskId} row={row} />
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <Typography mt={3} variant="h6" paddingBottom={1}>
                Tasks for my LDAP Groups
            </Typography>
            <TableContainer component={Paper}>
                <Table className={classes.table} aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell>Details</TableCell>
                            <TableCell>Task Type</TableCell>
                            <TableCell>Summary</TableCell>
                            <TableCell>Creation Time</TableCell>
                            <TableCell>Execution Link</TableCell>
                            <TableCell>{''}</TableCell>
                            <TableCell>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {myGroupTasks.map((row) => (
                            <HumanTaskRow key={row.processId + '_' + row.taskId} row={row} />
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

export default HumanTasks;
