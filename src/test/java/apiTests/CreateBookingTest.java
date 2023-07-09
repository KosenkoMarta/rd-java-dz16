package apiTests;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import models.BookingDates;
import models.CreateBookingBody;
import models.ResponseBooking;
import org.testng.Assert;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;

import java.util.List;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public class CreateBookingTest {

    public static String TOKEN_VALUE;
    public static final String TOKEN = "token";

    @BeforeGroups(groups = {"createBooking", "getBooking", "getBookingIds", "partialUpdateBooking", "updateBooking", "deleteBooking"})
    public void setUp(){
        RestAssured.baseURI="https://restful-booker.herokuapp.com";
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Accept", "application/json")
                .build();

        JSONObject body = new JSONObject();
        body.put("username", "admin");
        body.put("password", "password123");

        Response response = RestAssured.given()
                .body(body.toString())
                .post("/auth");
        TOKEN_VALUE = response.then().extract().jsonPath().get(TOKEN);
    }

    @Test(description = "This test checks positive case of creation a booking", groups = "createBooking")
    public void createNewBookingTest(){
        String firstNameExpected = "FirstName-t";
        Integer totalPriceExpected = 1000;
        Boolean depositPaidExpected = false;

        CreateBookingBody body = new CreateBookingBody().builder()
                .firstname("FirstName-t")
                .lastname("LastName-t")
                .totalprice(1000)
                .depositpaid(false)
                .bookingdates(new BookingDates("2023-07-01","2023-07-05"))
                .additionalneeds("Breakfast-t")
                .build();

        Response newBooking = RestAssured.given()
                .body(body)
                .post("/booking");
        newBooking.prettyPrint();
        newBooking.as(ResponseBooking.class);
        String firstNameResponse = newBooking.as(ResponseBooking.class).getBooking().getFirstname();
        Integer totalPriceResponse = newBooking.as(ResponseBooking.class).getBooking().getTotalprice();
        Boolean depositPaidResponse = newBooking.as(ResponseBooking.class).getBooking().getDepositpaid();
        newBooking.then().statusCode(200);
        newBooking.then().body("bookingid", notNullValue());
        Assert.assertEquals(firstNameResponse, firstNameExpected, "The First name is wrong");
        Assert.assertEquals(totalPriceResponse, totalPriceExpected, "The Total price is wrong");
        Assert.assertEquals(depositPaidResponse, depositPaidExpected, "The Deposit paid is wrong");
    }

    @Test(description = "This test checks positive case of getting all booking IDs", groups = "getBooking")
    public void getAllBookingIdsTest(){
        Response allBookingIds = RestAssured.given().log().all().get("/booking");
        allBookingIds.then().statusCode(200);
        allBookingIds.prettyPrint();
        allBookingIds.then().assertThat().body(matchesJsonSchemaInClasspath("jsonAllBookingIdsSchema.json"));
    }

    @Test(description = "This test checks positive case of getting a booking by ID", groups = "getBookingIds")
    public void getBookingByIdTest(){
        Response bookingId = RestAssured.given().log().all().get("/booking/{id}",20);
        bookingId.prettyPrint();
        bookingId.then().statusCode(200);
        bookingId.then().body("bookingdates.checkin", greaterThanOrEqualTo("2014-01-01"));
        bookingId.jsonPath().get("totalprice");
    }


    @Test(description = "This test checks positive case of price booking update", groups = "partialUpdateBooking")
    public void priceUpdateBookingTest(){

        JSONObject body = new JSONObject();
        body.put("totalprice", 500);

        Response updatedBooking = RestAssured.given()
                .header("Accept", "application/json")
                .contentType(ContentType.JSON)
                .cookie(TOKEN, TOKEN_VALUE)
                .body(body.toString())
                .patch("/booking/{id}", 5);
        updatedBooking.prettyPrint();
        updatedBooking.then().statusCode(200);
    }

    @Test(description = "This test checks positive case of updating a booking", groups = "updateBooking")
    public void updateBookingTest(){
        Response allBookingIds = RestAssured.given().get("/booking");
        allBookingIds.then().statusCode(200);
        JsonPath jsonPath = allBookingIds.jsonPath();

        int randomIndex = (int) (Math.random() * jsonPath.getList("bookingid").size());
        int bookingId = jsonPath.getInt("bookingid[" + randomIndex + "]");

        Response bookingInfo = RestAssured.given().get("/booking/{id}", bookingId);
        bookingInfo.then().statusCode(200);
        JsonPath bookingJsonPath = bookingInfo.jsonPath();

        String newFirstname = "NewFirstname";
        String newAdditionalNeeds = "NewAdditionalNeeds";

        String lastname = bookingJsonPath.getString("lastname");
        int totalprice = bookingJsonPath.getInt("totalprice");
        boolean depositpaid = bookingJsonPath.getBoolean("depositpaid");
        String checkin = bookingJsonPath.getString("bookingdates.checkin");
        String checkout = bookingJsonPath.getString("bookingdates.checkout");

        CreateBookingBody body = new CreateBookingBody().builder()
                .firstname(newFirstname)
                .lastname(lastname)
                .totalprice(totalprice)
                .depositpaid(depositpaid)
                .bookingdates(new BookingDates(checkin, checkout))
                .additionalneeds(newAdditionalNeeds)
                .build();


        Response updatedBooking = RestAssured.given()
                .header("Accept", "application/json")
                .contentType(ContentType.JSON)
                .cookie(TOKEN, TOKEN_VALUE)
                .body(body)
                .put("/booking/{id}", bookingId);

        updatedBooking.prettyPrint();
        updatedBooking.then().statusCode(200);

        String updatedFirstname = updatedBooking.jsonPath().getString("firstname");
        String updatedAdditionalNeeds = updatedBooking.jsonPath().getString("additionalneeds");

        Assert.assertEquals(updatedFirstname, newFirstname, "The First name is incorrect. Expected: " + newFirstname + ", but got: " + updatedFirstname);
        Assert.assertEquals(updatedAdditionalNeeds, newAdditionalNeeds, "The Additional needs is incorrect. Expected: " + newAdditionalNeeds + ", but got: " + updatedAdditionalNeeds);
    }

    @Test(description = "This test checks positive case of removal a booking", groups = "deleteBooking")
    public void deleteBookingTest(){
        Response allBookingIds = RestAssured.given().get("/booking");
        allBookingIds.then().statusCode(200);
        JsonPath jsonPath = allBookingIds.jsonPath();

        int randomIndex = (int) (Math.random() * jsonPath.getList("bookingid").size());
        int bookingId = jsonPath.getInt("bookingid[" + randomIndex + "]");

        Response deleteBooking = RestAssured.given()
                .header("Accept", "application/json")
                .cookie(TOKEN, TOKEN_VALUE)
                .delete("/booking/{id}", bookingId);
        deleteBooking.prettyPrint();
        deleteBooking.then().statusCode(201);
        Response updatedBookingList = RestAssured.given().get("/booking");
        updatedBookingList.then().statusCode(200);
        JsonPath updatedJsonPath = updatedBookingList.jsonPath();

        List<Integer> bookingIds = updatedJsonPath.getList("bookingid");
        Assert.assertFalse(bookingIds.contains(bookingId), "The booking with ID " + bookingId + " was not deleted");
    }

}
