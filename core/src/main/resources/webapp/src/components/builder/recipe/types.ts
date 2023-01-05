import { IProposedResource } from "../../execution/types";

export interface IRecipe {
    tags: string[];
    recipeName: string;
    description: string;
    // TODO: update type for image data
    screenshot?: any;
}

export interface IRecipeContent {
    [key: string]: IProposedResource
}