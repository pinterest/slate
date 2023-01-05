import React, { useEffect, useState } from 'react';
import AddIcon from '@mui/icons-material/Add';
import ShareIcon from '@mui/icons-material/Share';
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    Snackbar,
    Stack,
    Tooltip,
    Typography,
} from '@mui/material';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import { useDispatch, useSelector } from 'react-redux';
import { useSearchParams } from 'react-router-dom';
import {
    addNodesToWorkspace,
    addWorkspaceTab,
    setOpenResourceEditor,
    setNodeToEdit,
} from '../../../store/slice/builder';
import { IRecipe, IRecipeContent } from './types';

interface IReceipesModalProps {
    open: boolean;
    recipeName: string | null;
}

const RecipeModal: React.FC<IReceipesModalProps> = ({ open, recipeName }) => {
    const dispatch = useDispatch();
    const [searchParams, setSearchParams] = useSearchParams();
    const [loading, setLoading] = useState<boolean>(true);
    const [loadingContent, setLoadingContent] = useState<boolean>(true);
    const [recipe, setRecipe] = useState<IRecipe>();
    const [recipeContent, setRecipeContent] = useState<IRecipeContent>();
    const [copied, setCopied] = useState(false);
    const [openSnackbar, setOpenSnackbar] = useState(false);

    useEffect(() => {
        if (recipeName) {
            fetchRecipes();
            fetchRecipeContent();
        }
    }, [recipeName]);

    // TODO: replace it with the API of getting the recipe details for one recipe
    async function fetchRecipes() {
        setLoading(true);
        fetch('/api/v2/recipes', {
            headers: {
                'Content-Type': 'application/json',
            },
        })
            .then((response) => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error('Failed to load recipes');
                }
            })
            .then((data: IRecipe[]) => {
                setRecipe(data.find((recipe) => recipe.recipeName === recipeName));
                setLoading(false);
            });
    }

    async function fetchRecipeContent() {
        setLoadingContent(true);
        fetch(`/api/v2/recipes/${recipeName}/content`, {
            headers: {
                'Content-Type': 'application/json',
            },
        })
            .then((response) => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error('Failed to load recipe content');
                }
            })
            .then((data) => {
                setRecipeContent(data);
                setLoadingContent(false);
            });
    }

    const handleOnClose = () => {
        searchParams.delete('recipe');
        setSearchParams(searchParams);
    };

    const handleOnCopy = () => {
        setCopied(true);
        setTimeout(() => {
            setCopied(false);
        }, 2000);
    };

    const handleAddToWorkspace = async () => {
        if (!!recipeContent) {
            dispatch(addWorkspaceTab({ makeActive: true }));

            const nodes = Object.values(recipeContent).map((resource) => ({
                position: { x: 0, y: 0 }, // will auto layout
                type: resource.resourceDefinitionClass,
                data: resource,
                id: resource.id,
            }));

            dispatch(addNodesToWorkspace({ nodes }));
            dispatch(setNodeToEdit(null));
            dispatch(setOpenResourceEditor(false));

            setOpenSnackbar(true);
            handleOnClose();
        }
    };

    const getDialogContent = () => {
        if (loading || loadingContent) {
            return (
                <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                    <CircularProgress />
                </Box>
            );
        }

        if (!recipe || !recipeContent) {
            return <Box>can't find recipe </Box>;
        }

        return (
            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Typography variant="h6" gutterBottom component="div">
                        Description
                    </Typography>
                    <Stack direction="row" spacing={2}>
                        <CopyToClipboard text={window.location.href} onCopy={handleOnCopy}>
                            <IconButton aria-label="Copy sharable url" color="secondary">
                                <Tooltip title={copied ? 'Copied' : 'Copy sharable url'}>
                                    <ShareIcon />
                                </Tooltip>
                            </IconButton>
                        </CopyToClipboard>
                        <Button variant="outlined" startIcon={<AddIcon />} onClick={handleAddToWorkspace}>
                            Add To Workspace
                        </Button>
                    </Stack>
                </Box>
                <Typography variant="body1" gutterBottom component="div">
                    {recipe.description}
                </Typography>
                <Box
                    component="img"
                    src={`/api/v2/recipes/${recipeName}/screenshot`}
                    sx={{ width: '100%', objectFit: 'contain' }}
                />
            </Box>
        );
    };

    return (
        <Box>
            <Dialog
                open={open}
                onClose={() => {
                    searchParams.delete('recipe');
                    setSearchParams(searchParams);
                }}
                fullWidth={true}
                scroll={'paper'}
                maxWidth={'lg'}
                PaperProps={{
                    style: {
                        height: '90%',
                        padding: 0,
                    },
                }}
            >
                <DialogTitle sx={{ fontSize: '1.5rem' }}>Recipe - {recipeName}</DialogTitle>
                <DialogContent dividers>{getDialogContent()}</DialogContent>
            </Dialog>
            <Snackbar anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }} open={openSnackbar}>
                <Alert
                    onClose={(event: React.SyntheticEvent | Event, reason?: string) => {
                        if (reason === 'clickaway') {
                            return;
                        }

                        setOpenSnackbar(false);
                    }}
                    severity="warning"
                >
                    Please edit each resource and fill required properties before planning.
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default RecipeModal;
