import React from 'react';
import { Box, IconButton, Tooltip } from '@mui/material';
import { ContentCopy } from '@mui/icons-material';
import { useStyles } from '../../AppStyles';
import JSONPretty from 'react-json-pretty';
// @ts-ignore theme doesn't have typescript support yet
import JSONPrettyMon from 'react-json-pretty/dist/monikai';
import 'react-json-pretty/themes/monikai.css';

interface IJsonPrettierProps {
    data: Object;
    height?: string;
    width?: string;
    stringStyle?: string;
}

const JsonPrettier: React.FC<IJsonPrettierProps> = (data, height = '100%', width = '100%', stringStyle = undefined) => {
    const classes = useStyles();
    return (
        <Box height={height} style={{ height: height, width: width, overflow: 'scroll', position: 'relative' }}>
            <Tooltip title="Copy JSON" placement="top" arrow>
                <IconButton
                    style={{ padding: '0', paddingLeft: '4px', position: 'absolute', top: '12px', right: '12px' }}
                    color="info"
                    component="span"
                    onClick={() => {
                        navigator.clipboard.writeText(JSON.stringify(data.data?data.data:data));
                    }}
                >
                    <ContentCopy fontSize="small" />
                </IconButton>
            </Tooltip>
            <JSONPretty
                style={{ overflow: 'scroll', padding: '0px' }}
                data={data.data?data.data:data}
                theme={JSONPrettyMon}
                mainStyle="margin:0"
                stringStyle={stringStyle}
            />
        </Box>
    );
};

export default JsonPrettier;
