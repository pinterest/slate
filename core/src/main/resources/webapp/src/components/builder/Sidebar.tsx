import React, { useState } from 'react';
import { styled } from '@mui/material/styles';
import { Box, Menu, MenuItem, ToggleButtonGroup, ToggleButton, Tooltip, IconButton } from '@mui/material';
import { DashboardCustomizeOutlined, RestaurantMenuOutlined, HelpOutlineOutlined } from '@mui/icons-material';
import ResourceBar from './ResourceBar';
import RecipeBar from './recipe/RecipeBar';
import { SidebarType } from '../../const/types';
import { useTour } from '@reactour/tour';

const NavButtonGroup = styled(ToggleButtonGroup)(({ theme }) => ({
    '& .MuiToggleButtonGroup-grouped': {
        border: 0,
        borderRadius: 0,
        '&.Mui-selected': {
            backgroundColor: theme.palette.grey[100],
        },
    },
}));

const NAVBAR_WIDTH = 60;
const SIDEBAR_WIDTH = 250;

const Sidebar: React.FC = () => {
    const { setIsOpen: setIsTourOpen, setCurrentStep } = useTour();
    const [anchorEl, setAnchorEl] = React.useState<HTMLButtonElement | null>(null);
    const [selectedSidebar, setSelectedSidebar] = useState<SidebarType>(SidebarType.Resource);

    const getSidebarContent = () => {
        if (selectedSidebar === SidebarType.Resource) {
            return <ResourceBar />;
        } else if (selectedSidebar === SidebarType.Recipe) {
            return <RecipeBar />;
        }
    };

    return (
        <Box
            sx={{
                display: 'flex',
                borderRight: 1,
                borderColor: 'grey.200',
                backgroundColor: !!selectedSidebar ? 'white' : 'grey.100',
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                }}
            >
                <NavButtonGroup
                    size="large"
                    exclusive
                    value={selectedSidebar}
                    onChange={(event, newValue) => setSelectedSidebar(newValue)}
                    orientation="vertical"
                    aria-label="text formatting"
                    sx={{ width: NAVBAR_WIDTH }}
                >
                    <ToggleButton value={SidebarType.Resource} aria-label="bold" id="resourceBtn">
                        <Tooltip title="Resources" placement="right">
                            <DashboardCustomizeOutlined />
                        </Tooltip>
                    </ToggleButton>
                    {/* hide the recipes before everything is hooked up */}
                    <ToggleButton value={SidebarType.Recipe} aria-label="italic" id="recipeBtn">
                        <Tooltip title="Recipes" placement="right">
                            <RestaurantMenuOutlined />
                        </Tooltip>
                    </ToggleButton>
                </NavButtonGroup>
                <Box
                    sx={{
                        display: 'flex',
                        justifyContent: 'center',
                        marginY: 1,
                    }}
                >
                    <IconButton
                        onClick={(event: React.MouseEvent<HTMLButtonElement>) => {
                            setAnchorEl(event.currentTarget);
                        }}
                    >
                        <Tooltip title="Help" placement="right">
                            <HelpOutlineOutlined />
                        </Tooltip>
                    </IconButton>
                    <Menu
                        id="help-menu"
                        anchorEl={anchorEl}
                        open={!!anchorEl}
                        onClose={() => {
                            setAnchorEl(null);
                        }}
                        anchorOrigin={{
                            vertical: 'top',
                            horizontal: 'right',
                        }}
                    >
                        <MenuItem
                            onClick={() => {
                                setCurrentStep(0);
                                setIsTourOpen(true);
                                setAnchorEl(null);
                            }}
                        >
                            Tutorial
                        </MenuItem>
                    </Menu>
                </Box>
            </Box>
            {/* sidebar content */}
            {!!selectedSidebar && (
                <Box sx={{ width: SIDEBAR_WIDTH, backgroundColor: 'grey.100' }}>{getSidebarContent()}</Box>
            )}
        </Box>
    );
};

export default Sidebar;
