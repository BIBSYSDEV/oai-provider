package no.sikt.oai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

public class OaiProviderHandlerTest {

    @BeforeEach
    public void init() {

    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    public void shouldReturnNull() {
        var handler = new OaiProviderHandler();
        var response = handler.handleRequest(null, null);
        assertNull(response);
    }

}
