package eu.h2020.symbiote;

import eu.h2020.symbiote.interfaces.PluginRepository;
import eu.h2020.symbiote.interfaces.ResourceAccessRestController;
import eu.h2020.symbiote.interfaces.ResourcesRepository;
import eu.h2020.symbiote.model.data.Observation;
import eu.h2020.symbiote.plugin.PlatformSpecificPlugin;
import eu.h2020.symbiote.resources.PlatformInfo;
import eu.h2020.symbiote.resources.ResourceInfo;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class ResourceAccessProxyApplicationTests {
    
    private MockMvc mockMvc;
    
    private final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    @Autowired
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private ResourcesRepository resourceRepo;
        
    protected String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
 
    @Before
    public void setup() throws Exception {
    	this.mockMvc = webAppContextSetup(webApplicationContext).build();
    	
        //this.resourceRepo.deleteAll();
        
        ResourceInfo resourceTest = new ResourceInfo("test_resource", "test_res", PlatformSpecificPlugin.getPluginPlatformId());
        resourceRepo.saveAndFlush(resourceTest);
    }
	
	
    @Test
    public void contextLoads() {
    }

        
    
    @Test
    public void testReadExistingResource() throws Exception {
        System.out.println("Test read existing resource");
        this.mockMvc.perform(get("/rap/Sensor('test_resource')"))
                .andExpect(status().isOk())
                /*.andExpect(content().contentType(contentType))
		.andExpect(jsonPath("vnfInfo[0].instantiatedVnfInfo.vnfcResourceInfo[0].vnfcInstanceId", is("VM-01-testVnfc")))*/;
        
    }
    
    @Test
    public void testReadNotExistingResource() throws Exception {
        System.out.println("Test read not existing resource");
        this.mockMvc.perform(get("/rap/Sensor('test_res')")).andExpect(status().isBadRequest());
    }
    
    @Test
    public void testReadExistingResourceHistory() throws Exception {
        System.out.println("Test read existing resource history");
        this.mockMvc.perform(get("/rap/Sensor('test_resource')/history")).andExpect(status().isInternalServerError());
    }
    
    @Test
    public void testReadNotExistingResourceHistory() throws Exception {
        System.out.println("Test read not existing resource history");
        this.mockMvc.perform(get("/rap/Sensor('test_res')/history")).andExpect(status().isBadRequest());
    }
    
    @Test
    public void testWriteExistingResource() throws Exception {
        System.out.println("Test write existing resource history");
        Observation observation = Observation.observationExampleValue();
        String json = json(observation);
        this.mockMvc.perform(post("/rap/Resource('test_resource')")
                            .contentType(contentType)
                            .content(json))
                            .andExpect(status().isOk());
    }
    
    @Test
    public void testWriteNotExistingResource() throws Exception {
        System.out.println("Test write not existing resource history");
        Observation observation = Observation.observationExampleValue();
        String json = json(observation);
        this.mockMvc.perform(post("/rap/Resource('test_res')")
                            .contentType(contentType)
                            .content(json))
                            .andExpect(status().isBadRequest());
    }
    
    @After
    public void clearInternalDatabases() {
        this.resourceRepo.delete("test_resource");
    }
}
