package com.easypump.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import com.easypump.config.AppConfig;
import com.easypump.engine.PumpEngine;

public class Main {
	
	final static Logger logger = LoggerFactory.getLogger(Main.class);
	
	public static void main (String [] args)
	{
		@SuppressWarnings({ "resource"})
		AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		
		PumpEngine engine = (PumpEngine)context.getBean(PumpEngine.class);
		engine.setArgs(args);
		try {
			engine.startPump();
		} catch (Exception e) {			
			logger.error("Exception Occured while doing buy/sell transaction", e);			
		}		
		
		System.out.println("\n\n\nHope you will make a profit in this pump ;)");
		System.out.println("if you could make a proit using this app please conside doing some donation with 1$ or 2$ to BTC address 1PfnwEdmU3Ki9htakiv4tciPXzo49RRkai \nit will help us doing more features in the future");
	}
}
