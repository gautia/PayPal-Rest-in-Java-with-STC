package com.paypal.demo.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

import com.paypal.demo.dto.ApplicationConfiguration;
import com.paypal.demo.dto.CreatePaymentDto;
import com.paypal.demo.helpers.Helper;
import com.paypal.demo.helpers.RestClient;

/**
 * This class handles CreatePayment API calls to paypal
 * Gets the payload from html and add access Token and sends the request as Post to paypal server
 * 
 */
@WebServlet("/CreatePayment")
public class CreatePayment extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CreatePayment() {
        super();
        // TODO Auto-generated constructor stub
    }
    

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		try {
			//load the config 
			ApplicationConfiguration ac =  (ApplicationConfiguration) getServletContext().getAttribute("config");
			
			CreatePaymentDto createPaymentDto = getCreatePaymentObject(request); // Helper method to get the payload data
	        
	        RestClient restClient = new RestClient();
	        JSONObject accessTokenObjectFromPayPalServer = restClient.getAccessToken(ac.getClientId(), ac.getSecret(), ac.getAccessTokenUrl(), ac.getBnCode());
			String accessToken = accessTokenObjectFromPayPalServer.getString("access_token");
			JSONObject dataFromCreatePaymentsAPI = restClient.createPayment(accessToken, ac.getCreatePaymentsUrl(), createPaymentDto, ac.getBnCode());

			try {
				int codeFromSTCAPI = restClient.createSTC(accessToken, dataFromCreatePaymentsAPI.get("id").toString(),
						ac.getStcUrl(), ac.getBnCode());
				if (codeFromSTCAPI != 200) {
					response.setStatus(500);
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					Map<String, String> error = new HashMap<String, String>();
					error.put("error", "Error in STC Call");
					out.print(error);
				}
			} catch (Exception e) {
				response.setStatus(500);
				response.setContentType("application/json");
				PrintWriter out = response.getWriter();
				Map<String, String> error = new HashMap<String, String>();
				error.put("error", e.toString());
				out.print(error);
			}
		
			response.setContentType("application/json");
			response.setStatus(200);
			PrintWriter out = response.getWriter();
			out.print(dataFromCreatePaymentsAPI); // post back the object to invoker
			
		}catch(Exception e) {
			response.setStatus(500);
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			Map<String, String> error = new HashMap<String, String>();
			error.put("error", e.toString());
			out.print(error);
		}
        
		
	}
	
	public void loadApplicationProperties(HttpServletRequest request) {
		// load properties to get url and other credentials
		try {
			InputStream input = getServletContext().getResourceAsStream("/WEB-INF/application.properties");
			Properties properties = new Properties();
			properties.load(input);
			
			ApplicationConfiguration ac = new ApplicationConfiguration();
			
			// check the application property wheather the app is in live or sandbox and load config accordingly
			if(properties.getProperty("IS_APPLICATION_IN_SANDBOX").toString().equals("true")) {
				// load all properties for sandbox
				
				ac.setAccessTokenUrl(properties.getProperty("ACCESS_TOKEN_URL").toString());
				ac.setClientId(properties.getProperty("CLIENT_ID").toString());
				ac.setCreatePaymentsUrl(properties.getProperty("CREATE_PAYMENT_URL").toString());
				ac.setExecutePaymentsUrl(properties.getProperty("EXECUTE_PAYMENT_URL").toString());
				ac.setExpressCheckoutUrl(properties.getProperty("EXPRESS_CHECKOUT_SANDBOX_URL").toString());
				ac.setGetPaymentsDetailsUrl(properties.getProperty("GET_PAYMENT_DETAILS").toString());
				ac.setSecret(properties.getProperty("SECRET").toString());
				
			}else {
				
				// load all properties for live
				ac.setAccessTokenUrl(properties.getProperty("ACCESS_TOKEN_URL_LIVE").toString());
				ac.setClientId(properties.getProperty("CLIENT_ID_LIVE").toString());
				ac.setCreatePaymentsUrl(properties.getProperty("CREATE_PAYMENT_URL_LIVE").toString());
				ac.setExecutePaymentsUrl(properties.getProperty("EXECUTE_PAYMENT_URL_LIVE").toString());
				ac.setExpressCheckoutUrl(properties.getProperty("EXPRESS_CHECKOUT_LIVE_URL").toString());
				ac.setGetPaymentsDetailsUrl(properties.getProperty("GET_PAYMENT_DETAILS_LIVE").toString());
				ac.setSecret(properties.getProperty("SECRET_LIVE").toString());
			}
			
			 HttpSession session = request.getSession();  
		     session.setAttribute("config", ac);  
			
		}catch(Exception e) {
			System.out.println("Failed to load Application properties :" + e);
		}
        
	}
	
	private CreatePaymentDto getCreatePaymentObject(HttpServletRequest request) throws IOException {
		String BASEURL = request.getScheme() + "://"+ request.getServerName() + ((request.getServerPort() == 80) ? "" : ":" + request.getServerPort()) + request.getContextPath();
		BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";
        if(br != null) {
            json = br.readLine();
        }
       
        JSONObject dataFromHtml = new JSONObject(json);
        Helper helper = new Helper();
        ApplicationConfiguration ac =  (ApplicationConfiguration) getServletContext().getAttribute("config");
        CreatePaymentDto createPaymentDto = helper.getCreatePaymentData(BASEURL, dataFromHtml,ac.getReturnUrl(),ac.getCancelUrl());
        return createPaymentDto;
        
	}

}
