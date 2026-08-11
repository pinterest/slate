import React, { useEffect, useState } from 'react';
import { Button } from '@mui/material';

type ApprovalReviewButtonConfig = Record<string, string>;

const SAFE_ATTRIBUTE_PATTERN = /^(data|aria)-[a-z0-9_.:-]+$/;
const SAFE_CLASS_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;
const URL_PLACEHOLDER = '{url}';

interface IApprovalReviewButtonProps {
    executionUrl?: string;
}

const ApprovalReviewButton: React.FC<IApprovalReviewButtonProps> = ({ executionUrl }) => {
    const [config, setConfig] = useState<ApprovalReviewButtonConfig>({});

    useEffect(() => {
        let mounted = true;

        fetch('/api/v2/mgmt/approval-review-button')
            .then((response) => (response.ok ? response.json() : {}))
            .then((data) => {
                if (
                    mounted &&
                    data &&
                    typeof data === 'object' &&
                    !Array.isArray(data) &&
                    Object.values(data).every((value) => typeof value === 'string')
                ) {
                    setConfig(data as ApprovalReviewButtonConfig);
                }
            })
            .catch(() => {
                // The approval review button is optional.
            });

        return () => {
            mounted = false;
        };
    }, []);

    const { label, icon, title, className = '', ...attributes } = config;
    if (!label) {
        return null;
    }

    const url = executionUrl || window.location.href;
    const safeAttributes = Object.entries(attributes).reduce<Record<string, string>>((result, [name, value]) => {
        if (SAFE_ATTRIBUTE_PATTERN.test(name) && typeof value === 'string') {
            result[name] = name === 'data-pre-prompt' ? value.split(URL_PLACEHOLDER).join(url) : value;
        }
        return result;
    }, {});
    const safeClassName = className
        .split(/\s+/)
        .filter((name) => SAFE_CLASS_NAME_PATTERN.test(name))
        .join(' ');

    return (
        <Button
            className={safeClassName}
            {...safeAttributes}
            aria-label={safeAttributes['aria-label'] ?? label}
            title={title}
            variant="contained"
            size="small"
            sx={{
                textTransform: 'none',
                flexShrink: 0,
                backgroundColor: '#ed6c02',
                '&:hover': {
                    backgroundColor: '#c55a02',
                },
            }}
        >
            {icon && (
                <span aria-hidden="true" style={{ marginRight: 8 }}>
                    {icon}
                </span>
            )}
            {label}
        </Button>
    );
};

export default ApprovalReviewButton;
