package nl.wjglerum._05_pinning;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@VirtualThreadUnit
@TestHTTPEndpoint(PinningBeverageResource.class)
class PinningBeverageResourceTest {

    @Test
    // Intentionally NOT annotated with @ShouldNotPin — this demonstrates
    // that synchronized pins virtual threads. Add @ShouldNotPin to see the test fail.
    void testPinnedEndpointDoesPinCarrierThread() {
        given()
                .when()
                .get("/pinned")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(containsString("Pinning Coffee"));
    }

    @Test
    @ShouldNotPin
    void testUnpinnedEndpointDoesNotPinCarrierThread() {
        given()
                .when()
                .get("/unpinned")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(containsString("Unpinning Coffee"));
    }

    @Test
    void testPinnedParallelEndpoint() {
        given()
                .when()
                .get("/pinned/parallel")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(3))
                .body(containsString("Pinning Coffee"));
    }

    @Test
    @ShouldNotPin
    void testUnpinnedParallelEndpoint() {
        given()
                .when()
                .get("/unpinned/parallel")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(3))
                .body(containsString("Unpinning Coffee"));
    }
}
