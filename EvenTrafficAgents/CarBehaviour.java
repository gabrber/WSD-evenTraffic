package EvenTrafficBehaviours;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.Random;
import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import EvenTrafficClasses.*;
import java.util.concurrent.TimeUnit;

public class CarBehaviour extends CyclicBehaviour 
{
	private double x;
	private double y;
	private double x0;
	private double y0;
	private String currentDirection;
	private String mode;
	private AgentClass nextAgent;
	private DFAgentDescription trafficManagerTemplate;
	private MessageTemplate requestMessage;
	private MessageTemplate informMessage;
	private MessageTemplate informIFMessage;
	private long startTime;
	private long tempTime;
	private long measureTime;
	//prędkość agenta
	private double v;
	private AID trafficManagerAID;
	private boolean ambulanceIsNear;
	
	public CarBehaviour(Agent a,double X,double Y,String CurrentDirection)
	{
		super(a);
		x=X;
		y=Y;
		x0=X;
		y0=Y;
		currentDirection=CurrentDirection;
		mode="run";
		nextAgent=null;
		ambulanceIsNear=false;
		//wyszukanie agenta typu TrafficManager
		trafficManagerTemplate = new DFAgentDescription();
		trafficManagerTemplate=setTemplate(trafficManagerTemplate,"TrafficManager");
		trafficManagerAID=GetAgents(trafficManagerTemplate)[0];
		//ustawienie templateMessages
		requestMessage=MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		informMessage=MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		informIFMessage=MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF);
		startTime = System.currentTimeMillis();
		v=0.1;
	}
