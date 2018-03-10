package eu.h2020.symbiote.messages.plugin;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class RapPluginErrorResponseTest {
    RapPluginErrorResponse response;

    @Test
    public void testDefaultConstructor() {
        response = new RapPluginErrorResponse();
        assertThat(response.getResponseCode()).isEqualTo(500);
        assertThat(response.getMessage()).isNull();
    }
    
    @Test
    public void testResponseCodesForNormalError() throws Exception {
        createAndAssert(100);
    }

    private void createAndAssert(int responseCode) {
        response = new RapPluginErrorResponse(responseCode, "");
        assertThat(response.getResponseCode()).isEqualTo(responseCode);
    }

    @Test
    public void testNormalResponseCodesShoultAcceptBorderValue199() throws Exception {
        createAndAssert(199);
    }

    @Test
    public void testNormalResponseCodesShoultAcceptBorderValue300() throws Exception {
        createAndAssert(300);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNormalResponseCodesShoultNoAcceptBorderValue299() throws Exception {
        createAndAssert(299);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNormalResponseCodesShoultNoAcceptBorderValue200() throws Exception {
        createAndAssert(200);
    }
    
    @Test
    public void testMessageAndContent() throws Exception {
        response = new RapPluginErrorResponse(400, "some message");
        assertThat(response.getMessage()).isEqualTo("some message");
        assertThat(response.getContent()).isEqualTo("\"some message\"");
    }

    @Test
    public void testContentWithQuotationMark() throws Exception {
        response = new RapPluginErrorResponse(400, "He said \"HI!\"");
        assertThat(response.getContent()).isEqualTo("\"He said \\\"HI!\\\"\"");
    }

}
