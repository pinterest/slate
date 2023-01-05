import React, { useState, useEffect } from 'react';
import Form from '@rjsf/material-ui';
import JSONInput from 'react-json-editor-ajrm';
import locale from 'react-json-editor-ajrm/locale/en';
import { Box, Drawer, Button, List, ListItem, Tab, Typography, Link } from '@material-ui/core';
import { useStyles } from '../AppStyles.js';
import { TabContext, TabList, TabPanel } from '@material-ui/lab';
import JSONPretty from 'react-json-pretty';
import 'react-json-pretty/themes/monikai.css';
import ProcessDesigner from './ProcessDesigner.js';

var JSONPrettyMon = require('react-json-pretty/dist/monikai');

export default function ResourceDefinitionStudio(props) {
    const [resourceConfigSchema, setResourceConfigSchema] = useState({});
    const [resourceUISchema, setResourceUISchema] = useState({});
    const [resourceSampleData, setResourceSampleData] = useState({});
    const [myResourceDefinitions, setMyResourceDefintions] = useState(null);
    const [urrentResourceDefinition, setCurrentResourceDefinition] = useState(null);
    const [value, setValue] = React.useState('3');
    const handleChange = (event, newValue) => {
        setValue(newValue);
    };
    let schemaTester = renderSchemaTester(
        resourceConfigSchema,
        resourceUISchema,
        resourceSampleData,
        setResourceConfigSchema,
        setResourceUISchema,
        setResourceSampleData
    );

    let processDesigner = renderProcessDesigner();

    useEffect(() => {
        fetchMyResourceDefinitions(setMyResourceDefintions);
    }, []);

    let resourceDefinitions = renderMyResourceDefinitions(
        myResourceDefinitions,
        urrentResourceDefinition,
        setCurrentResourceDefinition
    );
    return (
        <div
            style={{
                marginLeft: '5px',
                height: '85vh',
                // overflow: "hidden",
            }}
        >
            <TabContext value={value}>
                <Box>
                    <TabList onChange={handleChange}>
                        <Tab label="My Resource Definitions" value="1" />
                        <Tab label="Schema Tester" value="2" />
                        <Tab label="Process Designer" value="3" />
                    </TabList>
                </Box>
                <TabPanel value="1">{resourceDefinitions}</TabPanel>
                <TabPanel value="2">{schemaTester}</TabPanel>
                <TabPanel value="3">{processDesigner}</TabPanel>
            </TabContext>
        </div>
    );
}

function renderProcessDesigner() {
    return <ProcessDesigner />;
}

function fetchMyResourceDefinitions(setMyResourceDefintions) {
    fetch('/api/v2/resources/mydefinitions')
        .then((res) => {
            if (res.ok) {
                return res.json();
            } else {
                throw new Error('Error fetching my resource definitions');
            }
        })
        .then((result) => {
            setMyResourceDefintions(result);
        })
        .catch((error) => {
            setMyResourceDefintions(null);
        });
}

function renderMyResourceDefinitions(myResourceDefinitions, currentResourceDefinition, setCurrentResourceDefinition) {
    if (myResourceDefinitions) {
        return (
            <>
                <div
                    style={{
                        float: 'left',
                        width: '15vw',
                        paddingLeft: '0px',
                        height: '80vh',
                    }}
                >
                    <List>
                        {myResourceDefinitions.map((r) => (
                            <ListItem key={r.simpleName}>
                                <Button
                                    style={{ widht: '100%' }}
                                    onClick={() => {
                                        setCurrentResourceDefinition(r);
                                    }}
                                    variant="contained"
                                >
                                    <Box>{r.simpleName}</Box>
                                </Button>
                            </ListItem>
                        ))}
                    </List>
                </div>
                <div
                    style={{
                        float: 'left',
                        width: '70vw',
                        height: '85vh',
                        paddingLeft: '5px',
                    }}
                >
                    {renderResourceDefinition(currentResourceDefinition)}
                </div>
            </>
        );
    } else {
        return (
            <>
                <Typography>There are no resource definitions authored by your team.</Typography>
            </>
        );
    }
}

function renderResourceDefinition(rd) {
    if (rd) {
        return (
            <Box>
                <Box style={{ width: '100%', height: '10vh' }}>
                    <Box style={{ float: 'left' }}>
                        <Typography style={{ fontSize: '10pt' }}>Author: {rd.author}</Typography>
                        <Typography style={{ fontSize: '10pt' }}>Short Description: {rd.shortDescription}</Typography>
                        <Typography style={{ fontSize: '10pt' }}>
                            <Link href={rd.chatLink} target="_blank">
                                Chat Link
                            </Link>
                        </Typography>
                        <Link href={rd.documentationLink} target="_blank">
                            Wiki
                        </Link>
                    </Box>
                </Box>
                <br />
                <hr />
                <Typography style={{ fontSize: '10pt' }}>Config Schema</Typography>
                <JSONPretty
                    style={{
                        height: '65vh',
                        overflow: 'scroll',
                        fontSize: '8pt',
                        padding: '0px',
                    }}
                    id="configSchema"
                    data={rd.configSchema}
                    theme={JSONPrettyMon}
                ></JSONPretty>
            </Box>
        );
    } else {
        return <></>;
    }
}

function renderSchemaTester(
    resourceConfigSchema,
    resourceUISchema,
    resourceSampleData,
    setResourceConfigSchema,
    setResourceUISchema,
    setResourceSampleData
) {
    return (
        <>
            <div style={{ float: 'left' }}>
                <Typography style={{ fontWeight: '600' }}>Config Schema</Typography>
                <JSONInput
                    id="configSchema"
                    placeholder={resourceConfigSchema}
                    locale={locale}
                    onChange={(data) => {
                        if (data.jsObject) {
                            setResourceConfigSchema(data.jsObject);
                        }
                    }}
                    width={'300px'}
                    height={'600px'}
                />
            </div>
            <div style={{ float: 'left', marginLeft: '4px', marginRight: '4px' }}>
                <Typography style={{ fontWeight: '600' }}>UI Schema</Typography>
                <JSONInput
                    id="uiSchema"
                    placeholder={resourceUISchema}
                    locale={locale}
                    onChange={(data) => setResourceUISchema(data.jsObject)}
                    width={'300px'}
                    height={'600px'}
                />
            </div>
            <div
                style={{
                    maxWidth: '300px',
                    padding: '5px',
                    float: 'right',
                    height: '750px',
                    overflow: 'scroll',
                }}
            >
                <Typography style={{ fontWeight: '600' }}>Rendered Result</Typography>
                <Form
                    schema={resourceConfigSchema}
                    uiSchema={resourceUISchema}
                    formData={resourceSampleData}
                    onSubmit={({ formData }, e) => {
                        console.log('Submitted:' + JSON.stringify(formData));
                        setResourceSampleData(formData);
                    }}
                    showErrorList={true}
                    liveValidate
                />
            </div>
        </>
    );
}
