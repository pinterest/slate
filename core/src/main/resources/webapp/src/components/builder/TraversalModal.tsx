import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
    Typography,
    DialogTitle,
    Dialog,
    DialogContent,
    DialogActions,
    Button,
    Grid,
    Box,
    FormControlLabel,
    Checkbox,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Stack,
    Paper,
    Alert,
} from '@mui/material';
import { ExpandMore } from '@mui/icons-material';
import { TNode } from '../../const/types';
import { selectBuilderState, updateOrAddNodeInElements } from '../../store/slice/builder';
import { fetchResource } from '../../const/datasources';
import { getRandomInt } from '../../const/basicUtils';
import { useLoadingSpinner } from '../../context/LoadingSpinnerContext';

type THandleSelectionMap = Record<string, Record<string, boolean>>;

interface IHandleTraversalProps {
    handle: string;
    idSelectionMap: Record<string, boolean>;
    expand?: boolean;
    onChange: (map: Record<string, boolean>) => void;
}

const HandleTraversal: React.FC<IHandleTraversalProps> = ({ handle, idSelectionMap, onChange, expand }) => {
    const onChangeSelection = (id: string, event: React.ChangeEvent<HTMLInputElement>) => {
        onChange({ ...idSelectionMap, [id]: event.target.checked });
    };
    const onChangeHeader = (event: React.ChangeEvent<HTMLInputElement>) => {
        const newMap = { ...idSelectionMap };
        for (const id in idSelectionMap) {
            newMap[id] = event.target.checked;
        }
        onChange(newMap);
    };
    const mapValues = Object.values(idSelectionMap);
    return (
        <Accordion expanded={expand}>
            <AccordionSummary expandIcon={<ExpandMore />}>
                <FormControlLabel
                    label={handle}
                    onClick={(e) => {
                        e.stopPropagation();
                    }}
                    control={
                        <Checkbox
                            checked={mapValues.every((v) => v === true)}
                            indeterminate={mapValues.some((v) => v === true) && mapValues.some((v) => v === false)}
                            onChange={onChangeHeader}
                        />
                    }
                />
            </AccordionSummary>
            <AccordionDetails>
                <Paper
                    style={{ paddingLeft: '16px', maxHeight: 300, overflow: 'auto', width: '100%', boxShadow: 'none' }}
                >
                    <Stack>
                        {Object.keys(idSelectionMap).map((id, i) => (
                            <FormControlLabel
                                key={id}
                                label={id}
                                style={{ wordBreak: 'break-all' }}
                                control={
                                    <Checkbox
                                        checked={idSelectionMap[id]}
                                        onChange={(evt) => {
                                            onChangeSelection(id, evt);
                                        }}
                                    />
                                }
                            />
                        ))}
                    </Stack>
                </Paper>
            </AccordionDetails>
        </Accordion>
    );
};

interface ITraversalProps {
    handleMap: Record<string, string[]>;
    handleSelection: THandleSelectionMap;
    onChange: (map: THandleSelectionMap) => void;
}
const Traversal: React.FC<ITraversalProps> = ({ handleMap, handleSelection, onChange }) => {
    const onChangeHandle = (handle: string, selection: Record<string, boolean>) => {
        onChange({ ...handleSelection, [handle]: selection });
    };
    const NumOfHandles = Object.keys(handleMap).length;
    if (NumOfHandles < 1) {
        return (
            <Alert icon={false} severity="info">
                No records exist
            </Alert>
        );
    }
    return (
        <div>
            {Object.keys(handleMap).map((handle, i) => {
                return (
                    <HandleTraversal
                        expand={NumOfHandles == 1 ? true : undefined}
                        key={i + handle}
                        handle={handle}
                        idSelectionMap={handleSelection[handle]}
                        onChange={(obj) => {
                            onChangeHandle(handle, obj);
                        }}
                    />
                );
            })}
        </div>
    );
};

interface ITraversalModalProps {
    node: TNode;
    show: boolean;
    onClose: () => void;
}

