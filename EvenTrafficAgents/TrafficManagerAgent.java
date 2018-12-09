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

public class TrafficManagerAgent extends Agent {
	
	//Rejestracja typ agenta, aby później łatwo go znaleźć
	//Dodanie odpowiednią klase zachowań 
	protected void setup()
	{
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("TrafficManager");
		sd.setName("TrafficManagerAgent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		TrafficManagerBehaviour trafficManager = new TrafficManagerBehaviour(this);
		addBehaviour(trafficManager);
	}

	protected void takeDown()
	{
	    // Printout a dismissal message
	    System.out.println("CarAgent "+getAID().getName()+" terminating.");
	}
	
}
