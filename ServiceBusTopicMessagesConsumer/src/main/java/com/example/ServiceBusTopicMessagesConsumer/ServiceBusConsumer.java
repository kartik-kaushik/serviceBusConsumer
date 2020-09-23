package com.example.ServiceBusTopicMessagesConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.ISubscriptionClient;
import com.microsoft.azure.servicebus.ITopicClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.SubscriptionClient;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

import lombok.extern.log4j.Log4j2;

import org.apache.geronimo.daytrader.javaee6.entities.OrderDataBean;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
//import org.apache.tomcat.util.json.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;


@Log4j2
@Component
class ServiceBusConsumer implements Ordered {

	@Bean
	public RestTemplate restTemplate() {
	    return new RestTemplate();
	}
	
    //private ISubscriptionClient iSubscriptionClient1 ;
    //@Autowired
    private IQueueClient iqueue;
    @Autowired
    private RestTemplate restTemplate;
	OrderDataBean orderDataBean = null;
	
	  //private ISubscriptionClient iSubscriptionClient2 ; private
	  //ISubscriptionClient iSubscriptionClient3 ; 
    private final Logger log = LoggerFactory.getLogger(ServiceBusConsumer.class); 
   // private String connectionString ="Endpoint=sb://topicsinservicebus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=ny3fKCHs2eFllaAkmT4VUDw5F+r815o1P2ftwOhZLhI=";
	 
    ServiceBusConsumer(IQueueClient iq) {
		this.iqueue = iq;
	}
    
   
    
    
	@EventListener(ApplicationReadyEvent.class)
    public void consume() throws Exception {

    	recievingmessages(iqueue);
    	//recievingmessages(iSubscriptionClient2);
    	//recievingmessages(iSubscriptionClient3);
    	    	
    }

    @SuppressWarnings("deprecation")
	public void recievingmessages(IQueueClient iqueueclient) throws InterruptedException, ServiceBusException {


    	iqueueclient.registerMessageHandler(new IMessageHandler() {

            @Override
            public CompletableFuture<Void> onMessageAsync(IMessage message) {
                
            	log.info("received message " + new String(message.getBody()) + " with body ID " + message.getMessageId());
            
            	//JsonObject jsonObject = new JsonParser().parse(new String(message.getBody())).getAsJsonObject();
            	
            	
            	try {
					Object obj = new JSONParser().parse(new String(message.getBody()));
					
					JSONObject jo = (JSONObject) obj;
					orderDataBean = new OrderDataBean();
					
					String userId = (String) jo.get("userId");
					orderDataBean.setSymbol((String) jo.get("symbol"));
					
					double value = Double.parseDouble((String) jo.get("quantity"));
					orderDataBean.setQuantity(value );
					
					orderDataBean.setPrice(new BigDecimal((String)jo.get("price")));
					orderDataBean.setOrderType((String)jo.get("buySell"));
					orderDataBean.setOrderStatus("open");
					
					log.info("Symbol is-->"+orderDataBean.getSymbol());
					log.info("Quantity is-->"+orderDataBean.getQuantity());
					log.info("Price is-->"+orderDataBean.getPrice());
					
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					
					HttpEntity<OrderDataBean> requestEntity = new HttpEntity<>(orderDataBean, headers);
					
					invokeEndpoint("https://localhost:3443/portfolios/"+userId+"/orders",
							"POST", new String(message.getBody()));
					
					/*
					 * restTemplate.exchange("https://localhost:3443/portfolios/"+userId+"/orders",
					 * HttpMethod.POST, requestEntity, ResponseEntity.class);
					 */
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public void notifyException(Throwable exception, ExceptionPhase phase) {
                log.error("eeks!", exception);
            }
        });
        
    
    	
    }
    
    
    public static String invokeEndpoint(String url, String method, String body) throws Exception
    {
    	return invokeEndpoint(url, method, body, -1);
    }
    
    
    public static String invokeEndpoint(String url, String method, String body, int connTimeOut) throws Exception
    {       	
   		Response  response = sendRequest(url, method, body, connTimeOut);
   		int responseCode = response.getStatus();
   		
   		String responseEntity = response.readEntity(String.class);
   		response.close();
        return responseEntity;
    }
    
    public static Response sendRequest(String url, String method, String body, int connTimeOut) 
    {
    	// Jersey client doesn't support the Http PATCH method without this workaround
        Client client = ClientBuilder.newClient()
        		.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
        
        if (connTimeOut > 0)
        {
        	client.property(ClientProperties.CONNECT_TIMEOUT, connTimeOut);
        }
        
        WebTarget target = client.target(url);
        Response response = target.request().method(method, Entity.json(body));
        return response;
    }
    
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
