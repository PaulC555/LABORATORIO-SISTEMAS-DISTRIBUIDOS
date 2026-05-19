package grpc.calculator;

public class Request {

    private int a;
    private int b;

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final Request request = new Request();

        public Builder setA(int a) {
            request.a = a;
            return this;
        }

        public Builder setB(int b) {
            request.b = b;
            return this;
        }

        public Request build() {
            return request;
        }
    }
}