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

const LIGHT_COLORS = {
    key: '#0d47a1',
    string: '#2e7d32',
    number: '#b45309',
    boolean: '#7b1fa2',
    null: '#78909c',
};

const escapeHtml = (value: string): string =>
    value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

// Robust JSON syntax highlighter (handles strings with embedded escaped quotes,
// which react-json-pretty's line-based tokenizer cannot). Based on the classic
// MDN JSON.stringify + span-wrapping approach.
const highlightJson = (data: unknown): string => {
    let json: string | undefined;
    try {
        json = JSON.stringify(data, null, 2);
    } catch {
        json = undefined;
    }
    if (typeof json !== 'string') {
        return escapeHtml(String(data));
    }
    return escapeHtml(json).replace(
        /("(\\u[a-fA-F0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g,
        (match) => {
            let color: string = LIGHT_COLORS.number;
            if (match[0] === '"') {
                color = /:\s*$/.test(match) ? LIGHT_COLORS.key : LIGHT_COLORS.string;
            } else if (match === 'true' || match === 'false') {
                color = LIGHT_COLORS.boolean;
            } else if (match === 'null') {
                color = LIGHT_COLORS.null;
            }
            return `<span style="color:${color}">${match}</span>`;
        }
    );
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
            {isLight ? (
                <pre
                    style={{
                        margin: 0,
                        padding: '8px 32px 8px 8px',
                        lineHeight: 1.4,
                        color: '#37474f',
                        background: '#fafafa',
                        whiteSpace: 'pre-wrap',
                        overflowWrap: 'anywhere',
                        wordBreak: 'break-word',
                        fontFamily:
                            'ui-monospace, SFMono-Regular, Menlo, Consolas, "Liberation Mono", monospace',
                        fontSize: '13px',
                    }}
                    dangerouslySetInnerHTML={{ __html: highlightJson(data) }}
                />
            ) : (
                <JSONPretty
                    style={{ overflow: 'auto', padding: '0px' }}
                    data={data}
                    theme={JSONPrettyMon}
                    mainStyle="margin:0"
                    stringStyle={stringStyle}
                />
            )}
        </Box>
    );
};

export default JsonPrettier;
