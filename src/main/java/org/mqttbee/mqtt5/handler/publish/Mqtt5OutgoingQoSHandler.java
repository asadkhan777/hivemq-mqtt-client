package org.mqttbee.mqtt5.handler.publish;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.mqttbee.annotations.NotNull;
import org.mqttbee.api.mqtt.datatypes.MqttQoS;
import org.mqttbee.mqtt.MqttServerConnectionDataImpl;
import org.mqttbee.mqtt.datatypes.MqttTopicImpl;
import org.mqttbee.mqtt.message.publish.MqttPublish;
import org.mqttbee.mqtt.message.publish.MqttPublishWrapper;
import org.mqttbee.mqtt.message.publish.MqttTopicAliasMapping;
import org.mqttbee.mqtt.message.publish.puback.MqttPubAck;
import org.mqttbee.mqtt.message.publish.pubcomp.MqttPubComp;
import org.mqttbee.mqtt.message.publish.pubrec.MqttPubRec;
import org.mqttbee.mqtt.message.publish.pubrel.MqttPubRel;
import org.mqttbee.mqtt.message.publish.pubrel.MqttPubRelBuilder;
import org.mqttbee.mqtt5.ioc.ChannelScope;
import org.mqttbee.mqtt5.persistence.OutgoingQoSFlowPersistence;
import org.mqttbee.util.Ranges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.mqttbee.mqtt.message.publish.MqttPublishWrapper.*;

/**
 * @author Silvio Giebl
 */
@ChannelScope
public class Mqtt5OutgoingQoSHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mqtt5OutgoingQoSHandler.class);

    private final Ranges packetIdentifiers;
    private final OutgoingQoSFlowPersistence persistence;

    @Inject
    Mqtt5OutgoingQoSHandler(
            final Ranges packetIdentifiers, @NotNull final OutgoingQoSFlowPersistence persistence) {

        this.packetIdentifiers = packetIdentifiers;
        this.persistence = persistence;
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        if (msg instanceof MqttPublish) {
            handlePublish(ctx, (MqttPublish) msg, promise);
        } else {
            ctx.write(msg, promise);
        }
    }

    private void handlePublish(
            @NotNull final ChannelHandlerContext ctx, @NotNull final MqttPublish publish,
            @NotNull final ChannelPromise promise) {

        if (publish.getQos() == MqttQoS.AT_MOST_ONCE) {
            handlePublishQoS0(ctx, publish, promise);
        } else {
            handlePublishQoS1Or2(ctx, publish, promise);
        }
    }

    private void handlePublishQoS0(
            @NotNull final ChannelHandlerContext ctx, @NotNull final MqttPublish publish,
            @NotNull final ChannelPromise promise) {

        final MqttPublishWrapper publishWrapper =
                wrapPublish(ctx.channel(), publish, NO_PACKET_IDENTIFIER_QOS_0, false);
        ctx.write(publishWrapper, promise);
    }

    private void handlePublishQoS1Or2(
            @NotNull final ChannelHandlerContext ctx, @NotNull final MqttPublish publish,
            @NotNull final ChannelPromise promise) {

        final int packetIdentifier = packetIdentifiers.getId();
        if (packetIdentifier < 0) {
            // TODO
        }

        final MqttPublishWrapper publishWrapper = wrapPublish(ctx.channel(), publish, packetIdentifier, false);
        persistence.persist(publishWrapper).whenCompleteAsync((aVoid, throwable) -> {
            ctx.writeAndFlush(publishWrapper, promise);
            if (throwable != null) {
                LOGGER.error("Unexpected exception while persisting PUBLISH in outgoing QoSFlowPersistence", throwable);
            }
        }, ctx.executor());
    }

    private MqttPublishWrapper wrapPublish(
            @NotNull final Channel channel, @NotNull final MqttPublish publish, final int packetIdentifier,
            final boolean isDup) {

        final MqttTopicAliasMapping topicAliasMapping = MqttServerConnectionDataImpl.getTopicAliasMapping(channel);
        int topicAlias;
        final boolean isNewTopicAlias;
        if (topicAliasMapping == null) {
            topicAlias = DEFAULT_NO_TOPIC_ALIAS;
            isNewTopicAlias = false;
        } else {
            final MqttTopicImpl topic = publish.getTopic();
            topicAlias = topicAliasMapping.get(topic);
            if (topicAlias != DEFAULT_NO_TOPIC_ALIAS) {
                isNewTopicAlias = false;
            } else {
                topicAlias = topicAliasMapping.set(topic, publish.getTopicAliasUsage());
                isNewTopicAlias = topicAlias != DEFAULT_NO_TOPIC_ALIAS;
            }
        }
        return publish.wrap(packetIdentifier, isDup, topicAlias, isNewTopicAlias, DEFAULT_NO_SUBSCRIPTION_IDENTIFIERS);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof MqttPubAck) {
            handlePubAck((MqttPubAck) msg);
        } else if (msg instanceof MqttPubRec) {
            handlePubRec(ctx, (MqttPubRec) msg);
        } else if (msg instanceof MqttPubComp) {
            handlePubComp((MqttPubComp) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void handlePubAck(@NotNull final MqttPubAck pubAck) {
        // TODO call control provider

        persistence.remove(pubAck.getPacketIdentifier());
    }

    private void handlePubRec(@NotNull final ChannelHandlerContext ctx, @NotNull final MqttPubRec pubRec) {
        final MqttPubRelBuilder pubRelBuilder = new MqttPubRelBuilder(pubRec);

        // TODO call control provider, add user properties

        final MqttPubRel pubRel = pubRelBuilder.build();
        persistence.persist(pubRel).whenCompleteAsync((aVoid, throwable) -> {
            ctx.writeAndFlush(pubRel);
            if (throwable != null) {
                LOGGER.error("Unexpected exception while persisting PUBREL in outgoing QoSFlowPersistence", throwable);
            }
        }, ctx.executor());
    }

    private void handlePubComp(@NotNull final MqttPubComp pubComp) {
        // TODO call control provider

        persistence.remove(pubComp.getPacketIdentifier());
    }

}
