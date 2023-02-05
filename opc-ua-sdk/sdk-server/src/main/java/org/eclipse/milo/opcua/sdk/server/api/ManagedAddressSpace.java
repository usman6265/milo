/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.api;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.services.MethodServices;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.factories.NodeFactory;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.eclipse.milo.opcua.stack.core.util.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ManagedAddressSpace implements AddressSpace {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UaNodeContext nodeContext;
    private final NodeFactory nodeFactory;

    private final OpcUaServer server;
    private final UaNodeManager nodeManager;

    public ManagedAddressSpace(OpcUaServer server) {
        this(server, new UaNodeManager());
    }

    public ManagedAddressSpace(OpcUaServer server, UaNodeManager nodeManager) {
        this.server = server;

        this.nodeManager = nodeManager;

        nodeContext = new UaNodeContext() {
            @Override
            public OpcUaServer getServer() {
                return ManagedAddressSpace.this.getServer();
            }

            @Override
            public NodeManager<UaNode> getNodeManager() {
                return ManagedAddressSpace.this.getNodeManager();
            }
        };

        nodeFactory = createNodeFactory();
    }

    protected NodeFactory createNodeFactory() {
        return new NodeFactory(nodeContext);
    }

    protected OpcUaServer getServer() {
        return server;
    }

    protected UaNodeContext getNodeContext() {
        return nodeContext;
    }

    protected NodeFactory getNodeFactory() {
        return nodeFactory;
    }

    protected UaNodeManager getNodeManager() {
        return nodeManager;
    }

    @Override
    public void browse(BrowseContext context, ViewDescription viewDescription, NodeId nodeId) {
        if (nodeManager.containsNode(nodeId)) {
            List<Reference> references = nodeManager.getReferences(nodeId);

            logger.debug("Browsed {} references for {}", references.size(), nodeId);

            context.success(references);
        } else {
            context.failure(StatusCodes.Bad_NodeIdUnknown);
        }
    }

    @Override
    public void getReferences(BrowseContext context, ViewDescription viewDescription, NodeId nodeId) {
        List<Reference> references = nodeManager.getReferences(nodeId);

        logger.debug("Got {} references for {}", references.size(), nodeId);

        context.success(references);
    }

    @Override
    public void registerNodes(RegisterNodesContext context, List<NodeId> nodeIds) {
        context.success(nodeIds);
    }

    @Override
    public void unregisterNodes(UnregisterNodesContext context, List<NodeId> nodeIds) {
        context.success(Collections.nCopies(nodeIds.size(), Unit.VALUE));
    }

    /**
     * @auther Muhammad Usman
     * @param context      the {@link HistoryReadContext}.
     * @param details
     * @param timestamps   requested timestamp values.
     * @param readValueIds the values to read.
     *                     hi
     */

    @Override
    public void historyRead(
            HistoryReadContext context,
            HistoryReadDetails details,
            TimestampsToReturn timestamps,
            List<HistoryReadValueId> readValueIds

    ) {
        List<HistoryReadResult> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (HistoryReadValueId hisreadValueId : readValueIds) {
            Optional<UaNode> node = nodeManager.getNode(hisreadValueId.getNodeId());


            if (details instanceof ReadRawModifiedDetails) {

                if (node.isPresent()) {
                    UaVariableNode varNode = (UaVariableNode) node.get();
                    if (varNode.getHistorizing()) {
                        ReadRawModifiedDetails readDetails = (ReadRawModifiedDetails) details;

                        long startTime = readDetails.getStartTime().getUtcTime();
                        long endTime = readDetails.getEndTime().getUtcTime();


                        List<DataValue> historyDataList = null;

                        historyDataList = queries.getData(varNode);

                        if (endTime == DateTime.NULL_VALUE.getUtcTime()) {
                            DataValue d = historyDataList.get(0);
                            HistoryData h = new HistoryData(new DataValue[]{d});

                            HistoryReadResult result = new HistoryReadResult(StatusCode.GOOD,
                                    ByteString.NULL_VALUE, ExtensionObject.encode(server.getSerializationContext(),
                                    h));

                            results.add(result);
                        } else {

                            List<DataValue> collect = historyDataList.stream().filter(dataValue -> dataValue.getServerTime().getUtcTime() > startTime && dataValue.getServerTime().getUtcTime() < endTime).collect(Collectors.toList());
                            DataValue[] array = new DataValue[collect.size()];

                            for (int i = 0; i < collect.size(); i++) {
                                array[i] = collect.get(i);
                            }

                            HistoryData historyDat = new HistoryData(array);

                            HistoryReadResult result = new HistoryReadResult(StatusCode.GOOD,
                                    ByteString.NULL_VALUE, ExtensionObject.encode(server.getSerializationContext(),
                                    historyDat));
                            results.add(result);
                        }
                    } else {
                        HistoryReadResult result = new HistoryReadResult(
                                new StatusCode(StatusCodes.Bad_HistoryOperationUnsupported),
                                null,
                                null);
                        results.add(result);
                    }
                } else {
                    HistoryReadResult result = new HistoryReadResult(
                            new StatusCode(StatusCodes.Bad_NodeIdUnknown),
                            null,
                            null);
                    results.add(result);
                }

            }
            if (details instanceof ReadEventDetails) {
                //Write code here for reading Event History
                results.add(null);
            }
            if (details instanceof ReadAtTimeDetails) {//Write Code for reading history of this specific type
                results.add(null);
            }
            if (details instanceof ReadProcessedDetails) {//Write Code for reading history of this specific type
                results.add(null);
            }

            context.success(results);
        }
    }


    @Override
    public void read(
        ReadContext context,
        Double maxAge,
        TimestampsToReturn timestamps,
        List<ReadValueId> readValueIds
    ) {

        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId readValueId : readValueIds) {
            UaServerNode node = nodeManager.get(readValueId.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(
                    new AttributeContext(context),
                    readValueId.getAttributeId(),
                    timestamps,
                    readValueId.getIndexRange(),
                    readValueId.getDataEncoding()
                );

                logger.debug("Read value {} from attribute {} of {}",
                    value.getValue().getValue(),
                    AttributeId.from(readValueId.getAttributeId())
                        .map(Object::toString).orElse("unknown"),
                    node.getNodeId()
                );

                results.add(value);
            } else {
                results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.success(results);
    }

    @Override
    public void write(
        WriteContext context,
        List<WriteValue> writeValues
    ) {

        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            UaServerNode node = nodeManager.get(writeValue.getNodeId());

            if (node != null) {
                try {
                    node.writeAttribute(
                        new AttributeContext(context),
                        writeValue.getAttributeId(),
                        writeValue.getValue(),
                        writeValue.getIndexRange()
                    );

                    results.add(StatusCode.GOOD);

                    logger.debug(
                        "Wrote value {} to {} attribute of {}",
                        writeValue.getValue().getValue(),
                        AttributeId.from(writeValue.getAttributeId())
                            .map(Object::toString).orElse("unknown"),
                        node.getNodeId()
                    );
                } catch (UaException e) {
                    logger.error("Unable to write value={}", writeValue.getValue(), e);
                    results.add(e.getStatusCode());
                }
            } else {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.success(results);
    }

    /**
     * Invoke one or more methods belonging to this {@link MethodServices}.
     *
     * @param context  the {@link CallContext}.
     * @param requests The {@link CallMethodRequest}s for the methods to invoke.
     */
    @Override
    public void call(CallContext context, List<CallMethodRequest> requests) {
        List<CallMethodResult> results = Lists.newArrayListWithCapacity(requests.size());

        for (CallMethodRequest request : requests) {
            try {
                MethodInvocationHandler handler = getInvocationHandler(
                    request.getObjectId(),
                    request.getMethodId()
                );

                results.add(handler.invoke(context, request));
            } catch (UaException e) {
                results.add(
                    new CallMethodResult(
                        e.getStatusCode(),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                    )
                );
            } catch (Throwable t) {
                LoggerFactory.getLogger(getClass())
                    .error("Uncaught Throwable invoking method handler for methodId={}.", request.getMethodId(), t);

                results.add(
                    new CallMethodResult(
                        new StatusCode(StatusCodes.Bad_InternalError),
                        new StatusCode[0],
                        new DiagnosticInfo[0],
                        new Variant[0]
                    )
                );
            }
        }

        context.success(results);
    }

    /**
     * Get the {@link MethodInvocationHandler} for the method identified by {@code methodId}.
     *
     * @param objectId the {@link NodeId} identifying the object the method will be invoked on.
     * @param methodId the {@link NodeId} identifying the method.
     * @return the {@link MethodInvocationHandler} for {@code methodId}.
     * @throws UaException a {@link UaException} containing the appropriate operation result if
     *                     either the object or method can't be found.
     */
    protected MethodInvocationHandler getInvocationHandler(NodeId objectId, NodeId methodId) throws UaException {
        UaNode node = nodeManager.getNode(objectId)
            .orElseThrow(() -> new UaException(StatusCodes.Bad_NodeIdUnknown));

        UaMethodNode methodNode = null;

        if (node instanceof UaObjectNode) {
            UaObjectNode objectNode = (UaObjectNode) node;

            methodNode = objectNode.findMethodNode(methodId);
        } else if (node instanceof UaObjectTypeNode) {
            UaObjectTypeNode objectTypeNode = (UaObjectTypeNode) node;

            methodNode = objectTypeNode.findMethodNode(methodId);
        }

        if (methodNode != null) {
            return methodNode.getInvocationHandler();
        } else {
            throw new UaException(StatusCodes.Bad_MethodInvalid);
        }
    }

}
