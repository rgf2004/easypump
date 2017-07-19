# Easy Pump

This tool is used to speed up the buy and sell actions of crypto currencies during pump period

usually pump period is not more than one minute so buy and sell actions should be placed very fast in order to be able to make a profit

that is why I've created this tool in order to make the buy and sell actions in less than 6 seconds. (it depends in your network traffic)

## How to compile ?
- Computer should have JAVA version 1.8 or more you may download and install it from https://java.com/en/download/
- download the execuatable Jar file or compile it your self using mvn install

## How to run ?

1- this tool takes the following parameters:
- exchange api key.
- exhcange secret key.
- amount of BTC that will be used
- profit percentage
	
	example : java -jar easy-pump-0.0.1-SNAPSHOT.jar fba4d194540e4b998f570cdwef0cdwecew3o b090f44028f54wrgregee12922cc04d 0.5 20

	
	in this example you ask the tool to use creditainls passed as api key and secret to buy a specific coin, tool will ask about it later, with BTC amount 0.5 and to make the sell price = buy price + 20% as profit
	
2- when tool loads in memory it will prompt the user to enter the desired coin
	coin should be passed as it is in exchange with "btc" keyword
	
	for example:
		eth for Ethereum
		dash for Dash
		xvg for Verge 
	
	you can get coin name from the exchange
	
3- once the user enters the coin name the tool will do the following:
- it tries to get the current ASK price from the exchange
- it calcuated the quantity that can be bought using the available BTC amount passed to the process.
- it places the buy order for this coin
- it monitors the exchange till this order is completed
- once order is completed it will calculate the sell price based on the profit percentage passed to it.
- it places the sell order for this coin using the propsed sell price and same quantity has been bought in the earlier buy order.
	


## Contribution

We are glad about every contribution to the project. Dont hesitate to open an issue, if you found a bug (with or without fix) or have an idea for a new feature!

If you want to share your own code, please follow these steps:
- create a fork of this repository
- add a new branch for your changings
- add your changes to the code
- dont forget to mention the issue number in the commit messages (just write something like ```<message> #<id>```)
- open a pull request and try to describe what the change is for
- done :)

## Donations :moneybag:

If you want to me, you can donate to:

```
- [ Bitcoin ] 1PfnwEdmU3Ki9htakiv4tciPXzo49RRkai
```
