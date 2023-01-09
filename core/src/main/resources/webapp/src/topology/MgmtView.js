import React, { useEffect, useState } from 'react';
import { makeStyles } from '@material-ui/core/styles';
import ExecutionTable from './../components/execution/ExecutionTable';
import Box from '@material-ui/core/Box';

const UPDATE_FREQUENCY = 10000;

export const useRowStyles = makeStyles({
    root: {
        '& > *': {
            borderBottom: 'unset',
        },
    },
});

export default function MgmtView() {
    const [allActiveExecutions, setAllActiveExecutions] = useState([]);

    function fetchAllActiveExecutions() {
        fetch('/api/v2/graphs/all', {
            method: 'GET',
        })
            .then((response) => response.json())
            .then((json) => {
                setAllActiveExecutions(json);
            });
    }

    useEffect(() => {
        // fetch executions
        setInterval(() => {
            fetchAllActiveExecutions();
        }, UPDATE_FREQUENCY);
    }, []);

    return (
        <Box display="flex" flexDirection="column" width="100%">
            <h2>Active Executions</h2>
            <ExecutionTable myExecutions={allActiveExecutions} showRequester={true} />
            <hr />
        </Box>
    );
}
