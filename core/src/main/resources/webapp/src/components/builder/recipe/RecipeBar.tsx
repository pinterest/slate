import React, { useEffect, useState } from 'react';
import { Autocomplete, Box, Chip, Stack, TextField } from '@mui/material';
import RecipeCard from './RecipeCard';
import { IRecipe } from './types';

interface IRecipeMarketProps {}

const RecipeBar: React.FC<IRecipeMarketProps> = () => {
    const [tags, setTags] = useState<string[]>([]);
    const [recipes, setRecipes] = useState<IRecipe[]>([]);
    const [selectedTags, setSelectedTags] = useState<string[]>([]);

    useEffect(() => {
        fetchTags();
        fetchRecipes();
    }, []);

    async function fetchTags() {
        fetch('/api/v2/recipes/tags', {
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
            .then((data) => {
                setTags(data);
            });
    }
    async function fetchRecipes() {
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
            .then((data) => {
                setRecipes(data);
            });
    }

    const getFilteredRecipes = () => {
        if (selectedTags.length === 0) {
            return recipes;
        }

        return recipes.filter((recipe) => selectedTags.every((t) => recipe.tags.includes(t)));
    };

    return (
        <Box
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <Box sx={{ p: 1, pb: 0 }}>
                <Autocomplete
                    multiple
                    id="tags-outlined"
                    options={tags}
                    filterSelectedOptions
                    forcePopupIcon={false}
                    renderInput={(params) => (
                        <TextField {...params} hiddenLabel variant="standard" placeholder="Search recipes by tags" />
                    )}
                    value={selectedTags}
                    onChange={(event, value, reason) => {
                        setSelectedTags(value);
                    }}
                />
                <Stack direction="row" spacing={1} overflow="auto" pt={2} pb={2}>
                    {tags.map((tag) => (
                        <Chip
                            key={tag}
                            label={tag}
                            size="small"
                            onClick={(params) => {
                                if (!selectedTags.includes(tag)) {
                                    setSelectedTags([...selectedTags, tag]);
                                }
                            }}
                        />
                    ))}
                </Stack>
            </Box>
            <Box sx={{ flex: 1, padding: 1, overflow: 'auto' }}>
                <Stack spacing={2}>
                    {getFilteredRecipes().map((item) => (
                        <RecipeCard key={item.recipeName} recipe={item} />
                    ))}
                </Stack>
            </Box>
        </Box>
    );
};

export default RecipeBar;
