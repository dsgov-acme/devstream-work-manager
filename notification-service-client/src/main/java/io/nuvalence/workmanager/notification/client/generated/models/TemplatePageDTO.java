/*
 * Notification Service
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package io.nuvalence.workmanager.notification.client.generated.models;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.notification.client.generated.models.PagingMetadata;
import io.nuvalence.workmanager.notification.client.generated.models.TemplateResponseModel;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * TemplatePageDTO
 */
@JsonPropertyOrder({
  TemplatePageDTO.JSON_PROPERTY_ITEMS,
  TemplatePageDTO.JSON_PROPERTY_PAGING_METADATA
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-07-25T17:45:48.925365-05:00[America/Bogota]")
public class TemplatePageDTO {
  public static final String JSON_PROPERTY_ITEMS = "items";
  private List<TemplateResponseModel> items = new ArrayList<>();

  public static final String JSON_PROPERTY_PAGING_METADATA = "pagingMetadata";
  private PagingMetadata pagingMetadata;

  public TemplatePageDTO() { 
  }

  public TemplatePageDTO items(List<TemplateResponseModel> items) {
    this.items = items;
    return this;
  }

  public TemplatePageDTO addItemsItem(TemplateResponseModel itemsItem) {
    this.items.add(itemsItem);
    return this;
  }

   /**
   * Get items
   * @return items
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ITEMS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<TemplateResponseModel> getItems() {
    return items;
  }


  @JsonProperty(JSON_PROPERTY_ITEMS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setItems(List<TemplateResponseModel> items) {
    this.items = items;
  }


  public TemplatePageDTO pagingMetadata(PagingMetadata pagingMetadata) {
    this.pagingMetadata = pagingMetadata;
    return this;
  }

   /**
   * Get pagingMetadata
   * @return pagingMetadata
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_PAGING_METADATA)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public PagingMetadata getPagingMetadata() {
    return pagingMetadata;
  }


  @JsonProperty(JSON_PROPERTY_PAGING_METADATA)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setPagingMetadata(PagingMetadata pagingMetadata) {
    this.pagingMetadata = pagingMetadata;
  }


  /**
   * Return true if this TemplatePageDTO object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TemplatePageDTO templatePageDTO = (TemplatePageDTO) o;
    return Objects.equals(this.items, templatePageDTO.items) &&
        Objects.equals(this.pagingMetadata, templatePageDTO.pagingMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, pagingMetadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TemplatePageDTO {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    pagingMetadata: ").append(toIndentedString(pagingMetadata)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `items` to the URL query string
    if (getItems() != null) {
      for (int i = 0; i < getItems().size(); i++) {
        if (getItems().get(i) != null) {
          joiner.add(getItems().get(i).toUrlQueryString(String.format("%sitems%s%s", prefix, suffix,
          "".equals(suffix) ? "" : String.format("%s%d%s", containerPrefix, i, containerSuffix))));
        }
      }
    }

    // add `pagingMetadata` to the URL query string
    if (getPagingMetadata() != null) {
      joiner.add(getPagingMetadata().toUrlQueryString(prefix + "pagingMetadata" + suffix));
    }

    return joiner.toString();
  }
}
