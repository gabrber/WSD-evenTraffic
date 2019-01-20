package EvenTrafficAgents;

import jade.core.*;
import jade.core.behaviours.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import EvenTrafficBehaviours.*;


public class CarAgent extends Agent {

	private double x;
	private double y;
	private String currentDirection;
	
	//Rejestracja typ agenta, aby później łatwo go znaleźć
	//Dodanie odpowiednią klase zachowań 
	protected void setup() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("car");
		sd.setName("carAgent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Ustawienie początkowego położenia agenta
		Object[] objects =getArguments();
		x=Double.parseDouble((String)objects[0]);
		y=Double.parseDouble((String)objects[1]);
		GetNode();
		 CarBehaviour carBehaviour = new CarBehaviour(this,x,y,currentDirection);
			addBehaviour(carBehaviour);
		 
		System.out.println("Hallo! CarAgent "+getAID().getName()+" is ready.");
	}

	protected void takeDown() {
	    System.out.println("CarAgent "+getAID().getName()+" terminating.");
	}
	// Wyznaczenie początkowego kierunku ruchu agenta
	protected void GetNode()
	{
		if(x<20 && (y==20 || y==40))
			currentDirection="east";
		else if(y>=30 && ( x==20 || x==40))
			currentDirection="south";
		else if(y<=30 && ( x==20 || x==40))
			currentDirection="north";
		else
			currentDirection="west";		
	}
}
