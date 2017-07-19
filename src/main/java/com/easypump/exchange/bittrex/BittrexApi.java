package com.easypump.exchange.bittrex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.easypump.exceptions.DustTradeException;
import com.easypump.exceptions.InsufficientFundsException;
import com.easypump.exceptions.InvalidCreditionals;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
public class BittrexApi {

	final static Logger logger = LoggerFactory.getLogger(BittrexApi.class);

	private final static String BITTREX_URL = "https://bittrex.com/api/v1.1/";

	private final long CHECK_SLEEP_INTERVAL_MILLISECOND = 100;
	
	RestTemplate restTemplate = new RestTemplate();
	Gson gson = new GsonBuilder().create();
	

	public BigDecimal getCoinLimit(String coinName) throws Exception {

		String marketName = "btc-" + coinName.toLowerCase();

		StringBuilder url = new StringBuilder(BITTREX_URL + "public/getmarketsummary?market=").append(marketName);
		
		String response = restTemplate.postForObject(url.toString(), null, String.class);

		MarketSummaryResponse marketResponse = gson.fromJson(response, MarketSummaryResponse.class);

		if (!marketResponse.isSuccess())
			throw new Exception("Error While Getting Orders - " + marketResponse.getMessage());

		return BigDecimal.valueOf(marketResponse.getResult().get(0).getAsk());

	}

	public String placeBuyOrder(String apiKey, String apiSecret, String coinName, BigDecimal quantity, BigDecimal rate)
			throws Exception {

		long nonce = new Date().getTime();

		String marketName = "btc-" + coinName.toLowerCase();

		StringBuilder url = new StringBuilder(BITTREX_URL + "market/buylimit?apikey=").append(apiKey).append("&market=")
				.append(marketName).append("&quantity=").append(quantity.doubleValue()).append("&rate=")
				.append(rate.doubleValue()).append("&nonce=").append(nonce);

		HttpHeaders headers = new HttpHeaders();

		try {
			headers.add("apisign", encode(apiSecret, url.toString()));
		} catch (Exception e) {
			logger.error("Error while setting header ", e);
		}

		HttpEntity<String> request = new HttpEntity<String>(headers);

		String response = restTemplate.postForObject(url.toString(), request, String.class);

		OrderResponse orderResponse = gson.fromJson(response, OrderResponse.class);

		if (!orderResponse.isSuccess()) {
			if ("INSUFFICIENT_FUNDS".equals(orderResponse.getMessage())) {
				throw new InsufficientFundsException();
			} else if ("DUST_TRADE_DISALLOWED_MIN_VALUE_50K_SAT".equals(orderResponse.getMessage())) {
				throw new DustTradeException();
			} else if ("APIKEY_INVALID".equals(orderResponse.getMessage()))
			{
				throw new InvalidCreditionals();
			}
			else {
				logger.error("Error Occured with response {}", response);
				throw new Exception();
			}
		}

		return orderResponse.getResult().getUuid();
	}

	public String placeSellOrder(String apiKey, String apiSecret, String coinName, BigDecimal quantity, BigDecimal rate)
			throws Exception {

		long nonce = new Date().getTime();

		String marketName = "btc-" + coinName.toLowerCase();
		
		StringBuilder url = new StringBuilder(BITTREX_URL + "market/selllimit?apikey=").append(apiKey)
				.append("&market=").append(marketName).append("&quantity=").append(quantity.doubleValue())
				.append("&rate=").append(rate.doubleValue()).append("&nonce=").append(nonce);

		HttpHeaders headers = new HttpHeaders();

		try {
			headers.add("apisign", encode(apiSecret, url.toString()));
		} catch (Exception e) {
			logger.error("Error while setting header ", e);
		}

		HttpEntity<String> request = new HttpEntity<String>(headers);

		String response = restTemplate.postForObject(url.toString(), request, String.class);

		OrderResponse orderResponse = gson.fromJson(response, OrderResponse.class);

		if (!orderResponse.isSuccess()) {

			logger.error("Error while setting header with response {}", response);
			throw new Exception();

		}

		return orderResponse.getResult().getUuid();
	}

	public void waitOpenOrderToClose(String apiKey, String apiSecret, String uuid) {
		boolean isOpen = true;
		do {
			isOpen = isOrderOpen(apiKey, apiSecret, uuid);

			if (isOpen == true) {
				try {
					Thread.sleep(CHECK_SLEEP_INTERVAL_MILLISECOND);
				} catch (InterruptedException e) {
					logger.error("Error occured", e);
				}
			}
		} while (isOpen == true);
	}

