package com.pinterest.slate.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.JsonObject;
import com.pinterest.slate.recipe.AbstractRecipeStore;
import com.pinterest.slate.recipe.Recipe;

@Path("/v2/recipes")
public class RecipeApi {

  private AbstractRecipeStore store;

  public RecipeApi(AbstractRecipeStore store) {
    this.store = store;
  }

  @Produces({ MediaType.APPLICATION_JSON })
  @Consumes({ MediaType.APPLICATION_JSON })
  @GET
  public Collection<Recipe> getAllRecipes() {
    return store.getAllRecipes();
  }
  
  @Produces({ MediaType.APPLICATION_JSON })
  @Consumes({ MediaType.APPLICATION_JSON })
  @Path("/tags")
  @GET
  public Collection<String> getTags() {
    return store.getTags();
  }
  
  @Produces({ MediaType.APPLICATION_JSON })
  @Consumes({ MediaType.APPLICATION_JSON })
  @Path("/filtered")
  @GET
  public List<Recipe> getFilteredList(@QueryParam("tags") String tags) {
    String[] split = tags.split(",");
    return store.getRecipeByTags(Arrays.asList(split));
  }
  
  @Produces({ MediaType.APPLICATION_JSON })
  @Consumes({ MediaType.APPLICATION_JSON })
  @Path("/{recipeName}/content")
  @GET
  public JsonObject getContent(@PathParam("recipeName") String recipeName) throws Exception {
    return store.getRecipeContent(recipeName);
  }

  @Consumes({ MediaType.APPLICATION_JSON })
  @Produces("image/png")
  @Path("/{recipeName}/screenshot")
  @GET
  public Response getScreenshot(@PathParam("recipeName") String recipeName) throws Exception {
    return Response.ok(store.getRecipeScreenshot(recipeName)).build();
  }

}
