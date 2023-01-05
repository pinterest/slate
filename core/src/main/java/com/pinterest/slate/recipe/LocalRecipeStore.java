package com.pinterest.slate.recipe;

import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration2.Configuration;

public class LocalRecipeStore extends AbstractRecipeStore {

  @Override
  public void init(Configuration config) throws Exception {
  }

  @Override
  public List<Recipe> getRecipeByTags(List<String> tagFilters) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<Recipe> getAllRecipes() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<String> getTags() {
    // TODO Auto-generated method stub
    return null;
  }

}
