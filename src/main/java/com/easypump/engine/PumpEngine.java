package com.easypump.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easypump.exchange.bittrex.BittrexApi;

@Service
public class PumpEngine {

	final static Logger logger = LoggerFactory.getLogger(PumpEngine.class);

	private BigDecimal safeFactor = BigDecimal.valueOf(1.01);

	@Autowired
	private BittrexApi bittrexApi;

	private String[] args;

	private String apiKey;
	private String apiSecret;
	private String coinName;

	private BigDecimal btcValue;
	private BigDecimal profitPercentage;

	private BigDecimal buyFacotr;

	public void setArgs(String[] args) {
		this.args = args;
	}

	public void startPump() throws Exception {

		String msg;
		logger.info("Passed Parameters [{}]", Arrays.toString(args));

		validateAndParseArgs();

		logger.info("Waiting for Coin Name [example : eth]");
		Scanner sc = new Scanner(System.in);
		coinName = sc.next();
		sc.close();

		msg = String.format("Coin Name : BTC-%s, BTC Amount %.8f, Profit Percentage %f", coinName.toUpperCase(),
				btcValue, profitPercentage);
		logger.info(msg);

		logger.info("################################### Start Pump Engine ###################################");
		BigDecimal currentBid = bittrexApi.getCoinLimit(coinName);
		BigDecimal proposedBid = currentBid.multiply(buyFacotr);
		BigDecimal quantity = btcValue.divide(proposedBid.multiply(safeFactor), RoundingMode.DOWN);
		BigDecimal proposedAsk = currentBid
				.multiply(profitPercentage.add(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(100)));

		msg = String.format("Current Bid %.8f, Proposed Quantity %.8f, Proposed Ask %.8f", proposedBid, quantity,
				proposedAsk);
		logger.info(msg);

		String buyOrderId = bittrexApi.placeBuyOrder(apiKey, apiSecret, coinName, quantity, proposedBid);
		logger.info("Buy Order UUID {}", buyOrderId);

		bittrexApi.waitOpenOrderToClose(apiKey, apiSecret, buyOrderId);
		logger.info("Buy Order UUID {} closed, Engine will place sell order id", buyOrderId);

		String sellOrderId = bittrexApi.placeSellOrder(apiKey, apiSecret, coinName, quantity, proposedAsk);

		logger.info("Sell Order UUID {}", sellOrderId);
		logger.info("################################### Sell Order Placed ###################################");

	}

	public void validateAndParseArgs() throws Exception {
		if (this.args.length < 4) {
			logger.info(
					"Invalid Parameters : [API Key] [API Secret] [BTC Amount] [Profit Percentage]");
			logger.info("Example : 2f32827101 1ce239fod 0.02 40 eth 1.1");
			throw new Exception("Invalid arguments...");
		}

		apiKey = this.args[0];
		apiSecret = this.args[1];

		btcValue = BigDecimal.valueOf(Double.parseDouble(this.args[2]));
		profitPercentage = BigDecimal.valueOf(Double.parseDouble(this.args[3]));

		if (this.args.length >= 5)
			buyFacotr = BigDecimal.valueOf(Double.parseDouble(this.args[4]));
		else
			buyFacotr = BigDecimal.valueOf(1.0);

	}

}
