import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Box } from '@material-ui/core';
import { useStyles } from '../../AppStyles';
import Search from '../../Search';
import {
    selectBuilderState,
    setOpenResourceEditor,
    updateOrAddNodeInElements,
    setNodeToEdit,
} from '../../store/slice/builder';
import { useNavigate, useLocation } from 'react-router-dom';
import { getResourceAndItsDependents } from '../../const/graphHelper';

interface IResourceSearchProps {}

const ResourceSearch: React.FC<IResourceSearchProps> = () => {
    const classes = useStyles();
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const location = useLocation();
    const { selectedTabId } = useSelector(selectBuilderState);

    const handleSearch = async (resourceId: string) => {
        if (!resourceId) {
            return;
        }
        // As per initial design, readonly + edit modes are in builder tab. So redirect to builder tab
        if (location.pathname != '/builder') {
            navigate('/builder');
        }
        const nodes = await getResourceAndItsDependents(resourceId, true, true, false);
        nodes.forEach((node) => {
            dispatch(updateOrAddNodeInElements({ tabId: selectedTabId, node: node, addedFromSearch: true }));
        });
        dispatch(setNodeToEdit(null));
        dispatch(setOpenResourceEditor(false));
    };

    return (
        <Box display="flex" className={classes.searchBar}>
            <Search handleSubmit={handleSearch} />
        </Box>
    );
};

export default ResourceSearch;
