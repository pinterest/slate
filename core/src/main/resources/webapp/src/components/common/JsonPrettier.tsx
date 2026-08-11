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
    variant?: 'dark' | 'light';
}

const lightTheme = {
    main: 'line-height:1.4;color:#37474f;background:#fafafa;overflow:auto;',
    error: 'line-height:1.4;color:#37474f;background:#fafafa;overflow:auto;',
    key: 'color:#0d47a1;',
    string: 'color:#2e7d32;',
    value: 'color:#b45309;',
    boolean: 'color:#7b1fa2;',
};

const JsonPrettier: React.FC<IJsonPrettierProps> = ({
    data,
    height,
    width = '100%',
    stringStyle,
    variant = 'dark',
}: IJsonPrettierProps) => {
    const classes = useStyles();
    const isLight = variant === 'light';
    const resolvedHeight = height ?? (isLight ? 'auto' : '100%');
    return (
        <Box
            height={resolvedHeight}
            style={{
                height: resolvedHeight,
                width: width,
                overflow: isLight ? 'visible' : 'auto',
                position: 'relative',
                border: isLight ? '1px solid #e0e0e0' : undefined,
                borderRadius: isLight ? '4px' : undefined,
            }}
        >
            <Tooltip title="Copy JSON" placement="top" arrow>
                <IconButton
                    style={{
                        padding: '0',
                        paddingLeft: '4px',
                        position: 'absolute',
                        top: '12px',
                        right: '12px',
                        color: isLight ? '#546e7a' : undefined,
                    }}
                    color="info"
                    component="span"
                    onClick={() => {
                        navigator.clipboard.writeText(JSON.stringify(data));
                    }}
                >
                    <ContentCopy fontSize="small" />
                </IconButton>
            </Tooltip>
            <JSONPretty
                style={{ overflow: isLight ? 'visible' : 'auto', padding: '0px' }}
                data={data}
                theme={isLight ? lightTheme : JSONPrettyMon}
                mainStyle="margin:0"
                stringStyle={stringStyle}
            />
        </Box>
    );
};

export default JsonPrettier;
