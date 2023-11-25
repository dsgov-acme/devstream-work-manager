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
import io.nuvalence.workmanager.auditservice.client.generated.models.ActivityEventData;
import io.nuvalence.workmanager.auditservice.client.generated.models.AuditEventDataBase;
import io.nuvalence.workmanager.auditservice.client.generated.models.StateChangeEventData;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.nuvalence.workmanager.auditservice.client.JSON;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2023-09-25T21:47:55.006746-05:00[America/Bogota]")
@JsonDeserialize(using = AuditEventRequestEventData.AuditEventRequestEventDataDeserializer.class)
@JsonSerialize(using = AuditEventRequestEventData.AuditEventRequestEventDataSerializer.class)
public class AuditEventRequestEventData extends AbstractOpenApiSchema {
    private static final Logger log = Logger.getLogger(AuditEventRequestEventData.class.getName());

    public static class AuditEventRequestEventDataSerializer extends StdSerializer<AuditEventRequestEventData> {
        public AuditEventRequestEventDataSerializer(Class<AuditEventRequestEventData> t) {
            super(t);
        }

        public AuditEventRequestEventDataSerializer() {
            this(null);
        }

        @Override
        public void serialize(AuditEventRequestEventData value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeObject(value.getActualInstance());
        }
    }

    public static class AuditEventRequestEventDataDeserializer extends StdDeserializer<AuditEventRequestEventData> {
        public AuditEventRequestEventDataDeserializer() {
            this(AuditEventRequestEventData.class);
        }

        public AuditEventRequestEventDataDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public AuditEventRequestEventData deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode tree = jp.readValueAsTree();
            Object deserialized = null;
            boolean typeCoercion = ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS);
            int match = 0;
            JsonToken token = tree.traverse(jp.getCodec()).nextToken();
            // deserialize ActivityEventData
            try {
                boolean attemptParsing = true;
                // ensure that we respect type coercion as set on the client ObjectMapper
                if (ActivityEventData.class.equals(Integer.class) || ActivityEventData.class.equals(Long.class) || ActivityEventData.class.equals(Float.class) || ActivityEventData.class.equals(Double.class) || ActivityEventData.class.equals(Boolean.class) || ActivityEventData.class.equals(String.class)) {
                    attemptParsing = typeCoercion;
                    if (!attemptParsing) {
                        attemptParsing |= ((ActivityEventData.class.equals(Integer.class) || ActivityEventData.class.equals(Long.class)) && token == JsonToken.VALUE_NUMBER_INT);
                        attemptParsing |= ((ActivityEventData.class.equals(Float.class) || ActivityEventData.class.equals(Double.class)) && token == JsonToken.VALUE_NUMBER_FLOAT);
                        attemptParsing |= (ActivityEventData.class.equals(Boolean.class) && (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE));
                        attemptParsing |= (ActivityEventData.class.equals(String.class) && token == JsonToken.VALUE_STRING);
                    }
                }
                if (attemptParsing) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(ActivityEventData.class);
                    // TODO: there is no validation against JSON schema constraints
                    // (min, max, enum, pattern...), this does not perform a strict JSON
                    // validation, which means the 'match' count may be higher than it should be.
                    match++;
                    log.log(Level.FINER, "Input data matches schema 'ActivityEventData'");
                }
            } catch (Exception e) {
                // deserialization failed, continue
                log.log(Level.FINER, "Input data does not match schema 'ActivityEventData'", e);
            }

            // deserialize AuditEventDataBase
            try {
                boolean attemptParsing = true;
                // ensure that we respect type coercion as set on the client ObjectMapper
                if (AuditEventDataBase.class.equals(Integer.class) || AuditEventDataBase.class.equals(Long.class) || AuditEventDataBase.class.equals(Float.class) || AuditEventDataBase.class.equals(Double.class) || AuditEventDataBase.class.equals(Boolean.class) || AuditEventDataBase.class.equals(String.class)) {
                    attemptParsing = typeCoercion;
                    if (!attemptParsing) {
                        attemptParsing |= ((AuditEventDataBase.class.equals(Integer.class) || AuditEventDataBase.class.equals(Long.class)) && token == JsonToken.VALUE_NUMBER_INT);
                        attemptParsing |= ((AuditEventDataBase.class.equals(Float.class) || AuditEventDataBase.class.equals(Double.class)) && token == JsonToken.VALUE_NUMBER_FLOAT);
                        attemptParsing |= (AuditEventDataBase.class.equals(Boolean.class) && (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE));
                        attemptParsing |= (AuditEventDataBase.class.equals(String.class) && token == JsonToken.VALUE_STRING);
                    }
                }
                if (attemptParsing) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(AuditEventDataBase.class);
                    // TODO: there is no validation against JSON schema constraints
                    // (min, max, enum, pattern...), this does not perform a strict JSON
                    // validation, which means the 'match' count may be higher than it should be.
                    match++;
                    log.log(Level.FINER, "Input data matches schema 'AuditEventDataBase'");
                }
            } catch (Exception e) {
                // deserialization failed, continue
                log.log(Level.FINER, "Input data does not match schema 'AuditEventDataBase'", e);
            }

            // deserialize StateChangeEventData
            try {
                boolean attemptParsing = true;
                // ensure that we respect type coercion as set on the client ObjectMapper
                if (StateChangeEventData.class.equals(Integer.class) || StateChangeEventData.class.equals(Long.class) || StateChangeEventData.class.equals(Float.class) || StateChangeEventData.class.equals(Double.class) || StateChangeEventData.class.equals(Boolean.class) || StateChangeEventData.class.equals(String.class)) {
                    attemptParsing = typeCoercion;
                    if (!attemptParsing) {
                        attemptParsing |= ((StateChangeEventData.class.equals(Integer.class) || StateChangeEventData.class.equals(Long.class)) && token == JsonToken.VALUE_NUMBER_INT);
                        attemptParsing |= ((StateChangeEventData.class.equals(Float.class) || StateChangeEventData.class.equals(Double.class)) && token == JsonToken.VALUE_NUMBER_FLOAT);
                        attemptParsing |= (StateChangeEventData.class.equals(Boolean.class) && (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE));
                        attemptParsing |= (StateChangeEventData.class.equals(String.class) && token == JsonToken.VALUE_STRING);
                    }
                }
                if (attemptParsing) {
                    deserialized = tree.traverse(jp.getCodec()).readValueAs(StateChangeEventData.class);
                    // TODO: there is no validation against JSON schema constraints
                    // (min, max, enum, pattern...), this does not perform a strict JSON
                    // validation, which means the 'match' count may be higher than it should be.
                    match++;
                    log.log(Level.FINER, "Input data matches schema 'StateChangeEventData'");
                }
            } catch (Exception e) {
                // deserialization failed, continue
                log.log(Level.FINER, "Input data does not match schema 'StateChangeEventData'", e);
            }

            if (match == 1) {
                AuditEventRequestEventData ret = new AuditEventRequestEventData();
                ret.setActualInstance(deserialized);
                return ret;
            }
            throw new IOException(String.format("Failed deserialization for AuditEventRequestEventData: %d classes match result, expected 1", match));
        }

        /**
         * Handle deserialization of the 'null' value.
         */
        @Override
        public AuditEventRequestEventData getNullValue(DeserializationContext ctxt) throws JsonMappingException {
            throw new JsonMappingException(ctxt.getParser(), "AuditEventRequestEventData cannot be null");
        }
    }

    // store a list of schema names defined in oneOf
    public static final Map<String, Class<?>> schemas = new HashMap<>();

    public AuditEventRequestEventData() {
        super("oneOf", Boolean.FALSE);
    }

    public AuditEventRequestEventData(ActivityEventData o) {
        super("oneOf", Boolean.FALSE);
        setActualInstance(o);
    }

    public AuditEventRequestEventData(AuditEventDataBase o) {
        super("oneOf", Boolean.FALSE);
        setActualInstance(o);
    }

    public AuditEventRequestEventData(StateChangeEventData o) {
        super("oneOf", Boolean.FALSE);
        setActualInstance(o);
    }

    static {
        schemas.put("ActivityEventData", ActivityEventData.class);
        schemas.put("AuditEventDataBase", AuditEventDataBase.class);
        schemas.put("StateChangeEventData", StateChangeEventData.class);
        JSON.registerDescendants(AuditEventRequestEventData.class, Collections.unmodifiableMap(schemas));
        // Initialize and register the discriminator mappings.
        Map<String, Class<?>> mappings = new HashMap<String, Class<?>>();
        mappings.put("ActivityEventData", ActivityEventData.class);
        mappings.put("AuditEventDataBase", AuditEventDataBase.class);
        mappings.put("StateChangeEventData", StateChangeEventData.class);
        mappings.put("AuditEventRequest_eventData", AuditEventRequestEventData.class);
        JSON.registerDiscriminator(AuditEventRequestEventData.class, "type", mappings);
    }

    @Override
    public Map<String, Class<?>> getSchemas() {
        return AuditEventRequestEventData.schemas;
    }

    /**
     * Set the instance that matches the oneOf child schema, check
     * the instance parameter is valid against the oneOf child schemas:
     * ActivityEventData, AuditEventDataBase, StateChangeEventData
     *
     * It could be an instance of the 'oneOf' schemas.
     * The oneOf child schemas may themselves be a composed schema (allOf, anyOf, oneOf).
     */
    @Override
    public void setActualInstance(Object instance) {
        if (JSON.isInstanceOf(ActivityEventData.class, instance, new HashSet<Class<?>>())) {
            super.setActualInstance(instance);
            return;
        }

        if (JSON.isInstanceOf(AuditEventDataBase.class, instance, new HashSet<Class<?>>())) {
            super.setActualInstance(instance);
            return;
        }

        if (JSON.isInstanceOf(StateChangeEventData.class, instance, new HashSet<Class<?>>())) {
            super.setActualInstance(instance);
            return;
        }

        throw new RuntimeException("Invalid instance type. Must be ActivityEventData, AuditEventDataBase, StateChangeEventData");
    }

    /**
     * Get the actual instance, which can be the following:
     * ActivityEventData, AuditEventDataBase, StateChangeEventData
     *
     * @return The actual instance (ActivityEventData, AuditEventDataBase, StateChangeEventData)
     */
    @Override
    public Object getActualInstance() {
        return super.getActualInstance();
    }

    /**
     * Get the actual instance of `ActivityEventData`. If the actual instance is not `ActivityEventData`,
     * the ClassCastException will be thrown.
     *
     * @return The actual instance of `ActivityEventData`
     * @throws ClassCastException if the instance is not `ActivityEventData`
     */
    public ActivityEventData getActivityEventData() throws ClassCastException {
        return (ActivityEventData)super.getActualInstance();
    }

    /**
     * Get the actual instance of `AuditEventDataBase`. If the actual instance is not `AuditEventDataBase`,
     * the ClassCastException will be thrown.
     *
     * @return The actual instance of `AuditEventDataBase`
     * @throws ClassCastException if the instance is not `AuditEventDataBase`
     */
    public AuditEventDataBase getAuditEventDataBase() throws ClassCastException {
        return (AuditEventDataBase)super.getActualInstance();
    }

    /**
     * Get the actual instance of `StateChangeEventData`. If the actual instance is not `StateChangeEventData`,
     * the ClassCastException will be thrown.
     *
     * @return The actual instance of `StateChangeEventData`
     * @throws ClassCastException if the instance is not `StateChangeEventData`
     */
    public StateChangeEventData getStateChangeEventData() throws ClassCastException {
        return (StateChangeEventData)super.getActualInstance();
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

    if (getActualInstance() instanceof AuditEventDataBase) {
        if (getActualInstance() != null) {
          joiner.add(((AuditEventDataBase)getActualInstance()).toUrlQueryString(prefix + "one_of_0" + suffix));
        }
        return joiner.toString();
    }
    if (getActualInstance() instanceof ActivityEventData) {
        if (getActualInstance() != null) {
          joiner.add(((ActivityEventData)getActualInstance()).toUrlQueryString(prefix + "one_of_1" + suffix));
        }
        return joiner.toString();
    }
    if (getActualInstance() instanceof StateChangeEventData) {
        if (getActualInstance() != null) {
          joiner.add(((StateChangeEventData)getActualInstance()).toUrlQueryString(prefix + "one_of_2" + suffix));
        }
        return joiner.toString();
    }
    return null;
  }

}

