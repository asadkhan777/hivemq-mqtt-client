/*
 * Copyright 2018 dc-square and the HiveMQ MQTT Client Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hivemq.client.mqtt.mqtt5.message.publish.pubrec;

import com.hivemq.client.annotations.CheckReturnValue;
import com.hivemq.client.annotations.DoNotImplement;
import com.hivemq.client.mqtt.datatypes.MqttUtf8String;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builder for a {@link Mqtt5PubRec}.
 *
 * @author Silvio Giebl
 * @since 1.0
 */
@DoNotImplement
public interface Mqtt5PubRecBuilder {

    /**
     * Sets the {@link Mqtt5PubRec#getReasonCode() Reason Code}.
     *
     * @param reasonCode the Reason Code.
     * @return the builder.
     */
    @NotNull Mqtt5PubRecBuilder reasonCode(@NotNull Mqtt5PubRecReasonCode reasonCode);

    /**
     * Set the optional {@link Mqtt5PubRec#getReasonString() Reason String}.
     *
     * @param reasonString the Reason String or <code>null</code> to remove any previously set Reason String.
     * @return the builder.
     */
    @NotNull Mqtt5PubRecBuilder reasonString(@Nullable String reasonString);

    /**
     * Set the optional {@link Mqtt5PubRec#getReasonString() Reason String}.
     *
     * @param reasonString the Reason String or <code>null</code> to remove any previously set Reason String.
     * @return the builder.
     */
    @NotNull Mqtt5PubRecBuilder reasonString(@Nullable MqttUtf8String reasonString);

    /**
     * Sets the {@link Mqtt5PubRec#getUserProperties() User Properties}.
     *
     * @param userProperties the User Properties.
     * @return the builder.
     */
    @NotNull Mqtt5PubRecBuilder userProperties(@NotNull Mqtt5UserProperties userProperties);

    /**
     * Fluent counterpart of {@link #userProperties(Mqtt5UserProperties)}.
     * <p>
     * Calling {@link Mqtt5UserPropertiesBuilder.Nested#applyUserProperties()} on the returned builder has the effect of
     * {@link Mqtt5UserProperties#extend() extending} the current User Properties.
     *
     * @return the fluent builder for the User Properties.
     * @see #userProperties(Mqtt5UserProperties)
     */
    @CheckReturnValue
    @NotNull Mqtt5UserPropertiesBuilder.Nested<? extends Mqtt5PubRecBuilder> userProperties();
}