//Główna metoda klasy; Wykonuje się w nieskończonej pętli
	public void action()
	{ 
		tempTime=System.currentTimeMillis();
		measureTime=System.currentTimeMillis();
		// wysyłanie danych o agencie do trafficManagera
		sendStatusMessage();
		// pobieranie agenta znajdującego się przed danym agentem
		nextAgent=getNextAgent();
		while((measureTime-tempTime)<100)
		{
		// sprawdzenie czy w zasięgu pojawiła się karetka, jeżeli tak, samochód zjeżdża na pobocze 
	    boolean isAmbulance = isNeedToMakePlaceForAmbulance(x,y,currentDirection); 
	    if (isAmbulance && ambulanceIsNear==false)
	    { 							
			
	    	makePlaceForAmbulance(currentDirection);
			
	    }
		// jeżeli samochód zjechał, następuje sprawdzenie można wrócić na drogę
		if(ambulanceIsNear)
		{
			if(CanComeBackToRoad())
			{
				getBackOnRoad();
			}
			measureTime=System.currentTimeMillis();
		}
		else
		{
			//samochód jedzie
			if(mode=="run")
			{
				move();
			}
			//samochód stoi na drodze
			if(mode=="stop")
			{
				stop();
			}
			measureTime=System.currentTimeMillis();
			}
		}
	}
	
	// sprawdznie czy można wrócić na drogę
	public boolean CanComeBackToRoad()
	{
		sendQueryIFMessage();
		try
		{
			TimeUnit.MILLISECONDS.sleep(200);
		}
		catch(Exception ex)
		{
			
		}
		ArrayList<ACLMessage> msgs= ReceiveMessages(informIFMessage);
		if(msgs != null && msgs.size()>0)
			return true;
		return false;
		
		
	}
	// ruch samochodu
	public void move()
	{
		// jeżeli typ ="none" (nikogo nie ma przed samochodem) jedź 
		if(new String(nextAgent.type).equals("none"))
		{
			takeStep(currentDirection);
        }
		// jeżeli możesz (najbliższy agent nie jest bliżej niż określona wartość) to jedź, jeżeli nie, to sprawdź typ agenta i zareaguj odpowiednio 
		if(canTakeStep())
			takeStep(currentDirection);
		else{
		if(new String(nextAgent.type).equals("intersection"))
		{
			// skomplikowane warunki są spowodowane tym, że agent typu intersection przyjmuje tylko dwie wartości lightColor - "green" oznacza jazdę dla samochodów w kierunku "north" i "south", "red" przeciwnie.
			if(((new String(nextAgent.lightColor).equals("red")) && ((new String(currentDirection).equals("north")) || (new String(currentDirection).equals("south"))))
				|| ((new String(nextAgent.lightColor).equals("green")) && ((new String(currentDirection).equals("east")) || (new String(currentDirection).equals("west")))))
			{
				// jeżeli czerwone to stój
				mode="stop";
			}
			if(((new String(nextAgent.lightColor).equals("green")) && ((new String(currentDirection).equals("north")) || (new String(currentDirection).equals("south"))))
				|| ((new String(nextAgent.lightColor).equals("red")) && ((new String(currentDirection).equals("east")) || (new String(currentDirection).equals("west")))))
			{
				// jeżeli zielone to losowo wybierz kierunek ruchu i wjedź na drogę, a następnie pobierz następnego agenta
				if(canTakeStep())
					takeStep(currentDirection);
				else{
				mode="run";
				currentDirection=GetRandomDirection(currentDirection);
				x0=nextAgent.x;
				y0=nextAgent.y;
				x=x0;
				y=y0;
				System.out.println("next agent x= "+nextAgent.x);
				System.out.println("next agent y= "+nextAgent.y);
				startTime=System.currentTimeMillis();
				sendStatusMessage();
				try
			{
				// czekamy dla pewności, aż nowy status samochodu dotrze do TrafficManagera
				TimeUnit.MILLISECONDS.sleep(500);
			}
			catch(Exception ex)
			{
			
			}
				nextAgent=getNextAgent();
				}
			}
		}
		if(new String(nextAgent.type).equals("car"))
		{
			if(canTakeStep())
			{
				takeStep(currentDirection);
			}
			else
			{
				mode="stop";
			}
		}
		}
	}
	// sprawdzenie czy następny agent jest wystarczająco daleko, żeby można było jechać
	public boolean canTakeStep()
	{
		boolean canTakeStep=false;
		switch(currentDirection)
		{
			case "north":
				canTakeStep=((nextAgent.y - y)>1);
				break;		
			case "south":
				canTakeStep=((nextAgent.y - y)<-1);
				break;
			case "east":
				canTakeStep=((nextAgent.x - x)>1);
				break;
			case "west":
				canTakeStep=((nextAgent.x - x)<-1);
				break;
			default:
			break;
		}			
return canTakeStep;
	}		
// wyznacz losowy kierunek jazdy
	public String GetRandomDirection(String currentDirection)
	{
		String newDirection=currentDirection;
		Random rand=new Random();
		int n;
			n=rand.nextInt(4);
			switch(n) {
			case 0:
				newDirection="north";
				break;
			case 1:
				newDirection="south";
				break;
			case 2:
				newDirection="east";
				break;
			case 3:
				newDirection="west";
				break;
			}
		
		return newDirection;
	}
	
	
	// pobierz agenta znajdującego się przed danym samochodem
	public AgentClass getNextAgent()
	{
		sendQueryRefMessage();
		System.out.println("query ref sent");
		ACLMessage msg = myAgent.receive(informMessage);
		while(msg==null)
		{
			msg=myAgent.receive(informMessage);
		}
		System.out.println("Response received");
		
		return ParseAgent(msg);
		
	}
	//parsowanie agenta na klase AgentClass
	public AgentClass ParseAgent(ACLMessage message)
	{
		String content=message.getContent();
		String[] stringList = content.split(" ");
		for(String str:stringList)
		{
			System.out.println("STRING LIST ELEMENTS:" + str);
		}
		AgentClass agent=new AgentClass();
		agent.type=stringList[0];
		agent.x=Double.parseDouble(stringList[1]);
		agent.y=Double.parseDouble(stringList[2]);
		agent.direction=stringList[3];
		agent.lightColor=stringList[4];

		return agent;
	}
	// sprawdzenie czy trzeba zjechać na pobocze
	public boolean isNeedToMakePlaceForAmbulance(double x, double y, String currentDirection)
	{	
		ArrayList<ACLMessage> msgs=ReceiveMessages(requestMessage);
		
		if(msgs!=null && msgs.size()>0)
		{
			return true;
		}
		
		return false;
	}
