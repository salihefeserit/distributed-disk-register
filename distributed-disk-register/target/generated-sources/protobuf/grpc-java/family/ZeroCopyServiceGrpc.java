package family;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.67.1)",
    comments = "Source: family.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ZeroCopyServiceGrpc {

  private ZeroCopyServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "family.ZeroCopyService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<family.ChatMessage,
      family.StoreResult> getStoreZeroCopyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StoreZeroCopy",
      requestType = family.ChatMessage.class,
      responseType = family.StoreResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<family.ChatMessage,
      family.StoreResult> getStoreZeroCopyMethod() {
    io.grpc.MethodDescriptor<family.ChatMessage, family.StoreResult> getStoreZeroCopyMethod;
    if ((getStoreZeroCopyMethod = ZeroCopyServiceGrpc.getStoreZeroCopyMethod) == null) {
      synchronized (ZeroCopyServiceGrpc.class) {
        if ((getStoreZeroCopyMethod = ZeroCopyServiceGrpc.getStoreZeroCopyMethod) == null) {
          ZeroCopyServiceGrpc.getStoreZeroCopyMethod = getStoreZeroCopyMethod =
              io.grpc.MethodDescriptor.<family.ChatMessage, family.StoreResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StoreZeroCopy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.ChatMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.StoreResult.getDefaultInstance()))
              .setSchemaDescriptor(new ZeroCopyServiceMethodDescriptorSupplier("StoreZeroCopy"))
              .build();
        }
      }
    }
    return getStoreZeroCopyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<family.MessageId,
      family.ChatMessage> getRetrieveZeroCopyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RetrieveZeroCopy",
      requestType = family.MessageId.class,
      responseType = family.ChatMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<family.MessageId,
      family.ChatMessage> getRetrieveZeroCopyMethod() {
    io.grpc.MethodDescriptor<family.MessageId, family.ChatMessage> getRetrieveZeroCopyMethod;
    if ((getRetrieveZeroCopyMethod = ZeroCopyServiceGrpc.getRetrieveZeroCopyMethod) == null) {
      synchronized (ZeroCopyServiceGrpc.class) {
        if ((getRetrieveZeroCopyMethod = ZeroCopyServiceGrpc.getRetrieveZeroCopyMethod) == null) {
          ZeroCopyServiceGrpc.getRetrieveZeroCopyMethod = getRetrieveZeroCopyMethod =
              io.grpc.MethodDescriptor.<family.MessageId, family.ChatMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RetrieveZeroCopy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.MessageId.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.ChatMessage.getDefaultInstance()))
              .setSchemaDescriptor(new ZeroCopyServiceMethodDescriptorSupplier("RetrieveZeroCopy"))
              .build();
        }
      }
    }
    return getRetrieveZeroCopyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<family.MessageId,
      family.StoreResult> getDeleteZeroCopyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteZeroCopy",
      requestType = family.MessageId.class,
      responseType = family.StoreResult.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<family.MessageId,
      family.StoreResult> getDeleteZeroCopyMethod() {
    io.grpc.MethodDescriptor<family.MessageId, family.StoreResult> getDeleteZeroCopyMethod;
    if ((getDeleteZeroCopyMethod = ZeroCopyServiceGrpc.getDeleteZeroCopyMethod) == null) {
      synchronized (ZeroCopyServiceGrpc.class) {
        if ((getDeleteZeroCopyMethod = ZeroCopyServiceGrpc.getDeleteZeroCopyMethod) == null) {
          ZeroCopyServiceGrpc.getDeleteZeroCopyMethod = getDeleteZeroCopyMethod =
              io.grpc.MethodDescriptor.<family.MessageId, family.StoreResult>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteZeroCopy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.MessageId.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  family.StoreResult.getDefaultInstance()))
              .setSchemaDescriptor(new ZeroCopyServiceMethodDescriptorSupplier("DeleteZeroCopy"))
              .build();
        }
      }
    }
    return getDeleteZeroCopyMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ZeroCopyServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ZeroCopyServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ZeroCopyServiceStub>() {
        @java.lang.Override
        public ZeroCopyServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ZeroCopyServiceStub(channel, callOptions);
        }
      };
    return ZeroCopyServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ZeroCopyServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ZeroCopyServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ZeroCopyServiceBlockingStub>() {
        @java.lang.Override
        public ZeroCopyServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ZeroCopyServiceBlockingStub(channel, callOptions);
        }
      };
    return ZeroCopyServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ZeroCopyServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ZeroCopyServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ZeroCopyServiceFutureStub>() {
        @java.lang.Override
        public ZeroCopyServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ZeroCopyServiceFutureStub(channel, callOptions);
        }
      };
    return ZeroCopyServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void storeZeroCopy(family.ChatMessage request,
        io.grpc.stub.StreamObserver<family.StoreResult> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStoreZeroCopyMethod(), responseObserver);
    }

    /**
     */
    default void retrieveZeroCopy(family.MessageId request,
        io.grpc.stub.StreamObserver<family.ChatMessage> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRetrieveZeroCopyMethod(), responseObserver);
    }

    /**
     */
    default void deleteZeroCopy(family.MessageId request,
        io.grpc.stub.StreamObserver<family.StoreResult> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteZeroCopyMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ZeroCopyService.
   */
  public static abstract class ZeroCopyServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ZeroCopyServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ZeroCopyService.
   */
  public static final class ZeroCopyServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ZeroCopyServiceStub> {
    private ZeroCopyServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ZeroCopyServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ZeroCopyServiceStub(channel, callOptions);
    }

    /**
     */
    public void storeZeroCopy(family.ChatMessage request,
        io.grpc.stub.StreamObserver<family.StoreResult> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getStoreZeroCopyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void retrieveZeroCopy(family.MessageId request,
        io.grpc.stub.StreamObserver<family.ChatMessage> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRetrieveZeroCopyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteZeroCopy(family.MessageId request,
        io.grpc.stub.StreamObserver<family.StoreResult> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteZeroCopyMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ZeroCopyService.
   */
  public static final class ZeroCopyServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ZeroCopyServiceBlockingStub> {
    private ZeroCopyServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ZeroCopyServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ZeroCopyServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public family.StoreResult storeZeroCopy(family.ChatMessage request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getStoreZeroCopyMethod(), getCallOptions(), request);
    }

    /**
     */
    public family.ChatMessage retrieveZeroCopy(family.MessageId request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRetrieveZeroCopyMethod(), getCallOptions(), request);
    }

    /**
     */
    public family.StoreResult deleteZeroCopy(family.MessageId request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteZeroCopyMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ZeroCopyService.
   */
  public static final class ZeroCopyServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ZeroCopyServiceFutureStub> {
    private ZeroCopyServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ZeroCopyServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ZeroCopyServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<family.StoreResult> storeZeroCopy(
        family.ChatMessage request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getStoreZeroCopyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<family.ChatMessage> retrieveZeroCopy(
        family.MessageId request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRetrieveZeroCopyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<family.StoreResult> deleteZeroCopy(
        family.MessageId request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteZeroCopyMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_STORE_ZERO_COPY = 0;
  private static final int METHODID_RETRIEVE_ZERO_COPY = 1;
  private static final int METHODID_DELETE_ZERO_COPY = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_STORE_ZERO_COPY:
          serviceImpl.storeZeroCopy((family.ChatMessage) request,
              (io.grpc.stub.StreamObserver<family.StoreResult>) responseObserver);
          break;
        case METHODID_RETRIEVE_ZERO_COPY:
          serviceImpl.retrieveZeroCopy((family.MessageId) request,
              (io.grpc.stub.StreamObserver<family.ChatMessage>) responseObserver);
          break;
        case METHODID_DELETE_ZERO_COPY:
          serviceImpl.deleteZeroCopy((family.MessageId) request,
              (io.grpc.stub.StreamObserver<family.StoreResult>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getStoreZeroCopyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              family.ChatMessage,
              family.StoreResult>(
                service, METHODID_STORE_ZERO_COPY)))
        .addMethod(
          getRetrieveZeroCopyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              family.MessageId,
              family.ChatMessage>(
                service, METHODID_RETRIEVE_ZERO_COPY)))
        .addMethod(
          getDeleteZeroCopyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              family.MessageId,
              family.StoreResult>(
                service, METHODID_DELETE_ZERO_COPY)))
        .build();
  }

  private static abstract class ZeroCopyServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ZeroCopyServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return family.FamilyProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ZeroCopyService");
    }
  }

  private static final class ZeroCopyServiceFileDescriptorSupplier
      extends ZeroCopyServiceBaseDescriptorSupplier {
    ZeroCopyServiceFileDescriptorSupplier() {}
  }

  private static final class ZeroCopyServiceMethodDescriptorSupplier
      extends ZeroCopyServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ZeroCopyServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ZeroCopyServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ZeroCopyServiceFileDescriptorSupplier())
              .addMethod(getStoreZeroCopyMethod())
              .addMethod(getRetrieveZeroCopyMethod())
              .addMethod(getDeleteZeroCopyMethod())
              .build();
        }
      }
    }
    return result;
  }
}
