package grpc.calculator;

public class ClientMain {

    public static void main(String[] args) {

        CalculatorService service = new CalculatorService();

        Request request = Request.newBuilder()
                .setA(8)
                .setB(4)
                .build();

        Response response = service.sum(request);

        System.out.println("Resultado: " + response.getResult());
    }
}