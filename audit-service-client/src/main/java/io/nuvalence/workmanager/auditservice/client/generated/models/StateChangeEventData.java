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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEventDataBase;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.nuvalence.workmanager.auditservice.client.JSON;
/**
 * An audit event which indicates an business object&#39;s state has changed.
 */
@JsonPropertyOrder({
  StateChangeEventData.JSON_PROPERTY_NEW_STATE,
  StateChangeEventData.JSON_PROPERTY_OLD_STATE
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-09-25T21:47:55.006746-05:00[America/Bogota]")
@JsonIgnoreProperties(
  value = "type", // ignore manually set type, it will be automatically generated by Jackson during serialization
  allowSetters = true // allows the type to be set during deserialization
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)

public class StateChangeEventData extends AuditEventDataBase {
  public static final String JSON_PROPERTY_NEW_STATE = "newState";
  private String newState;

  public static final String JSON_PROPERTY_OLD_STATE = "oldState";
  private String oldState;

  public StateChangeEventData() { 
  }

  public StateChangeEventData newState(String newState) {
    this.newState = newState;
    return this;
  }

   /**
   * The new state of the business object data. May be null or undefined if this state change is to indicate business object deletion.
   * @return newState
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NEW_STATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getNewState() {
    return newState;
  }


  @JsonProperty(JSON_PROPERTY_NEW_STATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNewState(String newState) {
    this.newState = newState;
  }


  public StateChangeEventData oldState(String oldState) {
    this.oldState = oldState;
    return this;
  }

   /**
   * The old state of the business object data. May be null or undefined if this state change is to indicate business object creation.
   * @return oldState
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OLD_STATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getOldState() {
    return oldState;
  }


  @JsonProperty(JSON_PROPERTY_OLD_STATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOldState(String oldState) {
    this.oldState = oldState;
  }


  /**
   * Return true if this StateChangeEventData object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StateChangeEventData stateChangeEventData = (StateChangeEventData) o;
    return Objects.equals(this.newState, stateChangeEventData.newState) &&
        Objects.equals(this.oldState, stateChangeEventData.oldState) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newState, oldState, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StateChangeEventData {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    newState: ").append(toIndentedString(newState)).append("\n");
    sb.append("    oldState: ").append(toIndentedString(oldState)).append("\n");
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

    // add `schema` to the URL query string
    if (getSchema() != null) {
      joiner.add(String.format("%sschema%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getSchema()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `activityType` to the URL query string
    if (getActivityType() != null) {
      joiner.add(String.format("%sactivityType%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getActivityType()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `data` to the URL query string
    if (getData() != null) {
      joiner.add(String.format("%sdata%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getData()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `type` to the URL query string
    if (getType() != null) {
      joiner.add(String.format("%stype%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getType()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `newState` to the URL query string
    if (getNewState() != null) {
      joiner.add(String.format("%snewState%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getNewState()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `oldState` to the URL query string
    if (getOldState() != null) {
      joiner.add(String.format("%soldState%s=%s", prefix, suffix, URLEncoder.encode(String.valueOf(getOldState()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
static {
  // Initialize and register the discriminator mappings.
  Map<String, Class<?>> mappings = new HashMap<String, Class<?>>();
  mappings.put("StateChangeEventData", StateChangeEventData.class);
  JSON.registerDiscriminator(StateChangeEventData.class, "type", mappings);
}
}

