import React, { useState } from 'react';
import 'typeface-roboto';
// TODO: change this to use mui v5
import { Typography, Box, Tabs, Tab, CssBaseline, AppBar } from '@material-ui/core/';
import BuilderView from './components/builder/BuilderView';
import Executions from './components/execution/Executions';
import HumanTasks from './components/task/HumanTasks';
import ResourceDefinitionStudio from './topology/ResourceDefinitionStudio';
import Resources from './components/resource/Resources';
import { Link as RouterLink, Navigate, Routes, Route, useParams } from 'react-router-dom';
import { useStyles } from './AppStyles';
import MgmtView from './topology/MgmtView';
import ResourceSearch from './components/common/ResourceSearch';
import 'reactflow/dist/style.css';
import './App.scss';

interface TTabConfig {
    subpath: string;
    component: React.FC;
    label: string;
}

const routes: TTabConfig[] = [
    {
        subpath: 'builder',
        component: BuilderView,
        label: 'Builder',
    },
    {
        subpath: 'executions',
        component: Executions,
        label: 'Executions',
    },
    {
        subpath: 'tasks',
        component: HumanTasks,
        label: 'Tasks',
    },
    {
        subpath: 'rstudio',
        component: ResourceDefinitionStudio,
        label: 'R Studio',
    },
    {
        subpath: 'resources',
        component: Resources,
        label: 'Resources',
    },
];

interface ISlateAppProps {}

const App: React.FC<ISlateAppProps> = () => {
    const [tabs, setTabs] = useState<TTabConfig[]>(routes);
    const [selectedTab, setSelectedTab] = useState('builder');
    const classes = useStyles();

    React.useEffect(() => {
        fetch('/api/v2/mgmt/isadmin')
            .then((response) => response.json())
            .then((json) => {
                if (json) {
                    setTabs([
                        ...tabs,
                        {
                            subpath: 'mgmt',
                            component: MgmtView,
                            label: 'Mgmt View',
                        },
                    ]);
                }
            });
    }, []);

    const NavTabs = () => {
        return (
            <Tabs
                value={selectedTab}
                onChange={(evt, val) => {
                    setSelectedTab(val);
                }}
                style={{ maxWidth: '100%' }}
                variant="scrollable"
                scrollButtons="auto"
            >
                {tabs.map((route, idx) => (
                    <Tab
                        id={route.subpath}
                        key={idx}
                        value={route.subpath}
                        label={route.label}
                        to={'/' + route.subpath}
                        component={RouterLink}
                    />
                ))}
            </Tabs>
        );
    };

    const appBarHeight = '48px';
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <CssBaseline />
            <AppBar
                id="appBar"
                position="static"
                className={classes.appBar}
                style={{ zIndex: 1201, height: appBarHeight }}
            >
                <Box display="flex" alignItems="center">
                    <Box display="flex" alignItems="center" mx={2}>
                        <Typography variant="h6" style={{ paddingLeft: '10px' }}>
                            Slate
                        </Typography>
                    </Box>
                    <NavTabs />
                    <ResourceSearch />
                </Box>
            </AppBar>
            <Box
                sx={{
                    flex: 1,
                    display: 'flex',
                    overflow: 'hidden',
                }}
            >
                <Routes>
                    <Route path="/" element={<Navigate to="/builder" replace />} />
                    <Route path={'/builder'} element={<BuilderView />} />
                    <Route path={'/builder/resource/:resourceId'} element={<BuilderView />} />
                    <Route path={'/executions/:executionId'} element={<Executions />} />
                    <Route path={'/executions'} element={<Executions />} />
                    <Route path={'/tasks'} element={<HumanTasks />} />
                    <Route path={'/rstudio'} element={<ResourceDefinitionStudio />} />
                    <Route path={'/resources'} element={<Resources />} />
                    <Route path={'/mgmt'} element={<MgmtView />} />
                </Routes>
            </Box>
        </Box>
    );
};

export default App;
