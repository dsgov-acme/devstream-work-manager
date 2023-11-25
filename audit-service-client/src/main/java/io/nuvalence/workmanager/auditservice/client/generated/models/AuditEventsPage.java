/*
 * Nuvalence Audit Service
 * System of record for activities performed within a distributed system.  **Terminology** - A **business object** describes a single domain object, with some logical type (eg: user, document) on which system activities occur and will be audited. - An **event** refers to any action occurring within the distributed system which should be audited. The supported types of events are further enumerated below.
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package io.nuvalence.workmanager.auditservice.client.generated.models;

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
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEvent;
import io.nuvalence.workmanager.auditservice.client.generated.models.PagingMetadata;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Paged response detailing a collection of audit events.
 */
@JsonPropertyOrder({
  AuditEventsPage.JSON_PROPERTY_EVENTS,
  AuditEventsPage.JSON_PROPERTY_PAGING_METADATA
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-09-25T21:47:55.006746-05:00[America/Bogota]")
public class AuditEventsPage {
  public static final String JSON_PROPERTY_EVENTS = "events";
  private List<AuditEvent> events = new ArrayList<>();

  public static final String JSON_PROPERTY_PAGING_METADATA = "pagingMetadata";
  private PagingMetadata pagingMetadata;

  public AuditEventsPage() { 
  }

  public AuditEventsPage events(List<AuditEvent> events) {
    this.events = events;
    return this;
  }

  public AuditEventsPage addEventsItem(AuditEvent eventsItem) {
    this.events.add(eventsItem);
    return this;
  }

   /**
   * Get events
   * @return events
  **/
  @jakarta.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_EVENTS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)

  public List<AuditEvent> getEvents() {
    return events;
  }


  @JsonProperty(JSON_PROPERTY_EVENTS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setEvents(List<AuditEvent> events) {
    this.events = events;
  }


  public AuditEventsPage pagingMetadata(PagingMetadata pagingMetadata) {
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
   * Return true if this AuditEventsPage object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuditEventsPage auditEventsPage = (AuditEventsPage) o;
    return Objects.equals(this.events, auditEventsPage.events) &&
        Objects.equals(this.pagingMetadata, auditEventsPage.pagingMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(events, pagingMetadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuditEventsPage {\n");
    sb.append("    events: ").append(toIndentedString(events)).append("\n");
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

    // add `events` to the URL query string
    if (getEvents() != null) {
      for (int i = 0; i < getEvents().size(); i++) {
        if (getEvents().get(i) != null) {
          joiner.add(getEvents().get(i).toUrlQueryString(String.format("%sevents%s%s", prefix, suffix,
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