const TraversalModal: React.FC<ITraversalModalProps> = ({ show, node, onClose }) => {
    const dispatch = useDispatch();
    const { selectedTabId } = useSelector(selectBuilderState);
    const [selectedInputs, setSelectedInputs] = useState<THandleSelectionMap>({});
    const [selectedOutputs, setSelectedOutputs] = useState<THandleSelectionMap>({});
    const { showLoadingOverlay } = useLoadingSpinner();

    useEffect(() => {
        if (!show) {
            return;
        }
        const inputs: THandleSelectionMap = {};
        const outputs: THandleSelectionMap = {};
        const inputMap = node.data?.inputResources ?? {};
        const outputMap = node.data?.outputResources ?? {};
        Object.keys(inputMap).forEach((key) => {
            inputs[key] = {};
            inputMap[key].forEach((id) => {
                inputs[key][id] = true;
            });
        });
        Object.keys(outputMap).forEach((key) => {
            outputs[key] = {};
            outputMap[key].forEach((id) => {
                outputs[key][id] = true;
            });
        });
        setSelectedInputs(inputs);
        setSelectedOutputs(outputs);
    }, [node]);

    const getResource = async (id: string, type: 'input' | 'output') => {
        await fetchResource(
            id,
            (data: any) => {
                if (!data || !data.id) {
                    return;
                }
                const position = {
                    x: node.position.x,
                    y: node.position.y,
                };
                // show inputs to the left of node. The width of the node is ~180px. so used 200px
                if (type == 'input') {
                    position.x = position.x - 200;
                    position.y = getRandomInt(100, 400);
                } else {
                    position.x = position.x + 200;
                    position.y = getRandomInt(100, 400);
                }
                const newNode: TNode = {
                    position: position,
                    type: data.resourceDefinitionClass,
                    data: data,
                    id: data.id,
                };
                dispatch(updateOrAddNodeInElements({ tabId: selectedTabId, node: newNode, addedFromSearch: true }));
            },
            (err: any) => {
                console.error(`Error getting resource: ${id} -> ${err.message}`);
            }
        );
    };

    const getResources = async () => {
        showLoadingOverlay(true);
        const apiCalls: Promise<void>[] = [];
        Object.keys(selectedInputs).forEach(async (key) => {
            Object.keys(selectedInputs[key]).forEach(async (id) => {
                if (selectedInputs[key][id]) {
                    apiCalls.push(getResource(id, 'input'));
                }
            });
        });
        Object.keys(selectedOutputs).forEach(async (key) => {
            Object.keys(selectedOutputs[key]).forEach(async (id) => {
                if (selectedOutputs[key][id]) {
                    apiCalls.push(getResource(id, 'output'));
                }
            });
        });
        Promise.all(apiCalls).then(() => {
            showLoadingOverlay(false);
            onClose && onClose();
        });
    };

    return (
        <Dialog open={show} onClose={onClose} fullWidth={true} maxWidth={'lg'}>
            <DialogTitle>Choose resources to traverse</DialogTitle>
            <DialogContent>
                <Grid container spacing={2}>
                    <Grid item xs={6}>
                        <Typography align="center" variant="h6">
                            Inputs
                        </Typography>
                        <Box>
                            <Traversal
                                handleMap={node.data?.inputResources ?? {}}
                                handleSelection={selectedInputs}
                                onChange={(obj) => {
                                    setSelectedInputs(obj);
                                }}
                            />
                        </Box>
                    </Grid>
                    <Grid item xs={6}>
                        <Typography align="center" variant="h6">
                            Outputs
                        </Typography>
                        <Box>
                            <Traversal
                                handleMap={node.data?.outputResources ?? {}}
                                handleSelection={selectedOutputs}
                                onChange={(obj) => {
                                    setSelectedOutputs(obj);
                                }}
                            />
                        </Box>
                    </Grid>
                </Grid>
            </DialogContent>
            <DialogActions>
                <Button variant="outlined" onClick={getResources}>
                    Fetch resources
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default TraversalModal;
