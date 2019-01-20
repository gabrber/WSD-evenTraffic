package EvenTrafficBehaviours;

import jade.core.*;
import jade.core.behaviours.*;
import java.util.concurrent.TimeUnit;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.ArrayList;
import EvenTrafficClasses.*;

public class IntersectionAgentBehaviour extends CyclicBehaviour 
{
	private DFAgentDescription trafficManagerTemplate;
	private MessageTemplate requestMessage;

	private MessageTemplate informMessage;
	private String lightColor;
	private double x;
	private double y;
	private long startTime;
	private long tempTime;
	private long currentTime;
	private String ambulanceDirection;
	// czas po którym zmieniają się światła 
	private long lightChangingRatio=50000;
	private AID trafficManagerAID;
	
	public IntersectionAgentBehaviour(Agent a,double X,double Y) 
	{
		super(a);
		x=X;
		y=Y;
		lightColor="green";
		//pobierz agenta typu TrafficManager
		trafficManagerTemplate = new DFAgentDescription();
		trafficManagerTemplate=setTemplate(trafficManagerTemplate,"TrafficManager");
		trafficManagerAID=GetAgents(trafficManagerTemplate)[0];
		requestMessage=MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		informMessage=MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		startTime = System.currentTimeMillis();		
	}
	//Główna metoda klasy; Wykonuje się w nieskończonej pętli
	public void action()
	{
		startTime=System.currentTimeMillis();
		currentTime=System.currentTimeMillis();
		while((currentTime-startTime)<lightChangingRatio)
		{
			
			tempTime=System.currentTimeMillis();
			while((currentTime-tempTime)<100)
			{
				// jeżeli otrzymano wiadomość typu request, zmień światło odpowiednio do kierunku ruchu karetki
				ArrayList<ACLMessage> msgs = ReceiveMessages(requestMessage);
				if(msgs!=null && msgs.size()>0)
				{
					ambulanceDirection=msgs.get(0).getContent();
					if((new String(ambulanceDirection).equals("north")) || (new String(ambulanceDirection).equals("south")))
						lightColor="green";
					else if((new String(ambulanceDirection).equals("east")) || (new String(ambulanceDirection).equals("west")))
						lightColor="red";
					
					startTime=System.currentTimeMillis();
				}
				currentTime=System.currentTimeMillis();
			}
			//wyślij wiadomość o stanie agenta co 500 milisekund
			sendStatusMessage();
			currentTime=System.currentTimeMillis();
		}
		changeLights();
	}
	//wyślij dane o agencie do Traffic Managera
	public void sendStatusMessage()
	{
		ACLMessage msg = new ACLMessage( ACLMessage.INFORM );
		msg.setContent("intersection "+ x +" "+y +" null "+lightColor);
		msg.addReceiver(trafficManagerAID);
		myAgent.send(msg);
	}
	//odbierz wiadomości
	private ArrayList<ACLMessage> ReceiveMessages(MessageTemplate template)
	{
		ArrayList<ACLMessage> msgs = new ArrayList<ACLMessage>();
		ACLMessage msg = myAgent.receive(template);
		while(msg!=null)
		{
		msgs.add(msg);
		msg = myAgent.receive(template);
		}
		return msgs;
	}
	
	public void changeLights()
	{
		if(lightColor=="red")
			lightColor="green";
		else
			lightColor="red";
	} 

	public AID[] GetAgents(DFAgentDescription template)
	{
		try
		{
			DFAgentDescription[] result = DFService.search(myAgent, template); 
			if(result==null)
				return new AID[0];
			AID[] tempAgents = new AID[result.length];
			for (int i = 0; i < result.length; ++i)
			{
				tempAgents[i] = result[i].getName();	
			}
			return tempAgents;
		}
		catch(FIPAException fe)
		{
			return null;
		}
	}
	public DFAgentDescription setTemplate(DFAgentDescription template,String AgentType)
	{
		ServiceDescription sd = new ServiceDescription();
		sd.setType(AgentType);
		template.addServices(sd);
		return template;
	}		
}
		