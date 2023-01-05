import { Box, Chip, Paper, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import React from 'react';
import { IRecipe } from './types';

interface IRecipeCardProps {
    recipe: IRecipe;
}

const RecipeCard: React.FC<IRecipeCardProps> = ({ recipe }) => {
    const navigate = useNavigate();
    const { recipeName, description, tags } = recipe;
    const screenshot = `/api/v2/recipes/${recipeName}/screenshot`;
    return (
        <Paper
            elevation={0}
            sx={{
                cursor: 'pointer',
            }}
            onClick={() => {
                navigate(`/builder?recipe=${recipeName}`);
            }}
        >
            <Typography variant="subtitle2" component="div" padding={1}>
                {recipeName}
            </Typography>
            <Box component="img" src={screenshot} sx={{ width: '100%', objectFit: 'contain' }} />
            <Box padding={1}>
                <Stack direction="row" spacing={1} mb={1}>
                    {tags.map((tag) => (
                        <Chip key={tag} label={tag} size="small" />
                    ))}
                </Stack>
                <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{
                        display: '-webkit-box',
                        '-webkit-box-orient': 'vertical',
                        '-webkit-line-clamp': '4',
                        overflow: 'hidden',
                    }}
                >
                    {description}
                </Typography>
            </Box>
        </Paper>
    );
};

export default RecipeCard;
