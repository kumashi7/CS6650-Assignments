package assignment1;

import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import static assignment1.Constants.BASE_URL;

public class test {
    public static void main(String[] args) throws ApiException {
        SkiersApi skiersApi = new SkiersApi();
        skiersApi.getApiClient().setBasePath(BASE_URL);
        skiersApi.writeNewLiftRide(new LiftRide(), 1, "2022", "1", 10000);
    }
}
