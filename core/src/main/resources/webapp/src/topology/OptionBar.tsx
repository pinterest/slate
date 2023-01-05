import React from 'react';
import { Button, Checkbox, FormControlLabel, Typography, Tooltip } from '@material-ui/core';

interface IOptionBarProps {
    snapToGrid: boolean;
    setSnapToGrid: (val: boolean) => void;
    grid: boolean;
    setGrid: (val: boolean) => void;
    submitPlan: () => void;
    executePlan: () => void;
    enableExecute: boolean;
}

const OptionBar: React.FC<IOptionBarProps> = ({
    snapToGrid,
    grid,
    setSnapToGrid,
    setGrid,
    submitPlan,
    executePlan,
    enableExecute = true,
}) => {
    const fontSizeCheckBox = '7pt';
    const fontSizeButton = '8pt';
    const buttonWidth = '60px';
    return (
        <div id="optionBar" style={{ right: 10, top: 60, zIndex: 10, position: 'fixed' }}>
            <div id="canvasControls">
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={snapToGrid}
                            onChange={(event) => {
                                setSnapToGrid(event.target.checked);
                            }}
                            name="snapToGrid"
                            color="primary"
                            style={{ transform: 'scale(0.6)' }}
                        />
                    }
                    label={<Typography style={{ fontSize: fontSizeCheckBox }}>SnapToGrid</Typography>}
                />
                <FormControlLabel
                    control={
                        <Checkbox
                            checked={grid}
                            onChange={(event) => {
                                setGrid(event.target.checked);
                            }}
                            name="showGrid"
                            color="primary"
                            style={{ transform: 'scale(0.6)' }}
                        />
                    }
                    label={<Typography style={{ fontSize: fontSizeCheckBox }}>Show Grid</Typography>}
                />
            </div>
            <Button
                variant="contained"
                style={{ width: buttonWidth, margin: '2px', fontSize: fontSizeButton }}
                color="primary"
                onClick={submitPlan}
            >
                Plan
            </Button>
            <Tooltip title={!enableExecute ? 'Plan should be run before execute' : ''} placement="top" arrow>
                {/* tooltip cannot be displayed on hidden button, so use span around as a hack */}
                <span>
                    <Button
                        variant="contained"
                        style={{ width: buttonWidth, margin: '2px', fontSize: fontSizeButton }}
                        color="secondary"
                        onClick={executePlan}
                        disabled={!enableExecute}
                    >
                        Execute
                    </Button>
                </span>
            </Tooltip>
        </div>
    );
};

export default OptionBar;
