import { Box, Button, Link, Tooltip, Typography } from '@material-ui/core';
import React, { useEffect, useState } from 'react';
import LaunchIcon from '@material-ui/icons/Launch';

const RenderTools = ({ resource }) => {
    const [tools, setTools] = useState(null);
    const [currentTool, setCurrentTool] = useState(null);

    useEffect(() => {
        fetch('/api/v2/resources/tools/' + resource.id)
            .then((response) => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error('Resource fetch failed for:' + resource.id);
                }
            })
            .then((data) => {
                setTools(data);
            })
            .catch((error) => {
                console.log(error);
            });
    });

    const renderTools = () => {
        return (
            <Box
                sx={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <Box>
                    {tools.map((t) => (
                        <Tooltip title={t.description} key={'tool' + t.label}>
                            <Button
                                variant="contained"
                                color="primary"
                                onClick={() => {
                                    if (t.embed) {
                                        setCurrentTool(t);
                                    } else {
                                        window.open(t.url, '_blank');
                                    }
                                }}
                                style={{ marginRight: '8px' }}
                            >
                                {t.label}
                            </Button>
                        </Tooltip>
                    ))}
                </Box>
                {currentTool && (
                    <>
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                marginY: 2,
                            }}
                        >
                            <Typography variant="h6">{currentTool.label}</Typography>
                            <Tooltip title="Open in a new window" placement="top">
                                <Link href={currentTool.url} target="_blank">
                                    <LaunchIcon />
                                </Link>
                            </Tooltip>
                        </Box>
                        <Box sx={{ flex: 1 }}>
                            <iframe
                                src={currentTool.url}
                                frameBorder={0}
                                style={{
                                    height: '100%',
                                    width: '100%',
                                    top: 0,
                                    left: 0,
                                    bottom: 0,
                                    right: 0,
                                }}
                                height={'100%'}
                                width={'100%'}
                                seamless
                            />
                        </Box>
                    </>
                )}
            </Box>
        );
    };

    if (tools && tools.length > 0) {
        return renderTools();
    } else {
        return (
            <Typography>
                No tools available for this Resource, please make sure the Resource has been provisioned. If it's
                provisioned and this is empty then likely no tools are provided by the Author.
            </Typography>
        );
    }
};

export default RenderTools;