	private boolean isOrderOpen(String apiKey, String apiSecret, String uuid) {

		long nonce = new Date().getTime();

		StringBuilder url = new StringBuilder(BITTREX_URL + "account/getorder?apikey=").append(apiKey).append("&uuid=")
				.append(uuid).append("&nonce=").append(nonce);

		HttpHeaders headers = new HttpHeaders();

		try {
			headers.add("apisign", encode(apiSecret, url.toString()));
		} catch (Exception e) {
			logger.error("Error while setting header ", e);
		}

		HttpEntity<String> request = new HttpEntity<String>(headers);

		String response = restTemplate.postForObject(url.toString(), request, String.class);

		OrderStatusResponse orderStatusResponse = gson.fromJson(response, OrderStatusResponse.class);

		return orderStatusResponse.getResult().isIsOpen();

	}

	private String encode(String key, String data) throws Exception {

		byte[] byteKey = key.getBytes("UTF-8");
		final String HMAC_SHA512 = "HmacSHA512";
		Mac sha512_HMAC = Mac.getInstance(HMAC_SHA512);
		SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA512);
		sha512_HMAC.init(keySpec);
		byte[] mac_data = sha512_HMAC.doFinal(data.getBytes("UTF-8"));
		String result = bytesToHex(mac_data);
		return result;

	}

	private String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

}

@JsonIgnoreProperties(ignoreUnknown = true)
class OrderStatusResponse {

	private boolean success;
	private String message;
	private OrderStatusResult result;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public OrderStatusResult getResult() {
		return result;
	}

	public void setResult(OrderStatusResult result) {
		this.result = result;
	}

	class OrderStatusResult {

		boolean IsOpen;

		public boolean isIsOpen() {
			return IsOpen;
		}

		public void setIsOpen(boolean isOpen) {
			IsOpen = isOpen;
		}

	}

}

@JsonIgnoreProperties(ignoreUnknown = true)
class OrderResponse {

	private boolean success;
	private String message;
	private OrderUUIDResult result;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public OrderUUIDResult getResult() {
		return result;
	}

	public void setResult(OrderUUIDResult result) {
		this.result = result;
	}

	class OrderUUIDResult {

		String uuid;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

	}

}

@JsonIgnoreProperties(ignoreUnknown = true)
class MarketSummaryResponse {

	private boolean success;
	private String message;
	private List<MarketDetailsResult> result = new ArrayList<>();

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<MarketDetailsResult> getResult() {
		return result;
	}

	public void setResult(List<MarketDetailsResult> result) {
		this.result = result;
	}

	class MarketDetailsResult {
		private String MarketName;
		private double High;
		private double Low;
		private double Volume;
		private double Last;
		private double BaseVolume;
		private String TimeStamp;
		private double Bid;
		private double Ask;
		private long OpenBuyOrders;
		private long OpenSellOrders;
		private double PrevDay;
		private String Created;

		public String getMarketName() {
			return MarketName;
		}

		public void setMarketName(String marketName) {
			MarketName = marketName;
		}

		public double getHigh() {
			return High;
		}

		public void setHigh(double high) {
			High = high;
		}

		public double getLow() {
			return Low;
		}

		public void setLow(double low) {
			Low = low;
		}

		public double getVolume() {
			return Volume;
		}

		public void setVolume(double volume) {
			Volume = volume;
		}

		public double getLast() {
			return Last;
		}

		public void setLast(double last) {
			Last = last;
		}

		public double getBaseVolume() {
			return BaseVolume;
		}

		public void setBaseVolume(double baseVolume) {
			BaseVolume = baseVolume;
		}

		public String getTimeStamp() {
			return TimeStamp;
		}

		public void setTimeStamp(String timeStamp) {
			TimeStamp = timeStamp;
		}

		public double getBid() {
			return Bid;
		}

		public void setBid(double bid) {
			Bid = bid;
		}

		public double getAsk() {
			return Ask;
		}

		public void setAsk(double ask) {
			Ask = ask;
		}

		public long getOpenBuyOrders() {
			return OpenBuyOrders;
		}

		public void setOpenBuyOrders(long openBuyOrders) {
			OpenBuyOrders = openBuyOrders;
		}

		public long getOpenSellOrders() {
			return OpenSellOrders;
		}

		public void setOpenSellOrders(long openSellOrders) {
			OpenSellOrders = openSellOrders;
		}

		public double getPrevDay() {
			return PrevDay;
		}

		public void setPrevDay(double prevDay) {
			PrevDay = prevDay;
		}

		public String getCreated() {
			return Created;
		}

		public void setCreated(String created) {
			Created = created;
		}

		@Override
		public String toString() {

			return String.format(
					"MarketDetails [MarketName=%s, High=%.8f, Low=%.8f, Volume=%.8f, Last=%.8f, Ask=%.8f, OpenBuyOrders=%d, OpenSellOrders=%d, PrevDay=%.8f, Created=%s]",
					MarketName, High, Low, Volume, Last, Ask, OpenBuyOrders, OpenSellOrders, PrevDay, Created);

		}
	}

}
