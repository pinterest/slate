import React from 'react';

const SLATE_ONBOARDING_AGENT_ID = '019f94de-ede7-7968-9397-7c300ca49a69';

const HelixUltraButton: React.FC = () => (
    <button
        type="button"
        className="helix-ultra-button slate-helix-ultra-button"
        data-agent-id={SLATE_ONBOARDING_AGENT_ID}
        data-button-name="Ask Slate Agent"
        data-create-new-chat="true"
        data-initial-placeholder-text="Ask about onboarding to Slate..."
        aria-label="Ask Slate Agent"
        title="Ask Slate Agent — requires the Helix Ultra browser extension"
    >
        <span className="slate-helix-ultra-button__icon" aria-hidden="true">
            ✨
        </span>
        Ask Slate Agent
    </button>
);

export default HelixUltraButton;
