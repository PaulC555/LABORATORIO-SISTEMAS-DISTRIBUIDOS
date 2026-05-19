package grpc.calculator;

public class Response {

    private int result;

    public int getResult() {
        return result;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final Response response = new Response();

        public Builder setResult(int result) {
            response.result = result;
            return this;
        }

        public Response build() {
            return response;
        }
    }
}