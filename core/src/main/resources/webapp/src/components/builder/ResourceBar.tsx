import React from 'react';
import { useSelector } from 'react-redux';
import { selectBuilderState } from '../../store/slice/builder';
import { Accordion, AccordionDetails, AccordionSummary, Box, IconButton, Tooltip, Typography } from '@mui/material';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

interface IResourceBarProps { }

const ResourceBar: React.FC<IResourceBarProps> = () => {
    const { resourceDefinitions } = useSelector(selectBuilderState);

    let tags = resourceDefinitions ? Object.keys(resourceDefinitions.resourceTagMap) : [];

    const onDragStart = (event: React.DragEvent, nodeType: string) => {
        event.dataTransfer.setData('application/reactflow', nodeType);
        event.dataTransfer.effectAllowed = 'move';
    };

    if (!resourceDefinitions) {
        return null;
    }
    return (
        <Box id="resourceBar" style={{ maxWidth: '100%', maxHeight: '100%', overflowY: 'scroll' }}>
            {tags.map((tag) => (
                <Accordion key={tag} defaultExpanded={true}>
                    <AccordionSummary
                        expandIcon={<ExpandMoreIcon />}
                        aria-controls="panel1a-content"
                        id="panel1a-header"
                    >
                        <Typography>{tag}</Typography>
                    </AccordionSummary>
                    <AccordionDetails>
                        <div style={{ overflow: 'wrap' }}>
                            {resourceDefinitions.resourceTagMap[tag].map((resourceName) => {
                                const { shortDescription, documentationLink } =
                                    resourceDefinitions.resourceMap[resourceName];
                                return (
                                    <Tooltip
                                        key={resourceName}
                                        title={
                                            <Typography variant="caption">
                                                {shortDescription}
                                                {documentationLink && (
                                                    <IconButton
                                                        color="inherit"
                                                        size="small"
                                                        onClick={() => {
                                                            window.open(documentationLink, '_blank');
                                                        }}
                                                        sx={{ marginLeft: '4px' }}
                                                    >
                                                        <HelpOutlineIcon
                                                            sx={{
                                                                fontSize: 18,
                                                            }}
                                                        />
                                                    </IconButton>
                                                )}
                                            </Typography>
                                        }
                                        placement="right"
                                        sx={{
                                            backgroundColor: 'background.paper',
                                        }}
                                        arrow
                                    >
                                        <Box
                                            className="react-flow__node-default"
                                            sx={{
                                                backgroundColor: 'grey.300',
                                                borderColor: 'grey.300',
                                                margin: '8px',
                                                width: '90%',
                                                cursor: 'grab',
                                            }}
                                            onDragStart={(event) => onDragStart(event, resourceName)}
                                            draggable
                                        >
                                            {resourceDefinitions.resourceMap[resourceName].simpleName}
                                        </Box>
                                    </Tooltip>
                                );
                            })}
                        </div>
                    </AccordionDetails>
                </Accordion>
            ))}
        </Box>
    );
};

export default ResourceBar;
