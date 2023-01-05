import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Autocomplete,
    Box,
    IconButton,
    InputBase,
    Link,
    Paper,
    TextField,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TablePagination,
    Typography,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useDispatch, useSelector } from 'react-redux';
import { selectBuilderState, setResourceDefinitions } from '../../store/slice/builder';
import { useSnackBar } from '../../context/SnackbarContext';

interface IResourcesProps {}

interface ResourceSummary {
    id: string;
    type: string;
    project: string;
    owner: string;
}

const ResourcesPage: React.FC<IResourcesProps> = ({}) => {
    const { resourceDefinitions } = useSelector(selectBuilderState);
    const [projectOptions, setProjectOptions] = useState<string[]>([]);
    const [ownerOptions, setOwnerOptions] = useState<string[]>([]);
    const [name, setName] = useState('');
    const [type, setType] = useState<string | null>(null);
    const [project, setProject] = useState<string | null>(null);
    const [owner, setOwner] = useState<string | null>(null);
    const [resources, setResources] = useState<ResourceSummary[]>([]);
    const [page, setPage] = useState(0);
    const [total, setTotal] = useState(-1);
    const [search, setSearch] = useState('');
    const pageSize = 15;
    const dispatch = useDispatch();
    const { showSnackbar } = useSnackBar();

    useEffect(() => {
        fetch('/api/v2/resources/definitions')
            .then((response) => response.json())
            .then((json) => {
                dispatch(setResourceDefinitions(json));
            })
            .catch((error) => {
                console.error('error', error);
                showSnackbar({
                    type: 'error',
                    message: 'Looks like we are disconnected from the server',
                });
            });

        fetch('/api/v2/resources/allprojects')
            .then((response) => response.json())
            .then((json) => {
                setProjectOptions(json);
            })
            .catch((error) => {
                console.error('error', error);
                showSnackbar({
                    type: 'error',
                    message: 'Looks like we are disconnected from the server',
                });
            });

        fetch('/api/v2/resources/allowners')
            .then((response) => response.json())
            .then((json) => {
                setOwnerOptions(json);
            })
            .catch((error) => {
                console.error('error', error);
                showSnackbar({
                    type: 'error',
                    message: 'Looks like we are disconnected from the server',
                });
            });
    }, []);

    useEffect(() => {
        getReources(name, type, project, owner, page, pageSize);
    }, [page, type, name, project, owner]);

    const getReources = useCallback(
        (
            name: string | null,
            type: string | null,
            project: string | null,
            owner: string | null,
            page: number = 0,
            pageSize: number = 20
        ) => {
            let queryString = `pageNo=${page}&pageSize=${pageSize}`;

            if (name) {
                queryString += `&idContent=${name}`;
            }
            if (type) {
                queryString += `&resourceDefinitionClass=${type}`;
            }
            if (project) {
                queryString += `&project=${project}`;
            }
            if (owner) {
                queryString += `&owner=${owner}`;
            }

            fetch('/api/v2/resources/fullsearch?' + queryString)
                .then((response) => response.json())
                .then((json) => {
                    setResources(json);
                })
                .catch((error) => {
                    console.error('error', error);
                })
                .finally(() => {});
        },
        []
    );

    const resourceTypeOptions = useMemo(
        () =>
            Object.entries(resourceDefinitions?.resourceMap || []).map(([key, value]) => ({
                label: value.simpleName,
                value: key,
            })),
        [resourceDefinitions]
    );

    const onSearch = () => {
        setPage(0);
        setTotal(-1);
        setName(search);
    };

    return (
        <Box
            sx={{
                width: '100%',
                display: 'flex',
                padding: '12px',
                alignItems: 'flex-start',
            }}
        >
            <Paper
                sx={{
                    width: '250px',
                    padding: '12px',
                }}
            >
                <Typography>Filters</Typography>
                <Autocomplete
                    disablePortal
                    id="autocomplete-type"
                    options={resourceTypeOptions}
                    sx={{ marginY: 2 }}
                    renderInput={(params) => <TextField {...params} size="small" label="Type" />}
                    onChange={(event, value) => {
                        setType(value?.value || null);
                        setPage(0);
                    }}
                />
                <Autocomplete
                    disablePortal
                    id="autocomplete-project"
                    options={projectOptions}
                    sx={{ marginY: 2 }}
                    renderInput={(params) => <TextField {...params} size="small" label="Project" />}
                    onChange={(event, value) => {
                        setProject(value);
                        setPage(0);
                    }}
                />
                <Autocomplete
                    disablePortal
                    id="autocomplete-owner"
                    options={ownerOptions}
                    sx={{ marginY: 2 }}
                    renderInput={(params) => <TextField {...params} size="small" label="Owner LDAP Group" />}
                    onChange={(event, value) => {
                        setOwner(value);
                        setPage(0);
                    }}
                />
            </Paper>
            <Box
                sx={{
                    marginLeft: '12px',
                    flex: 1,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <Paper sx={{ p: '2px 4px', display: 'flex', alignItems: 'center' }}>
                    <InputBase
                        type="search"
                        sx={{ ml: 1, flex: 1 }}
                        placeholder="Search Resource by Name"
                        onChange={(event) => {
                            setSearch(event.target.value);
                        }}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter') {
                                onSearch();
                            }
                        }}
                    />
                    <IconButton type="submit" sx={{ p: '10px' }} aria-label="search" onClick={onSearch}>
                        <SearchIcon />
                    </IconButton>
                </Paper>
                <TablePagination
                    rowsPerPageOptions={[pageSize]}
                    component="div"
                    count={total}
                    rowsPerPage={pageSize}
                    page={page}
                    onPageChange={(event, newPage) => {
                        setPage(newPage);
                    }}
                />
                <TableContainer
                    component={Paper}
                    sx={{
                        marginTop: '12px',
                    }}
                >
                    <Table sx={{ minWidth: 650 }} aria-label="simple table">
                        <TableHead>
                            <TableRow>
                                <TableCell>Name(PRN)</TableCell>
                                <TableCell align="right">Type</TableCell>
                                <TableCell align="right">Project</TableCell>
                                <TableCell align="right">Owner</TableCell>
                                <TableCell align="right"></TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {resources.map((row) => (
                                <TableRow hover key={row.id} sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                                    <TableCell component="th" scope="row">
                                        {row.id}
                                    </TableCell>
                                    <TableCell align="right">
                                        {resourceDefinitions?.resourceMap[row.type]?.simpleName}
                                    </TableCell>
                                    <TableCell align="right">{row.project}</TableCell>
                                    <TableCell align="right">{row.owner}</TableCell>
                                    <TableCell align="right">
                                        <Link target="_blank" href={`/builder/resource/${row.id}`}>
                                            Details
                                        </Link>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Box>
        </Box>
    );
};

export default ResourcesPage;
