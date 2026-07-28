import React, { useEffect, useState } from 'react';

interface UiAction {
    id: string;
    label: string;
    icon?: string;
    title?: string;
    classNames?: string[];
    attributes?: Record<string, string>;
}

const SAFE_ATTRIBUTE_PATTERN = /^(data|aria)-[a-z0-9_.:-]+$/;
const SAFE_CLASS_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

const getSafeAttributes = (attributes: UiAction['attributes']): Record<string, string> => {
    return Object.entries(attributes ?? {}).reduce<Record<string, string>>((safeAttributes, [name, value]) => {
        if (SAFE_ATTRIBUTE_PATTERN.test(name) && typeof value === 'string') {
            safeAttributes[name] = value;
        }
        return safeAttributes;
    }, {});
};

const getSafeClassNames = (classNames: UiAction['classNames']): string[] => {
    return (classNames ?? []).filter((className) => SAFE_CLASS_NAME_PATTERN.test(className));
};

const UiActions: React.FC = () => {
    const [actions, setActions] = useState<UiAction[]>([]);

    useEffect(() => {
        let mounted = true;

        fetch('/api/v2/ui/actions')
            .then((response) => (response.ok ? response.json() : []))
            .then((data) => {
                if (mounted && Array.isArray(data)) {
                    setActions(data);
                }
            })
            .catch(() => {
                // UI actions are optional; Slate remains usable if they cannot be loaded.
            });

        return () => {
            mounted = false;
        };
    }, []);

    if (actions.length === 0) {
        return null;
    }

    return (
        <div className="slate-ui-actions">
            {actions.map((action) => {
                const attributes = getSafeAttributes(action.attributes);
                const classNames = ['slate-ui-action', ...getSafeClassNames(action.classNames)].join(' ');

                return (
                    <button
                        key={action.id}
                        type="button"
                        className={classNames}
                        {...attributes}
                        aria-label={attributes['aria-label'] ?? action.label}
                        title={action.title}
                    >
                        {action.icon && (
                            <span className="slate-ui-action__icon" aria-hidden="true">
                                {action.icon}
                            </span>
                        )}
                        {action.label}
                    </button>
                );
            })}
        </div>
    );
};

export default UiActions;
