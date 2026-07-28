import React, { useEffect, useState } from 'react';
import { Fab } from '@material-ui/core';

type OnboardingButtonConfig = Record<string, string>;

const SAFE_ATTRIBUTE_PATTERN = /^(data|aria)-[a-z0-9_.:-]+$/;
const SAFE_CLASS_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

const OnboardingButton: React.FC = () => {
    const [config, setConfig] = useState<OnboardingButtonConfig>({});

    useEffect(() => {
        let mounted = true;

        fetch('/api/v2/mgmt/onboarding-button')
            .then((response) => (response.ok ? response.json() : {}))
            .then((data) => {
                if (
                    mounted &&
                    data &&
                    typeof data === 'object' &&
                    !Array.isArray(data) &&
                    Object.values(data).every((value) => typeof value === 'string')
                ) {
                    setConfig(data as OnboardingButtonConfig);
                }
            })
            .catch(() => {
                // The onboarding button is optional.
            });

        return () => {
            mounted = false;
        };
    }, []);

    const { label, icon, title, className = '', ...attributes } = config;
    if (!label) {
        return null;
    }

    const safeAttributes = Object.entries(attributes).reduce<Record<string, string>>((result, [name, value]) => {
        if (SAFE_ATTRIBUTE_PATTERN.test(name) && typeof value === 'string') {
            result[name] = value;
        }
        return result;
    }, {});
    const safeClassName = className
        .split(/\s+/)
        .filter((name) => SAFE_CLASS_NAME_PATTERN.test(name))
        .join(' ');

    return (
        <Fab
            variant="extended"
            color="primary"
            className={safeClassName}
            {...safeAttributes}
            aria-label={safeAttributes['aria-label'] ?? label}
            title={title}
            style={{
                position: 'fixed',
                right: 24,
                bottom: 180,
                zIndex: 1300,
                fontWeight: 600,
                textTransform: 'none',
            }}
        >
            {icon && (
                <span aria-hidden="true" style={{ marginRight: 8 }}>
                    {icon}
                </span>
            )}
            {label}
        </Fab>
    );
};

export default OnboardingButton;
