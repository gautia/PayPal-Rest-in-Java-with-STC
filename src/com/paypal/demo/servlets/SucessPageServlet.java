package com.paypal.demo.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.paypal.demo.dto.ApplicationConfiguration;
import com.paypal.demo.helpers.RestClient;

/**
 * Servlet handles the success page redirect 
 * Get payment details call is handled in post method which will bring the transaction details by 
 * passing payment Id
 */
@WebServlet("/SucessPageServlet")
public class SucessPageServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SucessPageServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html"); 
		PrintWriter pw = response.getWriter();  
		response.sendRedirect("success.html?token="+request.getParameter("token")+"&payerID="+request.getParameter("payerID"));  

	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//load the config 
		ApplicationConfiguration ac =  (ApplicationConfiguration) getServletContext().getAttribute("config");
		
		
	    RestClient restClient = new RestClient();
	    JSONObject accessTokenObjectFromPayPalServer = restClient.getAccessToken(ac.getClientId(),ac.getSecret(), ac.getAccessTokenUrl(), ac.getBnCode());
	    String accessToken = accessTokenObjectFromPayPalServer.getString("access_token");
	    
	    // get url from properties file and append paymentId to it
	    String url = ac.getGetPaymentsDetailsUrl().trim();
	    url = url.replace("{payment_id}", request.getParameter("token"));
	    JSONObject dataFromGetPaymentsAPI = restClient.getPaymentDetails(accessToken, url, ac.getBnCode());
	    
	    response.setContentType("application/json");
		response.setStatus(200);
		PrintWriter out = response.getWriter();
		out.print(dataFromGetPaymentsAPI);
			
		
	}


}
