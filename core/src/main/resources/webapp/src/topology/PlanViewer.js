import * as React from 'react';
import { Alert, TabContext, TabList, TabPanel } from '@material-ui/lab';
import { Box, Tab, Typography, Link, IconButton, Tooltip } from '@material-ui/core';
import JSONPretty from 'react-json-pretty';
import { isEqual } from 'lodash';
import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer';
import { ContentCopy } from '@mui/icons-material';
import JsonPrettier from '../components/common/JsonPrettier';
import ExecPlanJsonDiff from '../components/execution/ExecPlanJsonDiff';
import PlanGraph from '../components/execution/PlanGraph';

export default function PlanViewer(props) {
    const { planInfo, previousPlanInfo, defaultTab } = props;
    const { plan, executionGraph, deltaGraph, error: planError } = planInfo;
    const { deltaGraph: previousDeltaGraph } = previousPlanInfo ?? {};
    const height = props.height ?? '400px';
    const width = props.width ?? '800px';
    const dagWidth = props.dagWidth ?? '800px';
    const enableStatus = props.enableStatus ?? false;
    const hideMessage = planInfo.hideMessage ?? false;
    const isExecution = planInfo.isExecution ?? false;
    const [tabValue, setTabValue] = React.useState(defaultTab ?? 'plan_graph');
    const [showJsonDiffView, setShowJsonDiffView] = React.useState(false);

    React.useEffect(() => {
        if (previousDeltaGraph && deltaGraph && !isEqual(deltaGraph, previousDeltaGraph)) {
            setShowJsonDiffView(true);
        } else {
            setShowJsonDiffView(false);
        }
    }, [previousDeltaGraph, deltaGraph]);

    const tabs = React.useMemo(() => {
        const tabsObj = {};
        tabsObj['plan_graph'] = 'Plan Graph';
        tabsObj['plan_json'] = 'Plan JSON';
        if (deltaGraph) {
            tabsObj['delta_graph'] = 'Requested Delta Graph';
        }
        if (showJsonDiffView) {
            tabsObj['json_diff_view'] = 'View Changes';
        }
        tabsObj['resource_diff'] = 'Resource Changes';
        return tabsObj;
    }, [deltaGraph, showJsonDiffView]);

    const renderPlanGraph = (plan, error) => {
        if (!plan) {
            return (
                <>
                    <JSONPretty json={error} stringStyle="white-space: pre-wrap; word-wrap: break-word" />
                    <Typography>
                        Please share json from REQUESTED DELTA GRAPH tab so the Resource authors can help.
                    </Typography>
                </>
            );
        }

        return (
            <Box display="flex" flex="1" flexDirection="column" width={width} height={height} paddingTop={1}>
                {!hideMessage && (
                    <Box margin={1}>
                        <Alert severity="success">
                            {isExecution && executionGraph?.executionId ? (
                                <Box>
                                    {`Execution submitted successfully with ID: `}
                                    <Link href={`/executions/${executionGraph?.executionId}`} target="_blank">
                                        {executionGraph?.executionId}
                                    </Link>
                                    {`. Please`}
                                    <Link href={`/executions/${executionGraph?.executionId}`} target="_blank">
                                        {' click here '}
                                    </Link>
                                    to view the status.
                                    <Tooltip title="Copy execution ID" placement="top" arrow>
                                        <IconButton
                                            style={{ padding: '0', paddingLeft: '4px' }}
                                            component="span"
                                            aria-label="Copy execution ID"
                                            onClick={() => {
                                                navigator.clipboard.writeText(executionGraph?.executionId);
                                            }}
                                        >
                                            <ContentCopy fontSize="small" />
                                        </IconButton>
                                    </Tooltip>
                                </Box>
                            ) : (
                                'Plan was successful'
                            )}
                        </Alert>
                    </Box>
                )}
                <PlanGraph
                    plan={plan}
                    width={width}
                    height={height}
                    dagWidth={dagWidth}
                    showExecStatus={enableStatus}
                />
            </Box>
        );
    };

    const diffViewDom = React.useMemo(() => {
        if (!showJsonDiffView) {
            return null;
        }
        return (
            <ReactDiffViewer
                oldValue={JSON.stringify(previousDeltaGraph, null, 2)}
                newValue={JSON.stringify(deltaGraph, null, 2)}
                splitView={true}
                hideLineNumbers={true}
                showDiffOnly={true}
                compareMethod={DiffMethod.WORDS}
            />
        );
    }, [deltaGraph, previousDeltaGraph, showJsonDiffView]);

    let renderPlanGraphTab = renderPlanGraph(plan, planError);

    return (
        <Box display="flex" flexDirection="column" width={width} height={height}>
            <TabContext value={tabValue}>
                <Box display="flex" flexDirection="column">
                    <TabList
                        onChange={(_, newVal) => {
                            setTabValue(newVal);
                        }}
                    >
                        {Object.entries(tabs).map(([key, value], i) => {
                            return <Tab label={value} value={String(key)} key={i} />;
                        })}
                    </TabList>
                </Box>
                <Box display="flex" flexDirection="column" flex="1" overflow="hidden">
                    <TabPanel value="plan_graph" style={{ padding: '0px', flex: '1', overflow: 'hidden' }}>
                        {renderPlanGraphTab}
                    </TabPanel>
                    <TabPanel
                        value="plan_json"
                        style={{ padding: '0px', paddingTop: '8px', flex: '1', overflow: 'scroll' }}
                    >
                        <JsonPrettier data={plan} />
                    </TabPanel>
                    {deltaGraph && (
                        <TabPanel value="delta_graph" style={{ padding: '0px', paddingTop: '8px', overflow: 'scroll' }}>
                            <JsonPrettier data={deltaGraph} />
                        </TabPanel>
                    )}
                    {showJsonDiffView && (
                        <TabPanel value="json_diff_view" style={{ padding: '0px' }}>
                            <Box>{diffViewDom}</Box>
                        </TabPanel>
                    )}
                    <TabPanel
                        value="resource_diff"
                        style={{ padding: '0px', paddingTop: '8px', flex: '1', overflow: 'scroll' }}
                    >
                        {plan && <ExecPlanJsonDiff execPlan={plan} />}
                    </TabPanel>
                </Box>
            </TabContext>
        </Box>
    );
}