// jazda do przodu. Zastosowanie czasu (measureTime-startTime) sprawia, że nie ma opóźnień ruchu agenta, przez dłuższy czas obliczeń
	void takeStep(String currentDirection)
	{
		measureTime = System.currentTimeMillis();
		
		if (currentDirection == "north")
		{
			y =y0 + v*0.001*(measureTime-startTime);
		}
		if (currentDirection == "south")
		{
			y =y0 - v*0.001*(measureTime-startTime);
		}
		if (currentDirection == "east")
		{
			x = x0 + v*0.001*(measureTime-startTime);
		}
		if (currentDirection == "west")
		{
			x = x0 - v*0.001*(measureTime-startTime);
		}
	}
		// zjedź na pobocze
	void makePlaceForAmbulance(String currentDirection)
	{
		ambulanceIsNear=true;
		x0=x;
		y0=y;
		if (currentDirection == "north")
		{
			x = x0 + 1;
		}
		if (currentDirection == "south")
		{
			x = x0 - 1;
		}
		if (currentDirection == "east")
		{
			y = y0 - 1;
		}
		if (currentDirection == "west")
		{
			y = y0 + 1;
		}
	}
	//wróć na drogę
	void getBackOnRoad()
	{
		ambulanceIsNear=false;
		x=x0;
		y=y0;
		startTime=System.currentTimeMillis();
	}
	// samochód stoi  
	void stop()
	{
		try
		{
			TimeUnit.MILLISECONDS.sleep(200);
		}
		catch(Exception ex)
		{
			
		}
		x0=x;
		y0=y;
		startTime=System.currentTimeMillis();
		mode="run";
	}
	
	private ArrayList<ACLMessage> ReceiveMessages()
	{
		ArrayList<ACLMessage> msgs = new ArrayList<ACLMessage>();
		ACLMessage msg=myAgent.receive();
		while(msg!=null)
		{
		msgs.add(msg);
		msg = myAgent.receive();
		}
		return msgs;
	}
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
	// wyślij wiadomość typu INFORM
	public void sendStatusMessage()
	{
		ACLMessage msg = new ACLMessage( ACLMessage.INFORM );
		msg.setContent("car "+ x +" "+y +" "+currentDirection + " null");
		msg.addReceiver(trafficManagerAID);
		myAgent.send(msg);
	}
	// wyślij wiadomość typu REQUEST
	public void sendRequestMessage()
	{
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.setContent("car "+ x +" "+y +" "+currentDirection + " null");
		msg.addReceiver(trafficManagerAID);
		myAgent.send(msg);
	}
	// wyślij wiadomość typu QUERY_REF
	public void sendQueryRefMessage()
	{
		ACLMessage msg = new ACLMessage( ACLMessage.QUERY_REF);
		msg.setContent("car "+ x +" "+y +" "+currentDirection + " null");
		msg.addReceiver(trafficManagerAID);
		myAgent.send(msg);
	}
	public void sendQueryIFMessage()
	{
		ACLMessage msg = new ACLMessage( ACLMessage.QUERY_IF);
		msg.setContent("car "+ x +" "+y +" "+currentDirection + " null");
		msg.addReceiver(trafficManagerAID);
		myAgent.send(msg);
	}
	public DFAgentDescription setTemplate(DFAgentDescription template,String AgentType)
	{
		ServiceDescription sd = new ServiceDescription();
		sd.setType(AgentType);
		template.addServices(sd);
		return template;
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
}