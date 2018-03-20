package com.paypal.demo.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.paypal.demo.dto.ApplicationConfiguration;
import com.paypal.demo.dto.TransactionAmountDto;
import com.paypal.demo.dto.TransactionDetailsDto;
import com.paypal.demo.helpers.RestClient;

/**
 * This class handles the execute payment api call to paypal serve
 * Function is invoked once the user authorize the payment 
 */
@WebServlet("/ExecutePayments")
public class ExecutePayments extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ExecutePayments() {
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
			
			String jsonFromHtml = readInputStreamForData(request); // holding the json from request
			JSONObject dataToSend = getExecutePaymentObject(jsonFromHtml); // helper to get execute payment API payload
			
			// look for shipping method, 
			//if amount greater than 0.00 then update the amount Object and post with payload
			
			
			
			String url = getUrl(jsonFromHtml, ac); // helper to get url from properties file and append payerId to it.
			
			RestClient restClient = new RestClient();
		    JSONObject accessTokenObjectFromPayPalServer = restClient.getAccessToken(ac.getClientId(), ac.getSecret(), ac.getAccessTokenUrl(), ac.getBnCode());
			String accessToken = accessTokenObjectFromPayPalServer.getString("access_token");
			
			JSONObject dataFromExecutePaymentAPI = restClient.executePayment(accessToken, url, dataToSend, ac.getBnCode());
			
			response.setContentType("application/json");
			response.setStatus(200);
			PrintWriter out = response.getWriter();
			out.print(dataFromExecutePaymentAPI);
			
		}catch(Exception e) {
			response.setStatus(500);
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			Map<String, String> error = new HashMap<String, String>();
			error.put("error", e.toString());
			out.print(error);
			
		}
	}
	
	private String getUrl(String jsonFromHtml, ApplicationConfiguration ac) throws IOException {
		String url = ac.getExecutePaymentsUrl().trim();
		JSONObject json = new JSONObject(jsonFromHtml);
		url = url.replace("{payment_id}", json.getString("paymentID"));
		return url;
	}
	
	private String readInputStreamForData(HttpServletRequest request) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";
        if(br != null) {
            json = br.readLine();
        }
        return json;
	}
	
	private JSONObject getExecutePaymentObject(String jsonFromHtml) throws IOException {
	    JSONObject json = new JSONObject(jsonFromHtml);
	    JSONObject dataToSend = new JSONObject();
		dataToSend.put("payer_id", json.getString("payerID"));
		return dataToSend;
	}
	
	private  JSONObject getShippingMethod(String jsonFromHtml) throws IOException {
		JSONObject json = new JSONObject(jsonFromHtml);
	    JSONObject dataToSend = new JSONObject();
	    
	    TransactionAmountDto transaction = new TransactionAmountDto();
	    transaction.setCurrency(json.getString("currency"));
	    Double totalSAmount = Double.parseDouble(json.getString("amount")) +  Double.parseDouble(json.getString("shippingMethodAmount")); 
	    transaction.setTotal(totalSAmount.toString());
	    TransactionDetailsDto transactionDto = new TransactionDetailsDto();
	    transactionDto.setSubtotal(json.getString("amount"));
	    transactionDto.setShipping(json.getString("shippingMethodAmount"));
	    transaction.setDetails(transactionDto);
	    JSONObject jsonAmountAsString = new JSONObject(transaction);
	    dataToSend.put("amount", jsonAmountAsString);
	    
		return dataToSend;
	    
	}

}
