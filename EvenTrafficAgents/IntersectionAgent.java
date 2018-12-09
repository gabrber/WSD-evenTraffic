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



public class IntersectionAgent extends Agent {

	private int x;
	private int y;
	private String currentDirection;
	//Rejestracja typ agenta, aby później łatwo go znaleźć
	//Dodanie odpowiednią klase zachowań 
	protected void setup() {
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("intersection");
		sd.setName("intersectionAgent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Ustawienie początkowego położenia agenta
		Object[] objects =getArguments();
		x=Integer.parseInt((String)objects[0]);
		y=Integer.parseInt((String)objects[1]);
		
		IntersectionAgentBehaviour intersectionBehaviour = new IntersectionAgentBehaviour(this,x,y);
		addBehaviour(intersectionBehaviour);
		System.out.println("Hallo! IntersectionAgent "+getAID().getName()+" is ready.");
		System.out.println("x= "+x+" y=" + y);
	}

	protected void takeDown() {
	    // Printout a dismissal message
	    System.out.println("IntersectionAgent "+getAID().getName()+" terminating.");
	  }
	
}