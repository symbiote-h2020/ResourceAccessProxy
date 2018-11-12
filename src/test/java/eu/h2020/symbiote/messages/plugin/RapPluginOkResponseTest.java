package eu.h2020.symbiote.messages.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import eu.h2020.symbiote.messages.plugin.RapPluginResponseTest.ClassWithSelfReference;
import eu.h2020.symbiote.plugin.RapPluginException;

public class RapPluginOkResponseTest {

    private RapPluginOkResponse response;

    @Test(expected=RapPluginException.class)
    public void testSettingToBodyNotSerializableObject() throws Exception {
        response = RapPluginOkResponse.createFromObject(new ClassWithSelfReference());
    }
    
    @Test
    public void testOkResponseCodeWithBodyShouldBe200() throws Exception {
        response = new RapPluginOkResponse("body");
        assertThat(response.getResponseCode()).isEqualTo(200);
    }

    @Test
    public void testOkResponseCodeWithEmptyBodyShouldBe204() throws Exception {
        response = new RapPluginOkResponse(null);
        assertThat(response.getResponseCode()).isEqualTo(204);
    }

    @Test
    public void testDefaultOkResponseCodeShouldBe204() throws Exception {
        response = new RapPluginOkResponse();
        assertThat(response.getResponseCode()).isEqualTo(204);
    }
    
    @Test
    public void testOkResponseCodeShouldBeAsInConstructor() throws Exception {
        response = new RapPluginOkResponse(205, null);
        assertThat(response.getResponseCode()).isEqualTo(205);
        assertThat(((RapPluginOkResponse)response).getBody()).isNull();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testOkResponseCodeCanNotBeLessThen200() throws Exception {
        response = new RapPluginOkResponse(199, null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOkResponseCodeCanNotBeGreaterThen299() throws Exception {
        response = new RapPluginOkResponse(300, null);
    }
}
