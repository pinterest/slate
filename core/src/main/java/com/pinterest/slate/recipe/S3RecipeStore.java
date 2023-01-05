package com.pinterest.slate.recipe;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.pinterest.slate.resources.Utils;

import jersey.repackaged.com.google.common.collect.Sets;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3RecipeStore extends AbstractRecipeStore {

  private String bucket;
  private String prefix;
  private S3Client s3;

  private Map<String, Recipe> recipeMap;
  private Map<String, Set<String>> tagIndex;

  @Override
  public void init(Configuration config) throws Exception {
    bucket = config.getString("bucket");
    prefix = config.getString("prefix");

    recipeMap = new ConcurrentHashMap<String, Recipe>();
    tagIndex = new ConcurrentHashMap<String, Set<String>>();

    s3 = S3Client.builder().region(Region.US_EAST_1)
        .credentialsProvider(InstanceProfileCredentialsProvider.create()).build();

    // load all of the recipes
    ListObjectsResponse objects = s3
        .listObjects(ListObjectsRequest.builder().bucket(bucket).prefix(prefix + "/store").build());
    for (S3Object s3Object : objects.contents()) {
      Recipe recipe = Utils.GSON.fromJson(
          new InputStreamReader(
              s3.getObject(GetObjectRequest.builder().bucket(bucket).key(s3Object.key()).build())),
          Recipe.class);
      recipeMap.put(recipe.getRecipeName(), recipe);
      for (String tag : recipe.getTags()) {
        Set<String> list = tagIndex.get(tag);
        if (list == null) {
          list = new HashSet<String>();
          tagIndex.put(tag, list);
        }
        list.add(recipe.getRecipeName());
      }
    }

  }

  @Override
  public List<Recipe> getRecipeByTags(List<String> tagFilters) {
    Set<String> recipe = new HashSet<>();
    for (String tagFilter : tagFilters) {
      Set<String> v = tagIndex.get(tagFilter);
      if (v != null) {
        if (recipe.isEmpty()) {
          recipe.addAll(recipe);
        } else {
          recipe = Sets.intersection(v, recipe);
          if (recipe.isEmpty()) {
            return ImmutableList.of();
          }
        }
      }
    }
    return recipe.stream().map(r -> recipeMap.get(r)).collect(Collectors.toList());
  }

  @Override
  public JsonObject getRecipeContent(String recipeName) throws Exception {
    if (!recipeMap.containsKey(recipeName)) {
      throw new NotFoundException();
    }
    String key = prefix + "/content/" + recipeName;
    System.out.println(key);
    return Utils.GSON.fromJson(new InputStreamReader(s3.getObject(GetObjectRequest.builder()
        .bucket(bucket).key(key).build())), JsonObject.class);
  }

  @Override
  public byte[] getRecipeScreenshot(String recipeName) throws Exception {
    if (!recipeMap.containsKey(recipeName)) {
      throw new NotFoundException();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    String key = prefix + "/screenshots/" + recipeName;
    System.out.println(key);
    IOUtils.copy(s3.getObject(GetObjectRequest.builder().bucket(bucket)
        .key(key).build()), baos);
    return baos.toByteArray();
  }

  @Override
  public Collection<Recipe> getAllRecipes() {
    return recipeMap.values();
  }

  @Override
  public Collection<String> getTags() {
    return tagIndex.keySet();
  }
}
