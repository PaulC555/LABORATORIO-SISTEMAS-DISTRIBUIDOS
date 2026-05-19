package grpc.calculator;

public class CalculatorService {

    public Response sum(Request req) {

        int result = req.getA() + req.getB();

        Response response = Response.newBuilder()
                .setResult(result)
                .build();

        return response;
    }
}